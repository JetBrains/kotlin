/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompilerRequiredAnnotationsResolveTransformer

internal class LLFirDesignatedAnnotationsResolveTransformed(
    private val designation: FirDeclarationDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirLazyTransformer, FirCompilerRequiredAnnotationsResolveTransformer(session, scopeSession) {

    private fun moveNextDeclaration(designationIterator: Iterator<FirDeclaration>) {
        if (!designationIterator.hasNext()) {
            val declaration = designation.declaration
            if (declaration is FirRegularClass || declaration is FirTypeAlias) {
                declaration.transform<FirDeclaration, Mode>(this, Mode.RegularAnnotations)
            }
            return
        }
        when (val nextElement = designationIterator.next()) {
            is FirFile -> {
                withFileAndScopes(nextElement) {
                    moveNextDeclaration(designationIterator)
                }
            }
            is FirRegularClass -> {
                moveNextDeclaration(designationIterator)
            }
            else -> {
                error("Unexpected declaration in designation: ${nextElement::class.qualifiedName}")
            }
        }
    }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) return

        val designationIterator = designation.toSequenceWithFile(includeTarget = false).iterator()

        ResolveTreeBuilder.resolvePhase(designation.declaration, FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {
            phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {
                moveNextDeclaration(designationIterator)
            }
        }

        LLFirLazyTransformer.updatePhaseDeep(designation.declaration, FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
        checkIsResolved(designation.declaration)
    }

    override fun checkIsResolved(declaration: FirDeclaration) {
        declaration.checkPhase(FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS)
        // todo add proper check that COMPILER_REQUIRED_ANNOTATIONS are resolved
//        checkNestedDeclarationsAreResolved(declaration)
    }

}
