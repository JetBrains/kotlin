/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.impls.CompilerCallbackServicesFacade
import org.jetbrains.kotlin.daemon.common.impls.Profiler
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.TranslationResultValue
import java.io.File

class RemoteIncrementalDataProvider(val facade: CompilerCallbackServicesFacade, val rpcProfiler: Profiler) :
    IncrementalDataProvider {
    override val headerMetadata: ByteArray
        get() = rpcProfiler.withMeasure(this) {
            facade.incrementalDataProvider_getHeaderMetadata()
        }

    override val compiledPackageParts: Map<File, TranslationResultValue>
        get() = rpcProfiler.withMeasure(this) {
            val result = mutableMapOf<File, TranslationResultValue>()
            facade.incrementalDataProvider_getCompiledPackageParts().forEach {
                val prev = result.put(File(it.filePath), TranslationResultValue(it.metadata, it.binaryAst))
                check(prev == null) { "compiledPackageParts: duplicated entry for file `${it.filePath}`" }
            }
            result
        }

    override val metadataVersion: IntArray
        get() = rpcProfiler.withMeasure(this) {
            facade.incrementalDataProvider_getMetadataVersion()
        }
}