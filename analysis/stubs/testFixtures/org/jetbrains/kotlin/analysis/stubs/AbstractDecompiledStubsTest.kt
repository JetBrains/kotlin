/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.stubs.PsiFileStub
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile


/**
 * This test is supposed to validate the decompiled stubs output.
 *
 * It takes a [KtDecompiledFile][org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile] and creates a stub for it.
 *
 * @see AbstractCompiledStubsTest
 * @see org.jetbrains.kotlin.analysis.decompiler.psi.AbstractDecompiledTextTest
 */
abstract class AbstractDecompiledStubsTest(defaultTargetPlatform: TargetPlatform) : AbstractStubsTest() {
    override val configurator: AnalysisApiTestConfigurator = DecompiledStubsTestConfigurator(defaultTargetPlatform)
    override fun computeStub(file: KtFile): PsiFileStub<*> {
        requireIsInstance<KtDecompiledFile>(file)

        return file.calcStubTree().root
    }

    internal open class DecompiledStubsTestConfigurator(
        defaultTargetPlatform: TargetPlatform,
    ) : AbstractCompiledStubsTest.CompiledStubsTestConfigurator(defaultTargetPlatform) {
        override val testPrefixes: List<String> get() = "decompiled".let { listOf("$platformPrefix.$it", it) } + super.testPrefixes
    }
}
