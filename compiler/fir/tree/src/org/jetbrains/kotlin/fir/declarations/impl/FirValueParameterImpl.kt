/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirValueParameterImpl(
    session: FirSession,
    psi: PsiElement?,
    name: Name,
    override var returnType: FirType,
    override var defaultValue: FirExpression?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean,
    override val isVararg: Boolean
) : FirAbstractNamedAnnotatedDeclaration(session, psi, name), FirValueParameter {
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        returnType = returnType.transformSingle(transformer, data)
        defaultValue = defaultValue?.transformSingle(transformer, data)

        return super<FirAbstractNamedAnnotatedDeclaration>.transformChildren(transformer, data)
    }
}