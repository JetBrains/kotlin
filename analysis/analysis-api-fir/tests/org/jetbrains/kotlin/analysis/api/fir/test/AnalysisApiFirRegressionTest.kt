/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test

import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.junit.jupiter.api.Test

class AnalysisApiFirRegressionTest : AbstractAnalysisApiExecutionTest("analysis/analysis-api-fir/testData/regressions") {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun kt82424ContextParameterDefaultLambda(mainFile: KtFile) {
        val functionLiteral = mainFile.descendantsOfType<KtFunctionLiteral>().single()

        analyze(functionLiteral) {
            functionLiteral.symbol.returnType
        }
    }
}
