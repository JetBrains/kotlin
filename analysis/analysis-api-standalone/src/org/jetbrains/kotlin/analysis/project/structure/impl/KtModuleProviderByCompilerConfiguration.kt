/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.google.common.io.Files
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.project.structure.KtBinaryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Paths

internal class KtModuleProviderByCompilerConfiguration(
    compilerConfig: CompilerConfiguration,
    project: Project,
    ktFiles: List<KtFile>,
) : ProjectStructureProvider() {
    private val sourceModule = buildKtSourceModule {
        val platform = TargetPlatform(setOf(JdkPlatform(JvmTarget.DEFAULT)))
        val moduleName = compilerConfig.get(CommonConfigurationKeys.MODULE_NAME) ?: "<no module name provided>"

        val libraryRoots = compilerConfig.jvmModularRoots + compilerConfig.jvmClasspathRoots
        val libraryRootsByType = libraryRoots.groupBy { it.isDirectory }
        libraryRootsByType[true]?.let { directories ->
            directories.forEach {
                // E.g., project/app/build/intermediates/javac/debug/classes
                val root = it.toPath()
                directRegularDependencies.add(
                    buildKtLibraryModule {
                        contentScope = ProjectScope.getLibrariesScope(project)
                        this.platform = platform
                        this.project = project
                        binaryRoots = listOf(root)
                        libraryName = "$moduleName-${root.toString().replace("/", "-")}"
                    }
                )
            }
        }
        libraryRootsByType[false]?.let { jars ->
            jars.forEach {
                // E.g., project/libs/libA/a.jar
                val root = it.toPath()
                directRegularDependencies.add(
                    buildKtLibraryModule {
                        contentScope = ProjectScope.getLibrariesScope(project)
                        this.platform = platform
                        this.project = project
                        binaryRoots = listOf(root)
                        libraryName = Files.getNameWithoutExtension(root.toString())
                        isBuiltinsContainingStdlib =
                            libraryName.startsWith("kotlin-stdlib") &&
                                    !libraryName.contains("common") && !libraryName.contains("jdk")
                    }
                )
            }
        }
        compilerConfig.get(JVMConfigurationKeys.JDK_HOME)?.let { jdkHome ->
            val vfm = VirtualFileManager.getInstance()
            val jdkHomePath = jdkHome.toPath()
            val jdkHomeVirtualFile = vfm.findFileByNioPath(jdkHomePath)
            val binaryRoots = LibraryUtils.findClassesFromJdkHome(jdkHomePath).map {
                Paths.get(URLUtil.extractPath(it))
            }
            directRegularDependencies.add(
                buildKtSdkModule {
                    contentScope = GlobalSearchScope.fileScope(project, jdkHomeVirtualFile)
                    this.platform = platform
                    this.project = project
                    this.binaryRoots = binaryRoots
                    sdkName = "JDK for $moduleName"
                }
            )
        }

        contentScope = TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)
        this.platform = platform
        this.project = project
        this.moduleName = moduleName
    }

    private val binaryModules: Collection<KtBinaryModule> by lazy {
        sourceModule.directRegularDependencies.filterIsInstance<KtBinaryModule>()
    }

    override fun getKtModuleForKtElement(element: PsiElement): KtModule {
        val containingFile = element.containingFile.virtualFile
        return if (containingFile in sourceModule.contentScope) {
            sourceModule
        } else {
            binaryModules.find { libModule -> containingFile in libModule.contentScope }
                ?: error("Can't find module for ${containingFile.path}")
        }
    }

    override fun getKtBinaryModules(): Collection<KtBinaryModule> {
        return binaryModules
    }

    override fun getStdlibWithBuiltinsModule(module: KtModule): KtLibraryModule? {
        return binaryModules
            .filterIsInstance<KtLibraryModuleImpl>()
            .firstOrNull { it.isBuiltinsContainingStdlib }
    }
}
