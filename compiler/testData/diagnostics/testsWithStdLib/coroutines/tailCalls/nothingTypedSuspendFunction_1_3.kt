// Tail calls are not allowed to be Nothing typed. See KT-15051
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendLogAndThrow(exception: Throwable): Nothing = <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>suspendCoroutineUninterceptedOrReturn<!> { c ->
    c.<!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>resumeWithException<!>(exception)
    COROUTINE_SUSPENDED
}
