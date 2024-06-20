/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.platform.isMultiPlatform
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.junit.Assume
import java.nio.file.Path

/**
 * @see TestModuleKind.LibrarySource
 */
object KtLibrarySourceTestModuleFactory : KtTestModuleFactory {
    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        Assume.assumeFalse("Compilation of multi-platform libraries is not supported", testModule.targetPlatform.isMultiPlatform())

        val (libraryJar, librarySourcesJar) = testServices.compiledLibraryProvider.compileToLibrary(testModule, dependencyBinaryRoots)

        require(librarySourcesJar != null)

        return createKtLibrarySourceModule(
            libraryJar = libraryJar,
            librarySourcesJar = librarySourcesJar,
            testModule = testModule,
            project = project,
            testServices = testServices,
        )
    }
}

fun createKtLibrarySourceModule(
    libraryJar: Path,
    librarySourcesJar: Path,
    testModule: TestModule,
    project: Project,
    testServices: TestServices,
): KtTestModule {
    val libraryKtModule = KaLibraryModuleImpl(
        testModule.name,
        testModule.targetPlatform,
        StandaloneProjectFactory.createSearchScopeByLibraryRoots(
            listOf(libraryJar),
            testServices.environmentManager.getProjectEnvironment(),
        ),
        project,
        binaryRoots = listOf(libraryJar),
        librarySources = null,
        isSdk = false,
    )

    val decompiledPsiFilesFromSourceJar = LibraryUtils.getAllPsiFilesFromJar(librarySourcesJar, project)
    val librarySourceKtModule = KaLibrarySourceModuleImpl(
        testModule.name,
        testModule.targetPlatform,
        GlobalSearchScope.filesScope(project, decompiledPsiFilesFromSourceJar.map { it.virtualFile }),
        project,
        binaryLibrary = libraryKtModule,
    )

    libraryKtModule.librarySources = librarySourceKtModule
    return KtTestModule(TestModuleKind.LibrarySource, testModule, librarySourceKtModule, decompiledPsiFilesFromSourceJar)
}
