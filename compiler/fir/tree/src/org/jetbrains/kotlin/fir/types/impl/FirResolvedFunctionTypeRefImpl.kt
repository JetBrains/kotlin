/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirResolvedFunctionTypeRefImpl(
    psi: PsiElement?,
    session: FirSession,
    override val isMarkedNullable: Boolean,
    override val annotations: MutableList<FirAnnotationCall>,
    override var receiverTypeRef: FirTypeRef?,
    override val valueParameters: MutableList<FirValueParameter>,
    override var returnTypeRef: FirTypeRef,
    override val type: ConeKotlinType
) : FirResolvedFunctionTypeRef, FirAbstractElement(session, psi) {

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        receiverTypeRef = receiverTypeRef?.transformSingle(transformer, data)
        returnTypeRef = returnTypeRef.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)

        return this
    }
}
