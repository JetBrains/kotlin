/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider

import org.jetbrains.kotlin.analysis.api.components.KtDeclarationRendererOptions
import org.jetbrains.kotlin.analysis.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractContainingDeclarationProviderByMemberScopeTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val declaration = testServices.expressionMarkerProvider.getSelectedElementOfType<KtClassOrObject>(ktFile)

        val memberToContainingClass = analyseForTest(declaration) {
            val symbol = declaration.getClassOrObjectSymbol()
            prettyPrint {
                printCollection(symbol.getMemberScope().getAllSymbols().toList(), separator = "\n") { symbol ->
                    val containingDeclaration = symbol.getContainingSymbol() as KtNamedClassOrObjectSymbol
                    append(symbol.render(renderingOptions))
                    append(" fromClass ")
                    append(containingDeclaration.classIdIfNonLocal?.asString())
                    if (symbol.typeParameters.isNotEmpty()) {
                        appendLine()
                        withIndent {
                            printCollection(symbol.typeParameters, separator = "\n") { typeParameter ->
                                val containingDeclarationForTypeParameter = typeParameter.getContainingSymbol()
                                append(typeParameter.render(renderingOptions))
                                append(" from ")
                                append(containingDeclarationForTypeParameter?.qualifiedNameString())
                            }
                        }
                    }
                }
            }
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(memberToContainingClass)
    }

    private fun KtSymbol.qualifiedNameString() = when (this) {
        is KtClassLikeSymbol -> classIdIfNonLocal!!.asString()
        is KtCallableSymbol -> callableIdIfNonLocal!!.toString()
        else -> error("unknown symbol $this")
    }

    companion object {
        val renderingOptions = KtDeclarationRendererOptions.DEFAULT.copy(
            typeRendererOptions = KtTypeRendererOptions.SHORT_NAMES,
            modifiers = emptySet()
        )
    }
}