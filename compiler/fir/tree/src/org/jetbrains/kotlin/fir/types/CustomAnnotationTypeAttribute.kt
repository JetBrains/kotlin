/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCallCopy
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import kotlin.reflect.KClass

class CustomAnnotationTypeAttribute(val annotations: List<FirAnnotation>) : ConeAttribute<CustomAnnotationTypeAttribute>() {
    override fun union(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute? = null

    override fun intersect(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute? = null

    override fun add(other: CustomAnnotationTypeAttribute?): CustomAnnotationTypeAttribute {
        if (other == null || other === this) return this
        return CustomAnnotationTypeAttribute(annotations + other.annotations)
    }

    override fun isSubtypeOf(other: CustomAnnotationTypeAttribute?): Boolean = true

    override fun toString(): String = annotations.joinToString(separator = " ") { it.render() }

    override fun renderForReadability(): String =
        annotations.joinToString(separator = " ") { FirRenderer.forReadability().renderElementAsString(it, trim = true) }

    override fun substituteTypes(transform: (ConeKotlinType) -> ConeKotlinType?): CustomAnnotationTypeAttribute {
        val annotationsWithSubstitutedTypesArguments = annotations.map { annotation ->
            substituteAnnotationTypeArgs(annotation, transform) ?: annotation
        }
        return CustomAnnotationTypeAttribute(annotationsWithSubstitutedTypesArguments)
    }

    private fun substituteAnnotationTypeArgs(
        annotation: FirAnnotation,
        transform: (ConeKotlinType) -> ConeKotlinType?,
    ): FirAnnotation? {
        var changed = false
        val newTypeArgs = annotation.typeArguments.map { typeArg ->
            val typeProjection = typeArg as? FirTypeProjectionWithVariance ?: return@map typeArg
            val newConeType = transform(typeProjection.typeRef.coneTypeOrNull ?: return@map typeArg) ?: return@map typeArg
            changed = true
            buildTypeProjectionWithVariance {
                source = typeProjection.source
                typeRef = buildResolvedTypeRef { coneType = newConeType }
                variance = typeProjection.variance
            }
        }
        if (!changed) return null
        if (annotation !is FirAnnotationCall) return null
        return buildAnnotationCallCopy(annotation) { typeArguments.clear(); typeArguments.addAll(newTypeArgs) }
    }

    override val key: KClass<out CustomAnnotationTypeAttribute>
        get() = CustomAnnotationTypeAttribute::class
    override val keepInInferredDeclarationType: Boolean
        get() = true
}

val ConeAttributes.custom: CustomAnnotationTypeAttribute? by ConeAttributes.attributeAccessor<CustomAnnotationTypeAttribute>()

/**
 * Returns type [FirAnnotation]s in [this] [ConeAttributes] which are not covered by other cone attributes.
 *
 * Use this property only when the annotation is known to be some third-party annotation.
 * Otherwise, prefer using [typeAnnotations] to get all possible annotations, or when annotations are being
 * aggregated separately.
 */
val ConeAttributes.customAnnotations: List<FirAnnotation> get() = custom?.annotations.orEmpty()

/**
 * Returns type [FirAnnotation]s on [this] [ConeKotlinType] which are not covered by other cone attributes.
 *
 * Use this property only when the annotation is known to be some third-party annotation.
 * Otherwise, prefer using [typeAnnotations] to get all possible annotations, or when annotations are being
 * aggregated separately.
 */
val ConeKotlinType.customAnnotations: List<FirAnnotation> get() = attributes.customAnnotations

/**
 * Returns all type [FirAnnotation]s on [this] [ConeKotlinType].
 *
 * This property is an aggregate of all attributes which contain [FirAnnotation]s.
 */
val ConeKotlinType.typeAnnotations: List<FirAnnotation>
    get() {
        val customAnnotations = customAnnotations

        // Lambda parameter names are uncommon, so optimize access to skip if not present.
        val parameterNameAttribute = attributes.parameterNameAttribute
        if (parameterNameAttribute == null) return customAnnotations

        return buildList {
            addAll(parameterNameAttribute.annotations)
            addAll(customAnnotations)
        }
    }
