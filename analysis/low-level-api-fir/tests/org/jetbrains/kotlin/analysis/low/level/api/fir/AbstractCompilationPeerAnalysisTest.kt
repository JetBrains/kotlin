/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.compile.CompilationPeerCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractCompilationPeerAnalysisTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val project = mainFile.project

        val resolveSession = mainModule.ktModule.getFirResolveSession(project)
        val firFile = mainFile.getOrBuildFirFile(resolveSession)

        val compilationPeerData = CompilationPeerCollector.process(firFile)

        val actualItems = compilationPeerData.files.map { "File " + it.name }.sorted() +
                compilationPeerData.inlinedClasses.map { "Class " + it.name }

        val actualText = actualItems.joinToString(separator = "\n")

        testServices.assertions.assertEqualsToTestDataFileSibling(actual = actualText)
    }
}