/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.worker9

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest1() {
    withLock { println("zzz") }
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        withLock {
            println("42")
        }
    }
    future.result
    worker.requestTermination().result
    println("OK")
}

fun withLock(op: () -> Unit) {
    op()
}

@Test fun runTest2() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, {}) {
        val me = Worker.current!!
        var x = 1
        me.executeAfter (20000) {
            println("second ${++x}")
        }
        me.executeAfter(10000) {
            println("first ${++x}")
        }
    }
    worker.requestTermination().result
}

@Test fun runTest3() {
    val worker = Worker.start()
    assertFailsWith<IllegalStateException> {
        worker.executeAfter {
            println("shall not happen")
        }
    }
    assertFailsWith<IllegalArgumentException> {
        worker.executeAfter(-1, {
            println("shall not happen")
        }.freeze())
    }

    worker.executeAfter(0, {
        println("frozen OK")
    }.freeze())

    worker.requestTermination().result
}