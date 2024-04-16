/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightReferenceListBuilder
import org.jetbrains.kotlin.analysis.api.KtAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.types.KtFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.asJava.classes.annotateByTypeAnnotationProvider
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.getContainingSymbolsWithSelf
import org.jetbrains.kotlin.light.classes.symbol.getTypeNullability
import org.jetbrains.kotlin.light.classes.symbol.asAnnotationQualifier
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_OVERLOADS_CLASS_ID
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

/**
 * @return [AnnotationUseSiteTargetFilter] which allows [this] and [NoAnnotationUseSiteTargetFilter] filter
 */
internal fun AnnotationUseSiteTarget?.toOptionalFilter(): AnnotationUseSiteTargetFilter {
    if (this == null) return NoAnnotationUseSiteTargetFilter

    return annotationUseSiteTargetFilterOf(NoAnnotationUseSiteTargetFilter, toFilter())
}

internal fun annotationUseSiteTargetFilterOf(
    vararg filters: AnnotationUseSiteTargetFilter,
): AnnotationUseSiteTargetFilter = AnnotationUseSiteTargetFilter { useSiteTarget ->
    filters.any { filter -> filter.isAllowed(useSiteTarget) }
}

internal fun KtAnnotatedSymbol.hasJvmSyntheticAnnotation(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean {
    if (this is KtPropertySymbol) return backingFieldSymbol?.hasJvmSyntheticAnnotation(useSiteTargetFilter) == true
    return hasAnnotation(JVM_SYNTHETIC_ANNOTATION_CLASS_ID, useSiteTargetFilter)
}

internal fun KtAnnotatedSymbol.getJvmNameFromAnnotation(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): String? {
    val annotation = findAnnotation(JvmStandardClassIds.Annotations.JvmName, useSiteTargetFilter)
    return annotation?.let {
        (it.arguments.firstOrNull()?.expression as? KtConstantAnnotationValue)?.constantValue?.value as? String
    }
}

context(KtAnalysisSession)
internal fun isHiddenByDeprecation(
    symbol: KtAnnotatedSymbol,
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
): Boolean = symbol.getDeprecationStatus(annotationUseSiteTarget)?.deprecationLevel == DeprecationLevelValue.HIDDEN

context(KtAnalysisSession)
internal fun KtAnnotatedSymbol.isHiddenOrSynthetic(
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = annotationUseSiteTarget.toFilter(),
) = isHiddenByDeprecation(this, annotationUseSiteTarget) || hasJvmSyntheticAnnotation(useSiteTargetFilter)

internal fun KtAnnotatedSymbol.hasJvmFieldAnnotation(): Boolean = hasAnnotation(JvmStandardClassIds.Annotations.JvmField)

internal fun KtAnnotatedSymbol.hasPublishedApiAnnotation(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean = hasAnnotation(StandardClassIds.Annotations.PublishedApi, useSiteTargetFilter)

internal fun KtAnnotatedSymbol.hasDeprecatedAnnotation(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean = hasAnnotation(StandardClassIds.Annotations.Deprecated, useSiteTargetFilter)

internal fun KtAnnotatedSymbol.hasJvmOverloadsAnnotation(): Boolean = hasAnnotation(JVM_OVERLOADS_CLASS_ID)

internal fun KtAnnotatedSymbol.hasJvmNameAnnotation(): Boolean = hasAnnotation(JvmStandardClassIds.Annotations.JvmName)

internal fun KtAnnotatedSymbol.hasJvmStaticAnnotation(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean = hasAnnotation(JvmStandardClassIds.Annotations.JvmStatic, useSiteTargetFilter)

internal fun KtAnnotatedSymbol.hasInlineOnlyAnnotation(): Boolean = hasAnnotation(StandardClassIds.Annotations.InlineOnly)

context(KtAnalysisSession)
internal fun KtDeclarationSymbol.suppressWildcardMode(
    declarationFilter: (KtDeclarationSymbol) -> Boolean = { true },
): Boolean? {
    return getContainingSymbolsWithSelf().firstNotNullOfOrNull { symbol ->
        symbol.takeIf(declarationFilter)?.suppressWildcard()
    }
}

internal fun KtAnnotatedSymbol.suppressWildcard(): Boolean? {
    if (hasJvmWildcardAnnotation()) return true
    return getJvmSuppressWildcardsFromAnnotation()
}

internal fun KtAnnotatedSymbol.getJvmSuppressWildcardsFromAnnotation(): Boolean? {
    return annotationsByClassId(JvmStandardClassIds.Annotations.JvmSuppressWildcards).firstOrNull()?.let { annoApp ->
        (annoApp.arguments.firstOrNull()?.expression as? KtConstantAnnotationValue)?.constantValue?.value as? Boolean
    }
}

internal fun KtAnnotatedSymbol.hasJvmWildcardAnnotation(): Boolean = hasAnnotation(JvmStandardClassIds.Annotations.JvmWildcard)

internal fun KtAnnotatedSymbol.findAnnotation(
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): KtAnnotationApplicationWithArgumentsInfo? {
    if (!hasAnnotation(classId, useSiteTargetFilter)) return null

    return annotationsByClassId(classId, useSiteTargetFilter).firstOrNull()
}

context(KtAnalysisSession)
internal fun KtAnnotatedSymbol.computeThrowsList(
    builder: LightReferenceListBuilder,
    useSitePosition: PsiElement,
    containingClass: SymbolLightClassBase,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
) {
    if (containingClass.isEnum && this is KtFunctionSymbol && name == StandardNames.ENUM_VALUE_OF && isStatic) {
        builder.addReference(java.lang.IllegalArgumentException::class.qualifiedName)
        builder.addReference(java.lang.NullPointerException::class.qualifiedName)
    }

    val annoApp = findAnnotation(JvmStandardClassIds.Annotations.Throws, useSiteTargetFilter) ?: return

    fun handleAnnotationValue(annotationValue: KtAnnotationValue) {
        when (annotationValue) {
            is KtArrayAnnotationValue -> {
                annotationValue.values.forEach(::handleAnnotationValue)
            }

            is KtKClassAnnotationValue -> {
                if (annotationValue.type is KtNonErrorClassType) {
                    val psiType = annotationValue.type.asPsiType(
                        useSitePosition,
                        allowErrorTypes = true,
                        KtTypeMappingMode.DEFAULT,
                        containingClass.isAnnotationType,
                    )
                    (psiType as? PsiClassType)?.let {
                        builder.addReference(it)
                    }
                }
            }

            else -> {}
        }
    }

    annoApp.arguments.forEach { handleAnnotationValue(it.expression) }
}

context(KtAnalysisSession)
@KtAnalysisNonPublicApi
fun annotateByKtType(
    psiType: PsiType,
    ktType: KtType,
    annotationParent: PsiElement,
): PsiType {
    fun getAnnotationsSequence(type: KtType): Sequence<List<PsiAnnotation>> = sequence {
        val unwrappedType = when (type) {
            // We assume that flexible types have to have the same set of annotations on upper and lower bound.
            // Also, the upper bound is more similar to the resulting PsiType as it has fewer restrictions.
            is KtFlexibleType -> type.upperBound
            else -> type
        }

        val explicitTypeAnnotations = unwrappedType.annotations.map { annotationApplication ->
            SymbolLightSimpleAnnotation(
                annotationApplication.classId?.asFqNameString(),
                annotationParent,
                annotationApplication.arguments.map { it.toLightClassAnnotationArgument() },
                annotationApplication.psi,
            )
        }

        // Original type should be used to infer nullability
        val typeNullability = when {
            psiType !is PsiPrimitiveType && type.isPrimitiveBacked -> KtTypeNullability.NON_NULLABLE
            else -> getTypeNullability(type)
        }

        val nullabilityAnnotation = typeNullability.asAnnotationQualifier?.let {
            SymbolLightSimpleAnnotation(it, annotationParent)
        }

        yield(explicitTypeAnnotations + listOfNotNull(nullabilityAnnotation))

        if (unwrappedType is KtNonErrorClassType) {
            unwrappedType.ownTypeArguments.forEach { typeProjection ->
                typeProjection.type?.let {
                    yieldAll(getAnnotationsSequence(it))
                }
            }
        }
    }

    return psiType.annotateByTypeAnnotationProvider(getAnnotationsSequence(ktType))
}
