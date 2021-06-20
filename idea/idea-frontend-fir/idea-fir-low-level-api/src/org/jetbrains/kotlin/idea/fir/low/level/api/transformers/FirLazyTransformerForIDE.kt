/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignation

internal interface FirLazyTransformerForIDE {
    fun transformDeclaration(phaseRunner: FirPhaseRunner)
    fun ensureResolved(declaration: FirDeclaration)
    fun ensureResolvedDeep(declaration: FirDeclaration) {
        if (!enableDeepEnsure) return
        ensureResolved(declaration)
        if (declaration is FirRegularClass) {
            declaration.declarations.forEach(::ensureResolvedDeep)
        }
    }

    companion object {
        internal var enableDeepEnsure: Boolean = false
            @TestOnly set

        private object ResolvePhaseWithForAllDeclarationsKey : FirDeclarationDataKey()

        private var FirDeclaration.resolvePhaseForDeclarationAndChildrenAttr: FirResolvePhase?
                by FirDeclarationDataRegistry.data(ResolvePhaseWithForAllDeclarationsKey)

        /**
         * This resolve phase is used to check if current declaration and it's children were resolved for phase
         */
        var FirDeclaration.resolvePhaseForDeclarationAndChildren: FirResolvePhase
            get() = resolvePhaseForDeclarationAndChildrenAttr ?: FirResolvePhase.RAW_FIR
            set(value) {
                resolvePhaseForDeclarationAndChildrenAttr = value
            }

        fun FirDeclaration.updateResolvedPhaseForDeclarationAndChildren(phase: FirResolvePhase) {
            val allDeclaration = resolvePhaseForDeclarationAndChildren
            if (allDeclaration < phase) {
                resolvePhaseForDeclarationAndChildren = phase
            }
        }

        fun FirDeclarationDesignation.resolvePhaseForAllDeclarations(includeDeclarationPhase: Boolean): FirResolvePhase {
            //resolvePhaseWithForAllDeclarations for these origins are derived from original declaration
            val includeTarget = when (declaration.origin) {
                is FirDeclarationOrigin.SubstitutionOverride,
                is FirDeclarationOrigin.IntersectionOverride,
                is FirDeclarationOrigin.Delegated -> false
                else -> true
            }

            val allContaining = toSequence(includeTarget = includeTarget)
                .maxByOrNull { it.resolvePhaseForDeclarationAndChildren }
                ?.resolvePhaseForDeclarationAndChildren
                ?: FirResolvePhase.RAW_FIR
            return if (includeDeclarationPhase) minOf(declaration.resolvePhase, allContaining) else allContaining
        }

        fun FirDeclarationDesignation.isResolvedForAllDeclarations(phase: FirResolvePhase, includeDeclarationPhase: Boolean) =
            resolvePhaseForAllDeclarations(includeDeclarationPhase) >= phase

        val DUMMY = object : FirLazyTransformerForIDE {
            override fun transformDeclaration(phaseRunner: FirPhaseRunner) = Unit
            override fun ensureResolved(declaration: FirDeclaration) = error("Not implemented")
        }
    }
}