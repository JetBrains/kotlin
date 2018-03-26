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
import org.jetbrains.kotlin.fir.declarations.FirMemberPlatformStatus
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

class FirTypeAliasImpl(
    session: FirSession,
    psi: PsiElement?,
    override val symbol: FirTypeAliasSymbol,
    name: Name,
    visibility: Visibility,
    platformStatus: FirMemberPlatformStatus,
    override var expandedType: FirType
) : FirAbstractMemberDeclaration(session, psi, name, visibility, Modality.FINAL, platformStatus), FirTypeAlias {

    init {
        symbol.bind(this)
    }

    override val modality: Modality
        get() = super.modality ?: Modality.FINAL

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        annotations.transformInplace(transformer, data)
        typeParameters.transformInplace(transformer, data)
        expandedType = expandedType.transformSingle(transformer, data)

        return this
    }
}