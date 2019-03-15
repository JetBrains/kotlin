/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableMember
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractCallableMember : FirAbstractMemberDeclaration, FirCallableMember {

    final override val symbol: FirBasedSymbol<FirCallableMember>
    final override var receiverTypeRef: FirTypeRef?
    final override var returnTypeRef: FirTypeRef

    constructor(
        session: FirSession,
        psi: PsiElement?,
        symbol: FirBasedSymbol<FirCallableMember>,
        name: Name,
        receiverTypeRef: FirTypeRef?,
        returnTypeRef: FirTypeRef
    ) : super(session, psi, name) {
        this.symbol = symbol
        symbol.bind(this)
        this.receiverTypeRef = receiverTypeRef
        this.returnTypeRef = returnTypeRef
    }

    constructor(
        session: FirSession,
        psi: PsiElement?,
        symbol: FirBasedSymbol<FirCallableMember>,
        name: Name,
        visibility: Visibility,
        modality: Modality?,
        isExpect: Boolean,
        isActual: Boolean,
        isOverride: Boolean,
        receiverTypeRef: FirTypeRef?,
        returnTypeRef: FirTypeRef
    ) : super(session, psi, name, visibility, modality, isExpect, isActual) {
        this.symbol = symbol
        symbol.bind(this)
        this.receiverTypeRef = receiverTypeRef
        this.returnTypeRef = returnTypeRef
        status.isOverride = isOverride
    }


    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)

        return super<FirAbstractMemberDeclaration>.transformChildren(transformer, data)
    }
}