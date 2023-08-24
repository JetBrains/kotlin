/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.MutableOrEmptyList
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.fir.visitors.transformSingle

class FirContractCallBlock(var call: FirFunctionCall) : FirBlock() {
    override val source: KtSourceElement?
        get() = call.source?.fakeElement(KtFakeSourceElementKind.ContractBlock)

    override val statements: List<FirStatement>
        get() = listOf(call)


    override var annotations: MutableOrEmptyList<FirAnnotation> = MutableOrEmptyList.empty()
    override var coneTypeOrNull: ConeKotlinType? = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        call.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirContractCallBlock {
        transformStatements(transformer, data)
        transformOtherChildren(transformer, data)
        return this
    }

    override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?) {
        coneTypeOrNull = newConeTypeOrNull
    }

    override fun <D> transformStatements(transformer: FirTransformer<D>, data: D): FirBlock {
        call = call.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirBlock {
        return this
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        annotations = newAnnotations.toMutableOrEmpty()
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirBlock {
        annotations.transformInplace(transformer, data)
        return this
    }


}
