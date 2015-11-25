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

import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler


class RemoteLookupTrackerClient(val facade: CompilerCallbackServicesFacade, val eventManger: EventManger, val profiler: Profiler = DummyProfiler()) : LookupTracker {
    private val isDoNothing = profiler.withMeasure(this) { facade.lookupTracker_isDoNothing() }

    private val lookups = hashSetOf<LookupInfo>()

    override val requiresPosition: Boolean = profiler.withMeasure(this) { facade.lookupTracker_requiresPosition() }

    override fun record(filePath: String, position: Position, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        if (isDoNothing) return

        lookups.add(LookupInfo(filePath, position, scopeFqName, scopeKind, name))
    }

    init {
        eventManger.onCompilationFinished { flush() }
    }

    private fun flush() {
        if (isDoNothing || lookups.isEmpty()) return

        profiler.withMeasure(this) {
            facade.lookupTracker_record(lookups)
        }

        lookups.clear()
    }
}
