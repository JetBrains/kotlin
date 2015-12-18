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
import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.RmiFriendlyCompilationCancelledException
import org.jetbrains.kotlin.progress.CompilationCanceledException
import java.util.concurrent.TimeUnit

val CANCELED_STATUS_CHECK_THRESHOLD_NS = TimeUnit.MILLISECONDS.toNanos(100)

class RemoteCompilationCanceledStatusClient(val facade: CompilerCallbackServicesFacade, val profiler: Profiler = DummyProfiler()): CompilationCanceledStatus {
    @Volatile var lastChecked: Long = System.nanoTime()
    override fun checkCanceled() {
        val curNanos = System.nanoTime()
        if (curNanos - lastChecked > CANCELED_STATUS_CHECK_THRESHOLD_NS) {
            profiler.withMeasure(this) {
                try {
                    facade.compilationCanceledStatus_checkCanceled()
                }
                catch (e: RmiFriendlyCompilationCancelledException) {
                    throw CompilationCanceledException()
                }
            }
            lastChecked = curNanos
        }
    }
}
