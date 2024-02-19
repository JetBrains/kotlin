/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal fun FirDesignation.asResolveTarget(): LLFirSingleResolveTarget = LLFirSingleResolveTarget(this)

/**
 * Resolves the target to the specified [phase].
 * The owning session must be a resolvable one.
 */
internal fun LLFirResolveTarget.resolve(phase: FirResolvePhase) {
    val session = target.llFirResolvableSession
        ?: errorWithAttachment("Resolvable session expected, got '${target.llFirSession::class.java}'") {
            withEntry("firSession", target.llFirSession) { it.toString() }
        }

    val lazyDeclarationResolver = session.moduleComponents.firModuleLazyDeclarationResolver
    lazyDeclarationResolver.lazyResolveTarget(this, phase)
}

internal val LLFirResolveTarget.session: LLFirSession get() = target.llFirSession
