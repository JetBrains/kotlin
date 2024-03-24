/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider

import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractContainingModuleByFileTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        analyseForTest(mainFile) {
            val fileSymbol = mainFile.getFileSymbol()
            val module = fileSymbol.getContainingModule()

            val providerModule = ProjectStructureProvider.getModule(mainFile.project, mainFile, contextualModule = null)
            assert(module == providerModule)
            assert(module == mainModule.ktModule)

            val actualString = buildString {
                append("File: ").appendLine(mainFile.name)
                append("Module: ").append(module.moduleDescription).append(" (").append(module.javaClass.simpleName).append(")")
            }

            testServices.assertions.assertEqualsToTestDataFileSibling(actualString)
        }
    }
}