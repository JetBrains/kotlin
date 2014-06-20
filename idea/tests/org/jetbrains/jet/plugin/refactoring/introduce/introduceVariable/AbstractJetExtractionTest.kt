/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.introduce.introduceVariable

import com.intellij.ide.DataManager
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.jet.JetTestCaseBuilder
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.refactoring.extractFunction.ExtractKotlinFunctionHandler
import java.io.File
import org.jetbrains.jet.plugin.refactoring.extractFunction.selectElements
import org.jetbrains.jet.lang.psi.JetTreeVisitorVoid
import com.intellij.psi.PsiComment
import com.intellij.refactoring.BaseRefactoringProcessor.ConflictsInTestsException
import org.jetbrains.jet.JetTestUtils
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.jet.lang.psi.JetPackageDirective
import org.jetbrains.jet.InTextDirectivesUtils
import org.jetbrains.jet.renderer.DescriptorRenderer
import java.util.ArrayList
import com.intellij.util.containers.ContainerUtil
import kotlin.test.assertEquals

public abstract class AbstractJetExtractionTest() : LightCodeInsightFixtureTestCase() {
    val fixture: JavaCodeInsightTestFixture get() = myFixture

    protected fun doIntroduceVariableTest(path: String) {
        doTest(path) { file ->
            KotlinIntroduceVariableHandler().invoke(
                    fixture.getProject(),
                    fixture.getEditor(),
                    file,
                    DataManager.getInstance().getDataContext(fixture.getEditor().getComponent())
            )
        }
    }

    protected fun doExtractFunctionTest(path: String) {
        doTest(path) { file ->
            var explicitPreviousSibling: PsiElement? = null
            file.accept(
                    object: JetTreeVisitorVoid() {
                        override fun visitComment(comment: PsiComment) {
                            if (comment.getText() == "// SIBLING:") {
                                explicitPreviousSibling = PsiTreeUtil.skipSiblingsForward(
                                        comment,
                                        javaClass<PsiWhiteSpace>(),
                                        javaClass<PsiComment>(),
                                        javaClass<JetPackageDirective>()
                                )
                            }
                        }
                    }
            )

            val expectedParameterTypes = ArrayList<String>()
            val fileText = file.getText()
            val expectedTypes =
                    InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileText, "// PARAM_TYPES: ")
                            .mapTo(expectedParameterTypes) { "[$it]" }
                            .makeString()

            val renderer = DescriptorRenderer.DEBUG_TEXT

            val editor = fixture.getEditor()
            selectElements(editor, file) { (elements, previousSibling) ->
                ExtractKotlinFunctionHandler().doInvoke(editor, file, elements, explicitPreviousSibling ?: previousSibling) {
                    val actualTypes = (ContainerUtil.createMaybeSingletonList(it.receiverParameter) + it.parameters).map {
                        it.parameterTypeCandidates.map { renderer.renderType(it) }. makeString(", ", "[", "]")
                    }.makeString()

                    assertEquals(expectedTypes, actualTypes, "Expected types mismatch.")
                }
            }
        }
    }

    protected fun doTest(path: String, action: (JetFile) -> Unit) {
        val mainFile = File(path)
        val afterFile = File("$path.after")
        val conflictFile = File("$path.conflicts")

        fixture.setTestDataPath("${JetTestCaseBuilder.getHomeDirectory()}/${mainFile.getParent()}")

        val file = fixture.configureByFile(mainFile.getName()) as JetFile

        try {
            action(file)

            assert(!conflictFile.exists())
            JetTestUtils.assertEqualsToFile(afterFile, file.getText()!!)
        }
        catch(e: Exception) {
            val message = if (e is ConflictsInTestsException) e.getMessages().sort().makeString(" ") else e.getMessage()
            JetTestUtils.assertEqualsToFile(conflictFile, message?.replace("\n", " ") ?: e.javaClass.getName())
        }
    }
}
