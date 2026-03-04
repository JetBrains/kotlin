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

    private fun parseSource(source: String): Pair<JavaSyntaxNode, JavaResolutionContext> {
        val builder = parseJavaToSyntaxTreeBuilder(source, 0)
        val root = buildSyntaxTree(builder, source)
        val context = JavaResolutionContext.create(root)
        return root to context
    }

    private fun parseFirstClass(source: String): JavaClassOverAst {
        val (root, context) = parseSource(source)
        val classNode = root.children.first { it.type.toString() == "CLASS" }
        return JavaClassOverAst(classNode, context)
    }

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
    fun testSupertypesAndTypeParameters() {
        val source = "class A<T> extends B implements C, D {}"
        val javaClass = parseFirstClass(source)

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
        val javaClass = parseFirstClass(source)
        assert(javaClass.fqName?.asString() == "com.example.A")
    }

    @Test
    fun testAnnotations() {
        val source = """
            @Deprecated
            class A {}
        """.trimIndent()
        val javaClass = parseFirstClass(source)

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
        val (root, context) = parseSource(source)

        val classes = root.children.filter { it.type.toString() == "CLASS" }
        assert(classes.size == 2) { "Expected 2 classes, got ${classes.size}" }

        val base = JavaClassOverAst(classes[0], context)
        val derived = JavaClassOverAst(classes[1], context)

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
    fun testClassifierQualifiedName() {
        val sourceSimpleName = """
            class Base {}
            class Derived extends Base {}
        """.trimIndent()
        val (root1, context1) = parseSource(sourceSimpleName)

        val derivedNode = root1.children.first { it.type.toString() == "CLASS" && it.findChildByType("IDENTIFIER")?.text == "Derived" }
        val derived = JavaClassOverAst(derivedNode, context1)

        assert(derived.supertypes.size == 1) { "Expected 1 supertype" }
        val supertype = derived.supertypes.first()
        assert(supertype.classifierQualifiedName == "Base") { "Expected 'Base', got '${supertype.classifierQualifiedName}'" }
        assert(supertype.classifier != null) { "Base should be resolved via local scope" }

        val sourceQualifiedName = """
            class MyClass extends java.util.ArrayList {}
        """.trimIndent()
        val myClass = parseFirstClass(sourceQualifiedName)

        assert(myClass.supertypes.size == 1) { "Expected 1 supertype" }
        val supertype2 = myClass.supertypes.first()
        assert(supertype2.classifierQualifiedName == "java.util.ArrayList") { "Expected 'java.util.ArrayList', got '${supertype2.classifierQualifiedName}'" }
        assert(supertype2.classifier == null) { "java.util.ArrayList should NOT be in local scope" }
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

        val javaClass = parseFirstClass(source)

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
    fun testTypeNameStripsTypeArguments() {
        val source = """
            import java.util.List;
            class A {
                List<String> a;
                java.util.Map<String, Integer> b;
                Object[] c;
            }
        """.trimIndent()

        val javaClass = parseFirstClass(source)

        val fieldA = javaClass.fields.first { it.name.asString() == "a" }
        val typeA = fieldA.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(typeA.classifierQualifiedName == "java.util.List") {
            "Expected qualified name java.util.List for List<String>, got ${typeA.classifierQualifiedName}"
        }

        val fieldB = javaClass.fields.first { it.name.asString() == "b" }
        val typeB = fieldB.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(typeB.classifierQualifiedName == "java.util.Map") {
            "Expected qualified name java.util.Map for java.util.Map<String, Integer>, got ${typeB.classifierQualifiedName}"
        }

        val fieldC = javaClass.fields.first { it.name.asString() == "c" }
        val typeC = fieldC.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(typeC.classifierQualifiedName == "Object") {
            "Expected raw name Object for Object[], got ${typeC.classifierQualifiedName}"
        }
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

    @Test
    fun testTypeResolution() {
        val source = """
            public class MyClass {
                public Object field;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assert(javaClass.fields.size == 1) { "Expected 1 field, got ${javaClass.fields.size}" }
        val field = javaClass.fields.first()
        val fieldType = field.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType

        assert(fieldType.classifierQualifiedName == "Object") { "Expected 'Object', got '${fieldType.classifierQualifiedName}'" }
        assert(!fieldType.isResolved) { "Expected isResolved=false for unqualified Object" }
        assert(fieldType.classifier == null) { "Expected classifier=null for external type" }

        val resolved = fieldType.resolve { candidateFqn ->
            candidateFqn == "java.lang.Object"
        }

        assert(resolved == "java.lang.Object") { "Expected resolution to 'java.lang.Object', got '$resolved'" }
    }

    @Test
    fun testLocalTypeResolutionInMembers() {
        val source = """
            public class A {
                public class B {}
                public B field;
                public B method() { return null; }
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val field = javaClass.fields.first { it.name.asString() == "field" }
        val fieldType = field.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(fieldType.classifier != null) { "Field type 'B' should have resolved classifier" }
        assert(fieldType.classifier?.name?.asString() == "B") { "Field type classifier should be 'B'" }

        val method = javaClass.methods.first { it.name.asString() == "method" }
        val returnType = method.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(returnType.classifier != null) { "Method return type 'B' should have resolved classifier" }
        assert(returnType.classifier?.name?.asString() == "B") { "Method return type classifier should be 'B'" }
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

        val resolved = paramType.resolve { candidateFqn ->
            candidateFqn == "java.lang.Object"
        }

        assert(resolved == "java.lang.Object") { "Expected 'java.lang.Object', got '$resolved'" }
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
    fun testNestedClassResolution() {
        val source = """
            public class Outer {
                public class Inner {
                    public class Deep {
                    }
                }
                
                public Inner field1;
                public Outer.Inner field2;
                public Outer.Inner.Deep field3;
                public Inner.Deep field4;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val field1 = javaClass.fields.first { it.name.asString() == "field1" }
        val type1 = field1.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(type1.classifier != null) { "field1 type 'Inner' should resolve" }
        assert(type1.classifier?.name?.asString() == "Inner") { "field1 type should be 'Inner'" }

        val field2 = javaClass.fields.first { it.name.asString() == "field2" }
        val type2 = field2.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(type2.classifier != null) { "field2 type 'Outer.Inner' should resolve" }
        assert(type2.classifier?.name?.asString() == "Inner") { "field2 type should be 'Inner'" }

        val field3 = javaClass.fields.first { it.name.asString() == "field3" }
        val type3 = field3.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(type3.classifier != null) { "field3 type 'Outer.Inner.Deep' should resolve" }
        assert(type3.classifier?.name?.asString() == "Deep") { "field3 type should be 'Deep'" }

        val field4 = javaClass.fields.first { it.name.asString() == "field4" }
        val type4 = field4.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(type4.classifier != null) { "field4 type 'Inner.Deep' should resolve" }
        assert(type4.classifier?.name?.asString() == "Deep") { "field4 type should be 'Deep'" }
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
    fun testSimpleTypeArguments() {
        val source = """
            import java.util.List;
            import java.util.Map;
            
            public class MyClass {
                public List<String> items;
                public List<Object> objects;
                public Map<String, Integer> map;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val items = javaClass.fields.first { it.name.asString() == "items" }
        val itemsType = items.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(itemsType.classifierQualifiedName == "java.util.List") { "Expected 'java.util.List', got ${itemsType.classifierQualifiedName}" }
        assert(itemsType.typeArguments.size == 1) { "Expected 1 type argument, got ${itemsType.typeArguments.size}" }
        val stringArg = itemsType.typeArguments[0] as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(stringArg.classifierQualifiedName == "String") { "Expected 'String', got ${stringArg.classifierQualifiedName}" }
        assert(!stringArg.isResolved) { "String should not be resolved (needs FIR)" }

        val objects = javaClass.fields.first { it.name.asString() == "objects" }
        val objectsType = objects.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(objectsType.typeArguments.size == 1) { "Expected 1 type argument, got ${objectsType.typeArguments.size}" }
        val objectArg = objectsType.typeArguments[0] as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(objectArg.classifierQualifiedName == "Object") { "Expected 'Object', got ${objectArg.classifierQualifiedName}" }
        assert(!objectArg.isResolved) { "Object should not be resolved (needs FIR)" }

        val map = javaClass.fields.first { it.name.asString() == "map" }
        val mapType = map.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(mapType.classifierQualifiedName == "java.util.Map") { "Expected 'java.util.Map', got ${mapType.classifierQualifiedName}" }
        assert(mapType.typeArguments.size == 2) { "Expected 2 type arguments, got ${mapType.typeArguments.size}" }
        val keyArg = mapType.typeArguments[0] as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(keyArg.classifierQualifiedName == "String") { "Expected 'String', got ${keyArg.classifierQualifiedName}" }
        val valueArg = mapType.typeArguments[1] as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(valueArg.classifierQualifiedName == "Integer") { "Expected 'Integer', got ${valueArg.classifierQualifiedName}" }
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
    fun testClassFinderWithPackage() {
        // Create temporary files with Java classes in packages
        val tempDir = kotlin.io.path.createTempDirectory("java-direct-test")
        try {
            val helloFile = tempDir.resolve("Hello.java")
            java.nio.file.Files.writeString(helloFile, """
                package example;
                
                public class Hello {
                    public void greet() {}
                }
            """.trimIndent())

            val finder = JavaClassFinderOverAstImpl(listOf(helloFile))

            // Try to find example.Hello
            val classId = org.jetbrains.kotlin.name.ClassId(
                org.jetbrains.kotlin.name.FqName("example"),
                org.jetbrains.kotlin.name.Name.identifier("Hello")
            )
            val request = org.jetbrains.kotlin.load.java.JavaClassFinder.Request(classId)
            val javaClass = finder.findClass(request)

            assert(javaClass != null) { "Expected to find example.Hello class" }
            assert(javaClass?.name?.asString() == "Hello") { "Expected class name 'Hello', got ${javaClass?.name?.asString()}" }
            assert(javaClass?.fqName?.asString() == "example.Hello") { "Expected fqName 'example.Hello', got ${javaClass?.fqName?.asString()}" }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
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
