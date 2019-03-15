/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.transformSingle
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirSingleExpressionBlock(
    session: FirSession,
    private var statement: FirStatement
) : FirAbstractAnnotatedElement(session, statement.psi), FirBlock {
    override val statements
        get() = listOf(statement)

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirElement {
        statement = statement.transformSingle(transformer, data)
        return super<FirAbstractAnnotatedElement>.transformChildren(transformer, data)
    }
}