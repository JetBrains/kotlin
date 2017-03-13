// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

class A {
    private fun foo() = "A"
}

fun box(): String {
    val f = A::class.declaredFunctions.single() as KFunction<String>

    try {
        f.call(A())
        return "Fail: no exception was thrown"
    } catch (e: IllegalCallableAccessException) {}

    f.isAccessible = true

    assertEquals("A", f.call(A()))

    return "OK"
}
