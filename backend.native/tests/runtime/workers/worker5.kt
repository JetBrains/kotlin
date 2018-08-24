package runtime.workers.worker5

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
    val worker = startWorker()
    val future = worker.schedule(TransferMode.CHECKED, { "zzz" }) {
        input -> input.length
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().consume { _ -> }
    println("OK")
}