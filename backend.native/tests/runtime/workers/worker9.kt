package runtime.workers.worker9

import kotlin.test.*

import konan.worker.*

@Test fun runTest() {
    withLock { println("zzz") }
    val worker = startWorker()
    val future = worker.schedule(TransferMode.CHECKED, {}) {
        withLock {
            println("42")
        }
    }
    future.result()
    worker.requestTermination().result()
    println("OK")
}

fun withLock(op: () -> Unit) {
    op()
}