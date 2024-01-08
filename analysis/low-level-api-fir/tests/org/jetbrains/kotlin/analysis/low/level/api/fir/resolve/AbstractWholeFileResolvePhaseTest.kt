/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolve

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolveWithClearCaches
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractWholeFileResolvePhaseTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        resolveWithClearCaches(mainFile) { firResolveSession ->
            val firFile = mainFile.getOrBuildFirOfType<FirFile>(firResolveSession)
            val actual = FirRenderer.withResolvePhase().renderElementAsString(firFile)
            testServices.assertions.assertEqualsToTestDataFileSibling(actual, extension = ".lazy.resolve.txt")
        }
    }
}

abstract class AbstractSourceWholeFileResolvePhaseTest : AbstractWholeFileResolvePhaseTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractOutOfContentRootWholeFileResolvePhaseTest : AbstractWholeFileResolvePhaseTest() {
    override val configurator get() = AnalysisApiFirOutOfContentRootTestConfigurator
}

abstract class AbstractScriptWholeFileResolvePhaseTest : AbstractWholeFileResolvePhaseTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
