/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors

import org.jetbrains.kotlin.fir.analysis.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.SimpleDiagnosticReporter
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger

class ParallelDiagnosticsCollector(private val numberOfThreads: Int) : AbstractDiagnosticCollector() {
    init {
        require(numberOfThreads >= 1) {
            "Number of threads should be at least 1"
        }
    }

    private var reporters = initializeReporters()
    private val collectorLocalIndex = ThreadLocal<Int>()
    private val collectorIndexCounter = AtomicInteger()
    private val futures = LinkedList<Future<*>>()

    private val pool = Executors.newFixedThreadPool(numberOfThreads) { runnable ->
        Thread {
            collectorLocalIndex.set(collectorIndexCounter.getAndIncrement())
            runnable.run()
        }
    }

    private fun initializeReporters(): List<SimpleDiagnosticReporter> {
        return (1..numberOfThreads).map { SimpleDiagnosticReporter() }
    }

    override fun initializeCollector() {
        reporters = initializeReporters()
        futures.clear()
    }

    override fun getCollectedDiagnostics(): Iterable<ConeDiagnostic> {
        futures.forEach { it.get() }
        return Iterable {
            object : Iterator<ConeDiagnostic> {
                private val globalIterator = reporters.iterator()
                private var localIterator = globalIterator.next().diagnostics.iterator()

                private fun update() {
                    while (!localIterator.hasNext() && globalIterator.hasNext()) {
                        localIterator = globalIterator.next().diagnostics.iterator()
                    }
                }

                override fun hasNext(): Boolean {
                    update()
                    return localIterator.hasNext()
                }

                override fun next(): ConeDiagnostic {
                    update()
                    return localIterator.next()
                }
            }
        }
    }

    override fun runCheck(block: (DiagnosticReporter) -> Unit) {
        futures += pool.submit {
            val reporter = reporters[collectorLocalIndex.get()]
            block(reporter)
        }
    }
}