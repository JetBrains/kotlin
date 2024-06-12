/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaExperimentalApi::class)

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.SymbolsData
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractScopeTestBase : AbstractSymbolByFqNameTest() {
    protected abstract fun KaSession.getScope(mainFile: KtFile, testServices: TestServices): KaScope

    protected open fun KaSession.getSymbolsFromScope(scope: KaScope): Sequence<KaDeclarationSymbol> = scope.declarations

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        super.doTestByMainFile(mainFile, mainModule, testServices)

        analyseForTest(mainFile) {
            val scope = getScope(mainFile, testServices)
            val actualNames = prettyPrint { renderNamesContainedInScope(scope) }
            testServices.assertions.assertEqualsToTestDataFileSibling(actualNames, extension = ".names.txt")
        }
    }

    override fun KaSession.collectSymbols(ktFile: KtFile, testServices: TestServices): SymbolsData =
        SymbolsData(getSymbolsFromScope(getScope(ktFile, testServices)).toList())
}
