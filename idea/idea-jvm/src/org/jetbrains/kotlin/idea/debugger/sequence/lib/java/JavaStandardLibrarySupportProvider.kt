// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.lib.java

import com.intellij.debugger.streams.lib.LibrarySupport
import com.intellij.debugger.streams.lib.LibrarySupportProvider
import com.intellij.debugger.streams.lib.impl.StandardLibrarySupport
import com.intellij.debugger.streams.trace.TraceExpressionBuilder
import com.intellij.debugger.streams.trace.dsl.impl.DslImpl
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.debugger.sequence.psi.impl.KotlinChainTransformerImpl
import org.jetbrains.kotlin.idea.debugger.sequence.psi.impl.PackageBasedCallChecker
import org.jetbrains.kotlin.idea.debugger.sequence.psi.impl.TerminatedChainBuilder
import org.jetbrains.kotlin.idea.debugger.sequence.psi.java.JavaStreamChainTypeExtractor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.java.StandardLibraryCallChecker
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.JavaPeekCallFactory
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinStatementFactory
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.KotlinTraceExpressionBuilder

class JavaStandardLibrarySupportProvider : LibrarySupportProvider {
    private companion object {
        val builder = TerminatedChainBuilder(
            KotlinChainTransformerImpl(JavaStreamChainTypeExtractor()),
            StandardLibraryCallChecker(PackageBasedCallChecker("java.util.stream"))
        )
        val support = StandardLibrarySupport()
        val dsl = DslImpl(KotlinStatementFactory(JavaPeekCallFactory()))
    }

    override fun getLanguageId(): String = KotlinLanguage.INSTANCE.id

    override fun getChainBuilder(): StreamChainBuilder = builder

    override fun getLibrarySupport(): LibrarySupport = support

    override fun getExpressionBuilder(project: Project): TraceExpressionBuilder =
        KotlinTraceExpressionBuilder(dsl, support.createHandlerFactory(dsl))
}