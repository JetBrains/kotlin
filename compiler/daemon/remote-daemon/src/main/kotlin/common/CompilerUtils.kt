/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import model.CompilationMetadata
import server.core.WorkspaceManager
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2

object CompilerUtils {

    const val SOURCE_FILE_KEY = ""

    fun getMap(args: List<String>): MutableMap<String, String> {
        //  TODO this must be verified
        val expectsPath = setOf(
            "-d",
            "-classpath",
            "-jdk-home",
            "-Xmodule-path",
            "-Xbuild-file",
            "-Xjava-source-roots",
            "-Xfriend-paths",
            "-Xklib",
            "-Xprofile"
        )
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
                if (next == null || next.startsWith("-") || (next.startsWith('/') && curr !in expectsPath)) {
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

        val sourceFiles = mutableListOf<String>()
        for (value in args.asReversed()) {
            if (value.startsWith("/")) {
                sourceFiles.add(value.trim())
            } else {
                break
            }
        }
        map[SOURCE_FILE_KEY] = sourceFiles.reversed().joinToString(" ")
        map.remove("-Xplugin=") //TODO temporary removal to make compilations work
        return map
    }

    fun getSourceFiles(args: Map<String, String>): List<File> {
        return args[SOURCE_FILE_KEY]?.split(" ")?.map { File(it.trim()) } ?: emptyList()
    }

    fun getDependencyFiles(args: Map<String, String>): List<File> {
        return args["-classpath"]?.split(":")?.map { File(it.trim()) } ?: emptyList()
    }

    fun getOutputDir(args: Map<String, String>): File {
        return File(args["-d"] ?: "")
    }

    fun getCompilerPluginFiles(args: Map<String, String>): List<File> {
        return args["-Xplugin="]?.split(",")?.map { File(it.trim()) } ?: emptyList()
    }

    // -d
    // -classpath
    // -jdk-home
    // -Xmodule-path
    // -Xbuild-file
    // -Xjava-source-roots
    // -Xfriend-paths
    // -Xklib
    // -Xprofile
    // <fragment name>:<path>
    // TODO there is more arguments that can take files as values, it needs to be properly handled

    fun replaceClientPathsWithRemotePaths(
        userId: String,
        compilationMetadata: CompilationMetadata,
        workspaceManager: WorkspaceManager,
        dependencyFiles: Map<String, File>,
        sourceFiles: Map<String, File>,
        compilerPluginFiles: Map<String, File>
    ): Map<String, String> {
        val args = compilationMetadata.compilerArguments.toMutableMap()

        if ("-d" in args) {
            args["-d"] = workspaceManager.getOutputDir(userId, compilationMetadata.projectName).toAbsolutePath().toString()
        }

        if ("-classpath" in args) {
            val clientDependencies = args["-classpath"]?.split(":") ?: emptyList()
            val remoteDependencies =
                clientDependencies.joinToString(":") { clientPath -> dependencyFiles[clientPath]?.absolutePath.toString() }
            args["-classpath"] = remoteDependencies
        }

        if ("-Xplugin=" in args) {
            val clientPlugins = args["-Xplugin="]?.split(",") ?: emptyList()
            val remotePlugins = clientPlugins.joinToString(",") { clientPath ->
                compilerPluginFiles[clientPath]?.absolutePath.toString()
            }
            args["-Xplugin="] = remotePlugins
        }

        if ("-Xfragment-sources" in args) {
            val clientSources = args["-Xfragment-sources"]?.split(",") ?: emptyList()
            val remoteSources = clientSources.joinToString(",") {
                val (fragmentName, clientPath) = it.split(":")
                val remotePath = sourceFiles[clientPath]?.absolutePath.toString()
                "$fragmentName:${remotePath}"
            }
            args["-Xfragment-sources"] = remoteSources
        }
        return args
    }

    fun getCompilerArgumentsList(args: Map<String, String>): List<String>{
        for ((key, value) in args) {
            if ('=' in key) println("$key$value") else println("$key $value")
        }
        return args.entries.flatMap { (key, value) ->
            when {
                key.endsWith('=') -> listOf("$key$value")
                value.isEmpty() -> listOf(key)
                key == SOURCE_FILE_KEY -> value.split(' ')
                else -> listOf(key, value)

            }
        }.filter { it.isNotBlank() }
    }
}