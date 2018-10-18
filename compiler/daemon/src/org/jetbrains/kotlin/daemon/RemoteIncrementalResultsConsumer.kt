/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.impls.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.impls.Profiler
import org.jetbrains.kotlin.incremental.js.FunctionWithSourceInfo
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.incremental.js.JsInlineFunctionHash
import java.io.File

class RemoteIncrementalResultsConsumer(val facade: CompilerCallbackServicesFacade, eventManager: EventManager, val rpcProfiler: Profiler) :
    IncrementalResultsConsumer {
    init {
        eventManager.onCompilationFinished(this::flush)
    }

    override fun processHeader(headerMetadata: ByteArray) {
        rpcProfiler.withMeasure(this) {
            facade.incrementalResultsConsumer_processHeader(headerMetadata)
        }
    }

    override fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray, binaryAst: ByteArray) {
        rpcProfiler.withMeasure(this) {
            facade.incrementalResultsConsumer_processPackagePart(sourceFile.path, packagePartMetadata, binaryAst)
        }
    }

    private class JsInlineFunction(val sourceFilePath: String, val fqName: String, val inlineFunction: FunctionWithSourceInfo)

    private val deferInlineFuncs = mutableListOf<JsInlineFunction>()

    override fun processInlineFunction(sourceFile: File, fqName: String, inlineFunction: Any, line: Int, column: Int) {
        deferInlineFuncs.add(JsInlineFunction(sourceFile.path, fqName, FunctionWithSourceInfo(inlineFunction, line, column)))
    }

    override fun processInlineFunctions(functions: Collection<JsInlineFunctionHash>) = error("Should not be called in Daemon Server")

    fun flush() {
        rpcProfiler.withMeasure(this) {
            facade.incrementalResultsConsumer_processInlineFunctions(deferInlineFuncs.map {
                JsInlineFunctionHash(it.sourceFilePath, it.fqName, it.inlineFunction.md5)
            })
        }
    }
}
