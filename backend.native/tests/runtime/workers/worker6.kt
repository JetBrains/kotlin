/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.worker6

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
    val worker = startWorker()
    val future = worker.schedule(TransferMode.CHECKED, { 42 }) {
        input -> input.toString()
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().consume { _ -> }
    println("OK")
}