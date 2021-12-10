/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.annotations.KtConstantAnnotationValue
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmNames.JVM_MULTIFILE_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_OVERLOADS_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_SYNTHETIC_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.resolve.inline.INLINE_ONLY_ANNOTATION_FQ_NAME

internal fun KtAnnotatedSymbol.hasJvmSyntheticAnnotation(annotationUseSiteTarget: AnnotationUseSiteTarget? = null): Boolean =
    hasAnnotation(JVM_SYNTHETIC_ANNOTATION_CLASS_ID, annotationUseSiteTarget)

internal fun KtFileSymbol.hasJvmMultifileClassAnnotation(): Boolean =
    hasAnnotation(JVM_MULTIFILE_CLASS_ID, AnnotationUseSiteTarget.FILE)

internal fun KtAnnotatedSymbol.getJvmNameFromAnnotation(annotationUseSiteTarget: AnnotationUseSiteTarget? = null): String? {
    val annotation = annotations.firstOrNull {
        val siteTarget = it.useSiteTarget
        (siteTarget == null || siteTarget == annotationUseSiteTarget) &&
                it.classId?.asString() == "kotlin/jvm/JvmName"
    }

    return annotation?.let {
        (it.arguments.firstOrNull()?.expression as? KtConstantAnnotationValue)?.constantValue?.value as? String
    }
}

internal fun isHiddenByDeprecation(
    project: Project,
    symbol: KtAnnotatedSymbol,
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null
): Boolean {
    return project.analyzeWithSymbolAsContext(symbol) {
        symbol.getDeprecationStatus(annotationUseSiteTarget)?.deprecationLevel == DeprecationLevelValue.HIDDEN
    }
}

internal fun KtAnnotatedSymbol.isHiddenOrSynthetic(project: Project, annotationUseSiteTarget: AnnotationUseSiteTarget? = null) =
    isHiddenByDeprecation(project, this, annotationUseSiteTarget) || hasJvmSyntheticAnnotation(annotationUseSiteTarget)

internal fun KtAnnotatedSymbol.hasJvmFieldAnnotation(): Boolean =
    hasAnnotation(JVM_FIELD_ANNOTATION_CLASS_ID, null)

internal fun KtAnnotatedSymbol.hasPublishedApiAnnotation(annotationUseSiteTarget: AnnotationUseSiteTarget? = null): Boolean =
    hasAnnotation(StandardClassIds.Annotations.PublishedApi, annotationUseSiteTarget)

internal fun KtAnnotatedSymbol.hasDeprecatedAnnotation(annotationUseSiteTarget: AnnotationUseSiteTarget? = null): Boolean =
    hasAnnotation(StandardClassIds.Annotations.Deprecated, annotationUseSiteTarget)

internal fun KtAnnotatedSymbol.hasJvmOverloadsAnnotation(): Boolean =
    hasAnnotation(JVM_OVERLOADS_CLASS_ID, null)

internal fun KtAnnotatedSymbol.hasJvmStaticAnnotation(annotationUseSiteTarget: AnnotationUseSiteTarget? = null): Boolean =
    hasAnnotation(JVM_STATIC_ANNOTATION_CLASS_ID, annotationUseSiteTarget)

internal fun KtAnnotatedSymbol.hasInlineOnlyAnnotation(): Boolean =
    hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME, null)

internal fun KtAnnotatedSymbol.hasAnnotation(classId: ClassId, annotationUseSiteTarget: AnnotationUseSiteTarget?): Boolean =
    annotations.any {
        it.useSiteTarget == annotationUseSiteTarget && it.classId == classId
    }

internal fun KtAnnotatedSymbol.hasAnnotation(fqName: FqName, annotationUseSiteTarget: AnnotationUseSiteTarget?): Boolean =
    annotations.any {
        it.useSiteTarget == annotationUseSiteTarget && it.classId?.asSingleFqName() == fqName
    }

internal fun KtAnnotatedSymbol.computeAnnotations(
    parent: PsiElement,
    nullability: NullabilityType,
    annotationUseSiteTarget: AnnotationUseSiteTarget?,
    includeAnnotationsWithoutSite: Boolean = true
): List<PsiAnnotation> {

    if (nullability == NullabilityType.Unknown && annotations.isEmpty()) return emptyList()

    val nullabilityAnnotation = when (nullability) {
        NullabilityType.NotNull -> NotNull::class.java
        NullabilityType.Nullable -> Nullable::class.java
        else -> null
    }?.let {
        FirLightSimpleAnnotation(it.name, parent)
    }

    if (annotations.isEmpty()) {
        return if (nullabilityAnnotation != null) listOf(nullabilityAnnotation) else emptyList()
    }

    val result = mutableListOf<PsiAnnotation>()
    for (annotation in annotations) {

        val siteTarget = annotation.useSiteTarget

        if ((includeAnnotationsWithoutSite && siteTarget == null) ||
            siteTarget == annotationUseSiteTarget
        ) {
            result.add(FirLightAnnotationForAnnotationCall(annotation, parent))
        }
    }

    if (nullabilityAnnotation != null) {
        result.add(nullabilityAnnotation)
    }

    return result
}
