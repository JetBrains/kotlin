/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.contracts.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirLegacyRawContractDescriptionImpl(
    override val source: FirSourceElement?,
    override var contractCall: FirFunctionCall,
) : FirLegacyRawContractDescription() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        contractCall.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirLegacyRawContractDescriptionImpl {
        contractCall = contractCall.transformSingle(transformer, data)
        return this
    }
}
