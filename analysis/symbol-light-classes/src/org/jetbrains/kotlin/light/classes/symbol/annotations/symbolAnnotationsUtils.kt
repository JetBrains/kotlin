/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightReferenceListBuilder
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.asJava.classes.annotateByTypeAnnotationProvider
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.light.classes.symbol.NullabilityAnnotation
import org.jetbrains.kotlin.light.classes.symbol.asAnnotationQualifier
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.getContainingSymbolsWithSelf
import org.jetbrains.kotlin.light.classes.symbol.getRequiredNullabilityAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_OVERLOADS_CLASS_ID
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_SYNTHETIC_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

internal fun KaAnnotatedSymbol.hasJvmSyntheticAnnotation(): Boolean {
    if (this is KaPropertySymbol) return backingFieldSymbol?.hasJvmSyntheticAnnotation() == true
    return JVM_SYNTHETIC_ANNOTATION_CLASS_ID in annotations
}

internal fun KaAnnotatedSymbol.getJvmNameFromAnnotation(): String? {
    val annotation = findAnnotation(JvmStandardClassIds.Annotations.JvmName)
    return annotation?.let {
        (it.arguments.firstOrNull()?.expression as? KaAnnotationValue.ConstantValue)?.value?.value as? String
    }
}

internal fun KaSession.isHiddenByDeprecation(
    symbol: KaAnnotatedSymbol,
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
): Boolean = symbol.deprecationStatus(annotationUseSiteTarget)?.deprecationLevel == DeprecationLevelValue.HIDDEN

internal fun KaSession.isHiddenOrSynthetic(
    symbol: KaAnnotatedSymbol,
    annotationUseSiteTarget: AnnotationUseSiteTarget? = null,
): Boolean = isHiddenByDeprecation(symbol, annotationUseSiteTarget) || symbol.hasJvmSyntheticAnnotation()

internal fun KaAnnotatedSymbol.hasJvmFieldAnnotation(): Boolean = JvmStandardClassIds.Annotations.JvmField in annotations

internal fun KaAnnotatedSymbol.hasPublishedApiAnnotation(): Boolean = StandardClassIds.Annotations.PublishedApi in annotations

internal fun KaAnnotatedSymbol.hasDeprecatedAnnotation(): Boolean = StandardClassIds.Annotations.Deprecated in annotations

internal fun KaAnnotatedSymbol.hasJvmOverloadsAnnotation(): Boolean = JVM_OVERLOADS_CLASS_ID in annotations

internal fun KaAnnotatedSymbol.hasJvmNameAnnotation(): Boolean = JvmStandardClassIds.Annotations.JvmName in annotations

internal fun KaAnnotatedSymbol.hasJvmStaticAnnotation(): Boolean = JvmStandardClassIds.Annotations.JvmStatic in annotations

internal fun KaAnnotatedSymbol.hasInlineOnlyAnnotation(): Boolean = StandardClassIds.Annotations.InlineOnly in annotations

internal fun KaSession.suppressWildcardMode(
    symbol: KaDeclarationSymbol,
    declarationFilter: (KaDeclarationSymbol) -> Boolean = { true },
): Boolean? = getContainingSymbolsWithSelf(symbol).firstNotNullOfOrNull { symbol ->
    symbol.takeIf(declarationFilter)?.suppressWildcard()
}

internal fun KaAnnotatedSymbol.suppressWildcard(): Boolean? {
    if (hasJvmWildcardAnnotation()) return true
    return getJvmSuppressWildcardsFromAnnotation()
}

internal fun KaAnnotatedSymbol.getJvmSuppressWildcardsFromAnnotation(): Boolean? {
    val annotation = findAnnotation(JvmStandardClassIds.Annotations.JvmSuppressWildcards) ?: return null

    val argument = annotation.arguments.firstOrNull()
    return (argument?.expression as? KaAnnotationValue.ConstantValue)?.value?.value as? Boolean != false
}

internal fun KaAnnotatedSymbol.hasJvmWildcardAnnotation(): Boolean = JvmStandardClassIds.Annotations.JvmWildcard in annotations

internal fun KaAnnotatedSymbol.findAnnotation(classId: ClassId): KaAnnotation? = annotations[classId].firstOrNull()

internal fun KaSession.computeThrowsList(
    symbol: KaAnnotatedSymbol,
    builder: LightReferenceListBuilder,
    useSitePosition: PsiElement,
    containingClass: SymbolLightClassBase,
) {
    if (containingClass.isEnum && symbol is KaNamedFunctionSymbol && symbol.name == StandardNames.ENUM_VALUE_OF && symbol.isStatic) {
        builder.addReference(IllegalArgumentException::class.qualifiedName)
        builder.addReference(NullPointerException::class.qualifiedName)
    }

    val annoApp = symbol.findAnnotation(JvmStandardClassIds.Annotations.Throws) ?: return

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
                        allowNonJvmPlatforms = true,
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

@KaImplementationDetail
fun KaSession.annotateByKtType(
    psiType: PsiType,
    ktType: KaType,
    annotationParent: PsiElement,
    inferNullabilityForTypeArguments: Boolean,
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
            !inferNullabilityForTypeArguments && type is KaTypeParameterType -> NullabilityAnnotation.NOT_REQUIRED
            psiType !is PsiPrimitiveType && type.isPrimitiveBacked -> NullabilityAnnotation.NON_NULLABLE
            else -> getRequiredNullabilityAnnotation(type)
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
