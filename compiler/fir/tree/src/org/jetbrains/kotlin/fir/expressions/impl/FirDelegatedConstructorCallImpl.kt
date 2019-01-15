/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirDelegatedConstructorCallImpl(
    session: FirSession,
    psi: PsiElement?,
    override var constructedType: FirType,
    override val isThis: Boolean
) : FirAbstractCall(session, psi), FirDelegatedConstructorCall {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        constructedType = constructedType.transformSingle(transformer, data)

        return super<FirAbstractCall>.transformChildren(transformer, data)
    }
}