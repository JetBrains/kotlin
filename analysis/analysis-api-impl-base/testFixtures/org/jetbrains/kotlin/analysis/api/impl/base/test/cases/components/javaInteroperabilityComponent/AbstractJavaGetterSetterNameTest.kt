/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import org.jetbrains.kotlin.analysis.api.javaInterop.javaMethodName
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.symbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractJavaGetterSetterNameTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actualText = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val property = testServices.expressionMarkerProvider.getTopmostSelectedElementOfType<KtProperty>(contextFile)
            val propertySymbol = property.symbol as KaPropertySymbol

            buildString {
                appendLine("Getter name: " + propertySymbol.getter?.javaMethodName)
                appendLine("Setter name: " + propertySymbol.setter?.javaMethodName)
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actualText)
    }
}
