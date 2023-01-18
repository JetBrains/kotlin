/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.analysis.utils.errors.withClassEntry
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal fun mapAnnotationParameters(annotation: FirAnnotation): Map<Name, FirExpression> {
    checkWithAttachmentBuilder(annotation.resolved, { "By now the annotations argument mapping should have been resolved" }) {
        withFirEntry("annotation", annotation)
        withClassEntry("annotationTypeRef", annotation.annotationTypeRef)
        withClassEntry("typeRef", annotation.typeRef)
    }

    return annotation.argumentMapping.mapping.mapKeys { (name, _) -> name }
}

internal fun annotationsByClassId(
    firSymbol: FirBasedSymbol<*>,
    classId: ClassId,
    useSiteSession: FirSession,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): List<KtAnnotationApplication> =
    if (firSymbol.isFromCompilerRequiredAnnotationsPhase(classId)) {
        annotationContainer.resolvedCompilerRequiredAnnotations(firSymbol).mapNotNull { annotation ->
            if (annotation.toAnnotationClassIdSafe(useSiteSession) != classId) return@mapNotNull null
            annotation.toKtAnnotationApplication(useSiteSession)
        }
    } else {
        annotationContainer.resolvedAnnotationsWithArguments(firSymbol).mapNotNull { annotation ->
            if (annotation.toAnnotationClassId(useSiteSession) != classId) return@mapNotNull null
            annotation.toKtAnnotationApplication(useSiteSession)
        }
    }

internal fun annotations(
    firSymbol: FirBasedSymbol<*>,
    useSiteSession: FirSession,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): List<KtAnnotationApplication> = annotationContainer.resolvedAnnotationsWithArguments(firSymbol).map { annotation ->
    annotation.toKtAnnotationApplication(useSiteSession)
}

internal fun annotationClassIds(
    firSymbol: FirBasedSymbol<*>,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): Collection<ClassId> = annotationContainer.resolvedAnnotationClassIds(firSymbol)

internal fun hasAnnotation(
    firSymbol: FirBasedSymbol<*>,
    classId: ClassId,
    useSiteSession: FirSession,
    useSiteTarget: AnnotationUseSiteTarget?,
    acceptAnnotationsWithoutUseSite: Boolean,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): Boolean =
    if (firSymbol.isFromCompilerRequiredAnnotationsPhase(classId)) {
        annotationContainer.resolvedCompilerRequiredAnnotations(firSymbol).any {
            (it.useSiteTarget == useSiteTarget || acceptAnnotationsWithoutUseSite && it.useSiteTarget == null) &&
                    it.toAnnotationClassIdSafe(useSiteSession) == classId
        }
    } else {
        annotationContainer.resolvedAnnotationsWithClassIds(firSymbol).any {
            (it.useSiteTarget == useSiteTarget || acceptAnnotationsWithoutUseSite && it.useSiteTarget == null) &&
                    it.toAnnotationClassId(useSiteSession) == classId
        }
    }

internal fun hasAnnotation(
    firSymbol: FirBasedSymbol<*>,
    classId: ClassId,
    useSiteSession: FirSession,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): Boolean =
    if (firSymbol.isFromCompilerRequiredAnnotationsPhase(classId)) {
        annotationContainer.resolvedCompilerRequiredAnnotations(firSymbol).hasAnnotationSafe(classId, useSiteSession)
    } else {
        annotationContainer.resolvedAnnotationsWithClassIds(firSymbol).hasAnnotation(classId, useSiteSession)
    }

private fun FirBasedSymbol<*>.isFromCompilerRequiredAnnotationsPhase(classId: ClassId): Boolean =
    fir.resolvePhase < FirResolvePhase.TYPES && classId in CompilerRequiredAnnotationsHelper.REQUIRED_ANNOTATIONS