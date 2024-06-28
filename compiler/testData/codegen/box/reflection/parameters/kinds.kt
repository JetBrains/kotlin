// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.full.*
import kotlin.reflect.KParameter.Kind.*
import kotlin.test.assertEquals

class A {
    fun Int.foo(x: String) {}

    inner class Inner(s: String) {}
}

fun box(): String {
    val foo = A::class.memberExtensionFunctions.single()

    assertEquals(listOf(INSTANCE, EXTENSION_RECEIVER, VALUE), foo.parameters.map { it.kind })
    assertEquals(listOf(INSTANCE, VALUE), A::Inner.parameters.map { it.kind })

    return "OK"
}
