// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
// TREAT_AS_ONE_FILE
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*
suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
    x.resume("OK")
}

suspend fun suspendThere(param: Int, param2: String, param3: Long): String {
    val a = suspendHere()
    val b = suspendHere()
    return a + b
}

/* 2 stores happen because the continuation parameter is visible in debug and should be spilled */
// 2 ASTORE 4
