/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirAnonymousFunctionImpl(
    session: FirSession,
    psi: PsiElement?,
    override var returnType: FirType,
    override var receiverType: FirType?
) : FirAbstractFunction(session, psi), FirAnonymousFunction, FirModifiableFunction {
    override var label: FirLabel? = null

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        returnType = returnType.transformSingle(transformer, data)
        receiverType = receiverType?.transformSingle(transformer, data)
        label = label?.transformSingle(transformer, data)
        return super<FirAbstractFunction>.transformChildren(transformer, data)
    }
}