// Tail calls are not allowed to be Nothing typed. See KT-15051
import kotlin.coroutines.*

suspend fun suspendLogAndThrow(exception: Throwable): Nothing = <!SUSPENSION_CALL_MUST_BE_USED_AS_RETURN_VALUE!>suspendWithCurrentContinuation { c ->
    c.resumeWithException(exception)
    SUSPENDED
}<!>
