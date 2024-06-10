/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

/**
 * @see TestModuleKind.Source
 */
object KtSourceTestModuleFactory : KtTestModuleFactory {
    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val psiFiles = TestModuleStructureFactory.createSourcePsiFiles(testModule, testServices, project)

        val module = KaSourceModuleImpl(
            testModule.name,
            testModule.targetPlatform,
            testModule.languageVersionSettings,
            project,
            GlobalSearchScope.filesScope(project, psiFiles.mapTo(mutableSetOf()) { it.virtualFile }),
        )

        return KtTestModule(TestModuleKind.Source, testModule, module, psiFiles)
    }
}
