/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import sun.misc.Signal
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

/** Where Ctrl+C goes. */
internal interface Interrupts {
    /**
     * Runs [block]. A Ctrl+C meanwhile calls [onInterrupt] with how many of them came so far, instead of ending the
     * process, which is what it does outside of [handling].
     */
    fun <T> handling(onInterrupt: (count: Int) -> Unit, block: () -> T): T
}

/** [Interrupts] of `SIGINT`, which Ctrl+C sends on every platform. */
internal object SignalInterrupts : Interrupts {
    private val handler = AtomicReference<((Int) -> Unit)?>()
    private val count = AtomicInteger()

    fun install() {
        try {
            Signal.handle(Signal("INT")) {
                val current = handler.get()
                if (current != null) {
                    current(count.incrementAndGet())
                } else {
                    System.err.println("Interrupted")
                    exitProcess(EXIT_INTERRUPTED)
                }
            }
        } catch (_: IllegalArgumentException) {
            // The process started with SIGINT ignored, like a background job: there is no Ctrl+C to handle
        }
    }

    override fun <T> handling(onInterrupt: (count: Int) -> Unit, block: () -> T): T {
        count.set(0)
        handler.set(onInterrupt)
        try {
            return block()
        } finally {
            handler.set(null)
        }
    }
}
