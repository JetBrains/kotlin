/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import org.jetbrains.kotlin.analysis.decompiler.psi.checkDecompiledText
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtLibraryBinaryDecompiledTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices

/**
 * This test is supposed to validate the compiled stubs output.
 *
 * It takes a compiled file as a binary data and creates stubs for it.
 *
 * The decompiled text is dumped and validated by the very same run, see [checkDecompiledText].
 */
abstract class AbstractCompiledStubsTest(defaultTargetPlatform: TargetPlatform) : AbstractStubsTest() {
    override val outputFileExtension: String get() = "compiled.stubs.txt"
    override val configurator: AnalysisApiTestConfigurator = CompiledStubsTestConfigurator(defaultTargetPlatform)
    override val stubsTestEngine: StubsTestEngine get() = CompiledStubsTestEngine

    /**
     * [checkDecompiledText] is embedded into the same class to avoid overhead on reading and decompiling the very same
     * binaries once again. It needs nothing the stubs run has not prepared already, so it is performed right after the stubs.
     *
     * The order matters for readability of failures: the decompiled text is rendered from the compiled stub tree
     * ([org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile]), so a stub mismatch is the cause and the text mismatch is
     * its consequence.
     */
    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        super.doTestByMainModuleAndOptionalMainFile(mainFile, mainModule, testServices)
        checkDecompiledText(mainModule.ktFiles, testServices)
    }

    internal open class CompiledStubsTestConfigurator(
        override val defaultTargetPlatform: TargetPlatform,
    ) : AnalysisApiFirBinaryTestConfigurator() {
        override val testModuleFactory: KtTestModuleFactory
            get() = KtLibraryBinaryDecompiledTestModuleFactory
    }
}
