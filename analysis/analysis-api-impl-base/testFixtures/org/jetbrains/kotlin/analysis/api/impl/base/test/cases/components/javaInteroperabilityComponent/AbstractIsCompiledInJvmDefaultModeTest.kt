/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import org.jetbrains.kotlin.analysis.api.javaInterop.isCompiledInJvmDefaultMode
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractIsCompiledInJvmDefaultModeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(mainFile) { contextFile ->
                val classSymbol = getSingleTestTargetSymbolOfType<KaNamedClassSymbol>(testDataPath, contextFile)

                val actualText = buildString {
                    appendLine("Class:")
                    appendLine(classSymbol.classId)
                    appendLine()
                    appendLine("isCompiledInJvmDefaultMode:")
                    appendLine(classSymbol.isCompiledInJvmDefaultMode)
                }

                testServices.assertions.assertEqualsToTestOutputFile(actualText)
            }
        }
    }
}
