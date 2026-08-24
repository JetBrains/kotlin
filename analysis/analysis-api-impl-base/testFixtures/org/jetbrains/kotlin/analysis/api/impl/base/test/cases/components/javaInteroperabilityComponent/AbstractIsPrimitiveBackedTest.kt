/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.javaInteroperabilityComponent

import org.jetbrains.kotlin.analysis.api.javaInterop.isPrimitiveBacked
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.api.types.type
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractIsPrimitiveBackedTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        executeOnPooledThreadInReadAction {
            copyAwareAnalyzeForTest(mainFile) {
                val typeReference = testServices.expressionMarkerProvider.getTopmostSelectedElementOfType<KtTypeReference>(mainFile)
                val type = typeReference.type

                val actualText = buildString {
                    appendLine("Type:")
                    appendLine(KaDebugRenderer().renderType(useSiteSession, type))
                    appendLine()
                    appendLine("isPrimitiveBacked:")
                    appendLine(type.isPrimitiveBacked)
                }

                testServices.assertions.assertEqualsToTestOutputFile(actualText)
            }
        }
    }
}
