/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractFirLazyDeclarationResolveOverAllPhasesTest.Directives.PRE_RESOLVED_PHASE
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractFirLazyDeclarationResolveOverAllPhasesTest.OutputRenderingMode.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveDesignationCollector
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This test iterates over all [FirResolvePhase] for the selected declaration and dump output after each phase
 */
abstract class AbstractFirLazyDeclarationResolveOverAllPhasesTest : AbstractFirLazyDeclarationResolveTestCase() {
    abstract fun checkSession(firSession: LLResolutionFacade)

    protected open val outputExtension: String get() = ".txt"

    /**
     * Represent how much of files from the testdata we want to see in our test output, additionally to the target declaration.
     *
     * Modes:
     * - [ALL_FILES_FROM_ALL_MODULES]: Render all files from all modules
     * - [USE_SITE_AND_DESIGNATION_FILES]: Render only a use-site kt file and a file from designation
     * - [ONLY_TARGET_DECLARATION]: Do not render any files, render only target declaration
     */
    enum class OutputRenderingMode {
        ALL_FILES_FROM_ALL_MODULES,
        USE_SITE_AND_DESIGNATION_FILES,
        ONLY_TARGET_DECLARATION,
    }

    protected fun doLazyResolveTest(
        ktFile: KtFile,
        testServices: TestServices,
        outputRenderingMode: OutputRenderingMode,
        resolverProvider: (LLResolutionFacade) -> Pair<FirElementWithResolveState, ((FirResolvePhase) -> Unit)>,
    ) {
        val resultBuilder = StringBuilder()
        val renderer = lazyResolveRenderer(resultBuilder)

        withResolveSession(ktFile) { firResolveSession ->
            checkSession(firResolveSession)
            val allKtFiles = testServices.ktTestModuleStructure.allMainKtFiles

            val preresolvedElementCarets = testServices.expressionMarkerProvider.getBottommostElementsOfTypeAtCarets<KtDeclaration>(
                files = allKtFiles,
                qualifier = "preresolved",
            )

            val phase = testServices.moduleStructure.allDirectives.singleOrZeroValue(PRE_RESOLVED_PHASE)
            if (preresolvedElementCarets.isEmpty() && phase != null) {
                error("$PRE_RESOLVED_PHASE is declared, but there is not any pre-resolved carets")
            }

            preresolvedElementCarets.forEach { (declaration, _) ->
                declaration.resolveToFirSymbol(firResolveSession, phase ?: FirResolvePhase.BODY_RESOLVE)
            }

            val (elementToResolve, resolver) = resolverProvider(firResolveSession)
            val filesToRender = when (outputRenderingMode) {
                OutputRenderingMode.ALL_FILES_FROM_ALL_MODULES -> {
                    allKtFiles.map(firResolveSession::getOrBuildFirFile)
                }
                OutputRenderingMode.USE_SITE_AND_DESIGNATION_FILES -> {
                    val firFile = firResolveSession.getOrBuildFirFile(ktFile)
                    val designation = LLFirResolveDesignationCollector.getDesignationToResolve(elementToResolve)
                    listOfNotNull(firFile, designation?.firFile).distinct()
                }
                OutputRenderingMode.ONLY_TARGET_DECLARATION -> emptyList()
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

        clearCaches(ktFile.project)

        withResolveSession(ktFile) { llSession ->
            checkSession(llSession)
            val firFile = llSession.getOrBuildFirFile(ktFile)
            firFile.lazyResolveToPhaseRecursively(FirResolvePhase.BODY_RESOLVE)
            if (resultBuilder.isNotEmpty()) {
                resultBuilder.appendLine()
            }

            resultBuilder.append("FILE RAW TO BODY:\n")
            renderer.renderElementAsString(firFile)
        }

        testServices.assertions.assertEqualsToTestOutputFile(
            resultBuilder.toString(),
            extension = outputExtension,
        )
    }

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    private object Directives : SimpleDirectivesContainer() {
        val PRE_RESOLVED_PHASE by enumDirective<FirResolvePhase>("Describes which phase should have pre-resolved element")
    }
}
