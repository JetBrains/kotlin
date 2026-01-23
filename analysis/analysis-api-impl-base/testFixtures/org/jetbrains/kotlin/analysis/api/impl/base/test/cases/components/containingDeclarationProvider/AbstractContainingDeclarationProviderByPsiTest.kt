/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.checkContainingFileSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.checkContainingJvmClassName
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaScriptSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractContainingDeclarationProviderByPsiTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val currentPath = mutableListOf<KtDeclaration>()
        val ktClasses = mutableListOf<KtClassOrObject>()

        copyAwareAnalyzeForTest(mainFile) { contextFile ->
            val expectedFileSymbol = contextFile.symbol
            contextFile.accept(object : KtVisitorVoid() {
                override fun visitElement(element: PsiElement) {
                    element.acceptChildren(this)
                }

                override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                    // Due to a PSI quirk, `KtFunctionLiteral` can't be reached by `visitDeclaration` directly,
                    // but we need to visit it to match it with `KaFirAnonymousFunctionSymbol`
                    visitDeclaration(lambdaExpression.functionLiteral)
                }

                override fun visitDeclaration(dcl: KtDeclaration) {
                    val parentDeclaration = currentPath.lastOrNull()
                    val currentDeclarationSymbol = dcl.symbol
                    val expectedParentDeclarationSymbol = parentDeclaration?.symbol?.let { symbol ->
                        // From the FIR point of view, the real containing declaration of enum entry functions
                        // is not the enum entry itself, but its `KaFirEnumEntryInitializerSymbol`.
                        // However, it is not a `KtDeclaration` from the PSI point of view.
                        if (symbol is KaEnumEntrySymbol) symbol.enumEntryInitializer else symbol
                    }
                    val actualParentDeclarationSymbol = currentDeclarationSymbol.containingDeclaration

                    if (dcl is KtScriptInitializer) {
                        testServices.assertions.assertTrue(currentDeclarationSymbol is KaScriptSymbol)
                    } else {
                        testServices.assertions.assertEquals(expectedParentDeclarationSymbol, actualParentDeclarationSymbol) {
                            "Invalid parent declaration for $currentDeclarationSymbol, expected $expectedParentDeclarationSymbol but $actualParentDeclarationSymbol found"
                        }
                    }

                    checkContainingFileSymbol(expectedFileSymbol, currentDeclarationSymbol, testServices)
                    if (currentDeclarationSymbol is KaCallableSymbol) {
                        checkContainingJvmClassName(contextFile, ktClasses.lastOrNull(), currentDeclarationSymbol, testServices)
                    }

                    currentPath.add(dcl)
                    if (dcl is KtClassOrObject) {
                        ktClasses.add(dcl)
                    }
                    super.visitDeclaration(dcl)
                    currentPath.removeLast()
                    if (dcl is KtClassOrObject) {
                        ktClasses.removeLast()
                    }
                }
            })
        }

    }
}