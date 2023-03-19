/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

abstract class AbstractIncrementalMultiModuleJsCompilerRunnerTest :
    AbstractIncrementalMultiModuleCompilerRunnerTest<K2JSCompilerArguments, ModulesApiHistoryJs>() {

    override val buildLogFinder: BuildLogFinder
        get() = super.buildLogFinder.copy(
            isJsEnabled = true,
            isScopeExpansionEnabled = scopeExpansionMode != CompileScopeExpansionMode.NEVER
        )

    override fun createCompilerArguments(destinationDir: File, testDir: File): K2JSCompilerArguments =
        K2JSCompilerArguments().apply {
            sourceMap = true
            metaInfo = true
            forceDeprecatedLegacyCompilerUsage = true
        }

    override fun makeForSingleModule(
        moduleCacheDir: File,
        sourceRoots: Iterable<File>,
        args: K2JSCompilerArguments,
        moduleBuildHistoryFile: File,
        messageCollector: MessageCollector,
        reporter: ICReporter,
        scopeExpansion: CompileScopeExpansionMode,
        modulesApiHistory: ModulesApiHistoryJs,
        providedChangedFiles: ChangedFiles?
    ) {
        makeJsIncrementally(
            moduleCacheDir,
            sourceRoots,
            args,
            moduleBuildHistoryFile,
            messageCollector,
            reporter,
            scopeExpansionMode,
            modulesApiHistory,
            providedChangedFiles
        )
    }

    override fun K2JSCompilerArguments.updateForSingleModule(moduleDependencies: List<String>, outFile: File) {
        val dependencies = moduleDependencies.joinToString(File.pathSeparator) {
            File(repository, it.asArtifactFileName()).absolutePath
        }

        libraries = dependencies
        outputFile = outFile.path
    }

    override fun transformToDependency(moduleName: String, rawArtifact: File): File {
        val rawDir = rawArtifact.parentFile
        val artifactFile = File(repository, moduleName.asArtifactFileName())
        val zipOut = ZipOutputStream(FileOutputStream(artifactFile))

        fun walkFiles(dir: File) {
            dir.listFiles()?.let { files ->
                files.forEach { file ->
                    if (file.isDirectory) walkFiles(file)
                    else {
                        val relativePath = file.relativeTo(rawDir).path
                        val zipEntry = ZipEntry(relativePath)
                        zipEntry.time = 0
                        zipOut.putNextEntry(zipEntry)
                        file.readBytes().let { bytes ->
                            zipOut.write(bytes, 0, bytes.size)
                        }
                        zipOut.closeEntry()
                    }
                }
            }
        }

        walkFiles(rawDir)

        zipOut.close()

        return artifactFile
    }

    override val modulesApiHistory: ModulesApiHistoryJs by lazy { ModulesApiHistoryJs(incrementalModuleInfo) }

    override fun String.asOutputFileName(): String = "$this.js"
    override fun String.asArtifactFileName(): String = "$this.jar"

    override val scopeExpansionMode = CompileScopeExpansionMode.NEVER
}