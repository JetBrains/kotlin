/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.Profiler
import org.jetbrains.kotlin.daemon.common.withMeasure
import org.jetbrains.kotlin.incremental.web.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.web.IrTranslationResultValue
import org.jetbrains.kotlin.incremental.web.TranslationResultValue
import java.io.File

class RemoteIncrementalDataProvider(
    @Suppress("DEPRECATION") val facade: org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacade,
    val rpcProfiler: Profiler
) : IncrementalDataProvider {
    override val serializedIrFiles: Map<File, IrTranslationResultValue>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val headerMetadata: ByteArray
        get() = rpcProfiler.withMeasure(this) {
            facade.incrementalDataProvider_getHeaderMetadata()
        }

    override val compiledPackageParts: Map<File, TranslationResultValue>
        get() = rpcProfiler.withMeasure(this) {
            val result = mutableMapOf<File, TranslationResultValue>()
            facade.incrementalDataProvider_getCompiledPackageParts().forEach {
                val prev = result.put(File(it.filePath), TranslationResultValue(it.metadata, it.binaryAst, it.inlineData))
                check(prev == null) { "compiledPackageParts: duplicated entry for file `${it.filePath}`" }
            }
            result
        }

    override val metadataVersion: IntArray
        get() = rpcProfiler.withMeasure(this) {
            facade.incrementalDataProvider_getMetadataVersion()
        }

    override val packageMetadata: Map<String, ByteArray>
        get() = rpcProfiler.withMeasure(this) {
            val result = mutableMapOf<String, ByteArray>()
            facade.incrementalDataProvider_getPackageMetadata().forEach {
                val prev = result.put(it.packageName, it.metadata)
                check(prev == null) { "packageMetadata: duplicated entry for package `${it.packageName}`" }
            }
            result
        }
}
