/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

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
        val (root, _) = parseSource(source)

        val packageStmt = root.findChildByType("PACKAGE_STATEMENT")
        assert(packageStmt != null) { "Expected PACKAGE_STATEMENT node" }
        val packageName = packageStmt?.findChildByType("JAVA_CODE_REFERENCE")?.text
        assert(packageName == "example") { "Expected 'example', got $packageName" }
    }

    @Test
    fun testPublicClassWithMalformedMembers() {
        // Regression: public class with syntactically invalid members (nameless method/field)
        // must have public visibility and a default constructor.
        // Previously, `void () {}` was treated as a constructor (no TYPE node in the AST)
        // with no IDENTIFIER, making hasDefaultConstructor() = false and causing
        // INVISIBLE_REFERENCE in FIR when the class was used as a supertype.
        val source = """
            package p;
            public class Nameless {
                void () {}
                int ;
            }
        """.trimIndent()
        val (root, context) = parseSource(source)
        val classNode = root.getChildrenByType("CLASS")
            .first { it.findChildByType("IDENTIFIER")?.text == "Nameless" }
        val javaClass = JavaClassOverAst(classNode, context)
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
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        println(root.dump())
    }

    @Test
    fun testDebugTypeArgumentsAST() {
        val source = """
            import java.util.List;
            
            public class MyClass {
                public List<String> items;
            }
        """.trimIndent()
        val (root, _) = parseSource(source)

        fun printTree(node: JavaSyntaxNode, indent: String = "") {
            println("$indent${node.type}: '${node.text.take(50).replace("\n", "\\n")}'")
            for (child in node.children) {
                printTree(child, "$indent  ")
            }
        }

        val classNode = root.children.first { it.type.toString() == "CLASS" }
        val fieldNode = classNode.findChildByType("FIELD")!!
        val typeNode = fieldNode.findChildByType("TYPE")!!

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
        val (root, _) = parseSource(source)

        fun collectTypes(node: JavaSyntaxNode): List<String> {
            val result = mutableListOf(node.type.toString())
            for (child in node.children) {
                result.addAll(collectTypes(child))
            }
            return result
        }

        val classNode = root.children.first { it.type.toString() == "CLASS" }
        val methods = classNode.getChildrenByType("METHOD")
        
        // Check foo: List<? extends T>
        val fooMethod = methods.first { it.findChildByType("IDENTIFIER")?.text == "foo" }
        val fooTypeNode = fooMethod.findChildByType("TYPE")!!
        val fooTypes = collectTypes(fooTypeNode)
        // Should contain QUEST for wildcard and EXTENDS_KEYWORD
        assert(fooTypes.any { it == "QUEST" }) { "foo should have QUEST in: $fooTypes" }
        
        // Check bar: List<?>
        val barMethod = methods.first { it.findChildByType("IDENTIFIER")?.text == "bar" }
        val barTypeNode = barMethod.findChildByType("TYPE")!!
        val barTypes = collectTypes(barTypeNode)
        assert(barTypes.any { it == "QUEST" }) { "bar should have QUEST in: $barTypes" }
        
        // Check baz: List<? super T>
        val bazMethod = methods.first { it.findChildByType("IDENTIFIER")?.text == "baz" }
        val bazTypeNode = bazMethod.findChildByType("TYPE")!!
        val bazTypes = collectTypes(bazTypeNode)
        assert(bazTypes.any { it == "QUEST" }) { "baz should have QUEST in: $bazTypes" }
        assert(bazTypes.any { it == "SUPER_KEYWORD" }) { "baz should have SUPER_KEYWORD in: $bazTypes" }
    }
}
