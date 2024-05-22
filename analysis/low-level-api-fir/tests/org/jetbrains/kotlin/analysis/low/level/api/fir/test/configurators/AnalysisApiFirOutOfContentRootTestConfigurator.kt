/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirBuiltinsSessionFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestModuleStructureFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.analysis.test.framework.utils.SkipTestException
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

object AnalysisApiFirOutOfContentRootTestConfigurator : AnalysisApiFirSourceLikeTestConfigurator(false) {
    override val testPrefix: String
        get() = "out_of_src_roots"

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)

        builder.apply {
            useDirectives(Directives)
            useAdditionalService<KtTestModuleFactory> { KtOutOfContentRootTestModuleFactory }
        }
    }

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project
    ): KtTestModuleStructure {
        if (Directives.SKIP_WHEN_OUT_OF_CONTENT_ROOT in moduleStructure.allDirectives) {
            throw SkipWhenOutOfContentRootException()
        }

        return super.createModules(moduleStructure, testServices, project)
    }

    object Directives : SimpleDirectivesContainer() {
        val SKIP_WHEN_OUT_OF_CONTENT_ROOT by directive(
            description = "Skip the test in out-of-content-root mode",
            applicability = DirectiveApplicability.Global
        )
    }
}

private class SkipWhenOutOfContentRootException : SkipTestException()

private object KtOutOfContentRootTestModuleFactory : KtTestModuleFactory {
    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val psiFiles = TestModuleStructureFactory.createSourcePsiFiles(testModule, testServices, project)
        val platform = testModule.targetPlatform
        val ktModule = KtNotUnderContentRootModuleForTest(testModule.name, psiFiles.first(), platform)
        return KtTestModule(TestModuleKind.NotUnderContentRoot, testModule, ktModule, psiFiles)
    }
}

internal class KtNotUnderContentRootModuleForTest(
    override val name: String,
    override val file: PsiFile,
    override val platform: TargetPlatform
) : KtNotUnderContentRootModule {
    override val directRegularDependencies: List<KtModule> by lazy {
        listOf(LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsModule(platform))
    }

    override val directDependsOnDependencies: List<KtModule>
        get() = emptyList()

    override val transitiveDependsOnDependencies: List<KtModule>
        get() = emptyList()

    override val directFriendDependencies: List<KtModule>
        get() = emptyList()

    override val contentScope: GlobalSearchScope
        get() = GlobalSearchScope.fileScope(file)

    override val project: Project
        get() = file.project

    override val moduleDescription: String
        get() = "Not under content root \"${name}\" for ${file.virtualFile.path}"
}
