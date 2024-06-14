/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.fir.toKaAnnotation
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaArrayAnnotationValueImpl
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEnumEntryAnnotationValueImpl
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
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import java.lang.annotation.ElementType

internal fun mapAnnotationParameters(annotation: FirAnnotation): Map<Name, FirExpression> {
    if (annotation is FirAnnotationCall && annotation.arguments.isEmpty()) return emptyMap()

    checkWithAttachment(annotation.resolved, { "By now the annotations argument mapping should have been resolved" }) {
        withFirEntry("annotation", annotation)
        withClassEntry("annotationTypeRef", annotation.annotationTypeRef)
        @OptIn(UnresolvedExpressionTypeAccess::class)
        withClassEntry("coneTypeOrNull", annotation.coneTypeOrNull)
        @OptIn(UnsafeExpressionUtility::class)
        annotation.toReferenceUnsafe()?.let { withClassEntry("calleeReference", it) }
    }

    return annotation.argumentMapping.mapping.mapKeys { (name, _) -> name }
}

internal fun annotationsByClassId(
    firSymbol: FirBasedSymbol<*>,
    classId: ClassId,
    builder: KaSymbolByFirBuilder,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): List<KaAnnotation> {
    if (firSymbol.isFromCompilerRequiredAnnotationsPhase(classId, builder.rootSession)) {
        when (classId) {
            StandardClassIds.Annotations.Target -> annotationContainer.resolvedCompilerRequiredAnnotations(firSymbol)
                .mapIndexedToAnnotationApplication(builder.rootSession, classId) { index, annotation ->
                    computeKotlinTargetAnnotation(annotation, builder, index)
                }
            JvmStandardClassIds.Annotations.Java.Target -> annotationContainer.resolvedCompilerRequiredAnnotations(firSymbol)
                .mapIndexedToAnnotationApplication(builder.rootSession, classId) { index, annotation ->
                    computeJavaTargetAnnotation(annotation, builder, index)
                }
        }
    }

    return annotationContainer.resolvedAnnotationsWithClassIds(firSymbol)
        .mapIndexedToAnnotationApplication(builder.rootSession, classId) { index, annotation ->
            annotation.toKaAnnotation(builder, index) {
                computeAnnotationArguments(firSymbol, annotationContainer, classId, index, builder)
            }
        }
}

internal fun computeAnnotationArguments(
    firSymbol: FirBasedSymbol<*>,
    annotationContainer: FirAnnotationContainer,
    classId: ClassId?,
    index: Int,
    builder: KaSymbolByFirBuilder
): List<KaNamedAnnotationValue> {
    if (firSymbol.fir.resolvePhase < FirResolvePhase.ANNOTATION_ARGUMENTS) {
        when (classId) {
            StandardClassIds.Annotations.Target -> {
                return computeKotlinTargetAnnotationArguments(annotationContainer.annotations[index], builder)
            }
            JvmStandardClassIds.Annotations.Java.Target -> {
                return computeJavaTargetAnnotationArguments(annotationContainer.annotations[index], builder)
            }
        }
    }

    val annotations = annotationContainer.resolvedAnnotationsWithArguments(firSymbol)

    return FirAnnotationValueConverter.toNamedConstantValue(
        builder.analysisSession,
        mapAnnotationParameters(annotations[index]),
        builder
    )
}

private inline fun List<FirAnnotation>.mapIndexedToAnnotationApplication(
    useSiteSession: FirSession,
    classId: ClassId,
    transformer: (index: Int, annotation: FirAnnotation) -> KaAnnotation?,
): List<KaAnnotation> = mapIndexedNotNull { index, annotation ->
    if (annotation.toAnnotationClassId(useSiteSession) != classId) {
        return@mapIndexedNotNull null
    }

    transformer(index, annotation)
}

private fun computeTargetAnnotationArguments(
    annotation: FirAnnotation,
    builder: KaSymbolByFirBuilder,
    expectedEnumClassId: ClassId,
    annotationParameterName: Name,
    nameMapper: (String) -> String?,
): List<KaNamedAnnotationValue> {
    val rawValues = annotation.findFromRawArguments(expectedEnumClass = expectedEnumClassId, nameMapper)

    if (rawValues.isNotEmpty()) {
        val token = builder.token

        val value = KaNamedAnnotationValue(
            name = annotationParameterName,
            expression = KaArrayAnnotationValueImpl(
                values = rawValues.map {
                    KaEnumEntryAnnotationValueImpl(
                        callableId = CallableId(classId = expectedEnumClassId, callableName = Name.identifier(it)),
                        sourcePsi = null,
                        token
                    )
                },
                sourcePsi = null,
                token
            ),
            token
        )

        return listOf(value)
    }

    return emptyList()
}

private fun computeKotlinTargetAnnotation(annotation: FirAnnotation, builder: KaSymbolByFirBuilder, index: Int): KaAnnotation {
    return annotation.toKaAnnotation(builder, index) {
        computeKotlinTargetAnnotationArguments(annotation, builder)
    }
}

private fun computeKotlinTargetAnnotationArguments(annotation: FirAnnotation, builder: KaSymbolByFirBuilder): List<KaNamedAnnotationValue> {
    val enumClassId = StandardClassIds.AnnotationTarget
    val parameterName = StandardClassIds.Annotations.ParameterNames.targetAllowedTargets
    return computeTargetAnnotationArguments(annotation, builder, enumClassId, parameterName) { KotlinTarget.valueOrNull(it)?.name }
}

private fun computeJavaTargetAnnotation(annotation: FirAnnotation, builder: KaSymbolByFirBuilder, index: Int): KaAnnotation {
    return annotation.toKaAnnotation(builder, index) {
        computeJavaTargetAnnotationArguments(annotation, builder)
    }
}

private fun computeJavaTargetAnnotationArguments(annotation: FirAnnotation, builder: KaSymbolByFirBuilder): List<KaNamedAnnotationValue> {
    val enumClassId = JvmStandardClassIds.Annotations.Java.ElementType
    val parameterName = StandardClassIds.Annotations.ParameterNames.value
    return computeTargetAnnotationArguments(annotation, builder, enumClassId, parameterName) {
        ElementType.entries.firstOrNull { enumValue -> enumValue.name == it }?.name
    }
}

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
            arg.unwrapAndFlattenArgument(flattenArrays = true).forEach(::addIfMatching)
        }
    }
}

internal fun annotations(
    firSymbol: FirBasedSymbol<*>,
    builder: KaSymbolByFirBuilder,
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): List<KaAnnotation> =
    annotationContainer.resolvedAnnotationsWithClassIds(firSymbol).mapIndexed { index, annotation ->
        annotation.toKaAnnotation(builder, index) { classId ->
            computeAnnotationArguments(firSymbol, annotationContainer, classId, index, builder)
        }
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
    annotationContainer: FirAnnotationContainer = firSymbol.fir,
): Boolean {
    return if (firSymbol.isFromCompilerRequiredAnnotationsPhase(classId, useSiteSession)) {
        // this loop by index is required to avoid possible ConcurrentModificationException
        val annotations = annotationContainer.resolvedCompilerRequiredAnnotations(firSymbol)
        for (index in annotations.indices) {
            val annotation = annotations[index]
            if (annotation.toAnnotationClassIdSafe(useSiteSession) == classId) {
                return true
            }
        }

        false
    } else {
        annotationContainer.resolvedAnnotationsWithClassIds(firSymbol).any {
            it.toAnnotationClassId(useSiteSession) == classId
        }
    }
}

private fun FirBasedSymbol<*>.isFromCompilerRequiredAnnotationsPhase(classId: ClassId, session: FirSession): Boolean {
    val requiredAnnotations = session.annotationPlatformSupport.requiredAnnotations
    return fir.resolvePhase < FirResolvePhase.TYPES && classId in requiredAnnotations
}
