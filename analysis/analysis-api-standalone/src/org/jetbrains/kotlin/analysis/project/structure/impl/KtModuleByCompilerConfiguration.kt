/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
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
    override val directRegularDependencies: List<KtModule> by lazy {
        (compilerConfig.jvmModularRoots + compilerConfig.jvmClasspathRoots).map {
            KtLibraryModuleByCompilerConfiguration(compilerConfig, project, it.toPath())
        }
    }

    override val contentScope: GlobalSearchScope =
        TopDownAnalyzerFacadeForJVM.newModuleSearchScope(project, ktFiles)
}

internal class KtLibraryModuleByCompilerConfiguration(
    compilerConfig: CompilerConfiguration,
    project: Project,
    private val jar: Path,
) : BaseKtModuleByCompilerConfiguration(compilerConfig, project), KtLibraryModule {
    override val directRegularDependencies: List<KtModule> get() = emptyList()

    internal val virtualFiles: Collection<VirtualFile> by lazy {
        LibraryUtils.getAllVirtualFilesFromJar(jar)
    }

    override val libraryName: String
        get() = moduleName

    override val librarySources: KtLibrarySourceModule?
        get() = null

    override val contentScope: GlobalSearchScope =
        GlobalSearchScope.filesScope(project, virtualFiles)

    private val binaryRoots by lazy {
        listOf(jar)
    }

    override fun getBinaryRoots(): Collection<Path> = binaryRoots
}
