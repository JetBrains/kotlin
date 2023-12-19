/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import java.nio.file.Path

/**
 * @see org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind.LibrarySource
 */
object KtLibrarySourceModuleFactory : KtModuleFactory {
    override fun createModule(testModule: TestModule, testServices: TestServices, project: Project): KtModuleWithFiles {
        val (libraryJar, librarySourcesJar) = testServices.compiledLibraryProvider.compileToLibrary(testModule)

        require(librarySourcesJar != null)

        return createKtLibrarySourceModule(
            libraryJar = libraryJar,
            librarySourcesJar = librarySourcesJar,
            testModule = testModule,
            project = project,
        )
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
