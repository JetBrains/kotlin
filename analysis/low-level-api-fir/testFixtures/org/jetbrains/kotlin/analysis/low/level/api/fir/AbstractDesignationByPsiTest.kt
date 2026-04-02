/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLFirSingleResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.asResolveTarget
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirElementFinder
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * The test covers [FirElementFinder] functionality.
 *
 * @see FirElementFinder.collectDesignationPath
 */
abstract class AbstractDesignationByPsiTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val targetDeclaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeByDirective(
            mainFile,
            mainModule.testModule,
            defaultType = KtDeclaration::class,
        ) as KtDeclaration

        val actual = withResolutionFacade(mainFile) { resolutionFacade ->
            val firFile = mainFile.getOrBuildFirFile(resolutionFacade)
            val designationPath = FirElementFinder.collectDesignationPath(firFile, targetDeclaration)
            val resolveTarget = designationPath?.asResolveTarget()
            val simplifiedOutput = resolveTarget?.toString()
                ?.removePrefix("${LLFirSingleResolveTarget::class.simpleName}(")
                ?.removeSuffix(")")

            prettyPrint {
                appendLine("KT element: ${targetDeclaration::class.simpleName}")
                appendLine("Resolve target: $simplifiedOutput")
                appendLine("FIR:")
                withIndent {
                    appendLine(designationPath?.target?.render())
                }
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}

abstract class AbstractSourceLikeDesignationByPsiTest : AbstractDesignationByPsiTest() {
    override val configurator = LLSourceLikeTestConfigurator()
}
