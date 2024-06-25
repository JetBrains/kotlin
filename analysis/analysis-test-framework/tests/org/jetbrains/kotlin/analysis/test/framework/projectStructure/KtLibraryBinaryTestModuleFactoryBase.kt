/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

abstract class KtLibraryBinaryTestModuleFactoryBase : KtTestModuleFactory {
    protected abstract val testModuleKind: TestModuleKind

    protected abstract fun decompileToPsiFiles(binaryRoot: Path, testServices: TestServices, project: Project): List<PsiFile>

    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val binaryRoot = testServices.compiledLibraryProvider.compileToLibrary(testModule, dependencyBinaryRoots).artifact
        val decompiledFiles = decompileToPsiFiles(binaryRoot, testServices, project)

        return KtTestModule(
            testModuleKind,
            testModule,
            KaLibraryModuleImpl(
                testModule.name,
                testModule.targetPlatform,
                StandaloneProjectFactory.createSearchScopeByLibraryRoots(
                    listOf(binaryRoot),
                    emptyList(),
                    testServices.environmentManager.getProjectEnvironment(),
                ),
                project,
                binaryRoots = listOf(binaryRoot),
                librarySources = null,
                isSdk = false,
            ),
            decompiledFiles,
        )
    }
}
