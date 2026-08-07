/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import org.jetbrains.kotlin.analysis.stubs.AbstractCompiledStubsTest
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

/**
 * This test is supposed to validate a decompiler text output
 *
 * @see org.jetbrains.kotlin.analysis.stubs.AbstractCompiledStubsTest
 */
abstract class AbstractDecompiledTextTest(defaultTargetPlatform: TargetPlatform) : AbstractAnalysisApiBasedTest() {
    override val configurator: AnalysisApiTestConfigurator = AbstractCompiledStubsTest.CompiledStubsTestConfigurator(defaultTargetPlatform)

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        checkDecompiledText(mainModule.ktFiles, testServices)
    }
}
