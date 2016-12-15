// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.coroutines.*

fun nonSuspend() {}

suspend fun foo() {
    CoroutineIntrinsics.<!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>suspendCoroutineOrReturn { x: Continuation<Int> -> }<!>

    nonSuspend()
}

suspend fun unitSuspend() {
    CoroutineIntrinsics.<!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>suspendCoroutineOrReturn { x: Continuation<Int> -> }<!>
}

suspend fun baz(): Int = run {
    CoroutineIntrinsics.<!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>suspendCoroutineOrReturn { x: Continuation<Int> -> }<!>
}
