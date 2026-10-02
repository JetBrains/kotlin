/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.builder

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KaSourceModuleImpl
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.collectSourceFilePaths
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.hasSuitableExtensionToAnalyse
import org.jetbrains.kotlin.analysis.project.structure.builder.KtSourceModuleBuilder
import java.nio.file.Path
import kotlin.io.path.isDirectory

internal class KtSourceModuleBuilderImpl(
    private val coreApplicationEnvironment: CoreApplicationEnvironment,
    private val project: Project,
) : KtSourceModuleBuilder() {
    private val sourceRoots: MutableList<Path> = mutableListOf()
    private val sourceVirtualFiles: MutableList<VirtualFile> = mutableListOf()
    override fun addSourceRoot(path: Path) {
        sourceRoots.add(path)
    }

    override fun addSourceRoots(paths: Collection<Path>) {
        sourceRoots.addAll(paths)
    }

    override fun addSourceVirtualFile(virtualFile: VirtualFile) {
        sourceVirtualFiles.add(virtualFile)
    }

    override fun addSourceVirtualFiles(virtualFiles: Collection<VirtualFile>) {
        sourceVirtualFiles.addAll(virtualFiles)
    }

    override fun build(): KaSourceModule {
        val virtualFiles = collectVirtualFilesByRoots()
        val psiManager = PsiManager.getInstance(project)
        val psiFiles = virtualFiles.mapNotNull { psiManager.findFile(it) }
        val contentScope = contentScope ?: GlobalSearchScope.filesScope(project, virtualFiles)
        return KaSourceModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            project,
            moduleName,
            languageVersionSettings,
            psiFiles,
        )
    }

    private fun collectVirtualFilesByRoots(): List<VirtualFile> {
        val localFileSystem = coreApplicationEnvironment.localFileSystem
        return buildList {
            for (root in sourceRoots) {
                val files = when {
                    root.isDirectory() -> collectSourceFilePaths(root)
                    root.hasSuitableExtensionToAnalyse() -> listOf(root)
                    else -> emptyList()
                }
                for (file in files) {
                    val virtualFile = localFileSystem.findFileByNioFile(file.toAbsolutePath()) ?: continue
                    add(virtualFile)
                }
            }
            addAll(sourceVirtualFiles)
            sortBy { it.path }
        }
    }
}
