/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.resolveExtensionInfoProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.isResolveExtensionFile
import org.jetbrains.kotlin.analysis.api.components.resolveExtensionNavigationElements
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider.TestScopeRenderer.renderForTests
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.resolve.extensions.KtResolveExtensionTestSupport
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.resolve.extensions.getDescription
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractResolveExtensionInfoProviderTest : AbstractAnalysisApiBasedTest() {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        KtResolveExtensionTestSupport.configure(builder)
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        copyAwareAnalyzeForTest(mainFile) { _ ->
            val resolveExtensionScope = resolveExtensionScopeWithTopLevelDeclarations

            val actual = renderSymbolsWithExtendedPsiInfo(resolveExtensionScope, printPretty = false)
            val actualPretty = renderSymbolsWithExtendedPsiInfo(resolveExtensionScope, printPretty = true)

            testServices.assertions.assertEqualsToTestOutputFile(actual)
            testServices.assertions.assertEqualsToTestOutputFile(actualPretty, extension = ".pretty.txt")
        }
    }

    context(_: KaSession)
    private fun renderSymbolsWithExtendedPsiInfo(scope: KaScope, printPretty: Boolean) = prettyPrint {
        renderForTests(scope, this@prettyPrint, printPretty) { symbol ->
            if (symbol is KaDeclarationSymbol) {
                getPsiDeclarationInfo(symbol)
            } else {
                null
            }
        }
    }

    context(_: KaSession)
    private fun getPsiDeclarationInfo(symbol: KaDeclarationSymbol): String = prettyPrint {
        val ktElement = symbol.psi as? KtElement
        val containingVirtualFile = ktElement?.containingFile?.virtualFile
        appendLine("PSI: ${ktElement?.getDescription()} [from ${containingVirtualFile?.name}]")
        if (ktElement == null || containingVirtualFile == null) {
            return@prettyPrint
        }

        withIndent {
            val isResolveExtensionFile = containingVirtualFile.isResolveExtensionFile
            appendLine("From resolve extension: $isResolveExtensionFile")

            val navTargets = ktElement.resolveExtensionNavigationElements
            appendLine("Resolve extension navigation targets: ${navTargets.size}")
            withIndent { navTargets.forEach { appendLine(it.toString()) } }
        }
    }.trimEnd()
}
