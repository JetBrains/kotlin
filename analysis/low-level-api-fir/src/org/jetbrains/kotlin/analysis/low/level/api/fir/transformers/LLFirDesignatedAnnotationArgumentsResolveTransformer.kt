/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.transformers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignationWithFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkTypeRefIsResolved
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsResolveTransformer
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

internal class LLFirDesignatedAnnotationArgumentsResolveTransformer(
    private val designation: FirDesignationWithFile,
    session: FirSession,
    scopeSession: ScopeSession,
) : LLFirLazyTransformer, FirAnnotationArgumentsResolveTransformer(session, scopeSession, FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) {

    private fun moveNextDeclaration(designationIterator: Iterator<FirElementWithResolveState>) {
        if (!designationIterator.hasNext()) {
            FirLazyBodiesCalculator.calculateAnnotations(designation.target)
            designation.target.transform<FirDeclaration, ResolutionMode>(declarationsTransformer, ResolutionMode.ContextIndependent)
            return
        }
        when (val nextElement = designationIterator.next()) {
            is FirFile -> {
                context.withFile(nextElement, components) {
                    moveNextDeclaration(designationIterator)
                }
            }
            is FirRegularClass -> {
                context.withRegularClass(nextElement, components) {
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

    override fun transformDeclaration(phaseRunner: LLFirPhaseRunner) {
        if (designation.target.resolvePhase >= FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) return
        designation.target.checkPhase(FirResolvePhase.STATUS)

        val designationIterator = designation.toSequenceWithFile(includeTarget = false).iterator()

        phaseRunner.runPhaseWithCustomResolve(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS) {
            moveNextDeclaration(designationIterator)
        }


        LLFirLazyTransformer.updatePhaseDeep(designation.target, FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)
        checkIsResolved(designation.target)
    }

    override fun checkIsResolved(target: FirElementWithResolveState) {
        if (target !is FirAnnotationContainer) return
        val unresolvedAnnotation = target.annotations.firstOrNull { it.annotationTypeRef !is FirResolvedTypeRef }
        check(unresolvedAnnotation == null) {
            "Unexpected annotationTypeRef annotation, expected resolvedType but actual ${unresolvedAnnotation?.annotationTypeRef}"
        }
        target.checkPhase(FirResolvePhase.ARGUMENTS_OF_ANNOTATIONS)

        for (annotation in target.annotations) {
            for (argument in annotation.argumentMapping.mapping.values) {
                checkTypeRefIsResolved(argument.typeRef, "annotation argument", target) {
                    withFirEntry("firAnnotation", annotation)
                    withFirEntry("firArgument", argument)
                }
            }
        }
        checkNestedDeclarationsAreResolved(target)
    }

}
