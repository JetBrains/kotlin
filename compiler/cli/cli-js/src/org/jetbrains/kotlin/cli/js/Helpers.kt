/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import com.intellij.util.ExceptionUtil
import org.jetbrains.kotlin.backend.js.JsGenerationGranularity
import org.jetbrains.kotlin.backend.js.TsCompilationStrategy
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.LOGGING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.SourceMapNamesPolicy
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.js.config.wasmCompilation
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.file.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.join
import org.jetbrains.kotlin.wasm.config.wasmTarget
import java.io.File
import java.io.IOException
import kotlin.math.min

fun checkKotlinPackageUsageForPsi(configuration: CompilerConfiguration, files: Collection<KtFile>): Boolean =
    org.jetbrains.kotlin.cli.common.checkKotlinPackageUsageForPsi(configuration, files)

val K2JSCompilerArguments.targetVersion: EcmaVersion?
    get() {
        val targetString = target
        return when {
            targetString != null -> EcmaVersion.entries.firstOrNull { it.name == targetString }
            else -> EcmaVersion.defaultVersion()
        }
    }

val K2JSCompilerArguments.granularity: JsGenerationGranularity
    get() = when {
        this.irPerFile -> JsGenerationGranularity.PER_FILE
        this.irPerModule -> JsGenerationGranularity.PER_MODULE
        else -> JsGenerationGranularity.WHOLE_PROGRAM
    }

val K2JSCompilerArguments.dtsStrategy: TsCompilationStrategy
    get() = when {
        !this.generateDts -> TsCompilationStrategy.NONE
        this.irPerFile -> TsCompilationStrategy.EACH_FILE
        else -> TsCompilationStrategy.MERGED
    }

internal class DisposableZipFileSystemAccessor private constructor(
    private val zipAccessor: ZipFileSystemCacheableAccessor,
) : Disposable, ZipFileSystemAccessor by zipAccessor {
    constructor(cacheLimit: Int) : this(ZipFileSystemCacheableAccessor(cacheLimit))

    override fun dispose() {
        zipAccessor.reset()
    }
}

internal val sourceMapContentEmbeddingMap: Map<String, SourceMapSourceEmbedding> = mapOf(
    K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS to SourceMapSourceEmbedding.ALWAYS,
    K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER to SourceMapSourceEmbedding.NEVER,
    K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING to SourceMapSourceEmbedding.INLINING
)

internal val sourceMapNamesPolicyMap: Map<String, SourceMapNamesPolicy> = mapOf(
    K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_NO to SourceMapNamesPolicy.NO,
    K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_SIMPLE_NAMES to SourceMapNamesPolicy.SIMPLE_NAMES,
    K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_FQ_NAMES to SourceMapNamesPolicy.FULLY_QUALIFIED_NAMES
)

internal val moduleKindMap: Map<String, ModuleKind> = mapOf(
    K2JsArgumentConstants.MODULE_PLAIN to ModuleKind.PLAIN,
    K2JsArgumentConstants.MODULE_COMMONJS to ModuleKind.COMMON_JS,
    K2JsArgumentConstants.MODULE_AMD to ModuleKind.AMD,
    K2JsArgumentConstants.MODULE_UMD to ModuleKind.UMD,
    K2JsArgumentConstants.MODULE_ES to ModuleKind.ES,
)

internal fun configureLibraries(libraryString: String?): List<String> =
    libraryString?.splitByPathSeparator() ?: emptyList()

private fun String.splitByPathSeparator(): List<String> {
    return this.split(File.pathSeparator.toRegex())
        .dropLastWhile { it.isEmpty() }
        .toTypedArray()
        .filterNot { it.isEmpty() }
}

internal fun calculateSourceMapSourceRoot(
    messageCollector: MessageCollector,
    arguments: K2JSCompilerArguments,
): String {
    var commonPath: File? = null
    val pathToRoot = mutableListOf<File>()
    val pathToRootIndexes = hashMapOf<File, Int>()

    try {
        for (path in arguments.freeArgs) {
            var file: File? = File(path).canonicalFile
            if (commonPath == null) {
                commonPath = file

                while (file != null) {
                    pathToRoot.add(file)
                    file = file.parentFile
                }
                pathToRoot.reverse()

                for (i in pathToRoot.indices) {
                    pathToRootIndexes[pathToRoot[i]] = i
                }
            } else {
                while (file != null) {
                    var existingIndex = pathToRootIndexes[file]
                    if (existingIndex != null) {
                        existingIndex = min(existingIndex, pathToRoot.size - 1)
                        pathToRoot.subList(existingIndex + 1, pathToRoot.size).clear()
                        commonPath = pathToRoot[pathToRoot.size - 1]
                        break
                    }
                    file = file.parentFile
                }
                if (file == null) {
                    break
                }
            }
        }
    } catch (e: IOException) {
        val text = ExceptionUtil.getThrowableText(e)
        messageCollector.report(ERROR, "IO error occurred calculating source root:\n$text", location = null)
        return "."
    }

    return commonPath?.path ?: "."
}

internal fun reportCompiledSourcesList(messageCollector: MessageCollector, sourceFiles: List<KtFile>) {
    val fileNames = sourceFiles.map { file ->
        val virtualFile = file.virtualFile
        if (virtualFile != null) {
            MessageUtil.virtualFileToPath(virtualFile)
        } else {
            file.name + " (no virtual file)"
        }
    }
    messageCollector.report(LOGGING, "Compiling source files: " + join(fileNames, ", "), null)
}

internal fun reportCollectedDiagnostics(
    compilerConfiguration: CompilerConfiguration,
    diagnosticsReporter: BaseDiagnosticsCollector,
    messageCollector: MessageCollector
) {
    val renderName = compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderName)
}

internal val CompilerConfiguration.platformChecker: KlibPlatformChecker
    get() = if (wasmCompilation) KlibPlatformChecker.Wasm(wasmTarget.alias) else KlibPlatformChecker.JS
