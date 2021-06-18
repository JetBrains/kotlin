/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.resolve

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based.FrontendApiSingleTestDataFileTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractInnerDeclarationsResolvePhaseTest : FrontendApiSingleTestDataFileTest() {
    override fun doTest(ktFile: KtFile, module: TestModule, resolveState: FirModuleResolveState, testServices: TestServices) {
        val firFile = ktFile.getOrBuildFirOfType<FirFile>(resolveState)

        val actual = firFile.render(FirRenderer.RenderMode.WithResolvePhases)
        KotlinTestUtils.assertEqualsToFile(testDataFileSibling(".fir.txt"), actual)
    }
}
