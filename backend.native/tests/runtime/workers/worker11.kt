/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.worker11

import kotlin.test.*

import kotlin.native.concurrent.*
import kotlinx.cinterop.convert

data class Job(val index: Int, var input: Int, var counter: Int)

fun initJobs(count: Int) = Array<Job?>(count) { i -> Job(i, i * 2, i)}

@Test fun runTest0() {
    val workers = Array(100, { _ -> Worker.start() })
    val jobs = initJobs(workers.size)
    val futures = Array(workers.size, { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, {
            val job = jobs[workerIndex]
            jobs[workerIndex] = null
            job!!
        }) { job ->
            job.counter += job.input
            job
        }
    })
    val futureSet = futures.toSet()
    var consumed = 0
    while (consumed < futureSet.size) {
        val ready = waitForMultipleFutures(futureSet, 10000)
        ready.forEach {
            it.consume { job ->
                assertEquals(job.index * 3, job.counter)
                jobs[job.index] = job
            }
            consumed++
        }
    }
    assertEquals(consumed, workers.size)
    workers.forEach {
        it.requestTermination().result
    }
    println("OK")
}

val COUNT = 2

@SharedImmutable
val counters = Array(COUNT) { AtomicInt(0) }

@Test fun runTest1() {
  val workers = Array(COUNT) { Worker.start() }
  // Ensure processQueue() can only be called on current Worker.
  workers.forEach {
      assertFailsWith<IllegalStateException> {
          it.processQueue()
      }
  }
  val futures = Array(workers.size) { workerIndex ->
      workers[workerIndex].execute(TransferMode.SAFE, {
          workerIndex
      }) {
          index ->
          assertEquals(0, counters[index].value)
          // Process following request.
          while (!Worker.current!!.processQueue()) {}
          // Ensure it has an effect.
          assertEquals(1, counters[index].value)
          // No more non-terminating tasks in this worker queue.
          assertEquals(false, Worker.current!!.processQueue())
      }
  }
  val futures2 = Array(workers.size) { workerIndex ->
      workers[workerIndex].execute(TransferMode.SAFE, {
          workerIndex
      }) { index ->
          assertEquals(0, counters[index].value)
          counters[index].increment()
      }
  }
  futures2.forEach { it.result }
  futures.forEach { it.result }
  workers.forEach {
      it.requestTermination().result
  }

  // Ensure terminated workers are no longer there.
  workers.forEach {
    assertFailsWith<IllegalStateException> { it.execute(TransferMode.SAFE, { Unit }) { println("ERROR") } }
  }
}

@Test fun runTest2() {
    val workers = Array(COUNT) { Worker.start() }
    val futures = Array(workers.size) { workerIndex ->
        workers[workerIndex].execute(TransferMode.SAFE, { null }) {
            // Here we processed termination request.
            assertEquals(false, Worker.current.processQueue())
        }
    }
    workers.forEach {
        it.executeAfter(1000*1000*1000, {
            println("DELAY EXECUTED")
            assert(false)
        }.freeze())
    }
    workers.forEach {
        it.requestTermination(processScheduledJobs = false).result
    }
    // Process futures, ignoring possible cancelled ones.
    futures.forEach {
        try { it.result } catch (e: IllegalStateException) {}
    }
}
