/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubTreeLoader
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirBinaryTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtLibraryBinaryDecompiledTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile

/**
 * This test is supposed to validate the compiled stubs output.
 *
 * It takes a compiled file as a binary data and creates stubs for it.
 *
 * @see AbstractDecompiledStubsTest
 */
abstract class AbstractCompiledStubsTest(defaultTargetPlatform: TargetPlatform) : AbstractStubsTest() {
    override val configurator: AnalysisApiTestConfigurator = CompiledStubsTestConfigurator(defaultTargetPlatform)

    override fun computeStub(file: KtFile): PsiFileStub<*>? = ClsClassFinder.allowMultifileClassPart {
        requireIsInstance<KtDecompiledFile>(file)

        // The tree loader is called to build a stub tree for a binary file directly
        StubTreeLoader.getInstance()
            .build(/* project = */ null, /* vFile = */ file.virtualFile, /* psiFile = */ null)
            ?.root
            ?.let { it as PsiFileStub<*> }
    }

    internal open class CompiledStubsTestConfigurator(
        override val defaultTargetPlatform: TargetPlatform,
    ) : AnalysisApiFirBinaryTestConfigurator() {
        val platformPrefix: String get() = defaultTargetPlatform.single().platformName

        override val testModuleFactory: KtTestModuleFactory get() = KtLibraryBinaryDecompiledTestModuleFactory
        override val testPrefixes: List<String> get() = "compiled".let { listOf("$platformPrefix.$it", it) }
    }
}
