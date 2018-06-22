package runtime.basic.worker_random

import konan.worker.*
import kotlin.collections.*
import kotlin.random.*
import kotlin.system.*
import kotlin.test.*

@Test
fun testRandomWorkers() {
    val seed = getTimeMillis()
    val workers = Array(5, { _ -> startWorker()})

    val attempts = 3
    val results = Array(attempts, { ArrayList<Int>() } )
    for (attempt in 0 until attempts) {
        Random.seed = seed
        // Produce a list of random numbers in each worker
        val futures = Array(workers.size, { workerIndex ->
            workers[workerIndex].schedule(TransferMode.CHECKED, { workerIndex }) { input ->
                Array(10, { Random.nextInt() }).toList()
            }
        })
        // Now collect all results into current attempt's list
        val futureSet = futures.toSet()
        for (i in 0 until futureSet.size) {
            val ready = futureSet.waitForMultipleFutures(10000)
            ready.forEach { results[attempt].addAll(it.result()) }
        }
    }

    workers.forEach {
        it.requestTermination().consume { _ -> }
    }
}