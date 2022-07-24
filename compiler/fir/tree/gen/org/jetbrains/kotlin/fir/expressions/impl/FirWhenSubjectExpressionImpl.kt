/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirExpressionRef
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirWhenSubjectExpressionImpl(
    override val source: KtSourceElement?,
    override val annotations: MutableList<FirAnnotation>,
    override val whenRef: FirExpressionRef<FirWhenExpression>,
) : FirWhenSubjectExpression() {
    override val typeRef: FirTypeRef get() = whenRef.value.subject!!.typeRef

    override val elementKind get() = FirElementKind.WhenSubjectExpression

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations.clear()
        annotations.addAll(newAnnotations)
    }
}
