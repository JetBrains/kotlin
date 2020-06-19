// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

class A(d: Double, s: String, parent: A?) {
    class Nested(a: A)
    inner class Inner(nested: Nested)
}

enum class E(val i: Int) { ENTRY(1) }

fun box(): String {
    assertEquals(listOf(java.lang.Double.TYPE, String::class.java, A::class.java), ::A.parameters.map { it.type.javaType })
    assertEquals(listOf(A::class.java), A::Nested.parameters.map { it.type.javaType })
    assertEquals(listOf(A::class.java, A.Nested::class.java), A::Inner.parameters.map { it.type.javaType })
    assertEquals(listOf(java.lang.Integer.TYPE), E::class.constructors.single().parameters.map { it.type.javaType })

    assertEquals(A::class.java, ::A.returnType.javaType)
    assertEquals(A.Nested::class.java, A::Nested.returnType.javaType)
    assertEquals(A.Inner::class.java, A::Inner.returnType.javaType)

    return "OK"
}
