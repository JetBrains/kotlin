/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer.psi

import org.jetbrains.kotlin.compiler.visualizer.PsiVisualizer
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicFrontendAnalysisHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.visualizer.AbstractVisualizerTest
import org.jetbrains.kotlin.visualizer.VisualizerDirectives
import java.io.File

abstract class AbstractPsiVisualizerTest : AbstractVisualizerTest() {
    override val frontendKind: FrontendKind<*> = FrontendKinds.ClassicFrontend
    override val frontendFacade: Constructor<FrontendFacade<*>> = ::ClassicFrontendFacade

    override val handler: Constructor<FrontendOutputHandler<*>> = {
        object : ClassicFrontendAnalysisHandler(it) {
            override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
                val renderer = PsiVisualizer(info.ktFiles.values.first(), info.analysisResult)
                val psiRenderResult = renderer.render()

                val replaceFrom = module.directives[VisualizerDirectives.TEST_FILE_PATH].first()
                val replaceTo = module.directives[VisualizerDirectives.EXPECTED_FILE_PATH].first()
                val path = module.files.first().originalFile.absolutePath.replace(replaceFrom, replaceTo)
                assertions.assertEqualsToFile(File(path), psiRenderResult) { text ->
                    text.replace("// FIR_IGNORE\n", "")
                }
            }

            override fun processAfterAllModules(someAssertionWasFailed: Boolean) {

            }
        }
    }
}
