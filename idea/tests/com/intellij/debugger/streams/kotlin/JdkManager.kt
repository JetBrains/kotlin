/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
