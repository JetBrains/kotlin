/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolsData
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractScopeTestBase : AbstractSymbolByFqNameTest() {
    protected abstract fun KtAnalysisSession.getScope(mainFile: KtFile, testServices: TestServices): KtScope

    protected open fun KtAnalysisSession.getSymbolsFromScope(scope: KtScope): Sequence<KtDeclarationSymbol> = scope.getAllSymbols()

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        super.doTestByMainFile(mainFile, mainModule, testServices)

        analyseForTest(mainFile) {
            val scope = getScope(mainFile, testServices)
            val actualNames = prettyPrint { renderNamesContainedInScope(scope) }
            testServices.assertions.assertEqualsToTestDataFileSibling(actualNames, extension = ".names.txt")
        }
    }

    override fun KtAnalysisSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData =
        SymbolsData(getSymbolsFromScope(getScope(ktFile, testServices)).toList())
}
