// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
// FULL_JDK
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

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
    private var nextStep: Continuation<Unit> = block.createCoroutine(this, object : ContinuationAdapter<Unit>() {
        override val context = EmptyCoroutineContext

        override fun resume(data: Unit) {
            done()
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
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
