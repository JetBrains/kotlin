// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*

class CompilerKillingIterator<T, out R>(private val underlying: Iterator<T>, private val transform: suspend (e: T) -> Iterator<R>) {
    private var currentIt: Iterator<R> = object : Iterator<R> {
        override fun hasNext() = false

        override fun next(): R = null!!
    }

    suspend tailrec fun next(): R {
        return when {
            currentIt.hasNext() -> currentIt.next()
            underlying.hasNext() -> {
                currentIt = transform(underlying.next())
                next()
            }
            else -> throw IllegalArgumentException("Cannot call next() on the empty iterator")
        }
    }

    suspend fun hasNext() = currentIt.hasNext() || underlying.hasNext()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = ""
    builder {
        val iter = CompilerKillingIterator("ok".asIterable().iterator()) { ("" + it.toUpperCase()).asIterable().iterator() }
        while (iter.hasNext()) {
            res += iter.next()
        }
    }
    return res
}