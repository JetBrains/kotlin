/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.js.dce

import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSDceArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.js.dce.*
import java.io.File

class K2JSDce : CLITool<K2JSDceArguments>() {
    override fun createArguments(): K2JSDceArguments = K2JSDceArguments()

    override fun execImpl(messageCollector: MessageCollector, services: Services, arguments: K2JSDceArguments): ExitCode {
        val baseDir = File(arguments.outputDirectory ?: "min")
        val files = arguments.freeArgs.map { arg ->
            val parts = arg.split(File.pathSeparator, ignoreCase = false, limit = 2)
            val inputName = parts[0]
            val moduleName = parts.getOrNull(1) ?: ""
            val resolvedModuleName = if (!moduleName.isEmpty()) moduleName else File(inputName).nameWithoutExtension
            val pathToSourceMapCandidate = "$inputName.map"
            val pathToSourceMap = if (File(pathToSourceMapCandidate).exists()) pathToSourceMapCandidate else null
            InputFile(inputName, pathToSourceMap, File(baseDir, resolvedModuleName + ".js").absolutePath, resolvedModuleName)
        }

        if (files.isEmpty() && !arguments.version) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "no source files")
            return ExitCode.COMPILATION_ERROR
        }
        if (!checkSourceFiles(messageCollector, files)) {
            return ExitCode.COMPILATION_ERROR
        }

        val includedDeclarations = arguments.declarationsToKeep.orEmpty().toSet()

        val logConsumer = { level: DCELogLevel, message: String ->
            val severity = when (level) {
                DCELogLevel.ERROR -> CompilerMessageSeverity.ERROR
                DCELogLevel.WARN -> CompilerMessageSeverity.WARNING
                DCELogLevel.INFO -> CompilerMessageSeverity.LOGGING
            }
            messageCollector.report(severity, message)
        }
        val dceResult = DeadCodeElimination.run(files, includedDeclarations, logConsumer)
        if (dceResult.status == DeadCodeEliminationStatus.FAILED) return ExitCode.COMPILATION_ERROR
        val nodes = dceResult.reachableNodes

        val reachabilitySeverity = if (arguments.printReachabilityInfo) CompilerMessageSeverity.INFO else CompilerMessageSeverity.LOGGING
        messageCollector.report(reachabilitySeverity, "")
        for (node in nodes.extractRoots()) {
            printTree(node, { messageCollector.report(reachabilitySeverity, it) },
                      printNestedMembers = false, showLocations = true)
        }

        return ExitCode.OK
    }

    private fun checkSourceFiles(messageCollector: MessageCollector, files: List<InputFile>): Boolean {
        return files.fold(true) { ok, file ->
            val inputFile = File(file.path)
            val outputFile = File(file.outputPath)

            val inputOk = when {
                !inputFile.exists() -> {
                    messageCollector.report(CompilerMessageSeverity.ERROR, "source file or directory not found: " + file.path)
                    false
                }
                inputFile.isDirectory -> {
                    messageCollector.report(CompilerMessageSeverity.ERROR, "input file '" + file.path + "' is a directory")
                    false
                }
                else -> true
            }

            val outputOk = when {
                outputFile.exists() && outputFile.isDirectory -> {
                    messageCollector.report(CompilerMessageSeverity.ERROR, "cannot open output file '${outputFile.path}': is a directory")
                    false
                }
                else -> true
            }

            ok and inputOk and outputOk
        }
    }

    override fun executableScriptFileName(): String = "kotlin-dce-js"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CLITool.doMain(K2JSDce(), args)
        }
    }
}