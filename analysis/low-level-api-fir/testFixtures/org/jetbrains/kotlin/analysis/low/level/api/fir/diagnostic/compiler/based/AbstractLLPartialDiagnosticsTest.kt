/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based

import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.analysis.api.platform.modification.publishGlobalSourceOutOfBlockModificationEvent
import org.jetbrains.kotlin.analysis.low.level.api.fir.CustomOutputDiagnosticsConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLResolutionFacadeService
import org.jetbrains.kotlin.analysis.low.level.api.fir.TestByDirectiveSuppressor
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.partialBodyAnalysisState
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.AbstractLLCompilerBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.LowLevelFirAnalyzerFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.facades.LLFirAnalyzerFacadeFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.bodyBlock
import org.jetbrains.kotlin.analysis.low.level.api.fir.getDeclarationsToResolve
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.body
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkPhase
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isPartialBodyResolvable
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphRenderOptions
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.renderControlFlowGraphTo
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.configuration.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.jetbrains.kotlin.utils.bind
import kotlin.test.assertEquals

abstract class AbstractLLPartialDiagnosticsTest : AbstractLLCompilerBasedTest() {
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        baseFirDiagnosticTestConfiguration(
            frontendFacade = ::LowLevelFirFrontendFacade.bind(LLFirPartialBodyAnalysisAnalyzerFacadeFactory),
            testDataConsistencyHandler = ::ReversedFirIdenticalChecker,
        )

        useAfterAnalysisCheckers(::LLFirPartialBodyTestSuppressor, ::ControlFlowGraphConsistencyChecker)
        useMetaTestConfigurators({ testServices -> CustomOutputDiagnosticsConfigurator(".partialBody.", testServices) })
    }
}

private class LLFirPartialBodyTestSuppressor(
    testServices: TestServices,
) : TestByDirectiveSuppressor(
    suppressDirective = Directives.IGNORE_PARTIAL_BODY_ANALYSIS,
    directivesContainer = Directives,
    testServices
) {
    private object Directives : SimpleDirectivesContainer() {
        val IGNORE_PARTIAL_BODY_ANALYSIS
                by stringDirective("Temporary disables reversed resolve checks until the issue is fixed. YT ticket must be provided")
    }
}

private object LLFirPartialBodyAnalysisAnalyzerFacadeFactory : LLFirAnalyzerFacadeFactory() {
    override fun createFirFacade(
        resolutionFacade: LLResolutionFacade,
        allFirFiles: Map<TestFile, FirFile>,
        diagnosticCheckerFilter: DiagnosticCheckerFilter
    ): LowLevelFirAnalyzerFacade {
        return object : LowLevelFirAnalyzerFacade(resolutionFacade, allFirFiles, diagnosticCheckerFilter) {
            override fun runResolution(): List<FirFile> {
                for (declaration in getDeclarationsToResolve(allFirFiles.values)) {
                    declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE.previous)
                    if (!analyzeDeclarationBodyGradually(declaration)) {
                        declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                    }
                    declaration.checkPhase(FirResolvePhase.BODY_RESOLVE)
                }

                return allFirFiles.values.toList()
            }

            private fun analyzeDeclarationBodyGradually(declaration: FirDeclaration): Boolean {
                if (!declaration.isPartialBodyResolvable) {
                    return false
                }

                val psiDeclaration = declaration.realPsi as? KtDeclaration ?: return false
                val psiBlock = psiDeclaration.bodyBlock ?: return false
                analyzeDeclarationBlock(declaration, psiBlock)
                return true
            }

            private fun analyzeDeclarationBlock(declaration: FirDeclaration, psiBlock: KtBlockExpression) {
                val psiStatements = psiBlock.statements
                if (psiStatements.size < 2) {
                    psiBlock.getOrBuildFir(resolutionFacade)
                    return
                }

                repeat(psiStatements.size) { index ->
                    val firElement = findCorrespondingFirElement(declaration, psiStatements, index)
                    check(firElement != null)

                    val lastState = declaration.partialBodyAnalysisState
                    check(lastState != null)
                    check(lastState.analyzedPsiStatementCount == index + 1)

                    // The default behavior is to analyze the body fully after several partial analysis attempts.
                    // The following assignment disables this optimization.
                    val newPerformedAnalysesCount = minOf(lastState.performedAnalysesCount, 1)
                    declaration.partialBodyAnalysisState = lastState.copy(performedAnalysesCount = newPerformedAnalysesCount)
                }
            }

            /**
             * Find a corresponding FIR element for the topmost [KtExpression] statement on the index [psiStatementIndex].
             *
             * Comparing to [getOrBuildFir], this function finds statements even for cases when there is no direct mapping
             * between PSI and FIR representations.
             */
            private fun findCorrespondingFirElement(
                declaration: FirDeclaration,
                psiStatements: List<KtExpression>,
                psiStatementIndex: Int
            ): FirElement? {
                val regularElement = psiStatements[psiStatementIndex].getOrBuildFir(resolutionFacade)
                if (regularElement != null) {
                    return regularElement
                }

                // At this point, we failed to find a corresponding FIR element.
                // So let's find some FIR element that relates (possibly indirectly) to the next PSI statement.
                // Then, we say the previous FIR element corresponds to 'psiStatements[psiStatementIndex]'.
                val body = declaration.body
                    ?: error("Block is missing for the declaration $declaration")

                if (psiStatementIndex == psiStatements.lastIndex) {
                    // A last FIR statement should correspond to the last PSI statement
                    return body.statements.lastOrNull()
                }

                val nextStatementDescendants = psiStatements[psiStatementIndex + 1]
                    .descendantsOfType<KtElement>()
                    .toSet()

                val nextStatementIndex = body.statements.indexOfFirst { it.psi in nextStatementDescendants }
                if (nextStatementIndex < 0) {
                    error("Failed to find the next statement for " + psiStatements[psiStatementIndex].text)
                }

                return body.statements.getOrNull(nextStatementIndex - 1)
            }
        }
    }
}

private class ControlFlowGraphConsistencyChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun check(failedAssertions: List<WrappedException>) {
        if (failedAssertions.isNotEmpty()) {
            return
        }

        val partialAnalysisControlFlowGraphText = renderControlFlowGraph(forceAnalysis = false)

        runWriteAction {
            // Invalidate all partial resolution results
            testServices.ktTestModuleStructure.project.publishGlobalSourceOutOfBlockModificationEvent()
        }

        val fullAnalysisControlFlowGraphText = renderControlFlowGraph(forceAnalysis = true)

        assertEquals(fullAnalysisControlFlowGraphText, partialAnalysisControlFlowGraphText)
    }

    private fun renderControlFlowGraph(forceAnalysis: Boolean): String {
        val builder = StringBuilder()

        val project = testServices.ktTestModuleStructure.project
        val renderOptions = ControlFlowGraphRenderOptions(renderLevels = true, renderFlow = true)

        for (testModule in testServices.ktTestModuleStructure.mainModules) {
            val module = testModule.ktModule
            val moduleName = testModule.name

            val resolutionFacade = LLResolutionFacadeService.getInstance(project).getResolutionFacade(module)
            val firFiles = testModule.ktFiles.map { it.getOrBuildFirFile(resolutionFacade) }

            if (forceAnalysis) {
                // Ideally, here we should compare the result with whole-file recursive analysis.
                // However, even without partial body resolution, the control flow graphs sometimes aren't consistent.
                for (declaration in getDeclarationsToResolve(firFiles)) {
                    declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                }
            }

            for (firFile in firFiles) {
                val fileName = firFile.name
                builder.append(fileName).append(" from module ").append(moduleName).appendLine()
                builder.append("-".repeat(fileName.length)).appendLine().appendLine()
                firFile.renderControlFlowGraphTo(builder, renderOptions)
            }
        }

        return builder.toString()
    }
}

private fun getDeclarationsToResolve(files: Collection<FirFile>): List<FirDeclaration> {
    return files.getDeclarationsToResolve().sortedBy { !it.isPartialBodyResolvable }
}