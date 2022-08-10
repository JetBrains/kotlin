/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.utils.errors.ExceptionAttachmentBuilder
import org.jetbrains.kotlin.analysis.utils.errors.checkWithAttachmentBuilder
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef

internal inline fun checkTypeRefIsResolved(
    typeRef: FirTypeRef,
    typeRefName: String,
    owner: FirDeclaration,
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
                append(" for $typeRefName of ${owner::class.simpleName}(${owner.origin}) but ${typeRef::class.simpleName} found")
            }
        }
    ) {
        withFirEntry("typeRef", typeRef)
        withFirEntry("firDeclaration", owner)
        extraAttachment()
    }
}

internal fun checkReturnTypeRefIsResolved(declaration: FirCallableDeclaration, acceptImplicitTypeRef: Boolean = false) {
    checkTypeRefIsResolved(declaration.returnTypeRef, typeRefName = "return type", declaration, acceptImplicitTypeRef)
}

internal fun checkReceiverTypeRefIsResolved(declaration: FirCallableDeclaration, acceptImplicitTypeRef: Boolean = false) {
    val receiverTypeRef = declaration.receiverTypeRef ?: return
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
        message = { "Expected ${FirResolvedDeclarationStatus::class.simpleName} but ${declaration::class.simpleName} found for ${declaration::class.simpleName}" }
    ) {
        withFirEntry("declaration", declaration)
    }
}
