/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.ResolveStateAccess
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry

internal fun FirElementWithResolveState.checkPhase(requiredResolvePhase: FirResolvePhase) {
    @OptIn(ResolveStateAccess::class)
    val declarationResolveState = resolveState
    checkWithAttachment(
        declarationResolveState.resolvePhase >= requiredResolvePhase,
        { "At least $requiredResolvePhase expected but $declarationResolveState found for ${this::class.simpleName}" },
    ) {
        withFirEntry("firDeclaration", this@checkPhase)
    }
}

internal fun FirDesignation.checkPathPhase(firResolvePhase: FirResolvePhase) =
    path.forEach { it.checkPhase(firResolvePhase) }

internal fun FirDesignation.checkDesignationPhase(firResolvePhase: FirResolvePhase) {
    checkPathPhase(firResolvePhase)
    target.checkPhase(firResolvePhase)
}

internal fun FirDesignation.checkDesignationPhaseForClasses(firResolvePhase: FirResolvePhase) {
    checkPathPhase(firResolvePhase)
    if (target is FirClassLikeDeclaration) {
        @OptIn(ResolveStateAccess::class)
        val resolveState = target.resolveState
        check(resolveState.resolvePhase >= firResolvePhase) {
            "Expected $firResolvePhase but found $resolveState"
        }
    }
}

internal fun FirDesignation.isTargetCallableDeclarationAndInPhase(firResolvePhase: FirResolvePhase): Boolean =
    (target as? FirCallableDeclaration)?.let { it.resolvePhase >= firResolvePhase } ?: false

internal fun FirDesignation.targetContainingDeclaration(): FirDeclaration? = path.lastOrNull()
