// WITH_STDLIB
import kotlin.coroutines.*

fun <T> runBlocking(c: suspend () -> T): T {
    var res: T? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

fun box(): String = runBlocking {          // Non-inline lambda;
    run {                                  // In it an inline lambda
        val foo = { Result.success("OK") } // Unboxes Result.
        foo().getOrNull()!!
    }
}
