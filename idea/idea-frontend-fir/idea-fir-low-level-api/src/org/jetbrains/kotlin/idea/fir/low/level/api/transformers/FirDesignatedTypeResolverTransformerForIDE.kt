/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.updatePhaseDeep
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhase

/**
 * Transform designation into TYPES phase. Affects only for designation, target declaration and it's children
 */
internal class FirDesignatedTypeResolverTransformerForIDE(
    private val designation: FirDeclarationDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : FirLazyTransformerForIDE, FirTypeResolveTransformer(session, scopeSession) {

    private val declarationTransformer = IDEDeclarationTransformer(designation)

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return if (element is FirDeclaration && (element is FirRegularClass || element is FirFile)) {
            @Suppress("UNCHECKED_CAST")
            declarationTransformer.transformDeclarationContent(this, element, data) {
                super.transformElement(element, data)
            } as E
        } else {
            super.transformElement(element, data)
        }
    }

    override fun transformDeclaration(phaseRunner: FirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.TYPES) return
        designation.declaration.ensurePhase(FirResolvePhase.SUPER_TYPES)

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.TYPES) {
            designation.firFile.transform<FirFile, Any?>(this, null)
        }

        declarationTransformer.ensureDesignationPassed()
        updatePhaseDeep(designation.declaration, FirResolvePhase.TYPES)

        ensureResolved(designation.declaration)
        ensureResolvedDeep(designation.declaration)
    }

    override fun ensureResolved(declaration: FirDeclaration) {
        if (declaration !is FirAnonymousInitializer) {
            declaration.ensurePhase(FirResolvePhase.TYPES)
        }
        when (declaration) {
            is FirFunction -> {
                check(declaration.returnTypeRef is FirResolvedTypeRef || declaration.returnTypeRef is FirImplicitTypeRef)
                check(declaration.receiverTypeRef?.let { it is FirResolvedTypeRef } ?: true)
                declaration.valueParameters.forEach {
                    check(it.returnTypeRef is FirResolvedTypeRef || it.returnTypeRef is FirImplicitTypeRef)
                }
            }
            is FirProperty -> {
                check(declaration.returnTypeRef is FirResolvedTypeRef || declaration.returnTypeRef is FirImplicitTypeRef)
                check(declaration.receiverTypeRef?.let { it is FirResolvedTypeRef } ?: true)
                declaration.getter?.run(::ensureResolved)
                declaration.setter?.run(::ensureResolved)
            }
            is FirField -> check(declaration.returnTypeRef is FirResolvedTypeRef || declaration.returnTypeRef is FirImplicitTypeRef)
            is FirClass, is FirTypeAlias, is FirAnonymousInitializer -> Unit
            is FirEnumEntry -> check(declaration.returnTypeRef is FirResolvedTypeRef)
            else -> error("Unexpected type: ${declaration::class.simpleName}")
        }
    }
}
