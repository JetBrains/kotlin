/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import junit.framework.TestCase
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.RawFirBuilderMode
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based.FrontendApiSingleTestDataFileTest
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractFirLazyBodiesCalculatorTest : FrontendApiSingleTestDataFileTest() {

    override fun doTest(ktFile: KtFile, module: TestModule, resolveState: FirModuleResolveState, testServices: TestServices) {
        val session = resolveState.rootModuleSession
        val provider = session.firIdeProvider.kotlinScopeProvider

        val laziedFirFile = RawFirBuilder(session, provider, RawFirBuilderMode.LAZY_BODIES).buildFirFile(ktFile)
        FirLazyBodiesCalculator.calculateLazyBodies(laziedFirFile)
        val fullFirFile = RawFirBuilder(session, provider, RawFirBuilderMode.NORMAL).buildFirFile(ktFile)

        val laziedFirFileDump = StringBuilder().also { FirRenderer(it).visitFile(laziedFirFile) }.toString()
        val fullFirFileDump = StringBuilder().also { FirRenderer(it).visitFile(fullFirFile) }.toString()

        TestCase.assertEquals(laziedFirFileDump, fullFirFileDump)
    }

}