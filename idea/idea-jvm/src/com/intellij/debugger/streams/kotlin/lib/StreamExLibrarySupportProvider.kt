// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.lib

import com.intellij.debugger.streams.kotlin.psi.impl.KotlinJavaStreamChainBuilder
import com.intellij.debugger.streams.kotlin.psi.impl.PackageBasedCallChecker
import com.intellij.debugger.streams.kotlin.trace.dsl.JavaPeekCallFactory
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinStatementFactory
import com.intellij.debugger.streams.kotlin.trace.impl.KotlinTraceExpressionBuilder
import com.intellij.debugger.streams.lib.LibrarySupport
import com.intellij.debugger.streams.lib.LibrarySupportProvider
import com.intellij.debugger.streams.lib.impl.StreamExLibrarySupport
import com.intellij.debugger.streams.trace.TraceExpressionBuilder
import com.intellij.debugger.streams.trace.dsl.impl.DslImpl
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.project.Project

/**
 * @author Vitaliy.Bibaev
 */
class StreamExLibrarySupportProvider : LibrarySupportProvider {
  private companion object {
    val streamChainBuilder = KotlinJavaStreamChainBuilder(PackageBasedCallChecker("one.util.streamex"))
    val support = StreamExLibrarySupport()
    val dsl = DslImpl(KotlinStatementFactory(JavaPeekCallFactory()))
    val expressionBuilder = KotlinTraceExpressionBuilder(dsl, support.createHandlerFactory(dsl))
  }

  override fun getLanguageId(): String = LibraryUtil.KOTLIN_LANGUAGE_ID

  override fun getChainBuilder(): StreamChainBuilder = streamChainBuilder

  override fun getLibrarySupport(): LibrarySupport = support

  override fun getExpressionBuilder(project: Project): TraceExpressionBuilder = expressionBuilder
}