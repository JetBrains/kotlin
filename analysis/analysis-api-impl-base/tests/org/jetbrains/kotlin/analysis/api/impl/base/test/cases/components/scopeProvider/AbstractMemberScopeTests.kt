/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.SymbolByFqName
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolsData
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractMemberScopeTestBase : AbstractSymbolByFqNameTest() {
    protected abstract fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope

    protected open fun KtAnalysisSession.getSymbolsFromScope(scope: KtScope): Sequence<KtDeclarationSymbol> = scope.getAllSymbols()

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        super.doTestByMainFile(mainFile, mainModule, testServices)

        analyseForTest(mainFile) {
            val memberScope = getScope(getSymbolWithMembers(mainFile))
            val actualNames = prettyPrint { renderNamesContainedInScope(memberScope) }
            testServices.assertions.assertEqualsToTestDataFileSibling(actualNames, extension = ".names.txt")
        }
    }

    override fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData =
        SymbolsData(getSymbolsFromScope(getScope(getSymbolWithMembers(ktFile))).toList())

    private fun KtAnalysisSession.getSymbolWithMembers(ktFile: KtFile): KtSymbolWithMembers {
        val symbolData = SymbolByFqName.getSymbolDataFromFile(testDataPath)
        val symbols = with(symbolData) { toSymbols(ktFile) }
        return symbols.singleOrNull() as? KtSymbolWithMembers
            ?: error("Should be a single `${KtSymbolWithMembers::class.simpleName}`, but $symbols found.")
    }
}

abstract class AbstractMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getMemberScope()
}

abstract class AbstractStaticMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getStaticMemberScope()
}

abstract class AbstractDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getDeclaredMemberScope()
}

abstract class AbstractStaticDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getStaticDeclaredMemberScope()
}

abstract class AbstractCombinedDeclaredMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getCombinedDeclaredMemberScope()
}

abstract class AbstractDelegateMemberScopeTest : AbstractMemberScopeTestBase() {
    override fun KtAnalysisSession.getScope(symbol: KtSymbolWithMembers): KtScope = symbol.getDelegatedMemberScope()

    override fun KtAnalysisSession.getSymbolsFromScope(scope: KtScope): Sequence<KtDeclarationSymbol> = scope.getCallableSymbols()
}
