/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.javaInterop.isCompiledInJvmDefaultMode
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.expandedSymbol
import org.jetbrains.kotlin.analysis.api.types.type
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractIsCompiledInJvmDefaultModeTest : AbstractAnalysisApiBasedTest() {
    @OptIn(KaNonPublicApi::class)
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(mainFile) {
                val typeReference = testServices.expressionMarkerProvider.getTopmostSelectedElementOfType<KtTypeReference>(mainFile)
                val classSymbol = typeReference.type.expandedSymbol as? KaNamedClassSymbol
                    ?: error("The selected type reference should refer to a named class")

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
