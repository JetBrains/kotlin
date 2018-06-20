package runtime.workers.atomic0

import kotlin.test.*

import konan.worker.*

fun test1(workers: Array<Worker>) {
    val atomic = AtomicInt(15)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].schedule(TransferMode.CHECKED, { atomic }) {
            input -> input.increment()
        }
    })
    futures.forEach {
        it.result()
    }
    println(atomic.get())
}

fun test2(workers: Array<Worker>) {
    val atomic = AtomicInt(1)
    val counter = AtomicInt(0)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].schedule(TransferMode.CHECKED, { Triple(atomic, workerIndex, counter) }) {
            (place, index, result) ->
            // Here we simulate mutex using [place] location to store tag of the current worker.
            // When it is negative - worker executes exclusively.
            val tag = index + 1
            while (place.compareAndSwap(tag, -tag) != tag) {}
            val ok1 = result.increment() == index + 1
            // Now, let the next worker run.
            val ok2 = place.compareAndSwap(-tag, tag + 1) == -tag
            ok1 && ok2
        }
    })
    futures.forEach {
        assertEquals(it.result(), true)
    }
    println(counter.get())
}

data class Data(val value: Int)

fun test3(workers: Array<Worker>) {
    val common = AtomicReference<Data>()
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].schedule(TransferMode.CHECKED, { Pair(common, workerIndex) }) {
            (place, index) ->
            val mine = Data(index).freeze()
            // Try to publish our own data, until successful, in a tight loop.
            while (place.compareAndSwap(null, mine) != null) {}
        }
    })
    val seen = mutableSetOf<Data>()
    for (i in 0 until workers.size) {
        do {
            val current = common.get()
            if (current != null && !seen.contains(current)) {
                seen += current
                // Let others publish.
                assertEquals(common.compareAndSwap(current, null), current)
                break
            }
        } while (true)
    }
    futures.forEach {
        it.result()
    }
    assertEquals(seen.size, workers.size)
}

fun test4() {
    assertFailsWith<InvalidMutabilityException> {
        AtomicReference(Data(1))
    }
    assertFailsWith<InvalidMutabilityException> {
        AtomicReference<Data>().compareAndSwap(null, Data(2))
    }
}

@Test fun runTest() {
    val COUNT = 20
    val workers = Array(COUNT, { _ -> startWorker()})

    test1(workers)
    test2(workers)
    test3(workers)
    test4()

    workers.forEach {
        it.requestTermination().consume { _ -> }
    }
    println("OK")
}