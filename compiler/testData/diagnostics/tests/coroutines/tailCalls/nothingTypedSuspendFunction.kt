// Tail calls are not allowed to be Nothing typed. See KT-15051
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendLogAndThrow(exception: Throwable): Nothing = suspendCoroutineOrReturn { c ->
    c.resumeWithException(exception)
    SUSPENDED_MARKER
}
