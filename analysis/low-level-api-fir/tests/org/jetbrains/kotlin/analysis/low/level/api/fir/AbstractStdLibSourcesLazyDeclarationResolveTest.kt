/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirStdlibSourceTestConfigurator
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractStdLibSourcesLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveOverAllPhasesTest() {
    override fun checkSession(firSession: LLFirResolveSession) {
        requireIsInstance<KtLibrarySourceModule>(firSession.useSiteKtModule)
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    override val configurator get() = AnalysisApiFirStdlibSourceTestConfigurator

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: TestModule, testServices: TestServices) {
        val psiFile = mainFile ?: testServices.ktModuleProvider.getModuleFiles(mainModule).first()
        val project = psiFile.project
        val moduleStructure = testServices.moduleStructure
        val classId = moduleStructure.allDirectives.singleValue(Directives.CLASS_ID).let(ClassId::fromString)
        val module = ProjectStructureProvider.getModule(project, psiFile, contextualModule = null)
        val resolveSession = LLFirResolveSessionService.getInstance(project).getFirResolveSession(module)
        val classDeclaration = findRegularClass(classId, module, resolveSession).findPsi() as KtClassOrObject
        val file = classDeclaration.containingFile as KtFile

        doLazyResolveTest(file, testServices, renderAllFiles = false) { firSession ->
            val regularClass = findRegularClass(classId, module, firSession)
            val declarationToResolve = chooseMemberDeclarationIfNeeded(regularClass, moduleStructure, firSession)
            declarationToResolve.fir to fun(phase: FirResolvePhase) {
                declarationToResolve.lazyResolveToPhase(phase)
            }
        }
    }

    private fun findRegularClass(classId: ClassId, module: KtModule, firSession: LLFirResolveSession): FirRegularClassSymbol {
        val symbolProvider = firSession.getSessionFor(module).symbolProvider
        return symbolProvider.getRegularClassSymbolByClassId(classId) ?: error("'$classId' is not found")
    }

    private object Directives : SimpleDirectivesContainer() {
        val CLASS_ID by stringDirective("ClassId of expected class")
    }
}