/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFileScopeTest : AbstractScopeTestBase() {
    override fun KaSession.getScope(mainFile: KtFile, testServices: TestServices): KaScope = mainFile.getFileSymbol().getFileScope()

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        super.doTestByMainFile(mainFile, mainModule, testServices)

        analyseForTest(mainFile) {
            val fileSymbol = mainFile.getFileSymbol()
            val renderedFileSymbol = DebugSymbolRenderer(renderExtra = true).render(analysisSession, fileSymbol)
            testServices.assertions.assertEqualsToTestDataFileSibling(renderedFileSymbol, extension = ".file_symbol.txt")
        }
    }
}
