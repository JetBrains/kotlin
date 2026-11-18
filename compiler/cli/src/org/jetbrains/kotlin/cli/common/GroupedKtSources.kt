/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtVirtualFileSourceFile
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.allSourceFilesSequence
import org.jetbrains.kotlin.cli.jvm.compiler.findFileByPath
import org.jetbrains.kotlin.cli.jvm.compiler.getSourceRootsCheckingForDuplicates
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.ParserMode
import org.jetbrains.kotlin.config.dontSortSourceFiles
import org.jetbrains.kotlin.config.parserMode
import org.jetbrains.kotlin.extensions.CompilerConfigurationExtension
import org.jetbrains.kotlin.fir.extensions.CollectAdditionalSourceFilesExtension
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.util.*

data class GroupedKtSources(
    val platformSources: Collection<KtSourceFile>,
    val commonSources: Collection<KtSourceFile>,
    val sourcesByModuleName: Map<String, Set<KtSourceFile>>,
) {
    fun isEmpty(): Boolean {
        return platformSources.isEmpty() && commonSources.isEmpty()
    }

    fun isCommonSource(file: KtSourceFile): Boolean {
        return file in commonSources
    }

    fun fileBelongsToModule(file: KtSourceFile, moduleName: String): Boolean {
        return sourcesByModuleName[moduleName]?.contains(file) == true
    }

    companion object {
        val EMPTY = GroupedKtSources(emptyList(), emptyList(), emptyMap())
    }
}

val GroupedKtSources.allFiles: List<KtSourceFile>
    get() = platformSources + commonSources

private val ktSourceFileComparator = Comparator<KtSourceFile> { o1, o2 ->
    val path1 = o1.path ?: error("Expected a file with a well-defined path")
    val path2 = o2.path ?: error("Expected a file with a well-defined path")
    path1.compareTo(path2)
}

/**
 * Collects the Kotlin sources of the compilation from the source roots in [compilerConfiguration].
 *
 * The kind of the sources depends on [CompilerConfiguration.parserMode], so that they match the raw FIR builder:
 * [KtPsiSourceFile]s for [ParserMode.Psi], and [KtVirtualFileSourceFile]s, which the raw FIR builder parses itself,
 * for the tree-based parser modes.
 */
fun collectSources(
    compilerConfiguration: CompilerConfiguration,
    projectEnvironment: VfsBasedProjectEnvironment
): GroupedKtSources {
    fun createSet(): MutableSet<KtSourceFile> = if (compilerConfiguration.dontSortSourceFiles) {
        mutableSetOf()
    } else {
        TreeSet(ktSourceFileComparator)
    }

    val platformSources = createSet()
    val commonSources = createSet()
    val sourcesByModuleName = mutableMapOf<String, MutableSet<KtSourceFile>>()

    var pluginsConfigured = false
    fun ensurePluginsConfigured() {
        if (!pluginsConfigured) {
            @OptIn(K1Deprecation::class)
            for (extension in compilerConfiguration.getCompilerExtensions(CompilerConfigurationExtension)) {
                extension.updateFileRegistry(projectEnvironment.project)
            }
            pluginsConfigured = true
        }
    }

    fun findVirtualFile(file: File): VirtualFile? =
        projectEnvironment.knownFileSystems.findFileByPath(file.normalize().path, StandardFileSystems.FILE_PROTOCOL)

    getSourceRootsCheckingForDuplicates(compilerConfiguration)
        .allSourceFilesSequence(
            compilerConfiguration,
            findVirtualFile = ::findVirtualFile,
            filter = { virtualFile, isExplicit ->
                when (virtualFile.extension) {
                    JavaFileType.DEFAULT_EXTENSION -> false
                    KotlinFileType.EXTENSION -> true
                    else -> {
                        if (virtualFile.isFile) {
                            ensurePluginsConfigured()
                            val isKotlin = virtualFile.fileType == KotlinFileType.INSTANCE
                            if (isExplicit && !isKotlin) {
                                compilerConfiguration.report(
                                    CliDiagnostics.ROOTS_RESOLUTION_ERROR,
                                    "Source entry is not a Kotlin file: ${virtualFile.path}"
                                )
                            }
                            isKotlin
                        } else false
                    }
                }
            },
            convertToSourceFiles = {
                val sources = listOf(KtVirtualFileSourceFile(it))
                if (it.extension == KotlinFileType.EXTENSION) sources
                else {
                    // currently applying the extension only to non-kt files, e.g. scripts
                    applyFirProcessSourcesExtension(projectEnvironment, compilerConfiguration, ::findVirtualFile, sources) ?: sources
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

    val sources = GroupedKtSources(platformSources, commonSources, sourcesByModuleName)
    return when (compilerConfiguration.parserMode) {
        ParserMode.Psi -> sources.toPsiSources(projectEnvironment.project)
        ParserMode.LightTree, ParserMode.KmpTree -> sources
    }
}

private fun GroupedKtSources.toPsiSources(project: Project): GroupedKtSources {
    val psiManager = PsiManager.getInstance(project)
    val sourceToKtFile = HashMap<KtSourceFile, KtPsiSourceFile?>()

    fun Collection<KtSourceFile>.mapToPsiSourceFiles(): Set<KtPsiSourceFile> {
        return mapNotNullTo(LinkedHashSet()) l@{ source ->
            sourceToKtFile.getOrPut(source) {
                check(source is KtVirtualFileSourceFile) { "Unexpected source file ${source::class.simpleName}: ${source.path}" }
                val ktFile = psiManager.findFile(source.virtualFile) as? KtFile ?: return@l null
                KtPsiSourceFile(ktFile)
            }
        }
    }

    return GroupedKtSources(
        platformSources = platformSources.mapToPsiSourceFiles(),
        commonSources = commonSources.mapToPsiSourceFiles(),
        sourcesByModuleName = sourcesByModuleName.mapValues { it.value.mapToPsiSourceFiles() },
    )
}

/**
 * Applies [CollectAdditionalSourceFilesExtension] instances to the set of initial sources
 * @param environment the project environment
 * @param configuration compiler configuration
 * @param findVirtualFile a function to find a virtual file by a file
 * @param sources sources to process
 * @return null if no applicable extensions were found or no processing was performed, otherwise returns the updated list of recursively processed sources
 *
 * @see CollectAdditionalSourceFilesExtension.isApplicable
 * @see CollectAdditionalSourceFilesExtension.collectSources
 */
private fun applyFirProcessSourcesExtension(
    environment: VfsBasedProjectEnvironment,
    configuration: CompilerConfiguration,
    findVirtualFile: (File) -> VirtualFile?,
    sources: Iterable<KtSourceFile>,
): Iterable<KtSourceFile>? {
    val extensions = configuration.getCompilerExtensions(CollectAdditionalSourceFilesExtension).filter { it.isApplicable(configuration) }
    return if (extensions.isEmpty()) sources
    else extensions.fold(sources) { res, ext -> ext.collectSources(environment, configuration, findVirtualFile, res) }
}
