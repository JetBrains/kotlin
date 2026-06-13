/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractMapToJvmTypeDescriptorTest : AbstractAnalysisApiBasedTest() {
    @OptIn(KtExperimentalApi::class)
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(mainFile) {
                val expressionMarkerProvider = testServices.expressionMarkerProvider

                val type: KaType

                val resolvable = expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtElement>(mainFile)
                if (resolvable != null) {
                    require(resolvable is KtResolvable)

                    val symbol = resolvable.resolveSymbol() ?: error("Failed to resolve symbol")
                    require(symbol is KaCallableSymbol)

                    type = symbol.returnType
                } else {
                    val typeReference = expressionMarkerProvider.getTopmostSelectedElementOfType<KtTypeReference>(mainFile)
                    type = typeReference.type
                }

                val actualText = buildString {
                    appendLine("Type:")
                    appendLine(KaDebugRenderer().renderType(useSiteSession, type))
                    appendLine()
                    appendLine("mapToJvmTypeDescriptor:")
                    appendLine(type.mapToJvmTypeDescriptor())
                }

                testServices.assertions.assertEqualsToTestOutputFile(actualText)
            }
        }
    }
}
