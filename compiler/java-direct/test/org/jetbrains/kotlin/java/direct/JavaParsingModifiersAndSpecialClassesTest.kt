/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.element.JavaSyntaxTokenType
import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class JavaParsingModifiersAndSpecialClassesTest : JavaParsingTestBase() {

    @Test
    fun testInterfaceFieldsImplicitlyStaticFinal() {
        val source = """
            public interface MyInterface {
                String CONSTANT = "value";
                int NUMBER = 42;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assertTrue(javaClass.isInterface)
        assertEquals(2, javaClass.fields.size)

        val constantField = javaClass.fields.first { it.name.asString() == "CONSTANT" }
        assertTrue(constantField.isStatic, "Interface field CONSTANT should be implicitly static")
        assertTrue(constantField.isFinal, "Interface field CONSTANT should be implicitly final")
        assertEquals("public", constantField.visibility.toString())

        val numberField = javaClass.fields.first { it.name.asString() == "NUMBER" }
        assertTrue(numberField.isStatic, "Interface field NUMBER should be implicitly static")
        assertTrue(numberField.isFinal, "Interface field NUMBER should be implicitly final")
    }

    @Test
    fun testClassFieldsNotImplicitlyStaticFinal() {
        val source = """
            public class MyClass {
                String field1;
                static String field2;
                final String field3 = "x";
                static final String field4 = "y";
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assertFalse(javaClass.isInterface, "Expected class, not interface")
        assertEquals(4, javaClass.fields.size)

        val field1 = javaClass.fields.first { it.name.asString() == "field1" }
        assertFalse(field1.isStatic, "field1 should NOT be static")
        assertFalse(field1.isFinal, "field1 should NOT be final")

        val field2 = javaClass.fields.first { it.name.asString() == "field2" }
        assertTrue(field2.isStatic, "field2 should be static")
        assertFalse(field2.isFinal, "field2 should NOT be final")

        val field3 = javaClass.fields.first { it.name.asString() == "field3" }
        assertFalse(field3.isStatic, "field3 should NOT be static")
        assertTrue(field3.isFinal, "field3 should be final")

        val field4 = javaClass.fields.first { it.name.asString() == "field4" }
        assertTrue(field4.isStatic, "field4 should be static")
        assertTrue(field4.isFinal, "field4 should be final")
    }

    @Test
    fun testInterfaceMethodsImplicitlyAbstract() {
        val source = """
            public interface MyInterface {
                void abstractMethod();
                String anotherAbstractMethod(int x);
                default void defaultMethod() { }
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assertTrue(javaClass.isInterface)
        assertEquals(3, javaClass.methods.size)

        val abstractMethod = javaClass.methods.first { it.name.asString() == "abstractMethod" }
        assertTrue(abstractMethod.isAbstract, "Interface method without body should be implicitly abstract")
        assertEquals("public", abstractMethod.visibility.toString())

        val anotherAbstract = javaClass.methods.first { it.name.asString() == "anotherAbstractMethod" }
        assertTrue(anotherAbstract.isAbstract, "Interface method without body should be implicitly abstract")
        assertEquals(1, anotherAbstract.valueParameters.size)

        val defaultMethod = javaClass.methods.first { it.name.asString() == "defaultMethod" }
        assertFalse(defaultMethod.isAbstract, "Default method with body should NOT be abstract")
    }

    @Test
    fun testClassMethodsNotImplicitlyAbstract() {
        val source = """
            public class MyClass {
                void regularMethod() { }
                abstract void abstractMethod();
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assertFalse(javaClass.isInterface, "Expected class, not interface")
        assertEquals(2, javaClass.methods.size)

        val regularMethod = javaClass.methods.first { it.name.asString() == "regularMethod" }
        assertFalse(regularMethod.isAbstract, "Regular method with body should NOT be abstract")

        val abstractMethod = javaClass.methods.first { it.name.asString() == "abstractMethod" }
        assertTrue(abstractMethod.isAbstract, "Method with explicit abstract keyword should be abstract")
    }

    @Test
    fun testFunctionalInterfaceForSamConversion() {
        val source = """
            @FunctionalInterface
            public interface MyFunction<T, R> {
                R apply(T t);
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assertTrue(javaClass.isInterface)
        assertEquals(1, javaClass.methods.size, "Expected exactly 1 method (SAM)")

        val applyMethod = javaClass.methods.first()
        assertEquals("apply", applyMethod.name.asString())
        assertTrue(applyMethod.isAbstract, "SAM method should be abstract for SAM conversion to work")
        assertEquals(1, applyMethod.valueParameters.size)

        // Verify type parameters
        assertEquals(2, javaClass.typeParameters.size)
        val typeParamNames = javaClass.typeParameters.map { it.name.asString() }
        assertTrue("T" in typeParamNames, "Expected type parameter T, got $typeParamNames")
        assertTrue("R" in typeParamNames, "Expected type parameter R, got $typeParamNames")

        // Verify the annotation is parsed
        assertEquals(1, javaClass.annotations.size)
        assertEquals("FunctionalInterface", javaClass.annotations.first().classId?.shortClassName?.asString())
    }

    @Test
    fun testNestedInterfaceWithTypeParameters() {
        // This tests the pattern that causes testJavaNestedSamInterface to fail
        // Outer class A<X> has nested interface I<T>
        val source = """
            public class A<X extends Number> {
                private final X x;

                public A(X x) {
                    this.x = x;
                }

                public interface I<T> {
                    T compute();
                }

                public <T> T get(I<T> value) { return value.compute(); }
            }
        """.trimIndent()
        val outerClass = parseFirstClass(source)

        // Verify outer class
        assertEquals("A", outerClass.name.asString())
        assertEquals(1, outerClass.typeParameters.size)
        assertEquals("X", outerClass.typeParameters.first().name.asString())

        // Verify nested interface exists
        assertEquals(1, outerClass.innerClassNames.size)
        assertEquals("I", outerClass.innerClassNames.first().asString())

        // Get nested interface via findInnerClass
        val nestedInterface = outerClass.findInnerClass(Name.identifier("I"))
        assertNotNull(nestedInterface) { "findInnerClass should find 'I'" }
        assertTrue(nestedInterface.isInterface, "I should be an interface")
        assertEquals("I", nestedInterface.name.asString())

        // Verify nested interface type parameters
        assertEquals(1, nestedInterface.typeParameters.size)
        assertEquals("T", nestedInterface.typeParameters.first().name.asString())

        // Verify nested interface has SAM method
        assertEquals(1, nestedInterface.methods.size)
        val computeMethod = nestedInterface.methods.first()
        assertEquals("compute", computeMethod.name.asString())
        assertTrue(computeMethod.isAbstract, "Interface method should be implicitly abstract")

        // Verify fqName of nested interface
        assertEquals("A.I", nestedInterface.fqName?.asString())

        // Verify outerClass reference
        assertEquals(outerClass, nestedInterface.outerClass, "Nested interface should reference outer class")

        // Verify get method in outer class that uses the nested interface
        val getMethod = outerClass.methods.first { it.name.asString() == "get" }
        assertEquals(1, getMethod.typeParameters.size)
        assertEquals("T", getMethod.typeParameters.first().name.asString())
        assertEquals(1, getMethod.valueParameters.size)

        val paramType = getMethod.valueParameters.first().type as JavaClassifierType
        // The type reference I<T> resolves to A.I since it's used within class A
        assertEquals("A.I", paramType.classifierQualifiedName)
        assertEquals(nestedInterface, paramType.classifier, "Parameter type should resolve to nested interface")
        assertEquals(1, paramType.typeArguments.size)
    }

    @Test
    fun testNestedInterfaceIsStatic() {
        // Java nested interfaces are always implicitly static
        val source = """
            public class Outer {
                public interface NestedInterface {
                    void method();
                }
                
                public static class NestedStaticClass {
                }
                
                public class NestedInnerClass {
                }
            }
        """.trimIndent()
        val outerClass = parseFirstClass(source)

        val nestedInterface = outerClass.findInnerClass(Name.identifier("NestedInterface"))
        assertNotNull(nestedInterface) { "Should find NestedInterface" }
        // Interfaces are implicitly static in Java
        assertTrue(nestedInterface.isInterface, "NestedInterface should be an interface")

        val nestedStaticClass = outerClass.findInnerClass(Name.identifier("NestedStaticClass"))
        assertNotNull(nestedStaticClass) { "Should find NestedStaticClass" }
        assertTrue(nestedStaticClass.isStatic, "NestedStaticClass should be explicitly static")

        val nestedInnerClass = outerClass.findInnerClass(Name.identifier("NestedInnerClass"))
        assertNotNull(nestedInnerClass) { "Should find NestedInnerClass" }
        assertFalse(nestedInnerClass.isStatic, "NestedInnerClass should NOT be static")
    }

    @Test
    fun testNestedInterfaceAndEnumImplicitlyStatic() {
        // Java nested interfaces and enums are implicitly static even without the keyword
        // This is critical for FIR to correctly set isInner=false for these types
        val source = """
            public class Outer<T> {
                public interface NestedInterface<U> {
                    U compute();
                }
                
                public enum NestedEnum {
                    A, B, C
                }
                
                public class InnerClass {
                }
            }
        """.trimIndent()
        val outerClass = parseFirstClass(source)

        // Nested interface should be implicitly static (no 'static' keyword in source)
        val nestedInterface = outerClass.findInnerClass(Name.identifier("NestedInterface"))
        assertNotNull(nestedInterface) { "Should find NestedInterface" }
        assertTrue(nestedInterface.isInterface, "NestedInterface should be an interface")
        assertTrue(nestedInterface.isStatic, "Nested interface should be implicitly static for FIR isInner=false")
        assertEquals(outerClass, nestedInterface.outerClass, "Nested interface should have outer class reference")

        // Nested enum should be implicitly static
        val nestedEnum = outerClass.findInnerClass(Name.identifier("NestedEnum"))
        assertNotNull(nestedEnum) { "Should find NestedEnum" }
        assertTrue(nestedEnum.isEnum, "NestedEnum should be an enum")
        assertTrue(nestedEnum.isStatic, "Nested enum should be implicitly static for FIR isInner=false")

        // Inner class (without static keyword) should NOT be static
        val innerClass = outerClass.findInnerClass(Name.identifier("InnerClass"))
        assertNotNull(innerClass) { "Should find InnerClass" }
        assertFalse(innerClass.isInterface, "InnerClass should not be an interface")
        assertFalse(innerClass.isEnum, "InnerClass should not be an enum")
        assertFalse(innerClass.isStatic, "Inner class without 'static' keyword should NOT be static")
    }

    @Test
    fun testEnumImplicitFinal() {
        // JLS 8.9: enums are implicitly final (unless they have abstract methods)
        val source = """
            public enum Day { MON, TUE }
            public enum Ops {
                PLUS { public int apply(int x) { return x + 1; } };
                public abstract int apply(int x);
            }
        """.trimIndent()
        val parsed = parseSource(source)
        val root = parsed.root
        val tree = parsed.tree
        val context = parsed.context
        val classes = tree.getChildrenByType(root, JavaSyntaxElementType.CLASS).map { JavaClassOverAst(it, tree, context) }

        val day = classes.first { it.name.asString() == "Day" }
        assertTrue(day.isEnum, "Day should be enum")
        assertTrue(day.isFinal, "Plain enum Day should be implicitly final")
        assertFalse(day.isAbstract, "Plain enum Day should not be abstract")

        val ops = classes.first { it.name.asString() == "Ops" }
        assertTrue(ops.isEnum, "Ops should be enum")
        assertFalse(ops.isFinal, "Enum Ops with abstract method should NOT be final")
        assertTrue(ops.isAbstract, "Enum Ops with abstract method should be abstract")
    }

    @Test
    fun testAnnotationTypeImplicitAbstract() {
        // Annotation types with methods are implicitly abstract
        val source = "public @interface Ann { String value(); }"
        val javaClass = parseFirstClass(source)
        assertTrue(javaClass.isAnnotationType, "Ann should be annotation type")
        assertTrue(javaClass.isAbstract, "Annotation type with methods should be abstract")
        assertFalse(javaClass.isFinal, "Annotation type should not be final")
    }

    @Test
    fun testSealedImplicitPermitsScansWholeCompilationUnit() {
        // JLS 8.1.6 / 9.1.4: with no `permits` clause, the permitted subtypes are *every* class in
        // the same compilation unit whose direct superclass is the sealed type — top-level siblings
        // and member types at any nesting depth, not only directly-nested members.
        val source = """
            sealed interface Shape {
                final class Inner implements Shape {}
            }

            final class Circle implements Shape {}

            final class Square implements Shape {}

            class Holder {
                static final class Triangle implements Shape {}
                static class Mid {
                    static final class Deep implements Shape {}
                }
            }
        """.trimIndent()
        val parsed = parseSource(source)
        val tree = parsed.tree
        val shapeNode = tree.getChildrenByType(parsed.root, JavaSyntaxElementType.CLASS).first { node ->
            tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.getText(it).toString() } == "Shape"
        }
        val shape = JavaClassOverAst(shapeNode, tree, parsed.context)
        assertTrue(shape.isSealed, "Shape should be sealed")

        val permitted = shape.permittedTypes.map { it.classifierQualifiedName }.toSet()
        assertEquals(
            setOf("Shape.Inner", "Circle", "Square", "Holder.Triangle", "Holder.Mid.Deep"),
            permitted,
            "Implicit permits must scan the whole compilation unit (siblings + deeply-nested)",
        )
    }

    @Test
    fun testSealedImplicitPermitsMatchesByResolutionNotText() {
        // The implicit-`permits` match must be resolution-based (like PSI's `isInheritor`), not a raw
        // text match. `Box.Impl implements Shape` textually names "Shape", but in `Impl`'s scope that
        // `Shape` resolves to the shadowing nested `Box.Shape`, NOT the top-level sealed `Shape`.
        // A purely textual match would wrongly count `Box.Impl` (false positive); resolution excludes
        // it while still including the genuine top-level `Circle`.
        val source = """
            sealed interface Shape {}

            final class Circle implements Shape {}

            class Box {
                interface Shape {}
                static final class Impl implements Shape {}
            }
        """.trimIndent()
        val parsed = parseSource(source)
        val tree = parsed.tree
        val shapeNode = tree.getChildrenByType(parsed.root, JavaSyntaxElementType.CLASS).first { node ->
            tree.findChildByType(node, JavaSyntaxTokenType.IDENTIFIER)?.let { tree.getText(it).toString() } == "Shape"
        }
        val shape = JavaClassOverAst(shapeNode, tree, parsed.context)
        assertTrue(shape.isSealed, "Top-level Shape should be sealed")

        val permitted = shape.permittedTypes.map { it.classifierQualifiedName }.toSet()
        assertEquals(
            setOf("Circle"),
            permitted,
            "Resolution-based match must include only the real subtype `Circle` and exclude `Box.Impl` " +
                    "(whose `Shape` resolves to the nested `Box.Shape`)",
        )
    }
}
