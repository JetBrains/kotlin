/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.isFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtVirtualFileSourceFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.allSourceFilesSequence
import org.jetbrains.kotlin.cli.jvm.compiler.findFileByPath
import org.jetbrains.kotlin.cli.jvm.compiler.getSourceRootsCheckingForDuplicates
import org.jetbrains.kotlin.cli.jvm.compiler.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.dontSortSourceFiles
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.extensions.PreprocessedFileCreator
import org.jetbrains.kotlin.fir.extensions.FirProcessSourcesBeforeCompilingExtension
import org.jetbrains.kotlin.idea.KotlinFileType
import java.util.TreeSet

private const val kotlinFileExtensionWithDot = ".${KotlinFileType.EXTENSION}"
private const val javaFileExtensionWithDot = ".${JavaFileType.DEFAULT_EXTENSION}"

data class GroupedKtSources(
    val platformSources: Collection<KtSourceFile>,
    val commonSources: Collection<KtSourceFile>,
    val sourcesByModuleName: Map<String, Set<KtSourceFile>>,
) {
    fun isEmpty(): Boolean = platformSources.isEmpty() && commonSources.isEmpty()
}

val GroupedKtSources.allFiles: List<KtSourceFile>
    get() = platformSources + commonSources

private val ktSourceFileComparator = Comparator<KtSourceFile> { o1, o2 ->
    val path1 = o1.path ?: error("Expected a file with a well-defined path")
    val path2 = o2.path ?: error("Expected a file with a well-defined path")
    path1.compareTo(path2)
}

fun collectSources(
    compilerConfiguration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment,
    messageCollector: MessageCollector
): GroupedKtSources {
    fun createSet(): MutableSet<KtSourceFile> = if (compilerConfiguration.dontSortSourceFiles) {
        mutableSetOf()
    } else {
        TreeSet(ktSourceFileComparator)
    }

    val platformSources = createSet()
    val commonSources = createSet()
    val sourcesByModuleName = mutableMapOf<String, MutableSet<KtSourceFile>>()

    val virtualFileCreator = PreprocessedFileCreator(projectEnvironment.project)

    var pluginsConfigured = false
    fun ensurePluginsConfigured() {
        if (!pluginsConfigured) {
            for (extension in CompilerConfigurationExtension.getInstances(projectEnvironment.project)) {
                extension.updateFileRegistry()
            }
            pluginsConfigured = true
        }
    }
    getSourceRootsCheckingForDuplicates(compilerConfiguration, messageCollector)
        .allSourceFilesSequence(
            compilerConfiguration,
            reportLocation = null,
            findVirtualFile = { projectEnvironment.knownFileSystems.findFileByPath(it.path, StandardFileSystems.FILE_PROTOCOL) },
            accept = { virtualFile, isExplicit ->
                when (virtualFile.extension) {
                    JavaFileType.DEFAULT_EXTENSION -> false
                    KotlinFileType.EXTENSION -> true
                    else -> {
                        if (virtualFile.isFile) {
                            ensurePluginsConfigured()
                            val isKotlin = virtualFile.fileType == KotlinFileType.INSTANCE
                            if (isExplicit && !isKotlin)
                                compilerConfiguration.report(
                                    CompilerMessageSeverity.ERROR,
                                    "Source entry is not a Kotlin file: ${virtualFile.path}"
                                )
                            isKotlin
                        } else false
                    }
                }
            },
            convertToSourceFiles = {
                val sources = listOf(KtVirtualFileSourceFile(virtualFileCreator.create(it)))
                if (it.extension == KotlinFileType.EXTENSION) sources
                else {
                    // currently applying the extension only to non-kt files, e.g. scripts
                    FirProcessSourcesBeforeCompilingExtension.processSources(
                        projectEnvironment.project, projectEnvironment, compilerConfiguration, sources
                    ) ?: sources
                }
            }
        ).forEach { fileInfo ->
            fileInfo.sourceFiles.forEach { file ->
                if (fileInfo.isCommon) commonSources.add(file)
                else platformSources.add(file)

                fileInfo.moduleName?.let {
                    sourcesByModuleName.getOrPut(it) { mutableSetOf() }.add(file)
                }
            }
        }

    return GroupedKtSources(platformSources, commonSources, sourcesByModuleName)
}
