// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

// FILE: stuff.kt
package stuff
import helpers.*

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

object Host {
    suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume("OK")
        COROUTINE_SUSPENDED
    }
}


// FILE: test.kt
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import stuff.Host.suspendHere

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
