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
            else -> <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!><!DEBUG_INFO_MISSING_UNRESOLVED!>fib<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>n<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>-<!> 1)<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>await<!>() <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!>fib<!>(n - 2).<!DEBUG_INFO_MISSING_UNRESOLVED!>await<!>()
        }
    }
