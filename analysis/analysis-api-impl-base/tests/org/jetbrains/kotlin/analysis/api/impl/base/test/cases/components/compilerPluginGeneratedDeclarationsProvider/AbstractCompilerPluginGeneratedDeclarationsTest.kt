/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.compilerPluginGeneratedDeclarationsProvider

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.scopeProvider.TestScopeRenderer.renderForTests
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractCompilerPluginGeneratedDeclarationsTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        analyseForTest(mainFile) {
            val resolveExtensionScope = mainModule.ktModule.compilerPluginGeneratedDeclarations.topLevelDeclarationsScope

            val actual = prettyPrint { renderForTests(resolveExtensionScope, this, printPretty = false) }
            val actualPretty = prettyPrint { renderForTests(resolveExtensionScope, this, printPretty = true) }

            testServices.assertions.assertEqualsToTestDataFileSibling(actual)
            testServices.assertions.assertEqualsToTestDataFileSibling(actualPretty, extension = ".pretty.txt")
        }
    }
}