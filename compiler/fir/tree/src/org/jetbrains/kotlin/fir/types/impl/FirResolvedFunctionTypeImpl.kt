/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedFunctionType
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirResolvedFunctionTypeImpl(
    override val psi: PsiElement?,
    override val session: FirSession,
    override val isNullable: Boolean,
    override val annotations: MutableList<FirAnnotationCall>,
    override var receiverType: FirType?,
    override val valueParameters: MutableList<FirValueParameter>,
    override var returnType: FirType,
    override val type: ConeKotlinType
) : FirResolvedFunctionType {

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        receiverType = receiverType?.transformSingle(transformer, data)
        returnType = returnType.transformSingle(transformer, data)
        valueParameters.transformInplace(transformer, data)

        return this
    }
}
