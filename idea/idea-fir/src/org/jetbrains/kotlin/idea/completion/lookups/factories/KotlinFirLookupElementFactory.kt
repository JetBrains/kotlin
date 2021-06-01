/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups.factories

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.kotlin.idea.completion.lookups.CallableImportStrategy
import org.jetbrains.kotlin.idea.completion.lookups.CallableInsertionStrategy
import org.jetbrains.kotlin.idea.completion.lookups.detectImportStrategy
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.name.FqName

internal class KotlinFirLookupElementFactory {
    private val classLookupElementFactory = ClassLookupElementFactory()
    private val variableLookupElementFactory = VariableLookupElementFactory()
    private val functionLookupElementFactory = FunctionLookupElementFactory()
    private val typeParameterLookupElementFactory = TypeParameterLookupElementFactory()
    private val packagePartLookupElementFactory = PackagePartLookupElementFactory()

    fun KtAnalysisSession.createLookupElement(symbol: KtNamedSymbol): LookupElement {
        return when (symbol) {
            is KtCallableSymbol -> createCallableLookupElement(
                symbol,
                importingStrategy = detectImportStrategy(symbol),
                CallableInsertionStrategy.AS_CALL
            )
            is KtClassLikeSymbol -> with(classLookupElementFactory) { createLookup(symbol) }
            is KtTypeParameterSymbol -> with(typeParameterLookupElementFactory) { createLookup(symbol) }
            else -> throw IllegalArgumentException("Cannot create a lookup element for $symbol")
        }
    }

    fun KtAnalysisSession.createCallableLookupElement(
        symbol: KtCallableSymbol,
        importingStrategy: CallableImportStrategy,
        insertionStrategy: CallableInsertionStrategy,
    ): LookupElementBuilder {
        return when (symbol) {
            is KtFunctionSymbol -> with(functionLookupElementFactory) { createLookup(symbol, importingStrategy, insertionStrategy) }
            is KtVariableLikeSymbol -> with(variableLookupElementFactory) { createLookup(symbol, importingStrategy) }
            else -> throw IllegalArgumentException("Cannot create a lookup element for $symbol")
        }
    }

    fun createPackagePartLookupElement(packagePartFqName: FqName): LookupElement =
        packagePartLookupElementFactory.createPackagePartLookupElement(packagePartFqName)

    fun KtAnalysisSession.createLookupElementForClassLikeSymbol(symbol: KtClassLikeSymbol, insertFqName: Boolean = true): LookupElement? {
        if (symbol !is KtNamedSymbol) return null
        return with(classLookupElementFactory) { createLookup(symbol, insertFqName) }
    }
}


