/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.base

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependenciesOfType
import org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.api.standalone.fir.projectStructure.FirStandaloneServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.LLDiagnosticsDirectives
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.*
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.jetbrains.kotlin.analysis.test.framework.services.configuration.AnalysisApiIndexingConfiguration
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

object FirLowLevelCompilerBasedTestConfigurator : AnalysisApiTestConfigurator {
    override val analyseInDependentSession: Boolean get() = false
    override val analysisApiMode: AnalysisApiMode get() = AnalysisApiMode.Ide

    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        builder.apply {
            useAdditionalService { AnalysisApiIndexingConfiguration(AnalysisApiBinaryLibraryIndexingMode.INDEX_STUBS) }
            configureFirConsistencyChecks()
        }
    }

    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> = listOf(
        AnalysisApiBaseTestServiceRegistrar,
        AnalysisApiIdeModeTestServiceRegistrar,
        FirStandaloneServiceRegistrar,
        AnalysisApiFirTestServiceRegistrar,
        JavaModuleResolverTestServiceRegistrar,
    )

    override fun createModules(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        project: Project,
    ): KtTestModuleStructure {
        val mainModules = moduleStructure.modules.map { testModule ->
            val files = TestModuleStructureFactory.createSourcePsiFiles(testModule, testServices, project)
            val scriptFile = files.singleOrNull() as? KtFile

            val [ktModule, testModuleKind] = if (scriptFile?.isScript() == true) {
                Pair(
                    KaScriptModuleByCompilerConfiguration(project, testModule, scriptFile, testServices),
                    TestModuleKind.ScriptSource,
                )
            } else {
                val scopeFileSystemItems = files + files.mapNotNull { it.parent }
                val baseSearchScope = GlobalSearchScope.filesScope(project, scopeFileSystemItems.map { it.virtualFile })
                Pair(
                    KaSourceModuleByCompilerConfiguration(project, testModule, files, testServices, baseSearchScope),
                    TestModuleKind.Source,
                )
            }

            KtTestModule(testModuleKind, testModule, ktModule, files)
        }

        return KtTestModuleStructure(
            testModuleStructure = moduleStructure,
            mainModules = mainModules,
            binaryModules = mainModules.asSequence().flatMap { it.ktModule.allDirectDependenciesOfType<KaLibraryModule>() }.asIterable(),
        )
    }

    override fun doGlobalModuleStateModification(project: Project) {
        error("Should not be called for compiler based tests")
    }
}

private object JavaModuleResolverTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        if (LLDiagnosticsDirectives.MOCK_MODULE_DOES_NOT_READ_SOURCE !in testServices.moduleStructure.allDirectives) return

        project.registerService(JavaModuleResolver::class.java, JavaModuleResolverForTest::class.java)
    }
}

private class JavaModuleResolverForTest : JavaModuleResolver {
    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? = null

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?,
        referencedFile: VirtualFile,
        referencedPackage: FqName?
    ): JavaModuleResolver.AccessError? {
        if (fileFromOurModule == null || referencedPackage != FqName("a")) return null
        return JavaModuleResolver.AccessError.ModuleDoesNotReadModule("moduleA")
    }
}
