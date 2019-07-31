/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.FirTransformer

open class FirValueParameterImpl(
    session: FirSession,
    psi: PsiElement?,
    name: Name,
    override var returnTypeRef: FirTypeRef,
    override var defaultValue: FirExpression?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isVararg: Boolean,
    override val symbol: FirVariableSymbol<FirValueParameter> = FirVariableSymbol(name)
) : FirAbstractNamedAnnotatedDeclaration(session, psi, name), FirValueParameter {

    init {
        symbol.bind(this)
    }

    override val isVar: Boolean
        get() = false
    override val initializer: FirExpression?
        get() = null
    override val delegate: FirExpression?
        get() = null
    override val receiverTypeRef: FirTypeRef?
        get() = null
    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirValueParameter>?
        get() = null

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)

        return super<FirAbstractNamedAnnotatedDeclaration>.transformChildren(transformer, data)
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D) {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
    }
}