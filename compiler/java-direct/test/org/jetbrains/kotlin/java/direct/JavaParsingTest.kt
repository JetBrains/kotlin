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
        // Unit test parses without FIR, so annotation is unresolved (just "Deprecated")
        // FIR will resolve it to java.lang.Deprecated via resolveAnnotation
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

        // Base has implicit java.lang.Object supertype
        assert(base.supertypes.size == 1) { "Base should have 1 supertype (implicit Object), got ${base.supertypes.size}" }
        assert(base.supertypes.first().classifierQualifiedName == "java.lang.Object") { "Base should extend Object" }
        
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
        val typeC = fieldC.type as org.jetbrains.kotlin.load.java.structure.JavaArrayType
        val componentType = typeC.componentType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(componentType.classifierQualifiedName == "Object") {
            "Expected component type Object for Object[], got ${componentType.classifierQualifiedName}"
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
        val nestedInterface = outerClass.findInnerClass(org.jetbrains.kotlin.name.Name.identifier("I"))
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

        val paramType = getMethod.valueParameters.first().type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
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

        val nestedInterface = outerClass.findInnerClass(org.jetbrains.kotlin.name.Name.identifier("NestedInterface"))
        assert(nestedInterface != null) { "Should find NestedInterface" }
        // Interfaces are implicitly static in Java
        assert(nestedInterface!!.isInterface) { "NestedInterface should be an interface" }

        val nestedStaticClass = outerClass.findInnerClass(org.jetbrains.kotlin.name.Name.identifier("NestedStaticClass"))
        assert(nestedStaticClass != null) { "Should find NestedStaticClass" }
        assert(nestedStaticClass!!.isStatic) { "NestedStaticClass should be explicitly static" }

        val nestedInnerClass = outerClass.findInnerClass(org.jetbrains.kotlin.name.Name.identifier("NestedInnerClass"))
        assert(nestedInnerClass != null) { "Should find NestedInnerClass" }
        assert(!nestedInnerClass!!.isStatic) { "NestedInnerClass should NOT be static" }
    }

    @Test
    fun testAnnotatedTypeArguments() {
        // Test TYPE_USE annotations on type arguments like List<@NotNull Integer>
        val source = """
            import java.util.List;
            import org.jetbrains.annotations.NotNull;
            
            public class MyClass {
                public List<@NotNull Integer> items;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val field = javaClass.fields.first { it.name.asString() == "items" }
        val fieldType = field.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        
        assert(fieldType.classifierQualifiedName == "java.util.List") { 
            "Expected 'java.util.List', got ${fieldType.classifierQualifiedName}" 
        }
        assert(fieldType.typeArguments.size == 1) { 
            "Expected 1 type argument, got ${fieldType.typeArguments.size}" 
        }
        
        val typeArg = fieldType.typeArguments[0] as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(typeArg.classifierQualifiedName == "Integer") { 
            "Expected 'Integer', got ${typeArg.classifierQualifiedName}" 
        }
        
        // TYPE_USE annotation @NotNull should be on the type argument
        assert(typeArg.annotations.size == 1) { 
            "Expected 1 annotation on type argument, got ${typeArg.annotations.size}: ${typeArg.annotations.map { it.classId }}" 
        }
        val annotation = typeArg.annotations.first()
        assert(annotation.classId?.shortClassName?.asString() == "NotNull") { 
            "Expected @NotNull annotation, got ${annotation.classId}" 
        }
    }

    @Test
    fun testAnnotatedTypeArgumentsMultiple() {
        // Test multiple TYPE_USE annotations on type arguments like Map<@NotNull String, @Nullable Integer>
        val source = """
            import java.util.Map;
            import org.jetbrains.annotations.NotNull;
            import org.jetbrains.annotations.Nullable;
            
            public class MyClass {
                public Map<@NotNull String, @Nullable Integer> map;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val field = javaClass.fields.first { it.name.asString() == "map" }
        val fieldType = field.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        
        assert(fieldType.classifierQualifiedName == "java.util.Map") { 
            "Expected 'java.util.Map', got ${fieldType.classifierQualifiedName}" 
        }
        assert(fieldType.typeArguments.size == 2) { 
            "Expected 2 type arguments, got ${fieldType.typeArguments.size}" 
        }
        
        // First type argument: @NotNull String
        val keyArg = fieldType.typeArguments[0] as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(keyArg.classifierQualifiedName == "String") { 
            "Expected 'String', got ${keyArg.classifierQualifiedName}" 
        }
        assert(keyArg.annotations.size == 1) { 
            "Expected 1 annotation on key type argument, got ${keyArg.annotations.size}" 
        }
        assert(keyArg.annotations.first().classId?.shortClassName?.asString() == "NotNull") { 
            "Expected @NotNull annotation on key" 
        }
        
        // Second type argument: @Nullable Integer
        val valueArg = fieldType.typeArguments[1] as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(valueArg.classifierQualifiedName == "Integer") { 
            "Expected 'Integer', got ${valueArg.classifierQualifiedName}" 
        }
        assert(valueArg.annotations.size == 1) { 
            "Expected 1 annotation on value type argument, got ${valueArg.annotations.size}" 
        }
        assert(valueArg.annotations.first().classId?.shortClassName?.asString() == "Nullable") { 
            "Expected @Nullable annotation on value" 
        }
    }

    @Test
    fun testAnnotatedTypeArgumentInMethodReturn() {
        // Test TYPE_USE annotation on method return type argument
        val source = """
            import java.util.List;
            import org.jetbrains.annotations.NotNull;
            
            public class MyClass {
                public List<@NotNull Integer> getItems() { return null; }
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val method = javaClass.methods.first { it.name.asString() == "getItems" }
        val returnType = method.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        
        assert(returnType.classifierQualifiedName == "java.util.List") { 
            "Expected 'java.util.List', got ${returnType.classifierQualifiedName}" 
        }
        assert(returnType.typeArguments.size == 1) { 
            "Expected 1 type argument, got ${returnType.typeArguments.size}" 
        }
        
        val typeArg = returnType.typeArguments[0] as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(typeArg.annotations.size == 1) { 
            "Expected 1 annotation on type argument, got ${typeArg.annotations.size}" 
        }
        assert(typeArg.annotations.first().classId?.shortClassName?.asString() == "NotNull") { 
            "Expected @NotNull annotation" 
        }
    }

    @Test
    fun testAnnotatedTypeArgumentInMethodParameter() {
        // Test TYPE_USE annotation on method parameter type argument
        val source = """
            import java.util.List;
            import org.jetbrains.annotations.NotNull;
            
            public class MyClass {
                public void process(List<@NotNull String> items) { }
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val method = javaClass.methods.first { it.name.asString() == "process" }
        val param = method.valueParameters.first()
        val paramType = param.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        
        assert(paramType.classifierQualifiedName == "java.util.List") { 
            "Expected 'java.util.List', got ${paramType.classifierQualifiedName}" 
        }
        assert(paramType.typeArguments.size == 1) { 
            "Expected 1 type argument, got ${paramType.typeArguments.size}" 
        }
        
        val typeArg = paramType.typeArguments[0] as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(typeArg.annotations.size == 1) { 
            "Expected 1 annotation on type argument, got ${typeArg.annotations.size}" 
        }
        assert(typeArg.annotations.first().classId?.shortClassName?.asString() == "NotNull") { 
            "Expected @NotNull annotation" 
        }
    }

    @Test
    fun testUnannotatedTypeArgument() {
        // Verify that type arguments without annotations have empty annotations
        val source = """
            import java.util.List;
            
            public class MyClass {
                public List<String> items;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val field = javaClass.fields.first { it.name.asString() == "items" }
        val fieldType = field.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        
        val typeArg = fieldType.typeArguments[0] as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(typeArg.annotations.isEmpty()) { 
            "Expected no annotations on type argument, got ${typeArg.annotations.size}" 
        }
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
        val nestedInterface = outerClass.findInnerClass(org.jetbrains.kotlin.name.Name.identifier("NestedInterface"))
        assert(nestedInterface != null) { "Should find NestedInterface" }
        assert(nestedInterface!!.isInterface) { "NestedInterface should be an interface" }
        assert(nestedInterface.isStatic) { "Nested interface should be implicitly static for FIR isInner=false" }
        assert(nestedInterface.outerClass == outerClass) { "Nested interface should have outer class reference" }

        // Nested enum should be implicitly static
        val nestedEnum = outerClass.findInnerClass(org.jetbrains.kotlin.name.Name.identifier("NestedEnum"))
        assert(nestedEnum != null) { "Should find NestedEnum" }
        assert(nestedEnum!!.isEnum) { "NestedEnum should be an enum" }
        assert(nestedEnum.isStatic) { "Nested enum should be implicitly static for FIR isInner=false" }

        // Inner class (without static keyword) should NOT be static
        val innerClass = outerClass.findInnerClass(org.jetbrains.kotlin.name.Name.identifier("InnerClass"))
        assert(innerClass != null) { "Should find InnerClass" }
        assert(!innerClass!!.isInterface) { "InnerClass should not be an interface" }
        assert(!innerClass.isEnum) { "InnerClass should not be an enum" }
        assert(!innerClass.isStatic) { "Inner class without 'static' keyword should NOT be static" }
    }

    @Test
    fun testCovariantWildcardReturnType() {
        // Test for the inheritanceWithWildcard pattern:
        // Interface A with method returning X<? extends A>
        // Interface B extends A with covariant override returning Y<? extends B>
        val source = """
            interface A {
                X<? extends A> foo();
                interface X<T extends A> {}
            }
            
            interface B extends A {
                @Override
                Y<? extends B> foo();
                interface Y<U extends B> extends A.X<U> {}
            }
            
            class BImpl implements B {
                @Override
                public B.Y<? extends B> foo() { return null; }
            }
        """.trimIndent()
        val (root, context) = parseSource(source)

        val classes = root.children.filter { it.type.toString() == "CLASS" }
        assert(classes.size == 3) { "Expected 3 classes (A, B, BImpl), got ${classes.size}" }

        // Find interface A
        val interfaceANode = classes.first { it.findChildByType("IDENTIFIER")?.text == "A" }
        val interfaceA = JavaClassOverAst(interfaceANode, context)
        assert(interfaceA.isInterface) { "A should be an interface" }
        
        // Check A.foo() return type
        val aFoo = interfaceA.methods.first { it.name.asString() == "foo" }
        val aFooReturnType = aFoo.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(aFooReturnType.classifierQualifiedName == "A.X") { 
            "A.foo() should return A.X, got ${aFooReturnType.classifierQualifiedName}" 
        }
        assert(aFooReturnType.typeArguments.size == 1) { 
            "A.X should have 1 type argument, got ${aFooReturnType.typeArguments.size}" 
        }
        
        // Check the wildcard type argument
        val aWildcard = aFooReturnType.typeArguments[0]
        assert(aWildcard is org.jetbrains.kotlin.load.java.structure.JavaWildcardType) { 
            "Expected JavaWildcardType, got ${aWildcard?.javaClass}" 
        }
        val aWildcardType = aWildcard as org.jetbrains.kotlin.load.java.structure.JavaWildcardType
        assert(aWildcardType.isExtends) { "Should be '? extends'" }
        assert(aWildcardType.bound != null) { "Wildcard should have a bound" }
        val aBound = aWildcardType.bound as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(aBound.classifierQualifiedName == "A") { 
            "Wildcard bound should be A, got ${aBound.classifierQualifiedName}" 
        }
        
        // Find interface B
        val interfaceBNode = classes.first { it.findChildByType("IDENTIFIER")?.text == "B" }
        val interfaceB = JavaClassOverAst(interfaceBNode, context)
        assert(interfaceB.isInterface) { "B should be an interface" }
        
        // Check B.foo() return type
        val bFoo = interfaceB.methods.first { it.name.asString() == "foo" }
        val bFooReturnType = bFoo.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(bFooReturnType.classifierQualifiedName == "B.Y") { 
            "B.foo() should return B.Y, got ${bFooReturnType.classifierQualifiedName}" 
        }
        assert(bFooReturnType.typeArguments.size == 1) { 
            "B.Y should have 1 type argument, got ${bFooReturnType.typeArguments.size}" 
        }
        
        // Check the wildcard type argument
        val bWildcard = bFooReturnType.typeArguments[0]
        assert(bWildcard is org.jetbrains.kotlin.load.java.structure.JavaWildcardType) { 
            "Expected JavaWildcardType, got ${bWildcard?.javaClass}" 
        }
        val bWildcardType = bWildcard as org.jetbrains.kotlin.load.java.structure.JavaWildcardType
        assert(bWildcardType.isExtends) { "Should be '? extends'" }
        assert(bWildcardType.bound != null) { "Wildcard should have a bound" }
        val bBound = bWildcardType.bound as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(bBound.classifierQualifiedName == "B") { 
            "Wildcard bound should be B, got ${bBound.classifierQualifiedName}" 
        }

        // Find class BImpl
        val bImplNode = classes.first { it.findChildByType("IDENTIFIER")?.text == "BImpl" }
        val bImpl = JavaClassOverAst(bImplNode, context)
        assert(!bImpl.isInterface) { "BImpl should be a class" }
        
        // Check BImpl.foo() return type
        val bImplFoo = bImpl.methods.first { it.name.asString() == "foo" }
        val bImplFooReturnType = bImplFoo.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(bImplFooReturnType.classifierQualifiedName == "B.Y") { 
            "BImpl.foo() should return B.Y, got ${bImplFooReturnType.classifierQualifiedName}" 
        }
        assert(bImplFooReturnType.typeArguments.size == 1) { 
            "B.Y should have 1 type argument, got ${bImplFooReturnType.typeArguments.size}" 
        }
        
        // Check the wildcard type argument
        val bImplWildcard = bImplFooReturnType.typeArguments[0]
        assert(bImplWildcard is org.jetbrains.kotlin.load.java.structure.JavaWildcardType) { 
            "Expected JavaWildcardType, got ${bImplWildcard?.javaClass}" 
        }
        val bImplWildcardType = bImplWildcard as org.jetbrains.kotlin.load.java.structure.JavaWildcardType
        assert(bImplWildcardType.isExtends) { "Should be '? extends'" }
        assert(bImplWildcardType.bound != null) { "Wildcard should have a bound" }
        val bImplBound = bImplWildcardType.bound as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(bImplBound.classifierQualifiedName == "B") { 
            "Wildcard bound should be B, got ${bImplBound.classifierQualifiedName}" 
        }
        
        // Check that nested interface B.Y properly extends A.X
        val nestedY = interfaceB.findInnerClass(org.jetbrains.kotlin.name.Name.identifier("Y"))
        assert(nestedY != null) { "Should find nested interface Y in B" }
        assert(nestedY!!.isInterface) { "Y should be an interface" }
        assert(nestedY.supertypes.size == 1) { "Y should have 1 supertype (A.X), got ${nestedY.supertypes.size}" }

        val ySupertype = nestedY.supertypes.first()
        // Y extends A.X<U>, so supertype should be A.X with type argument U
        assert(ySupertype.classifierQualifiedName == "A.X") {
            "Y's supertype should be A.X, got ${ySupertype.classifierQualifiedName}"
        }

        // Check that classifier is resolved for the return types
        // This is important for FIR to properly match method signatures
        assert(aFooReturnType.classifier != null) { "A.foo() return type classifier should be resolved" }
        assert(aFooReturnType.classifier == interfaceA.findInnerClass(org.jetbrains.kotlin.name.Name.identifier("X"))) {
            "A.foo() return type should resolve to A.X"
        }

        assert(bFooReturnType.classifier != null) { "B.foo() return type classifier should be resolved" }
        assert(bFooReturnType.classifier == nestedY) {
            "B.foo() return type should resolve to B.Y"
        }

        assert(bImplFooReturnType.classifier != null) { "BImpl.foo() return type classifier should be resolved" }
        assert(bImplFooReturnType.classifier == nestedY) {
            "BImpl.foo() return type should resolve to B.Y"
        }
    }

    @Test
    fun testUnboundedWildcard() {
        // Test unbounded wildcard (?) which should have isExtends=true and bound=null
        val source = """
            import java.util.List;
            
            public class MyClass {
                public List<?> items;
                public List<? extends Object> explicitExtends;
                public List<? super String> superWildcard;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        // Test unbounded wildcard: List<?>
        val itemsField = javaClass.fields.first { it.name.asString() == "items" }
        val itemsType = itemsField.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(itemsType.typeArguments.size == 1) { "List should have 1 type argument" }

        val unboundedWildcard = itemsType.typeArguments[0]
        assert(unboundedWildcard is org.jetbrains.kotlin.load.java.structure.JavaWildcardType) {
            "Expected JavaWildcardType for ?, got ${unboundedWildcard?.javaClass}"
        }
        val unboundedType = unboundedWildcard as org.jetbrains.kotlin.load.java.structure.JavaWildcardType
        assert(unboundedType.isExtends) { "Unbounded wildcard should have isExtends=true" }
        assert(unboundedType.bound == null) { "Unbounded wildcard should have bound=null, got ${unboundedType.bound}" }

        // Test explicit extends Object: List<? extends Object>
        val extendsField = javaClass.fields.first { it.name.asString() == "explicitExtends" }
        val extendsType = extendsField.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        val extendsWildcard = extendsType.typeArguments[0] as org.jetbrains.kotlin.load.java.structure.JavaWildcardType
        assert(extendsWildcard.isExtends) { "? extends Object should have isExtends=true" }
        assert(extendsWildcard.bound != null) { "? extends Object should have a bound" }
        val extendsBound = extendsWildcard.bound as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(extendsBound.classifierQualifiedName == "Object") {
            "Bound should be Object, got ${extendsBound.classifierQualifiedName}"
        }

        // Test super wildcard: List<? super String>
        val superField = javaClass.fields.first { it.name.asString() == "superWildcard" }
        val superType = superField.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        val superWildcard = superType.typeArguments[0] as org.jetbrains.kotlin.load.java.structure.JavaWildcardType
        assert(!superWildcard.isExtends) { "? super String should have isExtends=false" }
        assert(superWildcard.bound != null) { "? super String should have a bound" }
        val superBound = superWildcard.bound as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(superBound.classifierQualifiedName == "String") {
            "Bound should be String, got ${superBound.classifierQualifiedName}"
        }
    }

    @Test
    fun testAnnotationPositionOnMethodReturnType() {
        // Test 1: Annotation before modifiers (method annotation position)
        val source1 = """
            import org.jetbrains.annotations.Nullable;
            class A {
                @Nullable public String method1() { return null; }
            }
        """.trimIndent()

        // Test 2: Annotation after modifiers (type annotation position)
        val source2 = """
            import org.jetbrains.annotations.Nullable;
            class A {
                public @Nullable String method2() { return null; }
            }
        """.trimIndent()

        fun printTree(node: JavaSyntaxNode, indent: String = "") {
            println("$indent${node.type}: '${node.text.take(60).replace("\n", "\\n")}'")
            for (child in node.children) {
                printTree(child, "$indent  ")
            }
        }

        println("\n=== Test 1: @Nullable public String method1() ===")
        val (root1, _) = parseSource(source1)
        val classNode1 = root1.children.first { it.type.toString() == "CLASS" }
        val methodNode1 = classNode1.findChildByType("METHOD")!!
        println("METHOD structure:")
        printTree(methodNode1)

        println("\n=== Test 2: public @Nullable String method2() ===")
        val (root2, _) = parseSource(source2)
        val classNode2 = root2.children.first { it.type.toString() == "CLASS" }
        val methodNode2 = classNode2.findChildByType("METHOD")!!
        println("METHOD structure:")
        printTree(methodNode2)

        // Now check where annotations end up in the JavaType
        val javaClass1 = parseFirstClass(source1)
        val method1 = javaClass1.methods.first()
        val returnType1 = method1.returnType
        println("\n=== method1 return type annotations: ${returnType1.annotations.map { it.classId }} ===")
        println("=== method1 (member) annotations: ${method1.annotations.map { it.classId }} ===")

        val javaClass2 = parseFirstClass(source2)
        val method2 = javaClass2.methods.first()
        val returnType2 = method2.returnType
        println("\n=== method2 return type annotations: ${returnType2.annotations.map { it.classId }} ===")
        println("=== method2 (member) annotations: ${method2.annotations.map { it.classId }} ===")
    }

    @Test
    fun testRawTypeDetection() {
        // Test raw types - generic class used without type arguments
        val source = """
            public class Generic<T> {
                public static Generic raw = new Generic();
                public Generic<String> notRaw = new Generic<>();
                public Generic alsoRaw;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assert(javaClass.typeParameters.size == 1) { "Generic should have 1 type parameter" }
        assert(javaClass.typeParameters.first().name.asString() == "T")

        // Check the static raw field
        val rawField = javaClass.fields.first { it.name.asString() == "raw" }
        val rawType = rawField.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(rawType.classifierQualifiedName == "Generic") { "Expected 'Generic', got ${rawType.classifierQualifiedName}" }
        assert(rawType.typeArguments.isEmpty()) { "Raw type should have no type arguments" }
        // classifier should resolve to the containing class itself
        assert(rawType.classifier != null) { "classifier should resolve to Generic class" }
        assert(rawType.classifier == javaClass) { "classifier should be the same Generic class" }
        // isRaw should be true because Generic has type params but no args provided
        assert(rawType.isRaw) { "Expected isRaw=true for raw Generic field" }

        // Check the notRaw field (has explicit type argument)
        val notRawField = javaClass.fields.first { it.name.asString() == "notRaw" }
        val notRawType = notRawField.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(notRawType.classifierQualifiedName == "Generic") { "Expected 'Generic', got ${notRawType.classifierQualifiedName}" }
        assert(notRawType.typeArguments.size == 1) { "notRaw should have 1 type argument, got ${notRawType.typeArguments.size}" }
        assert(!notRawType.isRaw) { "Expected isRaw=false for Generic<String>" }

        // Check the alsoRaw field (instance field, also raw)
        val alsoRawField = javaClass.fields.first { it.name.asString() == "alsoRaw" }
        val alsoRawType = alsoRawField.type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(alsoRawType.typeArguments.isEmpty()) { "alsoRaw should have no type arguments" }
        assert(alsoRawType.isRaw) { "Expected isRaw=true for raw alsoRaw field" }
    }

    @Test
    fun testRawTypeWithExternalClass() {
        // Test raw types with external classes (via star import)
        val source = """
            import java.util.*;
            public class A {
                void foo(List x) {}
                void bar(List<String> y) {}
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val fooMethod = javaClass.methods.first { it.name.asString() == "foo" }
        val fooParamType = fooMethod.valueParameters.first().type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        // For external class via star import, classifier is null (not in local scope)
        assert(fooParamType.classifier == null) { "External class List should have null classifier" }
        // classifierQualifiedName returns "List" (unresolved via star import)
        assert(fooParamType.classifierQualifiedName == "List") { "Expected 'List', got ${fooParamType.classifierQualifiedName}" }
        assert(fooParamType.typeArguments.isEmpty()) { "Raw List should have no type args" }
        // isRaw returns false for external classes because we can't determine type params without FIR
        // FIR's type conversion handles this via fallback logic
        // This documents the current behavior - java-direct can't determine isRaw for external classes
        assert(!fooParamType.isRaw) { "isRaw is false for external classes (FIR handles this)" }

        val barMethod = javaClass.methods.first { it.name.asString() == "bar" }
        val barParamType = barMethod.valueParameters.first().type as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(barParamType.typeArguments.size == 1) { "List<String> should have 1 type arg" }
        assert(!barParamType.isRaw) { "List<String> should not be raw" }
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
        val (root, context) = parseSource(source)
        val classes = root.getChildrenByType("CLASS").map { JavaClassOverAst(it, context) }

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
    fun testFindAnnotationOnClass() {
        val source = """
            @Deprecated
            public class Foo {}
        """.trimIndent()
        val javaClass = parseFirstClass(source)
        assert(javaClass.annotations.isNotEmpty()) { "Should have annotations" }
        val found = javaClass.findAnnotation(org.jetbrains.kotlin.name.FqName("Deprecated"))
        assert(found != null) { "findAnnotation should find @Deprecated on class, got null" }
        val notFound = javaClass.findAnnotation(org.jetbrains.kotlin.name.FqName("Override"))
        assert(notFound == null) { "findAnnotation should return null for missing annotation" }
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
    fun testDeprecatedInJavaDoc() {
        val source = """
            /** @deprecated use Foo2 instead */
            public class Foo {
                /** @deprecated */
                public void oldMethod() {}
                public void newMethod() {}
                /**
                 * @deprecated Ha-ha-ha
                 */
                public int oldField = 0;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)
        assert(javaClass.isDeprecatedInJavaDoc) { "Class Foo should be deprecated via JavaDoc" }

        val oldMethod = javaClass.methods.first { it.name.asString() == "oldMethod" }
        assert(oldMethod.isDeprecatedInJavaDoc) { "oldMethod should be deprecated via JavaDoc" }

        val newMethod = javaClass.methods.first { it.name.asString() == "newMethod" }
        assert(!newMethod.isDeprecatedInJavaDoc) { "newMethod should NOT be deprecated" }

        val oldField = javaClass.fields.first { it.name.asString() == "oldField" }
        assert(oldField.isDeprecatedInJavaDoc) { "oldField should be deprecated via JavaDoc" }
    }

    @Test
    fun testPublicClassWithMalformedMembers() {
        // Regression: public class with syntactically invalid members (nameless method/field)
        // must have public visibility and a default constructor.
        // Previously, `void () {}` was treated as a constructor (no TYPE node in the AST)
        // with no IDENTIFIER, making hasDefaultConstructor() = false and causing
        // INVISIBLE_REFERENCE in FIR when the class was used as a supertype.
        val source = """
            package p;
            public class Nameless {
                void () {}
                int ;
            }
        """.trimIndent()
        val (root, context) = parseSource(source)
        val classNode = root.getChildrenByType("CLASS")
            .first { it.findChildByType("IDENTIFIER")?.text == "Nameless" }
        val javaClass = JavaClassOverAst(classNode, context)
        assert(javaClass.visibility.toString() == "public") {
            "Expected public visibility for 'public class Nameless', got ${javaClass.visibility}"
        }
        assert(javaClass.constructors.isEmpty()) {
            "Malformed 'void () {}' should not be treated as a constructor"
        }
        assert(javaClass.hasDefaultConstructor()) {
            "Class with no valid constructors should have a default constructor"
        }
    }

    @Test
    fun testTypeParameterBoundWithAnnotation() {
        // Test TYPE_USE annotations on type parameter bounds like T extends @NotNull Object
        val source = """
            import org.jetbrains.annotations.NotNull;
            import org.jetbrains.annotations.Nullable;
            
            public class TestBound<T extends @NotNull Object, U extends @Nullable Number> {
            }
        """.trimIndent()

        val (root, _) = parseSource(source)

        fun printTree(node: JavaSyntaxNode, indent: String = "") {
            println("$indent${node.type}: '${node.text.take(80).replace("\n", "\\n")}'")
            for (child in node.children) {
                printTree(child, "$indent  ")
            }
        }

        val classNode = root.children.first { it.type.toString() == "CLASS" }
        val typeParamList = classNode.findChildByType("TYPE_PARAMETER_LIST")
        println("=== TYPE_PARAMETER_LIST structure (full) ===")
        if (typeParamList != null) {
            printTree(typeParamList)
        }

        // Check the EXTENDS_BOUND_LIST structure more carefully
        val typeParam = typeParamList?.children?.firstOrNull { it.type.toString() == "TYPE_PARAMETER" }
        val extendsBoundList = typeParam?.findChildByType("EXTENDS_BOUND_LIST")
        println("=== EXTENDS_BOUND_LIST children ===")
        extendsBoundList?.children?.forEach { child ->
            println("  ${child.type}: '${child.text}'")
            child.children.forEach { grandchild ->
                println("    ${grandchild.type}: '${grandchild.text}'")
            }
        }

        val javaClass = JavaClassOverAst(classNode, JavaResolutionContext.create(root))

        assert(javaClass.typeParameters.size == 2) { "Expected 2 type parameters, got ${javaClass.typeParameters.size}" }

        val paramT = javaClass.typeParameters.first { it.name.asString() == "T" }
        println("paramT.upperBounds.size = ${paramT.upperBounds.size}")
        paramT.upperBounds.forEachIndexed { i, b -> println("  bound[$i] = ${b.classifierQualifiedName}, class=${b::class.simpleName}") }
        assert(paramT.upperBounds.size == 1) { "T should have 1 upper bound, got ${paramT.upperBounds.size}" }
        val boundT = paramT.upperBounds.first()
        println("boundT.classifierQualifiedName = ${boundT.classifierQualifiedName}")
        assert(boundT.classifierQualifiedName == "Object") { "T's bound should be Object, got ${boundT.classifierQualifiedName}" }

        // Check annotations on the bound type
        println("T's bound annotations: ${boundT.annotations.map { it.classId }}")
        assert(boundT.annotations.size == 1) { "T's bound should have 1 annotation (@NotNull), got ${boundT.annotations.size}" }
        assert(boundT.annotations.first().classId?.shortClassName?.asString() == "NotNull") {
            "Expected @NotNull annotation on T's bound"
        }

        val paramU = javaClass.typeParameters.first { it.name.asString() == "U" }
        assert(paramU.upperBounds.size == 1) { "U should have 1 upper bound" }
        val boundU = paramU.upperBounds.first()
        assert(boundU.classifierQualifiedName == "Number") { "U's bound should be Number" }

        println("U's bound annotations: ${boundU.annotations.map { it.classId }}")
        assert(boundU.annotations.size == 1) { "U's bound should have 1 annotation (@Nullable), got ${boundU.annotations.size}" }
        assert(boundU.annotations.first().classId?.shortClassName?.asString() == "Nullable") {
            "Expected @Nullable annotation on U's bound"
        }
    }

    @Test
    fun testMethodReturnTypeWithAnnotation() {
        // Test TYPE_USE annotations on method return types like @Nullable T bar()
        val source = """
            import org.jetbrains.annotations.NotNull;
            import org.jetbrains.annotations.Nullable;
            
            public class TestReturn {
                public <T> @NotNull T foo() { return null; }
                public <T> @Nullable T bar() { return null; }
            }
        """.trimIndent()

        val (root, _) = parseSource(source)

        fun printTree(node: JavaSyntaxNode, indent: String = "") {
            println("$indent${node.type}: '${node.text.take(80).replace("\n", "\\n")}'")
            for (child in node.children) {
                printTree(child, "$indent  ")
            }
        }

        val classNode = root.children.first { it.type.toString() == "CLASS" }
        val methods = classNode.getChildrenByType("METHOD")
        println("=== METHOD structures ===")
        methods.forEach { method ->
            println("\n--- Method: ${method.findChildByType("IDENTIFIER")?.text} ---")
            printTree(method)
        }

        val javaClass = JavaClassOverAst(classNode, JavaResolutionContext.create(root))

        val fooMethod = javaClass.methods.first { it.name.asString() == "foo" }
        val fooReturnType = fooMethod.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        println("foo return type: ${fooReturnType.classifierQualifiedName}")
        println("foo return type annotations: ${fooReturnType.annotations.map { it.classId }}")
        assert(fooReturnType.classifierQualifiedName == "T") { "foo's return type should be T" }
        assert(fooReturnType.annotations.size == 1) { "foo's return type should have 1 annotation (@NotNull), got ${fooReturnType.annotations.size}" }
        assert(fooReturnType.annotations.first().classId?.shortClassName?.asString() == "NotNull") {
            "Expected @NotNull annotation on foo's return type"
        }

        val barMethod = javaClass.methods.first { it.name.asString() == "bar" }
        val barReturnType = barMethod.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        println("bar return type: ${barReturnType.classifierQualifiedName}")
        println("bar return type annotations: ${barReturnType.annotations.map { it.classId }}")
        assert(barReturnType.classifierQualifiedName == "T") { "bar's return type should be T" }
        assert(barReturnType.annotations.size == 1) { "bar's return type should have 1 annotation (@Nullable), got ${barReturnType.annotations.size}" }
        assert(barReturnType.annotations.first().classId?.shortClassName?.asString() == "Nullable") {
            "Expected @Nullable annotation on bar's return type"
        }
    }
}
