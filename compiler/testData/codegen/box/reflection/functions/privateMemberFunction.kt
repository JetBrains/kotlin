// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KFunction
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
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
