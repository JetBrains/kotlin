/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.psi.KtExpression

private inline fun <reified T : KaParameterSymbol> Map<KtExpression, KaVariableSignature<KaParameterSymbol>>.toSpecializedArgumentMapping(): Map<KtExpression, KaVariableSignature<T>> {
    val filteredMap = if (isEmpty()) {
        this
    } else {
        filterTo(linkedMapOf()) {
            it.value.symbol is T
        }
    }

    @Suppress("UNCHECKED_CAST")
    return filteredMap as Map<KtExpression, KaVariableSignature<T>>
}

internal fun Map<KtExpression, KaVariableSignature<KaParameterSymbol>>.toValueArgumentMapping(): Map<KtExpression, KaVariableSignature<KaValueParameterSymbol>> {
    return toSpecializedArgumentMapping()
}

internal fun Map<KtExpression, KaVariableSignature<KaParameterSymbol>>.toContextArgumentMapping(): Map<KtExpression, KaVariableSignature<KaContextParameterSymbol>> {
    return toSpecializedArgumentMapping()
}
