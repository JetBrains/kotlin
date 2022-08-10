/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.CommonClassNames.JAVA_LANG_ANNOTATION_RETENTION
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtConstantAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtEnumEntryAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethod
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.RETENTION_POLICY_ENUM
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.name.JvmNames.JVM_MULTIFILE_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_NAME_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_OVERLOADS_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_SYNTHETIC_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.inline.INLINE_ONLY_ANNOTATION_FQ_NAME

internal fun KtAnnotatedSymbol.hasJvmSyntheticAnnotation(annotationUseSiteTarget: AnnotationUseSiteTarget? = null): Boolean =
    hasAnnotation(JVM_SYNTHETIC_ANNOTATION_CLASS_ID, annotationUseSiteTarget)

internal fun KtFileSymbol.hasJvmMultifileClassAnnotation(): Boolean =
    hasAnnotation(JVM_MULTIFILE_CLASS_ID, AnnotationUseSiteTarget.FILE)

internal fun KtAnnotatedSymbol.getJvmNameFromAnnotation(annotationUseSiteTarget: AnnotationUseSiteTarget? = null): String? {
    val annotation = annotations.firstOrNull {
        val siteTarget = it.useSiteTarget
        (siteTarget == null || siteTarget == annotationUseSiteTarget) &&
                it.classId == JVM_NAME_CLASS_ID
    }

    return annotation?.let {
        (it.arguments.firstOrNull()?.expression as? KtConstantAnnotationValue)?.constantValue?.value as? String
    }
}

context(KtAnalysisSession)
internal fun isHiddenByDeprecation(
    symbol: KtAnnotatedSymbol,
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null
): Boolean {
    return symbol.getDeprecationStatus(annotationUseSiteTarget)?.deprecationLevel == DeprecationLevelValue.HIDDEN
}

context(KtAnalysisSession)
internal fun KtAnnotatedSymbol.isHiddenOrSynthetic(annotationUseSiteTarget: AnnotationUseSiteTarget? = null) =
    isHiddenByDeprecation(this, annotationUseSiteTarget) || hasJvmSyntheticAnnotation(annotationUseSiteTarget)

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

internal fun NullabilityType.computeNullabilityAnnotation(parent: PsiElement): SymbolLightSimpleAnnotation? {
    return when (this) {
        NullabilityType.NotNull -> NotNull::class.java
        NullabilityType.Nullable -> Nullable::class.java
        else -> null
    }?.let {
        SymbolLightSimpleAnnotation(it.name, parent)
    }
}

internal fun KtAnnotatedSymbol.computeAnnotations(
    parent: PsiElement,
    nullability: NullabilityType,
    annotationUseSiteTarget: AnnotationUseSiteTarget?,
    includeAnnotationsWithoutSite: Boolean = true
): List<PsiAnnotation> {

    val nullabilityAnnotation = nullability.computeNullabilityAnnotation(parent)

    val parentIsAnnotation = (parent as? PsiClass)?.isAnnotationType == true

    val result = mutableListOf<PsiAnnotation>()

    if (parent is SymbolLightMethod) {
        if (parent.isDelegated || parent.isOverride()) {
            result.add(SymbolLightSimpleAnnotation(java.lang.Override::class.java.name, parent))
        }
    }

    if (annotations.isEmpty()) {
        if (parentIsAnnotation) {
            result.add(createRetentionRuntimeAnnotation(parent))
        }

        if (nullabilityAnnotation != null) {
            result.add(nullabilityAnnotation)
        }

        return result
    }

    for (annotation in annotations) {

        val siteTarget = annotation.useSiteTarget

        if ((includeAnnotationsWithoutSite && siteTarget == null) ||
            siteTarget == annotationUseSiteTarget
        ) {
            result.add(SymbolLightAnnotationForAnnotationCall(annotation, parent))
        }
    }

    if (parentIsAnnotation &&
        annotations.none { it.classId?.asFqNameString() == JAVA_LANG_ANNOTATION_RETENTION }
    ) {
        val argumentWithKotlinRetention = annotations.firstOrNull { it.classId == StandardClassIds.Annotations.Retention }
            ?.arguments
            ?.firstOrNull { it.name.asString() == "value" }
            ?.expression
        val kotlinRetentionName = (argumentWithKotlinRetention as? KtEnumEntryAnnotationValue)?.callableId?.callableName?.asString()
        result.add(createRetentionRuntimeAnnotation(parent, kotlinRetentionName))
    }

    if (nullabilityAnnotation != null) {
        result.add(nullabilityAnnotation)
    }

    return result
}

private fun createRetentionRuntimeAnnotation(parent: PsiElement, retentionName: String? = null): PsiAnnotation =
    SymbolLightSimpleAnnotation(
        JAVA_LANG_ANNOTATION_RETENTION,
        parent,
        listOf(
            KtNamedAnnotationValue(
                name = DEFAULT_VALUE_PARAMETER,
                expression = KtEnumEntryAnnotationValue(
                    callableId = CallableId(
                        ClassId.fromString(RETENTION_POLICY_ENUM.asString()),
                        Name.identifier(retentionName ?: AnnotationRetention.RUNTIME.name)
                    ),
                    sourcePsi = null
                )
            )
        )
    )
