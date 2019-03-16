// !DIAGNOSTICS: -DEPRECATION
// Tail calls are not allowed to be Nothing typed. See KT-15051
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendLogAndThrow(exception: Throwable): Nothing = <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>suspendCoroutineUninterceptedOrReturn<!> { c ->
    c.resumeWithException(exception)
    COROUTINE_SUSPENDED
}
