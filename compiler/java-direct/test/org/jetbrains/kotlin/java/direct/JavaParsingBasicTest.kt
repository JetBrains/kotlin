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
import org.junit.jupiter.api.Test

class JavaParsingBasicTest : JavaParsingTestBase() {

    @Test
    fun testBasicJavaParsing() {
        val source = "public final class A {}"
        val javaClass = parseFirstClass(source)
        assert(javaClass.name.asString() == "A")
        assert(javaClass.isFinal)
        assert(!javaClass.isAbstract)
        assert(javaClass.visibility.toString() == "public")
    }

    @Test
    fun testAbstractInterface() {
        val source = "interface I {}"
        val javaClass = parseFirstClass(source)
        assert(javaClass.name.asString() == "I")
        assert(javaClass.isInterface)
        assert(javaClass.isAbstract)
    }

    @Test
    fun testPackageAndFqName() {
        val source = """
            package com.example;
            class A {}
        """.trimIndent()
        val javaClass = parseFirstClass(source)
        assert(javaClass.fqName.asString() == "com.example.A")
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
        assert(packageStmt != null) { "Expected PACKAGE_STATEMENT node" }
        val packageName = packageStmt?.let {
            tree.findChildByType(it, JavaSyntaxElementType.JAVA_CODE_REFERENCE)?.let { ref -> tree.getText(ref).toString() }
        }
        assert(packageName == "example") { "Expected 'example', got $packageName" }
    }

    @Test
    fun testPublicClassWithMalformedMembers() {
        val source = """
            package p;
            public class Nameless {
                void () {}
                int ;
            }
        """.trimIndent()
        val parsed = parseSource(source)
        val tree = parsed.tree
        val classNode = tree.getChildrenByType(parsed.root, JavaSyntaxElementType.CLASS)
            .first {
                tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "Nameless"
            }
        val javaClass = JavaClassOverAst(classNode, tree, parsed.context)
        assert(javaClass.visibility.toString() == "public") {
            "Expected public visibility for 'public class Nameless', got ${javaClass.visibility}"
        }
        assert(javaClass.constructors.isEmpty()) {
            "Malformed 'void () {}' should not be treated as a constructor"
        }
        assert(javaClass.hasDefaultConstructor()) {
            "Class with no valid constructors should have a default constructor"
        }
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
        assert(fooTypes.any { it == "QUEST" }) { "foo should have QUEST in: $fooTypes" }

        val barMethod = methods.first {
            tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "bar"
        }
        val barTypeNode = tree.findChildByType(barMethod, JavaSyntaxElementType.TYPE)!!
        val barTypes = collectTypes(barTypeNode)
        assert(barTypes.any { it == "QUEST" }) { "bar should have QUEST in: $barTypes" }

        val bazMethod = methods.first {
            tree.findChildByType(it, JavaSyntaxTokenType.IDENTIFIER)?.let { id -> tree.getText(id).toString() } == "baz"
        }
        val bazTypeNode = tree.findChildByType(bazMethod, JavaSyntaxElementType.TYPE)!!
        val bazTypes = collectTypes(bazTypeNode)
        assert(bazTypes.any { it == "QUEST" }) { "baz should have QUEST in: $bazTypes" }
        assert(bazTypes.any { it == "SUPER_KEYWORD" }) { "baz should have SUPER_KEYWORD in: $bazTypes" }
    }
}
