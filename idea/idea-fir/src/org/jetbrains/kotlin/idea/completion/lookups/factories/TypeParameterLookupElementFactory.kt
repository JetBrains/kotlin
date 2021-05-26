/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups.factories

import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.kotlin.idea.completion.lookups.UniqueLookupObject
import org.jetbrains.kotlin.idea.completion.lookups.withSymbolInfo
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol

internal class TypeParameterLookupElementFactory {
    fun KtAnalysisSession.createLookup(symbol: KtTypeParameterSymbol): LookupElementBuilder {
        return LookupElementBuilder.create(UniqueLookupObject(), symbol.name.asString())
            .let { withSymbolInfo(symbol, it) }
    }
}