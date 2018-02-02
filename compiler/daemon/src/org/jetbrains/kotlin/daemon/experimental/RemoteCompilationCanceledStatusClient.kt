/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.RmiFriendlyCompilationCanceledException
import org.jetbrains.kotlin.progress.CompilationCanceledException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

val CANCELED_STATUS_CHECK_THRESHOLD_NS = TimeUnit.MILLISECONDS.toNanos(100)

class RemoteCompilationCanceledStatusClient(val facade: CompilerCallbackServicesFacade, val profiler: Profiler = DummyProfiler()): CompilationCanceledStatus {

    private val log by lazy { Logger.getLogger("compiler") }

    @Volatile var lastChecked: Long = System.nanoTime()

    override fun checkCanceled() {

        fun cancelOnError(e: Exception) {
            log.warning("error communicating with host, assuming compilation canceled (${e.message})")
            throw CompilationCanceledException()
        }

        val curNanos = System.nanoTime()
        if (curNanos - lastChecked > CANCELED_STATUS_CHECK_THRESHOLD_NS) {
            profiler.withMeasure(this) {
                try {
                    facade.compilationCanceledStatus_checkCanceled()
                }
                catch (e: RmiFriendlyCompilationCanceledException) {
                    throw CompilationCanceledException()
                }
                catch (e: java.rmi.ConnectIOException) {
                    cancelOnError(e)
                }
                catch (e: java.rmi.ConnectException) {
                    cancelOnError(e)
                }
                catch (e: java.rmi.NoSuchObjectException) {
                    // this was added mostly for tests since others are more difficult to emulate
                    cancelOnError(e)
                }
                catch (e: java.rmi.UnmarshalException) {
                    cancelOnError(e)
                }
            }
            lastChecked = curNanos
        }
    }
}
