/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.DummyAnalysisApiTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * A base class for tests that use only PSI.
 */
abstract class AbstractPsiBasedTest : AbstractAnalysisApiBasedTest() {
    override val configurator: AnalysisApiTestConfigurator get() = DummyAnalysisApiTestConfigurator

    protected open fun parseKtFile(factory: KtPsiFactory, fileName: String, content: String): KtFile {
        return factory.createFile(fileName, content)
    }

    final override fun doTest(testServices: TestServices) {
        val project = testServices.environmentManager.getProject()
        val content = testDataPath.readText().trim()

        val psiFile = parseKtFile(KtPsiFactory(project), testDataPath.fileName.name, content)
        doTest(psiFile, testServices)
    }

    abstract fun doTest(file: KtFile, testServices: TestServices)
}