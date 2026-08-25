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

    /**
     * The REPL mark has to survive tree unloading, as the tree can be dropped at any moment,
     * and the reloaded tree consists of new elements.
     *
     * @see com.intellij.psi.impl.source.PsiFileImpl.loadTreeElement
     */
    @Test
    @OptIn(KtExperimentalApi::class)
    fun replScriptTreeReload(testServices: TestServices) {
        val project = testServices.environmentManager.getProject()
        val replFile = KtPsiFactory(project).createReplSnippet("val foo = 1").containingKtFile
        testServices.assertions.assertEquals(true, replFile.script?.isReplSnippet)

        replFile.setTreeElementPointer(null)
        replFile.calcTreeElement()
        testServices.assertions.assertEquals(true, replFile.script?.isReplSnippet)
    }

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
