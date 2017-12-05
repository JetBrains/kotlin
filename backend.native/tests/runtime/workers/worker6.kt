package runtime.workers.worker6

import kotlin.test.*

import konan.worker.*

@Test fun runTest() {
    val worker = startWorker()
    val future = worker.schedule(TransferMode.CHECKED, { 42 }) {
        input -> input.toString()
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().consume { _ -> }
    println("OK")
}