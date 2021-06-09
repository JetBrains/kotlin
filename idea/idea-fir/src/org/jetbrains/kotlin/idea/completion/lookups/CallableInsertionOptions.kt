/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol

internal data class CallableInsertionOptions(
    val importingStrategy: ImportStrategy,
    val insertionStrategy: CallableInsertionStrategy,
) {
    fun withImportingStrategy(newImportStrategy: ImportStrategy): CallableInsertionOptions =
        copy(importingStrategy = newImportStrategy)

    fun withInsertionStrategy(newInsertionStrategy: CallableInsertionStrategy): CallableInsertionOptions =
        copy(insertionStrategy = newInsertionStrategy)
}

internal fun KtAnalysisSession.detectCallableOptions(symbol: KtCallableSymbol): CallableInsertionOptions = CallableInsertionOptions(
    importingStrategy = detectImportStrategy(symbol),
    insertionStrategy = when (symbol) {
        is KtFunctionSymbol -> CallableInsertionStrategy.AsCall
        else -> CallableInsertionStrategy.AsIdentifier
    }
)
