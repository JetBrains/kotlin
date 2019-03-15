/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.liveTemplates

import com.intellij.codeInsight.template.TemplateContextType
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.liveTemplates.KotlinTemplateContextType.*
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import java.io.File

class LiveTemplatesContextTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String =
            File(TEST_DATA_BASE_PATH, "/context").path + File.separator

    fun testInDocComment() {
        assertInContexts(Generic::class.java, Comment::class.java)
    }

    fun testTopLevel() {
        assertInContexts(Generic::class.java, TopLevel::class.java)
    }

    fun testInExpression() {
        assertInContexts(Generic::class.java, Expression::class.java)
    }

    fun testAnonymousObject() {
        assertInContexts(Generic::class.java, Class::class.java)
    }

    fun testCompanionObject() {
        assertInContexts(Generic::class.java, Class::class.java, ObjectDeclaration::class.java)
    }

    fun testLocalObject() {
        assertInContexts(Generic::class.java, Class::class.java, ObjectDeclaration::class.java)
    }

    fun testObjectInClass() {
        assertInContexts(Generic::class.java, Class::class.java, ObjectDeclaration::class.java)
    }

    fun testObjectInObject() {
        assertInContexts(Generic::class.java, Class::class.java, ObjectDeclaration::class.java)
    }

    fun testTopLevelObject() {
        assertInContexts(Generic::class.java, Class::class.java, ObjectDeclaration::class.java)
    }

    fun testStatementInBlock() {
        assertInContexts(Generic::class.java, Statement::class.java, Expression::class.java)
    }

    fun testStatementInDoWhile() {
        assertInContexts(Generic::class.java, Statement::class.java, Expression::class.java)
    }

    fun testStatementInFor() {
        assertInContexts(Generic::class.java, Statement::class.java, Expression::class.java)
    }

    fun testStatementInIfElse() {
        assertInContexts(Generic::class.java, Statement::class.java, Expression::class.java)
    }

    fun testStatementInIfThen() {
        assertInContexts(Generic::class.java, Statement::class.java, Expression::class.java)
    }

    fun testStatementInWhile() {
        assertInContexts(Generic::class.java, Statement::class.java, Expression::class.java)
    }

    fun testStatementInWhen() {
        assertInContexts(Generic::class.java, Statement::class.java, Expression::class.java)
    }

    private fun assertInContexts(vararg expectedContexts: java.lang.Class<out KotlinTemplateContextType>) {
        myFixture.configureByFile(getTestName(false) + ".kt")
        val allContexts = TemplateContextType.EP_NAME.extensions.filter { it is KotlinTemplateContextType }
        val enabledContexts = allContexts.filter { it.isInContext(myFixture.file, myFixture.caretOffset) }.map { it::class.java }
        UsefulTestCase.assertSameElements(enabledContexts, *expectedContexts)
    }
}
