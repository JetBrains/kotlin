/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.extensions.PreprocessedFileCreator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.hmppModuleName
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import java.io.File

fun CompilerConfiguration.report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation? = null) {
    messageCollector.report(severity, message, location)
}

fun List<KotlinSourceRoot>.forAllFiles(
    configuration: CompilerConfiguration,
    project: Project,
    reportLocation: CompilerMessageLocation? = null,
    body: (VirtualFile, Boolean, moduleName: String?) -> Unit
) {
    val localFileSystem = VirtualFileManager.getInstance()
        .getFileSystem(StandardFileSystems.FILE_PROTOCOL)

    val processedFiles = hashSetOf<VirtualFile>()

    val virtualFileCreator = PreprocessedFileCreator(project)

    var pluginsConfigured = false
    fun ensurePluginsConfigured() {
        if (!pluginsConfigured) {
            for (extension in CompilerConfigurationExtension.getInstances(project)) {
                extension.updateFileRegistry()
            }
            pluginsConfigured = true
        }
    }

    for ((sourceRootPath, isCommon, hmppModuleName) in this) {
        val sourceRoot = File(sourceRootPath)
        val vFile = localFileSystem.findFileByPath(sourceRoot.normalize().path)
        if (vFile == null) {
            val message = "Source file or directory not found: $sourceRootPath"

            val buildFilePath = configuration.get(JVMConfigurationKeys.MODULE_XML_FILE)
            if (buildFilePath != null && Logger.isInitialized()) {
                Logger.getInstance(KotlinCoreEnvironment::class.java)
                    .warn("$message\n\nbuild file path: $buildFilePath\ncontent:\n${buildFilePath.readText()}")
            }

            configuration.report(CompilerMessageSeverity.ERROR, message, reportLocation)
            continue
        }

        if (!vFile.isDirectory && vFile.extension != KotlinFileType.EXTENSION) {
            ensurePluginsConfigured()
            if (vFile.fileType != KotlinFileType.INSTANCE) {
                configuration.report(CompilerMessageSeverity.ERROR, "Source entry is not a Kotlin file: $sourceRootPath", reportLocation)
                continue
            }
        }

        for (file in sourceRoot.walkTopDown()) {
            if (!file.isFile) continue

            val virtualFile = localFileSystem.findFileByPath(file.absoluteFile.normalize().path)?.let(virtualFileCreator::create)
            if (virtualFile != null && processedFiles.add(virtualFile)) {
                if (virtualFile.extension != KotlinFileType.EXTENSION) {
                    ensurePluginsConfigured()
                }
                if (virtualFile.extension == KotlinFileType.EXTENSION || virtualFile.fileType == KotlinFileType.INSTANCE) {
                    body(virtualFile, isCommon, hmppModuleName)
                }
            }
        }
    }
}

fun createSourceFilesFromSourceRoots(
    configuration: CompilerConfiguration,
    project: Project,
    sourceRoots: List<KotlinSourceRoot>,
    reportLocation: CompilerMessageLocation? = null
): MutableList<KtFile> {
    val psiManager = PsiManager.getInstance(project)
    val result = mutableListOf<KtFile>()
    sourceRoots.forAllFiles(configuration, project, reportLocation) { virtualFile, isCommon, moduleName ->
        psiManager.findFile(virtualFile)?.let {
            if (it is KtFile) {
                it.isCommonSource = isCommon
                if (moduleName != null) {
                    it.hmppModuleName = moduleName
                }
                result.add(it)
            }
        }
    }
    return result
}

val KotlinCoreEnvironment.messageCollector: MessageCollector
    get() = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

fun CompilerConfiguration.createConfigurationForModule(module: Module, buildFile: File?): CompilerConfiguration {
    return copy().apply {
        applyModuleProperties(module, buildFile)
    }
}

fun CompilerConfiguration.applyModuleProperties(module: Module, buildFile: File?) {
    if (buildFile == null) return

    fun checkKeyIsNull(key: CompilerConfigurationKey<*>, name: String) {
        assert(get(key) == null) { "$name should be null, when buildFile is used" }
    }

    checkKeyIsNull(JVMConfigurationKeys.OUTPUT_DIRECTORY, "OUTPUT_DIRECTORY")
    checkKeyIsNull(JVMConfigurationKeys.OUTPUT_JAR, "OUTPUT_JAR")
    put(JVMConfigurationKeys.OUTPUT_DIRECTORY, File(module.getOutputDirectory()))
}

fun getSourceRootsCheckingForDuplicates(configuration: CompilerConfiguration, messageCollector: MessageCollector?): List<KotlinSourceRoot> {
    val uniqueSourceRoots = hashSetOf<String>()
    val result = mutableListOf<KotlinSourceRoot>()

    for (root in configuration.kotlinSourceRoots) {
        if (!uniqueSourceRoots.add(root.path)) {
            messageCollector?.report(CompilerMessageSeverity.STRONG_WARNING, "Duplicate source root: ${root.path}")
        }
        result.add(root)
    }

    return result
}


