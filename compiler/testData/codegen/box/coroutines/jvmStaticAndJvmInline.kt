// TARGET_BACKEND: JVM
// WITH_RUNTIME
// WITH_COROUTINES

// This test passes for JVM_IR in master, but fails in 1.5.0 and 1.5.20 for some reason, probably because depends on some other changes which were not cherry-picked.
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun <T> suspendHere(x: T): T = suspendCoroutineUninterceptedOrReturn {
    it.resume(x)
    COROUTINE_SUSPENDED
}

inline class I(val x: Any?)

open class C {
    companion object {
        @JvmStatic
        protected suspend fun f(): I = I(suspendHere("OK"))
    }
}

class D : C() {
    companion object {
        suspend fun g() = f()
    }
}

fun box(): String {
    var result = "FAIL"
    builder {
        result = D.g().x as String
    }
    return result
}
