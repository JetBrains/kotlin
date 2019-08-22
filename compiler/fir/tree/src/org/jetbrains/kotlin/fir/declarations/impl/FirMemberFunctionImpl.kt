/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

open class FirMemberFunctionImpl : FirAbstractCallableMember<FirNamedFunction>, FirNamedFunction, FirModifiableFunction<FirNamedFunction> {

    // NB: FirAccessorSymbol can be here
    override val symbol: FirFunctionSymbol<FirNamedFunction>

    constructor(
        session: FirSession,
        psi: PsiElement?,
        symbol: FirFunctionSymbol<FirNamedFunction>,
        name: Name,
        receiverTypeRef: FirTypeRef?,
        returnTypeRef: FirTypeRef
    ) : super(session, psi, name, receiverTypeRef, returnTypeRef) {
        this.symbol = symbol
        symbol.bind(this)
    }

    constructor(
        session: FirSession,
        psi: PsiElement?,
        symbol: FirNamedFunctionSymbol,
        name: Name,
        visibility: Visibility,
        modality: Modality?,
        isExpect: Boolean,
        isActual: Boolean,
        isOverride: Boolean,
        isOperator: Boolean,
        isInfix: Boolean,
        isInline: Boolean,
        isTailRec: Boolean,
        isExternal: Boolean,
        isSuspend: Boolean,
        receiverTypeRef: FirTypeRef?,
        returnTypeRef: FirTypeRef
    ) : super(
        session, psi, name, visibility, modality,
        isExpect, isActual, isOverride, receiverTypeRef, returnTypeRef
    ) {
        status.isOperator = isOperator
        status.isInfix = isInfix
        status.isInline = isInline
        status.isTailRec = isTailRec
        status.isExternal = isExternal
        status.isSuspend = isSuspend
        this.symbol = symbol
        symbol.bind(this)
    }

    override val valueParameters = mutableListOf<FirValueParameter>()

    override var body: FirBlock? = null

    override var controlFlowGraphReference: FirControlFlowGraphReference = FirEmptyControlFlowGraphReference()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        transformControlFlowGraphReference(transformer, data)

        return super<FirAbstractCallableMember>.transformChildren(transformer, data)
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirMemberFunctionImpl {
        valueParameters.transformInplace(transformer, data)
        return this
    }

    override fun <D> transformControlFlowGraphReference(transformer: FirTransformer<D>, data: D): FirMemberFunctionImpl {
        controlFlowGraphReference = controlFlowGraphReference.transformSingle(transformer, data)
        return this
    }
}