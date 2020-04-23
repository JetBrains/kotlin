// !LANGUAGE: +SuspendConversion
// WITH_RUNTIME
// WITH_COROUTINES
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// ^ this test hangs on JS_IR (even if it's IGNOREd)
// IGNORE_BACKEND: JVM_IR

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


fun runSuspend(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class C {
    var test = "failed"

    fun foo() {
        test = "OK"
    }
}

fun box(): String {
    val c = C()
    runSuspend(c::foo)
    return c.test
}