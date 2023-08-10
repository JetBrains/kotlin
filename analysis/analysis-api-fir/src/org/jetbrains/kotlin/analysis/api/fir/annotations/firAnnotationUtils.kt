/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationInfo
import org.jetbrains.kotlin.analysis.utils.errors.withClassEntry
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationsWithArguments
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationsWithClassIds
import org.jetbrains.kotlin.fir.symbols.resolvedCompilerRequiredAnnotations
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import java.lang.annotation.ElementType

internal fun mapAnnotationParameters(annotation: FirAnnotation): Map<Name, FirExpression> {
    if (annotation is FirAnnotationCall && annotation.arguments.isEmpty()) return emptyMap()

    checkWithAttachment(annotation.resolved, { "By now the annotations argument mapping should have been resolved" }) {
        withFirEntry("annotation", annotation)
        withClassEntry("annotationTypeRef", annotation.annotationTypeRef)
        @OptIn(UnresolvedExpressionTypeAccess::class)
        withClassEntry("coneTypeOrNull", annotation.coneTypeOrNull)
        annotation.calleeReference?.let { withClassEntry("calleeReference", it) }
    }

    return annotation.argumentMapping.mapping.mapKeys { (name, _) -> name }
}

internal fun annotationsByClassId(
    firSymbol: FirBasedSymbol<*>,
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    useSiteSession: FirSession,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): List<KtAnnotationApplicationWithArgumentsInfo> =
    if (classId == StandardClassIds.Annotations.Target && firSymbol.fir.resolvePhase < FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING) {
        annotationContainer.resolvedAnnotationsWithClassIds(firSymbol)
            .mapIndexedToAnnotationApplication(useSiteTargetFilter, useSiteSession, classId) { index, annotation ->
                annotation.asKtAnnotationApplicationForTargetAnnotation(useSiteSession, index)
            }
    } else if (classId == JvmStandardClassIds.Annotations.Java.Target && firSymbol.fir.resolvePhase < FirResolvePhase.ANNOTATIONS_ARGUMENTS_MAPPING) {
        annotationContainer.resolvedAnnotationsWithClassIds(firSymbol)
            .mapIndexedToAnnotationApplication(useSiteTargetFilter, useSiteSession, classId) { index, annotation ->
                annotation.asKtAnnotationApplicationForJavaTargetAnnotation(useSiteSession, index)
            }
    } else {
        annotationContainer.resolvedAnnotationsWithArguments(firSymbol)
            .mapIndexedToAnnotationApplication(useSiteTargetFilter, useSiteSession, classId) { index, annotation ->
                annotation.toKtAnnotationApplication(useSiteSession, index)
            }
    }

private inline fun List<FirAnnotation>.mapIndexedToAnnotationApplication(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    useSiteSession: FirSession,
    classId: ClassId,
    transformer: (index: Int, annotation: FirAnnotation) -> KtAnnotationApplicationWithArgumentsInfo?,
): List<KtAnnotationApplicationWithArgumentsInfo> = mapIndexedNotNull { index, annotation ->
    if (!useSiteTargetFilter.isAllowed(annotation.useSiteTarget) || annotation.toAnnotationClassId(useSiteSession) != classId) {
        return@mapIndexedNotNull null
    }

    transformer(index, annotation)
}

private fun FirAnnotation.asKtAnnotationApplicationForAnnotationWithEnumArgument(
    useSiteSession: FirSession,
    index: Int,
    expectedEnumClassId: ClassId,
    annotationParameterName: Name,
    nameMapper: (String) -> String?,
): KtAnnotationApplicationWithArgumentsInfo {
    val arguments = findFromRawArguments(
        expectedEnumClass = expectedEnumClassId,
        nameMapper,
    ).ifNotEmpty {
        listOf(
            KtNamedAnnotationValue(
                name = annotationParameterName,
                expression = KtArrayAnnotationValue(
                    values = map {
                        KtEnumEntryAnnotationValue(
                            callableId = CallableId(
                                classId = expectedEnumClassId,
                                callableName = Name.identifier(it),
                            ),
                            sourcePsi = null,
                        )
                    },
                    sourcePsi = null,
                )
            )
        )
    }.orEmpty()

    return toKtAnnotationApplication(useSiteSession, index, arguments)
}

private fun FirAnnotation.asKtAnnotationApplicationForTargetAnnotation(
    useSiteSession: FirSession,
    index: Int,
): KtAnnotationApplicationWithArgumentsInfo = asKtAnnotationApplicationForAnnotationWithEnumArgument(
    useSiteSession = useSiteSession,
    index = index,
    expectedEnumClassId = StandardClassIds.AnnotationTarget,
    annotationParameterName = StandardClassIds.Annotations.ParameterNames.targetAllowedTargets,
    nameMapper = { KotlinTarget.valueOrNull(it)?.name },
)

private fun FirAnnotation.asKtAnnotationApplicationForJavaTargetAnnotation(
    useSiteSession: FirSession,
    index: Int,
): KtAnnotationApplicationWithArgumentsInfo = asKtAnnotationApplicationForAnnotationWithEnumArgument(
    useSiteSession = useSiteSession,
    index = index,
    expectedEnumClassId = JvmStandardClassIds.Annotations.Java.ElementType,
    annotationParameterName = StandardClassIds.Annotations.ParameterNames.value,
    nameMapper = { ElementType.values().firstOrNull { enumValue -> enumValue.name == it }?.name },
)

private fun <T> FirAnnotation.findFromRawArguments(expectedEnumClass: ClassId, transformer: (String) -> T?): Set<T> = buildSet {
    fun addIfMatching(arg: FirExpression) {
        if (arg !is FirQualifiedAccessExpression) return
        val callableSymbol = arg.calleeReference.toResolvedCallableSymbol() ?: return
        if (callableSymbol.containingClassLookupTag()?.classId != expectedEnumClass) return
        val identifier = callableSymbol.callableId.callableName.identifier
        transformer(identifier)?.let(::add)
    }

    if (this@findFromRawArguments is FirAnnotationCall) {
        for (arg in argumentList.arguments) {
            arg.unwrapAndFlattenArgument().forEach(::addIfMatching)
        }
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
    return if (firSymbol.isFromCompilerRequiredAnnotationsPhase(classId, useSiteSession)) {
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

private fun FirBasedSymbol<*>.isFromCompilerRequiredAnnotationsPhase(classId: ClassId, session: FirSession): Boolean {
    val requiredAnnotations = session.annotationPlatformSupport.requiredAnnotations
    return fir.resolvePhase < FirResolvePhase.TYPES && classId in requiredAnnotations
}