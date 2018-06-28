/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.daemon.common.impls.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.impls.DummyProfiler
import org.jetbrains.kotlin.daemon.common.impls.Profiler
import org.jetbrains.kotlin.daemon.common.impls.RmiFriendlyCompilationCanceledException
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
