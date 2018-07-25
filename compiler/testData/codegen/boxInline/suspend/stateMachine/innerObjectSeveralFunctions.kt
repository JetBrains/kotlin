// FILE: inlined.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING

interface SuspendRunnable {
    suspend fun run1()
    suspend fun run2()
}

suspend inline fun crossinlineMe(crossinline c1: suspend () -> Unit, crossinline c2: suspend () -> Unit) {
    val o = object : SuspendRunnable {
        override suspend fun run1() {
            c1()
        }
        override suspend fun run2() {
            c2()
        }
    }
    o.run1()
    o.run2()
}

// FILE: inlineSite.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var i = 0;
var j = 0;

suspend fun incrementI() {
    i++
}

suspend fun incrementJ() {
    j++
}

fun box(): String {
    builder {
        crossinlineMe({ incrementI() }) { incrementJ() }
    }
    if (i != 1) return "FAIL i $i"
    if (j != 1) return "FAIL i $i"
    return "OK"
}