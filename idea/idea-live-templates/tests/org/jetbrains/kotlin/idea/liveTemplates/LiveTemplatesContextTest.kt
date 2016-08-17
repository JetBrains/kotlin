/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    private fun assertInContexts(vararg expectedContexts: java.lang.Class<out KotlinTemplateContextType>) {
        myFixture.configureByFile(getTestName(false) + ".kt")
        val allContexts = TemplateContextType.EP_NAME.extensions.filter { it is KotlinTemplateContextType }
        val enabledContexts = allContexts.filter { it.isInContext(myFixture.file, myFixture.caretOffset) }.map { it.javaClass }
        UsefulTestCase.assertSameElements(enabledContexts, *expectedContexts)
    }
}
