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
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationUntypedDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.isResolvedForAllDeclarations
import org.jetbrains.kotlin.idea.fir.low.level.api.transformers.FirLazyTransformerForIDE.Companion.updateResolvedPhaseForDeclarationAndChildren
import org.jetbrains.kotlin.idea.fir.low.level.api.util.*
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhaseForClasses

/**
 * Transform designation into TYPES phase. Affects only for designation, target declaration and it's children
 */
internal class FirDesignatedTypeResolverTransformerForIDE(
    private val designation: FirDeclarationUntypedDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
    private val declarationPhaseDowngraded: Boolean,
) : FirLazyTransformerForIDE, FirTypeResolveTransformer(session, scopeSession) {

    private val declarationTransformer = IDEDeclarationTransformer(designation)

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return if (element is FirDeclaration && (element is FirRegularClass || element is FirFile)) {
            declarationTransformer.transformDeclarationContent(this, element, data) {
                super.transformElement(element, data)
            }
        } else {
            super.transformElement(element, data)
        }
    }

    override fun transformDeclaration(phaseRunner: FirPhaseRunner) {
        if (designation.isResolvedForAllDeclarations(FirResolvePhase.TYPES, declarationPhaseDowngraded)) return
        designation.declaration.updateResolvedPhaseForDeclarationAndChildren(FirResolvePhase.TYPES)
        designation.ensurePhaseForClasses(FirResolvePhase.SUPER_TYPES)

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.TYPES) {
            designation.firFile.transform<FirFile, Any?>(this, null)
        }

        declarationTransformer.ensureDesignationPassed()

        designation.path.forEach(::ensureResolved)
        ensureResolved(designation.declaration)
        ensureResolvedDeep(designation.declaration)
    }

    override fun ensureResolved(declaration: FirDeclaration) {
        if (declaration !is FirAnonymousInitializer) {
            declaration.ensurePhase(FirResolvePhase.TYPES)
        }
        when (declaration) {
            is FirFunction<*> -> {
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
            is FirClass<*>, is FirTypeAlias, is FirAnonymousInitializer -> Unit
            is FirEnumEntry -> check(declaration.returnTypeRef is FirResolvedTypeRef)
            else -> error("Unexpected type: ${declaration::class.simpleName}")
        }
    }
}