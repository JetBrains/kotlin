/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeStubDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirErrorLoop
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirErrorLoopImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override var label: FirLabel?,
    override val diagnostic: ConeDiagnostic,
) : FirErrorLoop() {
    override var block: FirBlock = FirEmptyExpressionBlock()
    override var condition: FirExpression = FirErrorExpressionImpl(source, mutableListOf(), ConeStubDiagnostic(diagnostic), null)

    override val elementKind get() = FirElementKind.ErrorLoop

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }

    override fun replaceBlock(newBlock: FirBlock) {
        block = newBlock
    }

    override fun replaceCondition(newCondition: FirExpression) {
        condition = newCondition
    }

    override fun replaceLabel(newLabel: FirLabel?) {
        label = newLabel
    }
}
