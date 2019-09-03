/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.transformInplace
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
    override val symbol: FirVariableSymbol<FirVariableImpl> = FirVariableSymbol(name),
    override var delegate: FirExpression? = null
) : FirAbstractNamedAnnotatedDeclaration(session, psiElement, name), FirVariable<FirVariableImpl>, FirModifiableAccessorsOwner {

    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirVariableImpl>? =
        delegate?.let { FirDelegateFieldSymbol(symbol.callableId) }

    override var getter: FirPropertyAccessor? = null

    override var setter: FirPropertyAccessor? = null

    init {
        symbol.bind(this)
        delegateFieldSymbol?.bind(this)
        resolvePhase = FirResolvePhase.DECLARATIONS
    }

    override val receiverTypeRef: FirTypeRef?
        get() = null

    override fun <D> transformChildrenWithoutAccessors(transformer: FirTransformer<D>, data: D) {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        initializer = initializer?.transformSingle(transformer, data)
        delegate = delegate?.transformSingle(transformer, data)
        annotations.transformInplace(transformer, data)
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        getter = getter?.transformSingle(transformer, data)
        setter = setter?.transformSingle(transformer, data)
        transformChildrenWithoutAccessors(transformer, data)
        // Everything other (annotations, etc.) is done above
        return this
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D) {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
    }
}