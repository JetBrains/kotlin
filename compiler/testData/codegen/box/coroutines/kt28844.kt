// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
// IGNORE_BACKEND: NATIVE

import helpers.*
import COROUTINES_PACKAGE.*

fun builder(block: suspend Unit.() -> Unit) {
    block.startCoroutine(Unit, EmptyContinuation)
}

var res = "FAIL 1"

fun testOuterJobIsCancelled() = builder {
    suspend fun callJobScoped() = "OK"

    val outerJob = suspend {
        res = callJobScoped()
    }
    outerJob()
}

fun testOuterJobIsCancelled2() = builder {
    suspend fun callJobScoped() = "OK"

    suspend fun outerJob() {
        res = callJobScoped()
    }
    outerJob()
}

fun box(): String {
    testOuterJobIsCancelled()
    if (res != "OK") return res
    res = "FAIL 2"
    testOuterJobIsCancelled2()
    return res
}
