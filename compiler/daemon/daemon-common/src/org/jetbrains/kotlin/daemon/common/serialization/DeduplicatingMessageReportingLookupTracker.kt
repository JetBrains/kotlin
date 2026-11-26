/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.daemon.common.serialization

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import kotlin.collections.getOrPut

class DeduplicatingMessageReportingLookupTracker(val lookupTracker: CompilerLookupTracker) : CompilerLookupTracker {
    // Map: FileName -> (ScopeKind -> (ScopeFqName -> Set<Name[String] | LookupInfo>))
    private val lookups =
        mutableMapOf<String, MutableMap<CompilerLookupTracker.ScopeKind, MutableMap<String, MutableSet<String>>>>()

    override fun recordLookup(
        filePath: String,
        scopeFqName: String,
        scopeKind: CompilerLookupTracker.ScopeKind,
        name: String,
    ) {
        lookups.getOrPut(filePath, ::mutableMapOf)
            .getOrPut(scopeKind, ::mutableMapOf)
            .getOrPut(scopeFqName, ::mutableSetOf)
            .add(name)
    }

    override fun clear() {
        lookups.clear()
    }

    fun flush() {
        if (lookups.isEmpty()) return
        lookups.forEach { [filePath, lookupsByFile] ->
            lookupsByFile.forEach { [scopeKind, lookupsByScopeKind] ->
                lookupsByScopeKind.forEach { [scopeFqName, lookupsByScopeFqName] ->
                    lookupsByScopeFqName.forEach { lookupInfoOrString ->
                        lookupTracker.recordLookup(filePath, scopeFqName, scopeKind, lookupInfoOrString)
                    }
                }
            }
        }
        lookups.clear()
    }
}
