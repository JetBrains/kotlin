// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

import java.lang.reflect.ParameterizedType
import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class A(d: Double, s: String, parent: A?) {
    class Nested(a: A)
    inner class Inner(nested: Nested)
}

enum class E(val i: Int) { ENTRY(1) }

class Generic<T>(val t: T)

fun box(): String {
    assertEquals(listOf(java.lang.Double.TYPE, String::class.java, A::class.java), ::A.parameters.map { it.type.javaType })
    assertEquals(listOf(A::class.java), A::Nested.parameters.map { it.type.javaType })
    assertEquals(listOf(A::class.java, A.Nested::class.java), A::Inner.parameters.map { it.type.javaType })
    assertEquals(listOf(java.lang.Integer.TYPE), E::class.constructors.single().parameters.map { it.type.javaType })

    assertEquals(A::class.java, ::A.returnType.javaType)
    assertEquals(A.Nested::class.java, A::Nested.returnType.javaType)
    assertEquals(A.Inner::class.java, A::Inner.returnType.javaType)

    val generic = Generic::class.constructors.single().returnType.javaType
    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        assertEquals(Generic::class.java, generic)
    } else {
        assertTrue(generic is ParameterizedType)
        assertEquals(Generic::class.java, (generic as ParameterizedType).rawType)
    }

    return "OK"
}
