/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.sequence.lib.sequence

import com.intellij.debugger.streams.lib.LibrarySupport
import com.intellij.debugger.streams.lib.LibrarySupportProvider
import com.intellij.debugger.streams.trace.TraceExpressionBuilder
import com.intellij.debugger.streams.trace.dsl.impl.DslImpl
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.debugger.sequence.psi.impl.KotlinChainTransformerImpl
import org.jetbrains.kotlin.idea.debugger.sequence.psi.impl.TerminatedChainBuilder
import org.jetbrains.kotlin.idea.debugger.sequence.psi.sequence.SequenceCallChecker
import org.jetbrains.kotlin.idea.debugger.sequence.psi.sequence.SequenceCallCheckerWithNameHeuristics
import org.jetbrains.kotlin.idea.debugger.sequence.psi.sequence.SequenceTypeExtractor
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinCollectionsPeekCallFactory
import org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl.KotlinStatementFactory
import org.jetbrains.kotlin.idea.debugger.sequence.trace.impl.KotlinTraceExpressionBuilder

class KotlinSequenceSupportProvider : LibrarySupportProvider {
    override fun getLanguageId(): String = KotlinLanguage.INSTANCE.id

    private val builder: StreamChainBuilder = TerminatedChainBuilder(
        KotlinChainTransformerImpl(SequenceTypeExtractor()),
        SequenceCallCheckerWithNameHeuristics(SequenceCallChecker())
    )
    private val support: LibrarySupport by lazy { KotlinSequencesSupport() }
    private val dsl: DslImpl by lazy { DslImpl(KotlinStatementFactory(KotlinCollectionsPeekCallFactory())) }

    override fun getChainBuilder(): StreamChainBuilder = builder

    override fun getLibrarySupport(): LibrarySupport = support

    override fun getExpressionBuilder(project: Project): TraceExpressionBuilder =
        KotlinTraceExpressionBuilder(dsl, support.createHandlerFactory(dsl))

}