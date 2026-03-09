/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast
import org.junit.jupiter.api.Test

class LLScriptWithDanglingFileTest : AbstractAnalysisApiExecutionTest("testData/scriptWithDanglingFile") {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)

    /**
     * Tests that dangling file modules can have other dangling file modules as their context.
     * This is needed for Kotlin Notebooks (KTNB-1308, KT-84745).
     */
    @OptIn(KaExperimentalApi::class, UnsafeCastFunction::class)
    @Test
    fun scriptWithDanglingFileCopy(mainFile: KtFile, testServices: TestServices) {
        val assertions = testServices.assertions
        val ktPsiFactory = KtPsiFactory.contextual(mainFile, markGenerated = true, eventSystemEnabled = true)

        val firstDanglingFile = ktPsiFactory.createFile("first.kts", "val x = 1")
        val secondDanglingFile = ktPsiFactory.createFile("second.kts", "val y = 2")
        secondDanglingFile.originalFile = firstDanglingFile

        val secondModule = KotlinProjectStructureProvider.getModule(
            mainFile.project,
            secondDanglingFile,
            useSiteModule = null,
        )

        val secondDanglingModule = secondModule.assertedCast<KaDanglingFileModule> {
            "Expected KaDanglingFileModule, got ${secondModule::class.simpleName}"
        }

        val secondContextModule = secondDanglingModule.contextModule

        // The key assertion: the context module should be the first dangling module,
        // not the script module (baseContextModule).
        assertions.assertTrue(secondContextModule is KaDanglingFileModule) {
            "Expected context module to be KaDanglingFileModule, got ${secondContextModule::class.simpleName}"
        }
    }
}
