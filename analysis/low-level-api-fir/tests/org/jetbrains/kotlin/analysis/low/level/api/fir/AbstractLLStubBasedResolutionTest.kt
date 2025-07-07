/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLStubBasedTest.Directives.INCONSISTENT_DECLARATIONS
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirScriptTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * The test suite is supposed to check how far the resolution process can go for stub-only files.
 */
abstract class AbstractLLStubBasedResolutionTest : AbstractLLStubBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    private object Directives : SimpleDirectivesContainer() {
        val STUB_RESOLUTION_INCONSISTENCY by stringDirective("Indicates that stub-based and AST-based resolution differ. The YT issue number has to be provided")
    }

    override fun doTest(astBasedFile: KtFile, stubBasedFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val (stubBasedDump, phases) = withResolutionFacade(stubBasedFile) { facade ->
            val stubBasedFirFile = stubBasedFile.getOrBuildFirFile(facade)
            val phases = stubBasedFirFile.collectAllElementsOfType<FirElementWithResolveState>().map { element ->
                var lastPhase = FirResolvePhase.RAW_FIR
                for (phase in FirResolvePhase.entries) {
                    val isSucceed = computeAstLoadingAware {
                        element.lazyResolveToPhase(phase)
                        lastPhase = phase
                    } != null

                    if (!isSucceed) break
                }

                lastPhase
            }

            val stubBasedDump = dumpOutput(stubBasedFirFile)
            testServices.assertions.assertEqualsToTestOutputFile(stubBasedDump, extension = ".stubBasedResolve.txt")
            stubBasedDump to phases
        }

        clearCaches(astBasedFile.project)
        withResolutionFacade(astBasedFile) { facade ->
            // We need to clear caches to ensure that we will not load the file from the cache
            val astBasedFirFile = astBasedFile.getOrBuildFirFile(facade)
            val astBasedElements = astBasedFirFile.collectAllElementsOfType<FirElementWithResolveState>()
            val shouldHaveDifferentLength = INCONSISTENT_DECLARATIONS in testServices.moduleStructure.allDirectives
            if (shouldHaveDifferentLength) {
                if (astBasedElements.size == phases.size) {
                    testServices.assertions.fail {
                        "'// ${INCONSISTENT_DECLARATIONS}' directive is redundant and has to be dropped"
                    }
                }

                return
            } else {
                testServices.assertions.assertEquals(astBasedElements.size, phases.size) {
                    "Expected ${astBasedElements.size} elements, but got ${phases.size}"
                }
            }

            astBasedElements.zip(phases).forEach { (element, phase) ->
                element.lazyResolveToPhase(phase)
            }

            val astBasedDump = dumpOutput(astBasedFirFile)
            val astBasedOutputFile = getTestOutputFile(extension = ".astBasedResolve.txt").toFile()
            val stubDifferenceExpected = Directives.STUB_RESOLUTION_INCONSISTENCY in testServices.moduleStructure.allDirectives
            when {
                astBasedDump == stubBasedDump -> {
                    testServices.assertions.assertFalse(stubDifferenceExpected) {
                        "'// ${Directives.STUB_RESOLUTION_INCONSISTENCY}' directive is unused and has to be dropped"
                    }

                    testServices.assertions.assertFileDoesntExist(astBasedOutputFile) {
                        "${astBasedOutputFile.name} has the same content, so it should be dropped"
                    }
                }

                stubDifferenceExpected -> {
                    testServices.assertions.assertEqualsToFile(astBasedOutputFile, astBasedDump)
                }

                else -> {
                    testServices.assertions.assertEquals(astBasedDump, stubBasedDump) {
                        "AST-based and stub-based outputs are different"
                    }
                }
            }
        }
    }
}

private fun dumpOutput(file: FirFile): String = lazyResolveRenderer(StringBuilder()).renderElementAsString(file, trim = true)

abstract class AbstractLLSourceStubBasedResolutionTest : AbstractLLStubBasedResolutionTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractLLScriptStubBasedResolutionTest : AbstractLLStubBasedResolutionTest() {
    override val configurator = AnalysisApiFirScriptTestConfigurator(analyseInDependentSession = false)
}
