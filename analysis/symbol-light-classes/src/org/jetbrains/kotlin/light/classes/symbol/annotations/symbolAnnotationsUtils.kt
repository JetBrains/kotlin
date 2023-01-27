/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.light.LightReferenceListBuilder
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.annotations.KtKClassAnnotationValue.KtNonLocalKClassAnnotationValue
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmNames.JVM_OVERLOADS_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_SYNTHETIC_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

internal fun KtAnnotatedSymbol.hasJvmSyntheticAnnotation(
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
    acceptAnnotationsWithoutUseSite: Boolean = false,
): Boolean = hasAnnotation(JVM_SYNTHETIC_ANNOTATION_CLASS_ID, annotationUseSiteTarget, acceptAnnotationsWithoutUseSite)

internal fun KtAnnotatedSymbol.getJvmNameFromAnnotation(annotationUseSiteTarget: AnnotationUseSiteTarget? = null): String? {
    val annotation = findAnnotation(StandardClassIds.Annotations.JvmName, annotationUseSiteTarget, acceptAnnotationsWithoutUseSite = true)
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
    acceptAnnotationsWithoutUseSite: Boolean = false,
) = isHiddenByDeprecation(this, annotationUseSiteTarget) ||
        hasJvmSyntheticAnnotation(annotationUseSiteTarget, acceptAnnotationsWithoutUseSite)

internal fun KtAnnotatedSymbol.hasJvmFieldAnnotation(): Boolean = hasAnnotation(StandardClassIds.Annotations.JvmField)

internal fun KtAnnotatedSymbol.hasPublishedApiAnnotation(annotationUseSiteTarget: AnnotationUseSiteTarget? = null): Boolean =
    hasAnnotation(StandardClassIds.Annotations.PublishedApi, annotationUseSiteTarget)

internal fun KtAnnotatedSymbol.hasDeprecatedAnnotation(
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
    acceptAnnotationsWithoutUseSite: Boolean = false,
): Boolean = hasAnnotation(StandardClassIds.Annotations.Deprecated, annotationUseSiteTarget, acceptAnnotationsWithoutUseSite)

internal fun KtAnnotatedSymbol.hasJvmOverloadsAnnotation(): Boolean = hasAnnotation(JVM_OVERLOADS_CLASS_ID)

internal fun KtAnnotatedSymbol.hasJvmStaticAnnotation(
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
    acceptAnnotationsWithoutUseSite: Boolean = false,
): Boolean = hasAnnotation(StandardClassIds.Annotations.JvmStatic, annotationUseSiteTarget, acceptAnnotationsWithoutUseSite)

internal fun KtAnnotatedSymbol.hasInlineOnlyAnnotation(): Boolean = hasAnnotation(StandardClassIds.Annotations.InlineOnly)

internal fun KtAnnotatedSymbol.findAnnotation(
    classId: ClassId,
    annotationUseSiteTarget: AnnotationUseSiteTarget?,
    acceptAnnotationsWithoutUseSite: Boolean = false,
): KtAnnotationApplicationWithArgumentsInfo? {
    if (!hasAnnotation(classId, annotationUseSiteTarget, acceptAnnotationsWithoutUseSite)) return null

    return annotations.find {
        val useSiteTarget = it.useSiteTarget
        (useSiteTarget == annotationUseSiteTarget || acceptAnnotationsWithoutUseSite && useSiteTarget == null) && it.classId == classId
    }
}

context(KtAnalysisSession)
internal fun KtAnnotatedSymbol.computeThrowsList(
    builder: LightReferenceListBuilder,
    annotationUseSiteTarget: AnnotationUseSiteTarget?,
    useSitePosition: PsiElement,
    containingClass: SymbolLightClassBase,
    acceptAnnotationsWithoutUseSite: Boolean = false,
) {
    if (containingClass.isEnum && this is KtFunctionSymbol && name == StandardNames.ENUM_VALUE_OF && isStatic) {
        builder.addReference(java.lang.IllegalArgumentException::class.qualifiedName)
        builder.addReference(java.lang.NullPointerException::class.qualifiedName)
    }

    val annoApp = findAnnotation(StandardClassIds.Annotations.Throws, annotationUseSiteTarget, acceptAnnotationsWithoutUseSite) ?: return

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
