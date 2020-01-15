package kt35392

fun main() {
    testDeadlock()
}

fun testDeadlock() = runBlocking<Unit> { //1
    val job = launch {
        runBlocking { // 3
        }
    }
    yield()
    runBlocking { // 4
        println("Done")
        while (job.isActive) yield()
        //Breakpoint!
    }
}