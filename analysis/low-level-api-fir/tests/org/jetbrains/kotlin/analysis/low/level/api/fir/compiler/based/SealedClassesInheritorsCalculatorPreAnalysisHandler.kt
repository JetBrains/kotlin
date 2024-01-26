/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLSealedInheritorsProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.LLSealedInheritorsProviderFactoryForTests
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.project.structure.getKtFiles
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.FirSealedClassInheritorsProcessor
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.PreAnalysisHandler
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class SealedClassesInheritorsCalculatorPreAnalysisHandler(
    testServices: TestServices,
) : PreAnalysisHandler(testServices) {

    override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
    }

    // Manually process all inheritors of sealed classes so that SealedClassInheritorsProviderTestImpl can work correctly for tests.
    // In the actual IDE, SealedClassInheritorsProviderIdeImpl works by finding inheritors from the index instead of do a
    // preprocessing of all files. Therefore, the IDE does not rely on such a pre-analysis pass of all files in the module.
    override fun prepareSealedClassInheritors(moduleStructure: TestModuleStructure) {
        if (Directives.DISABLE_SEALED_INHERITOR_CALCULATOR in moduleStructure.allDirectives) {
            return
        }

        val ktFilesByModule = moduleStructure.modules.associateWith { testModule ->
            testServices.ktModuleProvider.getKtFiles(testModule)
        }

        for ((testModule, ktFiles) in ktFilesByModule) {
            if (ktFiles.isEmpty()) continue
            val project = testServices.compilerConfigurationProvider.getProject(testModule)
            val projectStructureProvider = project.getService(ProjectStructureProvider::class.java)
            val ktModule = ktFiles.map { projectStructureProvider.getModule(it, contextualModule = null) }.distinct().single()

            val tmpFirResolveSession = LLFirResolveSessionService.getInstance(project).getFirResolveSessionNoCaching(ktModule)
            val firFiles = ktFiles.map { it.getOrBuildFirFile(tmpFirResolveSession) }
            val sealedInheritors = collectSealedClassInheritors(firFiles, tmpFirResolveSession)
            val provider = project.getService(LLSealedInheritorsProviderFactory::class.java) as LLSealedInheritorsProviderFactoryForTests
            provider.registerInheritors(ktModule, sealedInheritors)
        }
    }

    private fun collectSealedClassInheritors(
        firFiles: List<FirFile>,
        tmpFirResolveSession: LLFirResolveSession,
    ): Map<ClassId, List<ClassId>> {
        firFiles.forEach { it.lazyResolveToPhaseRecursively(FirResolvePhase.TYPES) }
        val inheritorsCollector = FirSealedClassInheritorsProcessor.InheritorsCollector(tmpFirResolveSession.useSiteFirSession)
        val sealedClassInheritorsMap = mutableMapOf<FirRegularClass, MutableList<ClassId>>()
        firFiles.forEach { it.accept(inheritorsCollector, sealedClassInheritorsMap) }
        return sealedClassInheritorsMap.mapKeys { (firClass, _) -> firClass.symbol.classId }
    }

    object Directives : SimpleDirectivesContainer() {
        val DISABLE_SEALED_INHERITOR_CALCULATOR by directive(
            description = "Disable mock sealed class inheritor calculation",
            applicability = DirectiveApplicability.Global
        )
    }
}
