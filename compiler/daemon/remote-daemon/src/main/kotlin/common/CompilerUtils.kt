/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import model.CompilationMetadata

import server.core.WorkspaceManager
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
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
        if (X_PLUGIN_ARG in map) {
            val clientPlugins = map[X_PLUGIN_ARG]?.split(",") ?: emptyList()
            val remotePlugins = clientPlugins.joinToString(",") { clientPath ->
                // TODO: proper solution
                // THIS IS JUST A WORKAROUND, because we use a compiler from Kotlin project and the older libraries are not compatible
                // with the current Kotlin compiler
                if (clientPath.contains("kotlin-scripting-compiler-impl-embeddable")) {
                    "/Users/michal.svec/Desktop/kotlin/plugins/scripting/scripting-compiler-impl-embeddable/build/libs/kotlin-scripting-compiler-impl-embeddable-2.3.255-SNAPSHOT.jar"
                } else if (clientPath.contains("kotlin-scripting-compiler-embeddable")) {
                    "/Users/michal.svec/Desktop/kotlin/plugins/scripting/scripting-compiler-embeddable/build/libs/kotlin-scripting-compiler-embeddable-2.3.255-SNAPSHOT.jar"
                } else if (clientPath.contains("kotlin-serialization-compiler-plugin-embeddable")) {
                    "/Users/michal.svec/Desktop/kotlin/plugins/kotlinx-serialization/kotlinx-serialization.embeddable/build/libs/kotlinx-serialization-compiler-plugin.embeddable-2.3.255-SNAPSHOT.jar"
                } else {
                    clientPath
                }
            }
            map[X_PLUGIN_ARG] = remotePlugins
        }
        return map
    }

    fun getSourceFilePaths(args: Map<String, String>): Set<Path> {
        return args[SOURCE_FILE_ARG]?.split(" ")?.map { Paths.get(it.trim()).toAbsolutePath().normalize() }?.toSet() ?: emptySet()
    }

    fun getDependencyFilePaths(args: Map<String, String>): Set<Path> {
        return args[CLASS_PATH_ARG]?.split(":")?.map { Paths.get(it.trim()).toAbsolutePath().normalize() }?.toSet() ?: emptySet()
    }

    fun getOutputDir(args: Map<String, String>): File {
        return File(args[DIRECTORY_ARG] ?: "")
    }

    fun getModuleName(args: Map<String, String>): String {
        return args[MODULE_NAME_ARG] ?: ""
    }

    fun getCompilerPluginFilesPath(args: Map<String, String>): Set<Path> {
        return args[X_PLUGIN_ARG]?.split(",")?.map { Paths.get(it.trim()).toAbsolutePath().normalize() }?.toSet() ?: emptySet()
    }

    fun getFragmentSources(args: Map<String, String>): Map<String, Set<Path>> {
        val fragmentSourcesList = args[X_FRAGMENT_SOURCES_ARG]?.split(",") ?: emptyList()
        val fragmentSourcesMap = mutableMapOf<String, MutableSet<Path>>()
        fragmentSourcesList.forEach { source ->
            val (fragmentName, clientPathString) = source.split(":").map { it.trim() }
            val clientPath = Paths.get(clientPathString).toAbsolutePath().normalize()
            fragmentSourcesMap.getOrPut(fragmentName) { mutableSetOf() }.add(clientPath)
        }
        return fragmentSourcesMap
    }

    fun getXFriendPaths(args: Map<String, String>): Set<Path> {
        return args[X_FRIEND_PATHS_ARG]
            ?.split(",")
            ?.map { Paths.get(it.trim()).toAbsolutePath().normalize() }
            ?.toSet()
            ?: emptySet()
    }

    fun replaceClientPathsWithRemotePaths(
        userId: String,
        compilationMetadata: CompilationMetadata,
        workspaceManager: WorkspaceManager,
        dependencyFiles: ConcurrentHashMap<Path, File>,
        sourceFiles: ConcurrentHashMap<Path, File>,
        compilerPluginFiles: ConcurrentHashMap<Path, File>
    ): Map<String, String> {

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

        val args = compilationMetadata.compilerArguments.toMutableMap()

        if (DIRECTORY_ARG in args) {
            args[DIRECTORY_ARG] =
                workspaceManager.getOutputDir(userId, compilationMetadata.projectName, getModuleName(args)).toAbsolutePath().toString()
        }

        if (CLASS_PATH_ARG in args) {
            val clientDependencies = getDependencyFilePaths(args)
            val remoteDependencies =
                clientDependencies.joinToString(":") { clientPath -> dependencyFiles[clientPath]?.absolutePath.toString() }
            args[CLASS_PATH_ARG] = remoteDependencies
        }

        if (X_PLUGIN_ARG in args) {
            val clientPlugins = args[X_PLUGIN_ARG]?.split(",") ?: emptyList()
            val remotePlugins = clientPlugins.joinToString(",") { clientPath ->
                compilerPluginFiles[Paths.get(clientPath).normalize()]?.absolutePath.toString()
            }
            args[X_PLUGIN_ARG] = remotePlugins
        }

        if (X_FRAGMENT_SOURCES_ARG in args) {
            val fragmentSources = getFragmentSources(args)
            val remoteFragmentSources = fragmentSources.map { (fragmentName, clientPaths) ->
                clientPaths.joinToString(",") { clientPath ->
                    "$fragmentName:${sourceFiles[clientPath]?.toPath()?.toAbsolutePath()}"
                }
            }.joinToString(",")
            args[X_FRAGMENT_SOURCES_ARG] = remoteFragmentSources
        }

        if (X_FRIEND_PATHS_ARG in args) {
            val clientPaths = getXFriendPaths(args)
            val remotePaths = clientPaths.joinToString(",") { clientPath ->
                dependencyFiles[clientPath]?.absolutePath.toString()
            }
            args[X_FRIEND_PATHS_ARG] = remotePaths
        }

        if (SOURCE_FILE_ARG in args) {
            args[SOURCE_FILE_ARG] = sourceFiles.values.joinToString(" ") { it.absolutePath }
        }
        return args
    }

    fun getCompilerArgumentsList(args: Map<String, String>): List<String>{
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