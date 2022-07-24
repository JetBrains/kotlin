/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirTryExpressionImpl(
    override val source: KtSourceElement?,
    override var typeRef: FirTypeRef,
    override val annotations: MutableList<FirAnnotation>,
    override var calleeReference: FirReference,
    override var tryBlock: FirBlock,
    override val catches: MutableList<FirCatch>,
    override var finallyBlock: FirBlock?,
) : FirTryExpression() {
    override val elementKind get() = FirElementKind.TryExpression

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceTryBlock(newTryBlock: FirBlock) {
        tryBlock = newTryBlock
    }

    override fun replaceCatches(newCatches: List<FirCatch>) {
        catches.clear()
        catches.addAll(newCatches)
    }

    override fun replaceFinallyBlock(newFinallyBlock: FirBlock?) {
        finallyBlock = newFinallyBlock
    }
}
