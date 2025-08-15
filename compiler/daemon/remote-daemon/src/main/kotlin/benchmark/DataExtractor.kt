/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmark

import java.io.File


data class Task(
    val name: String,
    val compilerArgs: List<String> = emptyList(),
    val messages: List<String> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val sourceFiles: List<String> = emptyList(),
    val outputDirectory: String,
)

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

                val compilerArgs = line.substringAfter(compileTaskSearchString).split(" ").toMutableList()

                // extract a list of dependencies from the compiler args
                val dependenciesIndex = compilerArgs.indexOfFirst { it.trim() == "-classpath" } + 1
                val dependencies = compilerArgs[dependenciesIndex].split(":").toMutableList()
                compilerArgs.removeAt(dependenciesIndex)
                compilerArgs.removeAt(dependenciesIndex - 1)
                println("Dependencies: $dependencies")

                // extract the output directory from the compiler args
                val outputDirectoryIndex = compilerArgs.indexOfFirst { it.trim() == "-d" } + 1
                val outputDirectory = compilerArgs[outputDirectoryIndex]
                // in general we do not need the output directory
                // but it is useful for benchmarking because we need return back content of the directory
                // compilerArgs.removeAt(outputDirectoryIndex)
                // compilerArgs.removeAt(outputDirectoryIndex - 1)

                // extract a list of source files from the compiler args
                val sourceFiles = mutableListOf<String>()
                for ((index, value) in compilerArgs.asReversed().withIndex()) {
                    if (value.startsWith("/")) {
                        sourceFiles.add(value)
                    } else {
                        compilerArgs.lastIndex - index
                        //clear source files from the original list of compiler arguments
                        compilerArgs.subList(compilerArgs.lastIndex - index + 1, compilerArgs.size).clear()
                        break
                    }
                }
                println("Source files: $sourceFiles")

                println("Remaining compiler args:")
                for (arg in compilerArgs) {
                    println(arg)
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