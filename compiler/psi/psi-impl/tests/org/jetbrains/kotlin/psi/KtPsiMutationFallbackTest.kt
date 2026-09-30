/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockProject
import com.intellij.pom.PomModel
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Checks that Kotlin PSI overrides of platform mutation methods fall back to plain operations when [KtPsiMutationService] is not
 * registered, which is the case outside the IntelliJ Kotlin plugin.
 */
class KtPsiMutationFallbackTest : KotlinTestWithEnvironment() {
    /**
     * Makes the PSI of the [KotlinTestWithEnvironment] environment modifiable, which it isn't out of the box: PSI modifications are
     * dispatched through the [PomModel] and its [TreeAspect], and inserting an element from another tree, e.g., one created by
     * [KtPsiFactory], requires the [TreeCopyHandler] extension point.
     *
     * The extension point is registered in the application, which is shared between tests running in parallel, hence the lock.
     */
    @BeforeEach
    @Suppress("UnstableApiUsage")
    fun setUp() {
        synchronized(lock) {
            CoreApplicationEnvironment.registerApplicationDynamicExtensionPoint(TreeCopyHandler.EP_NAME.name, TreeCopyHandler::class.java)
        }

        val mockProject = project as MockProject
        mockProject.registerService(PomModel::class.java, PomModelImpl::class.java)
        mockProject.registerService(TreeAspect::class.java)
    }

    @Test
    fun testServiceIsNotRegistered() {
        Assertions.assertNull(KtPsiMutationService.getInstanceOrNull())
    }

    @Test
    fun testDeleteStubBasedElement() {
        val file = createFile("import a.B\nimport c.D\n")
        file.importDirectives.first().delete()
        Assertions.assertEquals("\nimport c.D\n", file.text)
    }

    @Test
    fun testDeleteNonStubElement() {
        val file = createFile("fun f() { if (c) a(); b() }")
        file.findDescendantOfType<KtIfExpression>()!!.delete()
        Assertions.assertEquals("fun f() { ; b() }", file.text)
    }

    @Test
    fun testDeleteBlockExpression() {
        val file = createFile("fun f() { a() }")
        file.findDescendantOfType<KtNamedFunction>()!!.bodyBlockExpression!!.delete()
        Assertions.assertEquals("fun f() ", file.text)
    }

    @Test
    fun testDeleteClass() {
        val file = createFile("class A\nclass B\n")
        file.findDescendantOfType<KtClass> { it.name == "A" }!!.delete()
        Assertions.assertEquals("\nclass B\n", file.text)
    }

    @Test
    fun testDeleteEnumEntry() {
        val file = createFile("enum class E { A, B }")
        file.findDescendantOfType<KtEnumEntry> { it.name == "B" }!!.delete()
        Assertions.assertEquals("enum class E { A,  }", file.text)
    }

    @Test
    fun testDeleteSuperTypeList() {
        val file = createFile("class A : B")
        file.findDescendantOfType<KtSuperTypeList>()!!.delete()
        Assertions.assertEquals("class A : ", file.text)
    }

    @Test
    fun testReplaceStubBasedExpression() {
        val file = createFile("val v = a * b")
        val reference = file.findDescendantOfType<KtNameReferenceExpression> { it.getReferencedName() == "a" }!!
        reference.replace(KtPsiFactory(project).createExpression("x + y"))
        Assertions.assertEquals("val v = x + y * b", file.text)
    }

    @Test
    fun testReplaceNonStubExpression() {
        val file = createFile("val v = if (c) 1 else 2")
        file.findDescendantOfType<KtIfExpression>()!!.replace(KtPsiFactory(project).createExpression("3"))
        Assertions.assertEquals("val v = 3", file.text)
    }

    @Test
    fun testSetNameOfStubBasedDeclaration() {
        val file = createFile("fun foo() {}")
        file.findDescendantOfType<KtNamedFunction>()!!.setName("class")
        Assertions.assertEquals("fun `class`() {}", file.text)
    }

    @Test
    fun testSetNameOfNonStubDeclaration() {
        val file = createFile("val (a, b) = p")
        file.findDescendantOfType<KtDestructuringDeclarationEntry> { it.name == "a" }!!.setName("c")
        Assertions.assertEquals("val (c, b) = p", file.text)
    }

    @Test
    fun testSetNameOfUnnamedObjectDeclaration() {
        val file = createFile("class A { companion object {} }")
        file.findDescendantOfType<KtObjectDeclaration>()!!.setName("Named")
        Assertions.assertEquals("class A { companion object Named {} }", file.text)
    }

    @Test
    fun testSetNameOfImportAlias() {
        val file = createFile("import a.B as C")
        file.findDescendantOfType<KtImportAlias>()!!.setName("D")
        Assertions.assertEquals("import a.B as D", file.text)
    }

    @Test
    fun testSetNameOfLabeledExpression() {
        val file = createFile("fun f() { foo@ for (i in x) {} }")
        file.findDescendantOfType<KtLabeledExpression>()!!.setName("bar")
        Assertions.assertEquals("fun f() { bar@ for (i in x) {} }", file.text)
    }

    @Test
    fun testSetNameOfFile() {
        val file = createFile("class A")
        file.setName("renamed.kt")
        Assertions.assertEquals("renamed.kt", file.name)
    }

    @Test
    fun testUpdateTextOfStringTemplate() {
        val file = createFile("val s = \"a\"")
        file.findDescendantOfType<KtStringTemplateExpression>()!!.updateText("\"b\"")
        Assertions.assertEquals("val s = \"b\"", file.text)
    }

    @Test
    fun testUpdateTextOfKDocSection() {
        val file = createFile("/** a */\nfun f() {}")
        val section = file.findDescendantOfType<KDocSection>()!!.updateText("b")
        Assertions.assertEquals("b", section.text.trim())
    }

    private fun createFile(text: String): KtFile = KtPsiFactory(project).createFile(text)

    private companion object {
        private val lock = Any()
    }
}
