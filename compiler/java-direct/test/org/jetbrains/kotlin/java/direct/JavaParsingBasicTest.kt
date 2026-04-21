/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
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
        assert(javaClass.fqName?.asString() == "com.example.A")
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

    @Test fun testJavaClassWithImport() {
        val source = """
            // FILE: JavaClass.java
            import java.util.concurrent.atomic.*;

            public class JavaClass {
                public String foo(AtomicInteger i) {
                    return "JavaClass";
                }
                public AtomicInteger a = new AtomicInteger(1);
            }
        """.trimIndent()
        val tree = parseJavaToLightTree(source, 0)
        println(tree.dump())
    }

    @Test
    fun testDebugTypeArgumentsAST() {
        val source = """
            import java.util.List;

            public class MyClass {
                public List<String> items;
            }
        """.trimIndent()
        val parsed = parseSource(source)
        val tree = parsed.tree

        fun printTree(node: JavaLightNode, indent: String = "") {
            println("$indent${tree.getType(node)}: '${tree.getText(node).toString().take(50).replace("\n", "\\n")}'")
            for (child in tree.getChildren(node)) {
                printTree(child, "$indent  ")
            }
        }

        val classNode = tree.getChildren(parsed.root).first { tree.getType(it).toString() == "CLASS" }
        val fieldNode = tree.findChildByType(classNode, JavaSyntaxElementType.FIELD)!!
        val typeNode = tree.findChildByType(fieldNode, JavaSyntaxElementType.TYPE)!!

        println("=== TYPE node structure ===")
        printTree(typeNode)
    }

    @Test
    fun testDebugWildcardAST() {
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

        val classNode = tree.getChildren(parsed.root).first { tree.getType(it).toString() == "CLASS" }
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
