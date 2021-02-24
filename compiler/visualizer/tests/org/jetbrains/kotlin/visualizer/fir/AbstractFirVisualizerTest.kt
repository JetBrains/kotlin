/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.visualizer.fir

import org.jetbrains.kotlin.compiler.visualizer.FirVisualizer
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.visualizer.AbstractVisualizerTest
import org.jetbrains.kotlin.visualizer.VisualizerDirectives
import java.io.File

abstract class AbstractFirVisualizerTest : AbstractVisualizerTest() {
    override val frontendKind: FrontendKind<*> = FrontendKinds.FIR
    override val frontendFacade: Constructor<FrontendFacade<*>> = ::FirFrontendFacade

    override val handler: Constructor<FrontendOutputHandler<*>> = {
        object : FirAnalysisHandler(it) {
            override fun processModule(module: TestModule, info: FirOutputArtifact) {
                val renderer = FirVisualizer(info.firFiles.values.first())
                val firRenderResult = renderer.render()

                val replaceFrom = module.directives[VisualizerDirectives.TEST_FILE_PATH].first()
                val replaceTo = module.directives[VisualizerDirectives.EXPECTED_FILE_PATH].first()
                val path = module.files.first().originalFile.absolutePath.replace(replaceFrom, replaceTo)
                val expectedText = File(path).readLines()
                if (expectedText[0].startsWith("// FIR_IGNORE")) {
                    assertions.assertFalse(expectedText.drop(1).joinToString("\n") == firRenderResult.trim()) {
                        "Files are identical, please delete ignore directive"
                    }
                    return
                }
                assertions.assertEqualsToFile(File(path), firRenderResult) { text ->
                    text.replace("// FIR_IGNORE\n", "")
                }
            }

            override fun processAfterAllModules(someAssertionWasFailed: Boolean) {

            }
        }
    }
}
