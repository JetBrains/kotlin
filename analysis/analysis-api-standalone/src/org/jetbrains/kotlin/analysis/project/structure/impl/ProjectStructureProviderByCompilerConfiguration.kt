/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.cli.jvm.config.javaSourceRoots
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Files
import java.nio.file.Paths

internal class ProjectStructureProviderByCompilerConfiguration(
    private val compilerConfig: CompilerConfiguration,
    project: Project,
    ktFiles: List<KtFile>,
) : ProjectStructureProvider() {
    private val sourceFiles: Set<String> by lazy {
        buildSet {
            compilerConfig.javaSourceRoots.forEach { srcRoot ->
                val path = Paths.get(srcRoot)
                if (Files.isDirectory(path)) {
                    // E.g., project/app/src
                    Files.walk(Paths.get(srcRoot))
                        .filter(Files::isRegularFile)
                        .forEach { add(it.toString()) }
                } else {
                    // E.g., project/app/src/some/pkg/main.kt
                    add(srcRoot)
                }
            }
        }
    }

    private val sourceModule = KtSourceModuleByCompilerConfiguration(compilerConfig, project, ktFiles)

    override fun getKtModuleForKtElement(element: PsiElement): KtModule {
        val containingFilePath = element.containingFile.virtualFile.path
        return if (containingFilePath in sourceFiles) {
            sourceModule
        } else {
            sourceModule.directRegularDependencies.find { libModule ->
                (libModule as KtLibraryModuleByCompilerConfiguration).virtualFiles.any { it.path == containingFilePath }
            } ?: error("Can't find module for $containingFilePath")
        }
    }
}
