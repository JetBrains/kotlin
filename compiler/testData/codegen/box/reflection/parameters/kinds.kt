// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_REFLECT

import kotlin.reflect.*
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
