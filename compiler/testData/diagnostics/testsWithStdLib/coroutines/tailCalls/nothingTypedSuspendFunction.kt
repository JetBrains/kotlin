// Tail calls are not allowed to be Nothing typed. See KT-15051
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun suspendLogAndThrow(exception: Throwable): Nothing = suspendCoroutineOrReturn { c ->
    c.resumeWithException(exception)
    COROUTINE_SUSPENDED
}
