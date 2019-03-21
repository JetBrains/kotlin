/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirCallableReferenceAccessImpl(
    session: FirSession,
    psi: PsiElement?
) : FirAbstractQualifiedAccess(session, psi), FirCallableReferenceAccess {
    override var typeRef: FirTypeRef = FirImplicitTypeRefImpl(session, null)

    override fun replaceTypeRef(newTypeRef: FirTypeRef) {
        typeRef = newTypeRef
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        typeRef = typeRef.transformSingle(transformer, data)

        return super<FirAbstractQualifiedAccess>.transformChildren(transformer, data)
    }
}