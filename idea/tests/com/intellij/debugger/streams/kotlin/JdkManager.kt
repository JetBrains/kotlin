// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl

import java.io.File

/**
 * @author Vitaliy.Bibaev
 */
object JdkManager {
  private val MOCK_JDK_DIR_NAME_PREFIX = "mockJDK-"
  val JDK18_PATH: String = File("lib/" + MOCK_JDK_DIR_NAME_PREFIX + "1.8").absolutePath

  val mockJdk18: Sdk by lazy { (JavaSdk.getInstance() as JavaSdkImpl).createMockJdk("java 1.8", JDK18_PATH, false) }
}
