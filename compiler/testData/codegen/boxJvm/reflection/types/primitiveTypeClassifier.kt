// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public class J {
    public J(char primitive, Character wrapper) {}
}

// FILE: box.kt
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.test.assertEquals

class A(
    val primitive: Boolean,
    val wrapper: Boolean?,
)

fun primitive(): Byte = 0.toByte()
fun wrapper(): Byte? = null

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: IntArray)
fun primitiveInAnnoArrayArg(): @Anno([1, 2, 3]) Unit {}

fun primitiveArray(): IntArray = intArrayOf(1)

class B<T : Float>

fun array(): Array<Long> = arrayOf(1L)
fun list(): List<Double> = listOf(1.0)

private fun KType.javaClass(): Class<*> = (classifier as KClass<*>).java

fun box(): String {
    assertEquals(
        listOfNotNull(Char::class.javaPrimitiveType, Char::class.javaObjectType),
        J::class.constructors.single().parameters.map { it.type.javaClass() },
    )

    assertEquals(
        listOfNotNull(Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType),
        A::class.constructors.single().parameters.map { it.type.javaClass() },
    )

    assertEquals(Byte::class.javaPrimitiveType!!, ::primitive.returnType.javaClass())
    assertEquals(Byte::class.javaObjectType, ::wrapper.returnType.javaClass())

    assertEquals(Short::class.javaPrimitiveType!!, typeOf<Short>().javaClass())
    assertEquals(Short::class.javaObjectType, typeOf<Short?>().javaClass())

    assertEquals(
        Int::class.javaPrimitiveType!!,
        ::primitiveInAnnoArrayArg.returnType.findAnnotation<Anno>()!!.value::class.java.componentType,
    )

    assertEquals(Int::class.javaPrimitiveType!!, ::primitiveArray.returnType.javaClass().componentType)

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        assertEquals(Float::class.javaPrimitiveType!!, B::class.typeParameters.single().upperBounds.single().javaClass())
    } else {
        assertEquals(Float::class.javaObjectType, B::class.typeParameters.single().upperBounds.single().javaClass())
    }

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        assertEquals(Long::class.javaPrimitiveType!!, ::array.returnType.arguments.single().type!!.javaClass())
    } else {
        assertEquals(Long::class.javaObjectType, ::array.returnType.arguments.single().type!!.javaClass())
    }

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        assertEquals(Double::class.javaPrimitiveType!!, ::list.returnType.arguments.single().type!!.javaClass())
    } else {
        assertEquals(Double::class.javaObjectType, ::list.returnType.arguments.single().type!!.javaClass())
    }

    return "OK"
}
