/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import java.nio.file.Path
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtLibraryModuleImpl
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtLibrarySourceModuleImpl
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtModuleFactory
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

object AnalysisApiFirStdlibSourceTestConfigurator : AnalysisApiFirSourceLikeTestConfigurator(false) {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)
        builder.apply {
            useAdditionalService<KtModuleFactory> { KtStdlibSourceModuleFactory() }
        }
    }
}

fun createKtLibrarySourceModule(libraryJar: Path, librarySourcesJar: Path, testModule: TestModule, project: Project): KtModuleWithFiles {
    val libraryKtModule = KtLibraryModuleImpl(
        testModule.name,
        testModule.targetPlatform,
        GlobalSearchScope.filesScope(project, LibraryUtils.getAllVirtualFilesFromJar(libraryJar)),
        project,
        binaryRoots = listOf(libraryJar),
        librarySources = null,
    )

    val decompiledPsiFilesFromSourceJar = LibraryUtils.getAllPsiFilesFromJar(librarySourcesJar, project)
    val librarySourceKtModule = KtLibrarySourceModuleImpl(
        testModule.name,
        testModule.targetPlatform,
        GlobalSearchScope.filesScope(project, decompiledPsiFilesFromSourceJar.map { it.virtualFile }),
        project,
        binaryLibrary = libraryKtModule,
    )

    libraryKtModule.librarySources = librarySourceKtModule
    return KtModuleWithFiles(librarySourceKtModule, decompiledPsiFilesFromSourceJar)
}

private class KtStdlibSourceModuleFactory : KtModuleFactory {
    override fun createModule(testModule: TestModule, testServices: TestServices, project: Project): KtModuleWithFiles {
        val libraryJar = ForTestCompileRuntime.runtimeJarForTests().toPath()
        val librarySourcesJar = ForTestCompileRuntime.runtimeSourcesJarForTests().toPath()
        return createKtLibrarySourceModule(
            libraryJar = libraryJar,
            librarySourcesJar = librarySourcesJar,
            testModule = testModule,
            project = project,
        )
    }

}