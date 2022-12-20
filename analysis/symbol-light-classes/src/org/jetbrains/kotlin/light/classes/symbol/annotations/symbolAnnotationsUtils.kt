/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightReferenceListBuilder
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.annotations.KtKClassAnnotationValue.KtNonLocalKClassAnnotationValue
import org.jetbrains.kotlin.analysis.api.components.buildClassType
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.DEFAULT_VALUE_PARAMETER
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethod
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAbi.JVM_FIELD_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.name.JvmNames.JVM_MULTIFILE_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_NAME_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_OVERLOADS_CLASS_ID
import org.jetbrains.kotlin.name.JvmNames.JVM_SYNTHETIC_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.resolve.annotations.JVM_THROWS_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.inline.INLINE_ONLY_ANNOTATION_FQ_NAME
import java.lang.annotation.ElementType

internal fun KtAnnotatedSymbol.hasJvmSyntheticAnnotation(
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
    strictUseSite: Boolean = true,
): Boolean = hasAnnotation(JVM_SYNTHETIC_ANNOTATION_CLASS_ID, annotationUseSiteTarget, strictUseSite)

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
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
): Boolean {
    return symbol.getDeprecationStatus(annotationUseSiteTarget)?.deprecationLevel == DeprecationLevelValue.HIDDEN
}

context(KtAnalysisSession)
internal fun KtAnnotatedSymbol.isHiddenOrSynthetic(
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
    strictUseSite: Boolean = true,
) = isHiddenByDeprecation(this, annotationUseSiteTarget) || hasJvmSyntheticAnnotation(annotationUseSiteTarget, strictUseSite)

internal fun KtAnnotatedSymbol.hasJvmFieldAnnotation(): Boolean =
    hasAnnotation(JVM_FIELD_ANNOTATION_CLASS_ID, null)

internal fun KtAnnotatedSymbol.hasPublishedApiAnnotation(annotationUseSiteTarget: AnnotationUseSiteTarget? = null): Boolean =
    hasAnnotation(StandardClassIds.Annotations.PublishedApi, annotationUseSiteTarget)

internal fun KtAnnotatedSymbol.hasDeprecatedAnnotation(
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
    strictUseSite: Boolean = true,
): Boolean = hasAnnotation(StandardClassIds.Annotations.Deprecated, annotationUseSiteTarget, strictUseSite)

internal fun KtAnnotatedSymbol.hasJvmOverloadsAnnotation(): Boolean = hasAnnotation(JVM_OVERLOADS_CLASS_ID, null)

internal fun KtAnnotatedSymbol.hasJvmStaticAnnotation(
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
    strictUseSite: Boolean = true,
): Boolean = hasAnnotation(JVM_STATIC_ANNOTATION_CLASS_ID, annotationUseSiteTarget, strictUseSite)

internal fun KtAnnotatedSymbol.hasInlineOnlyAnnotation(): Boolean =
    hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME, null)

internal fun KtAnnotatedSymbol.hasAnnotation(
    classId: ClassId,
    annotationUseSiteTarget: AnnotationUseSiteTarget?,
    strictUseSite: Boolean = true,
): Boolean = findAnnotation(classId, annotationUseSiteTarget, strictUseSite) != null

internal fun KtAnnotatedSymbol.findAnnotation(
    classId: ClassId,
    annotationUseSiteTarget: AnnotationUseSiteTarget?,
    strictUseSite: Boolean = true,
): KtAnnotationApplication? =
    annotations.find {
        val useSiteTarget = it.useSiteTarget
        (useSiteTarget == annotationUseSiteTarget || !strictUseSite && useSiteTarget == null) && it.classId == classId
    }

internal fun KtAnnotatedSymbol.hasAnnotation(
    fqName: FqName,
    annotationUseSiteTarget: AnnotationUseSiteTarget?,
    strictUseSite: Boolean = true,
): Boolean = findAnnotation(fqName, annotationUseSiteTarget, strictUseSite) != null

internal fun KtAnnotatedSymbol.findAnnotation(
    fqName: FqName,
    annotationUseSiteTarget: AnnotationUseSiteTarget?,
    strictUseSite: Boolean = true,
): KtAnnotationApplication? =
    annotations.find {
        val useSiteTarget = it.useSiteTarget
        (useSiteTarget == annotationUseSiteTarget || !strictUseSite && useSiteTarget == null) && it.classId?.asSingleFqName() == fqName
    }

internal fun NullabilityType.computeNullabilityAnnotation(parent: PsiModifierList): SymbolLightSimpleAnnotation? {
    return when (this) {
        NullabilityType.NotNull -> NotNull::class.java
        NullabilityType.Nullable -> Nullable::class.java
        else -> null
    }?.let {
        SymbolLightSimpleAnnotation(it.name, parent)
    }
}

context(KtAnalysisSession)
internal fun KtAnnotatedSymbol.computeAnnotations(
    modifierList: PsiModifierList,
    nullability: NullabilityType,
    annotationUseSiteTarget: AnnotationUseSiteTarget?,
    includeAnnotationsWithoutSite: Boolean = true,
): List<PsiAnnotation> {
    val parent = modifierList.parent
    val nullabilityAnnotation = nullability.computeNullabilityAnnotation(modifierList)
    val parentIsAnnotation = (parent as? PsiClass)?.isAnnotationType == true

    val result = mutableListOf<PsiAnnotation>()

    if (parent is SymbolLightMethod<*>) {
        if (parent.isDelegated || parent.isOverride()) {
            result.add(SymbolLightSimpleAnnotation(java.lang.Override::class.java.name, modifierList))
        }
    }

    val foundAnnotations = hashSetOf<String>()
    for (annotation in annotations) {
        annotation.classId?.asFqNameString()?.let(foundAnnotations::add)

        val siteTarget = annotation.useSiteTarget
        if (includeAnnotationsWithoutSite && siteTarget == null || siteTarget == annotationUseSiteTarget) {
            result.add(SymbolLightAnnotationForAnnotationCall(annotation, modifierList))
        }
    }

    if (parentIsAnnotation) {
        for (annotation in annotations) {
            val convertedAnnotation = annotation.tryConvertAsRetention(foundAnnotations, modifierList)
                ?: annotation.tryConvertAsTarget(foundAnnotations, modifierList)
                ?: annotation.tryConvertAsMustBeDocumented(foundAnnotations, modifierList)
                ?: annotation.tryConvertAsRepeatable(foundAnnotations, modifierList, this)
                ?: continue

            result += convertedAnnotation
        }

        if (StandardClassIds.Annotations.Retention.asFqNameString() !in foundAnnotations) {
            result += createRetentionAnnotation(modifierList)
        }
    }

    if (nullabilityAnnotation != null) {
        result.add(nullabilityAnnotation)
    }

    return result
}

private fun KtAnnotationApplication.tryConvertAsRetention(foundAnnotations: Set<String>, modifierList: PsiModifierList): PsiAnnotation? {
    if (classId != StandardClassIds.Annotations.Retention) return null
    if (JvmAnnotationNames.RETENTION_ANNOTATION.asString() in foundAnnotations) return null

    val argumentWithKotlinRetention = arguments.firstOrNull {
        it.name == DEFAULT_VALUE_PARAMETER
    }?.expression as? KtEnumEntryAnnotationValue

    val kotlinRetentionName = argumentWithKotlinRetention?.callableId?.callableName?.asString()
    return createRetentionAnnotation(modifierList, kotlinRetentionName)
}

private fun KtAnnotationApplication.tryConvertAsTarget(
    foundAnnotations: Set<String>,
    modifierList: PsiModifierList
): PsiAnnotation? = tryConvertAs(
    foundAnnotations = foundAnnotations,
    modifierList = modifierList,
    kotlinClassId = StandardClassIds.Annotations.Target,
    javaQualifier = JvmAnnotationNames.TARGET_ANNOTATION.asString(),
    argumentsComputer = fun(): List<KtNamedAnnotationValue>? {
        val allowedKotlinTargets = arguments.firstOrNull()?.expression as? KtArrayAnnotationValue ?: return null
        val javaTargetNames = allowedKotlinTargets.values.mapNotNullTo(linkedSetOf(), KtAnnotationValue::mapToJavaTarget)
        return listOf(
            KtNamedAnnotationValue(
                name = DEFAULT_VALUE_PARAMETER,
                expression = KtArrayAnnotationValue(
                    values = javaTargetNames.map {
                        KtEnumEntryAnnotationValue(
                            callableId = CallableId(
                                classId = StandardClassIds.Annotations.Java.ElementType,
                                callableName = Name.identifier(it),
                            ),
                            sourcePsi = null,
                        )
                    },
                    sourcePsi = null,
                )
            )
        )
    }
)

private fun KtAnnotationValue.mapToJavaTarget(): String? {
    if (this !is KtEnumEntryAnnotationValue) return null
    val callableId = callableId ?: return null
    if (callableId.classId != StandardClassIds.AnnotationTarget) return null
    return when (callableId.callableName.asString()) {
        AnnotationTarget.CLASS.name -> ElementType.TYPE
        AnnotationTarget.ANNOTATION_CLASS.name -> ElementType.ANNOTATION_TYPE
        AnnotationTarget.FIELD.name -> ElementType.FIELD
        AnnotationTarget.LOCAL_VARIABLE.name -> ElementType.LOCAL_VARIABLE
        AnnotationTarget.VALUE_PARAMETER.name -> ElementType.PARAMETER
        AnnotationTarget.CONSTRUCTOR.name -> ElementType.CONSTRUCTOR
        AnnotationTarget.FUNCTION.name, AnnotationTarget.PROPERTY_GETTER.name, AnnotationTarget.PROPERTY_SETTER.name -> ElementType.METHOD
        AnnotationTarget.TYPE_PARAMETER.name -> ElementType.TYPE_PARAMETER
        AnnotationTarget.TYPE.name -> ElementType.TYPE_USE
        else -> null
    }?.name
}

context(KtAnalysisSession)
private fun KtAnnotationApplication.tryConvertAsRepeatable(
    foundAnnotations: Set<String>,
    modifierList: PsiModifierList,
    ktAnnotatedSymbol: KtAnnotatedSymbol
): PsiAnnotation? = tryConvertAs(
    foundAnnotations = foundAnnotations,
    modifierList = modifierList,
    kotlinClassId = StandardClassIds.Annotations.Repeatable,
    javaQualifier = JvmAnnotationNames.REPEATABLE_ANNOTATION.asString(),
    argumentsComputer = fun(): List<KtNamedAnnotationValue>? {
        if (ktAnnotatedSymbol !is KtNamedClassOrObjectSymbol) return null
        val annotationClassId = ktAnnotatedSymbol.classIdIfNonLocal ?: return null

        return listOf(
            KtNamedAnnotationValue(
                name = DEFAULT_VALUE_PARAMETER,
                expression = KtNonLocalKClassAnnotationValue(
                    classId = annotationClassId.createNestedClassId(Name.identifier(JvmAbi.REPEATABLE_ANNOTATION_CONTAINER_NAME)),
                    sourcePsi = null,
                )
            )
        )
    },
)

private fun KtAnnotationApplication.tryConvertAsMustBeDocumented(
    foundAnnotations: Set<String>,
    modifierList: PsiModifierList,
): PsiAnnotation? = tryConvertAs(
    foundAnnotations = foundAnnotations,
    modifierList = modifierList,
    kotlinClassId = StandardClassIds.Annotations.MustBeDocumented,
    javaQualifier = JvmAnnotationNames.DOCUMENTED_ANNOTATION.asString(),
)

private fun KtAnnotationApplication.tryConvertAs(
    foundAnnotations: Set<String>,
    modifierList: PsiModifierList,
    kotlinClassId: ClassId,
    javaQualifier: String,
    argumentsComputer: (() -> List<KtNamedAnnotationValue>?)? = null,
): PsiAnnotation? {
    if (classId != kotlinClassId) return null
    if (javaQualifier in foundAnnotations) return null
    val arguments = if (argumentsComputer != null) {
        argumentsComputer() ?: return null
    } else {
        emptyList()
    }

    return SymbolLightSimpleAnnotation(fqName = javaQualifier, parent = modifierList, arguments = arguments)
}

private fun createRetentionAnnotation(
    modifierList: PsiModifierList,
    retentionName: String? = null,
): PsiAnnotation = SymbolLightSimpleAnnotation(
    fqName = JvmAnnotationNames.RETENTION_ANNOTATION.asString(),
    parent = modifierList,
    arguments = listOf(
        KtNamedAnnotationValue(
            name = DEFAULT_VALUE_PARAMETER,
            expression = KtEnumEntryAnnotationValue(
                callableId = CallableId(
                    StandardClassIds.Annotations.Java.RetentionPolicy,
                    Name.identifier(retentionName ?: AnnotationRetention.RUNTIME.name),
                ),
                sourcePsi = null,
            )
        )
    )
)

context(KtAnalysisSession)
internal fun KtAnnotatedSymbol.computeThrowsList(
    builder: LightReferenceListBuilder,
    annotationUseSiteTarget: AnnotationUseSiteTarget?,
    useSitePosition: PsiElement,
    containingClass: SymbolLightClassBase,
    strictUseSite: Boolean = true,
) {
    if (containingClass.isEnum && this is KtFunctionSymbol && name == StandardNames.ENUM_VALUE_OF && isStatic) {
        builder.addReference(java.lang.IllegalArgumentException::class.qualifiedName)
        builder.addReference(java.lang.NullPointerException::class.qualifiedName)
    }

    val annoApp = findAnnotation(JVM_THROWS_ANNOTATION_FQ_NAME, annotationUseSiteTarget, strictUseSite) ?: return

    fun handleAnnotationValue(annotationValue: KtAnnotationValue) = when (annotationValue) {
        is KtArrayAnnotationValue -> {
            annotationValue.values.forEach(::handleAnnotationValue)
        }

        is KtNonLocalKClassAnnotationValue -> {
            val psiType = buildClassType(annotationValue.classId).asPsiType(
                useSitePosition,
                KtTypeMappingMode.DEFAULT,
                containingClass.isAnnotationType
            )
            (psiType as? PsiClassType)?.let {
                builder.addReference(it)
            }
        }
        else -> {}
    }

    annoApp.arguments.forEach { handleAnnotationValue(it.expression) }
}
