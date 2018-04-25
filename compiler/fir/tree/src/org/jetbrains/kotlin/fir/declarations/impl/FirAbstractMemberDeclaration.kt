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
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractMemberDeclaration(
    session: FirSession,
    psi: PsiElement?,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    isExpect: Boolean,
    isActual: Boolean
) : FirAbstractNamedAnnotatedDeclaration(session, psi, name), FirMemberDeclaration {
    final override val typeParameters = mutableListOf<FirTypeParameter>()

    final override var status = FirDeclarationStatusImpl(
        session,
        visibility,
        modality
    ).apply {
        this.isExpect = isExpect
        this.isActual = isActual
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        typeParameters.transformInplace(transformer, data)
        status = status.transformSingle(transformer, data)

        return super<FirAbstractNamedAnnotatedDeclaration>.transformChildren(transformer, data)
    }
}