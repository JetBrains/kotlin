/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation

internal interface FirLazyTransformerForIDE {
    fun transformDeclaration()

    companion object {
        val EMPTY = object : FirLazyTransformerForIDE {
            override fun transformDeclaration() = Unit
        }

        fun FirDeclarationDesignation.ensurePathPhase(firResolvePhase: FirResolvePhase) {
            toSequence(includeTarget = false).forEach { firDeclaration ->
                check(firDeclaration.resolvePhase >= firResolvePhase) {
                    "Designation element phase required to be $firResolvePhase but element resolved to ${firDeclaration.resolvePhase}"
                }
            }
        }

        fun FirDeclarationDesignation.ensureTargetPhase(firResolvePhase: FirResolvePhase) =
            check(declaration.resolvePhase >= firResolvePhase) { "Expected $firResolvePhase but found ${declaration.resolvePhase}" }

        fun FirDeclarationDesignation.ensureTargetPhaseIfClass(firResolvePhase: FirResolvePhase) = when (declaration) {
            is FirProperty, is FirSimpleFunction -> Unit
            is FirClass<*>, is FirTypeAlias -> ensureTargetPhase(firResolvePhase)
            else -> error("Unexpected target")
        }
    }
}