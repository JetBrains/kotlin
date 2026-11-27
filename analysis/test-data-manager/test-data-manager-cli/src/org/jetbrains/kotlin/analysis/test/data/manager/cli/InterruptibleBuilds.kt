/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.cli

import java.io.OutputStream
import java.io.PrintStream
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * The builds of one run, writing into [log], under one Ctrl+C handler for the whole run, [interrupt]: a Ctrl+C between the
 * builds ends the run with its summary too. On Ctrl+C, Gradle is asked to cancel the running build, which [run] waits
 * for; a second Ctrl+C stops waiting, leaving the build to the daemon, which cancels it when this process is gone. A
 * Ctrl+C while a build is being started takes effect once it has started; after a Ctrl+C, no further build starts.
 *
 * @param root the repository root, whose `gradlew` the [commandLine] of each build names
 * @param err where the user is told that the run waits for Gradle
 */
internal class InterruptibleBuilds(
    private val gradle: GradleLauncher,
    private val root: Path,
    log: OutputStream,
    private val err: PrintStream,
) {
    private val log = SynchronizedOutputStream(log)
    private val interruptCount = AtomicInteger()
    private val running = AtomicReference<RunningBuild?>()

    val interrupted: Boolean
        get() = interruptCount.get() > 0

    /** Handles the Ctrl+C that is number [count] of the run. */
    fun interrupt(count: Int) {
        // The callbacks may run on several threads, so a later Ctrl+C may be recorded first
        interruptCount.accumulateAndGet(count, ::maxOf)
        if (count == 1) {
            err.println("Interrupted, waiting for Gradle to cancel the build; Ctrl+C again stops waiting")
            err.flush()
        }

        running.get()?.interrupt(count)
    }

    /**
     * Runs the build with [arguments], with its [commandLine] first in the log; returns how it ended, or `null` if it wasn't
     * waited for or didn't start after a Ctrl+C.
     */
    fun run(arguments: List<String>): GradleOutcome? {
        if (interrupted) return null
        log.write((commandLine(root, arguments) + "\n").toByteArray())
        val started = gradle.start(arguments, log)
        running.set(started)
        try {
            // An interrupt that came before the build was set; cancelling or abandoning twice does nothing more
            interruptCount.get().takeIf { it > 0 }?.let { started.interrupt(it) }
            return started.await()
        } finally {
            running.set(null)
        }
    }

    private fun RunningBuild.interrupt(count: Int) {
        if (count == 1) cancel() else abandon()
    }
}

/** Gradle writes its standard output and error from different threads into one log. */
private class SynchronizedOutputStream(private val delegate: OutputStream) : OutputStream() {
    @Synchronized
    override fun write(b: Int) = delegate.write(b)

    @Synchronized
    override fun write(b: ByteArray, off: Int, len: Int) = delegate.write(b, off, len)

    @Synchronized
    override fun flush() = delegate.flush()
}
