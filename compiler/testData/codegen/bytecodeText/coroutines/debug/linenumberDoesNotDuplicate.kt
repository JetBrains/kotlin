suspend fun foo(i: Int) {
    println("Start foo")
    coroutineScope {
        if (i == 25) {
            startMethod(i)
        }
        delay(1)
        println("After delay $i")
    }
    println("coroutineScope completed $i")
}

suspend fun startMethod(i: Int) {}

suspend fun coroutineScope(c: suspend () -> Unit) {}

suspend fun delay(i: Int) {}

// 1 LINENUMBER 7 L*
