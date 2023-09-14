/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.SymbolByFqName
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolsData
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractMemberScopeTestBase : AbstractSymbolByFqNameTest() {
    context(KtAnalysisSession)
    protected abstract fun KtSymbolWithMembers.getSymbolsFromScope(): Sequence<KtDeclarationSymbol>

    override fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData {
        val symbolData = SymbolByFqName.getSymbolDataFromFile(testDataPath)
        val symbols = with(symbolData) { toSymbols(ktFile) }
        val symbolWithMembers = symbols.singleOrNull() as? KtSymbolWithMembers
            ?: error("Should be a single `${KtSymbolWithMembers::class.simpleName}`, but $symbols found.")
        return SymbolsData(symbolWithMembers.getSymbolsFromScope().toList())
    }
}

abstract class AbstractMemberScopeTest : AbstractMemberScopeTestBase() {
    context(KtAnalysisSession)
    override fun KtSymbolWithMembers.getSymbolsFromScope(): Sequence<KtDeclarationSymbol> = getMemberScope().getAllSymbols()
}

abstract class AbstractStaticMemberScopeTest : AbstractMemberScopeTestBase() {
    context(KtAnalysisSession)
    override fun KtSymbolWithMembers.getSymbolsFromScope(): Sequence<KtDeclarationSymbol> = getStaticMemberScope().getAllSymbols()
}

abstract class AbstractDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    context(KtAnalysisSession)
    override fun KtSymbolWithMembers.getSymbolsFromScope(): Sequence<KtDeclarationSymbol> = getDeclaredMemberScope().getAllSymbols()
}

abstract class AbstractStaticDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    context(KtAnalysisSession)
    override fun KtSymbolWithMembers.getSymbolsFromScope(): Sequence<KtDeclarationSymbol> = getStaticDeclaredMemberScope().getAllSymbols()
}

abstract class AbstractCombinedDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    context(KtAnalysisSession)
    override fun KtSymbolWithMembers.getSymbolsFromScope(): Sequence<KtDeclarationSymbol> = getCombinedDeclaredMemberScope().getAllSymbols()
}

abstract class AbstractDelegateMemberScopeTest : AbstractMemberScopeTestBase() {
    context(KtAnalysisSession)
    override fun KtSymbolWithMembers.getSymbolsFromScope(): Sequence<KtDeclarationSymbol> = getDelegatedMemberScope().getCallableSymbols()
}
