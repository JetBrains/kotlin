/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class FirTypeParameterImpl(
    session: FirSession,
    psi: PsiElement?,
    override val symbol: FirTypeParameterSymbol,
    name: Name,
    override val variance: Variance,
    override val isReified: Boolean
) : FirAbstractNamedAnnotatedDeclaration(session, psi, name), FirTypeParameter {
    init {
        symbol.bind(this)
    }

    override val bounds = mutableListOf<FirTypeRef>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        bounds.transformInplace(transformer, data)

        return super<FirAbstractNamedAnnotatedDeclaration>.transformChildren(transformer, data)
    }
}