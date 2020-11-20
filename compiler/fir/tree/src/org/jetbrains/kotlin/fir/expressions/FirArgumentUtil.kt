/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirArraySetArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList

fun buildUnaryArgumentList(argument: FirExpression): FirArgumentList = buildArgumentList {
    arguments += argument
}

fun buildBinaryArgumentList(left: FirExpression, right: FirExpression): FirArgumentList = buildArgumentList {
    arguments += left
    arguments += right
}

fun buildArraySetArgumentList(rValue: FirExpression, indexes: List<FirExpression>): FirArgumentList =
    FirArraySetArgumentList(rValue, indexes)

fun buildResolvedArgumentList(mapping: LinkedHashMap<FirExpression, FirValueParameter>): FirArgumentList =
    FirResolvedArgumentList(mapping)

object FirEmptyArgumentList : FirAbstractArgumentList() {
    override val arguments: List<FirExpression>
        get() = emptyList()
}
