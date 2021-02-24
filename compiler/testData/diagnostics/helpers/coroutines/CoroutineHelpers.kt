@file:JvmMultifileClass
@file:JvmName("CoroutineUtilKt")

package helpers

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.jvm.internal.*

fun <T> handleResultContinuation(x: (T) -> Unit): Continuation<T> = object: Continuation<T> {
    override val context = EmptyCoroutineContext
    override fun resumeWith(result: Result<T>) {
        x(result.getOrThrow())
    }
}

fun handleExceptionContinuation(x: (Throwable) -> Unit): Continuation<Any?> = object: Continuation<Any?> {
    override val context = EmptyCoroutineContext
    override fun resumeWith(result: Result<Any?>) {
        result.exceptionOrNull()?.let(x)
    }
}

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) {
        result.getOrThrow()
    }
}

class ResultContinuation : Continuation<Any?> {
    override val context = EmptyCoroutineContext
    override fun resumeWith(result: Result<Any?>) {
        this.result = result.getOrThrow()
    }

    var result: Any? = null
}

abstract class ContinuationAdapter<in T> : Continuation<T> {
    override val context: CoroutineContext = EmptyCoroutineContext
    override fun resumeWith(result: Result<T>) {
        if (result.isSuccess) {
            resume(result.getOrThrow())
        } else {
            resumeWithException(result.exceptionOrNull()!!)
        }
    }

    abstract fun resumeWithException(exception: Throwable)
    abstract fun resume(value: T)
}
