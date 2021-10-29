//RELEASE_COROUTINE_NEEDED
suspend fun doSomething(foo: String): Int {}

fun <T> async(block: suspend () -> T)

// FIR_COMPARISON
