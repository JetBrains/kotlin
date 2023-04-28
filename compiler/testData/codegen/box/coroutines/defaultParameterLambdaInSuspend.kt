// WITH_STDLIB
// WITH_COROUTINES

// lowered IR can be dependent on file order here, so we will test both

// FILE: A.kt

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller1 {
    suspend fun suspendHere(block : () -> String = { "DEF" }): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(block())
        COROUTINE_SUSPENDED
    }
    suspend fun getDef() = "DEF2"
    suspend fun suspendHere2(block : suspend () -> String = { getDef() }): String {
        val result = block()
        return suspendCoroutineUninterceptedOrReturn { x ->
            x.resume(result)
            COROUTINE_SUSPENDED
        }
    }
}

fun builder1(c: suspend Controller1.() -> Unit) {
    c.startCoroutine(Controller1(), EmptyContinuation)
}

// FILE: B.kt

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun box(): String {
    var result = "FAIL 1"

    builder1 {
        if (suspendHere() != "DEF") {
            result = "FAIL"
            return@builder1
        }
        if (suspendHere2() != "DEF2") {
            result = "FAIL"
            return@builder1
        }
        result = suspendHere { "OK" }
    }
    if (result != "OK") return result
    result = "FAIL 2"

    builder2 {
        if (suspendHere() != "DEF") {
            result = "FAIL"
            return@builder2
        }
        if (suspendHere2() != "DEF2") {
            result = "FAIL"
            return@builder2
        }
        result = suspendHere { "OK" }
    }

    return result
}

// FILE: C.kt

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


class Controller2 {
    suspend fun suspendHere(block : () -> String = { "DEF" }): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(block())
        COROUTINE_SUSPENDED
    }
    suspend fun getDef() = "DEF2"
    suspend fun suspendHere2(block : suspend () -> String = { getDef() }): String {
        val result = block()
        return suspendCoroutineUninterceptedOrReturn { x ->
            x.resume(result)
            COROUTINE_SUSPENDED
        }
    }
}

fun builder2(c: suspend Controller2.() -> Unit) {
    c.startCoroutine(Controller2(), EmptyContinuation)
}


