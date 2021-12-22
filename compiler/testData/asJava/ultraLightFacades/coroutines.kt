//RELEASE_COROUTINE_NEEDED
suspend fun doSomething(foo: String): Int {}

fun <T> async(block: suspend () -> T)

// WITH_STDLIB
// FIR_COMPARISON
