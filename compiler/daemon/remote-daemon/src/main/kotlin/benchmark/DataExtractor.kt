/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmark

import java.io.File


data class Task(
    val name: String,
    val compilerArgs: List<String>,
    val messages: List<String>
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
//                println("Task name: $taskName")

                val compilerArgs = line.substringAfter(compileTaskSearchString).split(" ").toMutableList()

//                println("Compiler args:")
//                for (arg in compilerArgs) {
//                    println(arg)
//                }

                // extract compiler messages
                val messages = mutableListOf<String>()
                var nextIndex = index + 1
                while (nextIndex < lines.size && lines[nextIndex].contains(messageSearchString)) {
//                    println("Message: ${lines[nextIndex].substringAfter(messageSearchString)}")
                    nextIndex++
                    messages.add(lines[nextIndex].substringAfter(messageSearchString))
                }
                tasks.add(Task(taskName, compilerArgs, messages))
            }
        }
        return tasks
    }
}