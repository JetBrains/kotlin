/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolveExtensionInfoProvider

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider.TestScopeRenderer.renderForTests
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.resolve.extensions.KtResolveExtensionTestSupport
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.resolve.extensions.getDescription
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveExtensionInfoProviderTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        KtResolveExtensionTestSupport.configure(builder)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        analyseForTest(mainFile) {
            val resolveExtensionScope = getResolveExtensionScopeWithTopLevelDeclarations()

            val actual = resolveExtensionScope.renderSymbolsWithExtendedPsiInfo(pretty = false)
            val actualPretty = resolveExtensionScope.renderSymbolsWithExtendedPsiInfo(pretty = true)

            testServices.assertions.assertEqualsToTestDataFileSibling(actual)
            testServices.assertions.assertEqualsToTestDataFileSibling(actualPretty, extension = ".pretty.txt")
        }
    }

    context(KtAnalysisSession)
    private fun KtScope.renderSymbolsWithExtendedPsiInfo(pretty: Boolean) = prettyPrint {
        renderForTests(this@renderSymbolsWithExtendedPsiInfo, pretty) { symbol ->
            (symbol as? KtDeclarationSymbol)?.getPsiDeclarationInfo()
        }
    }

    context(KtAnalysisSession)
    private fun KtDeclarationSymbol.getPsiDeclarationInfo(): String = prettyPrint {
        val ktElement = psi as? KtElement
        val containingVirtualFile = ktElement?.containingFile?.virtualFile
        appendLine("PSI: ${ktElement?.getDescription()} [from ${containingVirtualFile?.name}]")
        if (ktElement == null || containingVirtualFile == null) {
            return@prettyPrint
        }

        withIndent {
            val isResolveExtensionFile = containingVirtualFile.isResolveExtensionFile
            appendLine("From resolve extension: $isResolveExtensionFile")

            val navTargets = ktElement.getResolveExtensionNavigationElements()
            appendLine("Resolve extension navigation targets: ${navTargets.size}")
            withIndent { navTargets.forEach { appendLine(it.toString()) } }
        }
    }.trimEnd()
}
