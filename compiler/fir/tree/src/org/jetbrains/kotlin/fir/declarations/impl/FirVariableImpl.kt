/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirVariableImpl(
    session: FirSession,
    psiElement: PsiElement?,
    name: Name,
    override var returnTypeRef: FirTypeRef,
    override val isVar: Boolean,
    override var initializer: FirExpression?,
    override val symbol: FirVariableSymbol = FirVariableSymbol(name),
    override var delegate: FirExpression? = null
) : FirAbstractNamedAnnotatedDeclaration(session, psiElement, name), FirVariable {

    init {
        symbol.bind(this)
    }

    override val receiverTypeRef: FirTypeRef?
        get() = null

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        initializer = initializer?.transformSingle(transformer, data)
        delegate = delegate?.transformSingle(transformer, data)

        return super<FirAbstractNamedAnnotatedDeclaration>.transformChildren(transformer, data)
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D) {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
    }
}