/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.FirIntegerLiteralOperatorCall
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.FirImplementationDetail

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirIntegerLiteralOperatorCallImpl(
    override var source: KtSourceElement?,
    override var typeRef: FirTypeRef,
    override val annotations: MutableList<FirAnnotation>,
    override val contextReceiverArguments: MutableList<FirExpression>,
    override val typeArguments: MutableList<FirTypeProjection>,
    override var explicitReceiver: FirExpression?,
    override var dispatchReceiver: FirExpression,
    override var extensionReceiver: FirExpression,
    override var argumentList: FirArgumentList,
    override var calleeReference: FirNamedReference,
    override val origin: FirFunctionCallOrigin,
) : FirIntegerLiteralOperatorCall() {
    override val elementKind get() = FirElementKind.IntegerLiteralOperatorCall

    @FirImplementationDetail
    override fun replaceSource(newSource: KtSourceElement?) {
        source = newSource
    }

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceContextReceiverArguments(newContextReceiverArguments: List<FirExpression>) {
        contextReceiverArguments.clear()
        contextReceiverArguments.addAll(newContextReceiverArguments)
    }

    override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>) {
        typeArguments.clear()
        typeArguments.addAll(newTypeArguments)
    }

    override fun replaceExplicitReceiver(newExplicitReceiver: FirExpression?) {
        explicitReceiver = newExplicitReceiver
    }

    override fun replaceDispatchReceiver(newDispatchReceiver: FirExpression) {
        dispatchReceiver = newDispatchReceiver
    }

    override fun replaceExtensionReceiver(newExtensionReceiver: FirExpression) {
        extensionReceiver = newExtensionReceiver
    }

    override fun replaceArgumentList(newArgumentList: FirArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceCalleeReference(newCalleeReference: FirNamedReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        require(newCalleeReference is FirNamedReference)
        replaceCalleeReference(newCalleeReference)
    }
}
