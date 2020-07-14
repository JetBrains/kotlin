/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.PreprocessedFileCreator
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.multiplatform.isCommonSource

fun CompilerConfiguration.report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation? = null) {
    get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)?.report(severity, message, location)
}

fun createSourceFilesFromSourceRoots(
    configuration: CompilerConfiguration,
    project: Project,
    sourceRoots: List<KotlinSourceRoot>,
    reportLocation: CompilerMessageLocation? = null
): MutableList<KtFile> {
    val fileManager = VirtualFileManager.getInstance()
    val localFileSystem = fileManager.getFileSystem(StandardFileSystems.FILE_PROTOCOL)
    val jarFileSystem = fileManager.getFileSystem(StandardFileSystems.JAR_PROTOCOL)
    val psiManager = PsiManager.getInstance(project)

    val processedFiles = hashSetOf<VirtualFile>()
    val result = mutableListOf<KtFile>()

    val virtualFileCreator = PreprocessedFileCreator(project)

    for ((sourceRootPath, isCommon) in sourceRoots) {
        var vFile = localFileSystem.findFileByPath(sourceRootPath)
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


        val isFile = !vFile.isDirectory
        if (isFile && vFile.path.endsWith(".srcjar")) {
            vFile = jarFileSystem.findFileByPath(vFile.path + JAR_SEPARATOR)
            if (vFile == null) {
                configuration.report(CompilerMessageSeverity.ERROR, "Source entry is not a valid srcjar: $sourceRootPath", reportLocation)
                continue
            }
        } else if (isFile && vFile.fileType != KotlinFileType.INSTANCE) {
            configuration.report(CompilerMessageSeverity.ERROR, "Source entry is not a Kotlin file: $sourceRootPath", reportLocation)
            continue
        }

        VfsUtilCore.processFilesRecursively(vFile) { child ->
            if (child.isDirectory) return@processFilesRecursively true

            val virtualFile = virtualFileCreator.create(child)
            if (processedFiles.add(virtualFile)) {
                val psiFile = psiManager.findFile(virtualFile)
                if (psiFile is KtFile) {
                    result.add(psiFile)
                    if (isCommon) {
                        psiFile.isCommonSource = true
                    }
                }
            }
            return@processFilesRecursively true
        }
    }

    return result
}