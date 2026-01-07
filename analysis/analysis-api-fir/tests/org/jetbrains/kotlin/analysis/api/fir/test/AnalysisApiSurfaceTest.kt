/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AnalysisApiSurfaceTest : AbstractAnalysisApiExecutionTest("analysis/analysis-api-fir/testData/surface") {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun supertypeIteration(mainFile: KtFile) {
        val implClass = mainFile.declarations.first { it is KtClass && it.name == "Impl" } as KtClass
        analyze(implClass) {
            val defaultClassType = implClass.classSymbol!!.defaultType

            val allSupertypeSequence = defaultClassType.allSupertypes
            val directSupertypeSequence = defaultClassType.directSupertypes

            // Iterate through the sequence multiple times
            assertEquals(allSupertypeSequence.toList(), allSupertypeSequence.toList())
            assertEquals(directSupertypeSequence.toList(), directSupertypeSequence.toList())
        }
    }
}
