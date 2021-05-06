/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.generation.ClassMember
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
import org.jetbrains.kotlin.idea.core.overrideImplement.AbstractGenerateMembersHandler
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.dumpErrorLines
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TagsTestDataUtil
import org.jetbrains.kotlin.test.utils.IgnoreTests
import org.jetbrains.kotlin.utils.rethrow
import org.junit.Assert
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

abstract class AbstractOverrideImplementTest<T : ClassMember> : KotlinLightCodeInsightFixtureTestCase(), OverrideImplementTestMixIn<T> {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected fun doImplementFileTest(memberToOverride: String? = null) {
        doFileTest(createImplementMembersHandler(), memberToOverride)
    }

    protected fun doOverrideFileTest(memberToOverride: String? = null) {
        doFileTest(createOverrideMembersHandler(), memberToOverride)
    }

    protected fun doMultiImplementFileTest() {
        doMultiFileTest(createImplementMembersHandler())
    }

    protected fun doMultiOverrideFileTest() {
        doMultiFileTest(createOverrideMembersHandler())
    }

    protected fun doImplementDirectoryTest(memberToOverride: String? = null) {
        doDirectoryTest(createImplementMembersHandler(), memberToOverride)
    }

    protected fun doOverrideDirectoryTest(memberToImplement: String? = null) {
        doDirectoryTest(createOverrideMembersHandler(), memberToImplement)
    }

    protected fun doMultiImplementDirectoryTest() {
        doMultiDirectoryTest(createImplementMembersHandler())
    }

    protected fun doMultiOverrideDirectoryTest() {
        doMultiDirectoryTest(createOverrideMembersHandler())
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

    private fun doFileTest(handler: AbstractGenerateMembersHandler<T>, memberToOverride: String? = null) {
        myFixture.configureByFile(getTestName(true) + ".kt")
        doOverrideImplement(handler, memberToOverride)
        checkResultByFile(getTestName(true))
    }

    private fun doMultiFileTest(handler: AbstractGenerateMembersHandler<T>) {
        myFixture.configureByFile(getTestName(true) + ".kt")
        doMultiOverrideImplement(handler)
        checkResultByFile(getTestName(true))
    }

    private fun doDirectoryTest(handler: AbstractGenerateMembersHandler<T>, memberToOverride: String? = null) {
        myFixture.copyDirectoryToProject(getTestName(true), "")
        myFixture.configureFromTempProjectFile("foo/Impl.kt")
        doOverrideImplement(handler, memberToOverride)
        checkResultByFile(getTestName(true) + "/foo/Impl")
    }

    private fun doMultiDirectoryTest(handler: AbstractGenerateMembersHandler<T>) {
        myFixture.copyDirectoryToProject(getTestName(true), "")
        myFixture.configureFromTempProjectFile("foo/Impl.kt")
        doMultiOverrideImplement(handler)
        checkResultByFile(getTestName(true) + "/foo/Impl")
    }

    private fun doOverrideImplement(handler: AbstractGenerateMembersHandler<T>, memberToOverride: String?) {
        val elementAtCaret = myFixture.file.findElementAt(myFixture.editor.caretModel.offset)
        val classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, KtClassOrObject::class.java)
            ?: error("Caret should be inside class or object")

        val chooserObjects = handler.collectMembersToGenerate(classOrObject)

        val singleToOverride = if (memberToOverride == null) {
            val filtered = chooserObjects.filter { !isMemberOfAny(classOrObject, it) }
            assertEquals(1, filtered.size, "Invalid number of available chooserObjects for override")
            filtered.single()
        } else {
            chooserObjects.single { chooserObject ->
                getMemberName(classOrObject, chooserObject) == memberToOverride
            }
        }

        performGenerateCommand(handler, classOrObject, listOf(singleToOverride))
    }

    private fun doMultiOverrideImplement(handler: AbstractGenerateMembersHandler<T>) {
        val elementAtCaret = myFixture.file.findElementAt(myFixture.editor.caretModel.offset)
        val classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, KtClassOrObject::class.java)
            ?: error("Caret should be inside class or object")

        val chooserObjects = handler.collectMembersToGenerate(classOrObject)
            .sortedBy { getMemberName(classOrObject, it) + " in " + getContainingClassName(classOrObject, it) }
        performGenerateCommand(handler, classOrObject, chooserObjects)
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
        handler: AbstractGenerateMembersHandler<T>,
        classOrObject: KtClassOrObject,
        selectedElements: List<T>
    ) {
        try {
            val copyDoc = InTextDirectivesUtils.isDirectiveDefined(classOrObject.containingFile.text, "// COPY_DOC")
            myFixture.project.executeWriteCommand("") {
                handler.generateMembers(myFixture.editor, classOrObject, selectedElements, copyDoc)
            }
        } catch (throwable: Throwable) {
            throw rethrow(throwable)
        }

    }

    private fun checkResultByFile(fileNameWithoutExtension: String) {
        val goldenResultFile = File(myFixture.testDataPath, "$fileNameWithoutExtension.kt.after")
        val firIdenticalIsPresent = InTextDirectivesUtils.isDirectiveDefined(
            goldenResultFile.readText(StandardCharsets.UTF_8),
            IgnoreTests.DIRECTIVES.FIR_IDENTICAL
        )

        val resultFile = if (isFirPlugin) {
            if (InTextDirectivesUtils.isDirectiveDefined(
                    goldenResultFile.readText(StandardCharsets.UTF_8),
                    IgnoreTests.DIRECTIVES.IGNORE_FIR
                )
            ) {
                return
            }

            if (firIdenticalIsPresent) {
                goldenResultFile
            } else {
                val firResultFile = File(myFixture.testDataPath, "$fileNameWithoutExtension.kt.fir.after")
                if (!firResultFile.exists()) {
                    goldenResultFile.copyTo(firResultFile)
                }
                firResultFile
            }
        } else {
            goldenResultFile
        }
        try {
            Assert.assertTrue(resultFile.exists())
            val errorLines = myFixture.dumpErrorLines()
            val file = myFixture.file as KtFile
            val fileLines = file.text.lines()
            val newTextContent = if (firIdenticalIsPresent) {
                // Ensure the directive is the first line
                val fileLinesWithoutFirIdentical = fileLines.filter { it != IgnoreTests.DIRECTIVES.FIR_IDENTICAL }
                (listOf(IgnoreTests.DIRECTIVES.FIR_IDENTICAL) + errorLines + fileLinesWithoutFirIdentical).joinToString("\n")
            } else {
                (errorLines + fileLines).joinToString("\n")
            }
            myFixture.project.executeWriteCommand("") {
                val document = myFixture.getDocument(file)
                document.replaceString(0, document.textLength, newTextContent)
            }
            myFixture.checkResultByFile(resultFile.relativeTo(File(myFixture.testDataPath)).path)
        } catch (error: AssertionError) {
            KotlinTestUtils.assertEqualsToFile(resultFile, TagsTestDataUtil.generateTextWithCaretAndSelection(myFixture.editor))
        }
        if (resultFile != goldenResultFile) {
            IgnoreTests.cleanUpIdenticalFirTestFile(
                goldenResultFile,
                resultFile,
                File(myFixture.testDataPath, "$fileNameWithoutExtension.kt"),
            )
        }
    }
}
