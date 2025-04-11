/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure

abstract class AbstractByQualifiedNameLazyDeclarationResolveTest : AbstractFirLazyDeclarationResolveOverAllPhasesTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val psiFile = mainFile ?: mainModule.psiFiles.first()
        val classId = testServices.moduleStructure.allDirectives.singleValue(Directives.CLASS_ID).let(ClassId::fromString)
        val resolutionFacade = LLFirResolveSessionService.getInstance(psiFile.project).getResolutionFacade(mainModule.ktModule)
        val classDeclaration = findRegularClass(classId, mainModule.ktModule, resolutionFacade).findPsi(mainModule.ktModule.contentScope) as KtClassOrObject
        val file = classDeclaration.containingFile as KtFile

        doLazyResolveTest(file, testServices, outputRenderingMode) { firSession ->
            val regularClass = findRegularClass(classId, mainModule.ktModule, firSession)
            val declarationToResolve = chooseMemberDeclarationIfNeeded(regularClass, testServices.moduleStructure, firSession)
            declarationToResolve.fir to fun(phase: FirResolvePhase) {
                declarationToResolve.lazyResolveToPhase(phase)
            }
        }
    }

    open val outputRenderingMode: OutputRenderingMode = OutputRenderingMode.USE_SITE_AND_DESIGNATION_FILES

    private fun findRegularClass(classId: ClassId, module: KaModule, resolutionFacade: LLResolutionFacade): FirRegularClassSymbol {
        return resolutionFacade.getSessionFor(module).getRegularClassSymbolByClassId(classId) ?: error("'$classId' is not found")
    }

    private object Directives : SimpleDirectivesContainer() {
        val CLASS_ID by stringDirective("ClassId of expected class")
    }
}