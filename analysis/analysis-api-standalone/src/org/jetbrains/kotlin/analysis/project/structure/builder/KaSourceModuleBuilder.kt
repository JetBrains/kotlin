/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KaSourceModuleImpl
import org.jetbrains.kotlin.analysis.project.structure.impl.collectSourceFilePaths
import org.jetbrains.kotlin.analysis.project.structure.impl.hasSuitableExtensionToAnalyse
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.io.path.isDirectory

@KtModuleBuilderDsl
public class KtSourceModuleBuilder(
    private val coreApplicationEnvironment: CoreApplicationEnvironment,
    private val project: Project,
) : KtModuleBuilder() {
    public lateinit var moduleName: String
    public var languageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)

    private val sourceRoots: MutableList<Path> = mutableListOf()
    private val sourceVirtualFiles: MutableList<VirtualFile> = mutableListOf()

    public var contentScope: GlobalSearchScope? = null

    public fun addSourceRoot(path: Path) {
        sourceRoots.add(path)
    }

    public fun addSourceRoots(paths: Collection<Path>) {
        sourceRoots.addAll(paths)
    }

    public fun addSourceVirtualFile(virtualFile: VirtualFile) {
        sourceVirtualFiles.add(virtualFile)
    }

    public fun addSourceVirtualFiles(virtualFiles: Collection<VirtualFile>) {
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

@OptIn(ExperimentalContracts::class)
public inline fun KtModuleProviderBuilder.buildKtSourceModule(init: KtSourceModuleBuilder.() -> Unit): KaSourceModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtSourceModuleBuilder(coreApplicationEnvironment, project).apply(init).build()
}