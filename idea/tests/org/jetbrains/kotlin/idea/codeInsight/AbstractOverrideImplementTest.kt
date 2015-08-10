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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementMethodsHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideImplementMethodsHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMethodsHandler
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.test.TagsTestDataUtil
import org.jetbrains.kotlin.utils.rethrow
import org.junit.Assert
import java.io.File
import kotlin.test.assertEquals

public abstract class AbstractOverrideImplementTest : JetLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = JetLightProjectDescriptor.INSTANCE

    protected fun doImplementFileTest() {
        doFileTest(ImplementMethodsHandler())
    }

    protected fun doOverrideFileTest() {
        doFileTest(OverrideMethodsHandler())
    }

    protected fun doMultiImplementFileTest() {
        doMultiFileTest(ImplementMethodsHandler())
    }

    protected fun doMultiOverrideFileTest() {
        doMultiFileTest(OverrideMethodsHandler())
    }

    protected fun doImplementDirectoryTest() {
        doDirectoryTest(ImplementMethodsHandler())
    }

    protected fun doOverrideDirectoryTest(memberToImplement: String?) {
        doDirectoryTest(OverrideMethodsHandler(), memberToImplement)
    }

    protected fun doImplementJavaDirectoryTest(className: String, methodName: String) {
        myFixture.copyDirectoryToProject(getTestName(true), "")
        myFixture.configureFromTempProjectFile("foo/JavaClass.java")

        val project = myFixture.project

        val aClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
                     ?: error("Can't find class: $className")

        val method = aClass.findMethodsByName(methodName, false)[0]
                     ?: error("Can't find method '$methodName' in class $className")

        generateImplementation(method)

        myFixture.checkResultByFile(getTestName(true) + "/foo/JavaClass.java.after")
    }

    private fun doFileTest(handler: OverrideImplementMethodsHandler) {
        myFixture.configureByFile(getTestName(true) + ".kt")
        doOverrideImplement(handler, null)
        checkResultByFile(getTestName(true) + ".kt.after")
    }

    private fun doMultiFileTest(handler: OverrideImplementMethodsHandler) {
        myFixture.configureByFile(getTestName(true) + ".kt")
        doMultiOverrideImplement(handler)
        checkResultByFile(getTestName(true) + ".kt.after")
    }

    protected fun doDirectoryTest(handler: OverrideImplementMethodsHandler) {
        doDirectoryTest(handler, null)
    }

    private fun doDirectoryTest(handler: OverrideImplementMethodsHandler, memberToOverride: String?) {
        myFixture.copyDirectoryToProject(getTestName(true), "")
        myFixture.configureFromTempProjectFile("foo/Impl.kt")
        doOverrideImplement(handler, memberToOverride)
        checkResultByFile(getTestName(true) + "/foo/Impl.kt.after")
    }

    private fun doOverrideImplement(handler: OverrideImplementMethodsHandler, memberToOverride: String?) {
        val elementAtCaret = myFixture.file.findElementAt(myFixture.editor.caretModel.offset)
        val classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, javaClass<JetClassOrObject>())
                            ?: error("Caret should be inside class or object")

        val chooserObjects = handler.collectMethodsToGenerate(classOrObject)

        val singleToOverride = if (memberToOverride == null) {
            val filtered = chooserObjects.filter { it.descriptor.containingDeclaration != KotlinBuiltIns.getInstance().any }
            assertEquals(1, filtered.size(), "Invalid number of available chooserObjects for override")
            filtered.single()
        }
        else {
            var candidateToOverride: OverrideMemberChooserObject? = null
            for (chooserObject in chooserObjects) {
                if (chooserObject.descriptor.name.asString() == memberToOverride) {
                    if (candidateToOverride != null) {
                        throw IllegalStateException("more then one descriptor with name $memberToOverride")
                    }
                    candidateToOverride = chooserObject
                }
            }
            if (candidateToOverride == null) {
                throw IllegalStateException("no chooserObjects to override with name $memberToOverride found")
            }
            candidateToOverride!!
        }

        performGenerateCommand(classOrObject, listOf(singleToOverride))
    }

    private fun doMultiOverrideImplement(handler: OverrideImplementMethodsHandler) {
        val elementAtCaret = myFixture.file.findElementAt(myFixture.editor.caretModel.offset)
        val classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, javaClass<JetClassOrObject>())
                            ?: error("Caret should be inside class or object")

        val chooserObjects = handler.collectMethodsToGenerate(classOrObject).sortBy { it.descriptor.name.asString() + " in " + it.immediateSuper.containingDeclaration.name.asString() }
        performGenerateCommand(classOrObject, chooserObjects)
    }

    private fun generateImplementation(method: PsiMethod) {
        getProject().executeWriteCommand("") {
            val aClass = (myFixture.file as PsiClassOwner).classes[0]

            val methodMember = PsiMethodMember(method, PsiSubstitutor.EMPTY)

            OverrideImplementUtil.overrideOrImplementMethodsInRightPlace(myFixture.editor, aClass, SmartList(methodMember), false)

            PostprocessReformattingAspect.getInstance(myFixture.project).doPostponedFormatting()
        }
    }

    private fun performGenerateCommand(
            classOrObject: JetClassOrObject,
            selectedElements: List<OverrideMemberChooserObject>) {
        try {
            myFixture.project.executeWriteCommand("") {
                OverrideImplementMethodsHandler.generateMethods(myFixture.editor, classOrObject, selectedElements)
            }
        }
        catch (throwable: Throwable) {
            throw rethrow(throwable)
        }

    }

    private fun checkResultByFile(fileName: String) {
        val expectedFile = File(myFixture.testDataPath, fileName)
        try {
            Assert.assertTrue(expectedFile.exists())
            myFixture.checkResultByFile(fileName)
        }
        catch (error: AssertionError) {
            JetTestUtils.assertEqualsToFile(expectedFile, TagsTestDataUtil.generateTextWithCaretAndSelection(myFixture.editor))
        }
    }
}
