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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideImplementMembersHandler
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMembersHandler
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.dumpTextWithErrors
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TagsTestDataUtil
import org.jetbrains.kotlin.utils.rethrow
import org.junit.Assert
import java.io.File
import kotlin.test.assertEquals

abstract class AbstractOverrideImplementTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinLightProjectDescriptor.INSTANCE

    protected fun doImplementFileTest(memberToOverride: String? = null) {
        doFileTest(ImplementMembersHandler(), memberToOverride)
    }

    protected fun doOverrideFileTest(memberToOverride: String? = null) {
        doFileTest(OverrideMembersHandler(), memberToOverride)
    }

    protected fun doMultiImplementFileTest() {
        doMultiFileTest(ImplementMembersHandler())
    }

    protected fun doMultiOverrideFileTest() {
        doMultiFileTest(OverrideMembersHandler())
    }

    protected fun doImplementDirectoryTest(memberToOverride: String? = null) {
        doDirectoryTest(ImplementMembersHandler(), memberToOverride)
    }

    protected fun doOverrideDirectoryTest(memberToImplement: String? = null) {
        doDirectoryTest(OverrideMembersHandler(), memberToImplement)
    }

    protected fun doMultiOverrideDirectoryTest() {
        doMultiDirectoryTest(OverrideMembersHandler())
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

    private fun doFileTest(handler: OverrideImplementMembersHandler, memberToOverride: String? = null) {
        myFixture.configureByFile(getTestName(true) + ".kt")
        doOverrideImplement(handler, memberToOverride)
        checkResultByFile(getTestName(true) + ".kt.after")
    }

    private fun doMultiFileTest(handler: OverrideImplementMembersHandler) {
        myFixture.configureByFile(getTestName(true) + ".kt")
        doMultiOverrideImplement(handler)
        checkResultByFile(getTestName(true) + ".kt.after")
    }

    private fun doDirectoryTest(handler: OverrideImplementMembersHandler, memberToOverride: String? = null) {
        myFixture.copyDirectoryToProject(getTestName(true), "")
        myFixture.configureFromTempProjectFile("foo/Impl.kt")
        doOverrideImplement(handler, memberToOverride)
        checkResultByFile(getTestName(true) + "/foo/Impl.kt.after")
    }

    private fun doMultiDirectoryTest(handler: OverrideImplementMembersHandler) {
        myFixture.copyDirectoryToProject(getTestName(true), "")
        myFixture.configureFromTempProjectFile("foo/Impl.kt")
        doMultiOverrideImplement(handler)
        checkResultByFile(getTestName(true) + "/foo/Impl.kt.after")
    }

    private fun doOverrideImplement(handler: OverrideImplementMembersHandler, memberToOverride: String?) {
        val elementAtCaret = myFixture.file.findElementAt(myFixture.editor.caretModel.offset)
        val classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, KtClassOrObject::class.java)
                            ?: error("Caret should be inside class or object")

        val chooserObjects = handler.collectMembersToGenerate(classOrObject)

        val singleToOverride = if (memberToOverride == null) {
            val filtered = chooserObjects.filter {
                (it.descriptor.containingDeclaration as? ClassDescriptor)?.let {
                    !KotlinBuiltIns.isAny(it)
                } ?: true
            }
            assertEquals(1, filtered.size, "Invalid number of available chooserObjects for override")
            filtered.single()
        }
        else {
            chooserObjects.single {
                chooserObject ->
                chooserObject.descriptor.name.asString() == memberToOverride
            }
        }

        performGenerateCommand(classOrObject, listOf(singleToOverride))
    }

    private fun doMultiOverrideImplement(handler: OverrideImplementMembersHandler) {
        val elementAtCaret = myFixture.file.findElementAt(myFixture.editor.caretModel.offset)
        val classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, KtClassOrObject::class.java)
                            ?: error("Caret should be inside class or object")

        val chooserObjects = handler.collectMembersToGenerate(classOrObject).sortedBy { it.descriptor.name.asString() + " in " + it.immediateSuper.containingDeclaration.name.asString() }
        performGenerateCommand(classOrObject, chooserObjects)
    }

    private fun generateImplementation(method: PsiMethod) {
        project.executeWriteCommand("") {
            val aClass = (myFixture.file as PsiClassOwner).classes[0]

            val methodMember = PsiMethodMember(method, PsiSubstitutor.EMPTY)

            OverrideImplementUtil.overrideOrImplementMethodsInRightPlace(myFixture.editor, aClass, SmartList(methodMember), false)

            PostprocessReformattingAspect.getInstance(myFixture.project).doPostponedFormatting()
        }
    }

    private fun performGenerateCommand(
            classOrObject: KtClassOrObject,
            selectedElements: List<OverrideMemberChooserObject>) {
        try {
            val copyDoc = InTextDirectivesUtils.isDirectiveDefined(classOrObject.containingFile.text, "// COPY_DOC")
            myFixture.project.executeWriteCommand("") {
                OverrideImplementMembersHandler.generateMembers(myFixture.editor, classOrObject, selectedElements, copyDoc)
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
            val file = myFixture.file as KtFile
            val document = myFixture.getDocument(file)
            myFixture.project.executeWriteCommand("") {
                document.replaceString(0, document.textLength, file.dumpTextWithErrors())
            }
            myFixture.checkResultByFile(fileName)
        }
        catch (error: AssertionError) {
            KotlinTestUtils.assertEqualsToFile(expectedFile, TagsTestDataUtil.generateTextWithCaretAndSelection(myFixture.editor))
        }
    }
}
