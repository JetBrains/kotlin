// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.experimental.ExperimentalTypeInference

interface AsyncGenerator<in T> {
    suspend fun yield(value: T)
}

interface AsyncSequence<out T> {
    operator fun iterator(): AsyncIterator<T>
}

interface AsyncIterator<out T> {
    operator suspend fun hasNext(): Boolean
    operator suspend fun next(): T
}

@OptIn(ExperimentalTypeInference::class)
fun <T> asyncGenerate(@BuilderInference block: suspend AsyncGenerator<T>.() -> Unit): AsyncSequence<T> = object : AsyncSequence<T> {
    override fun iterator(): AsyncIterator<T> {
        val iterator = AsyncGeneratorIterator<T>()
        iterator.nextStep = block.createCoroutine(receiver = iterator, completion = iterator)
        return iterator
    }
}

class AsyncGeneratorIterator<T>: AsyncIterator<T>, AsyncGenerator<T>, ContinuationAdapter<Unit>() {
    var computedNext = false
    var nextValue: T? = null
    var nextStep: Continuation<Unit>? = null

    // if (computesNext) computeContinuation is Continuation<T>
    // if (!computesNext) computeContinuation is Continuation<Boolean>
    var computesNext = false
    var computeContinuation: Continuation<*>? = null

    override val context = EmptyCoroutineContext

    suspend fun computeHasNext(): Boolean = suspendCoroutineUninterceptedOrReturn { c ->
        computesNext = false
        computeContinuation = c
        nextStep!!.resume(Unit)
        COROUTINE_SUSPENDED
    }

    suspend fun computeNext(): T = suspendCoroutineUninterceptedOrReturn { c ->
        computesNext = true
        computeContinuation = c
        nextStep!!.resume(Unit)
        COROUTINE_SUSPENDED
    }

    @Suppress("UNCHECKED_CAST")
    fun resumeIterator(exception: Throwable?) {
        if (exception != null) {
            done()
            computeContinuation!!.resumeWithException(exception)
            return
        }
        if (computesNext) {
            computedNext = false
            (computeContinuation as Continuation<T>).resume(nextValue as T)
        } else {
            (computeContinuation as Continuation<Boolean>).resume(nextStep != null)
        }
    }

    override suspend fun hasNext(): Boolean {
        if (!computedNext) return computeHasNext()
        return nextStep != null
    }

    override suspend fun next(): T {
        if (!computedNext) return computeNext()
        computedNext = false
        return nextValue as T
    }

    private fun done() {
        computedNext = true
        nextStep = null
    }

    // Completion continuation implementation
    override fun resume(value: Unit) {
        done()
        resumeIterator(null)
    }

    override fun resumeWithException(exception: Throwable) {
        done()
        resumeIterator(exception)
    }

    // Generator implementation
    override suspend fun yield(value: T): Unit = suspendCoroutineUninterceptedOrReturn { c ->
        computedNext = true
        nextValue = value
        nextStep = c
        resumeIterator(null)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun cst(a: Any?): String? = a as String?
fun any(a: Any?): Any? = a

fun box(): String {
    val seq = asyncGenerate {
        yield("O")
        yield("K")
    }

    var res = ""

    builder {
        // type of `prev` should be j/l/Object everywhere (even in a expected type position)
        var prev: Any? = null
        for (i in seq) {
            res += i
            prev = any(res)
            // merge of NULL_VALUE and j/l/Object should result in common j/l/Object value
            // but it was NULL_VALUE and we do not spill null values, we just put
            // ACONST_NULL after suspension point instead
        }

        res = cst(prev) ?: "fail 1"
    }

    return res
}
