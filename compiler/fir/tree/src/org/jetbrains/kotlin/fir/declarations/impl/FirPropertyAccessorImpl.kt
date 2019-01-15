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
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirPropertyAccessorImpl(
    session: FirSession,
    psi: PsiElement?,
    override val isGetter: Boolean,
    visibility: Visibility,
    override var returnType: FirType
) : FirAbstractFunction(session, psi), FirPropertyAccessor {
    override var status = FirDeclarationStatusImpl(
        session, visibility, Modality.FINAL
    )

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        returnType = returnType.transformSingle(transformer, data)
        status = status.transformSingle(transformer, data)

        return super<FirAbstractFunction>.transformChildren(transformer, data)
    }
}