/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitSuperReference
import org.jetbrains.kotlin.fir.references.impl.FirExplicitThisReference
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class FirDelegatedConstructorCallImpl(
    override val source: FirSourceElement?,
    override val annotations: MutableList<FirAnnotationCall>,
    override var argumentList: FirArgumentList,
    override var constructedTypeRef: FirTypeRef,
    override val isThis: Boolean,
) : FirDelegatedConstructorCall() {
    override var calleeReference: FirReference = if (isThis) FirExplicitThisReference(source, null) else FirExplicitSuperReference(source, constructedTypeRef)
    override val isSuper: Boolean get() = !isThis

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        calleeReference.accept(visitor, data)
        annotations.forEach { it.accept(visitor, data) }
        argumentList.accept(visitor, data)
        constructedTypeRef.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCallImpl {
        transformCalleeReference(transformer, data)
        transformAnnotations(transformer, data)
        argumentList = argumentList.transformSingle(transformer, data)
        constructedTypeRef = constructedTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformCalleeReference(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCallImpl {
        calleeReference = calleeReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirDelegatedConstructorCallImpl {
        annotations.transformInplace(transformer, data)
        return this
    }

    override fun replaceCalleeReference(newCalleeReference: FirReference) {
        calleeReference = newCalleeReference
    }

    override fun replaceArgumentList(newArgumentList: FirArgumentList) {
        argumentList = newArgumentList
    }

    override fun replaceConstructedTypeRef(newConstructedTypeRef: FirTypeRef) {
        constructedTypeRef = newConstructedTypeRef
    }
}
