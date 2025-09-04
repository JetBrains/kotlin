/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import model.CompilationMetadata
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile

import server.core.WorkspaceManager
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.component1
import kotlin.collections.component2

object CompilerUtils {

    fun parseArgs(compilerArguments: List<String>): K2JVMCompilerArguments {
        return parseCommandLineArguments<K2JVMCompilerArguments>(compilerArguments)
    }

    fun getSourceFiles(args: K2JVMCompilerArguments): List<File> {
        return args.freeArgs.filter { it.startsWith("/") }.map { File(it.trim()) }
    }

    fun getDependencyFiles(args: K2JVMCompilerArguments): List<File> {
        return args.classpathAsList
    }

    fun getXPluginFiles(args: K2JVMCompilerArguments): List<File> {
        return args.pluginClasspaths?.map { File(it.trim()) } ?: emptyList()
    }

    fun getModuleName(args: K2JVMCompilerArguments): String {
        // TODO: revisit null case
        return args.moduleName ?: "not set"
    }

    fun getOutputDir(args: K2JVMCompilerArguments): File {
        return args.destinationAsFile
    }

    fun replacePathsWithRemoteOnes(
        userId: String,
        compilationMetadata: CompilationMetadata,
        workspaceManager: WorkspaceManager,
        sourceFiles: ConcurrentHashMap<Path, File>,
        dependencyFiles: ConcurrentHashMap<Path, File>,
        compilerPluginFiles: ConcurrentHashMap<Path, File>
    ): K2JVMCompilerArguments {
        val parsed = parseCommandLineArguments<K2JVMCompilerArguments>(compilationMetadata.compilerArguments)

        // replace source file paths
        parsed.freeArgs = parsed.freeArgs.map {
            if (it.startsWith("/")) {
                sourceFiles[Paths.get(it.trim())]?.absolutePath.toString()
            } else {
                it
            }
        }

        // replace friend paths
        if (parsed.friendPaths != null) {
            val remoteFriendPaths = mutableListOf<String>()
            for (clientPath in parsed.friendPaths){
                val remotePath = dependencyFiles[Paths.get(clientPath)]?.absolutePath.toString()
                remoteFriendPaths.add(remotePath)
            }
            parsed.friendPaths = remoteFriendPaths.toTypedArray()
        }

        // replace output directory
        // TODO revisit module name
        parsed.destination =
            workspaceManager.getOutputDir(userId, compilationMetadata.projectName, parsed.moduleName ?: "module").toString()

        // replace -classpath
        // TODO revisit !!
        if (parsed.classpath != null) {
            parsed.classpathAsList = parsed.classpathAsList.map { clientFile -> dependencyFiles[clientFile.toPath()]!! }
        }

        // replace -Xcompiler-plugin
        if (parsed.pluginClasspaths != null) {
            val remoteFriendPaths = mutableListOf<String>()
            for (clientPath in parsed.pluginClasspaths){
                val remotePath = if (clientPath.contains("kotlin-scripting-compiler-impl-embeddable")) {
                    File("src/main/kotlin/server/libsworkaround/kotlin-scripting-compiler-impl-embeddable-2.3.255-SNAPSHOT.jar").absolutePath
                } else if (clientPath.contains("kotlin-scripting-compiler-embeddable")) {
                    File("src/main/kotlin/server/libsworkaround/kotlin-scripting-compiler-embeddable-2.3.255-SNAPSHOT.jar").absolutePath
                } else if (clientPath.contains("kotlin-serialization-compiler-plugin-embeddable")) {
                    File("src/main/kotlin/server/libsworkaround/kotlinx-serialization-compiler-plugin.embeddable-2.3.255-SNAPSHOT.jar").absolutePath
                } else if (clientPath.contains("kotlin-assignment-compiler-plugin-embeddable")) {
                    File("src/main/kotlin/server/libsworkaround/kotlin-assignment-compiler-plugin.embeddable-2.3.255-SNAPSHOT.jar").absolutePath
                } else {
                    compilerPluginFiles[Paths.get(clientPath)]?.absolutePath.toString()
                }
                remoteFriendPaths.add(remotePath)
            }
            parsed.pluginClasspaths = remoteFriendPaths.toTypedArray()
        }

        // replace -Xfragment-sources
        if (parsed.fragmentSources != null) {
            val remoteFragmentSources = mutableListOf<String>()
            for (fs in parsed.fragmentSources) {
                val (fragment, clientPaths) = fs.split(":")
                remoteFragmentSources.add("$fragment:${sourceFiles[Paths.get(clientPaths)]?.toPath()?.toAbsolutePath()}")
            }
            parsed.fragmentSources = remoteFragmentSources.toTypedArray()
        }

        return parsed
    }

}