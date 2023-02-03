// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
// FULL_JDK
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun box(): String {
    val x = gen().joinToString()
    if (x != "1, 2") return "fail1: $x"

    val y = gen().joinToString()
    if (y != "-1") return "fail2: $y"
    return "OK"
}

var was = false

fun gen() = generate<Int> {
    if (was) {
        yield(-1)
        return@generate
    }
    for (i in 1..2) {
        yield(i)
    }
    was = true
}

// LIBRARY CODE
interface Generator<in T> {
    suspend fun yield(value: T)
}

fun <T> generate(block: suspend Generator<T>.() -> Unit): Sequence<T> = GeneratedSequence(block)

class GeneratedSequence<out T>(private val block: suspend Generator<T>.() -> Unit) : Sequence<T> {
    override fun iterator(): Iterator<T> = GeneratedIterator(block)
}

class GeneratedIterator<T>(block: suspend Generator<T>.() -> Unit) : AbstractIterator<T>(), Generator<T> {
    private var nextStep: Continuation<Unit> = block.createCoroutine(this, object : Continuation<Unit> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(data: Result<Unit>) {
            data.getOrThrow()
            done()
        }
    })

    override fun computeNext() {
        nextStep.resume(Unit)
    }
    suspend override fun yield(value: T) = suspendCoroutineUninterceptedOrReturn<Unit> { c ->
        setNext(value)
        nextStep = c

        COROUTINE_SUSPENDED
    }
}
