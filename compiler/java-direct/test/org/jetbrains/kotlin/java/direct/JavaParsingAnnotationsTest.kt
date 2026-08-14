/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.resolution.getFirstStarImportCandidate
import org.jetbrains.kotlin.load.java.structure.JavaArrayType
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaEnumValueAnnotationArgument
import org.jetbrains.kotlin.load.java.structure.JavaLiteralAnnotationArgument
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class JavaParsingAnnotationsTest : JavaParsingTestBase() {

    @Test
    fun testAnnotations() {
        val source = """
            @Deprecated
            class A {}
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        assertEquals(1, javaClass.annotations.size)
        // Unit test parses without FIR, so annotation is unresolved (just "Deprecated")
        // FIR will resolve it to java.lang.Deprecated via resolveAnnotation
        assertEquals("Deprecated", javaClass.annotations.first().classId?.asSingleFqName()?.asString())
    }

    @Test
    fun testFindAnnotationOnClass() {
        val source = """
            @Deprecated
            public class Foo {}
        """.trimIndent()
        val javaClass = parseFirstClass(source)
        assertTrue(javaClass.annotations.isNotEmpty()) { "Should have annotations" }
        val found = javaClass.findAnnotation(FqName("Deprecated"))
        assertNotNull(found) { "findAnnotation should find @Deprecated on class" }
        val notFound = javaClass.findAnnotation(FqName("Override"))
        assertNull(notFound) { "findAnnotation should return null for missing annotation" }
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

        assertEquals("List", fieldType.classifierQualifiedName)
        assertEquals(1, fieldType.typeArguments.size)

        val typeArg = fieldType.typeArguments[0] as JavaClassifierType
        assertEquals("Integer", typeArg.classifierQualifiedName)

        // TYPE_USE annotation @NotNull should be on the type argument
        assertEquals(1, typeArg.annotations.size) { "Annotations on type argument: ${typeArg.annotations.map { it.classId }}" }
        val annotation = typeArg.annotations.first()
        assertEquals("NotNull", annotation.classId?.shortClassName?.asString())
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

        assertEquals("Map", fieldType.classifierQualifiedName)
        assertEquals(2, fieldType.typeArguments.size)

        // First type argument: @NotNull String
        val keyArg = fieldType.typeArguments[0] as JavaClassifierType
        assertEquals("String", keyArg.classifierQualifiedName)
        assertEquals(1, keyArg.annotations.size) { "Annotations on key type argument: ${keyArg.annotations.map { it.classId }}" }
        assertEquals("NotNull", keyArg.annotations.first().classId?.shortClassName?.asString())

        // Second type argument: @Nullable Integer
        val valueArg = fieldType.typeArguments[1] as JavaClassifierType
        assertEquals("Integer", valueArg.classifierQualifiedName)
        assertEquals(1, valueArg.annotations.size) { "Annotations on value type argument: ${valueArg.annotations.map { it.classId }}" }
        assertEquals("Nullable", valueArg.annotations.first().classId?.shortClassName?.asString())
    }

    @Test
    fun testArrayElementAndLevelAnnotations() {
        // JLS 9.7.4: an annotation written before the type name annotates the *element* type,
        // one written before a `[]` pair annotates only that array level (leftmost = outermost).
        val source = """
            import java.util.List;
            import org.jetbrains.annotations.NotNull;
            import org.jetbrains.annotations.Nullable;
            
            public class MyClass {
                public List<@NotNull String @Nullable [] []> items;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val field = javaClass.fields.first { it.name.asString() == "items" }
        val fieldType = field.type as JavaClassifierType

        val outerArray = fieldType.typeArguments[0] as JavaArrayType
        assertEquals(1, outerArray.annotations.size) { "Annotations on outer array: ${outerArray.annotations.map { it.classId }}" }
        assertEquals("Nullable", outerArray.annotations.first().classId?.shortClassName?.asString())

        val innerArray = outerArray.componentType as JavaArrayType
        assertTrue(innerArray.annotations.isEmpty()) { "Annotations on inner array: ${innerArray.annotations.map { it.classId }}" }

        val element = innerArray.componentType as JavaClassifierType
        assertEquals("String", element.classifierQualifiedName)
        assertEquals(1, element.annotations.size) { "Annotations on element type: ${element.annotations.map { it.classId }}" }
        assertEquals("NotNull", element.annotations.first().classId?.shortClassName?.asString())
    }

    @Test
    fun testArrayLevelAnnotationsOnField() {
        // Same rule for a *field*, where the annotation of the element type is written in front of
        // the type name and therefore lands in the field's MODIFIER_LIST, and where one declaration
        // node may carry several fields. Each `[]` pair still keeps only its own annotations.
        val source = """
            import org.jetbrains.annotations.NotNull;
            
            public class MyClass {
                public String @NotNull [] @Deprecated [] f1, f2;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        for (name in listOf("f1", "f2")) {
            val field = javaClass.fields.first { it.name.asString() == name }
            val outerArray = field.type as JavaArrayType
            assertEquals(1, outerArray.annotations.size) { "$name outer array: ${outerArray.annotations.map { it.classId }}" }
            assertEquals("NotNull", outerArray.annotations.first().classId?.shortClassName?.asString())

            val innerArray = outerArray.componentType as JavaArrayType
            assertEquals(1, innerArray.annotations.size) { "$name inner array: ${innerArray.annotations.map { it.classId }}" }
            assertEquals("Deprecated", innerArray.annotations.first().classId?.shortClassName?.asString())

            val element = innerArray.componentType as JavaClassifierType
            assertEquals("String", element.classifierQualifiedName)
            assertTrue(element.annotations.isEmpty()) { "$name element type: ${element.annotations.map { it.classId }}" }
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

        assertEquals("List", returnType.classifierQualifiedName)
        assertEquals(1, returnType.typeArguments.size)

        val typeArg = returnType.typeArguments[0] as JavaClassifierType
        assertEquals(1, typeArg.annotations.size) { "Annotations on type argument: ${typeArg.annotations.map { it.classId }}" }
        assertEquals("NotNull", typeArg.annotations.first().classId?.shortClassName?.asString())
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

        assertEquals("List", paramType.classifierQualifiedName)
        assertEquals(1, paramType.typeArguments.size)

        val typeArg = paramType.typeArguments[0] as JavaClassifierType
        assertEquals(1, typeArg.annotations.size) { "Annotations on type argument: ${typeArg.annotations.map { it.classId }}" }
        assertEquals("NotNull", typeArg.annotations.first().classId?.shortClassName?.asString())
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
        assertTrue(typeArg.annotations.isEmpty()) {
            "Expected no annotations on type argument, got ${typeArg.annotations.map { it.classId }}"
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

        assertEquals(2, javaClass.typeParameters.size)

        val paramT = javaClass.typeParameters.first { it.name.asString() == "T" }
        assertEquals(1, paramT.upperBounds.size)
        val boundT = paramT.upperBounds.first()
        assertEquals("Object", boundT.classifierQualifiedName)

        // Check annotations on the bound type
        assertEquals(1, boundT.annotations.size) { "Annotations on T's bound: ${boundT.annotations.map { it.classId }}" }
        assertEquals("NotNull", boundT.annotations.first().classId?.shortClassName?.asString())

        val paramU = javaClass.typeParameters.first { it.name.asString() == "U" }
        assertEquals(1, paramU.upperBounds.size)
        val boundU = paramU.upperBounds.first()
        assertEquals("Number", boundU.classifierQualifiedName)

        assertEquals(1, boundU.annotations.size) { "Annotations on U's bound: ${boundU.annotations.map { it.classId }}" }
        assertEquals("Nullable", boundU.annotations.first().classId?.shortClassName?.asString())
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
        assertEquals("T", fooReturnType.classifierQualifiedName)
        assertEquals(1, fooReturnType.annotations.size) { "Annotations on foo's return type: ${fooReturnType.annotations.map { it.classId }}" }
        assertEquals("NotNull", fooReturnType.annotations.first().classId?.shortClassName?.asString())

        val barMethod = javaClass.methods.first { it.name.asString() == "bar" }
        val barReturnType = barMethod.returnType as JavaClassifierType
        assertEquals("T", barReturnType.classifierQualifiedName)
        assertEquals(1, barReturnType.annotations.size) { "Annotations on bar's return type: ${barReturnType.annotations.map { it.classId }}" }
        assertEquals("Nullable", barReturnType.annotations.first().classId?.shortClassName?.asString())
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
        assertNotNull(starCandidate) { "Expected star import candidate for NotNull" }
        assertEquals("org.jetbrains.annotations", starCandidate.packageFqName.asString())

        // Find the class and method
        val classNode = tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, context)
        val method = javaClass.methods.first { it.name.asString() == "iteratorOfNotNull" }
        val returnType = method.returnType as JavaClassifierType

        // Get the type argument (Integer with @NotNull)
        val typeArg = returnType.typeArguments.firstOrNull() as? JavaClassifierType
        assertNotNull(typeArg) { "Expected type argument on Iterator" }

        val allAnnotations = typeArg.annotations.toList()
        assertEquals(1, allAnnotations.size) { "Annotations on type argument: ${allAnnotations.map { it.classId }}" }

        val ann = allAnnotations.first()
        assertEquals("NotNull", ann.classId?.shortClassName?.asString())
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
        assertEquals("java.util", starCandidate1?.packageFqName?.asString())

        // Find the class and method
        val classNode = tree.getChildren(root).first { tree.getType(it).toString() == "CLASS" }
        val javaClass = JavaClassOverAst(classNode, tree, context)
        val method = javaClass.methods.first { it.name.asString() == "iteratorOfNotNull" }
        val returnType = method.returnType as JavaClassifierType

        // Get the type argument (Integer with @NotNull)
        assertEquals(1, returnType.typeArguments.size)
        val typeArg = returnType.typeArguments.first() as JavaClassifierType

        val allAnnotations = typeArg.annotations.toList()
        assertEquals(1, allAnnotations.size) { "Annotations on type argument: ${allAnnotations.map { it.classId }}" }

        val ann = allAnnotations.first()
        assertEquals("NotNull", ann.classId?.shortClassName?.asString())
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

        assertEquals("java.lang.annotation.RetentionPolicy", arg.enumClassId?.asSingleFqName()?.asString())
        assertEquals("RUNTIME", arg.entryName?.asString())
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
        assertEquals("com.example.MyEnum", arg.enumClassId?.asSingleFqName()?.asString()) { "Expected the same-package heuristic to kick in" }
        assertEquals("A", arg.entryName?.asString())
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

        assertEquals("RUNTIME", arg.entryName?.asString())
        assertEquals("java.lang.annotation.RetentionPolicy", arg.enumClassId?.asSingleFqName()?.asString())
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

        assertNull(arg.enumClassId) { "Without any import hint, enumClassId must be null" }
        assertEquals("RUNTIME", arg.entryName?.asString())
    }

    @Test
    fun testConstantExpressionAnnotationArguments() {
        // An annotation argument is a constant expression (JLS 9.7.1), not necessarily a literal:
        // concatenation, arithmetic, parentheses and references to `static final` constants are all
        // allowed there and all have a value. Anything the model fails to evaluate reaches FIR as an
        // unknown argument, i.e. as `null` or an error expression, and the argument is silently lost.
        val source = """
            class Holder {
                static final String HEL = "hel";
                static final int TEN = 10;

                @Anno(text = HEL + "l" + "o", number = 2 * 8 + 13 * (TEN - 8), truncated = (byte) 300)
                void annotated() {}
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        val annotation = javaClass.methods.first { it.name.asString() == "annotated" }.annotations.single()
        val arguments = annotation.arguments.associate { it.name?.asString() to (it as JavaLiteralAnnotationArgument).value }

        assertEquals("hello", arguments["text"])
        assertEquals(42, arguments["number"])
        assertEquals(44.toByte(), arguments["truncated"])
    }

    @Test
    fun testConstantExpressionAnnotationMethodDefault() {
        // The default value of an annotation method is a constant expression too, and FIR turns it
        // into the default value of the corresponding value parameter.
        val source = """
            @interface Anno {
                String text() default "he" + "llo";
                int number() default (1 + 2) * 3;
            }
        """.trimIndent()
        val javaClass = parseFirstClass(source)

        fun defaultOf(name: String): Any? {
            val method = javaClass.methods.first { it.name.asString() == name }
            val default = method.annotationParameterDefaultValue
            assertNotNull(default) { "$name must have a default value" }
            return (default as JavaLiteralAnnotationArgument).value
        }

        assertEquals("hello", defaultOf("text"))
        assertEquals(9, defaultOf("number"))
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
        assertTrue(javaClass.isDeprecatedInJavaDoc) { "Class Foo should be deprecated via JavaDoc" }

        val oldMethod = javaClass.methods.first { it.name.asString() == "oldMethod" }
        assertTrue(oldMethod.isDeprecatedInJavaDoc) { "oldMethod should be deprecated via JavaDoc" }

        val newMethod = javaClass.methods.first { it.name.asString() == "newMethod" }
        assertFalse(newMethod.isDeprecatedInJavaDoc) { "newMethod should NOT be deprecated" }

        val oldField = javaClass.fields.first { it.name.asString() == "oldField" }
        assertTrue(oldField.isDeprecatedInJavaDoc) { "oldField should be deprecated via JavaDoc" }
    }
}
