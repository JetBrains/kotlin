/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import java.nio.file.Path
import java.nio.file.Paths

internal abstract class BaseKtModuleByCompilerConfiguration(
    private val compilerConfig: CompilerConfiguration,
    val project: Project,
) {
    val analyzerServices: PlatformDependentAnalyzerServices
        get() = JvmPlatformAnalyzerServices

    val directFriendDependencies: List<KtModule>
        get() = emptyList()

    val directRefinementDependencies: List<KtModule>
        get() = emptyList()

    val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)

    val moduleName: String
        get() = compilerConfig.get(CommonConfigurationKeys.MODULE_NAME) ?: "<no module name provided>"

    val platform: TargetPlatform
        get() = TargetPlatform(setOf(JdkPlatform(JvmTarget.DEFAULT)))
}

internal class KtSourceModuleByCompilerConfiguration(
    compilerConfig: CompilerConfiguration,
    project: Project,
    ktFiles: List<KtFile>,
) : BaseKtModuleByCompilerConfiguration(compilerConfig, project), KtSourceModule {
    override val directRegularDependencies: List<KtBinaryModule> by lazy {
        val libraryRoots = compilerConfig.jvmModularRoots + compilerConfig.jvmClasspathRoots
        val libraryRootsByType = libraryRoots.groupBy { it.isDirectory }
        buildList {
            libraryRootsByType[true]?.let { directories ->
                directories.forEach {
                    // E.g., project/app/build/intermediates/javac/debug/classes
                    val root = it.toPath()
                    add(KtLibraryModuleByCompilerConfiguration(compilerConfig, project, root))
                }
            }
            libraryRootsByType[false]?.let { jars ->
                jars.forEach {
                    // E.g., project/libs/libA/a.jar
                    val root = it.toPath()
                    add(KtLibraryModuleByCompilerConfiguration(compilerConfig, project, root))
                }
            }
            compilerConfig.get(JVMConfigurationKeys.JDK_HOME)?.let { jdkHome ->
                val vfm = VirtualFileManager.getInstance()
                val jdkHomePath = jdkHome.toPath()
                val jdkHomeVirtualFile = vfm.findFileByNioPath(jdkHomePath)
                val binaryRoots = LibraryUtils.findClassesFromJdkHome(jdkHomePath).map {
                    Paths.get(URLUtil.extractPath(it))
                }
                add(KtSdkModuleByCompilerConfiguration(compilerConfig, project, "JDK", jdkHomeVirtualFile, binaryRoots))
            }
        }
    }

    override val contentScope: GlobalSearchScope by lazy {
        TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)
    }
}

internal class KtLibraryModuleByCompilerConfiguration(
    compilerConfig: CompilerConfiguration,
    project: Project,
    root: Path,
) : BaseKtModuleByCompilerConfiguration(compilerConfig, project), KtLibraryModule {
    override val directRegularDependencies: List<KtModule> get() = emptyList()

    override val libraryName: String
        get() = moduleName

    override val librarySources: KtLibrarySourceModule?
        get() = null

    override val contentScope: GlobalSearchScope by lazy {
        ProjectScope.getLibrariesScope(project)
    }

    private val binaryRoots = listOf(root)

    override fun getBinaryRoots(): Collection<Path> = binaryRoots
}

internal class KtSdkModuleByCompilerConfiguration(
    compilerConfig: CompilerConfiguration,
    project: Project,
    override val sdkName: String,
    sdkHome: VirtualFile?,
    private val binaryRoots: Collection<Path>
) : BaseKtModuleByCompilerConfiguration(compilerConfig, project), KtSdkModule {
    override val directRegularDependencies: List<KtModule> get() = emptyList()

    override val contentScope: GlobalSearchScope =
        GlobalSearchScope.fileScope(project, sdkHome)

    override fun getBinaryRoots(): Collection<Path> = binaryRoots
}
