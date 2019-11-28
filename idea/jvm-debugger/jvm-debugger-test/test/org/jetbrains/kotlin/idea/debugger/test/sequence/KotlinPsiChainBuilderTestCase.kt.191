/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.test.sequence

import com.intellij.debugger.streams.test.StreamChainBuilderTestCase
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.idea.caches.project.LibraryModificationTracker
import org.jetbrains.kotlin.idea.debugger.test.DEBUGGER_TESTDATA_PATH_BASE
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase

abstract class KotlinPsiChainBuilderTestCase(private val relativePath: String) : StreamChainBuilderTestCase() {
    override fun getTestDataPath(): String = "$DEBUGGER_TESTDATA_PATH_BASE/sequence/psi/$relativeTestPath"

    override fun getFileExtension(): String = ".kt"
    abstract val kotlinChainBuilder: StreamChainBuilder
    override fun getChainBuilder(): StreamChainBuilder = kotlinChainBuilder
    private val stdLibName = "kotlin-stdlib"

    protected abstract fun doTest()

    final override fun getRelativeTestPath(): String = relativePath

    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().runWriteAction {
            @Suppress("UnstableApiUsage")
            if (ProjectLibraryTable.getInstance(LightPlatformTestCase.getProject()).getLibraryByName(stdLibName) == null) {
                val stdLibPath = ForTestCompileRuntime.runtimeJarForTests()
                PsiTestUtil.addLibrary(
                    testRootDisposable,
                    LightPlatformTestCase.getModule(),
                    stdLibName,
                    stdLibPath.parent,
                    stdLibPath.name
                )
            }
        }
        LibraryModificationTracker.getInstance(LightPlatformTestCase.getProject()).incModificationCount()
    }


    override fun getProjectJDK(): Sdk {
        return PluginTestCaseBase.mockJdk9()
    }

    abstract class Positive(relativePath: String) : KotlinPsiChainBuilderTestCase(relativePath) {
        override fun doTest() {
            val chains = buildChains()
            checkChains(chains)
        }

        private fun checkChains(chains: MutableList<StreamChain>) {
            TestCase.assertFalse(chains.isEmpty())
        }
    }

    abstract class Negative(relativePath: String) : KotlinPsiChainBuilderTestCase(relativePath) {
        override fun doTest() {
            val elementAtCaret = configureAndGetElementAtCaret()
            TestCase.assertFalse(chainBuilder.isChainExists(elementAtCaret))
            TestCase.assertTrue(chainBuilder.build(elementAtCaret).isEmpty())
        }
    }
}
