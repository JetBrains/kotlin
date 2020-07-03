/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.addExternalTestFiles
import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.getAnalysisSessionFor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

abstract class AbstractSymbolPointerTest : KotlinLightCodeInsightFixtureTestCase() {
    protected fun doTest(path: String) {
        val file = File(path)
        val ktFile = myFixture.configureByText(file.name, FileUtil.loadFile(file)) as KtFile

        val pointers = executeOnPooledThreadInReadAction {
            analyze(ktFile) {
                val declarationSymbols = ktFile.collectDescendantsOfType<KtDeclaration>().map { declaration ->
                    symbolProvider.getSymbol(declaration)
                }
                declarationSymbols.map { PointerWithPsi(it.createPointer(), it.psi!!) }
            }
        }

        // another read action
        executeOnPooledThreadInReadAction {
            analyze(ktFile) {
                pointers.forEach { (pointer, psi) ->
                    val restored = pointer.restoreSymbol(this) ?: error("Symbol $psi was not not restored correctly")
                    Assert.assertEquals(restored.psi!!, psi)
                }
            }
        }
    }
}

private data class PointerWithPsi(val pointer: KtSymbolPointer<*>, val psi: PsiElement)