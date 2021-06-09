/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups.factories

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.kotlin.idea.completion.lookups.*
import org.jetbrains.kotlin.idea.completion.lookups.CallableInsertionOptions
import org.jetbrains.kotlin.idea.completion.lookups.ImportStrategy
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
                detectCallableOptions(symbol),
            )
            is KtClassLikeSymbol -> with(classLookupElementFactory) { createLookup(symbol) }
            is KtTypeParameterSymbol -> with(typeParameterLookupElementFactory) { createLookup(symbol) }
            else -> throw IllegalArgumentException("Cannot create a lookup element for $symbol")
        }
    }

    fun KtAnalysisSession.createCallableLookupElement(
        symbol: KtCallableSymbol,
        options: CallableInsertionOptions,
    ): LookupElementBuilder {
        return when (symbol) {
            is KtFunctionSymbol -> with(functionLookupElementFactory) { createLookup(symbol, options) }
            is KtVariableLikeSymbol -> with(variableLookupElementFactory) { createLookup(symbol, options) }
            else -> throw IllegalArgumentException("Cannot create a lookup element for $symbol")
        }
    }

    fun createPackagePartLookupElement(packagePartFqName: FqName): LookupElement =
        packagePartLookupElementFactory.createPackagePartLookupElement(packagePartFqName)

    fun KtAnalysisSession.createLookupElementForClassLikeSymbol(
        symbol: KtClassLikeSymbol,
        importingStrategy: ImportStrategy = detectImportStrategy(symbol)
    ): LookupElement? {
        if (symbol !is KtNamedSymbol) return null
        return with(classLookupElementFactory) { createLookup(symbol, importingStrategy) }
    }
}


