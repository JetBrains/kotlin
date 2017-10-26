// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin

import com.intellij.debugger.streams.test.StreamChainBuilderTestCase
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.file.impl.FileManagerImpl
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.testFramework.LightPlatformTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.caches.resolve.LibraryModificationTracker
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import java.io.File
import java.nio.file.Paths
import java.util.*

/**
 * @author Vitaliy.Bibaev
 */
abstract class KotlinPsiChainBuilderTestCase(private val relativePath: String) : StreamChainBuilderTestCase() {
  override fun getTestDataPath(): String = Paths.get(File("").absolutePath, "/testData/psi/$relativeTestPath/").toString()
  override fun getFileExtension(): String = ".kt"
  abstract val kotlinChainBuilder: StreamChainBuilder

  override final fun getChainBuilder(): StreamChainBuilder = kotlinChainBuilder

  protected abstract fun doTest()

  override fun tearDown() {
    doKotlinTearDown(LightPlatformTestCase.getProject(), { super.tearDown() })
  }

  override final fun getRelativeTestPath(): String = relativePath

  override fun setUp() {
    super.setUp()
    LibraryModificationTracker.getInstance(LightPlatformTestCase.getProject()).incModificationCount()
  }

  override fun getProjectJDK(): Sdk {
    return JdkManager.mockJdk18
  }

  private fun doKotlinTearDown(project: Project, runnable: () -> Unit) {
    unInvalidateBuiltinsAndStdLib(project) {
      runnable()
    }
  }

  private fun unInvalidateBuiltinsAndStdLib(project: Project, runnable: () -> Unit) {
    val stdLibViewProviders = HashSet<KotlinDecompiledFileViewProvider>()
    val vFileToViewProviderMap = ((PsiManager.getInstance(project) as PsiManagerEx).fileManager as FileManagerImpl).vFileToViewProviderMap
    for ((file, viewProvider) in vFileToViewProviderMap) {
      if (file.isStdLibFile && viewProvider is KotlinDecompiledFileViewProvider) {
        stdLibViewProviders.add(viewProvider)
      }
    }

    runnable()

    // Base tearDown() invalidates builtins and std-lib files. Restore them with brute force.
    fun unInvalidateFile(file: PsiFileImpl) {
      val field = PsiFileImpl::class.java.getDeclaredField("myInvalidated")!!
      field.isAccessible = true
      field.set(file, false)
    }

    stdLibViewProviders.forEach {
      it.allFiles.forEach { unInvalidateFile(it as KtDecompiledFile) }
      vFileToViewProviderMap[it.virtualFile] = it
    }
  }

  private val VirtualFile.isStdLibFile: Boolean get() = presentableUrl.contains("kotlin-runtime.jar")

  abstract class Positive(relativePath: String) : KotlinPsiChainBuilderTestCase(relativePath) {
    override fun doTest() {
      val chains = buildChains()
      checkChains(chains)
    }

    protected fun checkChains(chains: MutableList<StreamChain>) {
      TestCase.assertFalse(chains.isEmpty())
    }
  }

  abstract class Negative(relativePath: String) : KotlinPsiChainBuilderTestCase(relativePath) {
    override fun doTest() {
      val elementAtCaret = configureAndGetElementAtCaret()
      TestCase.assertFalse(chainBuilder.isChainExists(elementAtCaret))
    }
  }
}
