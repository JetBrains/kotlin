/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class JavaParsingBasicTest : JavaParsingTestBase() {

    @Test
    fun testBasicJavaParsing() {
        val source = "public final class A {}"
        val javaClass = parseFirstClass(source)
        assertEquals("A", javaClass.name.asString())
        assertTrue(javaClass.isFinal)
        assertFalse(javaClass.isAbstract)
        assertEquals("public", javaClass.visibility.toString())
    }

    @Test
    fun testAbstractInterface() {
        val source = "interface I {}"
        val javaClass = parseFirstClass(source)
        assertEquals("I", javaClass.name.asString())
        assertTrue(javaClass.isInterface)
        assertTrue(javaClass.isAbstract)
    }

    @Test
    fun testPackageAndFqName() {
        val source = """
            package com.example;
            class A {}
        """.trimIndent()
        val javaClass = parseFirstClass(source)
        assertEquals("com.example.A", javaClass.fqName.asString())
    }

    @Test
    fun testPackageExtraction() {
        val source = """
            package example;

            public class Hello {
                public void greet() {}
            }
        """.trimIndent()
        val parsed = parseSource(source)
        val tree = parsed.tree

        val packageStmt = tree.findChildByType(parsed.root, JavaSyntaxElementType.PACKAGE_STATEMENT)
        assertNotNull(packageStmt, "Expected PACKAGE_STATEMENT node")
        val packageName =
            tree.findChildByType(packageStmt, JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.let { ref -> tree.getText(ref).toString() }
        assertEquals("example", packageName)
    }

    @Test
    fun testWildcardAST() {
        val source = """
            import java.util.List;

            interface A<T> {
                List<? extends T> foo();
                List<?> bar();
                List<? super T> baz();
            }
        """.trimIndent()
        val parsed = parseSource(source)
        val tree = parsed.tree

        fun collectTypes(node: JavaLightNode): List<String> {
            val result = mutableListOf(tree.getType(node).toString())
            for (child in tree.getChildren(node)) {
                result.addAll(collectTypes(child))
            }
            return result
        }

        val classNode = tree.findChildByType(parsed.root, JavaSyntaxElementType.CLASS)!!
        val methods = tree.getChildrenByType(classNode, JavaSyntaxElementType.METHOD)

        val fooMethod = methods.first {
            tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "foo"
        }
        val fooTypeNode = tree.findChildByType(fooMethod, JavaSyntaxElementType.TYPE)!!
        val fooTypes = collectTypes(fooTypeNode)
        assertTrue(fooTypes.any { it == "QUEST" }, "foo should have QUEST in: $fooTypes")

        val barMethod = methods.first {
            tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "bar"
        }
        val barTypeNode = tree.findChildByType(barMethod, JavaSyntaxElementType.TYPE)!!
        val barTypes = collectTypes(barTypeNode)
        assertTrue(barTypes.any { it == "QUEST" }, "bar should have QUEST in: $barTypes")

        val bazMethod = methods.first {
            tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "baz"
        }
        val bazTypeNode = tree.findChildByType(bazMethod, JavaSyntaxElementType.TYPE)!!
        val bazTypes = collectTypes(bazTypeNode)
        assertTrue(bazTypes.any { it == "QUEST" }, "baz should have QUEST in: $bazTypes")
        assertTrue(bazTypes.any { it == "SUPER_KEYWORD" }, "baz should have SUPER_KEYWORD in: $bazTypes")
    }
}
