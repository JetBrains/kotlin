/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.util.io.FileUtil
import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.idea.fir.low.level.api.KtDeclarationAndFirDeclarationEqualityChecker
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getResolveState
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import java.io.File

abstract class AbstractKtDeclarationAndFirDeclarationEqualityChecker : KotlinLightCodeInsightFixtureTestCase() {
    protected fun doTest(path: String) {
        val file = File(path)
        val ktFile = myFixture.configureByText(file.name, FileUtil.loadFile(file)) as KtFile
        val resolveState = ktFile.getResolveState()
        ktFile.forEachDescendantOfType<KtFunction> { ktFunction ->
            val firFunction = ktFunction.getOrBuildFirOfType<FirFunction<*>>(resolveState)
            if (!KtDeclarationAndFirDeclarationEqualityChecker.representsTheSameDeclaration(ktFunction, firFunction)) {
                throw FileComparisonFailure(
                    /* message= */          null,
                                            KtDeclarationAndFirDeclarationEqualityChecker.renderPsi(ktFunction),
                                            KtDeclarationAndFirDeclarationEqualityChecker.renderFir(firFunction),
                    /* expectedFilePath= */ null
                )
            }
        }
    }
}
