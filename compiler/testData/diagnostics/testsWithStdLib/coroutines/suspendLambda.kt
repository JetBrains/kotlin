// FIR_IDENTICAL
// SKIP_TXT
import kotlin.coroutines.*

fun <T> foo(): Continuation<T> = null!!

fun bar() {
    suspend {
        println()
    }.startCoroutine(foo<Unit>())
}
