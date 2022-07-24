/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.ExhaustivenessStatus
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirWhenExpressionImpl(
    override val source: KtSourceElement?,
    override var typeRef: FirTypeRef,
    override val annotations: MutableList<FirAnnotation>,
    override var calleeReference: FirReference,
    override var subject: FirExpression?,
    override var subjectVariable: FirVariable?,
    override val branches: MutableList<FirWhenBranch>,
    override var exhaustivenessStatus: ExhaustivenessStatus?,
    override val usedAsExpression: Boolean,
) : FirWhenExpression() {
    override val elementKind get() = FirElementKind.WhenExpression

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

    override fun replaceSubject(newSubject: FirExpression?) {
        subject = newSubject
    }

    override fun replaceSubjectVariable(newSubjectVariable: FirVariable?) {
        subjectVariable = newSubjectVariable
    }

    override fun replaceBranches(newBranches: List<FirWhenBranch>) {
        branches.clear()
        branches.addAll(newBranches)
    }

    override fun replaceExhaustivenessStatus(newExhaustivenessStatus: ExhaustivenessStatus?) {
        exhaustivenessStatus = newExhaustivenessStatus
    }
}
