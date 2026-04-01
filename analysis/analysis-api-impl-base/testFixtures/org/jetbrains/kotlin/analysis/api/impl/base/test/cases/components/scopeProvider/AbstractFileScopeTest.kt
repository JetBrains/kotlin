/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.fileScope
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFileScopeTest : AbstractScopeTestBase() {
    context(_: KaSession)
    override fun getScope(mainFile: KtFile, testServices: TestServices): KaScope = mainFile.symbol.fileScope

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        super.doTestByMainFile(mainFile, mainModule, testServices)

        analyzeForTest(mainFile) {
            val fileSymbol = mainFile.symbol
            val renderedFileSymbol = KaDebugRenderer(renderExtra = true).render(useSiteSession, fileSymbol)
            testServices.assertions.assertEqualsToTestOutputFile(renderedFileSymbol, extension = ".file_symbol.txt")
        }
    }
}
