/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirAbstractElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirErrorDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirErrorFunction(
    session: FirSession,
    psi: PsiElement?,
    override val reason: String
) : FirAbstractElement(session, psi), FirErrorDeclaration, FirFunction {
    override val annotations: List<FirAnnotationCall>
        get() = emptyList()

    override val valueParameters: List<FirValueParameter>
        get() = emptyList()

    override val body: FirBlock?
        get() = null

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        super<FirFunction>.accept(visitor, data)
}