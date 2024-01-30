/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.testModuleDecompiler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

/**
 * @see org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind.LibraryBinary
 */
object KtLibraryBinaryModuleFactory : KtModuleFactory {
    override fun createModule(
        testModule: TestModule,
        contextModule: KtModuleWithFiles?,
        testServices: TestServices,
        project: Project,
    ): KtModuleWithFiles {
        val library = testServices.compiledLibraryProvider.compileToLibrary(testModule).artifact
        val decompiledFiles = testServices.testModuleDecompiler.getAllPsiFilesFromLibrary(library, project)

        return KtModuleWithFiles(
            KtLibraryModuleImpl(
                testModule.name,
                testModule.targetPlatform,
                StandaloneProjectFactory.createSearchScopeByLibraryRoots(
                    listOf(library),
                    testServices.environmentManager.getProjectEnvironment(),
                ),
                project,
                binaryRoots = listOf(library),
                librarySources = null,
            ),
            decompiledFiles,
        )
    }
}
