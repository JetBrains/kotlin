/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.VisitedSupertype
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

abstract class FirAbstractPropertyAccessor(
    session: FirSession,
    psi: PsiElement?,
    final override val symbol: FirPropertyAccessorSymbol
) : FirAbstractAnnotatedDeclaration(session, psi), @VisitedSupertype FirPropertyAccessor {
    init {
        symbol.bind(this)
    }

    final override val receiverTypeRef: FirTypeRef? get() = null

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        return super<FirAbstractAnnotatedDeclaration>.transformChildren(transformer, data)
    }
}