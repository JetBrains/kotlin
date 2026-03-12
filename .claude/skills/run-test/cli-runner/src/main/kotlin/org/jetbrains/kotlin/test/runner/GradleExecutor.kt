package org.jetbrains.kotlin.test.runner

import java.io.File

class GradleExecutor(
    private val projectRoot: String,
    private val verbose: Boolean = false,
) {
    private val gradlew: String
        get() {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            return if (isWindows) "gradlew.bat" else "./gradlew"
        }

    fun execute(vararg arguments: String): Int {
        val command = listOf(gradlew) + arguments.toList()

        if (verbose) {
            println("[GradleExecutor] Running: ${command.joinToString(" ")}")
            println("[GradleExecutor] Working directory: $projectRoot")
        }

        val process =
            ProcessBuilder(command)
                .directory(File(projectRoot))
                .inheritIO()
                .start()

        val exitCode = process.waitFor()

        if (verbose) {
            println("[GradleExecutor] Exit code: $exitCode")
        }

        return exitCode
    }
}
