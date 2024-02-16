/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveDesignationCollector
import org.jetbrains.kotlin.analysis.test.framework.project.structure.allKtFiles
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhaseRecursively
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

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

        resolveWithClearCaches(ktFile) { firResolveSession ->
            checkSession(firResolveSession)

            val (elementToResolve, resolver) = resolverProvider(firResolveSession)
            val filesToRender = if (renderAllFiles) {
                testServices.allKtFiles().map(firResolveSession::getOrBuildFirFile)
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
}
