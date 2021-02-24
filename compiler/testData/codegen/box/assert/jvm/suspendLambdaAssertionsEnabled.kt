// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// WITH_RUNTIME
// WITH_COROUTINES
package suspendLambdaAssertionsEnabled

import helpers.*
import kotlin.coroutines.*

class Checker {
    fun check() {
        builder { assert(false) }
    }
}

class Dummy

fun enableAssertions(): Checker {
    val loader = Dummy::class.java.classLoader
    loader.setPackageAssertionStatus("suspendLambdaAssertionsEnabled", true)
    val c = loader.loadClass("suspendLambdaAssertionsEnabled.Checker")
    return c.newInstance() as Checker
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var c = enableAssertions()
    try {
        c.check()
        return "FAIL 6"
    } catch (ignore: AssertionError) {
    }

    return "OK"
}
