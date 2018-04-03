/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.expressions.FirBody
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

open class FirConstructorImpl(
    session: FirSession,
    psi: PsiElement?,
    final override val visibility: Visibility,
    final override var delegatedConstructor: FirDelegatedConstructorCall?,
    body: FirBody?
) : FirAbstractFunction(session, psi, body), FirConstructor {

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        delegatedConstructor = delegatedConstructor?.transformSingle(transformer, data)

        return super<FirAbstractFunction>.transformChildren(transformer, data)
    }
}