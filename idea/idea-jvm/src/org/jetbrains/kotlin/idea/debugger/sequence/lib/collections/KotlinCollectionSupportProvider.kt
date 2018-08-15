// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.lib.collections

import com.intellij.debugger.streams.kotlin.lib.LibraryUtil
import com.intellij.debugger.streams.kotlin.psi.collections.KotlinCollectionChainBuilder
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinCollectionsPeekCallFactory
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinStatementFactory
import com.intellij.debugger.streams.kotlin.trace.impl.KotlinTraceExpressionBuilder
import com.intellij.debugger.streams.lib.LibrarySupport
import com.intellij.debugger.streams.lib.LibrarySupportProvider
import com.intellij.debugger.streams.trace.TraceExpressionBuilder
import com.intellij.debugger.streams.trace.dsl.impl.DslImpl
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.project.Project

/**
 * @author Vitaliy.Bibaev
 */
class KotlinCollectionSupportProvider : LibrarySupportProvider {
  private companion object {
    val builder: StreamChainBuilder = KotlinCollectionChainBuilder()
    val support: LibrarySupport = KotlinCollectionLibrarySupport()
    val dsl = DslImpl(KotlinStatementFactory(KotlinCollectionsPeekCallFactory()))
  }

  override fun getLanguageId(): String = LibraryUtil.KOTLIN_LANGUAGE_ID

  override fun getChainBuilder(): StreamChainBuilder = builder

  override fun getLibrarySupport(): LibrarySupport = support

  override fun getExpressionBuilder(project: Project): TraceExpressionBuilder
      = KotlinTraceExpressionBuilder(dsl, support.createHandlerFactory(dsl))
}