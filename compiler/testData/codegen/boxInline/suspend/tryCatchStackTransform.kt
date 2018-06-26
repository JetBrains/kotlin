// FILE: inlined.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING

suspend inline fun inlineMe(c: suspend (String) -> String, d: suspend () -> String): String {
    return c(try { d() } catch (e: Exception) { "Exception 1 ${e.message}" })
}

suspend inline fun noinlineMe(noinline c: suspend (String) -> String, noinline d: suspend () -> String): String {
    return c(try { d() } catch (e: Exception) { "Exception 1 ${e.message}" })
}

suspend inline fun crossinlineMe(crossinline c: suspend (String) -> String, crossinline d: suspend () -> String): String {
    return c(try { d() } catch (e: Exception) { "Exception 1 ${e.message}" })
}

// FILE: inlineSite.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun yieldString(s: String) = s

suspend fun throwException(s: String): String {
    throw RuntimeException(s)
}

suspend fun inlineSite() {
    var res = inlineMe({ it }) { yieldString("OK") }
    if (res != "OK") throw RuntimeException("FAIL 1: $res")

    res = inlineMe({ it }) { throwException("OK") }
    if (res != "Exception 1 OK") throw RuntimeException("FAIL 2: $res")

    res = noinlineMe({ it }) { yieldString("OK") }
    if (res != "OK") throw RuntimeException("FAIL 3: $res")

    res = noinlineMe({ it }) { throwException("OK") }
    if (res != "Exception 1 OK") throw RuntimeException("FAIL 4: $res")

    res = crossinlineMe({ it }) { yieldString("OK") }
    if (res != "OK") throw RuntimeException("FAIL 5: $res")

    res = crossinlineMe({ it }) { throwException("OK") }
    if (res !=  "Exception 1 OK") throw RuntimeException("FAIL 6: $res")

    res = inlineMe({ try {yieldString(it) } catch (e: Exception) { yieldString("Exception 2 $it") } }) { yieldString("OK") }
    if (res != "OK") throw RuntimeException("FAIL 7: $res")

    res = inlineMe({ try {throwException(it) } catch (e: Exception) { yieldString("Exception 2 $it") } }) { yieldString("OK") }
    if (res != "Exception 2 OK") throw RuntimeException("FAIL 8: $res")

    res = noinlineMe({ try {yieldString(it) } catch (e: Exception) { yieldString("Exception 2 $it") } }) { yieldString("OK") }
    if (res != "OK") throw RuntimeException("FAIL 9: $res")

    res = noinlineMe({ try {throwException(it) } catch (e: Exception) { yieldString("Exception 2 $it") } }) { yieldString("OK") }
    if (res != "Exception 2 OK") throw RuntimeException("FAIL 10: $res")

    res = crossinlineMe({ try {yieldString(it) } catch (e: Exception) { yieldString("Exception 2 $it") } }) { yieldString("OK") }
    if (res != "OK") throw RuntimeException("FAIL 11: $res")

    res = crossinlineMe({ try {throwException(it) } catch (e: Exception) { yieldString("Exception 2 $it") } }) { yieldString("OK") }
    if (res !=  "Exception 2 OK") throw RuntimeException("FAIL 12: $res")
}

fun box(): String {
    builder {
        inlineSite()
    }
    return "OK"
}