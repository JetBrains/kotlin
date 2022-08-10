/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider

import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedSingleModuleTest
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractIsSubclassOfTest : AbstractAnalysisApiBasedSingleModuleTest() {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val subClass = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtClassOrObject>(ktFiles.first(), "sub")
        val superClass = testServices.expressionMarkerProvider.getElementOfTypeAtCaret<KtClassOrObject>(ktFiles.first(), "super")

        val actual = executeOnPooledThreadInReadAction {
            analyseForTest(subClass) {
                val subClassSymbol = subClass.getClassOrObjectSymbol()
                val superClassSymbol = superClass.getClassOrObjectSymbol()

                val isSubClass = subClassSymbol.isSubClassOf(superClassSymbol)
                val isDirectSubClass = subClassSymbol.isDirectSubClassOf(superClassSymbol)

                buildString {
                    appendLine("IS_SUBCLASS: $isSubClass")
                    appendLine("IS_DIRECT_SUBCLASS: $isDirectSubClass")
                }
            }
        }
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }
}