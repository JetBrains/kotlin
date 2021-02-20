/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer.fir

import org.jetbrains.kotlin.compiler.visualizer.FirVisualizer
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.defaultDirectives
import org.jetbrains.kotlin.visualizer.AbstractVisualizer
import org.jetbrains.kotlin.visualizer.VisualizerDirectives
import org.junit.Assert
import java.io.File

abstract class AbstractFirVisualizer : AbstractVisualizer() {
    override val frontendKind: FrontendKind<*> = FrontendKinds.FIR
    override val frontendFacade: Constructor<FrontendFacade<*>> = ::FirFrontendFacade

    override val handler: Constructor<FrontendOutputHandler<*>> = {
        object : FirAnalysisHandler(it) {
            override fun processModule(module: TestModule, info: FirOutputArtifact) {
                val renderer = FirVisualizer(info.firFiles.values.first())
                val firRenderResult = renderer.render()

                val replaceFrom = it.defaultDirectives[VisualizerDirectives.TEST_FILE_PATH].first()
                val replaceTo = it.defaultDirectives[VisualizerDirectives.EXPECTED_FILE_PATH].first()
                val path = info.firFiles.keys.first().originalFile.absolutePath.replace(replaceFrom, replaceTo)
                val expectedText = File(path).readLines()
                if (expectedText[0].startsWith("// FIR_IGNORE")) {
                    Assert.assertFalse(
                        "Files are identical, please delete ignore directive",
                        expectedText.drop(1).joinToString("\n") == firRenderResult.trim()
                    )
                    return
                }
                KotlinTestUtils.assertEqualsToFile(File(path), firRenderResult) {
                    return@assertEqualsToFile it.replace("// FIR_IGNORE\n", "")
                }
            }

            override fun processAfterAllModules(someAssertionWasFailed: Boolean) {

            }
        }
    }
}
