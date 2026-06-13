/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.resolution.getFirstStarImportCandidate
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaEnumValueAnnotationArgument
import org.jetbrains.kotlin.name.FqName
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
        val found = javaClass.findAnnotation(FqName("Deprecated"))
        assert(found != null) { "findAnnotation should find @Deprecated on class, got null" }
        val notFound = javaClass.findAnnotation(FqName("Override"))
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
        val fieldType = field.type as JavaClassifierType

        assert(fieldType.classifierQualifiedName == "List") {
            "Expected 'List', got ${fieldType.classifierQualifiedName}"
        }
        assert(fieldType.typeArguments.size == 1) {
            "Expected 1 type argument, got ${fieldType.typeArguments.size}"
        }

        val typeArg = fieldType.typeArguments[0] as JavaClassifierType
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
        val fieldType = field.type as JavaClassifierType

        assert(fieldType.classifierQualifiedName == "Map") {
            "Expected 'Map', got ${fieldType.classifierQualifiedName}"
        }
        assert(fieldType.typeArguments.size == 2) {
            "Expected 2 type arguments, got ${fieldType.typeArguments.size}"
        }

        // First type argument: @NotNull String
        val keyArg = fieldType.typeArguments[0] as JavaClassifierType
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
        val valueArg = fieldType.typeArguments[1] as JavaClassifierType
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
        val returnType = method.returnType as JavaClassifierType

        assert(returnType.classifierQualifiedName == "List") {
            "Expected 'List', got ${returnType.classifierQualifiedName}"
        }
        assert(returnType.typeArguments.size == 1) {
            "Expected 1 type argument, got ${returnType.typeArguments.size}"
        }

        val typeArg = returnType.typeArguments[0] as JavaClassifierType
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
        val paramType = param.type as JavaClassifierType

        assert(paramType.classifierQualifiedName == "List") {
            "Expected 'List', got ${paramType.classifierQualifiedName}"
        }
        assert(paramType.typeArguments.size == 1) {
            "Expected 1 type argument, got ${paramType.typeArguments.size}"
        }

        val typeArg = paramType.typeArguments[0] as JavaClassifierType
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
        val fieldType = field.type as JavaClassifierType

        val typeArg = fieldType.typeArguments[0] as JavaClassifierType
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

        val classNode = tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, parsed.context)

        assert(javaClass.typeParameters.size == 2) { "Expected 2 type parameters, got ${javaClass.typeParameters.size}" }

        val paramT = javaClass.typeParameters.first { it.name.asString() == "T" }
        assert(paramT.upperBounds.size == 1) { "T should have 1 upper bound, got ${paramT.upperBounds.size}" }
        val boundT = paramT.upperBounds.first()
        assert(boundT.classifierQualifiedName == "Object") { "T's bound should be Object, got ${boundT.classifierQualifiedName}" }

        // Check annotations on the bound type
        assert(boundT.annotations.size == 1) { "T's bound should have 1 annotation (@NotNull), got ${boundT.annotations.size}" }
        assert(boundT.annotations.first().classId?.shortClassName?.asString() == "NotNull") {
            "Expected @NotNull annotation on T's bound"
        }

        val paramU = javaClass.typeParameters.first { it.name.asString() == "U" }
        assert(paramU.upperBounds.size == 1) { "U should have 1 upper bound" }
        val boundU = paramU.upperBounds.first()
        assert(boundU.classifierQualifiedName == "Number") { "U's bound should be Number" }

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

        val classNode = tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, parsed.context)

        val fooMethod = javaClass.methods.first { it.name.asString() == "foo" }
        val fooReturnType = fooMethod.returnType as JavaClassifierType
        assert(fooReturnType.classifierQualifiedName == "T") { "foo's return type should be T" }
        assert(fooReturnType.annotations.size == 1) { "foo's return type should have 1 annotation (@NotNull), got ${fooReturnType.annotations.size}" }
        assert(fooReturnType.annotations.first().classId?.shortClassName?.asString() == "NotNull") {
            "Expected @NotNull annotation on foo's return type"
        }

        val barMethod = javaClass.methods.first { it.name.asString() == "bar" }
        val barReturnType = barMethod.returnType as JavaClassifierType
        assert(barReturnType.classifierQualifiedName == "T") { "bar's return type should be T" }
        assert(barReturnType.annotations.size == 1) { "bar's return type should have 1 annotation (@Nullable), got ${barReturnType.annotations.size}" }
        assert(barReturnType.annotations.first().classId?.shortClassName?.asString() == "Nullable") {
            "Expected @Nullable annotation on bar's return type"
        }
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
        val starCandidate = with(context) { getFirstStarImportCandidate("NotNull") }
        assert(starCandidate != null) { "Expected star import candidate for NotNull" }
        assert(starCandidate?.packageFqName?.asString() == "org.jetbrains.annotations") {
            "Expected package org.jetbrains.annotations, got ${starCandidate?.packageFqName}"
        }

        // Find the class and method
        val classNode = tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, context)
        val method = javaClass.methods.first { it.name.asString() == "iteratorOfNotNull" }
        val returnType = method.returnType as JavaClassifierType

        // Get the type argument (Integer with @NotNull)
        val typeArg = returnType.typeArguments.firstOrNull() as? JavaClassifierType
        assert(typeArg != null) { "Expected type argument on Iterator" }

        val allAnnotations = typeArg!!.annotations.toList()
        assert(allAnnotations.size == 1) { "Expected 1 annotation on type argument, got ${allAnnotations.size}: ${allAnnotations.map { it.classId }}" }

        val ann = allAnnotations.first()
        assert(ann.classId?.shortClassName?.asString() == "NotNull") { "Expected NotNull annotation, got ${ann.classId}" }
        // Type-position annotations (`@NotNull` on a type argument) flow through the
        // `typePositionAnnotations` path of `JavaTypeOverAst.annotations`, which is returned
        // unconditionally — no `@Target` callback needed.
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
        val starCandidate1 = with(context) { getFirstStarImportCandidate("Iterator") }
        assert(starCandidate1?.packageFqName?.asString() == "java.util") {
            "First star import should be java.util, got ${starCandidate1?.packageFqName}"
        }

        // Find the class and method
        val classNode = tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, context)
        val method = javaClass.methods.first { it.name.asString() == "iteratorOfNotNull" }
        val returnType = method.returnType as JavaClassifierType

        // Get the type argument (Integer with @NotNull)
        assert(returnType.typeArguments.size == 1) { "Expected 1 type arg, got ${returnType.typeArguments.size}" }
        val typeArg = returnType.typeArguments.first() as JavaClassifierType

        val allAnnotations = typeArg.annotations.toList()
        assert(allAnnotations.size == 1) { "Expected 1 annotation on type argument, got ${allAnnotations.size}" }

        val ann = allAnnotations.first()
        assert(ann.classId?.shortClassName?.asString() == "NotNull") { "Expected NotNull, got ${ann.classId}" }
        // See sibling test above — type-position annotations are exposed via the
        // unconditional `typePositionAnnotations` path of `JavaTypeOverAst.annotations`.
    }

    @Test
    fun testEnumValueArgumentQualifiedWithImport() {
        // Qualified reference `RetentionPolicy.RUNTIME` with the class directly imported —
        // enumClassId is built from the import, so isResolved must be true.
        val source = """
            package com.example;

            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention(RetentionPolicy.RUNTIME)
            public @interface MyAnno {}
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val retention = javaClass.annotations.first { it.classId?.shortClassName?.asString() == "Retention" }
        val arg = retention.arguments.first() as JavaEnumValueAnnotationArgument

        assert(arg.enumClassId?.asSingleFqName()?.asString() == "java.lang.annotation.RetentionPolicy") {
            "Expected enumClassId java.lang.annotation.RetentionPolicy, got ${arg.enumClassId}"
        }
        assert(arg.entryName?.asString() == "RUNTIME") { "Expected entry RUNTIME, got ${arg.entryName}" }
    }

    @Test
    fun testEnumValueArgumentQualifiedWithoutImport() {
        // Qualified reference `MyEnum.A` where the outer class has NO import.
        // the model itself owns resolution and `enumClassId` is reliable for every reference.
        // Here the parsing-level fixture has no symbol provider wired, so the unimported same-
        // package fallback hits the package+className heuristic — which gives the correct answer.
        val source = """
            package com.example;

            @AnnoOf(MyEnum.A)
            public class Host {}
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val anno = javaClass.annotations.first { it.classId?.shortClassName?.asString() == "AnnoOf" }
        val arg = anno.arguments.first() as JavaEnumValueAnnotationArgument

        // the assertion below covered the model-internal heuristic gate. Surrounding `enumClassId` /
        // `entryName` checks cover the user-visible invariants.
        assert(arg.enumClassId?.asSingleFqName()?.asString() == "com.example.MyEnum") {
            "Expected enumClassId com.example.MyEnum (same-package heuristic), got ${arg.enumClassId}"
        }
        assert(arg.entryName?.asString() == "A") { "Expected entry A, got ${arg.entryName}" }
    }

    @Test
    fun testEnumValueArgumentBareWithStaticImport() {
        // Bare identifier `RUNTIME` resolved via a static import.
        //
        // the model owns resolution; in this parsing-level fixture (no symbol
        // provider wired) the model still records the dotted className verbatim, which the
        // top-level-`ClassId` fallback maps to the correct FQN.
        val source = """
            package com.example;

            import java.lang.annotation.Retention;
            import static java.lang.annotation.RetentionPolicy.RUNTIME;

            @Retention(RUNTIME)
            public @interface MyAnno {}
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val retention = javaClass.annotations.first { it.classId?.shortClassName?.asString() == "Retention" }
        val arg = retention.arguments.first() as JavaEnumValueAnnotationArgument

        assert(arg.entryName?.asString() == "RUNTIME") { "Expected entry RUNTIME, got ${arg.entryName}" }
        assert(arg.enumClassId?.asSingleFqName()?.asString() == "java.lang.annotation.RetentionPolicy") {
            "Expected enumClassId java.lang.annotation.RetentionPolicy, got ${arg.enumClassId}"
        }
    }

    @Test
    fun testEnumValueArgumentBareNoImport() {
        // Bare identifier with no static import: className is null, so we treat it as an entry
        // name against the parameter's expected type. isResolved must be true to signal the FIR
        // mapper to fall back to the expected type rather than probing via the callback.
        val source = """
            package com.example;

            import java.lang.annotation.Retention;

            @Retention(RUNTIME)
            public @interface MyAnno {}
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val retention = javaClass.annotations.first { it.classId?.shortClassName?.asString() == "Retention" }
        val arg = retention.arguments.first() as JavaEnumValueAnnotationArgument

        assert(arg.enumClassId == null) { "Without any import hint, enumClassId must be null, got ${arg.enumClassId}" }
        assert(arg.entryName?.asString() == "RUNTIME") { "Expected entry RUNTIME, got ${arg.entryName}" }
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
