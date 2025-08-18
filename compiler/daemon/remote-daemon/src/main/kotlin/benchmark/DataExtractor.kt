/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmark

import java.io.File


data class Task(
    val name: String,
    val compilerArgs: Map<String, String> = mapOf(),
    val messages: List<String> = emptyList(),
    val dependencies: List<File> = emptyList(),
    val sourceFiles: List<File> = emptyList(),
    val outputDirectory: String,
)


fun getMapFromCompilerArguments(args: List<String>): MutableMap<String, String> {
    val map = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val curr = args[i]
        val next = args.getOrNull(i + 1)

        if (curr.startsWith("-")) {
            if ('=' in curr) {
                val (key, value) = curr.split("=")
                map["$key="] = value
                i++
                continue
            }
            if (next == null || next.startsWith("-")) {
                map[curr] = ""
                i++
                continue
            } else {
                map[curr] = next
                i += 2
                continue
            }
        }
        i++
    }
    return map
}

object DataExtractor {

    // run on a file generated using ./gradlew clean && ./gradlew assembleAllKotlin -Pkotlin.internal.compiler.arguments.log.level=warning > output
    fun getTask(filePath: String): List<Task> {
        val compileTaskSearchString = " Kotlin compiler args: "
        val messageSearchString = "w: "
        val tasks = mutableListOf<Task>()

        val lines = File(filePath).readLines()

        for ((index, line) in lines.withIndex()) {
            if (line.contains(compileTaskSearchString)) {

                // extract task name
                val taskName = line.substringBefore(compileTaskSearchString).substringAfter(":")
                println("Task name: $taskName")

                val compilerArgsList = line.substringAfter(compileTaskSearchString).split(" ").toMutableList()
                val compilerArgs = getMapFromCompilerArguments(compilerArgsList)

                // extract a list of dependencies from the compiler args
                val dependencies = compilerArgs["-classpath"]?.split(":")?.map { File(it) } ?: emptyList()
                compilerArgs.remove("-classpath")
                println("Dependencies: $dependencies")

                // extract the output directory from the compiler args
                val outputDirectory = compilerArgs["-d"]!!
                // in general we do not need the output directory
                // but it is useful for benchmarking because we need to return back content of the directory

                // extract a list of source files from the compiler args
                val sourceFiles = mutableListOf<File>()
                for (value in compilerArgsList.asReversed()) {
                    if (value.startsWith("/")) {
                        sourceFiles.add(File(value))
                    } else {
                        break
                    }
                }
                println("Source files: $sourceFiles")

                println("Remaining compiler args:")
                for (arg in compilerArgs.entries) {
                    println("key ${arg.key}    val ${arg.value}end")
                }

                // extract compiler messages
                val messages = mutableListOf<String>()
                var nextIndex = index + 1
                while (nextIndex < lines.size && lines[nextIndex].contains(messageSearchString)) {
                    println("Message: ${lines[nextIndex].substringAfter(messageSearchString)}")
                    nextIndex++
                    messages.add(lines[nextIndex].substringAfter(messageSearchString))
                }
                tasks.add(Task(taskName, compilerArgs, messages, dependencies, sourceFiles, outputDirectory))
            }
        }
        return tasks
    }
}