/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.containingDeclarationProvider

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.checkContainingFileSymbol
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.symbols.checkContainingJvmClassName
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtScriptSymbol
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractContainingDeclarationProviderByPsiTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: TestModule, testServices: TestServices) {
        val currentPath = mutableListOf<KtDeclaration>()
        val ktClasses = mutableListOf<KtClassOrObject>()
        analyseForTest(mainFile) {
            val expectedFileSymbol = mainFile.getFileSymbol()
            mainFile.accept(object : KtVisitorVoid() {
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
                        checkContainingJvmClassName(mainFile, ktClasses.lastOrNull(), currentDeclarationSymbol, testServices)
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