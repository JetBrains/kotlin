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
import org.jetbrains.kotlin.js.inline.util.RelativePathCalculator
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import java.io.*
import java.util.zip.ZipFile

class K2JSDce : CLITool<K2JSDceArguments>() {
    override fun createArguments(): K2JSDceArguments = K2JSDceArguments()

    override fun execImpl(messageCollector: MessageCollector, services: Services, arguments: K2JSDceArguments): ExitCode {
        val baseDir = File(arguments.outputDirectory ?: "min")
        val files = arguments.freeArgs.flatMap { arg ->
            collectInputFiles(baseDir, arg, messageCollector)
        }

        if (messageCollector.hasErrors()) return ExitCode.COMPILATION_ERROR

        if (files.isEmpty() && !arguments.version) {
            messageCollector.report(CompilerMessageSeverity.ERROR, "no source files")
            return ExitCode.COMPILATION_ERROR
        }

        val existingFiles = mutableMapOf<String, InputFile>()
        for (file in files) {
            existingFiles[file.outputPath]?.let {
                messageCollector.report(
                    CompilerMessageSeverity.ERROR,
                    "duplicate target file will be created for '${file.resource.name}' and '${it.resource.name}'"
                )
                return ExitCode.COMPILATION_ERROR
            }
            existingFiles[file.outputPath] = file
            if (File(file.outputPath).isDirectory) {
                messageCollector.report(CompilerMessageSeverity.ERROR, "cannot open output file '${file.outputPath}': it is a directory")
                return ExitCode.COMPILATION_ERROR
            }
        }

        return if (!arguments.devMode) {
            performDce(files, arguments, messageCollector)
        } else {
            copyFiles(files)
            ExitCode.OK
        }
    }

    private fun performDce(files: List<InputFile>, arguments: K2JSDceArguments, messageCollector: MessageCollector): ExitCode {
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
        val nodes = dceResult.reachableNodes.filterTo(mutableSetOf()) { it.reachable }

        val reachabilitySeverity = if (arguments.printReachabilityInfo) CompilerMessageSeverity.INFO else CompilerMessageSeverity.LOGGING
        messageCollector.report(reachabilitySeverity, "")
        for (node in nodes.extractRoots()) {
            printTree(
                node, { messageCollector.report(reachabilitySeverity, it) },
                printNestedMembers = false, showLocations = true
            )
        }

        return ExitCode.OK
    }

    private fun copyFiles(files: List<InputFile>) {
        for (file in files) {
            copyResource(file.resource, File(file.outputPath))
            file.sourceMapResource?.let { sourceMap ->
                val sourceMapTarget = File(file.outputPath + ".map")
                val inputFile = File(sourceMap.name)
                if (!inputFile.exists() || !mapSourcePaths(inputFile, sourceMapTarget)) {
                    copyResource(sourceMap, sourceMapTarget)
                }
            }
        }
    }

    private fun copyResource(resource: InputResource, targetFile: File) {
        if (targetFile.exists() && resource.lastModified() < targetFile.lastModified()) return

        targetFile.parentFile.mkdirs()
        resource.reader().use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun mapSourcePaths(inputFile: File, targetFile: File): Boolean {
        val json = try {
            InputStreamReader(FileInputStream(inputFile), "UTF-8").use { parseJson(it) }
        } catch (e: JsonSyntaxException) {
            return false
        }

        val sourcesArray = (json as? JsonObject)?.properties?.get("sources") as? JsonArray ?: return false
        val sources = sourcesArray.elements.map {
            (it as? JsonString)?.value ?: return false
        }

        val pathCalculator = RelativePathCalculator(targetFile.parentFile)
        val mappedSources = sources.map {
            val result = pathCalculator.calculateRelativePathTo(File(inputFile.parentFile, it))
            if (result != null) {
                if (File(targetFile.parentFile, result).exists()) {
                    result
                } else {
                    it
                }
            } else {
                it
            }
        }

        if (mappedSources == sources) return false

        json.properties["sources"] = JsonArray(*mappedSources.map { JsonString(it) }.toTypedArray())

        targetFile.parentFile.mkdirs()
        OutputStreamWriter(FileOutputStream(targetFile), "UTF-8").use { it.write(json.toString()) }

        return true
    }

    private fun collectInputFiles(baseDir: File, fileName: String, messageCollector: MessageCollector): List<InputFile> {
        val file = File(fileName)
        return when {
            file.isDirectory -> {
                collectInputFilesFromDirectory(baseDir, fileName)
            }
            file.isFile -> {
                when {
                    fileName.endsWith(".js") -> {
                        listOf(singleInputFile(baseDir, fileName))
                    }
                    fileName.endsWith(".zip") || fileName.endsWith(".jar") -> {
                        collectInputFilesFromZip(baseDir, fileName)
                    }
                    else -> {
                        messageCollector.report(
                            CompilerMessageSeverity.WARNING,
                            "invalid file name '${file.absolutePath}'; must end either with '.js', '.zip' or '.jar'"
                        )
                        emptyList()
                    }
                }
            }
            else -> {
                messageCollector.report(CompilerMessageSeverity.ERROR, "source file or directory not found: $fileName")
                emptyList()
            }
        }
    }

    private fun singleInputFile(baseDir: File, path: String): InputFile {
        val moduleName = getModuleNameFromPath(path)
        val pathToSourceMapCandidate = "$path.map"
        val pathToSourceMap = if (File(pathToSourceMapCandidate).exists()) pathToSourceMapCandidate else null
        return InputFile(
            InputResource.file(path), pathToSourceMap?.let { InputResource.file(it) },
            File(baseDir, "$moduleName.js").absolutePath, moduleName
        )
    }

    private fun collectInputFilesFromZip(baseDir: File, path: String): List<InputFile> {
        return ZipFile(path).use { zipFile ->
            zipFile.entries().asSequence()
                .filter { !it.isDirectory }
                .filter { it.name.endsWith(".js") }
                .filter { zipFile.getEntry(it.name.metaJs()) != null }
                .distinctBy { it.name }
                .map { entry ->
                    val moduleName = getModuleNameFromPath(entry.name)
                    val pathToSourceMapCandidate = "${entry.name}.map"
                    val pathToSourceMap = if (zipFile.getEntry(pathToSourceMapCandidate) != null) pathToSourceMapCandidate else null
                    InputFile(
                        InputResource.zipFile(path, entry.name), pathToSourceMap?.let { InputResource.zipFile(path, it) },
                        File(baseDir, "$moduleName.js").absolutePath, moduleName
                    )
                }
                .toList()
        }
    }

    private fun collectInputFilesFromDirectory(baseDir: File, path: String): List<InputFile> {
        return File(path).walkTopDown().asSequence()
            .filter { !it.isDirectory }
            .filter { it.name.endsWith(".js") }
            .filter { File(it.path.metaJs()).exists() }
            .map { entry ->
                val moduleName = getModuleNameFromPath(entry.name)
                val pathToSourceMapCandidate = "${entry.path}.map"
                val pathToSourceMap = if (File(pathToSourceMapCandidate).exists()) pathToSourceMapCandidate else null
                InputFile(
                    InputResource.file(entry.path), pathToSourceMap?.let { InputResource.file(it) },
                    File(baseDir, "$moduleName.js").absolutePath, moduleName
                )
            }
            .toList()
    }

    private fun String.metaJs() = removeSuffix(".js") + ".meta.js"

    private fun getModuleNameFromPath(path: String): String {
        val dotIndex = path.lastIndexOf('.')
        val slashIndex = maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
        return path.substring(slashIndex + 1, if (dotIndex < 0) path.length else dotIndex)
    }

    override fun executableScriptFileName(): String = "kotlin-dce-js"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CLITool.doMain(K2JSDce(), args)
        }
    }
}
