/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.util.ConstantEvaluator
import org.jetbrains.kotlin.kmp.tree.LightNode
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class JavaConstantEvaluatorTest : JavaParsingTestBase() {

    /**
     * Regression for the reviewer's concern on `ConstantEvaluator.evaluateReferenceExpression`:
     * a *statically imported* field referenced by its bare name (`import static a.b.C.FIELD;`,
     * then `int x = FIELD;`) must be routed to the cross-language resolver with the importing
     * class qualifier and member name; a bare name alone is never resolved externally.
     *
     * The lookup uses a stub FIR class with a const property, rather than a test-only resolver
     * callback, so it exercises the same cross-language path as production.
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

        val constantsClassId = ClassId.topLevel(FqName("com.example.Constants"))
        val session = createSessionWithSymbols(
            mapOf(constantsClassId to libraryClassSymbol(constantsClassId, constProperties = mapOf("MAX" to 42)))
        )
        val parsed = parseSource(source, session)
        val tree = parsed.tree
        val classNode = tree.getChildren(parsed.root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, parsed.context)

        val refNode = findReferenceExpression(tree, parsed.root, "MAX")
            ?: error("Could not find the 'MAX' reference expression in the parsed field initializer")

        assertEquals(42, ConstantEvaluator(javaClass).evaluate(refNode))
    }

    /**
     * A bare name that is neither a field in scope nor statically imported has no qualifier to
     * resolve it in another language, so it stays unresolved without an external lookup.
     */
    @Test
    fun testBareNameWithoutStaticImportIsNotResolvedExternally() {
        val source = """
            package test;
            class Foo {
                int x = MISSING;
            }
        """.trimIndent()

        val constantsClassId = ClassId.topLevel(FqName("com.example.Constants"))
        val session = createSessionWithSymbols(
            mapOf(constantsClassId to libraryClassSymbol(constantsClassId, constProperties = mapOf("MISSING" to 42)))
        )
        val parsed = parseSource(source, session)
        val tree = parsed.tree
        val classNode = tree.getChildren(parsed.root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, parsed.context)

        val refNode = findReferenceExpression(tree, parsed.root, "MISSING")
            ?: error("Could not find the 'MISSING' reference expression in the parsed field initializer")

        assertEquals(null, ConstantEvaluator(javaClass).evaluate(refNode))
    }

    /**
     * A cast to a primitive type is part of a constant expression (JLS 15.29) and performs the
     * narrowing conversion of JLS 5.4, so `(byte) 300` is the constant `44` — not "no constant".
     * `byte` and `short` constants are usually written with such a cast, which is why they were the
     * two types whose initializers went missing while all the others were read correctly.
     */
    @Test
    fun testCastInConstantInitializer() {
        val source = """
            package test;
            class Consts {
                static final int LOCAL = 3;
                static final byte BYTE = (byte) 300;
                static final short SHORT = (short) 70000;
                static final char CHAR = (char) 65;
                static final int TRUNCATED = (int) 2.75;
                static final byte OF_EXPRESSION = (byte) (LOCAL * 100);
            }
        """.trimIndent()

        val javaClass = parseFirstClass(source)
        fun constantOf(name: String): Any? {
            val field = javaClass.fields.first { it.name.asString() == name }
            assertTrue(field.hasConstantNotNullInitializer) { "$name must be a compile-time constant" }
            return field.initializerValue
        }

        assertEquals(44.toByte(), constantOf("BYTE"))
        assertEquals(4464.toShort(), constantOf("SHORT"))
        assertEquals('A', constantOf("CHAR"))
        assertEquals(2, constantOf("TRUNCATED"))
        assertEquals(44.toByte(), constantOf("OF_EXPRESSION"))
    }

    @Test
    fun testTextBlockConstantInitializer() {
        val textBlock =
            "\"\"\"\n                first\n                second\\s   \n                third\\\n                  fourth\n                \"\"\""
        val javaClass = parseFirstClass("class Foo { static final String TEXT = $textBlock; }")
        val field = javaClass.fields.first { it.name.asString() == "TEXT" }

        assertTrue(field.hasConstantNotNullInitializer)
        assertEquals("first\nsecond \nthird  fourth\n", field.initializerValue)
    }

    private fun findReferenceExpression(tree: LightSyntaxTree, node: LightNode, text: String): LightNode? {
        if (tree.getType(node) == JavaSyntaxElementType.REFERENCE_EXPRESSION && tree.getText(node).toString() == text) {
            return node
        }
        for (child in tree.getChildren(node)) {
            findReferenceExpression(tree, child, text)?.let { return it }
        }
        return null
    }
}
