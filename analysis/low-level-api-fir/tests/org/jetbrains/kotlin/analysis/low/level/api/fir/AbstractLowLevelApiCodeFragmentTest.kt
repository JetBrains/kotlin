/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceFileProvider
import java.io.File

abstract class AbstractLowLevelApiCodeFragmentTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    abstract fun doTest(ktCodeFragment: KtCodeFragment, moduleStructure: TestModuleStructure, testServices: TestServices)
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val moduleStructure = testServices.moduleStructure
        val contextElement = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtElement>(mainFile)

        val fragmentFile = moduleStructure.originalTestDataFiles.single()
            .run { File(parent, "$nameWithoutExtension.fragment.$extension") }

        val fragmentTestFile = TestFile(
            "fragment.kt",
            fragmentFile.readText(),
            fragmentFile,
            startLineNumberInOriginalFile = 0,
            isAdditional = true,
            RegisteredDirectives.Empty
        )

        val processedFragmentText = testServices.sourceFileProvider.getContentOfSourceFile(fragmentTestFile)

        val isBlockFragment = processedFragmentText.any { it == '\n' }

        val project = mainFile.project
        val factory = KtPsiFactory(project, markGenerated = false)

        val ktCodeFragment = when {
            isBlockFragment -> factory.createBlockCodeFragment(processedFragmentText, contextElement)
            else -> factory.createExpressionCodeFragment(processedFragmentText, contextElement)
        }

        doTest(ktCodeFragment, moduleStructure, testServices)
    }
}