/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.ResolveTreeBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ensurePhase
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsResolveTransformer
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

internal class LLFirDesignatedAnnotationArgumentsResolveTransformer(
    private val designation: FirDeclarationDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirLazyTransformer, FirAnnotationArgumentsResolveTransformer(session, scopeSession, FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) {

    private fun moveNextDeclaration(designationIterator: Iterator<FirDeclaration>) {
        if (!designationIterator.hasNext()) {
            designation.declaration.transform<FirDeclaration, ResolutionMode>(declarationsTransformer, ResolutionMode.ContextIndependent)
            return
        }
        when (val nextElement = designationIterator.next()) {
            is FirFile -> {
                context.withFile(nextElement, components) {
                    moveNextDeclaration(designationIterator)
                }
            }
            is FirRegularClass -> {
                moveNextDeclaration(designationIterator)
            }
            is FirEnumEntry -> {
                context.forEnumEntry {
                    moveNextDeclaration(designationIterator)
                }
            }
            else -> {
                error("Unexpected declaration in designation: ${nextElement::class.qualifiedName}")
            }
        }
    }

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) return
        designation.declaration.ensurePhase(FirResolvePhase.STATUS)

        val designationIterator = designation.toSequenceWithFile(includeTarget = false).iterator()

        ResolveTreeBuilder.resolvePhase(designation.declaration, FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) {
            phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) {
                moveNextDeclaration(designationIterator)
            }
        }

        LLFirLazyTransformer.updatePhaseDeep(designation.declaration, FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)
        ensureResolved(designation.declaration)
        ensureResolvedDeep(designation.declaration)
    }

    override fun ensureResolved(declaration: FirDeclaration) {
        val unresolvedAnnotation = declaration.annotations.firstOrNull { it.annotationTypeRef !is FirResolvedTypeRef }
        check(unresolvedAnnotation == null) {
            "Unexpected annotationTypeRef annotation, expected resolvedType but actual ${unresolvedAnnotation?.annotationTypeRef}"
        }
        when (declaration) {
            is FirSimpleFunction, is FirConstructor, is FirAnonymousInitializer ->
                declaration.ensurePhase(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)
            is FirProperty -> {
                declaration.ensurePhase(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)
//                declaration.getter?.ensurePhase(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)
//                declaration.setter?.ensurePhase(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)
            }
            is FirClass, is FirTypeAlias, is FirEnumEntry, is FirField -> Unit
            else -> error("Unexpected type: ${declaration::class.simpleName}")
        }
    }

}
