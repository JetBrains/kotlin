/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.isLLFirTestData

class LLFirDivergenceCommentChecker(testServices: TestServices) : AfterAnalysisChecker(testServices) {
    override fun check(failedAssertions: List<WrappedException>) {
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        if (!testDataFile.isLLFirTestData) return

        if (!testDataFile.hasLlFirDivergenceDirective()) {
            testServices.assertions.fail {
                """The LL FIR test data file `${testDataFile.name}` is missing an `LL_FIR_DIVERGENCE` directive. At the beginning of the 
                  |file, add the following directive:
                  |
                  |// LL_FIR_DIVERGENCE
                  |// A comment describing why the LL FIR result is diverging from the compiler result. You must provide a good reason, or
                  |// otherwise the divergence is probably a bug in LL FIR which needs to be fixed. Try to be as specific as possible.
                  |// LL_FIR_DIVERGENCE
                  |""".trimMargin()
            }
        }
    }
}
