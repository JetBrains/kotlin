/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightReferenceListBuilder
import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.types.*
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

internal fun KaAnnotatedSymbol.hasJvmSyntheticAnnotation(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean {
    if (this is KaPropertySymbol) return backingFieldSymbol?.hasJvmSyntheticAnnotation(useSiteTargetFilter) == true
    return hasAnnotation(JVM_SYNTHETIC_ANNOTATION_CLASS_ID, useSiteTargetFilter)
}

internal fun KaAnnotatedSymbol.getJvmNameFromAnnotation(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): String? {
    val annotation = findAnnotation(JvmStandardClassIds.Annotations.JvmName, useSiteTargetFilter)
    return annotation?.let {
        (it.arguments.firstOrNull()?.expression as? KaAnnotationValue.ConstantValue)?.value?.value as? String
    }
}

context(KaSession)
internal fun isHiddenByDeprecation(
    symbol: KaAnnotatedSymbol,
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
): Boolean = symbol.deprecationStatus(annotationUseSiteTarget)?.deprecationLevel == DeprecationLevelValue.HIDDEN

context(KaSession)
internal fun KaAnnotatedSymbol.isHiddenOrSynthetic(
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = annotationUseSiteTarget.toFilter(),
) = isHiddenByDeprecation(this, annotationUseSiteTarget) || hasJvmSyntheticAnnotation(useSiteTargetFilter)

internal fun KaAnnotatedSymbol.hasJvmFieldAnnotation(): Boolean = JvmStandardClassIds.Annotations.JvmField in annotations

internal fun KaAnnotatedSymbol.hasPublishedApiAnnotation(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean = hasAnnotation(StandardClassIds.Annotations.PublishedApi, useSiteTargetFilter)

internal fun KaAnnotatedSymbol.hasDeprecatedAnnotation(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean = hasAnnotation(StandardClassIds.Annotations.Deprecated, useSiteTargetFilter)

internal fun KaAnnotatedSymbol.hasJvmOverloadsAnnotation(): Boolean = JVM_OVERLOADS_CLASS_ID in annotations

internal fun KaAnnotatedSymbol.hasJvmNameAnnotation(): Boolean = JvmStandardClassIds.Annotations.JvmName in annotations

internal fun KaAnnotatedSymbol.hasJvmStaticAnnotation(
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): Boolean = hasAnnotation(JvmStandardClassIds.Annotations.JvmStatic, useSiteTargetFilter)

internal fun KaAnnotatedSymbol.hasInlineOnlyAnnotation(): Boolean = StandardClassIds.Annotations.InlineOnly in annotations

context(KaSession)
internal fun KaDeclarationSymbol.suppressWildcardMode(
    declarationFilter: (KaDeclarationSymbol) -> Boolean = { true },
): Boolean? {
    return getContainingSymbolsWithSelf().firstNotNullOfOrNull { symbol ->
        symbol.takeIf(declarationFilter)?.suppressWildcard()
    }
}

internal fun KaAnnotatedSymbol.suppressWildcard(): Boolean? {
    if (hasJvmWildcardAnnotation()) return true
    return getJvmSuppressWildcardsFromAnnotation()
}

internal fun KaAnnotatedSymbol.getJvmSuppressWildcardsFromAnnotation(): Boolean? {
    return annotations[JvmStandardClassIds.Annotations.JvmSuppressWildcards].firstOrNull()?.let { annoApp ->
        (annoApp.arguments.firstOrNull()?.expression as? KaAnnotationValue.ConstantValue)?.value?.value as? Boolean
    }
}

internal fun KaAnnotatedSymbol.hasJvmWildcardAnnotation(): Boolean = JvmStandardClassIds.Annotations.JvmWildcard in annotations

internal fun KaAnnotatedSymbol.findAnnotation(
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
): KaAnnotation? {
    if (!hasAnnotation(classId, useSiteTargetFilter)) return null

    return annotationsByClassId(classId, useSiteTargetFilter).firstOrNull()
}

internal fun KaAnnotatedSymbol.hasAnnotation(
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter,
): Boolean {
    return annotations[classId]
        .any { useSiteTargetFilter.isAllowed(it.useSiteTarget) }
}

internal fun KaAnnotatedSymbol.annotationsByClassId(
    classId: ClassId,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter
): List<KaAnnotation> {
    return annotations[classId]
        .filter { useSiteTargetFilter.isAllowed(it.useSiteTarget) }
}

context(KaSession)
internal fun KaAnnotatedSymbol.computeThrowsList(
    builder: LightReferenceListBuilder,
    useSitePosition: PsiElement,
    containingClass: SymbolLightClassBase,
    useSiteTargetFilter: AnnotationUseSiteTargetFilter = AnyAnnotationUseSiteTargetFilter,
) {
    if (containingClass.isEnum && this is KaFunctionSymbol && name == StandardNames.ENUM_VALUE_OF && isStatic) {
        builder.addReference(java.lang.IllegalArgumentException::class.qualifiedName)
        builder.addReference(java.lang.NullPointerException::class.qualifiedName)
    }

    val annoApp = findAnnotation(JvmStandardClassIds.Annotations.Throws, useSiteTargetFilter) ?: return

    fun handleAnnotationValue(annotationValue: KaAnnotationValue) {
        when (annotationValue) {
            is KaAnnotationValue.ArrayValue -> {
                annotationValue.values.forEach(::handleAnnotationValue)
            }

            is KaAnnotationValue.ClassLiteralValue -> {
                if (annotationValue.type is KaClassType) {
                    val psiType = annotationValue.type.asPsiType(
                        useSitePosition,
                        allowErrorTypes = true,
                        KaTypeMappingMode.DEFAULT,
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

context(KaSession)
@KaAnalysisNonPublicApi
fun annotateByKtType(
    psiType: PsiType,
    ktType: KaType,
    annotationParent: PsiElement,
): PsiType {
    fun getAnnotationsSequence(type: KaType): Sequence<List<PsiAnnotation>> = sequence {
        val unwrappedType = when (type) {
            // We assume that flexible types have to have the same set of annotations on upper and lower bound.
            // Also, the upper bound is more similar to the resulting PsiType as it has fewer restrictions.
            is KaFlexibleType -> type.upperBound
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
            psiType !is PsiPrimitiveType && type.isPrimitiveBacked -> KaTypeNullability.NON_NULLABLE
            else -> getTypeNullability(type)
        }

        val nullabilityAnnotation = typeNullability.asAnnotationQualifier?.let {
            SymbolLightSimpleAnnotation(it, annotationParent)
        }

        yield(explicitTypeAnnotations + listOfNotNull(nullabilityAnnotation))

        if (unwrappedType is KaClassType) {
            unwrappedType.typeArguments.forEach { typeProjection ->
                typeProjection.type?.let {
                    yieldAll(getAnnotationsSequence(it))
                }
            }
        }
    }

    return psiType.annotateByTypeAnnotationProvider(getAnnotationsSequence(ktType))
}
