/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import java.io.File

class RemoteIncrementalResultsConsumer(val facade: CompilerCallbackServicesFacade, val rpcProfiler: Profiler) :
    IncrementalResultsConsumer {
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

    override fun processInlineFunction(sourceFile: File, fqName: String, inlineFunction: Any, line: Int, column: Int) {
        rpcProfiler.withMeasure(this) {
            facade.incrementalResultsConsumer_processInlineFunction(sourceFile.path, fqName, inlineFunction, line, column)
        }
    }
}
