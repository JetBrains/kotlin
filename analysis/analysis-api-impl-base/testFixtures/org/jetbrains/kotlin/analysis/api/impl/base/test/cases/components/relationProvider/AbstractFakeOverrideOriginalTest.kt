/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.relationProvider

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFakeOverrideOriginalTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val symbol = getSingleTestTargetSymbolOfType<KaCallableSymbol>(testDataPath, contextFile)
            val original = symbol.fakeOverrideOriginal

            val renderer = KaDebugRenderer(renderExpandedTypes = true)
            prettyPrint {
                appendLine("IS_THE_SAME_SYMBOL: ${original == symbol}")
                appendLine("FAKE_OVERRIDE_ORIGINAL:")
                withIndent {
                    appendLine(renderer.render(useSiteSession, original))
                    withIndent {
                        appendLine("containingDeclaration: ${original.containingDeclaration?.qualifiedNameString()}")
                    }
                }
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    context(_: KaSession)
    private fun KaSymbol.qualifiedNameString(): String = when (this) {
        is KaConstructorSymbol -> "<constructor> ${containingClassId?.asString()}"
        is KaPropertyGetterSymbol -> "<getter> ${containingSymbol?.qualifiedNameString()}"
        is KaPropertySetterSymbol -> "<setter> ${containingSymbol?.qualifiedNameString()}"
        is KaClassLikeSymbol -> classId!!.asString()
        is KaCallableSymbol -> callableId!!.toString()
        else -> error("unknown symbol $this")
    }
}
