/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based

import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.getKtFilesForSourceFiles
import org.jetbrains.kotlin.test.services.sourceFileProvider

abstract class FrontendApiSingleTestDataFileTest : FrontendApiTestWithTestdata() {
    final override fun getArtifact(
        module: TestModule,
        testServices: TestServices,
        ktFiles: Map<TestFile, KtFile>,
        resolveState: FirModuleResolveState
    ): FirOutputArtifact? {
        val singleFile = ktFiles.values.single()
        doTest(singleFile, module, resolveState, testServices)
        return null
    }

    abstract fun doTest(ktFile: KtFile, module: TestModule, resolveState: FirModuleResolveState, testServices: TestServices)
}