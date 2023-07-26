/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class KtScriptModuleFactory : KtModuleFactory {
    override fun createModule(testModule: TestModule, testServices: TestServices, project: Project): KtModuleWithFiles {
        val ktFile = TestModuleStructureFactory.createSourcePsiFiles(testModule, testServices, project).single() as KtFile
        val module = KtScriptModuleImpl(
            ktFile,
            testModule.targetPlatform,
            testModule.languageVersionSettings,
            project,
        )

        return KtModuleWithFiles(module, listOf(ktFile))
    }
}
