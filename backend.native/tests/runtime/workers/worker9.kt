/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.worker9

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
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