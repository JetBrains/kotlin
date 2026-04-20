/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.name.ClassId
import org.junit.jupiter.api.Test

class JavaParsingAnnotationsTest : JavaParsingTestBase() {

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
    fun testTypeParameterBoundWithAnnotation() {
        // Test TYPE_USE annotations on type parameter bounds like T extends @NotNull Object
        val source = """
            import org.jetbrains.annotations.NotNull;
            import org.jetbrains.annotations.Nullable;
            
            public class TestBound<T extends @NotNull Object, U extends @Nullable Number> {
            }
        """.trimIndent()

        val parsed = parseSource(source)
        val root = parsed.root
        val tree = parsed.tree

        fun printTree(node: JavaLightNode, indent: String = "") {
            println("$indent${tree.getType(node)}: '${tree.getText(node).toString().take(80).replace("\n", "\\n")}'")
            for (child in tree.getChildren(node)) {
                printTree(child, "$indent  ")
            }
        }

        val classNode = tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }
        val typeParamList = tree.findChildByType(classNode, "TYPE_PARAMETER_LIST")
        println("=== TYPE_PARAMETER_LIST structure (full) ===")
        if (typeParamList != null) {
            printTree(typeParamList)
        }

        // Check the EXTENDS_BOUND_LIST structure more carefully
        val typeParam = typeParamList?.let { tpl ->
            tree.getChildren(tpl).firstOrNull { tree.getType(it).toString() == "TYPE_PARAMETER" }
        }
        val extendsBoundList = typeParam?.let { tp -> tree.findChildByType(tp, "EXTENDS_BOUND_LIST") }
        println("=== EXTENDS_BOUND_LIST children ===")
        extendsBoundList?.let { ebl ->
            tree.getChildren(ebl).forEach { child ->
                println("  ${tree.getType(child)}: '${tree.getText(child)}'")
                tree.getChildren(child).forEach { grandchild ->
                    println("    ${tree.getType(grandchild)}: '${tree.getText(grandchild)}'")
                }
            }
        }

        val javaClass = JavaClassOverAst(classNode, tree, parsed.context)

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

        val parsed = parseSource(source)
        val root = parsed.root
        val tree = parsed.tree

        fun printTree(node: JavaLightNode, indent: String = "") {
            println("$indent${tree.getType(node)}: '${tree.getText(node).toString().take(80).replace("\n", "\\n")}'")
            for (child in tree.getChildren(node)) {
                printTree(child, "$indent  ")
            }
        }

        val classNode = tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }
        val methods = tree.getChildrenByType(classNode, "METHOD")
        println("=== METHOD structures ===")
        methods.forEach { method ->
            println("\n--- Method: ${tree.findChildByType(method, "IDENTIFIER")?.let { tree.getText(it) }} ---")
            printTree(method)
        }

        val javaClass = JavaClassOverAst(classNode, tree, parsed.context)

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

        println("\n=== Test 1: @Nullable public String method1() ===")
        val parsed1 = parseSource(source1)
        val tree1 = parsed1.tree
        fun printTree1(node: JavaLightNode, indent: String = "") {
            println("$indent${tree1.getType(node)}: '${tree1.getText(node).toString().take(60).replace("\n", "\\n")}'")
            for (child in tree1.getChildren(node)) {
                printTree1(child, "$indent  ")
            }
        }
        val classNode1 = tree1.getChildren(parsed1.root).first { tree1.getType(it).toString() == "CLASS" }
        val methodNode1 = tree1.findChildByType(classNode1, "METHOD")!!
        println("METHOD structure:")
        printTree1(methodNode1)

        println("\n=== Test 2: public @Nullable String method2() ===")
        val parsed2 = parseSource(source2)
        val tree2 = parsed2.tree
        fun printTree2(node: JavaLightNode, indent: String = "") {
            println("$indent${tree2.getType(node)}: '${tree2.getText(node).toString().take(60).replace("\n", "\\n")}'")
            for (child in tree2.getChildren(node)) {
                printTree2(child, "$indent  ")
            }
        }
        val classNode2 = tree2.getChildren(parsed2.root).first { tree2.getType(it).toString() == "CLASS" }
        val methodNode2 = tree2.findChildByType(classNode2, "METHOD")!!
        println("METHOD structure:")
        printTree2(methodNode2)

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
    fun testStarImportAnnotationResolution() {
        val source = """
            import org.jetbrains.annotations.*;
            
            public class J {
                public static java.util.Iterator<@NotNull Integer> iteratorOfNotNull() {
                    return null;
                }
            }
        """.trimIndent()

        val parsed = parseSource(source)
        val root = parsed.root
        val tree = parsed.tree
        val context = parsed.context

        // Check that star import is extracted
        val starCandidate = context.getFirstStarImportCandidate("NotNull")
        assert(starCandidate != null) { "Expected star import candidate for NotNull" }
        assert(starCandidate?.packageFqName?.asString() == "org.jetbrains.annotations") {
            "Expected package org.jetbrains.annotations, got ${starCandidate?.packageFqName}"
        }

        // Find the class and method
        val classNode = tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, context)
        val method = javaClass.methods.first { it.name.asString() == "iteratorOfNotNull" }
        val returnType = method.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        
        // Get the type argument (Integer with @NotNull)
        val typeArg = returnType.typeArguments.firstOrNull() as? org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        assert(typeArg != null) { "Expected type argument on Iterator" }
        
        val allAnnotations = typeArg!!.annotations.toList()
        assert(allAnnotations.size == 1) { "Expected 1 annotation on type argument, got ${allAnnotations.size}: ${allAnnotations.map { it.classId }}" }
        
        val ann = allAnnotations.first()
        assert(ann.classId?.shortClassName?.asString() == "NotNull") { "Expected NotNull annotation, got ${ann.classId}" }
        assert(!ann.isResolved) { "Annotation should be unresolved (star import)" }
        
        // Try resolving via resolveAnnotation
        val candidates = mutableListOf<ClassId>()
        val resolved = ann.resolveAnnotation { candidateClassId ->
            candidates.add(candidateClassId)
            // Simulate: accept org.jetbrains.annotations.NotNull
            candidateClassId.asSingleFqName().asString() == "org.jetbrains.annotations.NotNull"
        }
        
        assert(candidates.isNotEmpty()) { "resolveAnnotation should try candidates" }
        assert(resolved != null) { "resolveAnnotation should resolve to org.jetbrains.annotations.NotNull, candidates tried: $candidates" }
        assert(resolved?.asSingleFqName()?.asString() == "org.jetbrains.annotations.NotNull") {
            "Expected org.jetbrains.annotations.NotNull, got ${resolved?.asSingleFqName()}"
        }
        
        // Test filterTypeUseAnnotations
        val callbackFqNames = mutableListOf<String>()
        val filtered = typeArg.filterTypeUseAnnotations { fqName ->
            callbackFqNames.add(fqName)
            fqName == "org.jetbrains.annotations.NotNull"
        }
        
        assert(filtered.size == 1) { 
            "Expected 1 TYPE_USE annotation, got ${filtered.size}. Callback received: $callbackFqNames" 
        }
    }

    @Test
    fun testExactTestDataFormat() {
        // Test the exact format from javaIteratorOfNotNullFailFast.kt
        // J.java has no package and uses star import
        val source = """
            import java.util.*;
            import org.jetbrains.annotations.*;
            
            public class J {
                public static Iterator<@NotNull Integer> iteratorOfNotNull() {
                    return Collections.<Integer>singletonList(null).iterator();
                }
            }
        """.trimIndent()

        val parsed = parseSource(source)
        val root = parsed.root
        val tree = parsed.tree
        val context = parsed.context

        // Verify star imports are extracted
        val starCandidate1 = context.getFirstStarImportCandidate("Iterator")
        assert(starCandidate1?.packageFqName?.asString() == "java.util") {
            "First star import should be java.util, got ${starCandidate1?.packageFqName}"
        }

        // Check if org.jetbrains.annotations is in star imports
        @Suppress("UNUSED_VARIABLE")
        val starCandidate2 = context.getFirstStarImportCandidate("NotNull")
        // Note: getFirstStarImportCandidate returns the FIRST star import package
        // We need to check if org.jetbrains.annotations is also there

        // Find the class and method
        val classNode = tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, context)
        val method = javaClass.methods.first { it.name.asString() == "iteratorOfNotNull" }
        val returnType = method.returnType as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        
        // Get the type argument (Integer with @NotNull)
        assert(returnType.typeArguments.size == 1) { "Expected 1 type arg, got ${returnType.typeArguments.size}" }
        val typeArg = returnType.typeArguments.first() as org.jetbrains.kotlin.load.java.structure.JavaClassifierType
        
        val allAnnotations = typeArg.annotations.toList()
        assert(allAnnotations.size == 1) { "Expected 1 annotation on type argument, got ${allAnnotations.size}" }
        
        val ann = allAnnotations.first()
        assert(ann.classId?.shortClassName?.asString() == "NotNull") { "Expected NotNull, got ${ann.classId}" }
        assert(!ann.isResolved) { "Annotation should be unresolved" }
        
        // Test resolution - simulate what FIR does
        // FIR's isTypeUseAnnotationClass will accept org.jetbrains.annotations.NotNull
        val candidatesTried = mutableListOf<String>()
        val resolved = ann.resolveAnnotation { candidateClassId ->
            val fqn = candidateClassId.asSingleFqName().asString()
            candidatesTried.add(fqn)
            fqn == "org.jetbrains.annotations.NotNull"
        }
        
        assert(candidatesTried.contains("org.jetbrains.annotations.NotNull")) {
            "Should have tried org.jetbrains.annotations.NotNull, tried: $candidatesTried"
        }
        assert(resolved != null) { "Should resolve to org.jetbrains.annotations.NotNull" }
        
        // Test filterTypeUseAnnotations
        val filteredAnnotations = typeArg.filterTypeUseAnnotations { fqName ->
            fqName == "org.jetbrains.annotations.NotNull"
        }
        assert(filteredAnnotations.size == 1) { "Expected 1 filtered annotation, got ${filteredAnnotations.size}" }
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
}
