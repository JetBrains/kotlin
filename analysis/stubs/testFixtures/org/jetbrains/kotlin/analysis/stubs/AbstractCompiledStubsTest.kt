/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtLibraryBinaryDecompiledTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.platform.TargetPlatform

/**
 * This test is supposed to validate the compiled stubs output.
 *
 * It takes a compiled file as a binary data and creates stubs for it.
 *
 * @see org.jetbrains.kotlin.analysis.decompiler.psi.AbstractDecompiledTextTest
 */
abstract class AbstractCompiledStubsTest(defaultTargetPlatform: TargetPlatform) : AbstractStubsTest() {
    override val outputFileExtension: String get() = "compiled.stubs.txt"
    override val configurator: AnalysisApiTestConfigurator = CompiledStubsTestConfigurator(defaultTargetPlatform)
    override val stubsTestEngine: StubsTestEngine get() = CompiledStubsTestEngine

    internal open class CompiledStubsTestConfigurator(
        override val defaultTargetPlatform: TargetPlatform,
    ) : AnalysisApiFirBinaryTestConfigurator() {
        override val testModuleFactory: KtTestModuleFactory
            get() = KtLibraryBinaryDecompiledTestModuleFactory
    }
}
