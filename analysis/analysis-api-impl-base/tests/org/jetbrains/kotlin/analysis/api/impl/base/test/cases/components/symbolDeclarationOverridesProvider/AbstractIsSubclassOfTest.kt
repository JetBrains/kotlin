/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.symbolDeclarationOverridesProvider

import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileResolutionMode
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.utils.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractIsSubclassOfTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val actual = executeOnPooledThreadInReadAction {
            // Since we're analyzing a whole type hierarchy in the main file, we should use `PREFER_SELF` for dependent analysis.
            // `IGNORE_SELF` is too narrow. For example, if we have a type chain `A -> B -> C` and we try to get from `A` (subclass) to
            // `C` (superclass), `IGNORE_SELF` would resolve `B` from the original file instead of the copied file.
            dependentAnalyzeForTest(
                mainFile,
                danglingFileResolutionMode = KaDanglingFileResolutionMode.PREFER_SELF,
            ) { contextFile ->
                val subClass = testServices.expressionMarkerProvider
                    .getBottommostElementOfTypeAtCaret<KtClassOrObject>(contextFile, "sub")

                val superClass = testServices.expressionMarkerProvider
                    .getBottommostElementOfTypeAtCaret<KtClassOrObject>(contextFile, "super")

                val subClassSymbol = subClass.classSymbol!!
                val superClassSymbol = superClass.classSymbol!!

                val isSubClass = subClassSymbol.isSubClassOf(superClassSymbol)
                val isDirectSubClass = subClassSymbol.isDirectSubClassOf(superClassSymbol)

                buildString {
                    appendLine("IS_SUBCLASS: $isSubClass")
                    appendLine("IS_DIRECT_SUBCLASS: $isDirectSubClass")
                }
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
