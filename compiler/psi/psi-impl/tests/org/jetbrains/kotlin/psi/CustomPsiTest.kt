/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.DummyAnalysisApiTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.junit.jupiter.api.Test

class CustomPsiTest : AbstractAnalysisApiExecutionTest("testData/custom") {
    override val configurator: AnalysisApiTestConfigurator get() = DummyAnalysisApiTestConfigurator

    @Test
    @OptIn(KtExperimentalApi::class)
    fun replScriptCopy(testServices: TestServices) {
        val project = testServices.environmentManager.getProject()

        val originalRepl = KtPsiFactory(project).createReplSnippet("1 + 1")
        testServices.assertions.assertTrue(originalRepl.isReplSnippet)

        val fileCopy = originalRepl.containingKtFile.copy() as KtFile
        val scriptCopy = fileCopy.script
        testServices.assertions.assertEquals(true, scriptCopy?.isReplSnippet)
    }
}
