/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractFirLazyDeclarationResolveOverAllPhasesTest.Directives.PRE_RESOLVED_PHASE
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveDesignationCollector
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This test iterates over all [FirResolvePhase] for the selected declaration and dump output after each phase
 */
abstract class AbstractFirLazyDeclarationResolveOverAllPhasesTest : AbstractFirLazyDeclarationResolveTestCase() {
    abstract fun checkSession(firSession: LLFirResolveSession)

    protected open val outputExtension: String get() = ".txt"

    protected fun doLazyResolveTest(
        ktFile: KtFile,
        testServices: TestServices,
        renderAllFiles: Boolean,
        resolverProvider: (LLFirResolveSession) -> Pair<FirElementWithResolveState, ((FirResolvePhase) -> Unit)>,
    ) {
        val resultBuilder = StringBuilder()
        val renderer = lazyResolveRenderer(resultBuilder)

        resolveWithCaches(ktFile) { firResolveSession ->
            checkSession(firResolveSession)
            val allKtFiles = testServices.ktTestModuleStructure.allMainKtFiles

            val preresolvedElementCarets = testServices.expressionMarkerProvider.getElementsOfTypeAtCarets<KtDeclaration>(
                files = allKtFiles,
                caretTag = "preresolved",
            )

            val phase = testServices.moduleStructure.allDirectives.singleOrZeroValue(PRE_RESOLVED_PHASE)
            if (preresolvedElementCarets.isEmpty() && phase != null) {
                error("$PRE_RESOLVED_PHASE is declared, but there is not any pre-resolved carets")
            }

            preresolvedElementCarets.forEach { (declaration, _) ->
                declaration.resolveToFirSymbol(firResolveSession, phase ?: FirResolvePhase.BODY_RESOLVE)
            }

            val (elementToResolve, resolver) = resolverProvider(firResolveSession)
            val filesToRender = if (renderAllFiles) {
                allKtFiles.map(firResolveSession::getOrBuildFirFile)
            } else {
                val firFile = firResolveSession.getOrBuildFirFile(ktFile)
                val designation = LLFirResolveDesignationCollector.getDesignationToResolve(elementToResolve)
                listOfNotNull(firFile, designation?.firFile).distinct()
            }

            val basePhase = elementToResolve.resolvePhase
            val shouldRenderDeclaration = elementToResolve !in filesToRender
            for (currentPhase in FirResolvePhase.entries) {
                if (currentPhase == FirResolvePhase.SEALED_CLASS_INHERITORS || currentPhase < basePhase) continue
                resolver(currentPhase)

                if (resultBuilder.isNotEmpty()) {
                    resultBuilder.appendLine()
                }

                resultBuilder.append("${currentPhase.name}:")
                if (shouldRenderDeclaration) {
                    resultBuilder.append("\nTARGET: ")
                    renderer.renderElementAsString(elementToResolve)
                }

                for (file in filesToRender) {
                    resultBuilder.appendLine()
                    renderer.renderElementAsString(file)
                }
            }
        }

        resolveWithClearCaches(ktFile) { llSession ->
            checkSession(llSession)
            val firFile = llSession.getOrBuildFirFile(ktFile)
            firFile.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE)
            if (resultBuilder.isNotEmpty()) {
                resultBuilder.appendLine()
            }

            resultBuilder.append("FILE RAW TO BODY:\n")
            renderer.renderElementAsString(firFile)
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(
            resultBuilder.toString(),
            extension = outputExtension,
        )
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

    private object Directives : SimpleDirectivesContainer() {
        val PRE_RESOLVED_PHASE by enumDirective<FirResolvePhase>("Describes which phase should have pre-resolved element")
    }
}
