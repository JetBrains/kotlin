/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.fir.toKaAnnotation
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaArrayAnnotationValueImpl
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEnumEntryAnnotationValueImpl
import org.jetbrains.kotlin.analysis.api.utils.errors.withClassEntry
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationsWithClassIds
import org.jetbrains.kotlin.fir.symbols.resolvedCompilerRequiredAnnotations
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.name.ClassId
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
    val session = builder.rootSession
    if (firSymbol.isFromCompilerRequiredAnnotationsPhase(classId, builder.rootSession)) {
        // This is safe to iterate over the collection without indices since all annotations after 582b640b commit
        // declared as `MutableOrEmptyList<FirAnnotation>`, so:
        // - `replaceAnnotations` replaces the entire collection without modifications
        // - `transformAnnotations` theoretically may modify annotations, but it is not allowed due
        // to the compiler contract to change already published annotations – only their content can be changed
        return annotationContainer.resolvedCompilerRequiredAnnotations(firSymbol).mapNotNull { annotation ->
            if (annotation.toAnnotationClassIdSafe(session) != classId) {
                return@mapNotNull null
            }

            annotation.toKaAnnotation(builder)
        }
    }

    return annotationContainer.resolvedAnnotationsWithClassIds(firSymbol).mapNotNull { annotation ->
        if (annotation.toAnnotationClassId(session) != classId) {
            return@mapNotNull null
        }

        annotation.toKaAnnotation(builder)
    }
}

internal fun computeAnnotationArguments(annotation: FirAnnotation, builder: KaSymbolByFirBuilder): List<KaNamedAnnotationValue> {
    if (annotation is FirAnnotationCall) {
        if (annotation.arguments.isEmpty()) return emptyList()

        val symbol = annotation.containingDeclarationSymbol
        if (symbol.fir.resolvePhase < FirResolvePhase.ANNOTATION_ARGUMENTS) {
            when (annotation.toAnnotationClassId(builder.rootSession)) {
                StandardClassIds.Annotations.Target -> return computeKotlinTargetAnnotationArguments(annotation, builder)
                JvmStandardClassIds.Annotations.Java.Target -> return computeJavaTargetAnnotationArguments(annotation, builder)
            }
        }

        symbol.lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
    }

    return FirAnnotationValueConverter.toNamedConstantValue(
        builder.analysisSession,
        mapAnnotationParameters(annotation),
        builder,
    )
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

        val value = KaBaseNamedAnnotationValue(
            annotationParameterName,
            KaArrayAnnotationValueImpl(
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
        )

        return listOf(value)
    }

    return emptyList()
}

private fun computeKotlinTargetAnnotationArguments(annotation: FirAnnotation, builder: KaSymbolByFirBuilder): List<KaNamedAnnotationValue> {
    val enumClassId = StandardClassIds.AnnotationTarget
    val parameterName = StandardClassIds.Annotations.ParameterNames.targetAllowedTargets
    return computeTargetAnnotationArguments(annotation, builder, enumClassId, parameterName) { KotlinTarget.valueOrNull(it)?.name }
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
): List<KaAnnotation> = annotationContainer.resolvedAnnotationsWithClassIds(firSymbol).map { annotation ->
    annotation.toKaAnnotation(builder)
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
): Boolean = if (firSymbol.isFromCompilerRequiredAnnotationsPhase(classId, useSiteSession)) {
    // This is safe to iterate over the collection without indices since all annotations after 582b640b commit
    // declared as `MutableOrEmptyList<FirAnnotation>`, so:
    // - `replaceAnnotations` replaces the entire collection without modifications
    // - `transformAnnotations` theoretically may modify annotations, but it is not allowed due
    // to the compiler contract to change already published annotations – only their content can be changed
    annotationContainer.resolvedCompilerRequiredAnnotations(firSymbol).any {
        it.toAnnotationClassIdSafe(useSiteSession) == classId
    }
} else {
    annotationContainer.resolvedAnnotationsWithClassIds(firSymbol).any {
        it.toAnnotationClassId(useSiteSession) == classId
    }
}

private fun FirBasedSymbol<*>.isFromCompilerRequiredAnnotationsPhase(classId: ClassId, session: FirSession): Boolean {
    val requiredAnnotations = session.annotationPlatformSupport.requiredAnnotations
    return fir.resolvePhase < FirResolvePhase.TYPES && classId in requiredAnnotations
}
