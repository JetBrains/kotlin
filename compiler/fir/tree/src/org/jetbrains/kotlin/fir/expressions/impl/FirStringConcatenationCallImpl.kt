/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitStringTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirStringConcatenationCallImpl(
    session: FirSession,
    psi: PsiElement?
) : FirStringConcatenationCall(session, psi) {
    override fun replaceTypeRef(newTypeRef: FirTypeRef) {}

    override var typeRef: FirTypeRef = FirImplicitStringTypeRef(session, psi)

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        typeRef = typeRef.transformSingle(transformer, data)
        return super.transformChildren(transformer, data)
    }
}