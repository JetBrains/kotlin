/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer.psi

import org.jetbrains.kotlin.compiler.visualizer.PsiVisualizer
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicFrontendAnalysisHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.defaultDirectives
import org.jetbrains.kotlin.visualizer.AbstractVisualizer
import org.jetbrains.kotlin.visualizer.VisualizerDirectives
import java.io.File

abstract class AbstractPsiVisualizer : AbstractVisualizer() {
    override val frontendKind: FrontendKind<*> = FrontendKinds.ClassicFrontend
    override val frontendFacade: Constructor<FrontendFacade<*>> = { ClassicFrontendFacade(it) }

    override val handler: Constructor<FrontendOutputHandler<*>> = {
        object : ClassicFrontendAnalysisHandler(it) {
            override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
                val renderer = PsiVisualizer(info.ktFiles.values.first(), info.analysisResult)
                val psiRenderResult = renderer.render()

                val replaceFrom = it.defaultDirectives[VisualizerDirectives.TEST_FILE_PATH].first()
                val replaceTo = it.defaultDirectives[VisualizerDirectives.EXPECTED_FILE_PATH].first()
                val path = info.ktFiles.keys.first().originalFile.absolutePath.replace(replaceFrom, replaceTo)
                KotlinTestUtils.assertEqualsToFile(File(path), psiRenderResult) {
                    return@assertEqualsToFile it.replace("// FIR_IGNORE\n", "")
                }
            }

            override fun processAfterAllModules(someAssertionWasFailed: Boolean) {

            }
        }
    }
}
