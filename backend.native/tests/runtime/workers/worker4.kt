package runtime.workers.worker4

import kotlin.test.*

import konan.worker.*

@Test fun runTest() {
    val worker = startWorker()
    val future = worker.schedule(TransferMode.CHECKED, { 41 }) {
        input -> input + 1
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().consume { _ -> }
    println("OK")
}