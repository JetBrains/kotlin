/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.load.java.structure.JavaArrayType
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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

        assertEquals(1, javaClass.fields.size)
        assertEquals("field", javaClass.fields.first().name.asString())

        assertEquals(1, javaClass.methods.size)
        assertEquals("method", javaClass.methods.first().name.asString())

        assertEquals(1, javaClass.constructors.size)
        assertEquals("A", javaClass.constructors.first().name.asString())
    }

    @Test
    fun testDefaultConstructor() {
        val sourceWithoutConstructor = """
            public class A {}
        """.trimIndent()
        val javaClass1 = parseFirstClass(sourceWithoutConstructor)

        assertTrue(javaClass1.constructors.isEmpty(), "Expected no explicit constructors")
        assertTrue(javaClass1.hasDefaultConstructor(), "Expected hasDefaultConstructor() = true for class without explicit constructor")
        assertFalse(javaClass1.isInterface, "A is not an interface")

        val sourceWithConstructor = """
            public class B {
                public B() {}
            }
        """.trimIndent()
        val javaClass2 = parseFirstClass(sourceWithConstructor)

        assertEquals(1, javaClass2.constructors.size)
        assertFalse(javaClass2.hasDefaultConstructor(), "Expected hasDefaultConstructor() = false for class with explicit constructor")

        val sourceInterface = """
            public interface I {}
        """.trimIndent()
        val javaClass3 = parseFirstClass(sourceInterface)

        assertTrue(javaClass3.constructors.isEmpty(), "Expected no constructors for interface")
        assertFalse(javaClass3.hasDefaultConstructor(), "Expected hasDefaultConstructor() = false for interface")
        assertTrue(javaClass3.isInterface, "I should be an interface")
    }

    @Test
    fun testVoidReturnType() {
        val source = """
            public class A {
                public void method() {}
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assertEquals(1, javaClass.methods.size)
        val method = javaClass.methods.first()
        assertEquals("method", method.name.asString())

        val returnType = method.returnType
        assertTrue(returnType is JavaPrimitiveType, "Expected JavaPrimitiveType, got ${returnType::class.java}")
        assertNull((returnType as JavaPrimitiveType).type, "A void return type must be a JavaPrimitiveType with no primitive kind")
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
        assertTrue(method1.valueParameters.isEmpty(), "method1 should have 0 parameters, got ${method1.valueParameters.size}")

        val method2 = javaClass.methods.first { it.name.asString() == "method2" }
        assertEquals(1, method2.valueParameters.size)
        val param2 = method2.valueParameters.first()
        assertEquals("a", param2.name?.asString())
        assertTrue(param2.type is JavaPrimitiveType, "Expected int to be JavaPrimitiveType")

        val method3 = javaClass.methods.first { it.name.asString() == "method3" }
        assertEquals(3, method3.valueParameters.size)
        val params3 = method3.valueParameters.toList()
        assertEquals("a", params3[0].name?.asString())
        assertEquals("b", params3[1].name?.asString())
        assertEquals("c", params3[2].name?.asString())

        val paramAType = params3[0].type as JavaClassifierType
        assertEquals("String", paramAType.classifierQualifiedName)

        val paramBType = params3[1].type as JavaPrimitiveType
        assertEquals(PrimitiveType.INT, paramBType.type)

        val paramCType = params3[2].type as JavaClassifierType
        assertEquals("List", paramCType.classifierQualifiedName)

        val constructor0 = javaClass.constructors.first { it.valueParameters.isEmpty() }
        assertTrue(constructor0.valueParameters.isEmpty(), "Constructor should have 0 parameters")

        val constructor1 = javaClass.constructors.first { it.valueParameters.size == 1 }
        assertEquals(1, constructor1.valueParameters.size)
        val constParam1 = constructor1.valueParameters.first()
        assertEquals("x", constParam1.name?.asString())

        val constructor2 = javaClass.constructors.first { it.valueParameters.size == 2 }
        assertEquals(2, constructor2.valueParameters.size)
        val constParams2 = constructor2.valueParameters.toList()
        assertEquals("s", constParams2[0].name?.asString())
        assertEquals("o", constParams2[1].name?.asString())
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
        assertEquals(1, equalsMethod.valueParameters.size)

        val param = equalsMethod.valueParameters.first()
        assertEquals("o", param.name?.asString())

        val paramType = param.type as JavaClassifierType
        assertEquals("Object", paramType.classifierQualifiedName)
        assertNull(paramType.classifier, "Object should have null classifier without a wired symbol provider")
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
        assertTrue(nativeMethod.isNative, "nativeMethod should have isNative=true")
        assertFalse(normalMethod.isNative, "normalMethod should have isNative=false")
    }

    @Test
    fun testConstructorImplicitFinal() {
        val source = "public class Foo { public Foo() {} }"
        val javaClass = parseFirstClass(source)
        val ctor = javaClass.constructors.single()
        assertTrue(ctor.isFinal, "Constructor should be implicitly final")
        assertFalse(ctor.isAbstract, "Constructor should not be abstract")
        assertFalse(ctor.isStatic, "Constructor should not be static")
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
            assertTrue(field.isStatic, "${field.name} should be static")
            assertTrue(field.isFinal, "${field.name} should be final")
            assertEquals(org.jetbrains.kotlin.descriptors.Visibilities.Public, field.visibility, "${field.name} should be public")
        }

        // All fields share the same type (int)
        assertTrue(eofField.type is JavaPrimitiveType, "EOF type should be primitive, got ${eofField.type::class.simpleName}")
        assertTrue(eolField.type is JavaPrimitiveType, "EOL type should be primitive, got ${eolField.type::class.simpleName}")
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

        // Regular parameter: structural shape only — type is JavaClassifierType for `String`.
        // Annotation placement on the parameter's *type* is not assertable in parsing-only mode:
        // `JavaTypeOverAst.annotations` pre-filters member annotations via
        // `JavaResolutionContext.isTypeUseAnnotationClass`, which needs a session with a
        // `FirSymbolProvider` to resolve `@Target`. The dummy session used by `parseFirstClass`
        // has none, so the filter drops every member annotation. The parser itself still captures
        // the annotation correctly — see `regularParam.annotations` below — and the end-to-end
        // propagation contract is covered by the `JavaUsingAst*` integration suite, which runs
        // against a full FIR session.
        val regular = javaClass.methods.first { it.name.asString() == "ofRegular" }
        val regularParam = regular.valueParameters.first()
        assertFalse(regularParam.isVararg, "Regular param should not be vararg")
        assertTrue(
            regularParam.type is JavaClassifierType,
            "Regular param type should be JavaClassifierType, got ${regularParam.type::class.simpleName}"
        )
        assertTrue(
            regularParam.annotations.any { it.classId?.asString()?.contains("NonNull") == true },
            "Parser should capture @NonNull on the parameter, got: ${regularParam.annotations.map { it.classId }}"
        )

        // Varargs parameter: type should be JavaArrayType (String[]) with a JavaClassifierType
        // component (String). Component-vs-array annotation placement is again covered by
        // `JavaUsingAst*` integration tests rather than this parsing-only test (see comment above).
        val vararg = javaClass.methods.first { it.name.asString() == "ofJspecify" }
        val varargParam = vararg.valueParameters.first()
        assertTrue(varargParam.isVararg, "Vararg param should be vararg")
        assertTrue(
            varargParam.type is JavaArrayType,
            "Vararg param type should be JavaArrayType, got ${varargParam.type::class.simpleName}"
        )
        val arrayType = varargParam.type as JavaArrayType
        val componentType = arrayType.componentType
        assertTrue(
            componentType is JavaClassifierType,
            "Vararg component type should be JavaClassifierType, got ${componentType::class.simpleName}"
        )
        assertTrue(
            varargParam.annotations.any { it.classId?.asString()?.contains("NonNull") == true },
            "Parser should capture @NonNull on the vararg parameter, got: ${varargParam.annotations.map { it.classId }}"
        )
    }
}
