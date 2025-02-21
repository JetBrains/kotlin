/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.FirImplicitInvokeCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.buildImplicitInvokeCall
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.typeAttributeExtensions
import org.jetbrains.kotlin.fir.resolve.directExpansionType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.name.ClassId

inline fun FirFunctionCall.copyAsImplicitInvokeCall(
    setupCopy: FirImplicitInvokeCallBuilder.() -> Unit
): FirImplicitInvokeCall {
    val original = this

    return buildImplicitInvokeCall {
        source = original.source
        annotations.addAll(original.annotations)
        typeArguments.addAll(original.typeArguments)
        explicitReceiver = original.explicitReceiver
        dispatchReceiver = original.dispatchReceiver
        extensionReceiver = original.extensionReceiver
        argumentList = original.argumentList
        calleeReference = original.calleeReference

        setupCopy()
    }
}

fun FirTypeRef.resolvedTypeFromPrototype(
    type: ConeKotlinType,
    fallbackSource: KtSourceElement?,
): FirResolvedTypeRef {
    if (this is FirResolvedTypeRef) {
        return withReplacedSourceAndType(this@resolvedTypeFromPrototype.source ?: fallbackSource, type)
    }
    return if (type is ConeErrorType) {
        buildErrorTypeRef {
            source = this@resolvedTypeFromPrototype.source ?: fallbackSource
            this.coneType = type
            diagnostic = type.diagnostic
            annotations += this@resolvedTypeFromPrototype.annotations
        }
    } else {
        buildResolvedTypeRef {
            source = this@resolvedTypeFromPrototype.source ?: fallbackSource
            this.coneType = type
            delegatedTypeRef = this@resolvedTypeFromPrototype as? FirUserTypeRef
            annotations += this@resolvedTypeFromPrototype.annotations
        }
    }
}

/**
 * [shouldExpandTypeAliases] should be set to `false` if this function is called during deserialization of some binary declaration
 * For details see KT-57876
 */
fun List<FirAnnotation>.computeTypeAttributes(
    session: FirSession,
    predefined: List<ConeAttribute<*>> = emptyList(),
    allowExtensionFunctionType: Boolean = true,
    shouldExpandTypeAliases: Boolean
): ConeAttributes {
    if (this.isEmpty()) {
        if (predefined.isEmpty()) return ConeAttributes.Empty
        return ConeAttributes.create(predefined)
    }
    val attributes = mutableListOf<ConeAttribute<*>>()
    var parameterName: ParameterNameTypeAttribute? = null
    attributes += predefined
    val customAnnotations = mutableListOf<FirAnnotation>()
    for (annotation in this) {
        val classId = when (shouldExpandTypeAliases) {
            true -> annotation.tryExpandClassId(session)
            false -> annotation.resolvedType.classId
        }
        when (classId) {
            CompilerConeAttributes.Exact.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.Exact
            CompilerConeAttributes.NoInfer.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.NoInfer
            CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID -> when {
                allowExtensionFunctionType -> attributes += CompilerConeAttributes.ExtensionFunctionType
            }
            CompilerConeAttributes.ContextFunctionTypeParams.ANNOTATION_CLASS_ID ->
                attributes +=
                    CompilerConeAttributes.ContextFunctionTypeParams(
                        annotation.extractContextParameterCount() ?: 0
                    )
            ParameterNameTypeAttribute.ANNOTATION_CLASS_ID -> {
                // ConeAttributes.create() will always take the last attribute of a given type,
                // where ParameterName should prefer the first annotation.
                if (parameterName == null) {
                    parameterName = ParameterNameTypeAttribute(annotation)
                } else {
                    // Preserve repeated ParameterName annotations to check for repeated errors.
                    parameterName = ParameterNameTypeAttribute(parameterName.annotation, parameterName.others + annotation)
                }
            }
            CompilerConeAttributes.UnsafeVariance.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.UnsafeVariance
            CompilerConeAttributes.EnhancedNullability.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.EnhancedNullability
            else -> {
                val attributeFromPlugin = session.extensionService.typeAttributeExtensions.firstNotNullOfOrNull {
                    it.extractAttributeFromAnnotation(annotation)
                }
                if (attributeFromPlugin != null) {
                    attributes += attributeFromPlugin
                } else {
                    customAnnotations += annotation
                }
            }
        }
    }

    if (parameterName != null) {
        attributes += parameterName
    }
    if (customAnnotations.isNotEmpty()) {
        attributes += CustomAnnotationTypeAttribute(customAnnotations)
    }

    return ConeAttributes.create(attributes)
}

private fun FirAnnotation.tryExpandClassId(session: FirSession): ClassId? {
    return when (val directlyExpanded = unexpandedConeClassLikeType?.directExpansionType(session) { it.expandedConeType }) {
        null -> unexpandedConeClassLikeType?.classId // mutually recursive typealiases
        else -> directlyExpanded.fullyExpandedType(session).classId
    }
}

private fun FirAnnotation.extractContextParameterCount() =
    (argumentMapping.mapping[StandardNames.CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME] as? FirLiteralExpression)?.value as? Int
