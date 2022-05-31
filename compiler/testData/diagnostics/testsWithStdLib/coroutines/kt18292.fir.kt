// SKIP_TXT
// WITH_STDLIB

import kotlin.coroutines.*

interface Job : CoroutineContext.Element {}
interface Deferred<out T> : Job {
    suspend fun await(): T
}
fun <T> async(block: suspend () -> T): Deferred<T> = TODO()

suspend fun fib(n: Long) =
    async {
        when {
            n < 2 -> n
            else -> <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>fib(n - 1)<!>.<!UNRESOLVED_REFERENCE!>await<!>() + <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!>fib(n - 2)<!>.<!UNRESOLVED_REFERENCE!>await<!>()
        }
    }
