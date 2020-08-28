// TARGET_BACKEND: JVM

// WITH_REFLECT
// FULL_JDK

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

annotation class Z
enum class E

annotation class Anno(
        val b: Byte,
        val s: String,
        val ss: Array<String>,
        val z: Z,
        val zs: Array<Z>,
        val e: E,
        val es: Array<E>,
        val k: KClass<*>,
        val ka: Array<KClass<*>>
)

fun tmp(): Array<Class<*>> = null!!

fun box(): String {
    val t = Anno::class.constructors.single().parameters.map { it.type.javaType }

    assertEquals(Byte::class.java, t[0])
    assertEquals(String::class.java, t[1])
    assertEquals(Array<String>::class.java, t[2])
    assertEquals(Z::class.java, t[3])
    assertEquals(Array<Z>::class.java, t[4])
    assertEquals(E::class.java, t[5])
    assertEquals(Array<E>::class.java, t[6])

    assertTrue(t[7] is ParameterizedType)
    assertEquals(Class::class.java, (t[7] as ParameterizedType).rawType)

    assertTrue(t[8] is GenericArrayType)
    val e = (t[8] as GenericArrayType).genericComponentType
    assertTrue(e is ParameterizedType)
    assertEquals(Class::class.java, (e as ParameterizedType).rawType)

    return "OK"
}
