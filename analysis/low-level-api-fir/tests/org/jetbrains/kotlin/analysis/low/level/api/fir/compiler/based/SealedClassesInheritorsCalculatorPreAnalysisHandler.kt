/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLSealedInheritorsProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.services.LLSealedInheritorsProviderFactoryForTests
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.impl.KotlinStaticDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.test.framework.project.structure.mainModules
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirSealedClassInheritorsProcessor
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
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
    // In the actual IDE, SealedClassInheritorsProviderIdeImpl works by finding inheritors from the index instead of doing a
    // preprocessing of all files. Therefore, the IDE does not rely on such a pre-analysis pass of all files in the module.
    override fun prepareSealedClassInheritors(moduleStructure: TestModuleStructure) {
        if (Directives.DISABLE_SEALED_INHERITOR_CALCULATOR in moduleStructure.allDirectives) {
            return
        }

        val project = testServices.compilerConfigurationProvider.getProject(moduleStructure.modules.first())

        val sealedInheritorsProviderFactory =
            project.getService(LLSealedInheritorsProviderFactory::class.java) as? LLSealedInheritorsProviderFactoryForTests
                ?: return

        val declarationProviderFactory = KotlinDeclarationProviderFactory.getInstance(project) as KotlinStaticDeclarationProviderFactory
        val projectStructureProvider = project.getService(ProjectStructureProvider::class.java)

        val allKtClasses = declarationProviderFactory.getAllKtClasses()
        val ktClassesByKtModule = allKtClasses.groupBy { projectStructureProvider.getModule(it, contextualModule = null) }

        for (ktTestModule in testServices.ktModuleProvider.mainModules) {
            val ktClasses = ktClassesByKtModule[ktTestModule.ktModule] ?: continue
            val sealedInheritors = collectSealedInheritorClassIds(ktTestModule.ktModule, ktClasses, project)

            sealedInheritorsProviderFactory.registerInheritors(ktTestModule.ktModule, sealedInheritors)
        }

        // We request and cache sessions while collecting sealed inheritors. To not interfere with the test, we should invalidate all
        // sessions. Note that, while we could get uncached sessions from `LLFirSessionCache`, dependency sessions will still be cached, so
        // that is not really an alternative to invalidating sessions.
        LLFirSessionCache.getInstance(project).removeAllSessions(includeLibraryModules = true)
    }

    private fun collectSealedInheritorClassIds(
        ktModule: KtModule,
        ktClasses: List<KtClassOrObject>,
        project: Project,
    ): Map<ClassId, List<ClassId>> {
        val sealedInheritorsByFirClass = mutableMapOf<FirRegularClass, MutableList<ClassId>>()

        ktClasses.forEach { ktClass ->
            val classId = ktClass.getClassId() ?: return@forEach

            // Using a resolve session/source-preferred session will cause class stubs from binary libraries to be decompiled (which
            // results in an exception since we don't have a decompiler for them).
            val firSession = LLFirSessionCache.getInstance(project).getSession(ktModule, preferBinary = true)
            val firClassSymbol = firSession.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return@forEach
            firClassSymbol.lazyResolveToPhaseRecursively(FirResolvePhase.TYPES)

            val inheritorsCollector = FirSealedClassInheritorsProcessor.InheritorsCollector(firSession)
            firClassSymbol.fir.accept(inheritorsCollector, sealedInheritorsByFirClass)
        }

        return buildMap {
            sealedInheritorsByFirClass.forEach { firClass, classIds ->
                put(firClass.symbol.classId, classIds.distinct())
            }
        }
    }

    object Directives : SimpleDirectivesContainer() {
        val DISABLE_SEALED_INHERITOR_CALCULATOR by directive(
            description = "Disable mock sealed class inheritor calculation",
            applicability = DirectiveApplicability.Global
        )
    }
}
