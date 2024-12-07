/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.substitutorProvider

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance
import kotlin.collections.single

abstract class AbstractCreateInheritanceTypeSubstitutorTest : AbstractAnalysisApiBasedTest() {
    override fun doTest(testServices: TestServices) {
        val baseClass = testServices.expressionMarkerProvider
            .getElementsOfTypeAtCarets<KtClassOrObject>(testServices, "base")
            .single().first
        val superClass = testServices.expressionMarkerProvider
            .getElementsOfTypeAtCarets<KtClassOrObject>(testServices, "super")
            .single().first

        val substitutorRendered = analyseForTest(baseClass) {
            val superClassSymbol = superClass.classSymbol!!
            val substitutor = createInheritanceTypeSubstitutor(baseClass.classSymbol!!, superClassSymbol)
            prettyPrint {
                appendLine("Substitutor: ${stringRepresentation(substitutor)}")
                if (substitutor != null) {
                    val functions = superClassSymbol.declaredMemberScope.declarations
                        .filterIsInstance<KaNamedFunctionSymbol>()
                        .toList()
                    if (functions.isNotEmpty()) {
                        appendLine("Substituted callables:")
                        withIndent {
                            for (function in functions) {
                                val signature = function.substitute(substitutor)
                                append(signature.callableId!!.callableName.asString())
                                printCollection(signature.valueParameters, prefix = "(", postfix = ")") {
                                    append(it.returnType.render(typeRenderer, position = Variance.IN_VARIANCE))
                                }
                                append(": ${signature.returnType.render(typeRenderer, position = Variance.OUT_VARIANCE)}")
                            }
                        }
                    }
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(substitutorRendered, extension = ".result.txt")
    }

    companion object {
        private val typeRenderer = KaTypeRendererForSource.WITH_SHORT_NAMES
    }
}