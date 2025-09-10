/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider

import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.analysisContextModule
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractContainingModuleByFileTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    object Directives : SimpleDirectivesContainer() {
        // Note that there's already a global directive `ANALYSIS_CONTEXT_MODULE` which sets a dangling file's context module directly, so
        // we cannot use that name here.
        val VIRTUAL_FILE_ANALYSIS_CONTEXT_MODULE by stringDirective(
            "Sets the `analysisContextModule` of the file to the given module.",
            applicability = DirectiveApplicability.File,
        )
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        assignAnalysisContextModuleIfNecessary(mainFile, mainModule, testServices)

        analyzeForTest(mainFile) {
            val fileSymbol = mainFile.symbol
            val module = fileSymbol.containingModule

            val providerModule = KotlinProjectStructureProvider.getModule(mainFile.project, mainFile, useSiteModule = null)
            assert(module == providerModule)

            val actualString = buildString {
                append("File: ").appendLine(mainFile.name)
                append("Module: ").append(module.moduleDescription).append(" (").append(module.javaClass.simpleName).append(")")
            }

            testServices.assertions.assertEqualsToTestOutputFile(actualString)
        }
    }

    private fun assignAnalysisContextModuleIfNecessary(
        mainFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ) {
        val mainTestFile = mainModule.getKtTestFile(mainFile).testFile
        val analysisContextModuleName = mainTestFile.directives[Directives.VIRTUAL_FILE_ANALYSIS_CONTEXT_MODULE].singleOrNull()

        if (analysisContextModuleName != null) {
            val contextModule = testServices.ktTestModuleStructure.getKtTestModule(analysisContextModuleName).ktModule
            mainFile.virtualFile.analysisContextModule = contextModule
        }
    }
}
