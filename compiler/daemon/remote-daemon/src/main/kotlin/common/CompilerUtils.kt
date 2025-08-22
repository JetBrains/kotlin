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

    const val SOURCE_FILE_ARG = ""
    const val DIRECTORY_ARG = "-d"
    const val CLASS_PATH_ARG = "-classpath"
    const val JDK_HOME_ARG = "-jdk-home"
    const val MODULE_NAME_ARG = "-module-name"
    const val X_PLUGIN_ARG = "-Xplugin="
    const val X_FRIEND_PATHS_ARG = "-Xfriend-paths="
    const val X_FRAGMENT_SOURCES_ARG = "-Xfragment-sources="
    const val X_MODULE_PATH_ARG = "-Xmodule-path="
    const val X_BUILD_FILE_ARG = "-Xbuild-file="
    const val X_PROFILE_ARG = "-Xprofile="
    const val X_JAVA_SOURCE_ROOTS_ARG = "-Xjava-source-roots="
    const val X_KLIB_ARG = "-Xklib="


    fun getMap(args: List<String>): MutableMap<String, String> {
        //  TODO this must be verified
        val argumentsExpectingFilePath = setOf(
            DIRECTORY_ARG,
            CLASS_PATH_ARG,
            JDK_HOME_ARG,
            X_MODULE_PATH_ARG,
            X_BUILD_FILE_ARG,
            X_JAVA_SOURCE_ROOTS_ARG,
            X_FRIEND_PATHS_ARG,
            X_KLIB_ARG,
            X_PROFILE_ARG
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
                if (next == null || next.startsWith("-") || (next.startsWith('/') && curr !in argumentsExpectingFilePath)) {
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
        map[SOURCE_FILE_ARG] = sourceFiles.reversed().joinToString(" ")
        map.remove(X_PLUGIN_ARG) //TODO temporary removal to make compilations work
        return map
    }

    fun getSourceFiles(args: Map<String, String>): List<File> {
        return args[SOURCE_FILE_ARG]?.split(" ")?.map { File(it.trim()) } ?: emptyList()
    }

    fun getDependencyFiles(args: Map<String, String>): List<File> {
        return args[CLASS_PATH_ARG]?.split(":")?.map { File(it.trim()) } ?: emptyList()
    }

    fun getOutputDir(args: Map<String, String>): File {
        return File(args[DIRECTORY_ARG] ?: "")
    }

    fun getModuleName(args: Map<String, String>): String {
        return args[MODULE_NAME_ARG] ?: ""
    }

    fun getCompilerPluginFiles(args: Map<String, String>): List<File> {
        return args[X_PLUGIN_ARG]?.split(",")?.map { File(it.trim()) } ?: emptyList()
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

        if (DIRECTORY_ARG in args) {
            args[DIRECTORY_ARG] =
                workspaceManager.getOutputDir(userId, compilationMetadata.projectName, getModuleName(args)).toAbsolutePath().toString()
        }

        if (CLASS_PATH_ARG in args) {
            val clientDependencies = args[CLASS_PATH_ARG]?.split(":") ?: emptyList()
            val remoteDependencies =
                clientDependencies.joinToString(":") { clientPath -> dependencyFiles[clientPath]?.absolutePath.toString() }
            args[CLASS_PATH_ARG] = remoteDependencies
        }

        if (X_PLUGIN_ARG in args) {
            val clientPlugins = args[X_PLUGIN_ARG]?.split(",") ?: emptyList()
            val remotePlugins = clientPlugins.joinToString(",") { clientPath ->
                compilerPluginFiles[clientPath]?.absolutePath.toString()
            }
            args[X_PLUGIN_ARG] = remotePlugins
        }

        if (X_FRAGMENT_SOURCES_ARG in args) {
            val clientSources = args[X_FRAGMENT_SOURCES_ARG]?.split(",") ?: emptyList()
            val remoteSources = clientSources.joinToString(",") {
                val (fragmentName, clientPath) = it.split(":")
                val remotePath = sourceFiles[clientPath]?.absolutePath.toString()
                "$fragmentName:${remotePath}"
            }
            args[X_FRAGMENT_SOURCES_ARG] = remoteSources
        }

        if (X_FRIEND_PATHS_ARG in args) {
            val clientPaths = args[X_FRIEND_PATHS_ARG]?.split(",") ?: emptyList()
            val remotePaths = clientPaths.joinToString(",") { clientPath -> dependencyFiles[clientPath]?.absolutePath.toString() }
            args[X_FRIEND_PATHS_ARG] = remotePaths
        }

        if (SOURCE_FILE_ARG in args) {
            args[SOURCE_FILE_ARG] = sourceFiles.values.joinToString(" ") { it.absolutePath }
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
                key == SOURCE_FILE_ARG -> value.split(' ')
                else -> listOf(key, value)

            }
        }.filter { it.isNotBlank() }
    }
}