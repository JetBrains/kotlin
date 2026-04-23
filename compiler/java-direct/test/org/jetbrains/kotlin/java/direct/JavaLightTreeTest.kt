/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.parse.dump
import org.jetbrains.kotlin.java.direct.parse.parseJavaToLightTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for the [org.jetbrains.kotlin.java.direct.parse.JavaLightTree] infrastructure. These tests exercise the tree
 * navigation API directly — offsets, text, parent/child relationships, token vs. composite
 * discrimination, and error-input handling — independent of the `*OverAst` model classes that
 * consume the tree. Several behaviours here (token parent via [org.jetbrains.kotlin.java.direct.parse.JavaLightTree.getParent],
 * [org.jetbrains.kotlin.java.direct.parse.JavaLightTree.isToken] / [org.jetbrains.kotlin.java.direct.parse.JavaLightTree.isComposite] discrimination, [org.jetbrains.kotlin.java.direct.parse.JavaLightTree.textEquals]
 * edge cases, malformed-source tolerance, [dump]) are not covered elsewhere and
 * form the regression safety net for the tree primitives.
 */
class JavaLightTreeTest {

    private fun parse(source: String): JavaLightTree =
        parseJavaToLightTree(source, 0)

    private fun JavaLightTree.findFirstClass(): JavaLightNode {
        val classes = getChildrenByType(getRoot(), JavaSyntaxElementType.CLASS)
        check(classes.isNotEmpty()) { "No CLASS node found in tree" }
        return classes.first()
    }

    @Test
    fun testRootNodeType() {
        val tree = parse("class A {}")
        val root = tree.getRoot()
        assertTrue(tree.isComposite(root))
        // The root is the file-level production marker.
        // Type can be JAVA_FILE / FILE / similar — we just verify it's a composite with content.
        assertEquals(0, tree.getStartOffset(root))
        assertEquals("class A {}".length, tree.getEndOffset(root))
    }

    @Test
    fun testRootHasClassChild() {
        val tree = parse("public final class A {}")
        val root = tree.getRoot()
        val children = tree.getChildren(root)
        val classChildren = children.filter { tree.getType(it) == JavaSyntaxElementType.CLASS }
        assertEquals(1, classChildren.size)
    }

    @Test
    fun testGetTextForCompositeAndToken() {
        val source = "class A {}"
        val tree = parse(source)
        val root = tree.getRoot()
        assertEquals(source, tree.getText(root).toString())
        val classNode = tree.findFirstClass()
        assertEquals("class A {}", tree.getText(classNode).toString())
        // Find the IDENTIFIER token inside the class node.
        val identifier = tree.findChildByType(classNode, JavaSyntaxTokenType.IDENTIFIER)
        assertNotNull(identifier)
        assertEquals("A", tree.getText(identifier!!).toString())
        assertTrue(tree.isToken(identifier))
    }

    @Test
    fun testTextEqualsMatchesAndDifferentiates() {
        val tree = parse("class Foo {}")
        val classNode = tree.findFirstClass()
        val identifier = tree.findChildByType(classNode, JavaSyntaxTokenType.IDENTIFIER)!!
        assertTrue(tree.textEquals(identifier, "Foo"))
        assertFalse(tree.textEquals(identifier, "Bar"))
        assertFalse(tree.textEquals(identifier, "Foox"))  // longer
        assertFalse(tree.textEquals(identifier, "Fo"))    // shorter
    }

    @Test
    fun testFindChildByTypeReturnsNullWhenAbsent() {
        val tree = parse("class A {}")
        val classNode = tree.findFirstClass()
        // No interface keyword, no record header in a plain `class A {}`.
        assertNull(tree.findChildByType(classNode, JavaSyntaxTokenType.INTERFACE_KEYWORD))
        assertNull(tree.findChildByType(classNode, JavaSyntaxElementType.RECORD_HEADER))
    }

    @Test
    fun testGetChildrenByTypeReturnsAllMatches() {
        val source = """
            class A {
                int x;
                int y;
                int z;
            }
        """.trimIndent()
        val tree = parse(source)
        val classNode = tree.findFirstClass()
        val fields = tree.getChildrenByType(classNode, JavaSyntaxElementType.FIELD)
        assertEquals(3, fields.size)
        // Verify they are all FIELD nodes.
        for (field in fields) {
            assertEquals(JavaSyntaxElementType.FIELD, tree.getType(field))
        }
    }

    @Test
    fun testHasChildOfType() {
        val tree = parse("class A {}")
        val classNode = tree.findFirstClass()
        assertTrue(tree.hasChildOfType(classNode, JavaSyntaxTokenType.CLASS_KEYWORD))
        assertFalse(tree.hasChildOfType(classNode, JavaSyntaxTokenType.INTERFACE_KEYWORD))
    }

    @Test
    fun testGetParentForCompositeAndToken() {
        val tree = parse("class Outer { class Inner {} }")
        val root = tree.getRoot()
        val outer = tree.findFirstClass()
        assertEquals(root.index, tree.getParent(outer)!!.index)
        // Inner class is a child of the outer class.
        val inner = tree.getChildrenByType(outer, JavaSyntaxElementType.CLASS).first()
        assertEquals(outer.index, tree.getParent(inner)!!.index)
        // Token (CLASS_KEYWORD) parent is `outer`.
        val classKeyword = tree.findChildByType(outer, JavaSyntaxTokenType.CLASS_KEYWORD)!!
        assertEquals(outer.index, tree.getParent(classKeyword)!!.index)
    }

    @Test
    fun testGetParentOfRootIsNull() {
        val tree = parse("class A {}")
        assertNull(tree.getParent(tree.getRoot()))
    }

    @Test
    fun testChildrenOfTokenIsEmpty() {
        val tree = parse("class A {}")
        val classNode = tree.findFirstClass()
        val identifier = tree.findChildByType(classNode, JavaSyntaxTokenType.IDENTIFIER)!!
        assertTrue(tree.getChildren(identifier).isEmpty())
    }

    @Test
    fun testDeeplyNestedNavigation() {
        val source = """
            package com.example;

            @Deprecated
            public class Hello {
                public void greet() {}
            }
        """.trimIndent()
        val tree = parse(source)
        val classNode = tree.findFirstClass()
        val modifierList = tree.findChildByType(classNode, JavaSyntaxElementType.MODIFIER_LIST)
        assertNotNull(modifierList)
        val annotations = tree.getChildrenByType(modifierList!!, JavaSyntaxElementType.ANNOTATION)
        assertEquals(1, annotations.size)
        val annotation = annotations.first()
        // ANNOTATION → JAVA_CODE_REFERENCE → IDENTIFIER ("Deprecated")
        val ref = tree.findChildByType(annotation, JavaSyntaxElementType.JAVA_CODE_REFERENCE)
        assertNotNull(ref)
        val identifier = tree.findChildByType(ref!!, JavaSyntaxTokenType.IDENTIFIER)
        assertNotNull(identifier)
        assertEquals("Deprecated", tree.getText(identifier!!).toString())
    }

    @Test
    fun testMultipleTopLevelClasses() {
        val tree = parse("class A {} class B {} class C {}")
        val root = tree.getRoot()
        val classNodes = tree.getChildrenByType(root, JavaSyntaxElementType.CLASS)
        assertEquals(3, classNodes.size)
        val names = classNodes.map { c ->
            tree.getText(tree.findChildByType(c, JavaSyntaxTokenType.IDENTIFIER)!!).toString()
        }
        assertEquals(listOf("A", "B", "C"), names)
    }

    @Test
    fun testParseFileWithErrorsHandled() {
        // Malformed input should still produce a tree (with error markers).
        // We just check we don't crash and we get reasonable structure for the part that parses.
        val source = "class A { void () {} int ; }"
        val tree = parse(source)
        val classNode = tree.findFirstClass()
        // The class node should still exist with name A.
        val identifier = tree.findChildByType(classNode, JavaSyntaxTokenType.IDENTIFIER)
        assertNotNull(identifier)
        assertEquals("A", tree.getText(identifier!!).toString())
    }

    @Test
    fun testPackageStatementChildren() {
        val source = """
            package example;

            public class Hello {}
        """.trimIndent()
        val tree = parse(source)
        val root = tree.getRoot()
        val packageStmt = tree.findChildByType(root, JavaSyntaxElementType.PACKAGE_STATEMENT)
        assertNotNull(packageStmt)
        val ref = tree.findChildByType(packageStmt!!, JavaSyntaxElementType.JAVA_CODE_REFERENCE)
        assertNotNull(ref)
        val identifier = tree.findChildByType(ref!!, JavaSyntaxTokenType.IDENTIFIER)
        assertNotNull(identifier)
        assertEquals("example", tree.getText(identifier!!).toString())
    }

    @Test
    fun testFieldsAndMethodsAreSiblings() {
        val source = """
            class A {
                int x;
                void f() {}
                int y;
                void g() {}
            }
        """.trimIndent()
        val tree = parse(source)
        val classNode = tree.findFirstClass()
        val fields = tree.getChildrenByType(classNode, JavaSyntaxElementType.FIELD)
        val methods = tree.getChildrenByType(classNode, JavaSyntaxElementType.METHOD)
        assertEquals(2, fields.size)
        assertEquals(2, methods.size)
        // Ensure children come back in source order.
        val children = tree.getChildren(classNode)
        val fieldOrMethod = children.filter {
            tree.getType(it) == JavaSyntaxElementType.FIELD || tree.getType(it) == JavaSyntaxElementType.METHOD
        }
        val sourceOrderTypes = fieldOrMethod.map { tree.getType(it) }
        assertEquals(
            listOf<SyntaxElementType>(
                JavaSyntaxElementType.FIELD,
                JavaSyntaxElementType.METHOD,
                JavaSyntaxElementType.FIELD,
                JavaSyntaxElementType.METHOD,
            ),
            sourceOrderTypes,
        )
    }

    @Test
    fun testTypeParameterAndExtendsList() {
        val source = """
            class A<T extends Number> extends Object implements Runnable {}
        """.trimIndent()
        val tree = parse(source)
        val classNode = tree.findFirstClass()
        val typeParamList = tree.findChildByType(classNode, JavaSyntaxElementType.TYPE_PARAMETER_LIST)
        assertNotNull(typeParamList)
        val extendsList = tree.findChildByType(classNode, JavaSyntaxElementType.EXTENDS_LIST)
        assertNotNull(extendsList)
        val implementsList = tree.findChildByType(classNode, JavaSyntaxElementType.IMPLEMENTS_LIST)
        assertNotNull(implementsList)
    }

    @Test
    fun testCompositeAndTokenStartEndOffsets() {
        val source = "class A {}"
        val tree = parse(source)
        val classNode = tree.findFirstClass()
        // class keyword at offset 0..5
        val classKeyword = tree.findChildByType(classNode, JavaSyntaxTokenType.CLASS_KEYWORD)!!
        assertEquals(0, tree.getStartOffset(classKeyword))
        assertEquals(5, tree.getEndOffset(classKeyword))
        // identifier "A" at offset 6..7
        val identifier = tree.findChildByType(classNode, JavaSyntaxTokenType.IDENTIFIER)!!
        assertEquals(6, tree.getStartOffset(identifier))
        assertEquals(7, tree.getEndOffset(identifier))
    }

    @Test
    fun testDumpProducesTextRepresentation() {
        val tree = parse("class A {}")
        val dump = tree.dump()
        assertTrue(dump.contains("CLASS"))
        assertTrue(dump.contains("class A {}"))
    }
}
