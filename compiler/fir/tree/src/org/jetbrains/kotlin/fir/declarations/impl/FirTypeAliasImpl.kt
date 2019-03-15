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
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirTypeAliasImpl(
    session: FirSession,
    psi: PsiElement?,
    override val symbol: FirTypeAliasSymbol,
    name: Name,
    visibility: Visibility,
    isExpect: Boolean,
    isActual: Boolean,
    override var expandedTypeRef: FirTypeRef
) : FirAbstractMemberDeclaration(session, psi, name, visibility, Modality.FINAL, isExpect, isActual), FirTypeAlias {

    init {
        symbol.bind(this)
    }

    override fun replaceExpandTypeRef(typeRef: FirTypeRef): FirTypeAlias {
        expandedTypeRef = typeRef
        return this
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        expandedTypeRef = expandedTypeRef.transformSingle(transformer, data)

        return super<FirAbstractMemberDeclaration>.transformChildren(transformer, data)
    }
}
