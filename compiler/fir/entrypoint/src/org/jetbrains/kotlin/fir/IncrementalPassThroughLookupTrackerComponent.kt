/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnInPsiFile
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartList
import java.util.concurrent.ConcurrentHashMap

class IncrementalPassThroughLookupTrackerComponent(
    private val lookupTracker: LookupTracker,
    private val sourceToFilePath: (KtSourceElement) -> String?
) : FirLookupTrackerComponent() {

    private val requiresPosition = lookupTracker.requiresPosition
    private val sourceToFilePathsCache = ConcurrentHashMap<KtSourceElement, String>()

    override fun recordLookup(name: Name, inScopes: List<String>, source: KtSourceElement?, fileSource: KtSourceElement?) {
        assert(fileSource != null || source is KtPsiSourceElement) // finding file for a source only possible for PSI, here it means
        // that we allow null for file source only for PSI-only "sources", currently - java ones
        val definedSource = fileSource ?: source ?: throw AssertionError("Cannot record lookup for \"$name\" without a source")
        val path = sourceToFilePathsCache.getOrPut(definedSource) {
            sourceToFilePath(definedSource) ?:
                // TODO: the lookup by non-file source mostly doesn't work for the LT, so we cannot afford null file sources here
                return
        }
        val position = if (requiresPosition && source != null && source is KtPsiSourceElement) {
            getLineAndColumnInPsiFile(source.psi.containingFile, source.psi.textRange).let { Position(it.line, it.column) }
        } else Position.NO_POSITION

        for (scope in inScopes) {
            lookupTracker.record(path, position, scope, ScopeKind.PACKAGE, name.asString())
        }
    }

    override fun recordLookup(name: Name, inScope: String, source: KtSourceElement?, fileSource: KtSourceElement?) {
        recordLookup(name, SmartList(inScope), source, fileSource)
    }
}
