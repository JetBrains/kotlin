/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment

class KtCodeFragmentTest : KotlinTestWithEnvironment() {
    fun testSingleImportDirectiveWithoutContext() {
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", "lib.foo", context = null)

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(emptyList<String>(), textImports)
    }

    fun testSingleImportDirective() {
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", "lib.foo", context)

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.foo"), textImports)
    }

    fun testSingleImportDirectiveExplicitImportKeyword() {
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", "import lib.foo", context)

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.foo"), textImports)
    }

    fun testMultipleImportDirectives() {
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val importString = "lib.foo" + KtCodeFragment.IMPORT_SEPARATOR + "lib.bar"
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", importString, context)

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.foo", "import lib.bar"), textImports)
    }

    fun testMultipleImportDirectives2() {
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val importString = "lib.bar" + KtCodeFragment.IMPORT_SEPARATOR + "lib.foo"
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", importString, context)

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.bar", "import lib.foo"), textImports)
    }

    fun testMultipleImportDirectivesAdding() {
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", "lib.foo", context)
        codeFragment.addImportsFromString("lib.bar")
        codeFragment.addImportsFromString("import lib.baz")

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.foo", "import lib.bar", "import lib.baz"), textImports)
    }

    fun testClone() {
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", "import lib.foo", context)
        val codeFragmentClone = codeFragment.copy() as KtCodeFragment

        codeFragment.addImportsFromString("lib.bar")
        codeFragmentClone.addImportsFromString("lib.baz")

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.foo", "import lib.bar"), textImports)

        val textImportsClone = codeFragmentClone.importDirectives.map { it.text }
        assertEquals(listOf("import lib.foo", "import lib.baz"), textImportsClone)
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(
            testRootDisposable,
            KotlinTestUtils.newConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }
}
