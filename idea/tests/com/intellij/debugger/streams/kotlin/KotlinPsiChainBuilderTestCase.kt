// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin

import com.intellij.debugger.streams.test.StreamChainBuilderTestCase
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.projectRoots.Sdk
import junit.framework.TestCase
import java.io.File
import java.nio.file.Paths

/**
 * @author Vitaliy.Bibaev
 */
abstract class KotlinPsiChainBuilderTestCase(private val relativePath: String) : StreamChainBuilderTestCase() {
  override fun getTestDataPath(): String = Paths.get(File("").absolutePath, "/testData/psi/$relativeTestPath/").toString()
  override fun getFileExtension(): String = ".kt"
  abstract val kotlinChainBuilder: StreamChainBuilder

  override final fun getChainBuilder(): StreamChainBuilder = kotlinChainBuilder

  protected abstract fun doTest()

  override final fun getRelativeTestPath(): String = relativePath

  override fun getProjectJDK(): Sdk {
    return JdkManager.mockJdk18
  }

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
