package runtime.workers.worker1

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
    val COUNT = 5
    val workers = Array(COUNT, { _ -> startWorker()})

    for (attempt in 1 .. 3) {
        val futures = Array(workers.size,
                { i -> workers[i].schedule(TransferMode.CHECKED, { "$attempt: Input $i" })
                { input -> input + " processed" }
        })
        futures.forEachIndexed { index, future ->
            future.consume {
                result ->
                if ("$attempt: Input $index processed" != result) {
                    println("Got unexpected $result")
                    throw Error(result)
                }
            }
        }
    }
    workers.forEach {
        it.requestTermination().consume { _ -> }
    }
    println("OK")
}