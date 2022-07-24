/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAugmentedArraySetCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirAugmentedArraySetCallImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override var lhsGetCall: FirFunctionCall,
    override var rhs: FirExpression,
    override val operation: FirOperation,
    override var calleeReference: FirReference,
    override val arrayAccessSource: KtSourceElement?,
) : FirAugmentedArraySetCall() {
    override val elementKind get() = FirElementKind.AugmentedArraySetCall

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceLhsGetCall(newLhsGetCall: FirFunctionCall) {
        lhsGetCall = newLhsGetCall
    }

    override fun replaceRhs(newRhs: FirExpression) {
        rhs = newRhs
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        calleeReference = newCalleeReference
    }
}
