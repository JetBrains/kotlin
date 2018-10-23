// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS, JS_IR
// KOTLIN_CONFIGURATION_FLAGS: ASSERTIONS_MODE=jvm
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

package suspendFunctionAssertionDisabled

import helpers.*
import COROUTINES_PACKAGE.*

class Checker {
    suspend fun check() {
        assert(false)
    }
}

class Dummy

fun disableAssertions(): Checker {
    val loader = Dummy::class.java.classLoader
    loader.setPackageAssertionStatus("suspendFunctionAssertionDisabled", false)
    val c = loader.loadClass("suspendFunctionAssertionDisabled.Checker")
    return c.newInstance() as Checker
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var c = disableAssertions()
    builder { c.check() }

    return "OK"
}