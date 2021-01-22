// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// WITH_RUNTIME
// WITH_COROUTINES
package suspendLambdaAssertionsDisabled

import helpers.*
import kotlin.coroutines.*

class Checker {
    fun check() {
        builder { assert(false) }
    }
}

class Dummy

fun disableAssertions(): Checker {
    val loader = Dummy::class.java.classLoader
    loader.setPackageAssertionStatus("suspendLambdaAssertionsDisabled", false)
    val c = loader.loadClass("suspendLambdaAssertionsDisabled.Checker")
    return c.newInstance() as Checker
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var c = disableAssertions()
    c.check()

    return "OK"
}
