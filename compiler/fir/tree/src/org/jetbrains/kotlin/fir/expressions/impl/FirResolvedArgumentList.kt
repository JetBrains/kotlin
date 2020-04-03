/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.impl

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirAbstractArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirResolvedArgumentList internal constructor(
    val mapping: Map<FirExpression, FirValueParameter>
) : FirAbstractArgumentList() {
    override val arguments: List<FirExpression>
        get() = mapping.keys.toList()

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {
        for (argument in mapping.keys) {
            argument.accept(visitor, data)
        }
    }
}
