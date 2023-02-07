/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.incremental.web.FunctionWithSourceInfo
import org.jetbrains.kotlin.incremental.web.IncrementalResultsConsumer
import org.jetbrains.kotlin.incremental.web.JsInlineFunctionHash
import java.io.File

class RemoteIncrementalResultsConsumer(
    @Suppress("DEPRECATION") val facade: org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade,
    eventManager: EventManager,
    val rpcProfiler: Profiler
) : IncrementalResultsConsumer {
    override fun processIrFile(
        sourceFile: File,
        fileData: ByteArray,
        types: ByteArray,
        signatures: ByteArray,
        strings: ByteArray,
        declarations: ByteArray,
        bodies: ByteArray,
        fqn: ByteArray,
        debugInfo: ByteArray?
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    init {
        eventManager.onCompilationFinished(this::flush)
    }

    override fun processHeader(headerMetadata: ByteArray) {
        rpcProfiler.withMeasure(this) {
            facade.incrementalResultsConsumer_processHeader(headerMetadata)
        }
    }

    override fun processPackagePart(sourceFile: File, packagePartMetadata: ByteArray, binaryAst: ByteArray, inlineData: ByteArray) {
        rpcProfiler.withMeasure(this) {
            facade.incrementalResultsConsumer_processPackagePart(sourceFile.path, packagePartMetadata, binaryAst, inlineData)
        }
    }

    private class JsInlineFunction(val sourceFilePath: String, val fqName: String, val inlineFunction: FunctionWithSourceInfo)

    private val deferInlineFuncs = mutableListOf<JsInlineFunction>()

    override fun processInlineFunction(sourceFile: File, fqName: String, inlineFunction: Any, line: Int, column: Int) {
        deferInlineFuncs.add(JsInlineFunction(sourceFile.path, fqName, FunctionWithSourceInfo(inlineFunction, line, column)))
    }

    override fun processInlineFunctions(functions: Collection<JsInlineFunctionHash>) = error("Should not be called in Daemon Server")

    override fun processPackageMetadata(packageName: String, metadata: ByteArray) {
        rpcProfiler.withMeasure(this) {
            facade.incrementalResultsConsumer_processPackageMetadata(packageName, metadata)
        }
    }

    fun flush() {
        rpcProfiler.withMeasure(this) {
            facade.incrementalResultsConsumer_processInlineFunctions(deferInlineFuncs.map {
                JsInlineFunctionHash(it.sourceFilePath, it.fqName, it.inlineFunction.md5)
            })
        }
    }
}
