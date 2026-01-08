/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.junit.Test

class JavaParsingTest {

    @Test
    fun testBasicJavaParsing() {
        val source = "public final class A {}"
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildDirectSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassDirectImpl(it, source) }
        assert(javaClass.name.asString() == "A")
        assert(javaClass.isFinal)
        assert(!javaClass.isAbstract)
        assert(javaClass.visibility.toString() == "public")
    }

    @Test
    fun testAbstractInterface() {
        val source = "interface I {}"
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildDirectSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassDirectImpl(it, source) }
        assert(javaClass.name.asString() == "I")
        assert(javaClass.isInterface)
        assert(javaClass.isAbstract)
    }
    @Test
    fun testMembers() {
        val source = """
            class A {
                public int field;
                public void method() {}
                public A() {}
            }
        """.trimIndent()
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildDirectSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassDirectImpl(it, source) }
        
        assert(javaClass.fields.size == 1)
        assert(javaClass.fields.first().name.asString() == "field")
        
        assert(javaClass.methods.size == 1)
        assert(javaClass.methods.first().name.asString() == "method")
        
        assert(javaClass.constructors.size == 1)
        assert(javaClass.constructors.first().name.asString() == "A")
    }
    @Test
    fun testSupertypesAndTypeParameters() {
        val source = "class A<T> extends B implements C, D {}"
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildDirectSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassDirectImpl(it, source) }
        
        assert(javaClass.typeParameters.size == 1)
        assert(javaClass.typeParameters.first().name.asString() == "T")
        
        assert(javaClass.supertypes.size == 3)
        val supertypeNames = javaClass.supertypes.map { it.classifierQualifiedName }
        assert(supertypeNames.contains("B"))
        assert(supertypeNames.contains("C"))
        assert(supertypeNames.contains("D"))
    }
    @Test
    fun testPackageAndFqName() {
        val source = """
            package com.example;
            class A {}
        """.trimIndent()
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildDirectSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassDirectImpl(it, source) }
        
        assert(javaClass.fqName?.asString() == "com.example.A")
    }
    @Test
    fun testAnnotations() {
        val source = """
            @Deprecated
            class A {}
        """.trimIndent()
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildDirectSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassDirectImpl(it, source) }
        
        assert(javaClass.annotations.size == 1)
        assert(javaClass.annotations.first().classId?.asSingleFqName()?.asString() == "Deprecated")
    }
}
