/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.OutputMessageUtil
import org.jetbrains.kotlin.cli.common.output.writeAll
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.GenerationStateEventCallback
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jdom.Attribute
import org.jdom.Document
import org.jdom.Element
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.config.*
import java.io.File


fun dumpModel(dir: String, chunk: List<Module>, configuration: CompilerConfiguration) {

    val modules = Element("modules").apply {
        for (module in chunk) {
            addContent(Element("module").apply {
                attributes.add(
                    Attribute("timestamp", System.currentTimeMillis().toString())
                )

                attributes.add(
                    Attribute("name", module.getModuleName())
                )
                attributes.add(
                    Attribute("type", module.getModuleType())
                )
                attributes.add(
                    Attribute("outputDir", module.getOutputDirectory())
                )

                for (friendDir in module.getFriendPaths()) {
                    addContent(Element("friendDir").setAttribute("path", friendDir))
                }
                for (source in module.getSourceFiles()) {
                    addContent(Element("sources").setAttribute("path", source))
                }
                for (javaSourceRoots in module.getJavaSourceRoots()) {
                    addContent(
                        Element("javaSourceRoots").apply {
                            setAttribute("path", javaSourceRoots.path)
                            javaSourceRoots.packagePrefix?.let { setAttribute("packagePrefix", it) }
                        }
                    )
                }
                for (classpath in configuration.get(CLIConfigurationKeys.CONTENT_ROOTS).orEmpty()) {
                    if (classpath is JvmClasspathRoot) {
                        addContent(Element("classpath").setAttribute("path", classpath.file.absolutePath))
                    } else if (classpath is JvmModulePathRoot) {
                        addContent(Element("modulepath").setAttribute("path", classpath.file.absolutePath))
                    }
                }
                for (commonSources in module.getCommonSourceFiles()) {
                    addContent(Element("commonSources").setAttribute("path", commonSources))
                }
                module.modularJdkRoot?.let {
                    addContent(Element("modularJdkRoot").setAttribute("path", module.modularJdkRoot))
                }
                attributes.add(
                    Attribute("jdkHome", configuration.get(JVMConfigurationKeys.JDK_HOME)?.absolutePath)
                )
                for (optInAnnotation in configuration.languageVersionSettings.getFlag(AnalysisFlags.useExperimental)) {
                    addContent(Element("useOptIn").setAttribute("annotation", optInAnnotation))
                }
            })
        }
    }
    val document = Document(modules)
    val outputter = XMLOutputter(Format.getPrettyFormat())
    val dirFile = File(dir)
    if (!dirFile.exists()) {
        dirFile.mkdirs()
    }
    val fileName = "model-${chunk.first().getModuleName()}"
    var counter = 0
    fun file(): File {
        val postfix = if (counter != 0) ".$counter" else ""
        return File(dirFile, "$fileName$postfix.xml")
    }

    var outputFile: File
    do {
        outputFile = file()
        counter++
    } while (outputFile.exists())
    outputFile.bufferedWriter().use {
        outputter.output(document, it)
    }

}

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

    val outputDir = configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY) ?: File(".")
    outputFiles.writeAll(outputDir, messageCollector, reportOutputFiles)
}

fun writeOutputs(
    project: Project?,
    projectConfiguration: CompilerConfiguration,
    chunk: List<Module>,
    outputs: Map<Module, GenerationState>,
    mainClassFqName: FqName?
): Boolean {
    try {
        for ((_, state) in outputs) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            writeOutput(state.configuration, state.factory, mainClassFqName)
        }
    } finally {
        outputs.values.forEach(GenerationState::destroy)
    }

    if (projectConfiguration.getBoolean(JVMConfigurationKeys.COMPILE_JAVA)) {
        val singleModule = chunk.singleOrNull()
        if (singleModule != null) {
            return JavacWrapper.getInstance(project!!).use {
                it.compile(File(singleModule.getOutputDirectory()))
            }
        } else {
            projectConfiguration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
                CompilerMessageSeverity.WARNING,
                "A chunk contains multiple modules (${chunk.joinToString { it.getModuleName() }}). " +
                        "-Xuse-javac option couldn't be used to compile java files"
            )
        }
    }

    return true
}
