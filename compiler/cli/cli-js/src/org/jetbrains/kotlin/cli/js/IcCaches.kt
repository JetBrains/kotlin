/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import org.jetbrains.kotlin.backend.js.JsGenerationGranularity
import org.jetbrains.kotlin.backend.wasm.ic.WasmICContext
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.JsICContext
import org.jetbrains.kotlin.ir.backend.js.ic.CacheUpdater
import org.jetbrains.kotlin.ir.backend.js.ic.DirtyFileState
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

sealed class IcCachesConfigurationData {
    abstract val includes: String

    data class Js(
        override val includes: String,
        val granularity: JsGenerationGranularity,
    ) : IcCachesConfigurationData()

    data class Wasm(
        override val includes: String,
        val wasmDebug: Boolean,
        val preserveIcOrder: Boolean,
        val generateWat: Boolean,
    ) : IcCachesConfigurationData()
}

internal fun prepareIcCaches(
    cacheDirectory: String,
    arguments: K2JSCompilerArguments,
    messageCollector: MessageCollector,
    outputDir: File,
    libraries: List<String>,
    friendLibraries: List<String>,
    targetConfiguration: CompilerConfiguration,
    mainCallArguments: List<String>?,
    icCacheReadOnly: Boolean,
): IcCachesArtifacts {
    val data = when {
        arguments.wasm -> IcCachesConfigurationData.Wasm(
            arguments.includes!!,
            arguments.wasmDebug,
            arguments.preserveIcOrder,
            arguments.wasmGenerateWat,
        )
        else -> IcCachesConfigurationData.Js(
            arguments.includes!!,
            arguments.granularity
        )
    }
    return prepareIcCaches(
        cacheDirectory,
        data,
        messageCollector,
        outputDir,
        libraries,
        friendLibraries,
        targetConfiguration,
        mainCallArguments,
        icCacheReadOnly
    )
}

internal fun prepareIcCaches(
    cacheDirectory: String,
    icConfigurationData: IcCachesConfigurationData,
    messageCollector: MessageCollector,
    outputDir: File,
    libraries: List<String>,
    friendLibraries: List<String>,
    targetConfiguration: CompilerConfiguration,
    mainCallArguments: List<String>?,
    icCacheReadOnly: Boolean,
): IcCachesArtifacts {

    messageCollector.report(INFO, "")
    messageCollector.report(INFO, "Building cache:")
    messageCollector.report(INFO, "to: $outputDir")
    messageCollector.report(INFO, "cache directory: $cacheDirectory")
    messageCollector.report(INFO, libraries.toString())

    val start = System.currentTimeMillis()

    val icContext = when (icConfigurationData) {
        is IcCachesConfigurationData.Js -> JsICContext(
            mainCallArguments,
            icConfigurationData.granularity,
        )
        is IcCachesConfigurationData.Wasm -> WasmICContext(
            allowIncompleteImplementations = false,
            skipLocalNames = !icConfigurationData.wasmDebug,
            safeFragmentTags = icConfigurationData.preserveIcOrder,
            skipCommentInstructions = !icConfigurationData.generateWat,
        )
    }
    val cacheUpdater = CacheUpdater(
        mainModule = icConfigurationData.includes,
        allModules = libraries,
        mainModuleFriends = friendLibraries,
        cacheDir = cacheDirectory,
        compilerConfiguration = targetConfiguration,
        icContext = icContext,
        checkForClassStructuralChanges = icConfigurationData is IcCachesConfigurationData.Wasm,
        commitIncrementalCache = !icCacheReadOnly,
    )

    val artifacts = cacheUpdater.actualizeCaches()

    messageCollector.report(INFO, "IC rebuilt overall time: ${System.currentTimeMillis() - start}ms")
    for ((event, duration) in cacheUpdater.getStopwatchLastLaps()) {
        messageCollector.report(INFO, "  $event: ${(duration / 1e6).toInt()}ms")
    }

    var libIndex = 0
    for ((libFile, srcFiles) in cacheUpdater.getDirtyFileLastStats()) {
        val singleState = srcFiles.values.firstOrNull()?.singleOrNull()?.let { singleState ->
            singleState.takeIf { srcFiles.values.all { it.singleOrNull() == singleState } }
        }

        val (msg, showFiles) = when {
            singleState == DirtyFileState.NON_MODIFIED_IR -> continue
            singleState == DirtyFileState.REMOVED_FILE -> "removed" to emptyMap()
            singleState == DirtyFileState.ADDED_FILE -> "built clean" to emptyMap()
            srcFiles.values.any { it.singleOrNull() == DirtyFileState.NON_MODIFIED_IR } -> "partially rebuilt" to srcFiles
            else -> "fully rebuilt" to srcFiles
        }
        messageCollector.report(INFO, "${++libIndex}) module [${File(libFile.path).name}] was $msg")
        var fileIndex = 0
        for ((srcFile, stat) in showFiles) {
            val filteredStats = stat.filter { it != DirtyFileState.NON_MODIFIED_IR }
            val statStr = filteredStats.takeIf { it.isNotEmpty() }?.joinToString { it.str } ?: continue
            // Use index, because MessageCollector ignores already reported messages
            messageCollector.report(INFO, "  $libIndex.${++fileIndex}) file [${File(srcFile.path).name}]: ($statStr)")
        }
    }

    return IcCachesArtifacts(artifacts)
}

class IcCachesArtifacts(val artifacts: List<ModuleArtifact>)
