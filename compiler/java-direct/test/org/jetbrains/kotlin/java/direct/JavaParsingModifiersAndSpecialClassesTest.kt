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
import org.junit.jupiter.api.Test

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

        assert(javaClass.isInterface) { "Expected interface" }
        assert(javaClass.fields.size == 2) { "Expected 2 fields, got ${javaClass.fields.size}" }

        val constantField = javaClass.fields.first { it.name.asString() == "CONSTANT" }
        assert(constantField.isStatic) { "Interface field CONSTANT should be implicitly static" }
        assert(constantField.isFinal) { "Interface field CONSTANT should be implicitly final" }
        assert(constantField.visibility.toString() == "public") { "Interface field should be public" }

        val numberField = javaClass.fields.first { it.name.asString() == "NUMBER" }
        assert(numberField.isStatic) { "Interface field NUMBER should be implicitly static" }
        assert(numberField.isFinal) { "Interface field NUMBER should be implicitly final" }
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

        assert(!javaClass.isInterface) { "Expected class, not interface" }
        assert(javaClass.fields.size == 4) { "Expected 4 fields, got ${javaClass.fields.size}" }

        val field1 = javaClass.fields.first { it.name.asString() == "field1" }
        assert(!field1.isStatic) { "field1 should NOT be static" }
        assert(!field1.isFinal) { "field1 should NOT be final" }

        val field2 = javaClass.fields.first { it.name.asString() == "field2" }
        assert(field2.isStatic) { "field2 should be static" }
        assert(!field2.isFinal) { "field2 should NOT be final" }

        val field3 = javaClass.fields.first { it.name.asString() == "field3" }
        assert(!field3.isStatic) { "field3 should NOT be static" }
        assert(field3.isFinal) { "field3 should be final" }

        val field4 = javaClass.fields.first { it.name.asString() == "field4" }
        assert(field4.isStatic) { "field4 should be static" }
        assert(field4.isFinal) { "field4 should be final" }
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

        assert(javaClass.isInterface) { "Expected interface" }
        assert(javaClass.methods.size == 3) { "Expected 3 methods, got ${javaClass.methods.size}" }

        val abstractMethod = javaClass.methods.first { it.name.asString() == "abstractMethod" }
        assert(abstractMethod.isAbstract) { "Interface method without body should be implicitly abstract" }
        assert(abstractMethod.visibility.toString() == "public") { "Interface method should be public" }

        val anotherAbstract = javaClass.methods.first { it.name.asString() == "anotherAbstractMethod" }
        assert(anotherAbstract.isAbstract) { "Interface method without body should be implicitly abstract" }
        assert(anotherAbstract.valueParameters.size == 1) { "Should have 1 parameter" }

        val defaultMethod = javaClass.methods.first { it.name.asString() == "defaultMethod" }
        assert(!defaultMethod.isAbstract) { "Default method with body should NOT be abstract" }
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

        assert(!javaClass.isInterface) { "Expected class, not interface" }
        assert(javaClass.methods.size == 2) { "Expected 2 methods, got ${javaClass.methods.size}" }

        val regularMethod = javaClass.methods.first { it.name.asString() == "regularMethod" }
        assert(!regularMethod.isAbstract) { "Regular method with body should NOT be abstract" }

        val abstractMethod = javaClass.methods.first { it.name.asString() == "abstractMethod" }
        assert(abstractMethod.isAbstract) { "Method with explicit abstract keyword should be abstract" }
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

        assert(javaClass.isInterface) { "Expected interface" }
        assert(javaClass.methods.size == 1) { "Expected 1 method (SAM), got ${javaClass.methods.size}" }

        val applyMethod = javaClass.methods.first()
        assert(applyMethod.name.asString() == "apply") { "Expected method 'apply'" }
        assert(applyMethod.isAbstract) { "SAM method should be abstract for SAM conversion to work" }
        assert(applyMethod.valueParameters.size == 1) { "apply should have 1 parameter" }

        // Verify type parameters
        assert(javaClass.typeParameters.size == 2) { "Expected 2 type parameters, got ${javaClass.typeParameters.size}" }
        val typeParamNames = javaClass.typeParameters.map { it.name.asString() }
        assert("T" in typeParamNames) { "Expected type parameter T" }
        assert("R" in typeParamNames) { "Expected type parameter R" }

        // Verify the annotation is parsed
        assert(javaClass.annotations.size == 1) { "Expected 1 annotation, got ${javaClass.annotations.size}" }
        assert(javaClass.annotations.first().classId?.shortClassName?.asString() == "FunctionalInterface") {
            "Expected @FunctionalInterface annotation"
        }
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
        assert(outerClass.name.asString() == "A") { "Expected outer class name 'A'" }
        assert(outerClass.typeParameters.size == 1) { "Outer class should have 1 type parameter, got ${outerClass.typeParameters.size}" }
        assert(outerClass.typeParameters.first().name.asString() == "X") { "Outer type param should be 'X'" }

        // Verify nested interface exists
        assert(outerClass.innerClassNames.size == 1) { "Expected 1 inner class, got ${outerClass.innerClassNames.size}" }
        assert(outerClass.innerClassNames.first().asString() == "I") { "Expected inner class name 'I'" }

        // Get nested interface via findInnerClass
        val nestedInterface = outerClass.findInnerClass(Name.identifier("I"))
        assert(nestedInterface != null) { "findInnerClass should find 'I'" }
        assert(nestedInterface!!.isInterface) { "I should be an interface" }
        assert(nestedInterface.name.asString() == "I") { "Nested interface name should be 'I'" }

        // Verify nested interface type parameters
        assert(nestedInterface.typeParameters.size == 1) { "Nested interface should have 1 type parameter, got ${nestedInterface.typeParameters.size}" }
        assert(nestedInterface.typeParameters.first().name.asString() == "T") { "Nested type param should be 'T'" }

        // Verify nested interface has SAM method
        assert(nestedInterface.methods.size == 1) { "Nested interface should have 1 method, got ${nestedInterface.methods.size}" }
        val computeMethod = nestedInterface.methods.first()
        assert(computeMethod.name.asString() == "compute") { "Method name should be 'compute'" }
        assert(computeMethod.isAbstract) { "Interface method should be implicitly abstract" }

        // Verify fqName of nested interface
        assert(nestedInterface.fqName?.asString() == "A.I") { "Expected fqName 'A.I', got ${nestedInterface.fqName?.asString()}" }

        // Verify outerClass reference
        assert(nestedInterface.outerClass == outerClass) { "Nested interface should reference outer class" }

        // Verify get method in outer class that uses the nested interface
        val getMethod = outerClass.methods.first { it.name.asString() == "get" }
        assert(getMethod.typeParameters.size == 1) { "get method should have 1 type parameter" }
        assert(getMethod.typeParameters.first().name.asString() == "T") { "get method type param should be 'T'" }
        assert(getMethod.valueParameters.size == 1) { "get method should have 1 parameter" }

        val paramType = getMethod.valueParameters.first().type as JavaClassifierType
        // The type reference I<T> resolves to A.I since it's used within class A
        assert(paramType.classifierQualifiedName == "A.I") { "Parameter type name should be 'A.I', got ${paramType.classifierQualifiedName}" }
        assert(paramType.classifier == nestedInterface) { "Parameter type should resolve to nested interface" }
        assert(paramType.typeArguments.size == 1) { "Parameter type should have 1 type argument, got ${paramType.typeArguments.size}" }
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
        assert(nestedInterface != null) { "Should find NestedInterface" }
        // Interfaces are implicitly static in Java
        assert(nestedInterface!!.isInterface) { "NestedInterface should be an interface" }

        val nestedStaticClass = outerClass.findInnerClass(Name.identifier("NestedStaticClass"))
        assert(nestedStaticClass != null) { "Should find NestedStaticClass" }
        assert(nestedStaticClass!!.isStatic) { "NestedStaticClass should be explicitly static" }

        val nestedInnerClass = outerClass.findInnerClass(Name.identifier("NestedInnerClass"))
        assert(nestedInnerClass != null) { "Should find NestedInnerClass" }
        assert(!nestedInnerClass!!.isStatic) { "NestedInnerClass should NOT be static" }
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
        assert(nestedInterface != null) { "Should find NestedInterface" }
        assert(nestedInterface!!.isInterface) { "NestedInterface should be an interface" }
        assert(nestedInterface.isStatic) { "Nested interface should be implicitly static for FIR isInner=false" }
        assert(nestedInterface.outerClass == outerClass) { "Nested interface should have outer class reference" }

        // Nested enum should be implicitly static
        val nestedEnum = outerClass.findInnerClass(Name.identifier("NestedEnum"))
        assert(nestedEnum != null) { "Should find NestedEnum" }
        assert(nestedEnum!!.isEnum) { "NestedEnum should be an enum" }
        assert(nestedEnum.isStatic) { "Nested enum should be implicitly static for FIR isInner=false" }

        // Inner class (without static keyword) should NOT be static
        val innerClass = outerClass.findInnerClass(Name.identifier("InnerClass"))
        assert(innerClass != null) { "Should find InnerClass" }
        assert(!innerClass!!.isInterface) { "InnerClass should not be an interface" }
        assert(!innerClass.isEnum) { "InnerClass should not be an enum" }
        assert(!innerClass.isStatic) { "Inner class without 'static' keyword should NOT be static" }
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
        assert(day.isEnum) { "Day should be enum" }
        assert(day.isFinal) { "Plain enum Day should be implicitly final" }
        assert(!day.isAbstract) { "Plain enum Day should not be abstract" }

        val ops = classes.first { it.name.asString() == "Ops" }
        assert(ops.isEnum) { "Ops should be enum" }
        assert(!ops.isFinal) { "Enum Ops with abstract method should NOT be final" }
        assert(ops.isAbstract) { "Enum Ops with abstract method should be abstract" }
    }

    @Test
    fun testAnnotationTypeImplicitAbstract() {
        // Annotation types with methods are implicitly abstract
        val source = "public @interface Ann { String value(); }"
        val javaClass = parseFirstClass(source)
        assert(javaClass.isAnnotationType) { "Ann should be annotation type" }
        assert(javaClass.isAbstract) { "Annotation type with methods should be abstract" }
        assert(!javaClass.isFinal) { "Annotation type should not be final" }
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
        assert(shape.isSealed) { "Shape should be sealed" }

        val permitted = shape.permittedTypes.map { it.classifierQualifiedName }.toSet()
        assert(permitted == setOf("Shape.Inner", "Circle", "Square", "Holder.Triangle", "Holder.Mid.Deep")) {
            "Implicit permits must scan the whole compilation unit (siblings + deeply-nested), got $permitted"
        }
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
        assert(shape.isSealed) { "Top-level Shape should be sealed" }

        val permitted = shape.permittedTypes.map { it.classifierQualifiedName }.toSet()
        assert(permitted == setOf("Circle")) {
            "Resolution-based match must include only the real subtype `Circle` and exclude `Box.Impl` " +
                    "(whose `Shape` resolves to the nested `Box.Shape`), got $permitted"
        }
    }
}
