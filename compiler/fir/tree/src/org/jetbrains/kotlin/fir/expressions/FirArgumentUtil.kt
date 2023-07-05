/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentListForErrorCall
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentListImpl

fun buildUnaryArgumentList(argument: FirExpression): FirArgumentList = buildArgumentList {
    arguments += argument
}

fun buildBinaryArgumentList(left: FirExpression, right: FirExpression): FirArgumentList = buildArgumentList {
    arguments += left
    arguments += right
}

fun buildResolvedArgumentList(
    mapping: LinkedHashMap<FirExpression, FirValueParameter>,
    source: KtSourceElement? = null
): FirResolvedArgumentList =
    FirResolvedArgumentListImpl(source, mapping)

fun buildArgumentListForErrorCall(
    original: FirArgumentList,
    mapping: Map<FirExpression, FirValueParameter?>
): FirArgumentList {
    return FirResolvedArgumentListForErrorCall(
        original.source,
        original.arguments.map { key -> key to mapping[key] }.toMap(LinkedHashMap())
    )
}

object FirEmptyArgumentList : FirAbstractArgumentList() {
    override val arguments: List<FirExpression>
        get() = emptyList()

    override val source: KtSourceElement?
        get() = null
}
