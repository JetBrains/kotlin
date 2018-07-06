package runtime.workers.lazy0

import kotlin.test.*

import konan.worker.*

data class Data(val x: Int, val y: String)

object Immutable {
    val x by atomicLazy {
        42
    }
}

object Immutable2 {
    val y by atomicLazy {
        Data(239, "Kotlin")
    }
}

@Test fun runTest() {
    assertEquals(42, Immutable.x)

    val COUNT = 5
    val workers = Array(COUNT, { _ -> startWorker()})

    val set = mutableSetOf<Any?>()
    for (attempt in 1 .. 3) {
        val futures = Array(workers.size, { workerIndex ->
            workers[workerIndex].schedule(TransferMode.CHECKED, { "" }) { _  -> Immutable2.y }
        })
        futures.forEach { set += it.result() }
    }
    assertEquals(set.size, 1)
    assertEquals(set.single(), Immutable2.y)

    println("OK")
}
