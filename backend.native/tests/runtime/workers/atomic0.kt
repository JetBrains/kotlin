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
    val atomic = AtomicInt(0)
    val counter = AtomicInt(0)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].schedule(TransferMode.CHECKED, { Triple(atomic, workerIndex, counter) }) {
            (place, index, result) ->
            while (place.compareAndSwap(index, index + 1) != index) {}
            result.increment() == index + 1
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
}