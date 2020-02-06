package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CopyCommonSources : DefaultTask() {
    @Input
    var zipSources: Boolean = false

    @InputFiles
    var sourcePaths: ConfigurableFileCollection = project.files()

    @OutputDirectory
    var outputDir: File = project.buildDir.resolve("sources")

    fun zipSources(needToZip: Boolean) {
        zipSources = needToZip
    }

    fun outputDir(path: Any) {
        outputDir = project.file(path)
    }

    fun sourcePaths(paths: Any) {
        sourcePaths = project.files(paths)
    }

    @TaskAction
    fun copySources() {
        if (zipSources) copyAndZip() else copyPlain()
    }

    private fun copyPlain() {
        for (sourcePath in sourcePaths) {
            sourcePath.copyFilteredTo(outputDir)
        }
    }

    private fun copyAndZip() {
        for (sourcePath in sourcePaths) {
            val filePrefix = sourcePath.name.replace(Regex("-\\d+.*"), "")
            val targetFileName = "$filePrefix-sources.zip"

            val tempDir = project.buildDir.resolve(name).resolve(filePrefix).also {
                it.deleteRecursively()
                it.mkdirs()
            }

            sourcePath.copyFilteredTo(tempDir)

            project.ant.invokeMethod(
                    "zip",
                    mapOf(
                            "destfile" to outputDir.resolve(targetFileName).absolutePath,
                            "basedir" to tempDir.absolutePath
                    )
            )
        }
    }

    private fun File.copyFilteredTo(destinationDir: File) {
        val fileTree = if (isFile) project.zipTree(this) else project.fileTree(this)

        project.copy {
            it.from(fileTree)
            it.includeEmptyDirs = false
            it.include("generated/**/*.kt")
            it.include("kotlin/**/*.kt")
            it.include("kotlin.test/*.kt")
            it.into(destinationDir)
        }
    }
}

