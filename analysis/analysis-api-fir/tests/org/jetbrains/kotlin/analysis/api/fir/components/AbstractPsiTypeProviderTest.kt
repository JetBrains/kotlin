/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.analysis.api.fir.test.framework.AbstractHLApiSingleModuleTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractPsiTypeProviderTest : AbstractHLApiSingleModuleTest() {

    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val mainKtFile = ktFiles.singleOrNull() ?: ktFiles.first { it.name == "main.kt" }
        val declaration = testServices.expressionMarkerProvider.getElementOfTypAtCaret<KtDeclaration>(mainKtFile)
        val actual = buildString {
            executeOnPooledThreadInReadAction {
                analyse(declaration) {
                    val ktType = declaration.getReturnKtType()
                    appendLine("KtType: ${ktType.render()}")
                    appendLine("PsiType: ${ktType.asPsiType(declaration)}")
                }
            }
        }
        testServices.assertions.assertEqualsToFile(testDataFileSibling(".txt"), actual)
    }

}
