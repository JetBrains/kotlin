package runtime.workers.worker0

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
    val worker = startWorker()
    val future = worker.schedule(TransferMode.CHECKED, { "Input" }) {
        input -> input + " processed"
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().consume { _ -> }
    println("OK")
}