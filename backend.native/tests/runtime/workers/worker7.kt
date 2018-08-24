package runtime.workers.worker7

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
    val worker = startWorker()
    val future = worker.schedule(TransferMode.CHECKED, { "Input" }) {
        input -> println(input)
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().consume { _ -> }
    println("OK")
}