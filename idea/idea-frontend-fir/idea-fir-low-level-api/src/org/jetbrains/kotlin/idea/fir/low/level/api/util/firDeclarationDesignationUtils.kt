/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolved
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation

internal fun FirDeclaration.ensurePhase(firResolvePhase: FirResolvePhase) =
    check(resolvePhase >= firResolvePhase) {
        "Element phase required to be $firResolvePhase but element resolved to $resolvePhase"
    }

internal fun FirDeclarationDesignation.ensurePathPhase(firResolvePhase: FirResolvePhase) =
    path.forEach { it.ensurePhase(firResolvePhase) }

internal fun FirDeclarationDesignation.ensureDesignation(firResolvePhase: FirResolvePhase) {
    ensurePathPhase(firResolvePhase)
    declaration.ensurePhase(firResolvePhase)
}
internal fun FirDeclarationDesignation.ensurePhaseForClasses(firResolvePhase: FirResolvePhase) {
    ensurePathPhase(firResolvePhase)
    if (declaration is FirClassLikeDeclaration<*>) {
        check(declaration.resolvePhase >= firResolvePhase) {
            "Expected $firResolvePhase but found ${declaration.resolvePhase}"
        }
    }
}

internal fun FirDeclarationDesignation.isTargetCallableDeclarationAndInPhase(firResolvePhase: FirResolvePhase): Boolean =
    (declaration as? FirCallableDeclaration<*>)?.let { it.resolvePhase >= firResolvePhase } ?: false

internal fun FirDeclarationDesignation.targetContainingDeclaration(): FirDeclaration? = path.lastOrNull()