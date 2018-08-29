/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.workers.worker4

import kotlin.test.*

import kotlin.native.concurrent.*

@Test fun runTest() {
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { 41 }) {
        input -> input + 1
    }
    future.consume {
        result -> println("Got $result")
    }
    worker.requestTermination().result
    println("OK")
}