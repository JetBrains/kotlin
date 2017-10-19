// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.lib

import com.intellij.debugger.streams.lib.LibrarySupport
import com.intellij.debugger.streams.lib.LibrarySupportProvider
import com.intellij.debugger.streams.trace.TraceExpressionBuilder
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.project.Project

/**
 * @author Vitaliy.Bibaev
 */
class KotlinSequenceSupportProvider : LibrarySupportProvider {
  override fun getLanguageId(): String = LibraryUtil.KOTLIN_LANGUAGE_ID

  override fun getChainBuilder(): StreamChainBuilder {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getLibrarySupport(): LibrarySupport {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getExpressionBuilder(project: Project): TraceExpressionBuilder = TODO()
}