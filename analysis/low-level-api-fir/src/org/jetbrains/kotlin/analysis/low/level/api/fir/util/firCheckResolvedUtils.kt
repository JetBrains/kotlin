/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnresolvedExpressionTypeAccess::class)

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.forEachType
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.utils.exceptions.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal inline fun checkTypeRefIsResolved(
    typeRef: FirTypeRef,
    typeRefName: String,
    owner: FirElementWithResolveState,
    acceptImplicitTypeRef: Boolean = false,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    checkWithAttachment(
        condition = typeRef is FirResolvedTypeRef || acceptImplicitTypeRef && typeRef is FirImplicitTypeRef,
        message = {
            buildString {
                append("Expected ${FirResolvedTypeRef::class.simpleName}")
                if (acceptImplicitTypeRef) {
                    append(" or ${FirImplicitTypeRef::class.simpleName}")
                }
                append(" for $typeRefName of ${owner::class.simpleName}(${(owner as? FirDeclaration)?.origin}) but ${typeRef::class.simpleName} found")
            }
        }
    ) {
        withFirEntry("typeRef", typeRef)
        withFirEntry("firDeclaration", owner)
        extraAttachment()
    }
}

internal inline fun checkExpressionTypeIsResolved(
    type: ConeKotlinType?,
    typeName: String,
    owner: FirElement,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    checkWithAttachment(
        condition = type != null,
        message = {
            buildString {
                append("Expected resolved expression type")
                append(" for $typeName of ${owner::class.simpleName}(${(owner as? FirDeclaration)?.origin})")
            }
        }
    ) {
        withFirEntry("firDeclaration", owner)
        extraAttachment()
    }
}

internal fun <T> checkAnnotationTypeIsResolved(annotationContainer: T) where T : FirAnnotationContainer, T : FirElementWithResolveState {
    annotationContainer.annotations.forEach { annotation ->
        checkTypeRefIsResolved(annotation.annotationTypeRef, "annotation type", owner = annotationContainer) {
            withFirEntry("firAnnotation", annotation)
        }

        annotation.typeArguments.forEach {
            if (it is FirTypeProjectionWithVariance) {
                checkTypeRefIsResolved(it.typeRef, "annotation type argument", owner = annotationContainer) {
                    withFirEntry("typeProjection", it)
                }
            }
        }
    }
}

internal fun checkBodyIsResolved(function: FirFunction) {
    val block = function.body ?: return
    checkExpressionTypeIsResolved(block.coneTypeOrNull, "block type", function) {
        withFirEntry("block", block)
    }
}

internal fun checkExpectForActualIsResolved(memberDeclaration: FirMemberDeclaration) {
    if (memberDeclaration.isExpect) return

    checkWithAttachment(
        condition = memberDeclaration.expectForActual != null,
        message = { "Expect for actual matching is missing" }
    ) {
        withFirEntry("declaration", memberDeclaration)
    }
}

internal fun checkDelegatedConstructorIsResolved(constructor: FirConstructor) {
    val delegatedConstructorCall = constructor.delegatedConstructor ?: return
    val calleeReference = delegatedConstructorCall.calleeReference
    checkReferenceIsResolved(reference = calleeReference, owner = delegatedConstructorCall) {
        withFirEntry("constructor", constructor)
    }
}

internal fun checkReferenceIsResolved(
    reference: FirReference,
    owner: FirResolvable,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    checkWithAttachment(
        condition = reference is FirResolvedNamedReference || reference is FirErrorNamedReference || reference is FirFromMissingDependenciesNamedReference,
        message = {
            "Expected ${FirNamedReference::class.simpleName}, " +
                    "${FirErrorNamedReference::class.simpleName} " +
                    "or ${FirFromMissingDependenciesNamedReference::class.simpleName}, " +
                    "but ${reference::class.simpleName} found"
        }
    ) {
        withFirEntry("referenceOwner", owner)
        extraAttachment()
    }
}

internal fun checkInitializerIsResolved(variable: FirVariable) {
    val initializer = variable.initializer ?: return
    checkExpressionTypeIsResolved(initializer.coneTypeOrNull, "initializer type", variable) {
        withFirEntry("initializer", initializer)
    }
}

internal fun checkDefaultValueIsResolved(parameter: FirValueParameter) {
    val defaultValue = parameter.defaultValue ?: return
    checkExpressionTypeIsResolved(defaultValue.coneTypeOrNull, "default value type", parameter) {
        withFirEntry("defaultValue", defaultValue)
    }
}

internal fun checkDeprecationProviderIsResolved(declaration: FirDeclaration, provider: DeprecationsProvider) {
    checkWithAttachment(
        condition = provider !is UnresolvedDeprecationProvider,
        message = { "Unresolved deprecation provider found for ${declaration::class.simpleName}" }
    ) {
        withFirEntry("declaration", declaration)
    }
}

internal fun checkReturnTypeRefIsResolved(declaration: FirCallableDeclaration, acceptImplicitTypeRef: Boolean = false) {
    checkTypeRefIsResolved(declaration.returnTypeRef, typeRefName = "return type", declaration, acceptImplicitTypeRef)
}

internal fun checkReceiverTypeRefIsResolved(declaration: FirCallableDeclaration, acceptImplicitTypeRef: Boolean = false) {
    val receiverTypeRef = declaration.receiverParameter?.typeRef ?: return
    checkTypeRefIsResolved(receiverTypeRef, typeRefName = "receiver type", declaration, acceptImplicitTypeRef)
}

internal fun checkContextReceiverTypeRefIsResolved(declaration: FirCallableDeclaration, acceptImplicitTypeRef: Boolean = false) {
    for (contextReceiver in declaration.contextReceivers) {
        val receiverTypeRef = contextReceiver.typeRef
        checkTypeRefIsResolved(receiverTypeRef, typeRefName = "context receiver type", declaration, acceptImplicitTypeRef)
    }
}

internal fun checkContractDescriptionIsResolved(declaration: FirContractDescriptionOwner) {
    val contractDescription = declaration.contractDescription
    checkWithAttachment(
        condition = contractDescription is FirResolvedContractDescription ||
                contractDescription is FirEmptyContractDescription ||
                contractDescription is FirLegacyRawContractDescription /* TODO: should be dropped after KT-60310 */,
        message = { "Expected ${FirResolvedContractDescription::class.simpleName} or ${FirEmptyContractDescription::class.simpleName} but ${contractDescription::class.simpleName} found for ${declaration::class.simpleName}" }
    ) {
        withFirEntry("declaration", declaration)
    }
}

internal fun checkDeclarationStatusIsResolved(declaration: FirMemberDeclaration) {
    val status = declaration.status
    checkWithAttachment(
        condition = status is FirResolvedDeclarationStatus,
        message = { "Expected ${FirResolvedDeclarationStatus::class.simpleName} but ${status::class.simpleName} found for ${declaration::class.simpleName}" }
    ) {
        withFirEntry("declaration", declaration)
    }
}

internal fun checkAnnotationsAreResolved(owner: FirAnnotationContainer, typeRef: FirTypeRef) {
    checkWithAttachment(typeRef is FirResolvedTypeRef, { "Unexpected type: ${typeRef::class.simpleName}" }) {
        withFirEntry("owner", owner)
        withFirEntry("type", typeRef)
    }

    typeRef.accept(AnnotationChecker, owner)
}

internal fun FirAbstractBodyResolveTransformerDispatcher.checkAnnotationCallIsResolved(
    symbol: FirBasedSymbol<*>,
    annotationCall: FirAnnotationCall,
) {
    val annotationContainer = context.containerIfAny ?: errorWithAttachment("Container cannot be found") {
        withFirSymbolEntry("symbol", symbol)
        withFirEntry("annotation", annotationCall)
    }

    checkAnnotationIsResolved(annotationCall, annotationContainer)
}

private object AnnotationChecker : AnnotationVisitorVoid<FirAnnotationContainer>() {
    override fun visitAnnotation(annotation: FirAnnotation, data: FirAnnotationContainer) {
        checkAnnotationIsResolved(annotation, data)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: FirAnnotationContainer) {
        checkAnnotationIsResolved(annotationCall, data)
    }
}

/**
 * This visitor is responsible for visiting all annotations recursively
 */
internal abstract class AnnotationVisitorVoid<T> : FirVisitor<Unit, T>() {
    abstract override fun visitAnnotation(annotation: FirAnnotation, data: T)
    abstract override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: T)

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: T) {
        resolvedTypeRef.acceptChildren(this, data)

        resolvedTypeRef.coneType.forEachType {
            it.type.attributes.customAnnotations.forEach { typeArgumentAnnotation ->
                typeArgumentAnnotation.accept(this, data)
            }
        }
    }

    override fun visitElement(element: FirElement, data: T) {
        element.acceptChildren(this, data)
    }
}

internal fun checkAnnotationsAreResolved(annotationContainer: FirAnnotationContainer) {
    for (annotation in annotationContainer.annotations) {
        checkAnnotationIsResolved(annotation, annotationContainer)
    }
}

internal fun checkAnnotationIsResolved(annotation: FirAnnotation, annotationContainer: FirAnnotationContainer) {
    if (annotation is FirAnnotationCall) {
        checkWithAttachment(
            condition = annotation.argumentList is FirResolvedArgumentList,
            message = {
                buildString {
                    append("Expected ${FirResolvedArgumentList::class.simpleName}")
                    append(" for ${annotation::class.simpleName} of ${annotationContainer::class.simpleName}(${(annotationContainer as? FirDeclaration)?.origin})")
                    append(" but ${annotation.argumentList::class.simpleName} found")
                }
            }
        ) {
            withFirEntry("firAnnotation", annotation)
            withFirEntry("firDeclaration", annotationContainer)
        }
    }

    for (argument in annotation.argumentMapping.mapping.values) {
        checkExpressionTypeIsResolved(argument.coneTypeOrNull, "annotation argument", annotationContainer) {
            withFirEntry("firAnnotation", annotation)
            withFirEntry("firArgument", argument)
        }
    }
}
