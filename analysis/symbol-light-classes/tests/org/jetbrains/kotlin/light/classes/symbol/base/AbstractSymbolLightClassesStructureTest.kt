/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractSymbolLightClassesStructureTest(
    configurator: AnalysisApiTestConfigurator,
    testPrefix: String,
    stopIfCompilationErrorDirectivePresent: Boolean,
) : AbstractSymbolLightClassesStructureTestBase(configurator, testPrefix, stopIfCompilationErrorDirectivePresent) {

    override fun doLightClassTest(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val result = prettyPrint {
            for (ktFile in ktFiles.sortedBy(KtFile::getName)) {
                if (ktFiles.size > 1) {
                    appendLine("${ktFile.name}:")
                    withIndent {
                        handleFile(ktFile)
                    }
                } else {
                    handleFile(ktFile)
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(result, testPrefix = testPrefix)
        doTestInheritors(ktFiles, testServices)
    }
}
