/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPrimitiveType
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class JavaParsingTest {

    @Test
    fun testBasicJavaParsing() {
        val source = "public final class A {}"
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassOverAst(it, source) }
        assert(javaClass.name.asString() == "A")
        assert(javaClass.isFinal)
        assert(!javaClass.isAbstract)
        assert(javaClass.visibility.toString() == "public")
    }

    @Test
    fun testAbstractInterface() {
        val source = "interface I {}"
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassOverAst(it, source) }
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
        val root = buildSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassOverAst(it, source) }

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
        val root = buildSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassOverAst(it, source) }

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
        val root = buildSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassOverAst(it, source) }

        assert(javaClass.fqName?.asString() == "com.example.A")
    }

    @Test
    fun testAnnotations() {
        val source = """
            @Deprecated
            class A {}
        """.trimIndent()
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        println(root.dump())
        val javaClass = root.children.first { it.type.toString() == "CLASS" }.let { JavaClassOverAst(it, source) }

        assert(javaClass.annotations.size == 1)
        assert(javaClass.annotations.first().classId?.asSingleFqName()?.asString() == "Deprecated")
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
    fun testLocalInheritance() {
        val source = """
            class Base {}
            class Derived extends Base {}
        """.trimIndent()
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        
        val localScope = LocalJavaScope(root, source)
        
        val classes = root.children.filter { it.type.toString() == "CLASS" }
        assert(classes.size == 2) { "Expected 2 classes, got ${classes.size}" }
        
        val base = JavaClassOverAst(classes[0], source, null, localScope)
        val derived = JavaClassOverAst(classes[1], source, null, localScope)
        
        assert(base.name.asString() == "Base")
        assert(derived.name.asString() == "Derived")
        
        assert(base.supertypes.isEmpty()) { "Base should have no supertypes" }
        assert(derived.supertypes.size == 1) { "Derived should have 1 supertype, got ${derived.supertypes.size}" }
        
        val supertype = derived.supertypes.first()
        assert(supertype.classifierQualifiedName == "Base") { "Expected Base, got ${supertype.classifierQualifiedName}" }
        
        val classifier = supertype.classifier
        assert(classifier != null) { "Expected classifier to be resolved" }
        assert(classifier is JavaClass) { "Expected JavaClass, got ${classifier?.javaClass}" }
        assert((classifier as JavaClass).name.asString() == "Base") { "Expected Base class, got ${classifier.name}" }
    }

    @Test
    fun testDefaultConstructor() {
        val sourceWithoutConstructor = """
            public class A {}
        """.trimIndent()
        val builder1 = parseJavaToSyntaxTreeBuilder(sourceWithoutConstructor, 0)
        val root1 = buildSyntaxTree(builder1, sourceWithoutConstructor)
        val classNode1 = root1.children.first { it.type.toString() == "CLASS" }
        val javaClass1 = JavaClassOverAst(classNode1, sourceWithoutConstructor)
        
        assert(javaClass1.constructors.isEmpty()) { "Expected no explicit constructors" }
        assert(javaClass1.hasDefaultConstructor()) { "Expected hasDefaultConstructor() = true for class without explicit constructor" }
        assert(!javaClass1.isInterface) { "A is not an interface" }
        
        val sourceWithConstructor = """
            public class B {
                public B() {}
            }
        """.trimIndent()
        val builder2 = parseJavaToSyntaxTreeBuilder(sourceWithConstructor, 0)
        val root2 = buildSyntaxTree(builder2, sourceWithConstructor)
        val classNode2 = root2.children.first { it.type.toString() == "CLASS" }
        val javaClass2 = JavaClassOverAst(classNode2, sourceWithConstructor)
        
        assert(javaClass2.constructors.size == 1) { "Expected 1 explicit constructor, got ${javaClass2.constructors.size}" }
        assert(!javaClass2.hasDefaultConstructor()) { "Expected hasDefaultConstructor() = false for class with explicit constructor" }
        
        val sourceInterface = """
            public interface I {}
        """.trimIndent()
        val builder3 = parseJavaToSyntaxTreeBuilder(sourceInterface, 0)
        val root3 = buildSyntaxTree(builder3, sourceInterface)
        val classNode3 = root3.children.first { it.type.toString() == "CLASS" }
        val javaClass3 = JavaClassOverAst(classNode3, sourceInterface)
        
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
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        val classNode = root.children.first { it.type.toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, source)
        
        assert(javaClass.methods.size == 1) { "Expected 1 method, got ${javaClass.methods.size}" }
        val method = javaClass.methods.first()
        assert(method.name.asString() == "method")
        
        val returnType = method.returnType
        assert(returnType is JavaPrimitiveType) { "Expected JavaPrimitiveType, got ${returnType::class.java}" }
        assert((returnType as JavaPrimitiveType).type == null) { "Expected type=null for void, got ${returnType.type}" }
    }

    @Test
    fun testClassifierQualifiedName() {
        val sourceSimpleName = """
            class Base {}
            class Derived extends Base {}
        """.trimIndent()
        val builder1 = parseJavaToSyntaxTreeBuilder(sourceSimpleName, 0)
        val root1 = buildSyntaxTree(builder1, sourceSimpleName)
        val localScope1 = LocalJavaScope(root1, sourceSimpleName)
        
        val derivedNode = root1.children.first { it.type.toString() == "CLASS" && it.findChildByType("IDENTIFIER")?.text == "Derived" }
        val derived = JavaClassOverAst(derivedNode, sourceSimpleName, null, localScope1)
        
        assert(derived.supertypes.size == 1) { "Expected 1 supertype" }
        val supertype = derived.supertypes.first()
        assert(supertype.classifierQualifiedName == "Base") { "Expected 'Base', got '${supertype.classifierQualifiedName}'" }
        assert(supertype.classifier != null) { "Base should be resolved via LocalJavaScope" }
        
        val sourceQualifiedName = """
            class MyClass extends java.util.ArrayList {}
        """.trimIndent()
        val builder2 = parseJavaToSyntaxTreeBuilder(sourceQualifiedName, 0)
        val root2 = buildSyntaxTree(builder2, sourceQualifiedName)
        val localScope2 = LocalJavaScope(root2, sourceQualifiedName)
        
        val myClassNode = root2.children.first { it.type.toString() == "CLASS" }
        val myClass = JavaClassOverAst(myClassNode, sourceQualifiedName, null, localScope2)
        
        assert(myClass.supertypes.size == 1) { "Expected 1 supertype" }
        val supertype2 = myClass.supertypes.first()
        assert(supertype2.classifierQualifiedName == "java.util.ArrayList") { "Expected 'java.util.ArrayList', got '${supertype2.classifierQualifiedName}'" }
        assert(supertype2.classifier == null) { "java.util.ArrayList should NOT be in LocalJavaScope" }
    }

    @Test
    fun testImportExtraction() {
        val source = """
            package test;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.concurrent.atomic.*;
            
            class MyClass extends ArrayList {
                List list;
                AtomicInteger counter;
            }
        """.trimIndent()
        
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        
        val imports = extractImports(root, source)
        
        assert(imports.simpleImports.size == 2) { "Expected 2 simple imports, got ${imports.simpleImports.size}: ${imports.simpleImports}" }
        assert(imports.simpleImports["ArrayList"]?.asString() == "java.util.ArrayList") { "Expected ArrayList -> java.util.ArrayList, got ${imports.simpleImports["ArrayList"]}" }
        assert(imports.simpleImports["List"]?.asString() == "java.util.List") { "Expected List -> java.util.List, got ${imports.simpleImports["List"]}" }
        
        assert(imports.starImports.size == 1) { "Expected 1 star import, got ${imports.starImports.size}: ${imports.starImports}" }
        assert(imports.starImports[0].asString() == "java.util.concurrent.atomic") { "Expected java.util.concurrent.atomic (without asterisk), got ${imports.starImports[0]}" }
        
        val pathSegments = imports.starImports[0].pathSegments()
        assert(pathSegments.size == 4) { "Expected 4 path segments, got ${pathSegments.size}: $pathSegments" }
        assert(pathSegments[0].asString() == "java") { "Expected 'java' as first segment" }
        assert(pathSegments[3].asString() == "atomic") { "Expected 'atomic' as last segment, got ${pathSegments[3]}" }
        
        val classNode = root.children.first { it.type.toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, source, null, LocalJavaScope(root, source), imports)
        
        assert(javaClass.supertypes.size == 1) { "Expected 1 supertype" }
        val supertype = javaClass.supertypes.first()
        assert(supertype.classifierQualifiedName == "java.util.ArrayList") { "Expected qualified name java.util.ArrayList, got ${supertype.classifierQualifiedName}" }
        
        val listField = javaClass.fields.first { it.name.asString() == "list" }
        val listType = listField.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(listType.classifierQualifiedName == "java.util.List") { "Expected qualified name java.util.List for list field, got ${listType.classifierQualifiedName}" }
        
        val counterField = javaClass.fields.first { it.name.asString() == "counter" }
        val counterType = counterField.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(counterType.classifierQualifiedName == "AtomicInteger") { "Expected simple name AtomicInteger for star import, got ${counterType.classifierQualifiedName}" }
    }

    @Test
    fun testKnownClassNamesInPackage(@TempDir tempDir: Path) {
        // Create test Java files in different packages
        val comExampleDir = tempDir.resolve("com/example")
        comExampleDir.toFile().mkdirs()
        comExampleDir.resolve("ClassA.java").writeText("""
            package com.example;
            public class ClassA {}
        """.trimIndent())
        comExampleDir.resolve("ClassB.java").writeText("""
            package com.example;
            public class ClassB {}
        """.trimIndent())

        val testDir = tempDir.resolve("test")
        testDir.toFile().mkdirs()
        testDir.resolve("ClassC.java").writeText("""
            package test;
            public class ClassC {}
        """.trimIndent())

        // Create JavaClassFinder with this source root
        val finder = JavaClassFinderOverAstImpl(listOf(tempDir))

        // Test package with classes - should return class names
        val comExampleClasses = finder.knownClassNamesInPackage(FqName("com.example"))
        assert(comExampleClasses != null) { "Expected non-null for package com.example" }
        assert(comExampleClasses!!.size == 2) { "Expected 2 classes in com.example, got ${comExampleClasses.size}" }
        assert("ClassA" in comExampleClasses) { "Expected ClassA in com.example" }
        assert("ClassB" in comExampleClasses) { "Expected ClassB in com.example" }

        val testClasses = finder.knownClassNamesInPackage(FqName("test"))
        assert(testClasses != null) { "Expected non-null for package test" }
        assert(testClasses!!.size == 1) { "Expected 1 class in test, got ${testClasses.size}" }
        assert("ClassC" in testClasses) { "Expected ClassC in test" }

        // Test package NOT in our index - should return empty set (not null)
        // This is critical: FIR expects empty set to mean "no Java classes here from this source"
        val kotlinPackageClasses = finder.knownClassNamesInPackage(FqName("kotlin"))
        assert(kotlinPackageClasses != null) { "Expected non-null (empty set) for package kotlin, got null" }
        assert(kotlinPackageClasses!!.isEmpty()) { "Expected empty set for package kotlin, got $kotlinPackageClasses" }

        val javaLangClasses = finder.knownClassNamesInPackage(FqName("java.lang"))
        assert(javaLangClasses != null) { "Expected non-null (empty set) for package java.lang, got null" }
        assert(javaLangClasses!!.isEmpty()) { "Expected empty set for package java.lang, got $javaLangClasses" }

        // Test non-existent package - should also return empty set
        val nonExistentClasses = finder.knownClassNamesInPackage(FqName("does.not.exist"))
        assert(nonExistentClasses != null) { "Expected non-null (empty set) for non-existent package, got null" }
        assert(nonExistentClasses!!.isEmpty()) { "Expected empty set for non-existent package" }
    }
}
