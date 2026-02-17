import kotlin.coroutines.*

private fun runSuspend(block: suspend () -> String): String {
    var result: Result<String>? = null
    block.startCoroutine(object : Continuation<String> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(r: Result<String>) { result = r }
    })
    return result!!.getOrThrow()
}

fun test1(): String {
    val c = C()
    val s = with(c) { 10.foo() }
    return if (s == "10/B10/true") "OK" else "FAIL1"
}

fun test2(): String {
    val c = C()
    val s = runSuspend { c.bar(5) }
    return if (s == "bar/5/B5/true") "OK" else "FAIL2"
}

fun test3(): String {
    val c = C()
    val s = runSuspend { c.bar() }
    return if (s == "bar/1/B1/true") "OK" else "FAIL3"
}
