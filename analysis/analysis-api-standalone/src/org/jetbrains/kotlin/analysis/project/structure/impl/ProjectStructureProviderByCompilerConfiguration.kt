/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtFile

internal class ProjectStructureProviderByCompilerConfiguration(
    compilerConfig: CompilerConfiguration,
    project: Project,
    ktFiles: List<KtFile>,
    jarFileSystem: CoreJarFileSystem,
) : ProjectStructureProvider() {
    private val sourceModule = KtSourceModuleByCompilerConfiguration(compilerConfig, project, ktFiles, jarFileSystem)

    internal val libraryModules: Collection<KtLibraryModule> by lazy {
        sourceModule.directRegularDependencies
    }

    override fun getKtModuleForKtElement(element: PsiElement): KtModule {
        val containingFile = element.containingFile.virtualFile
        return if (containingFile in sourceModule.contentScope) {
            sourceModule
        } else {
            sourceModule.directRegularDependencies.find { libModule -> containingFile in libModule.contentScope }
                ?: error("Can't find module for ${containingFile.path}")
        }
    }
}
