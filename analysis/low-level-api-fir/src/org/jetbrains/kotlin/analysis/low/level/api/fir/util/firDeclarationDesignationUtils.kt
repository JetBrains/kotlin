/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignation
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase

internal fun FirDeclaration.checkPhase(requiredResolvePhase: FirResolvePhase) {
    val declarationResolvePhase = resolvePhase
    checkWithAttachmentBuilder(
        declarationResolvePhase >= requiredResolvePhase,
        { "At least $requiredResolvePhase expected but $declarationResolvePhase found for ${this::class.simpleName}" }
    ) {
        withFirEntry("firDeclaration", this@checkPhase)
    }
}

internal fun FirDeclarationDesignation.checkPathPhase(firResolvePhase: FirResolvePhase) =
    path.forEach { it.checkPhase(firResolvePhase) }

internal fun FirDeclarationDesignation.checkDesignationPhase(firResolvePhase: FirResolvePhase) {
    checkPathPhase(firResolvePhase)
    declaration.checkPhase(firResolvePhase)
}

internal fun FirDeclarationDesignation.checkDesignationPhaseForClasses(firResolvePhase: FirResolvePhase) {
    checkPathPhase(firResolvePhase)
    if (declaration is FirClassLikeDeclaration) {
        check(declaration.resolvePhase >= firResolvePhase) {
            "Expected $firResolvePhase but found ${declaration.resolvePhase}"
        }
    }
}

internal fun FirDeclarationDesignation.isTargetCallableDeclarationAndInPhase(firResolvePhase: FirResolvePhase): Boolean =
    (declaration as? FirCallableDeclaration)?.let { it.resolvePhase >= firResolvePhase } ?: false

internal fun FirDeclarationDesignation.targetContainingDeclaration(): FirDeclaration? = path.lastOrNull()
