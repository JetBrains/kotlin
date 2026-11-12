// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.4
// ^^^ Fixed in 2.4.20-Beta1.
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> String): String {
    var result = "FAIL 1"
    c.startCoroutine(handleResultContinuation {
        result = it
    })
    return result
}

suspend fun suspendBox(): String {
    // Lambda result might require boxing. The test checks that the compiler handles it properly.
    return if (suspendCoroutineUninterceptedOrReturn<Boolean> { cont -> true }) "OK" else "FAIL 2"
}

fun box(): String {
    return builder { suspendBox() }
}
