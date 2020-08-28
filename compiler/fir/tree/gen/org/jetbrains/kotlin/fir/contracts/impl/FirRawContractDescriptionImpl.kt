/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirRawContractDescriptionImpl(
    override val source: FirSourceElement?,
    override val rawEffects: MutableList<FirExpression>,
) : FirRawContractDescription() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        rawEffects.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirRawContractDescriptionImpl {
        rawEffects.transformInplace(transformer, data)
        return this
    }
}
