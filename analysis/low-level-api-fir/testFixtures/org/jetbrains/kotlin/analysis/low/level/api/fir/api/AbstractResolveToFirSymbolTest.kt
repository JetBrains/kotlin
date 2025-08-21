/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.withResolutionFacade
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.targets.getTestTargetKtElements
import org.jetbrains.kotlin.analysis.test.framework.utils.renderLocationDescription
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * Tests that [resolveToFirSymbol] resolves the expected FIR symbol for all PSI elements identified by a [TestSymbolTarget][org.jetbrains.kotlin.analysis.test.framework.targets.TestSymbolTarget].
 *
 * [resolveToFirSymbol] not only needs to ensure that ambiguities between different modules are properly handled, but also ambiguities
 * within a single module. As [resolveToFirSymbol] accepts a PSI element as a target, the function should return a FIR symbol for exactly
 * that PSI element, and not another PSI element with the same [ClassId].
 *
 * Each test file should have a unique name to ensure stable test results, as symbols are ordered by their PSI's containing file path and,
 * if equal, by the start offset of the PSI element (sources only).
 */
abstract class AbstractResolveToFirSymbolTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    private data class TargetElement(
        val ktDeclaration: KtDeclaration,
        val locationDescription: String,
    )

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val targetElements = getTestTargetKtElements(testDataPath, mainFile)
            .map { element ->
                require(element is KtDeclaration) {
                    "The target element should be a `${KtDeclaration::class.simpleName}`, but it's a `${element::class.simpleName}`"
                }
                TargetElement(element, element.renderLocationDescription())
            }
            .sortedWith(elementComparator)

        val actualText = withResolutionFacade(mainModule.ktModule) { resolutionFacade ->
            prettyPrint {
                targetElements.forEach { (ktDeclaration, locationDescription) ->
                    // Resolve to `BODY_RESOLVE` so that enum entry initializers are visible. This allows us to disambiguate enum entries with
                    // the same name in the test results according to their initializer members.
                    val firSymbol = ktDeclaration.resolveToFirSymbol(resolutionFacade, phase = FirResolvePhase.BODY_RESOLVE)

                    appendLine("${ktDeclaration::class.simpleName} '${ktDeclaration.name}' in $locationDescription:")
                    withIndent {
                        appendLine(firSymbol.fir.render())
                    }
                    appendLine()
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actualText)
    }

    /**
     * [elementComparator] ensures a stable order in the test results.
     *
     * We use the location description to sort elements because it takes different library JARs into account, in contrast to the file name.
     * For example, if we have two class files `A.class` in two JARs `library1.jar` and `library2.jar`, the file name (`A.class`) is not
     * sufficient to distinguish between the two class files. The location description, on the other hand, takes e.g. `library1.jar` into
     * account (`library1.jar!/A.class`).
     */
    private val elementComparator =
        compareBy<TargetElement> { it.locationDescription }
            .thenBy { targetElement ->
                if (targetElement.ktDeclaration.containingKtFile.isCompiled) return@thenBy null
                targetElement.ktDeclaration.startOffset
            }
}
