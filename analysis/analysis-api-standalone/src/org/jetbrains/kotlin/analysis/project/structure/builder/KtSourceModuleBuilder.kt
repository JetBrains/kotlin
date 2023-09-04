/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KtSourceModuleImpl
import org.jetbrains.kotlin.analysis.project.structure.impl.collectSourceFilePaths
import org.jetbrains.kotlin.analysis.project.structure.impl.hasSuitableExtensionToAnalyse
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
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
    private val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment,
) : KtModuleBuilder() {
    public lateinit var moduleName: String
    public var languageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)

    private val sourceRoots: MutableList<Path> = mutableListOf()

    public fun addSourceRoot(path: Path) {
        sourceRoots.add(path)
    }

    public fun addSourceRoots(paths: Collection<Path>) {
        sourceRoots.addAll(paths)
    }

    override fun build(): KtSourceModule {
        val virtualFiles = collectVirtualFilesByRoots()
        val psiManager = PsiManager.getInstance(kotlinCoreProjectEnvironment.project)
        val psiFiles = virtualFiles.mapNotNull { psiManager.findFile(it) }
        val contentScope = GlobalSearchScope.filesScope(kotlinCoreProjectEnvironment.project, virtualFiles)
        return KtSourceModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            kotlinCoreProjectEnvironment.project,
            moduleName,
            languageVersionSettings,
            psiFiles,
        )
    }

    private fun collectVirtualFilesByRoots(): List<VirtualFile> {
        val localFileSystem = kotlinCoreProjectEnvironment.environment.localFileSystem
        return buildList {
            for (root in sourceRoots) {
                val files = when {
                    root.isDirectory() -> collectSourceFilePaths(root)
                    root.hasSuitableExtensionToAnalyse() -> listOf(root)
                    else -> emptyList()
                }
                for (file in files) {
                    val virtualFile = localFileSystem.findFileByIoFile(file.toFile()) ?: continue
                    add(virtualFile)
                }
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun KtModuleProviderBuilder.buildKtSourceModule(init: KtSourceModuleBuilder.() -> Unit): KtSourceModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtSourceModuleBuilder(kotlinCoreProjectEnvironment).apply(init).build()
}