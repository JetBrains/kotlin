/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolveState
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KotlinProjectStructureProviderTestImpl
import org.jetbrains.kotlin.analysis.test.framework.project.structure.TestKtSourceModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.getKtFilesFromModule
import org.jetbrains.kotlin.analysis.test.framework.project.structure.projectModuleProvider
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirSealedClassInheritorsProcessor
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.test.services.*

class ModuleRegistrarPreAnalysisHandler(
    testServices: TestServices,
    private val parentDisposable: Disposable,
    private val shouldRegisterProjectServices: Boolean,
) : PreAnalysisHandler(testServices) {
    private val moduleInfoProvider = testServices.projectModuleProvider

    override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
        // todo rework after all modules will have the same Project instance
        val ktFilesByModule = moduleStructure.modules.associateWith { testModule ->
            val project = testServices.compilerConfigurationProvider.getProject(testModule)
            testServices.sourceFileProvider.getKtFilesForSourceFiles(testModule.files, project)
        }

        val allKtFiles = ktFilesByModule.values.flatMap { it.values.toList() }

        ktFilesByModule.forEach { (testModule, ktFiles) ->
            val project = testServices.compilerConfigurationProvider.getProject(testModule)
            moduleInfoProvider.registerModuleInfo(testModule, TestKtSourceModule(project, testModule, ktFiles, testServices))

            // Register services for compiler-based tests
            // On the other hand, frontend API tests will register services via test configurators.
            if (shouldRegisterProjectServices) {
                (project as MockProject).registerTestServices(
                    allKtFiles,
                    testServices.compilerConfigurationProvider.getPackagePartProviderFactory(testModule),
                    KotlinProjectStructureProviderTestImpl(testServices)
                )
            }
        }
    }

    override fun prepareSealedClassInheritors(moduleStructure: TestModuleStructure) {
        val ktFilesByModule = moduleStructure.modules.associateWith { testModule ->
            getKtFilesFromModule(testServices, testModule)
        }

        ktFilesByModule.forEach { (testModule, ktFiles) ->
            val project = testServices.compilerConfigurationProvider.getProject(testModule)
            // Manually process all inheritors of sealed classes so that SealedClassInheritorsProviderTestImpl can work correctly for tests.
            // In the actual IDE, SealedClassInheritorsProviderIdeImpl works by finding inheritors from the index instead of do a
            // preprocessing of all files. Therefore, the IDE does not rely on such a pre-analysis pass of all files in the module.
            val projectStructureProvider = project.getService(ProjectStructureProvider::class.java)
            val allFirFiles = mutableListOf<FirFile>()
            ktFiles.forEach { ktFile ->
                val ktModule = projectStructureProvider.getKtModuleForKtElement(ktFile)
                val resolveState = ktModule.getResolveState(project)
                allFirFiles.add(ktFile.getOrBuildFirFile(resolveState))
            }
            allFirFiles.groupBy { it.moduleData.session }.forEach { (session, firFiles) ->
                for (firFile in firFiles) {
                    firFile.ensureResolved(FirResolvePhase.TYPES)
                }
                val sealedProcessor = FirSealedClassInheritorsProcessor(session, ScopeSession())
                sealedProcessor.process(firFiles)
            }
        }
    }
}
