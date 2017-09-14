// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.full.IllegalCallableAccessException
import kotlin.reflect.jvm.isAccessible

class A {
    private fun foo(default: Any? = this) {
    }

    fun f() = A::foo
}

fun box(): String {
    val a = A()
    val f = a.f()

    try {
        f.callBy(mapOf(f.parameters.first() to a))
        return "Fail: IllegalCallableAccessException should have been thrown"
    }
    catch (e: IllegalCallableAccessException) {
        // OK
    }

    f.isAccessible = true
    f.callBy(mapOf(f.parameters.first() to a))

    return "OK"
}
