/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions
import org.jetbrains.kotlin.incremental.ClasspathChanges
import org.jetbrains.kotlin.incremental.ClasspathSnapshotFiles
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

    private fun getRemotePath(map: Map<Path, File>?, clientPath: Path, workspaceManager: WorkspaceManager): String {
        return map?.get(clientPath)?.absolutePath ?: workspaceManager.prependWorkspaceProjectDirectory(clientPath).toString()
    }

    fun getRemoteCompilerArguments(
        compilerArguments: List<String>,
        workspaceManager: WorkspaceManager,
        sourceFiles: ConcurrentHashMap<Path, File>?,
        dependencyFiles: ConcurrentHashMap<Path, File>?,
        compilerPluginFiles: ConcurrentHashMap<Path, File>?,
    ): K2JVMCompilerArguments {
        val parsed = parseCommandLineArguments<K2JVMCompilerArguments>(compilerArguments)

        // replace source file paths
        parsed.freeArgs = parsed.freeArgs.map { freeArg ->
            val clientPath = Paths.get(freeArg.trim())
            if (freeArg.startsWith("/")) {
                getRemotePath(sourceFiles, clientPath, workspaceManager)
            } else {
                freeArg
            }
        }

        // replace friend paths
        if (parsed.friendPaths != null) {
            val remoteFriendPaths = mutableListOf<String>()
            for (fp in parsed.friendPaths){
                val clientPath = Paths.get(fp)
                remoteFriendPaths.add(getRemotePath(dependencyFiles, clientPath, workspaceManager))
            }
            parsed.friendPaths = remoteFriendPaths.toTypedArray()
        }

        // replace output directory
        // TODO revisit module name
        parsed.destination =
            workspaceManager.getOutputDir(parsed.moduleName ?: "module").toString()

        // replace -classpath
        // TODO revisit !!
        if (parsed.classpath != null) {
            parsed.classpathAsList =
                parsed.classpathAsList.map { clientFile -> File(getRemotePath(dependencyFiles, clientFile.toPath(), workspaceManager)) }
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
                    getRemotePath(compilerPluginFiles, Paths.get(clientPath), workspaceManager)
                }
                remoteFriendPaths.add(remotePath)
            }
            parsed.pluginClasspaths = remoteFriendPaths.toTypedArray()
        }

        // replace -Xfragment-sources
        if (parsed.fragmentSources != null) {
            val remoteFragmentSources = mutableListOf<String>()
            for (fs in parsed.fragmentSources) {
                val (fragment, path) = fs.split(":")
                val clientPath = Paths.get(path)
                remoteFragmentSources.add("$fragment:${sourceFiles?.get(clientPath)?.toPath()?.toAbsolutePath() }")
            }
            parsed.fragmentSources = remoteFragmentSources.toTypedArray()
        }

        // TODO, this is just a workaround, the JDK-HOME will be inferred from $JAVA_HOME env variable
        parsed.jdkHome = null
        return parsed
    }

    fun getRemoteCompilationOptions(
        co: CompilationOptions,
        workspaceManager: WorkspaceManager,
        sourceChangesFiles: Map<Path, File>,
        classpathEntrySnapshotFiles: Map<Path, File>,
        shrunkClasspathSnapshotFiles: Map<Path, File>
    ): CompilationOptions {
        if (co !is IncrementalCompilationOptions) {
            return co
        }
        val remoteSourceChanges = when (val srcChanges = co.sourceChanges) {
            is SourcesChanges.Known -> {
                val remoteModified = srcChanges.modifiedFiles.map { client ->
                    sourceChangesFiles[client.toPath()] ?: client
                }
                // TODO: do we want to do something with the removed files?
                SourcesChanges.Known(remoteModified, srcChanges.removedFiles)
            }
            else -> {
                srcChanges
            }
        }

        val remoteClasspathChanges = when (val cpChanges = co.classpathChanges) {
            is ClasspathChanges.ClasspathSnapshotEnabled -> {
                val remoteClasspathEntrySnapshotFiles = cpChanges.classpathSnapshotFiles.currentClasspathEntrySnapshotFiles.map { client ->
                    classpathEntrySnapshotFiles[client.toPath()] ?: client
                }

                val remoteShrunkPreviousClasspathSnapshotFile =
                    shrunkClasspathSnapshotFiles[cpChanges.classpathSnapshotFiles.shrunkPreviousClasspathSnapshotFile.toPath()]

                val remoteClasspathSnapshotFiles = ClasspathSnapshotFiles(
                    currentClasspathEntrySnapshotFiles = remoteClasspathEntrySnapshotFiles,
                    classpathSnapshotDir = remoteShrunkPreviousClasspathSnapshotFile!!.parentFile
                )

                when (cpChanges) {
                    is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler -> {
                        ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(
                            remoteClasspathSnapshotFiles
                        )
                    }
                    is ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableForNonIncrementalRun -> {
                        ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(
                            remoteClasspathSnapshotFiles
                        )
                    }
                    is ClasspathChanges.ClasspathSnapshotEnabled.NotAvailableDueToMissingClasspathSnapshot -> {
                        ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(
                            remoteClasspathSnapshotFiles
                        )
                    }
                    else -> {
                        // TODO double check
                        ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler(
                            remoteClasspathSnapshotFiles
                        )
                    }
                }
            }
            else -> {
                cpChanges
            }
        }

        val remoteWorkingDir = workspaceManager.prependWorkspaceProjectDirectory(co.workingDir.toPath()).toFile()
        val remoteProjectDir = co.rootProjectDir?.let { workspaceManager.prependWorkspaceProjectDirectory(it.toPath()).toFile() }
        val remoteBuildDir = co.buildDir?.let { workspaceManager.prependWorkspaceProjectDirectory(it.toPath()).toFile() }
        val remoteOutputFiles = co.outputFiles?.map { workspaceManager.prependWorkspaceProjectDirectory(it.toPath()).toFile() }

        // TODO: modulesInfo and multiModuleICSettings are assigned to null for JVM compilations, but are used for K/JS which we are not currently targeting
//        val remoteMultiModuleICSettings = ico.multiModuleICSettings?.let {
//            MultiModuleICSettings(
//                buildHistoryFile = workspaceManager.prependWorkspaceProjectDirectory(it.buildHistoryFile.toPath()).toFile(),
//                useModuleDetection = it.useModuleDetection,
//            )
//        }
//        val remoteModulesInfo = ico.modulesInfo?.let {
//            IncrementalModuleInfo(
//                rootProjectBuildDir = workspaceManager.prependWorkspaceProjectDirectory(it.rootProjectBuildDir.toPath()).toFile(),
//                dirToModule = it.dirToModule.mapValues { (_, module) -> module.toProto() },
//
//                )
//        }

        return IncrementalCompilationOptions(
            sourceChanges = remoteSourceChanges,
            classpathChanges = remoteClasspathChanges,
            workingDir = remoteWorkingDir,
            compilerMode = co.compilerMode,
            targetPlatform = co.targetPlatform,
            reportSeverity = co.reportSeverity,
            reportCategories = co.reportCategories,
            requestedCompilationResults = co.requestedCompilationResults,
            useJvmFirRunner = co.useJvmFirRunner,
            outputFiles = remoteOutputFiles,
            multiModuleICSettings = co.multiModuleICSettings,
            modulesInfo = co.modulesInfo,
            rootProjectDir = remoteProjectDir,
            buildDir = remoteBuildDir,
            kotlinScriptExtensions = co.kotlinScriptExtensions,
            icFeatures = co.icFeatures,
        )
    }
}