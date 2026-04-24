/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test

class JavaParsingMembersTest : JavaParsingTestBase() {

    @Test
    fun testMembers() {
        val source = """
            class A {
                public int field;
                public void method() {}
                public A() {}
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assert(javaClass.fields.size == 1)
        assert(javaClass.fields.first().name.asString() == "field")

        assert(javaClass.methods.size == 1)
        assert(javaClass.methods.first().name.asString() == "method")

        assert(javaClass.constructors.size == 1)
        assert(javaClass.constructors.first().name.asString() == "A")
    }

    @Test
    fun testDefaultConstructor() {
        val sourceWithoutConstructor = """
            public class A {}
        """.trimIndent()
        val javaClass1 = parseFirstClass(sourceWithoutConstructor)

        assert(javaClass1.constructors.isEmpty()) { "Expected no explicit constructors" }
        assert(javaClass1.hasDefaultConstructor()) { "Expected hasDefaultConstructor() = true for class without explicit constructor" }
        assert(!javaClass1.isInterface) { "A is not an interface" }

        val sourceWithConstructor = """
            public class B {
                public B() {}
            }
        """.trimIndent()
        val javaClass2 = parseFirstClass(sourceWithConstructor)

        assert(javaClass2.constructors.size == 1) { "Expected 1 explicit constructor, got ${javaClass2.constructors.size}" }
        assert(!javaClass2.hasDefaultConstructor()) { "Expected hasDefaultConstructor() = false for class with explicit constructor" }

        val sourceInterface = """
            public interface I {}
        """.trimIndent()
        val javaClass3 = parseFirstClass(sourceInterface)

        assert(javaClass3.constructors.isEmpty()) { "Expected no constructors for interface" }
        assert(!javaClass3.hasDefaultConstructor()) { "Expected hasDefaultConstructor() = false for interface" }
        assert(javaClass3.isInterface) { "I should be an interface" }
    }

    @Test
    fun testVoidReturnType() {
        val source = """
            public class A {
                public void method() {}
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assert(javaClass.methods.size == 1) { "Expected 1 method, got ${javaClass.methods.size}" }
        val method = javaClass.methods.first()
        assert(method.name.asString() == "method")

        val returnType = method.returnType
        assert(returnType is JavaPrimitiveType) { "Expected JavaPrimitiveType, got ${returnType::class.java}" }
        assert((returnType as JavaPrimitiveType).type == null) { "Expected type=null for void, got ${returnType.type}" }
    }

    @Test
    fun testMethodParameters() {
        val source = """
            import java.util.List;
            public class A {
                public void method1() {}
                public void method2(int a) {}
                public void method3(String a, int b, List<String> c) {}
                public A() {}
                public A(int x) {}
                public A(String s, Object o) {}
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val method1 = javaClass.methods.first { it.name.asString() == "method1" }
        assert(method1.valueParameters.size == 0) { "method1 should have 0 parameters, got ${method1.valueParameters.size}" }

        val method2 = javaClass.methods.first { it.name.asString() == "method2" }
        assert(method2.valueParameters.size == 1) { "method2 should have 1 parameter, got ${method2.valueParameters.size}" }
        val param2 = method2.valueParameters.first()
        assert(param2.name?.asString() == "a") { "Expected parameter name 'a', got ${param2.name}" }
        assert(param2.type is org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType) { "Expected int to be JavaPrimitiveType" }

        val method3 = javaClass.methods.first { it.name.asString() == "method3" }
        assert(method3.valueParameters.size == 3) { "method3 should have 3 parameters, got ${method3.valueParameters.size}" }
        val params3 = method3.valueParameters.toList()
        assert(params3[0].name?.asString() == "a") { "Expected parameter name 'a', got ${params3[0].name}" }
        assert(params3[1].name?.asString() == "b") { "Expected parameter name 'b', got ${params3[1].name}" }
        assert(params3[2].name?.asString() == "c") { "Expected parameter name 'c', got ${params3[2].name}" }

        val paramAType = params3[0].type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(paramAType.classifierQualifiedName == "String") { "Expected String, got ${paramAType.classifierQualifiedName}" }

        val paramBType = params3[1].type as org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
        assert(paramBType.type == org.jetbrains.kotlin.builtins.PrimitiveType.INT) { "Expected INT primitive type" }

        val paramCType = params3[2].type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(paramCType.classifierQualifiedName == "java.util.List") { "Expected java.util.List, got ${paramCType.classifierQualifiedName}" }

        val constructor0 = javaClass.constructors.first { it.valueParameters.size == 0 }
        assert(constructor0.valueParameters.size == 0) { "Constructor should have 0 parameters" }

        val constructor1 = javaClass.constructors.first { it.valueParameters.size == 1 }
        assert(constructor1.valueParameters.size == 1) { "Constructor should have 1 parameter, got ${constructor1.valueParameters.size}" }
        val constParam1 = constructor1.valueParameters.first()
        assert(constParam1.name?.asString() == "x") { "Expected parameter name 'x', got ${constParam1.name}" }

        val constructor2 = javaClass.constructors.first { it.valueParameters.size == 2 }
        assert(constructor2.valueParameters.size == 2) { "Constructor should have 2 parameters, got ${constructor2.valueParameters.size}" }
        val constParams2 = constructor2.valueParameters.toList()
        assert(constParams2[0].name?.asString() == "s") { "Expected parameter name 's', got ${constParams2[0].name}" }
        assert(constParams2[1].name?.asString() == "o") { "Expected parameter name 'o', got ${constParams2[1].name}" }
    }

    @Test
    fun testMethodParametersWithObjectType() {
        val source = """
            public class JI {
                public abstract boolean equals(Object o);
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val equalsMethod = javaClass.methods.first { it.name.asString() == "equals" }
        assert(equalsMethod.valueParameters.size == 1) { "equals should have 1 parameter, got ${equalsMethod.valueParameters.size}" }

        val param = equalsMethod.valueParameters.first()
        assert(param.name?.asString() == "o") { "Expected parameter name 'o', got ${param.name}" }

        val paramType = param.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(paramType.classifierQualifiedName == "Object") { "Expected 'Object', got '${paramType.classifierQualifiedName}'" }
        assert(!paramType.isResolved) { "Object should not be pre-resolved" }
        assert(paramType.classifier == null) { "Object should have null classifier (external type)" }

        val resolved = paramType.resolve(tryResolve = { candidateClassId ->
            candidateClassId == ClassId.topLevel(FqName("java.lang.Object"))
        })

        assert(resolved == ClassId.topLevel(FqName("java.lang.Object"))) { "Expected 'java.lang.Object', got '$resolved'" }
    }

    @Test
    fun testNativeMethod() {
        val source = """
            public class Foo {
                public native void nativeMethod();
                public void normalMethod() {}
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)
        val nativeMethod = javaClass.methods.first { it.name.asString() == "nativeMethod" }
        val normalMethod = javaClass.methods.first { it.name.asString() == "normalMethod" }
        assert(nativeMethod.isNative) { "nativeMethod should have isNative=true" }
        assert(!normalMethod.isNative) { "normalMethod should have isNative=false" }
    }

    @Test
    fun testConstructorImplicitFinal() {
        val source = "public class Foo { public Foo() {} }"
        val javaClass = parseFirstClass(source)
        val ctor = javaClass.constructors.single()
        assert(ctor.isFinal) { "Constructor should be implicitly final" }
        assert(!ctor.isAbstract) { "Constructor should not be abstract" }
        assert(!ctor.isStatic) { "Constructor should not be static" }
    }

    @Test
    fun testMultiFieldModifiers() {
        val source = """
            public class Foo {
                public final static int
                    ERROR = -1,
                    EOF = 0,
                    EOL = 1;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)
        val errorField = javaClass.fields.find { it.name.asString() == "ERROR" }!!
        val eofField = javaClass.fields.find { it.name.asString() == "EOF" }!!
        val eolField = javaClass.fields.find { it.name.asString() == "EOL" }!!

        // All fields in a multi-field declaration share the same modifiers
        for (field in listOf(errorField, eofField, eolField)) {
            assert(field.isStatic) { "${field.name} should be static" }
            assert(field.isFinal) { "${field.name} should be final" }
            assert(field.visibility == org.jetbrains.kotlin.descriptors.Visibilities.Public) {
                "${field.name} should be public, got ${field.visibility}"
            }
        }

        // All fields share the same type (int)
        assert(eofField.type is JavaPrimitiveType) { "EOF type should be primitive, got ${eofField.type::class.simpleName}" }
        assert(eolField.type is JavaPrimitiveType) { "EOL type should be primitive, got ${eolField.type::class.simpleName}" }
    }

    @Test
    fun testVarargsParameterType() {
        val source = """
            import org.jspecify.annotations.NonNull;
            public class JavaClass {
                static JavaClass ofJspecify(@NonNull String... args) {
                    return new JavaClass();
                }
                static JavaClass ofRegular(@NonNull String arg) {
                    return new JavaClass();
                }
            }
        """.trimIndent()

        val javaClass = parseFirstClass(source)

        // Regular parameter: type should be JavaClassifierType (String) with annotation
        val regular = javaClass.methods.first { it.name.asString() == "ofRegular" }
        val regularParam = regular.valueParameters.first()
        assert(!regularParam.isVararg) { "Regular param should not be vararg" }
        val regularType = regularParam.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        val regularTypeAnnotations = regularType.annotations
        assert(regularTypeAnnotations.isNotEmpty()) {
            "Regular param type should have annotations (from modifier list), got empty. " +
            "Param annotations: ${regularParam.annotations.map { it.classId }}"
        }

        // Varargs parameter: type should be JavaArrayType (String[]) with annotation on component type
        val vararg = javaClass.methods.first { it.name.asString() == "ofJspecify" }
        val varargParam = vararg.valueParameters.first()
        assert(varargParam.isVararg) { "Vararg param should be vararg" }
        assert(varargParam.type is org.jetbrains.kotlin.load.java.structure.JavaArrayType) {
            "Vararg param type should be JavaArrayType, got ${varargParam.type::class.simpleName}"
        }
        val arrayType = varargParam.type as org.jetbrains.kotlin.load.java.structure.JavaArrayType
        val componentType = arrayType.componentType
        assert(componentType is org.jetbrains.kotlin.load.java.structure.JavaClassifierType) {
            "Vararg component type should be JavaClassifierType, got ${componentType::class.simpleName}"
        }

        // Annotations should be on the component type (String), not the array type (String[])
        val componentAnnotations = (componentType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType).annotations.toList()
        assert(componentAnnotations.any { it.classId?.asString()?.contains("NonNull") == true }) {
            "Expected @NonNull on component type (String), got: ${componentAnnotations.map { it.classId }}"
        }
        // Array type should NOT have member annotations for varargs
        val arrayAnnotations = arrayType.annotations.toList()
        assert(arrayAnnotations.none { it.classId?.asString()?.contains("NonNull") == true }) {
            "Array type should not have @NonNull for varargs, got: ${arrayAnnotations.map { it.classId }}"
        }
    }
}
