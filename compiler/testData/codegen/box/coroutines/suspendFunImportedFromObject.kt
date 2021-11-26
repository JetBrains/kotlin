// WITH_STDLIB
// WITH_COROUTINES
// FILE: stuff.kt
package stuff
import helpers.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

object Host {
    suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume("OK")
        COROUTINE_SUSPENDED
    }
}


// FILE: test.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
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
