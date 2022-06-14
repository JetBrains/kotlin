/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.google.common.io.Files.getNameWithoutExtension
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildProjectStructureProvider
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.javaSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import java.nio.file.Files
import java.nio.file.Paths

internal fun TargetPlatform.getAnalyzerServices(): PlatformDependentAnalyzerServices {
    return when {
        isJvm() -> JvmPlatformAnalyzerServices
        isJs() -> JsPlatformAnalyzerServices
        isNative() -> NativePlatformAnalyzerServices
        isCommon() -> CommonPlatformAnalyzerServices
        else -> error("Unknown target platform: $this")
    }
}

internal fun getSourceFilePaths(
    compilerConfig: CompilerConfiguration,
): Set<String> {
    return buildSet {
        compilerConfig.javaSourceRoots.forEach { srcRoot ->
            val path = Paths.get(srcRoot)
            if (Files.isDirectory(path)) {
                // E.g., project/app/src
                Files.walk(path)
                    .filter(java.nio.file.Files::isRegularFile)
                    .forEach { add(it.toString()) }
            } else {
                // E.g., project/app/src/some/pkg/main.kt
                add(srcRoot)
            }
        }
    }
}

internal inline fun <reified T : PsiFile> getPsiFilesFromPaths(
    project: Project,
    paths: Collection<String>,
): List<T> {
    val fs = StandardFileSystems.local()
    val psiManager = PsiManager.getInstance(project)
    return buildList {
        for (path in paths) {
            val vFile = fs.findFileByPath(path) ?: continue
            val psiFile = psiManager.findFile(vFile) as? T ?: continue
            add(psiFile)
        }
    }
}

internal fun buildKtModuleProviderByCompilerConfiguration(
    compilerConfig: CompilerConfiguration,
    project: Project,
    ktFiles: List<KtFile>,
): ProjectStructureProvider = buildProjectStructureProvider {
    addModule(
        buildKtSourceModule {
            val platform = TargetPlatform(setOf(JdkPlatform(JvmTarget.DEFAULT)))
            val moduleName = compilerConfig.get(CommonConfigurationKeys.MODULE_NAME) ?: "<no module name provided>"

            val libraryRoots = compilerConfig.jvmModularRoots + compilerConfig.jvmClasspathRoots
            val (directories, jars) = libraryRoots.partition { it.isDirectory }
            directories.forEach {
                // E.g., project/app/build/intermediates/javac/debug/classes
                val root = it.toPath()
                addRegularDependency(
                    buildKtLibraryModule {
                        contentScope = ProjectScope.getLibrariesScope(project)
                        this.platform = platform
                        this.project = project
                        binaryRoots = listOf(root)
                        libraryName = "$moduleName-${root.toString().replace("/", "-")}"
                    }
                )
            }
            jars.forEach {
                // E.g., project/libs/libA/a.jar
                val root = it.toPath()
                addRegularDependency(
                    buildKtLibraryModule {
                        contentScope = ProjectScope.getLibrariesScope(project)
                        this.platform = platform
                        this.project = project
                        binaryRoots = listOf(root)
                        libraryName = getNameWithoutExtension(root.toString())
                        isBuiltinsContainingStdlib =
                            libraryName.startsWith("kotlin-stdlib") &&
                                    !libraryName.contains("common") && !libraryName.contains("jdk")
                    }
                )
            }
            compilerConfig.get(JVMConfigurationKeys.JDK_HOME)?.let { jdkHome ->
                val vfm = VirtualFileManager.getInstance()
                val jdkHomePath = jdkHome.toPath()
                val jdkHomeVirtualFile = vfm.findFileByNioPath(jdkHomePath)
                val binaryRoots = LibraryUtils.findClassesFromJdkHome(jdkHomePath).map {
                    Paths.get(URLUtil.extractPath(it))
                }
                addRegularDependency(
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
            addSourceRoots(getPsiFilesFromPaths(project, getSourceFilePaths(compilerConfig)))
        }
    )
}
