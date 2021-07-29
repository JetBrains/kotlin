/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolveStatus
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsResolveTransformer
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirDeclarationDesignationWithFile
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ensurePhase

internal class FirDesignatedAnnotationArgumentsResolveTransformerForIDE(
    private val designation: FirDeclarationDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : FirLazyTransformerForIDE, FirAnnotationArgumentsResolveTransformer(session, scopeSession) {

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
                context.withContainingClass(nextElement) {
                    moveNextDeclaration(designationIterator)
                }
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

    override fun transformDeclaration(phaseRunner: FirPhaseRunner) {
        if (designation.declaration.resolvePhase >= FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) return
        designation.declaration.ensurePhase(FirResolvePhase.TYPES)

        val designationIterator = designation.toSequenceWithFile(includeTarget = false).iterator()

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) {
            moveNextDeclaration(designationIterator)
        }

        FirLazyTransformerForIDE.updatePhaseDeep(designation.declaration, FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)
        ensureResolved(designation.declaration)
        ensureResolvedDeep(designation.declaration)
    }

    override fun ensureResolved(declaration: FirDeclaration) {
        if (declaration is FirAnnotatedDeclaration) {
            val unresolvedAnnotation = declaration.annotations.firstOrNull { it.resolveStatus == FirAnnotationResolveStatus.Resolved }
            check(unresolvedAnnotation == null) {
                "Unexpected resolve status of annotation, expected Resolved but actual $unresolvedAnnotation"
            }
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