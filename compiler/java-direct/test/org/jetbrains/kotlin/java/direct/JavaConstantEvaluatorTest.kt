/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.util.ConstantEvaluator
import org.junit.jupiter.api.Test

class JavaConstantEvaluatorTest : JavaParsingTestBase() {

    /**
     * Regression for the reviewer's concern on `ConstantEvaluator.evaluateReferenceExpression`:
     * a *statically imported* field referenced by its bare name (`import static a.b.C.FIELD;`,
     * then `int x = FIELD;`) must be routed to the cross-language resolver with the importing
     * class qualifier and member name, not as the unqualified `(null, "FIELD")` pair (which the
     * session-backed resolver rejects, leaving the constant unresolved).
     *
     * A parsing-level [createDummyFirSessionForTests] has no `FirSymbolProvider`, so the real
     * cross-language lookup always returns null here. Instead we capture what the evaluator
     * passes to `resolveExternalReference` to prove the static import was resolved and split
     * correctly.
     */
    @Test
    fun testStaticImportedBareFieldRoutedWithResolvedQualifier() {
        val source = """
            package test;
            import static com.example.Constants.MAX;
            class Foo {
                int x = MAX;
            }
        """.trimIndent()

        val parsed = parseSource(source)
        val tree = parsed.tree
        val classNode = tree.getChildren(parsed.root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, parsed.context)

        val refNode = findReferenceExpression(tree, parsed.root, "MAX")
            ?: error("Could not find the 'MAX' reference expression in the parsed field initializer")

        var captured: Pair<String?, String>? = null
        val evaluator = ConstantEvaluator(javaClass) { classQualifier, fieldName ->
            captured = classQualifier to fieldName
            if (classQualifier == "com.example.Constants" && fieldName == "MAX") 42 else null
        }

        val result = evaluator.evaluate(refNode)

        assert(captured == "com.example.Constants" to "MAX") {
            "Statically imported field 'MAX' should reach the external resolver as " +
                    "('com.example.Constants', 'MAX'), but was routed as $captured"
        }
        assert(result == 42) {
            "Expected the statically imported field to evaluate to 42, got $result"
        }
    }

    /**
     * A bare name that is neither a local field nor a static import must still fall back to the
     * unqualified `(null, name)` callback — guarding against a regression where the static-import
     * branch swallows the plain fallback.
     */
    @Test
    fun testBareNameWithoutStaticImportFallsBackToUnqualifiedCallback() {
        val source = """
            package test;
            class Foo {
                int x = MISSING;
            }
        """.trimIndent()

        val parsed = parseSource(source)
        val tree = parsed.tree
        val classNode = tree.getChildren(parsed.root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, parsed.context)

        val refNode = findReferenceExpression(tree, parsed.root, "MISSING")
            ?: error("Could not find the 'MISSING' reference expression in the parsed field initializer")

        var captured: Pair<String?, String>? = null
        val evaluator = ConstantEvaluator(javaClass) { classQualifier, fieldName ->
            captured = classQualifier to fieldName
            null
        }

        evaluator.evaluate(refNode)

        assert(captured == (null to "MISSING")) {
            "A bare name with no static import must reach the resolver as (null, 'MISSING'), got $captured"
        }
    }

    private fun findReferenceExpression(tree: JavaLightTree, node: JavaLightNode, text: String): JavaLightNode? {
        if (tree.getType(node) == JavaSyntaxElementType.REFERENCE_EXPRESSION && tree.getText(node).toString() == text) {
            return node
        }
        for (child in tree.getChildren(node)) {
            findReferenceExpression(tree, child, text)?.let { return it }
        }
        return null
    }
}
