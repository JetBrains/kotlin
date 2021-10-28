/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.PreprocessedFileCreator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource
import java.io.File

fun CompilerConfiguration.report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation? = null) {
    get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)?.report(severity, message, location)
}

inline fun List<KotlinSourceRoot>.forAllFiles(
    configuration: CompilerConfiguration,
    project: Project,
    reportLocation: CompilerMessageLocation? = null,
    body: (VirtualFile, Boolean) -> Unit
) {
    val localFileSystem = VirtualFileManager.getInstance()
        .getFileSystem(StandardFileSystems.FILE_PROTOCOL)

    val processedFiles = hashSetOf<VirtualFile>()

    val virtualFileCreator = PreprocessedFileCreator(project)

    for ((sourceRootPath, isCommon) in this) {
        val vFile = localFileSystem.findFileByPath(sourceRootPath)
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

        if (!vFile.isDirectory && vFile.fileType != KotlinFileType.INSTANCE) {
            configuration.report(CompilerMessageSeverity.ERROR, "Source entry is not a Kotlin file: $sourceRootPath", reportLocation)
            continue
        }

        for (file in File(sourceRootPath).walkTopDown()) {
            if (!file.isFile) continue

            val virtualFile = localFileSystem.findFileByPath(file.absolutePath)?.let(virtualFileCreator::create)
            if (virtualFile != null && processedFiles.add(virtualFile)) {
                body(virtualFile, isCommon)
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
    sourceRoots.forAllFiles(configuration, project, reportLocation) { virtualFile, isCommon ->
        psiManager.findFile(virtualFile)?.let {
            if (it is KtFile) {
                it.isCommonSource = isCommon
                result.add(it)
            }
        }
    }
    return result
}

val KotlinCoreEnvironment.messageCollector: MessageCollector
    get() = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

fun CompilerConfiguration.applyModuleProperties(module: Module, buildFile: File?): CompilerConfiguration {
    return copy().apply {
        if (buildFile != null) {
            fun checkKeyIsNull(key: CompilerConfigurationKey<*>, name: String) {
                assert(get(key) == null) { "$name should be null, when buildFile is used" }
            }

            checkKeyIsNull(JVMConfigurationKeys.OUTPUT_DIRECTORY, "OUTPUT_DIRECTORY")
            checkKeyIsNull(JVMConfigurationKeys.OUTPUT_JAR, "OUTPUT_JAR")
            put(JVMConfigurationKeys.OUTPUT_DIRECTORY, File(module.getOutputDirectory()))
        }
    }
}

