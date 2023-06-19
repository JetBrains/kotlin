/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

abstract class AbstractLowLevelApiCodeFragmentTest : AbstractLowLevelApiSingleFileTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    abstract fun doTest(ktCodeFragment: KtCodeFragment, moduleStructure: TestModuleStructure, testServices: TestServices)

    final override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val contextElement = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtDeclaration>(ktFile)

        val fragmentText = moduleStructure.originalTestDataFiles.single()
            .run { File(parent, "$nameWithoutExtension.fragment.$extension") }
            .readText()

        val isBlockFragment = fragmentText.any { it == '\n' }

        val project = ktFile.project
        val factory = KtPsiFactory(project, markGenerated = false)

        val ktCodeFragment = when {
            isBlockFragment -> factory.createBlockCodeFragment(fragmentText, contextElement)
            else -> factory.createExpressionCodeFragment(fragmentText, contextElement)
        }

        doTest(ktCodeFragment, moduleStructure, testServices)
    }
}