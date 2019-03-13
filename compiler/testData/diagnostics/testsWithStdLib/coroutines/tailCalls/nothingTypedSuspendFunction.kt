// Tail calls are not allowed to be Nothing typed. See KT-15051
// COMMON_COROUTINES_TEST
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun suspendLogAndThrow(exception: Throwable): Nothing = <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>suspendCoroutineUninterceptedOrReturn<!> { c ->
    c.resumeWithException(exception)
    COROUTINE_SUSPENDED
}
