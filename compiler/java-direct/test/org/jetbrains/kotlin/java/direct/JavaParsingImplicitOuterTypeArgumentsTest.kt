/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * The JLS-implicit type arguments of the enclosing instance, added for a bare reference to a
 * non-static inner class (`Inner` inside `Outer<T>` denotes `Outer<T>.Inner`).
 *
 * Such an argument must be the *declaring* class's own [org.jetbrains.kotlin.load.java.structure.JavaTypeParameter]
 * instance: FIR maps `JavaTypeParameter`s to `FirTypeParameterSymbol`s by object identity through
 * the per-class `JavaTypeParameterStack`, so a same-named parameter of a different declaration is
 * not merely a cosmetic difference — it substitutes a different symbol, silently.
 */
class JavaParsingImplicitOuterTypeArgumentsTest : JavaParsingTestBase() {

    @Test
    fun testImplicitOuterArgumentIsOuterClassParameter() {
        val source = """
            public class A<T> {
                class Inner {
                    Inner foo() { return null; }
                }
            }
        """.trimIndent()
        val a = parseFirstClass(source)
        val inner = a.findInnerClass(Name.identifier("Inner"))!!

        val args = inner.implicitOuterArgumentsOfReturnTypeOf("foo")
        assertEquals(1, args.size, "`Inner` denotes `A<T>.Inner`, so it has one implicit outer argument")
        assertSame(a.typeParameters[0], args[0], "The implicit outer argument must be A's own T")
    }

    @Test
    fun testImplicitOuterArgumentIsNotShadowedByInnerClassParameter() {
        // `Inner<String>` inside `Inner` denotes `A<A.T>.Inner<String>`: the nested `T` shadows the
        // outer one for name resolution, but the enclosing instance is still parameterized by A's T.
        val source = """
            public class A<T> {
                class Inner<T> {
                    Inner<String> foo() { return null; }
                }
            }
        """.trimIndent()
        val a = parseFirstClass(source)
        val inner = a.findInnerClass(Name.identifier("Inner"))!!

        val returnType = inner.returnTypeOf("foo")
        assertEquals(
            listOf("String", "T"),
            returnType.typeArguments.map { (it as JavaClassifierType).classifierQualifiedName },
            "Explicit argument first, then the implicit outer one",
        )

        val outerArgument = returnType.typeArguments[1]!!.classifierOfTypeParameter()
        assertSame(a.typeParameters[0], outerArgument, "The implicit outer argument must be A's T, not Inner's T")
    }

    @Test
    fun testImplicitOuterArgumentIsNotShadowedByMethodTypeParameter() {
        // A generic method whose parameter happens to be named like the outer class's one must not
        // hijack the implicit outer argument of inner-class types written in its signature.
        val source = """
            public class A<T> {
                class Inner { }
                <T> Inner foo() { return null; }
            }
        """.trimIndent()
        val a = parseFirstClass(source)

        val args = a.implicitOuterArgumentsOfReturnTypeOf("foo")
        assertEquals(1, args.size)
        assertSame(a.typeParameters[0], args[0], "The implicit outer argument must be A's T, not foo's T")
    }

    @Test
    fun testImplicitOuterArgumentsOfNestedOuterChain() {
        // Both enclosing levels contribute, innermost first, and neither is shadowed by `Inner`'s own `U`.
        val source = """
            public class A<T> {
                class Mid<U> {
                    class Inner<U> {
                        Inner<String> foo() { return null; }
                    }
                }
            }
        """.trimIndent()
        val a = parseFirstClass(source)
        val mid = a.findInnerClass(Name.identifier("Mid"))!!
        val inner = mid.findInnerClass(Name.identifier("Inner"))!!

        val args = inner.implicitOuterArgumentsOfReturnTypeOf("foo")
        assertEquals(2, args.size, "`Inner` denotes `A<T>.Mid<U>.Inner`, so both outer levels contribute")
        assertSame(mid.typeParameters[0], args[0], "First implicit outer argument must be Mid's own U")
        assertSame(a.typeParameters[0], args[1], "Second implicit outer argument must be A's T")
    }
}

private fun JavaClass.returnTypeOf(methodName: String): JavaClassifierType =
    methods.first { it.name.asString() == methodName }.returnType as JavaClassifierType

/**
 * The type-parameter arguments of the method's return type, in order. Explicitly written arguments
 * are class-like (and, in these tests, unresolved), so filtering on the classifier isolates the
 * implicit outer ones the model appends.
 */
private fun JavaClass.implicitOuterArgumentsOfReturnTypeOf(methodName: String): List<JavaTypeParameter> =
    returnTypeOf(methodName).typeArguments.mapNotNull { (it as? JavaClassifierType)?.classifier as? JavaTypeParameter }

private fun JavaType.classifierOfTypeParameter(): JavaTypeParameter =
    (this as JavaClassifierType).classifier as JavaTypeParameter
