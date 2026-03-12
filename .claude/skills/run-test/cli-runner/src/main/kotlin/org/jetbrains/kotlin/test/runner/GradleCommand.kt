@file:Suppress("MatchingDeclarationName")

package org.jetbrains.kotlin.test.runner

import java.nio.file.Path

data class GradleCommand(
    val arguments: List<String>,
) {
    override fun toString(): String =
        arguments.joinToString(" ") { arg ->
            if (arg.contains(' ') || arg.contains('$')) "\"$arg\"" else arg
        }
}

fun buildGradleCommand(
    matchedTests: List<MatchedTest>,
    projectRoot: Path,
    extraGradleArgs: String? = null,
): GradleCommand {
    val singleTest = matchedTests.size == 1

    data class TaskKey(
        val modulePath: String,
        val taskName: String,
    )

    val grouped =
        matchedTests
            .map { test ->
                val moduleInfo = resolveGradleModule(test.classFilePath, projectRoot)
                val testFilter = buildTestFilter(test)
                TaskKey(moduleInfo.modulePath, moduleInfo.taskName) to testFilter
            }.groupBy(keySelector = { it.first }, valueTransform = { it.second })

    val args =
        buildList {
            for ((taskKey, filters) in grouped) {
                if (!singleTest) {
                    add("${taskKey.modulePath}:clean${taskKey.taskName.replaceFirstChar { it.uppercaseChar() }}")
                }
                add("${taskKey.modulePath}:${taskKey.taskName}")
                for (filter in filters) {
                    add("--tests")
                    add(filter)
                }
            }

            if (!singleTest) {
                add("--continue")
            }

            if (extraGradleArgs != null) {
                addAll(extraGradleArgs.split(" ").filter { it.isNotBlank() })
            }
        }

    return GradleCommand(args)
}

private fun buildTestFilter(test: MatchedTest): String =
    buildString {
        append(test.className)
        if (test.methodName != null) {
            append(".")
            append(test.methodName)
        }
    }
