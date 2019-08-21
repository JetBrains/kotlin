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
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirPropertyAccessorImpl(
    session: FirSession,
    psi: PsiElement?,
    override val isGetter: Boolean,
    visibility: Visibility,
    override var returnTypeRef: FirTypeRef,
    symbol: FirPropertyAccessorSymbol
) : FirAbstractPropertyAccessor(session, psi, symbol) {
    override var status = FirDeclarationStatusImpl(visibility, Modality.FINAL)

    override var body: FirBlock? = null

    override val valueParameters = mutableListOf<FirValueParameter>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)
        body = body?.transformSingle(transformer, data)
        status = status.transformSingle(transformer, data)

        return super.transformChildren(transformer, data)
    }

    override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D) {
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
    }

    override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirPropertyAccessorImpl {
        return this
    }
}