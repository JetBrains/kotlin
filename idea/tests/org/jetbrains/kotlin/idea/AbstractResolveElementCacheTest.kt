/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.util.ThrowableRunnable
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.runAll
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.*

abstract class AbstractResolveElementCacheTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): KotlinLightProjectDescriptor = KotlinLightProjectDescriptor.INSTANCE

    companion object {
        //language=kotlin
        const val FILE_TEXT =
            """
class C(param1: String = "", param2: Int = 0) {
    fun a(p: Int = 0) {
        b(1, 2)
        val x = c()
        d(x)
    }

    fun b() {
        x(1)
    }

    fun c() {
        x(2)
    }
}
"""
    }

    protected data class Data(
        val file: KtFile,
        val klass: KtClass,
        val members: List<KtDeclaration>,
        val statements: List<KtExpression>,
        val factory: KtPsiFactory
    )

    override fun tearDown() {
        runAll(
            ThrowableRunnable { ResolveElementCache.forceFullAnalysisModeInTests = false },
            ThrowableRunnable { super.tearDown() },
        )
    }

    protected fun configureWithKotlin(@Language("kotlin") text: String): KtFile =
        myFixture.configureByText("Test.kt", text.trimIndent()) as KtFile

    protected fun doTest(handler: Data.() -> Unit) {
        val file = myFixture.configureByText("Test.kt", FILE_TEXT) as KtFile
        val data = extractData(file)
        myFixture.project.executeWriteCommand("") { data.handler() }
    }

    protected fun extractData(file: KtFile): Data {
        val klass = file.declarations.single() as KtClass
        val members = klass.declarations
        val function = members.first() as KtNamedFunction
        val statements = function.bodyBlockExpression!!.statements
        return Data(file, klass, members, statements, KtPsiFactory(project))
    }
}