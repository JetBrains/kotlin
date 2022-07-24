/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBinaryLogicExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.LogicOperationKind
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirBinaryLogicExpressionImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override var leftOperand: FirExpression,
    override var rightOperand: FirExpression,
    override val kind: LogicOperationKind,
) : FirBinaryLogicExpression() {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(null)

    override val elementKind get() = FirElementKind.BinaryLogicExpression

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceLeftOperand(newLeftOperand: FirExpression) {
        leftOperand = newLeftOperand
    }

    override fun replaceRightOperand(newRightOperand: FirExpression) {
        rightOperand = newRightOperand
    }
}
