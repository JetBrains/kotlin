/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.java.direct.parse.JAVA_DIRECT_PLACEHOLDER_TYPE
import org.jetbrains.kotlin.java.direct.parse.JavaLightAstNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightNode
import org.jetbrains.kotlin.java.direct.parse.JavaLightTree
import org.jetbrains.kotlin.java.direct.parse.parseJavaToLightTree
import org.jetbrains.kotlin.text
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Verifies that the [org.jetbrains.kotlin.java.direct.parse.JavaLightTreeStructure] adapter and the
 * [JavaLightAstNode] wrapper produce correct, AST-backed [KtLightSourceElement]s with exact offsets and
 * text, as required for populating `source` on java-direct FIR declarations.
 */
class JavaLightSourceElementTest {

    private val source = """
        class A {
            int field;
            A() {}
            void method() {}
        }
    """.trimIndent()

    private fun parse(text: String): JavaLightTree = parseJavaToLightTree(text, 0)

    private fun JavaLightTree.firstClass(): JavaLightNode =
        getChildrenByType(getRoot(), JavaSyntaxElementType.CLASS).first()

    private fun JavaLightTree.sourceElement(node: JavaLightNode, kind: org.jetbrains.kotlin.KtSourceElementKind = KtRealSourceElementKind) =
        KtLightSourceElement(
            JavaLightAstNode(this, node),
            getStartOffset(node),
            getEndOffset(node),
            lightSourceTreeStructure,
            kind,
        )

    @Test
    fun testClassSourceOffsetsAndText() {
        val tree = parse(source)
        val classNode = tree.firstClass()
        val element = tree.sourceElement(classNode)

        assertEquals(source.indexOf("class A"), element.startOffset)
        assertEquals(source.length, element.endOffset)
        assertEquals(source.substring(element.startOffset, element.endOffset), element.text.toString())
    }

    @Test
    fun testFieldMethodConstructorSources() {
        val tree = parse(source)
        val classNode = tree.firstClass()

        val field = tree.getChildrenByType(classNode, JavaSyntaxElementType.FIELD).single()
        val methods = tree.getChildrenByType(classNode, JavaSyntaxElementType.METHOD)
        // In IJ Java syntax both the constructor and the regular method are METHOD nodes.
        assertEquals(2, methods.size)

        for (node in listOf(field) + methods) {
            val element = tree.sourceElement(node)
            assertEquals(tree.getStartOffset(node), element.startOffset)
            assertEquals(tree.getEndOffset(node), element.endOffset)
            assertEquals(source.substring(element.startOffset, element.endOffset), element.text.toString())
        }

        val fieldElement = tree.sourceElement(field)
        assertEquals("int field;", fieldElement.text.toString())
    }

    @Test
    fun testPlaceholderElementTypeAndTreeStructureNavigation() {
        val tree = parse(source)
        val classNode = tree.firstClass()
        val element = tree.sourceElement(classNode)

        // Element type is the shared placeholder (type fidelity intentionally not required).
        assertSame(JAVA_DIRECT_PLACEHOLDER_TYPE, element.elementType)

        // The treeStructure can navigate from the wrapped node.
        val treeStructure = element.treeStructure
        assertEquals(source, treeStructure.toString(treeStructure.getRoot()).toString())
        assertNull(treeStructure.getParent(treeStructure.getRoot()))
    }

    @Test
    fun testSharedTreeStructureAndDistinctness() {
        val tree = parse(source)
        val classNode = tree.firstClass()
        val field = tree.getChildrenByType(classNode, JavaSyntaxElementType.FIELD).single()

        val classElement = tree.sourceElement(classNode)
        val fieldElement = tree.sourceElement(field)

        // All elements from one tree share a single memoized treeStructure instance.
        assertSame(classElement.treeStructure, fieldElement.treeStructure)

        // Distinct nodes -> non-equal source elements.
        assertNotEquals(classElement, fieldElement)

        // Same node, same kind -> equal; different kind -> non-equal but same underlying node.
        val classElementAgain = tree.sourceElement(classNode)
        assertEquals(classElement, classElementAgain)

        val fakeKindElement = tree.sourceElement(classNode, KtFakeSourceElementKind.ImplicitConstructor)
        assertNotEquals(classElement, fakeKindElement)
        assertEquals(classElement.lighterASTNode, fakeKindElement.lighterASTNode)
    }

    @Test
    fun testIdentifierTokenSource() {
        val tree = parse(source)
        val classNode = tree.firstClass()
        val identifier = tree.findChildByType(classNode, JavaSyntaxTokenType.IDENTIFIER)!!
        val element = tree.sourceElement(identifier)
        assertEquals("A", element.text.toString())
    }
}
