/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirDefaultSetterValueParameter(
    session: FirSession,
    psi: PsiElement?,
    override var returnTypeRef: FirTypeRef,
    override val symbol: FirVariableSymbol = FirVariableSymbol(name)
) : FirAbstractNamedAnnotatedDeclaration(session, psi, name), FirValueParameter {

    init {
        symbol.bind(this)
    }

    override val initializer: FirExpression?
        get() = null
    override val delegate: FirExpression?
        get() = null
    override val receiverTypeRef: FirTypeRef?
        get() = null

    override val isCrossinline = false

    override val isNoinline = false

    override val isVararg = false

    override val isVar: Boolean = false
    override val isVal: Boolean = false

    override val defaultValue: FirExpression? = null

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)

        return super<FirAbstractNamedAnnotatedDeclaration>.transformChildren(transformer, data)
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D) {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
    }

    companion object {
        val name = Name.identifier("value")
    }
}