/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.substitutorProvider

import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.stringRepresentation
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance

abstract class AbstractCreateInheritanceTypeSubstitutorTest : AbstractAnalysisApiBasedTest() {
    override fun doTest(testServices: TestServices) {
        val baseClass = testServices.expressionMarkerProvider
            .getBottommostElementsOfTypeAtCarets<KtClassOrObject>(testServices, "base")
            .single().first
        val superClass = testServices.expressionMarkerProvider
            .getBottommostElementsOfTypeAtCarets<KtClassOrObject>(testServices, "super")
            .single().first

        val substitutorRendered = analyseForTest(baseClass) {
            val superClassSymbol = superClass.classSymbol!!
            val substitutor = createInheritanceTypeSubstitutor(baseClass.classSymbol!!, superClassSymbol)
            prettyPrint {
                appendLine("Substitutor: ${stringRepresentation(substitutor)}")
                if (substitutor != null) {
                    val collection = superClassSymbol.declaredMemberScope.callables.toList()
                    if (collection.isNotEmpty()) {
                        appendLine("Substituted callables:")
                        withIndent {
                            printCollectionIfNotEmpty(collection, separator = "\n\n") { callable ->
                                val signature = callable.substitute(substitutor)
                                printCollectionIfNotEmpty(signature.contextParameters, prefix = "context(", postfix = ") ") {
                                    append(it.returnType.render(typeRenderer, position = Variance.IN_VARIANCE))
                                }

                                signature.receiverType?.let {
                                    append(it.render(typeRenderer, position = Variance.IN_VARIANCE))
                                    append('.')
                                }

                                append(signature.callableId!!.callableName.asString())
                                if (callable is KaFunctionSymbol) {
                                    printCollection((signature as KaFunctionSignature<*>).valueParameters, prefix = "(", postfix = ")") {
                                        append(it.returnType.render(typeRenderer, position = Variance.IN_VARIANCE))
                                    }
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