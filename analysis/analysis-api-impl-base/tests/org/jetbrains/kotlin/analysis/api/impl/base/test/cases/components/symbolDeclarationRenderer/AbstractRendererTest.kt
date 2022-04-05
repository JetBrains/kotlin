/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationRenderer

import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractRendererTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val options = KtDeclarationRendererOptions.DEFAULT.copy(
            approximateTypes = true,
            renderClassMembers = true,
            typeRendererOptions = KtTypeRendererOptions.SHORT_NAMES,
            sortNestedDeclarations = true
        )

        val actual = executeOnPooledThreadInReadAction {
            buildString {
                ktFile.declarations.forEach { declaration ->
                    analyseForTest(declaration) {
                        val symbol = declaration.getSymbol() as? KtDeclarationSymbol ?: return@analyseForTest
                        append(symbol.render(options))
                        appendLine()
                        appendLine()
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = ".rendered")
    }
}
