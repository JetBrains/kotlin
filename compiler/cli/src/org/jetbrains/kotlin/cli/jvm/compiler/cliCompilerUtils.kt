/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.GenerationStateEventCallback
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.session.IncrementalCompilationContext
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectEnvironment
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.JavaRootPath
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import java.io.File

fun Module.getSourceFiles(
    allSourceFiles: List<KtFile>,
    localFileSystem: VirtualFileSystem?,
    multiModuleChunk: Boolean,
    buildFile: File?
): List<KtFile> {
    return if (multiModuleChunk) {
        // filter out source files from other modules
        assert(buildFile != null) { "Compiling multiple modules, but build file is null" }
        val (moduleSourceDirs, moduleSourceFiles) =
            getBuildFilePaths(buildFile, getSourceFiles())
                .mapNotNull(localFileSystem!!::findFileByPath)
                .partition(VirtualFile::isDirectory)

        allSourceFiles.filter { file ->
            val virtualFile = file.virtualFile
            virtualFile in moduleSourceFiles || moduleSourceDirs.any { dir ->
                VfsUtilCore.isAncestor(dir, virtualFile, true)
            }
        }
    } else {
        allSourceFiles
    }
}

fun getBuildFilePaths(buildFile: File?, sourceFilePaths: List<String>): List<String> =
    if (buildFile == null) sourceFilePaths
    else sourceFilePaths.map { path ->
        (File(path).takeIf(File::isAbsolute) ?: buildFile.resolveSibling(path)).absolutePath
    }

fun GenerationState.Builder.withModule(module: Module?) =
    apply {
        if (module != null) {
            targetId(TargetId(module))
            moduleName(module.getModuleName())
            outDirectory(File(module.getOutputDirectory()))
        }
    }


fun createOutputFilesFlushingCallbackIfPossible(configuration: CompilerConfiguration): GenerationStateEventCallback {
    if (configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY) == null) {
        return GenerationStateEventCallback.DO_NOTHING
    }
    return GenerationStateEventCallback { state ->
        val currentOutput = SimpleOutputFileCollection(state.factory.currentOutput)
        writeOutput(configuration, currentOutput, null)
        if (!configuration.get(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, false)) {
            state.factory.releaseGeneratedOutput()
        }
    }
}

fun writeOutput(
    configuration: CompilerConfiguration,
    outputFiles: OutputFileCollection,
    mainClassFqName: FqName?
) {
    val reportOutputFiles = configuration.getBoolean(CommonConfigurationKeys.REPORT_OUTPUT_FILES)
    val jarPath = configuration.get(JVMConfigurationKeys.OUTPUT_JAR)
    val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    if (jarPath != null) {
        val includeRuntime = configuration.get(JVMConfigurationKeys.INCLUDE_RUNTIME, false)
        val noReflect = configuration.get(JVMConfigurationKeys.NO_REFLECT, false)
        val resetJarTimestamps = !configuration.get(JVMConfigurationKeys.NO_RESET_JAR_TIMESTAMPS, false)
        CompileEnvironmentUtil.writeToJar(
            jarPath,
            includeRuntime,
            noReflect,
            resetJarTimestamps,
            mainClassFqName,
            outputFiles,
            messageCollector
        )
        if (reportOutputFiles) {
            val message = OutputMessageUtil.formatOutputMessage(outputFiles.asList().flatMap { it.sourceFiles }.distinct(), jarPath)
            messageCollector.report(CompilerMessageSeverity.OUTPUT, message)
        }
        return
    }

    val outputDir = configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
        ?.takeUnless { it.path.isBlank() }
        ?: File(".")

    outputFiles.writeAll(outputDir, messageCollector, reportOutputFiles)
}

fun writeOutputsIfNeeded(
    project: Project,
    configuration: CompilerConfiguration,
    messageCollector: MessageCollector,
    outputs: Collection<GenerationState>,
    mainClassFqName: FqName?
): Boolean {
    if (messageCollector.hasErrors()) {
        return false
    }

    try {
        for (state in outputs) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            writeOutput(state.configuration, state.factory, mainClassFqName)
        }
    } finally {
        outputs.forEach(GenerationState::destroy)
    }

    if (configuration.getBoolean(JVMConfigurationKeys.COMPILE_JAVA)) {
        val singleState = outputs.singleOrNull()
        if (singleState != null) {
            return JavacWrapper.getInstance(project).use {
                it.compile(singleState.outDirectory)
            }
        } else {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "A chunk contains multiple modules (${outputs.joinToString { it.moduleName }}). " +
                        "-Xuse-javac option couldn't be used to compile java files"
            )
        }
    }

    return true
}

fun ModuleBuilder.configureFromArgs(args: K2JVMCompilerArguments) {
    args.friendPaths?.forEach { addFriendDir(it) }
    args.classpath?.split(File.pathSeparator)?.forEach { addClasspathEntry(it) }
    args.javaSourceRoots?.forEach {
        addJavaSourceRoot(JavaRootPath(it, args.javaPackagePrefix))
    }

    val commonSources = args.commonSources?.toSet().orEmpty()
    for (arg in args.freeArgs) {
        if (arg.endsWith(JavaFileType.DOT_DEFAULT_EXTENSION)) {
            addJavaSourceRoot(JavaRootPath(arg, args.javaPackagePrefix))
        } else {
            addSourceFiles(arg)
            if (arg in commonSources) {
                addCommonSourceFiles(arg)
            }

            if (File(arg).isDirectory) {
                addJavaSourceRoot(JavaRootPath(arg, args.javaPackagePrefix))
            }
        }
    }
}

fun createContextForIncrementalCompilation(
    projectEnvironment: AbstractProjectEnvironment,
    incrementalComponents: IncrementalCompilationComponents?,
    moduleConfiguration: CompilerConfiguration,
    targetIds: List<TargetId>?,
    sourceScope: AbstractProjectFileSearchScope,
): IncrementalCompilationContext? {
    if (targetIds == null || incrementalComponents == null) return null
    val directoryWithIncrementalPartsFromPreviousCompilation =
        moduleConfiguration[JVMConfigurationKeys.OUTPUT_DIRECTORY]
            ?: return null
    val incrementalCompilationScope = directoryWithIncrementalPartsFromPreviousCompilation.walk()
        .filter { it.extension == "class" }
        .let { projectEnvironment.getSearchScopeByIoFiles(it.asIterable()) }
        .takeIf { !it.isEmpty }
        ?: return null
    val packagePartProvider = IncrementalPackagePartProvider(
        projectEnvironment.getPackagePartProvider(sourceScope),
        targetIds.map(incrementalComponents::getIncrementalCache)
    )
    return IncrementalCompilationContext(emptyList(), packagePartProvider, incrementalCompilationScope)
}

fun createLibraryListForJvm(
    moduleName: String,
    configuration: CompilerConfiguration,
    friendPaths: List<String>
): DependencyListForCliModule {
    val binaryModuleData = BinaryModuleData.initialize(
        Name.identifier(moduleName),
        JvmPlatforms.unspecifiedJvmPlatform,
        JvmPlatformAnalyzerServices
    )
    val libraryList = DependencyListForCliModule.build(binaryModuleData) {
        dependencies(configuration.jvmClasspathRoots.map { it.toPath() })
        dependencies(configuration.jvmModularRoots.map { it.toPath() })
        friendDependencies(configuration[JVMConfigurationKeys.FRIEND_PATHS] ?: emptyList())
        friendDependencies(friendPaths)
    }
    return libraryList
}


