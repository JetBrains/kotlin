/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CompilationPeerCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractCompilationPeerAnalysisTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val project = mainFile.project

        val resolutionFacade = mainModule.ktModule.getResolutionFacade(project)
        val firFile = mainFile.getOrBuildFirFile(resolutionFacade)

        val compilationPeerData = CompilationPeerCollector.process(firFile)

        val filesToCompile = compilationPeerData.peers.values
            .flatten()
            .map { "File " + it.name }
            .sorted()

        val inlineClassesToCompile = compilationPeerData
            .inlinedClasses
            .map { "Class " + (it.name ?: "<anonymous[${it.textRange}]>") }

        val actualItems = filesToCompile + inlineClassesToCompile
        val actualText = actualItems.joinToString(separator = "\n")

        testServices.assertions.assertEqualsToTestOutputFile(actual = actualText)
    }
}