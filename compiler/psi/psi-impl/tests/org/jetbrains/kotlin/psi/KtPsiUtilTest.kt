/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.isCallee
import org.jetbrains.kotlin.psi.psiUtil.isSingleQuoted
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class KtPsiUtilTest(private val testInfo: TestInfo) : KotlinTestWithEnvironment() {
    @Test
    fun testUnquotedIdentifier() {
        Assertions.assertEquals("", KtPsiUtil.unquoteIdentifier(""))
        Assertions.assertEquals("a2", KtPsiUtil.unquoteIdentifier("a2"))
        Assertions.assertEquals("", KtPsiUtil.unquoteIdentifier("``"))
        Assertions.assertEquals("a2", KtPsiUtil.unquoteIdentifier("`a2`"))
    }

    @Test
    fun testUnquotedIdentifierOrFieldReference() {
        Assertions.assertEquals("", KtPsiUtil.unquoteIdentifierOrFieldReference(""))
        Assertions.assertEquals("a2", KtPsiUtil.unquoteIdentifierOrFieldReference("a2"))
        Assertions.assertEquals("", KtPsiUtil.unquoteIdentifierOrFieldReference("``"))
        Assertions.assertEquals("a2", KtPsiUtil.unquoteIdentifierOrFieldReference("`a2`"))
        Assertions.assertEquals($$"$a2", KtPsiUtil.unquoteIdentifierOrFieldReference($$"$a2"))
        Assertions.assertEquals($$"$a2", KtPsiUtil.unquoteIdentifierOrFieldReference($$"$`a2`"))
    }

    @Test
    fun testConvertToImportPath() {
        Assertions.assertNull(getImportPathFromParsed("import "))
        Assertions.assertEquals(ImportPath(FqName("some"), false), getImportPathFromParsed("import some."))
        Assertions.assertNull(getImportPathFromParsed("import *"))
        Assertions.assertEquals(ImportPath(FqName("some.test"), true), getImportPathFromParsed("import some.test.* as SomeTest"))
        Assertions.assertEquals(ImportPath(FqName("some"), false), getImportPathFromParsed("import some?.Test"))

        Assertions.assertEquals(ImportPath(FqName("some"), false), getImportPathFromParsed("import some"))
        Assertions.assertEquals(ImportPath(FqName("some"), true), getImportPathFromParsed("import some.*"))
        Assertions.assertEquals(ImportPath(FqName("some.Test"), false), getImportPathFromParsed("import some.Test"))
        Assertions.assertEquals(ImportPath(FqName("some.test"), true), getImportPathFromParsed("import some.test.*"))
        Assertions.assertEquals(
            ImportPath(FqName("some.test"), false, Name.identifier("SomeTest")),
            getImportPathFromParsed("import some.test as SomeTest")
        )

        Assertions.assertEquals(
            ImportPath(FqName("some.Test"), false),
            getImportPathFromParsed("import some./* hello world */Test")
        )
        Assertions.assertEquals(ImportPath(FqName("some.Test"), false), getImportPathFromParsed("import some.    Test"))

        Assertions.assertNotSame(ImportPath(FqName("some.test"), false), getImportPathFromParsed("import some.Test"))
    }

    @Test
    fun testIsSingleQuoted() {
        val factory = KtPsiFactory(project)

        Assertions.assertTrue(factory.createStringTemplate("").isSingleQuoted())
        Assertions.assertTrue(factory.createStringTemplate("foo").isSingleQuoted())
        Assertions.assertTrue(factory.createMultiDollarStringTemplate("bar", 2, false).isSingleQuoted())
        Assertions.assertTrue(factory.createMultiDollarStringTemplate("baz", 5, false).isSingleQuoted())

        Assertions.assertFalse(factory.createMultiDollarStringTemplate("Foo\nBar", 2, false).isSingleQuoted())
        Assertions.assertFalse(factory.createMultiDollarStringTemplate("Foo", 2, true).isSingleQuoted())
    }

    @Test
    fun testPlainContent() {
        val factory = KtPsiFactory(project)
        val singleLineContent = "foo"
        val multiLineContent = "Bar\nBaz"

        Assertions.assertEquals("", factory.createStringTemplate("").plainContent)
        Assertions.assertEquals(singleLineContent, factory.createStringTemplate(singleLineContent).plainContent)
        Assertions.assertEquals(
            singleLineContent,
            factory.createMultiDollarStringTemplate(singleLineContent, 2, false).plainContent
        )
        Assertions.assertEquals(
            singleLineContent,
            factory.createMultiDollarStringTemplate(singleLineContent, 5, false).plainContent
        )

        Assertions.assertEquals(
            multiLineContent,
            factory.createMultiDollarStringTemplate(multiLineContent, 2, false).plainContent
        )
        Assertions.assertEquals(
            singleLineContent,
            factory.createMultiDollarStringTemplate(singleLineContent, 2, true).plainContent
        )
    }

    @Test
    fun testIsLocalClass() {
        val text = FileUtil.loadFile(
            /* file = */ ForTestCompileRuntime.transformTestDataPath("compiler/psi/psi-impl/testData/psiUtil/isLocalClass.kt"),
            /* convertLineSeparators = */ true
        )
        val aClass = KtPsiFactory(project).createClass(text)
        val classOrObjects = aClass.collectDescendantsOfType<KtClassOrObject>()

        for (classOrObject in classOrObjects) {
            val classOrObjectName = classOrObject.name
            if (classOrObjectName != null && classOrObjectName.contains("Local")) {
                Assertions.assertTrue(KtPsiUtil.isLocal(classOrObject)) { "KtPsiUtil.isLocal should return true for $classOrObjectName" }
            } else {
                Assertions.assertFalse(KtPsiUtil.isLocal(classOrObject)) { "KtPsiUtil.isLocal should return false for $classOrObjectName" }
            }
        }
    }

    @Test
    fun testIsSelectorInExpression() {
        checkIsSelectorInQualified()
    }

    @Test
    fun testIsSelectorInType() {
        checkIsSelectorInQualified()
    }

    @Test
    fun testIsCallee() {
        checkExpression(KtSimpleNameExpression::isCallee)
    }

    @Test
    fun testGetOutermostClassOrObjectForCompanionBlockMember() {
        val file = KtPsiFactory(project).createFile(
            """
            class C {
                companion {
                    class Nested
                    object NestedObject
                }
            }
            """.trimIndent()
        )

        val topLevel = file.getChildOfType<KtClass>()
        checkNotNull(topLevel) { "Top-level class C is expected" }

        for (memberName in listOf("Nested", "NestedObject")) {
            val member = file.findClassOrObjectByName(memberName)
            Assertions.assertSame(
                topLevel,
                KtPsiUtil.getOutermostClassOrObject(member),
                "getOutermostClassOrObject of companion-block member $memberName must be the enclosing class C"
            )
        }
    }

    private fun KtFile.findClassOrObjectByName(name: String): KtClassOrObject =
        findDescendantOfType<KtClassOrObject> { it.name == name } ?: error("Class or object '$name' is not found")

    private fun getImportPathFromParsed(text: String): ImportPath? {
        val importDirective = KtPsiFactory(project).createFile(text).findDescendantOfType<KtImportDirective>()
        checkNotNull(importDirective) { "At least one import directive is expected" }

        return importDirective.importPath
    }

    private fun checkIsSelectorInQualified() {
        checkExpression(KtPsiUtil::isSelectorInQualified)
    }

    private fun checkExpression(checkedFunction: (KtSimpleNameExpression) -> Boolean) {
        val trueResultString = "/*true*/"
        val falseResultString = "/*false*/"

        val testName = testInfo.testMethod.get().name
        val name = KtUsefulTestCase.getTestName(testName, true) + ".kt"
        val fileText = KtTestUtil.doLoadFile(ForTestCompileRuntime.transformTestDataPath("compiler/psi/psi-impl/testData/psiUtil/$name"))
        val file = KtTestUtil.createFile(name, fileText, project)

        val text = file.text

        // /*true*/|/*false*/
        val regex = Regex("${Regex.escape(trueResultString)}|${Regex.escape(falseResultString)}")

        for (match in regex.findAll(text)) {
            val expected = match.value == trueResultString
            val offset = match.range.last + 1
            val expression =
                PsiTreeUtil.findElementOfClassAtOffset(file, offset, KtSimpleNameExpression::class.java, true)!!
            val modifiedWithOffset = text.replaceRange(offset, offset, "<======caret======>")

            Assertions.assertSame(
                expected,
                checkedFunction(expression),
                "$expected result was expected at\n$modifiedWithOffset"
            )
        }
    }
}
