/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.KtModuleWithFiles
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtNotUnderContentRootModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.test.getAnalyzerServices
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class KtOutOfContentRootModuleFactory : KtModuleFactory {
    override fun createModule(testModule: TestModule, testServices: TestServices, project: Project): KtModuleWithFiles {
        val psiFiles = TestModuleStructureFactory.createSourcePsiFiles(testModule, testServices, project)
        val module = KtNotUnderContentRootModuleForTest(testModule.name, psiFiles.single(), testModule.targetPlatform)
        return KtModuleWithFiles(module, psiFiles)
    }
}

internal class KtNotUnderContentRootModuleForTest(
    val moduleName: String,
    override val file: PsiFile,
    override val platform: TargetPlatform
) : KtNotUnderContentRootModule {
    override val directRegularDependencies: List<KtModule>
        get() = emptyList()

    override val directRefinementDependencies: List<KtModule>
        get() = emptyList()

    override val directFriendDependencies: List<KtModule>
        get() = emptyList()

    override val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.fileScope(file)

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = platform.getAnalyzerServices()

    override val project: Project
        get() = file.project

    override val moduleDescription: String
        get() = "Not under content root for ${file.virtualFile.path}"
}