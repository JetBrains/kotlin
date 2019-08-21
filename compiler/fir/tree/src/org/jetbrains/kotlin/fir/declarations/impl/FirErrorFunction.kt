/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirErrorDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirErrorFunction(
    override val session: FirSession,
    psi: PsiElement?,
    override val reason: String,
    override val symbol: FirErrorFunctionSymbol = FirErrorFunctionSymbol()
) : FirAbstractElement(psi), FirErrorDeclaration, FirFunction<FirErrorFunction>, FirSymbolOwner<FirErrorFunction> {
    init {
        symbol.bind(this)
    }

    override val receiverTypeRef: FirTypeRef? get() = null

    override val returnTypeRef: FirTypeRef
        get() = TODO("not implemented")

    override val annotations: List<FirAnnotationCall>
        get() = emptyList()

    override val valueParameters: List<FirValueParameter>
        get() = emptyList()

    override val body: FirBlock?
        get() = null

    override var resolvePhase = FirResolvePhase.BODY_RESOLVE

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        super<FirFunction>.accept(visitor, data)

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirErrorFunction {
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D) {}
}