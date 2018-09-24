// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS
// COMMON_COROUTINES_TEST
// WITH_COROUTINES
// CHECK_BYTECODE_LISTING
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

interface SourceCrossinline<out E> {
    suspend fun consume(sink: Sink<E>)
    companion object Factory
}

interface Sink<in E> {
    suspend fun send(item: E)
    fun close(cause: Throwable?)
}

inline fun <E> source(crossinline action: suspend Sink<E>.() -> Unit): SourceCrossinline<E> = object : SourceCrossinline<E> {
    override suspend fun consume(sink: Sink<E>) {
        var cause: Throwable? = null
        try {
            action(sink)
        } catch (e: Throwable) {
            cause = e
        }
        sink.close(cause)
    }
}

fun SourceCrossinline.Factory.range(start: Int, count: Int): SourceCrossinline<Int> = source<Int> {
    var  i = start
    while (i < (start + count)) {
        send(i)
        ++i
    }
}

suspend inline fun <E> SourceCrossinline<E>.consumeEach(crossinline action: suspend (E) -> Unit) {
    consume(object : Sink<E> {
        override suspend fun send(item: E) = action(item)
        override fun close(cause: Throwable?) { cause?.let { throw it } }
    })
}

suspend inline fun <E, R> SourceCrossinline<E>.fold(initial: R, crossinline operation: suspend (acc: R, E) -> R): R {
    var acc = initial
    consumeEach {
        acc = operation(acc, it)
    }
    return acc
}

inline fun <E> SourceCrossinline<E>.filter(crossinline predicate: (E) -> Boolean) = source<E> {
    consumeEach {
        if (predicate(it)) send(it)
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun Int.isGood() = this % 4 == 0

fun box(): String {
    var res = 0
    builder {
        res = SourceCrossinline
            .range(1, 11)
            .filter { it.isGood() }
            .fold(0, { a, b -> a + b })
    }
    if (res != 12) return "FAIL"
    return "OK"
}