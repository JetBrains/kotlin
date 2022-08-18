/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.experimental

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.EventManager
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.utils.createStringInterner

class RemoteLookupTrackerClient(
    val facade: CompilerCallbackServicesFacadeAsync,
    eventManager: EventManager,
    val profiler: Profiler = DummyProfiler()
) : LookupTracker {
    private val isDoNothing = runBlocking { profiler.withMeasure(this) { facade.lookupTracker_isDoNothing() } }

    private val lookups = hashSetOf<LookupInfo>()
    private val interner = createStringInterner()

    override val requiresPosition: Boolean = runBlocking { profiler.withMeasure(this) { facade.lookupTracker_requiresPosition() } }

    override fun record(filePath: String, position: Position, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        if (isDoNothing) return

        val internedFilePath = interner.intern(filePath)
        val internedScopeFqName = interner.intern(scopeFqName)
        val internedName = interner.intern(name)

        lookups.add(LookupInfo(internedFilePath, position, internedScopeFqName, scopeKind, internedName))
    }

    override fun clear() {
        lookups.clear()
    }

    init {
        runBlocking {
            eventManager.onCompilationFinished { flush() }
        }
    }

    private fun flush() {
        if (isDoNothing || lookups.isEmpty()) return

        profiler.withMeasureBlocking(this) {
            facade.lookupTracker_record(lookups)
        }

        lookups.clear()
    }
}
