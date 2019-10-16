/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirDelegateFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

interface FirValueParameter : FirVariable<FirValueParameter> {
    override val psi: PsiElement?
    override val session: FirSession
    override val resolvePhase: FirResolvePhase
    override val returnTypeRef: FirTypeRef
    override val receiverTypeRef: FirTypeRef?
    override val name: Name
    override val symbol: FirVariableSymbol<FirValueParameter>
    override val initializer: FirExpression?
    override val delegate: FirExpression?
    override val delegateFieldSymbol: FirDelegateFieldSymbol<FirValueParameter>?
    override val isVar: Boolean
    override val isVal: Boolean
    override val getter: FirPropertyAccessor?
    override val setter: FirPropertyAccessor?
    override val annotations: List<FirAnnotationCall>
    val defaultValue: FirExpression?
    val isCrossinline: Boolean
    val isNoinline: Boolean
    val isVararg: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitValueParameter(this, data)

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirValueParameter

    override fun <D> transformGetter(transformer: FirTransformer<D>, data: D): FirValueParameter

    override fun <D> transformSetter(transformer: FirTransformer<D>, data: D): FirValueParameter

    override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): FirValueParameter
}
