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
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.CompilerRequiredAnnotationsHelper
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationsWithArguments
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationsWithClassIds
import org.jetbrains.kotlin.fir.symbols.resolvedCompilerRequiredAnnotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal fun mapAnnotationParameters(annotation: FirAnnotation): Map<Name, FirExpression> {
    if (annotation is FirAnnotationCall && annotation.arguments.isEmpty()) return emptyMap()

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
        annotationContainer.resolvedCompilerRequiredAnnotations(firSymbol).mapIndexedNotNull { index, annotation ->
            if (annotation.toAnnotationClassIdSafe(useSiteSession) != classId) return@mapIndexedNotNull null
            annotation.toKtAnnotationApplication(useSiteSession, index)
        }
    } else {
        annotationContainer.resolvedAnnotationsWithArguments(firSymbol).mapIndexedNotNull { index, annotation ->
            if (annotation.toAnnotationClassId(useSiteSession) != classId) return@mapIndexedNotNull null
            annotation.toKtAnnotationApplication(useSiteSession, index)
        }
    }

internal fun annotations(
    firSymbol: FirBasedSymbol<*>,
    useSiteSession: FirSession,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): List<KtAnnotationApplication> = annotationContainer.resolvedAnnotationsWithArguments(firSymbol).mapIndexed { index, annotation ->
    annotation.toKtAnnotationApplication(useSiteSession, index)
}

internal fun annotationClassIds(
    firSymbol: FirBasedSymbol<*>,
    useSiteSession: FirSession,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): Collection<ClassId> = annotationContainer.resolvedAnnotationsWithClassIds(firSymbol).mapNotNull {
    it.toAnnotationClassId(useSiteSession)
}

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