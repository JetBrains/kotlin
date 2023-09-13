/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal fun LLFirResolveTarget.forEachPathElementAndTarget(action: (FirElementWithResolveState) -> Unit) {
    path.forEach(action)
    forEachTarget(action)
}

internal fun FirDesignationWithFile.asResolveTarget(): LLFirSingleResolveTarget = LLFirSingleResolveTarget(firFile, path, target)

/**
 * Resolves the target to the specified [phase].
 * The owning session must be a resolvable one.
 */
internal fun LLFirResolveTarget.resolve(phase: FirResolvePhase) {
    val session = firFile.llFirResolvableSession
        ?: errorWithAttachment("Resolvable session expected, got '${firFile.llFirSession::class.java}'") {
            withEntry("firSession", firFile.llFirSession) { it.toString() }
        }

    val lazyDeclarationResolver = session.moduleComponents.firModuleLazyDeclarationResolver
    lazyDeclarationResolver.lazyResolveTarget(this, phase, towerDataContextCollector = null)
}
