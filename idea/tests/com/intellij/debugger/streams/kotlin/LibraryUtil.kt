// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin

import java.io.File

/**
 * @author Vitaliy.Bibaev
 */
object LibraryUtil {
  val LIBRARIES_DIRECTORY: String = File("lib").absolutePath

  val KOTLIN_STD_LIBRARY = "kotlin-stdlib.jar"
}