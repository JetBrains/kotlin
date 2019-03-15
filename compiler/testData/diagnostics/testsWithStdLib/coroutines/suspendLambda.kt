// COMMON_COROUTINES_TEST
// SKIP_TXT
import COROUTINES_PACKAGE.*

fun <T> foo(): Continuation<T> = null!!

fun bar() {
    suspend {
        println()
    }.startCoroutine(foo<Unit>())
}
