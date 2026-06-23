/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.DummyAnalysisApiTestConfigurator
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KtCodeFragmentTest : AbstractAnalysisApiBasedTest() {
    override val configurator: AnalysisApiTestConfigurator get() = DummyAnalysisApiTestConfigurator

    @Test
    fun testSingleImportDirectiveWithoutContext() = runCodeFragmentTest { project ->
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", "lib.foo", context = null)

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(emptyList<String>(), textImports)
    }

    @Test
    fun testSingleImportDirective() = runCodeFragmentTest { project ->
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", "lib.foo", context)

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.foo"), textImports)
    }

    @Test
    fun testSingleImportDirectiveExplicitImportKeyword() = runCodeFragmentTest { project ->
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", "import lib.foo", context)

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.foo"), textImports)
    }

    @Test
    fun testMultipleImportDirectives() = runCodeFragmentTest { project ->
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val importString = "lib.foo" + KtCodeFragment.IMPORT_SEPARATOR + "lib.bar"
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", importString, context)

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.foo", "import lib.bar"), textImports)
    }

    @Test
    fun testMultipleImportDirectives2() = runCodeFragmentTest { project ->
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val importString = "lib.bar" + KtCodeFragment.IMPORT_SEPARATOR + "lib.foo"
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", importString, context)

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.bar", "import lib.foo"), textImports)
    }

    @Test
    fun testMultipleImportDirectivesAdding() = runCodeFragmentTest { project ->
        val context = KtPsiFactory(project).createNameIdentifier("context")
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "foo()", "lib.foo", context)
        codeFragment.addImportsFromString("lib.bar")
        codeFragment.addImportsFromString("import lib.baz")

        val textImports = codeFragment.importDirectives.map { it.text }
        assertEquals(listOf("import lib.foo", "import lib.bar", "import lib.baz"), textImports)
    }

    @Test
    fun testClone() = runCodeFragmentTest { project ->
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

    private fun runCodeFragmentTest(action: (Project) -> Unit) {
        runTest("compiler/psi/psi-impl/testData/codeFragment/context.kt") { testServices ->
            action(testServices.environmentManager.getProject())
        }
    }
}
