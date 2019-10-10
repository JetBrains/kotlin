/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.impl.FirAbstractAnnotatedElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirModifiableFunction<F : FirFunction<F>>  : FirFunction<F>, FirAbstractAnnotatedElement {
    override val psi: PsiElement?
    override val session: FirSession
    override var resolvePhase: FirResolvePhase
    override val annotations: MutableList<FirAnnotationCall>
    override var returnTypeRef: FirTypeRef
    override var receiverTypeRef: FirTypeRef?
    override var controlFlowGraphReference: FirControlFlowGraphReference
    override val typeParameters: MutableList<FirTypeParameter>
    override val symbol: FirFunctionSymbol<F>
    override val valueParameters: MutableList<FirValueParameter>
    override var body: FirBlock?
    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        annotations.forEach { it.accept(visitor, data) }
        returnTypeRef.accept(visitor, data)
        receiverTypeRef?.accept(visitor, data)
        controlFlowGraphReference.accept(visitor, data)
        typeParameters.forEach { it.accept(visitor, data) }
        valueParameters.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirModifiableFunction<F> {
        annotations.transformInplace(transformer, data)
        transformReturnTypeRef(transformer, data)
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        transformControlFlowGraphReference(transformer, data)
        typeParameters.transformInplace(transformer, data)
        transformValueParameters(transformer, data)
        body = body?.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirModifiableFunction<F> {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirModifiableFunction<F> {
        controlFlowGraphReference = controlFlowGraphReference.transformSingle(transformer, data)
        return this
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirModifiableFunction<F> {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun replaceResolvePhase(newResolvePhase: FirResolvePhase) {
        resolvePhase = newResolvePhase
    }
}
