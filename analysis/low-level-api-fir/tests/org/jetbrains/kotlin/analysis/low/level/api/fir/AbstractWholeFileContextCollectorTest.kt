/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.util.descendants
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirCustomScriptDefinitionTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector.FilterResponse
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.getElementTextWithContext

abstract class AbstractWholeFileContextCollectorTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val resolutionFacade = mainModule.ktModule.getResolutionFacade(mainFile.project)
        val firFile = mainFile.getOrBuildFirFile(resolutionFacade)

        val firRenderer = FirRenderer.withResolvePhase()

        val actualText = buildString {
            val contextProvider = ContextCollector.process(
                firFile,
                designation = null,
                preferBodyContext = false,
                shouldTriggerBodyAnalysis = true,
                filter = { FilterResponse.CONTINUE }
            )

            var isFirst = true

            for (descendant in mainFile.descendants()) {
                val descendantContext = contextProvider[descendant, ContextCollector.ContextKind.SELF] ?: continue

                if (isFirst) {
                    isFirst = false
                } else {
                    appendLine()
                }

                append("--- ").append(descendant.javaClass.simpleName).appendLine(" ---")
                append(getElementTextWithContext(descendant)).append(" [").append(descendant.textRange).appendLine("]:")
                ElementContextRenderer.render(descendantContext, this)
            }

            appendLine()
            append(firRenderer.renderElementAsString(firFile, trim = true))
        }

        testServices.assertions.assertEqualsToTestOutputFile(actualText, extension = ".wholeFile.txt")
    }
}

abstract class AbstractWholeSourceFileContextCollectorTest : AbstractWholeFileContextCollectorTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractWholeScriptFileContextCollectorTest : AbstractWholeFileContextCollectorTest() {
    override val configurator: AnalysisApiTestConfigurator =
        AnalysisApiFirCustomScriptDefinitionTestConfigurator(analyseInDependentSession = false)
}