// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.lib.collections

import org.jetbrains.kotlin.idea.debugger.sequence.lib.LibraryUtil
import org.jetbrains.kotlin.idea.debugger.sequence.psi.collections.KotlinCollectionChainBuilder
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinCollectionsPeekCallFactory
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinStatementFactory
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.KotlinTraceExpressionBuilder
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