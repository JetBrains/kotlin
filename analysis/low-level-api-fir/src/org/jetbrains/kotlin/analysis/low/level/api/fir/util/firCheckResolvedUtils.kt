/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.utils.errors.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef

internal inline fun checkTypeRefIsResolved(
    typeRef: FirTypeRef,
    typeRefName: String,
    owner: FirElementWithResolveState,
    acceptImplicitTypeRef: Boolean = false,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {}
) {
    checkWithAttachmentBuilder(
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

internal fun checkBodyIsResolved(function: FirFunction) {
    val block = function.body ?: return
    checkTypeRefIsResolved(block.typeRef, "block type", function) {
        withFirEntry("block", block)
    }
}

internal fun checkExpectForActualIsResolved(memberDeclaration: FirMemberDeclaration) {
    if (!memberDeclaration.isActual) return

    checkWithAttachmentBuilder(
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
    checkWithAttachmentBuilder(
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
    checkTypeRefIsResolved(initializer.typeRef, "initializer type", variable) {
        withFirEntry("initializer", initializer)
    }
}

internal fun checkDefaultValueIsResolved(parameter: FirValueParameter) {
    val defaultValue = parameter.defaultValue ?: return
    checkTypeRefIsResolved(defaultValue.typeRef, "default value type", parameter) {
        withFirEntry("defaultValue", defaultValue)
    }
}

internal fun checkDeprecationProviderIsResolved(declaration: FirDeclaration, provider: DeprecationsProvider) {
    checkWithAttachmentBuilder(
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


internal fun checkContractDescriptionIsResolved(declaration: FirContractDescriptionOwner) {
    val contractDescription = declaration.contractDescription
    checkWithAttachmentBuilder(
        condition = contractDescription is FirResolvedContractDescription || contractDescription is FirEmptyContractDescription,
        message = { "Expected ${FirResolvedContractDescription::class.simpleName} or ${FirEmptyContractDescription::class.simpleName} but ${contractDescription::class.simpleName} found for ${declaration::class.simpleName}" }
    ) {
        withFirEntry("declaration", declaration)
    }
}

internal fun checkDeclarationStatusIsResolved(declaration: FirMemberDeclaration) {
    val status = declaration.status
    checkWithAttachmentBuilder(
        condition = status is FirResolvedDeclarationStatus,
        message = { "Expected ${FirResolvedDeclarationStatus::class.simpleName} but ${status::class.simpleName} found for ${declaration::class.simpleName}" }
    ) {
        withFirEntry("declaration", declaration)
    }
}

internal inline fun checkAnnotationArgumentsMappingIsResolved(
    annotation: FirAnnotationCall,
    owner: FirAnnotationContainer,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {}
) {
    checkWithAttachmentBuilder(
        condition = annotation.argumentList is FirResolvedArgumentList,
        message = {
            buildString {
                append("Expected ${FirResolvedArgumentList::class.simpleName}")
                append(" for ${annotation::class.simpleName} of ${owner::class.simpleName}(${(owner as? FirDeclaration)?.origin})")
                append(" but ${annotation.argumentList::class.simpleName} found")
            }
        }
    ) {
        withFirEntry("annotation", annotation)
        withFirEntry("firDeclaration", owner)
        extraAttachment()
    }
}
