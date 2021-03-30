/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnInPsiFile
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList
import java.util.concurrent.ConcurrentHashMap

class IncrementalPassThroughLookupTrackerComponent(
    private val lookupTracker: LookupTracker,
    private val sourceToFilePath: (FirSourceElement) -> String
) : FirLookupTrackerComponent() {

    private val requiresPosition = lookupTracker.requiresPosition
    private val sourceToFilePathsCache = ConcurrentHashMap<FirSourceElement, String>()

    override fun recordLookup(name: Name, inScopes: List<String>, source: FirSourceElement?, fileSource: FirSourceElement?) {
        val definedSource = fileSource ?: source ?: throw AssertionError("Cannot record lookup for \"$name\" without a source")
        val path = sourceToFilePathsCache.getOrPut(definedSource) {
            sourceToFilePath(definedSource)
        }
        val position = if (requiresPosition && source != null && source is FirPsiSourceElement<*>) {
            getLineAndColumnInPsiFile(source.psi.containingFile, source.psi.textRange).let { Position(it.line, it.column) }
        } else Position.NO_POSITION

        for (scope in inScopes) {
            lookupTracker.record(path, position, scope, ScopeKind.PACKAGE, name.asString())
        }
    }

    override fun recordLookup(name: Name, inScope: String, source: FirSourceElement?, fileSource: FirSourceElement?) {
        recordLookup(name, SmartList(inScope), source, fileSource)
    }
}
