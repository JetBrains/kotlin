/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.ir.backend.js.ic.DirtyFileState
import java.io.File
import java.util.regex.Pattern

class ProjectInfo(val name: String, val modules: List<String>, val steps: List<ProjectBuildStep>, val muted: Boolean) {

    class ProjectBuildStep(val id: Int, val order: List<String>, val dirtyJS: List<String>, val language: List<String>)
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

    class Dependency(val moduleName: String, val isFriend: Boolean)

    class ModuleStep(
        val id: Int,
        val dependencies: Collection<Dependency>,
        val modifications: List<Modification>,
        val expectedFileStats: Map<String, Set<String>>,
        val expectedDTS: Set<String>
    )

    val steps = hashMapOf</* step ID */ Int, ModuleStep>()
}

const val PROJECT_INFO_FILE = "project.info"
private const val MODULES_LIST = "MODULES"
private const val LIBS_LIST = "libs"
private const val DIRTY_JS_MODULES_LIST = "dirty js"
private const val LANGUAGE = "language"

const val MODULE_INFO_FILE = "module.info"
private const val DEPENDENCIES = "dependencies"
private const val FRIENDS = "friends"
private const val MODIFICATIONS = "modifications"
private const val MODIFICATION_UPDATE = "U"
private const val MODIFICATION_DELETE = "D"
private const val EXPECTED_DTS_LIST = "expected dts"

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
        val language = mutableListOf<String>()

        loop { line ->
            val splitIndex = line.indexOf(':')
            if (splitIndex < 0) throwSyntaxError(line)

            val split = line.split(":")
            val op = split[0]

            if (op.matches(STEP_PATTERN.toRegex())) {
                return@loop true // break the loop
            }

            ++lineCounter


            when (op) {
                LIBS_LIST -> order += split[1].splitAndTrim()
                DIRTY_JS_MODULES_LIST -> dirtyJS += split[1].splitAndTrim()
                LANGUAGE -> language += split[1].splitAndTrim()
                else -> println(diagnosticMessage("Unknown op $op", line))
            }

            false
        }

        return (firstId..lastId).map { ProjectInfo.ProjectBuildStep(it, order, dirtyJS, language) }
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

            val split = line.split(":")
            val op = split[0]

            when {
                op == MODULES_LIST -> libraries += split[1].splitAndTrim()
                op.matches(STEP_PATTERN.toRegex()) -> {
                    val m = STEP_PATTERN.matcher(op)
                    if (!m.matches()) throwSyntaxError(line)

                    val firstId = Integer.parseInt(m.group(1))
                    val lastId = m.group(2)?.let { Integer.parseInt(it) } ?: firstId
                    steps += parseSteps(firstId, lastId)
                }
                else -> error(diagnosticMessage("Unknown op $op", line))
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
                    MODIFICATION_UPDATE -> {
                        val (from, to) = cmd.split("->")
                        modifications.add(ModuleInfo.Modification.Update(from.trim(), to.trim()))
                    }
                    MODIFICATION_DELETE -> modifications.add(ModuleInfo.Modification.Delete(cmd.trim()))
                    else -> error("Unknown modification $line")
                }
                false
            } else {
                true
            }
        }

        return modifications
    }

    private fun parseSteps(firstId: Int, lastId: Int): List<ModuleInfo.ModuleStep> {
        val expectedFileStats = mutableMapOf<String, Set<String>>()
        val regularDependencies = mutableSetOf<String>()
        val friendDependencies = mutableSetOf<String>()
        val modifications = mutableListOf<ModuleInfo.Modification>()
        val expectedDTS = mutableSetOf<String>()

        loop { line ->
            if (line.matches(STEP_PATTERN.toRegex()))
                return@loop true
            lineCounter++

            val opIndex = line.indexOf(':')
            if (opIndex < 0) throwSyntaxError(line)
            val op = line.substring(0, opIndex)

            fun getOpArgs() = line.substring(opIndex + 1).splitAndTrim()

            val expectedState = DirtyFileState.values().find { it.str == op }
            if (expectedState != null) {
                expectedFileStats[expectedState.str] = getOpArgs().toSet()
            } else {
                when (op) {
                    DEPENDENCIES -> getOpArgs().forEach { regularDependencies += it }
                    FRIENDS -> getOpArgs().forEach { friendDependencies += it }
                    MODIFICATIONS -> modifications += parseModifications()
                    EXPECTED_DTS_LIST -> getOpArgs().forEach { expectedDTS += it }
                    else -> error(diagnosticMessage("Unknown op $op", line))
                }
            }

            false
        }

        (friendDependencies - regularDependencies)
            .takeIf(Set<String>::isNotEmpty)
            ?.let { error("Misconfiguration: There are friend modules that are not listed as regular dependencies: $it") }

        val dependencies = regularDependencies.map { regularDependency ->
            ModuleInfo.Dependency(regularDependency, regularDependency in friendDependencies)
        }

        return (firstId..lastId).map {
            ModuleInfo.ModuleStep(
                id = it,
                dependencies = dependencies,
                modifications = modifications,
                expectedFileStats = expectedFileStats,
                expectedDTS = expectedDTS
            )
        }
    }

    override fun parse(entryName: String): ModuleInfo {
        val result = ModuleInfo(entryName)

        loop { line ->
            lineCounter++
            val stepMatcher = STEP_PATTERN.matcher(line)
            if (stepMatcher.matches()) {
                val firstId = Integer.parseInt(stepMatcher.group(1))
                val lastId = stepMatcher.group(2)?.let { Integer.parseInt(it) } ?: firstId
                parseSteps(firstId, lastId).forEach { step ->
                    result.steps[step.id] = step
                }
            }
            false
        }

        return result
    }
}
