/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAugmentedArraySetCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirAugmentedArraySetCallImpl(
    override var source: FirSourceElement?,
    override val annotations: MutableList<FirAnnotationCall>,
    override var assignCall: FirFunctionCall,
    override var setGetBlock: FirBlock,
    override val operation: FirOperation,
    override var calleeReference: FirReference,
) : FirAugmentedArraySetCall() {
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        assignCall.accept(visitor, data)
        setGetBlock.accept(visitor, data)
        calleeReference.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirAugmentedArraySetCallImpl {
        transformAnnotations(transformer, data)
        assignCall = assignCall.transformSingle(transformer, data)
        setGetBlock = setGetBlock.transformSingle(transformer, data)
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAugmentedArraySetCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceSource(newSource: FirSourceElement?) {
        source = newSource
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        calleeReference = newCalleeReference
    }
}
