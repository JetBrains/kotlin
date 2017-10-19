// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.debugger.streams.kotlin.psi.java

import com.intellij.debugger.streams.kotlin.KotlinPsiChainBuilderTestCase
import com.intellij.debugger.streams.kotlin.LibraryManager
import com.intellij.debugger.streams.kotlin.lib.StreamExLibrarySupportProvider
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.PsiTestUtil

/**
 * @author Vitaliy.Bibaev
 */
class PositiveStreamExTest : KotlinPsiChainBuilderTestCase.Positive("streams/positive/streamex") {
  override val kotlinChainBuilder: StreamChainBuilder = StreamExLibrarySupportProvider().chainBuilder
  override fun setUp() {
    super.setUp()
    ApplicationManager.getApplication().runWriteAction {
      VfsRootAccess.allowRootAccess(LibraryManager.LIBRARIES_DIRECTORY)
      PsiTestUtil.addLibrary(testRootDisposable, LightPlatformTestCase.getModule(),
          "StreamEx", LibraryManager.LIBRARIES_DIRECTORY, "streamex-0.6.5.jar")
    }
  }

  override fun tearDown() {
    super.tearDown()
    ApplicationManager.getApplication().runWriteAction {
      VfsRootAccess.disallowRootAccess(LibraryManager.LIBRARIES_DIRECTORY)
    }
  }

  fun testSimple() {
    doTest()
  }
}
