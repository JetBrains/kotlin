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

@Test fun runTest() {
    val COUNT = 100
    val workers = Array(COUNT, { _ -> Worker.start() })
    val jobs = initJobs(COUNT)
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
    assertEquals(consumed, COUNT)
    workers.forEach {
        it.requestTermination().result
    }
    println("OK")
}