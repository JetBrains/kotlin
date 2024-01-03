/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightReferenceListBuilder
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.annotations.KtKClassAnnotationValue.KtNonLocalKClassAnnotationValue
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.classes.annotateByTypeAnnotationProvider
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
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

    fun handleAnnotationValue(annotationValue: KtAnnotationValue) = when (annotationValue) {
        is KtArrayAnnotationValue -> {
            annotationValue.values.forEach(::handleAnnotationValue)
        }

        is KtNonLocalKClassAnnotationValue -> {
            val psiType = buildClassType(annotationValue.classId).asPsiType(
                useSitePosition,
                allowErrorTypes = true,
                KtTypeMappingMode.DEFAULT,
                containingClass.isAnnotationType,
            )
            (psiType as? PsiClassType)?.let {
                builder.addReference(it)
            }
        }
        else -> {}
    }

    annoApp.arguments.forEach { handleAnnotationValue(it.expression) }
}

context(KtAnalysisSession)
internal fun annotateByKtType(
    psiType: PsiType,
    ktType: KtType,
    psiContext: PsiTypeElement,
    modifierListAsParent: PsiModifierList?,
): PsiType {
    fun KtType.getAnnotationsSequence(modifierList: PsiModifierList?): Sequence<List<PsiAnnotation>> = sequence {
        yield(
            annotations.map { annoApp ->
                SymbolLightSimpleAnnotation(
                    annoApp.classId?.asFqNameString(),
                    modifierList ?: psiContext,
                    annoApp.arguments,
                    annoApp.psi,
                )
            }
        )

        (this@getAnnotationsSequence as? KtNonErrorClassType)?.ownTypeArguments?.forEach { typeProjection ->
            typeProjection.type?.let {
                yieldAll(it.getAnnotationsSequence(modifierList = null))
            }
        }
    }

    return psiType.annotateByTypeAnnotationProvider(ktType.getAnnotationsSequence(modifierListAsParent))
}
