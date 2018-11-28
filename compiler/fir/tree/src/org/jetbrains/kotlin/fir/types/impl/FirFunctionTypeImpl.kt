/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirFunctionType
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirFunctionTypeImpl(
    session: FirSession,
    psi: PsiElement?,
    isNullable: Boolean,
    override var receiverType: FirType?,
    override var returnType: FirType
) : FirAbstractAnnotatedType(session, psi, isNullable), FirFunctionType {
    override val valueParameters = mutableListOf<FirValueParameter>()

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        receiverType = receiverType?.transformSingle(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)

        return super<FirAbstractAnnotatedType>.transformChildren(transformer, data)
    }
}