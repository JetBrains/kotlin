/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import java.io.File
import java.util.regex.Pattern

class ProjectInfo(val name: String, val modules: List<String>, val steps: List<ProjectBuildStep>, val muted: Boolean) {

    class ProjectBuildStep(val id: Int, val order: List<String>, val dirtyJS: List<String>)
}

class ModuleInfo(val moduleName: String) {

    sealed class Modification {
        class Delete(private val fileName: String) : Modification() {
            override fun execute(testDirectory: File, sourceDirectory: File, deletedFilesCollector: (File) -> Unit) {
                val file = File(sourceDirectory, fileName)
                file.delete()
                deletedFilesCollector(file)
            }
        }

        class Update(private val fromFile: String, private val toFile: String) : Modification() {
            override fun execute(testDirectory: File, sourceDirectory: File, deletedFilesCollector: (File) -> Unit) {
                val toFile = File(sourceDirectory, toFile)
                if (toFile.exists()) {
                    toFile.delete()
                }

                val fromFile = File(testDirectory, fromFile)

                fromFile.copyTo(toFile, overwrite = true)
            }
        }

        abstract fun execute(testDirectory: File, sourceDirectory: File, deletedFilesCollector: (File) -> Unit = {})
    }

    class ModuleStep(
        val id: Int,
        val dependencies: Collection<String>,
        val dirtyFiles: Collection<String>,
        val modifications: List<Modification>,
        val directives: Set<StepDirectives>
    )

    val steps = mutableListOf<ModuleStep>()
}

enum class StepDirectives(val mnemonic: String) {
    FAST_PATH_UPDATE("FP"),
    UNUSED_MODULE("UNUSED")
}

const val MODULES_LIST = "MODULES"
const val PROJECT_INFO_FILE = "project.info"
const val MODULE_INFO_FILE = "module.info"

private val STEP_PATTERN = Pattern.compile("^\\s*STEP\\s+(\\d+)\\.*(\\d+)?\\s*:?$")

private val MODIFICATION_PATTERN = Pattern.compile("^([UD])\\s*:(.+)$")

abstract class InfoParser<Info>(protected val infoFile: File) {
    protected var lineCounter = 0
    protected val lines = infoFile.readLines()

    abstract fun parse(entryName: String): Info

    protected fun loop(lambda: (String) -> Boolean) {
        while (lineCounter < lines.size) {
            val line = lines[lineCounter]
            if (line.isBlank()) {
                ++lineCounter
                continue
            }

            if (lambda(line.trim())) {
                break
            }
        }
    }


    protected fun diagnosticMessage(message: String, line: String): String {
        return "$message in '$line' at ${infoFile.path}:${lineCounter - 1}"
    }

    protected fun throwSyntaxError(line: String): Nothing {
        throw AssertionError(diagnosticMessage("Syntax error", line))
    }

}

private fun String.splitAndTrim() = split(",").map { it.trim() }.filter { it.isNotBlank() }

class ProjectInfoParser(infoFile: File) : InfoParser<ProjectInfo>(infoFile) {


    private fun parseSteps(firstId: Int, lastId: Int): List<ProjectInfo.ProjectBuildStep> {
        val order = mutableListOf<String>()
        val dirtyJS = mutableListOf<String>()

        loop { line ->
            val splitIndex = line.indexOf(':')
            if (splitIndex < 0) throwSyntaxError(line)

            val splitted = line.split(":")
            val op = splitted[0]

            if (op.matches(STEP_PATTERN.toRegex())) {
                return@loop true // break the loop
            }

            ++lineCounter


            when (op) {
                "libs" -> {
                    order += splitted[1].splitAndTrim()
                }
                "dirty js" -> {
                    dirtyJS += splitted[1].splitAndTrim()
                }
                else -> println(diagnosticMessage("Unknown op $op", line))
            }

            false
        }

        return (firstId..lastId).map { ProjectInfo.ProjectBuildStep(it, order, dirtyJS) }
    }

    override fun parse(entryName: String): ProjectInfo {
        val libraries = mutableListOf<String>()
        val steps = mutableListOf<ProjectInfo.ProjectBuildStep>()
        var muted = false

        loop { line ->
            lineCounter++

            if (line == "MUTED") {
                muted = true
                return@loop false
            }

            val splitIndex = line.indexOf(':')
            if (splitIndex < 0) throwSyntaxError(line)

            val splitted = line.split(":")
            val op = splitted[0]

            when {
                op == MODULES_LIST -> {
                    libraries += splitted[1].splitAndTrim()
                }
                op.matches(STEP_PATTERN.toRegex()) -> {
                    val m = STEP_PATTERN.matcher(op)
                    if (!m.matches()) throwSyntaxError(line)

                    val firstId = Integer.parseInt(m.group(1))
                    val lastId = m.group(2)?.let { Integer.parseInt(it) } ?: firstId
                    steps += parseSteps(firstId, lastId)
                }
                else -> println(diagnosticMessage("Unknown op $op", line))
            }

            false
        }

        return ProjectInfo(entryName, libraries, steps, muted)
    }
}

class ModuleInfoParser(infoFile: File) : InfoParser<ModuleInfo>(infoFile) {

    private fun parseModifications(): List<ModuleInfo.Modification> {
        val modifications = mutableListOf<ModuleInfo.Modification>()

        loop { line ->
            val matcher3 = MODIFICATION_PATTERN.matcher(line)
            if (matcher3.matches()) {
                lineCounter++
                val mop = matcher3.group(1)
                val cmd = matcher3.group(2)
                when (mop) {
                    "U" -> {
                        val (from, to) = cmd.split("->")
                        modifications.add(ModuleInfo.Modification.Update(from.trim(), to.trim()))
                    }
                    "D" -> modifications.add(ModuleInfo.Modification.Delete(cmd.trim()))
                    else -> error("Unknown modification $line")
                }
                false
            } else {
                true
            }
        }

        return modifications
    }

    private fun MutableSet<StepDirectives>.parseStepFlags(flags: String) {
        val tokens = flags.trim().split(" ").map { it.trim() }
        for (directive in StepDirectives.values()) {
            if (directive.mnemonic in tokens) add(directive)
        }
    }

    private fun parseSteps(firstId: Int, lastId: Int): List<ModuleInfo.ModuleStep> {
        val dependencies = mutableSetOf<String>()
        val dirtyFiles = mutableSetOf<String>()
        val modifications = mutableListOf<ModuleInfo.Modification>()
        val directives = mutableSetOf<StepDirectives>()

        loop { line ->
            if (line.matches(STEP_PATTERN.toRegex()))
                return@loop true
            lineCounter++

            val opIndex = line.indexOf(':')
            if (opIndex < 0) throwSyntaxError(line)
            val op = line.substring(0, opIndex)

            when (op) {
                "dependencies" -> line.substring(opIndex + 1).split(",").filter { it.isNotBlank() }.forEach { dependencies.add(it.trim()) }
                "dirty" -> line.substring(opIndex + 1).split(",").filter { it.isNotBlank() }.forEach { dirtyFiles.add(it.trim()) }
                "modifications" -> modifications.addAll(parseModifications())
                "flags" -> directives.parseStepFlags(line.substring(opIndex + 1))
                else -> println(diagnosticMessage("Unknown op $op", line))
            }
            false
        }

        return (firstId..lastId).map { ModuleInfo.ModuleStep(it, dependencies, dirtyFiles, modifications, directives) }
    }

    override fun parse(entryName: String): ModuleInfo {
        val result = ModuleInfo(entryName)

        loop { line ->
            lineCounter++
            val stepMatcher = STEP_PATTERN.matcher(line)
            if (stepMatcher.matches()) {
                val firstId = Integer.parseInt(stepMatcher.group(1))
                val lastId = stepMatcher.group(2)?.let { Integer.parseInt(it) } ?: firstId
                result.steps += parseSteps(firstId, lastId)
            }
            false
        }

        return result
    }
}
