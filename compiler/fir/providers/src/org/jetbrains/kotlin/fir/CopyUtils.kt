/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClassId
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.typeAttributeExtensions
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef

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
    type: ConeKotlinType
): FirResolvedTypeRef {
    return if (type is ConeErrorType) {
        buildErrorTypeRef {
            source = this@resolvedTypeFromPrototype.source
            this.type = type
            diagnostic = type.diagnostic
        }
    } else {
        buildResolvedTypeRef {
            source = this@resolvedTypeFromPrototype.source
            this.type = type
            annotations += this@resolvedTypeFromPrototype.annotations
        }
    }
}

fun FirTypeRef.errorTypeFromPrototype(
    diagnostic: ConeDiagnostic
): FirErrorTypeRef {
    return buildErrorTypeRef {
        source = this@errorTypeFromPrototype.source
        this.diagnostic = diagnostic
    }
}

fun List<FirAnnotation>.computeTypeAttributes(session: FirSession, predefined: List<ConeAttribute<*>> = emptyList()): ConeAttributes {
    if (this.isEmpty()) {
        if (predefined.isEmpty()) return ConeAttributes.Empty
        return ConeAttributes.create(predefined)
    }
    val attributes = mutableListOf<ConeAttribute<*>>()
    attributes += predefined
    val customAnnotations = mutableListOf<FirAnnotation>()
    for (annotation in this) {
        when (annotation.fullyExpandedClassId(session)) {
            CompilerConeAttributes.Exact.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.Exact
            CompilerConeAttributes.NoInfer.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.NoInfer
            CompilerConeAttributes.ExtensionFunctionType.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.ExtensionFunctionType
            CompilerConeAttributes.ContextFunctionTypeParams.ANNOTATION_CLASS_ID ->
                attributes +=
                    CompilerConeAttributes.ContextFunctionTypeParams(
                        annotation.extractContextReceiversCount() ?: 0
                    )

            CompilerConeAttributes.UnsafeVariance.ANNOTATION_CLASS_ID -> attributes += CompilerConeAttributes.UnsafeVariance
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
    if (customAnnotations.isNotEmpty()) {
        attributes += CustomAnnotationTypeAttribute(customAnnotations)
    }
    return ConeAttributes.create(attributes)
}

private fun FirAnnotation.extractContextReceiversCount() =
    (argumentMapping.mapping[StandardNames.CONTEXT_FUNCTION_TYPE_PARAMETER_COUNT_NAME] as? FirConstExpression<*>)?.value as? Int
