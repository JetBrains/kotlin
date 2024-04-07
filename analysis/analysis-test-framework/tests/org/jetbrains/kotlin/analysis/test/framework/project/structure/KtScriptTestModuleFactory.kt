/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

/**
 * @see TestModuleKind.ScriptSource
 */
object KtScriptTestModuleFactory : KtTestModuleFactory {
    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val ktFile = TestModuleStructureFactory.createSourcePsiFiles(testModule, testServices, project).single() as KtFile
        val module = KtScriptModuleImpl(
            ktFile,
            testModule.targetPlatform,
            testModule.languageVersionSettings,
            project,
        )

        return KtTestModule(TestModuleKind.ScriptSource, testModule, module, listOf(ktFile))
    }
}
