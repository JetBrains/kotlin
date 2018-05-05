// WITH_RUNTIME
// COMMON_COROUTINES_TEST
// WITH_COROUTINES
import helpers.*
// TREAT_AS_ONE_FILE
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
    x.resume("OK")
}

suspend fun suspendThere(param: Int, param2: String, param3: Long): String {
    val a = suspendHere()
    val b = suspendHere()
    return a + b
}

// 0 ASTORE 4
