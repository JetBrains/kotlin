import library.*
import kotlin.coroutines.experimental.*

suspend fun test() {
    append("[foo]")
    foo { // we are inlining foo here
        append("(block)")
    }
    append("[bar]")
    bar() // and invoking suspending function bar
    append("[test]")
}

fun runBlockingLibrary(block: suspend () -> Unit): String {
    var done = false
    block.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resume(value: Unit) { done = true }
        override fun resumeWithException(exception: Throwable) { throw exception }
    })
    var resumeCounter = 0
    while (!done) {
        resumeCounter++
        resumeLibrary()
    }
    return "$libraryResult:resumes=$resumeCounter"
}

// Retruns array of expected and received string
fun run(): Array<String> {
    val result = runBlockingLibrary {
        test()
    }
    return arrayOf("[foo](foo)(block)[bar](bar)[test]:resumes=2", result)
}

