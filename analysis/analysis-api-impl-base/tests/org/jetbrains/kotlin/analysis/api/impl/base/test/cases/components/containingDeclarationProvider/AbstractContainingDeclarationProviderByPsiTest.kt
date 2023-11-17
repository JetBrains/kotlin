/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.checkContainingFileSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.checkContainingJvmClassName
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtScriptSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiSingleFileTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractContainingDeclarationProviderByPsiTest : AbstractAnalysisApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, module: TestModule, testServices: TestServices) {
        val currentPath = mutableListOf<KtDeclaration>()
        val ktClasses = mutableListOf<KtClassOrObject>()
        analyseForTest(ktFile.declarations.first()) {
            val expectedFileSymbol = ktFile.getFileSymbol()
            ktFile.accept(object : KtVisitorVoid() {
                override fun visitElement(element: PsiElement) {
                    element.acceptChildren(this)
                }

                override fun visitDeclaration(dcl: KtDeclaration) {
                    val parentDeclaration = currentPath.lastOrNull()
                    val currentDeclarationSymbol = dcl.getSymbol()
                    val expectedParentDeclarationSymbol = parentDeclaration?.getSymbol()
                    val actualParentDeclarationSymbol = currentDeclarationSymbol.getContainingSymbol()

                    if (dcl is KtScriptInitializer) {
                        testServices.assertions.assertTrue(currentDeclarationSymbol is KtScriptSymbol)
                    } else {
                        testServices.assertions.assertEquals(expectedParentDeclarationSymbol, actualParentDeclarationSymbol) {
                            "Invalid parent declaration for $currentDeclarationSymbol, expected $expectedParentDeclarationSymbol but $actualParentDeclarationSymbol found"
                        }
                    }

                    checkContainingFileSymbol(expectedFileSymbol, currentDeclarationSymbol, testServices)
                    if (currentDeclarationSymbol is KtCallableSymbol) {
                        checkContainingJvmClassName(ktFile, ktClasses.lastOrNull(), currentDeclarationSymbol, testServices)
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