/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.fir.FirImplementationDetail

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirVariableAssignmentImpl(
    override var calleeReference: FirReference,
    override val annotations: MutableList<FirAnnotation>,
    override val contextReceiverArguments: MutableList<FirExpression>,
    override val typeArguments: MutableList<FirTypeProjection>,
    override var explicitReceiver: FirExpression?,
    override var dispatchReceiver: FirExpression,
    override var extensionReceiver: FirExpression,
    override var source: KtSourceElement?,
    override var rValue: FirExpression,
) : FirVariableAssignment() {
    override var lValue: FirReference 
        get() = calleeReference
        set(value) {
            calleeReference = value
        }
    override var lValueTypeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override val elementKind get() = FirElementKind.VariableAssignment

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        calleeReference = newCalleeReference
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

    @FirImplementationDetail
    override fun replaceSource(newSource: KtSourceElement?) {
        source = newSource
    }

    override fun replaceLValue(newLValue: FirReference) {
    }

    override fun replaceLValueTypeRef(newLValueTypeRef: FirTypeRef) {
        lValueTypeRef = newLValueTypeRef
    }

    override fun replaceRValue(newRValue: FirExpression) {
        rValue = newRValue
    }
}
