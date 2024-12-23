/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import java.io.File

class RemoteIncrementalResultsConsumer(
    @Suppress("DEPRECATION") val facade: org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade,
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
        fileMetadata: ByteArray,
        debugInfo: ByteArray?,
        fileEntries: ByteArray,
    ) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    override fun processPackageMetadata(packageName: String, metadata: ByteArray) {
        rpcProfiler.withMeasure(this) {
            facade.incrementalResultsConsumer_processPackageMetadata(packageName, metadata)
        }
    }
}
