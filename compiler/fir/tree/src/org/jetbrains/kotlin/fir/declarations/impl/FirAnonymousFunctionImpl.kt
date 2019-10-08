/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirAnonymousFunctionImpl(
    session: FirSession,
    psi: PsiElement?,
    override var returnTypeRef: FirTypeRef,
    override var receiverTypeRef: FirTypeRef?,
    override val symbol: FirAnonymousFunctionSymbol
) : FirAnonymousFunction(session, psi), FirModifiableFunction<FirAnonymousFunction> {
    init {
        symbol.bind(this)
    }

    override var label: FirLabel? = null

    override val valueParameters = mutableListOf<FirValueParameter>()

    override var body: FirBlock? = null

    override var resolvePhase = FirResolvePhase.DECLARATIONS

    override var controlFlowGraphReference: FirControlFlowGraphReference = FirEmptyControlFlowGraphReference()

    override var invocationKind: InvocationKind? = null

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        label = label?.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        transformControlFlowGraphReference(transformer, data)

        return super<FirAnonymousFunction>.transformChildren(transformer, data)
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D) {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
    }

    override fun replaceReceiverTypeRef(receiverTypeRef: FirTypeRef) {
        this.receiverTypeRef = receiverTypeRef
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunction {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirAnonymousFunction {
        controlFlowGraphReference = controlFlowGraphReference.transformSingle(transformer, data)
        return this
    }

    override fun replaceInvocationKind(invocationKind: InvocationKind) {
        this.invocationKind = invocationKind
    }
}