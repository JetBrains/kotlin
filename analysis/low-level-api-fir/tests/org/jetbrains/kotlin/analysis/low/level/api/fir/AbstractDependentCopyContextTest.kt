/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractDependentCopyContextTest : AbstractDependentCopyTest() {
    context(LLFirResolveSessionDepended)
    override fun doDependentCopyTest(file: KtFile, element: KtElement, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val context = getTowerContextProvider(file).getClosestAvailableParentContext(element)
        testServices.assertions.assertNotNull(context)
        val actual = buildString {
            ElementContextRenderer.render(ContextCollector.Context(context!!, emptyMap()), this)
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = ".context.txt")
    }
}

abstract class AbstractSourceDependentCopyContextTest : AbstractDependentCopyContextTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractScriptDependentCopyContextTest : AbstractDependentCopyContextTest() {
    override val configurator: AnalysisApiTestConfigurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
