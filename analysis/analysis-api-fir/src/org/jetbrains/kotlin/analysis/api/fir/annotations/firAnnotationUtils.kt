/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationInfo
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.withFirEntry
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.analysis.utils.errors.withClassEntry
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolved
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
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
import org.jetbrains.kotlin.fir.declarations.resolvePhase

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
    useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    useSiteSession: FirSession,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): List<KtAnnotationApplicationWithArgumentsInfo> {
    return annotationContainer.resolvedAnnotationsWithArguments(firSymbol).mapIndexedNotNull { index, annotation ->
        if (!useSiteTargetFilter.isAllowed(annotation.useSiteTarget) || annotation.toAnnotationClassId(useSiteSession) != classId) {
            return@mapIndexedNotNull null
        }

        annotation.toKtAnnotationApplication(useSiteSession, index)
    }
}

internal fun annotations(
    firSymbol: FirBasedSymbol<*>,
    useSiteSession: FirSession,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): List<KtAnnotationApplicationWithArgumentsInfo> =
    annotationContainer.resolvedAnnotationsWithArguments(firSymbol).mapIndexed { index, annotation ->
        annotation.toKtAnnotationApplication(useSiteSession, index)
    }

internal fun annotationInfos(
    firSymbol: FirBasedSymbol<*>,
    useSiteSession: FirSession,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): List<KtAnnotationApplicationInfo> = annotationContainer.resolvedAnnotationsWithClassIds(firSymbol).mapIndexed { index, annotation ->
    annotation.toKtAnnotationInfo(useSiteSession, index)
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
    useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    useSiteSession: FirSession,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): Boolean {
    return if (firSymbol.isFromCompilerRequiredAnnotationsPhase(classId)) {
        // this loop by index is required to avoid possible ConcurrentModificationException
        val annotations = annotationContainer.resolvedCompilerRequiredAnnotations(firSymbol)
        for (index in annotations.indices) {
            val annotation = annotations[index]
            if (useSiteTargetFilter.isAllowed(annotation.useSiteTarget) && annotation.toAnnotationClassIdSafe(useSiteSession) == classId) {
                return true
            }
        }

        false
    } else {
        annotationContainer.resolvedAnnotationsWithClassIds(firSymbol).any {
            useSiteTargetFilter.isAllowed(it.useSiteTarget) && it.toAnnotationClassId(useSiteSession) == classId
        }
    }
}

private fun FirBasedSymbol<*>.isFromCompilerRequiredAnnotationsPhase(classId: ClassId): Boolean =
    fir.resolvePhase < FirResolvePhase.TYPES && classId in CompilerRequiredAnnotationsHelper.REQUIRED_ANNOTATIONS