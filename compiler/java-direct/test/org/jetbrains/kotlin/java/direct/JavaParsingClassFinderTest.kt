/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.parseJavaToLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.classifierAdapterFor
import org.jetbrains.kotlin.java.direct.resolution.resolveInheritedInnerClassToClassId
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class JavaParsingClassFinderTest : JavaParsingTestBase() {

    /**
     * Drives the production inherited-nested-class path
     * ([resolveInheritedInnerClassToClassId]) for [containingClassId] as indexed by [finder].
     *
     * The containing class's *own* [JavaClassOverAst.resolutionContext] is used, so the walk runs
     * on the same `FirSession` the finder registered its cycle guards and direct-supertype cache
     * on, and each hierarchy level is resolved against its own file's import scope.
     *
     * Only [containingClass]'s *own* supertypes are reachable here: these unit tests run on a
     * session without a `FirSymbolProvider`, and deeper levels are expanded through
     * `directSupertypeClassIds`, whose source arm reads the materialised `classifier` and
     * therefore needs one. Transitive (grandparent) inheritance is covered end-to-end by
     * `compiler/testData/diagnostics/tests/jvm/javaDirect/qualifiedInheritedNestedClassInOwnImplementsClause.kt`.
     */
    private fun resolveInheritedNestedClass(
        finder: JavaClassFinderOverAstImpl,
        containingClassId: ClassId,
        simpleName: String,
    ): ClassId? {
        val containingClass = finder.findClass(JavaClassFinder.Request(containingClassId))
        assertTrue(containingClass is JavaClassOverAst, "Expected to find source class $containingClassId")
        containingClass as JavaClassOverAst
        return with(containingClass.resolutionContext) {
            resolveInheritedInnerClassToClassId(simpleName, containingClass)
        }
    }

    @Test
    fun testKnownClassNamesInPackage(@TempDir tempDir: Path) {
        // Create test Java files in different packages
        val comExampleDir = tempDir.resolve("com/example")
        comExampleDir.toFile().mkdirs()
        comExampleDir.resolve("ClassA.java").writeText(
            """
            package com.example;
            public class ClassA {}
        """.trimIndent()
        )
        comExampleDir.resolve("ClassB.java").writeText(
            """
            package com.example;
            public class ClassB {}
        """.trimIndent()
        )

        val testDir = tempDir.resolve("test")
        testDir.toFile().mkdirs()
        testDir.resolve("ClassC.java").writeText(
            """
            package test;
            public class ClassC {}
        """.trimIndent()
        )

        // Create JavaClassFinder with this source root
        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        // Test package with classes - should return class names
        val comExampleClasses = finder.knownClassNamesInPackage(FqName("com.example"))
        assertEquals(2, comExampleClasses.size)
        assertTrue("ClassA" in comExampleClasses, "Expected ClassA in com.example")
        assertTrue("ClassB" in comExampleClasses, "Expected ClassB in com.example")

        val testClasses = finder.knownClassNamesInPackage(FqName("test"))
        assertEquals(1, testClasses.size)
        assertTrue("ClassC" in testClasses, "Expected ClassC in test")

        // Test package NOT in our index - should return empty set (not null)
        val kotlinPackageClasses = finder.knownClassNamesInPackage(FqName("kotlin"))
        assertTrue(kotlinPackageClasses.isEmpty(), "Expected empty set for package kotlin, got $kotlinPackageClasses")

        val javaLangClasses = finder.knownClassNamesInPackage(FqName("java.lang"))
        assertTrue(javaLangClasses.isEmpty(), "Expected empty set for package java.lang, got $javaLangClasses")

        // Test non-existent package - should also return empty set
        val nonExistentClasses = finder.knownClassNamesInPackage(FqName("does.not.exist"))
        assertTrue(nonExistentClasses.isEmpty(), "Expected empty set for non-existent package")
    }

    @Test
    fun testClassFinderWithPackage(@TempDir tempDir: Path) {
        val helloFile = tempDir.resolve("Hello.java")
        helloFile.toFile().writeText(
            """
            package example;
            
            public class Hello {
                public void greet() {}
            }
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(helloFile.toFile()))

        // Try to find example.Hello
        val classId = ClassId(
            FqName("example"),
            Name.identifier("Hello")
        )
        val request = JavaClassFinder.Request(classId)
        val javaClass = finder.findClass(request)

        assertNotNull(javaClass, "Expected to find example.Hello class")
        assertEquals("Hello", javaClass.name.asString())
        assertEquals("example.Hello", javaClass.fqName?.asString())
    }

    @Test
    fun testMultiFileClassFinder(@TempDir tempDir: Path) {
        // Simulate the test scenario: J.java uses star import for org.jetbrains.annotations.*
        // NotNull.java defines the annotation in that package

        // Create NotNull.java in org/jetbrains/annotations/
        val annotationsDir = tempDir.resolve("org/jetbrains/annotations")
        annotationsDir.toFile().mkdirs()
        annotationsDir.resolve("NotNull.java").writeText(
            """
            package org.jetbrains.annotations;
            
            import java.lang.annotation.*;
            
            @Documented
            @Retention(RetentionPolicy.CLASS)
            @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
            public @interface NotNull {
            }
        """.trimIndent()
        )

        // Create J.java in root (default package)
        tempDir.resolve("J.java").writeText(
            """
            import java.util.*;
            import org.jetbrains.annotations.*;
            
            public class J {
                public static Iterator<@NotNull Integer> iteratorOfNotNull() {
                    return Collections.<Integer>singletonList(null).iterator();
                }
            }
        """.trimIndent()
        )

        // Create class finder with tempDir as source root
        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        // Verify NotNull.java is indexed
        val annotationPackageClasses = finder.knownClassNamesInPackage(FqName("org.jetbrains.annotations"))
        assertTrue(
            "NotNull" in annotationPackageClasses,
            "NotNull should be in org.jetbrains.annotations, found: $annotationPackageClasses"
        )

        // Verify we can find the annotation class
        val notNullClassId = ClassId(FqName("org.jetbrains.annotations"), Name.identifier("NotNull"))
        val notNullClass = finder.findClass(JavaClassFinder.Request(notNullClassId))
        assertNotNull(notNullClass, "Should find NotNull class")
        assertTrue(notNullClass.isAnnotationType, "NotNull should be an annotation type")

        // Check that NotNull has @Target annotation with TYPE_USE
        val allAnnotations = notNullClass.annotations.toList()
        assertTrue(allAnnotations.isNotEmpty(), "NotNull should have annotations, but found none")

        val targetAnnotation = allAnnotations.find {
            val classId = it.classId
            classId?.shortClassName?.asString() == "Target" ||
                    classId?.asSingleFqName()?.asString() == "java.lang.annotation.Target"
        }
        assertNotNull(targetAnnotation, "NotNull should have @Target annotation, found: ${allAnnotations.map { it.classId }}")
    }

    @Test
    fun testInheritedInnerClassResolutionCrossFile(@TempDir tempDir: Path) {
        // Cross-file version: all classes in the same package, separate files.
        // The class finder indexes all files, so the real inherited-nested-class walk resolves
        // `CopyBuilder` through SimpleFunctionDescriptor -> FunctionDescriptor across files.
        val pkgDir = tempDir.resolve("test")
        pkgDir.toFile().mkdirs()

        pkgDir.resolve("FunctionDescriptor.java").writeText(
            """
            package test;
            public interface FunctionDescriptor {
                interface CopyBuilder<D> {}
            }
        """.trimIndent()
        )

        pkgDir.resolve("SimpleFunctionDescriptor.java").writeText(
            """
            package test;
            public interface SimpleFunctionDescriptor extends FunctionDescriptor {
                // CopyBuilder inherited from FunctionDescriptor
            }
        """.trimIndent()
        )

        pkgDir.resolve("FunctionDescriptorImpl.java").writeText(
            """
            package test;
            public abstract class FunctionDescriptorImpl implements FunctionDescriptor {
                public class CopyConfiguration implements SimpleFunctionDescriptor.CopyBuilder<FunctionDescriptor> {
                }
            }
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        val simpleDescId = ClassId(FqName("test"), Name.identifier("SimpleFunctionDescriptor"))
        val copyBuilderId = resolveInheritedNestedClass(finder, simpleDescId, "CopyBuilder")
        assertEquals("test/FunctionDescriptor.CopyBuilder", copyBuilderId?.asString())
    }

    @Test
    fun testInheritedInnerClassCrossPackage(@TempDir tempDir: Path) {
        // Reproduces the UserDataKey issue: FunctionDescriptor (in package "base") declares inner
        // interface UserDataKey. FunctionDescriptorImpl (in "base.impl") implements
        // FunctionDescriptor via a star import, so the supertype reference has to be resolved
        // against FunctionDescriptorImpl's *own* file imports rather than the caller's.
        val basePkgDir = tempDir.resolve("base")
        basePkgDir.toFile().mkdirs()
        val implPkgDir = tempDir.resolve("base/impl")
        implPkgDir.toFile().mkdirs()

        basePkgDir.resolve("FunctionDescriptor.java").writeText(
            """
            package base;
            public interface FunctionDescriptor {
                interface UserDataKey<V> {}
            }
        """.trimIndent()
        )

        implPkgDir.resolve("FunctionDescriptorImpl.java").writeText(
            """
            package base.impl;
            import base.*;
            public abstract class FunctionDescriptorImpl implements FunctionDescriptor {
                // UserDataKey should be accessible through the star-imported FunctionDescriptor
            }
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        // Verify inherited inner class resolution works cross-package (star import in impl)
        val funcDescImplId = ClassId(FqName("base.impl"), Name.identifier("FunctionDescriptorImpl"))
        val userDataKeyId = resolveInheritedNestedClass(finder, funcDescImplId, "UserDataKey")
        assertEquals("base/FunctionDescriptor.UserDataKey", userDataKeyId?.asString())
    }

    @Test
    fun testInheritedNestedClassesFromCrossFileSuperclass(@TempDir tempDir: Path) {
        val pkgDir = tempDir.resolve("test")
        pkgDir.toFile().mkdirs()
        pkgDir.resolve("Parent.java").writeText(
            """
            package test;
            public class Parent {
                public class InnerA {}
                public class InnerB {}
            }
        """.trimIndent()
        )
        pkgDir.resolve("Child.java").writeText(
            """
            package test;
            public class Child extends Parent {}
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        val childId = ClassId(FqName("test"), Name.identifier("Child"))
        for (innerName in listOf("InnerA", "InnerB")) {
            val resolved = resolveInheritedNestedClass(finder, childId, innerName)
            assertEquals(ClassId(FqName("test"), FqName("Parent.$innerName"), isLocal = false), resolved)
        }
    }

    @Test
    fun testTypeParameterIdentityPreservedAcrossLookups(@TempDir tempDir: Path) {
        val pkgDir = tempDir.resolve("pkg")
        pkgDir.toFile().mkdirs()
        pkgDir.resolve("Outer.java").writeText(
            """
            package pkg;
            public class Outer<T> {
                public class Inner {
                    public T get() { return null; }
                }
            }
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        // First lookup: Outer
        val outerId = ClassId(FqName("pkg"), Name.identifier("Outer"))
        val outer1 = finder.findClass(JavaClassFinder.Request(outerId))
        assertNotNull(outer1, "Expected to find pkg.Outer")

        // Second lookup: same ClassId — must be the exact same instance
        val outer2 = finder.findClass(JavaClassFinder.Request(outerId))
        assertSame(outer1, outer2, "Repeated findClass must return the same JavaClassOverAst instance")

        // Lookup via inner class: navigating Outer.Inner should reference the same Outer
        val innerId = ClassId(FqName("pkg"), FqName("Outer.Inner"), isLocal = false)
        val inner = finder.findClass(JavaClassFinder.Request(innerId))
        assertNotNull(inner, "Expected to find pkg.Outer.Inner")
        assertSame(outer1, inner.outerClass, "Inner class's outerClass must be the same Outer instance")

        // Type parameters on both references must be object-identical
        val tp1 = (outer1 as JavaClassOverAst).typeParameters.single()
        val tp2 = (outer2 as JavaClassOverAst).typeParameters.single()
        assertSame(tp1, tp2, "Type parameter instances must be identical (===) across lookups")
    }

    @Test
    fun testNestedClassTakesPriorityOverPackageClass(@TempDir tempDir: Path) {
        val pkgDir = tempDir.resolve("pkg")
        pkgDir.toFile().mkdirs()

        pkgDir.resolve("Base.java").writeText(
            """
            package pkg;
            public class Base {
                public static class Conflict {
                    public int fromBase;
                }
            }
        """.trimIndent()
        )

        pkgDir.resolve("Conflict.java").writeText(
            """
            package pkg;
            public class Conflict {
                public int fromTopLevel;
            }
        """.trimIndent()
        )

        pkgDir.resolve("Sub.java").writeText(
            """
            package pkg;
            public class Sub extends Base {
                public Conflict field;
            }
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        val subId = ClassId(FqName("pkg"), Name.identifier("Sub"))
        val sub = finder.findClass(JavaClassFinder.Request(subId)) as JavaClassOverAst

        val field = sub.fields.single()
        val fieldType = field.type as JavaClassifierType

        // The simple name "Conflict" should resolve to Base.Conflict (inherited inner),
        // not to the top-level pkg.Conflict.
        val classifier = fieldType.classifier
        assertNotNull(classifier, "Conflict should resolve locally")
        // Inner class's outer class must be Base
        val outerClass = (classifier as? JavaClassOverAst)?.outerClass
        assertNotNull(outerClass, "Expected Conflict to resolve to Base.Conflict (inner class), but it has no outer class")
        assertEquals("Base", outerClass.name.asString(), "Expected Conflict to resolve to Base.Conflict (inner class)")
    }

    @Test
    fun testInheritedNestedClassAcrossMultipleDirectSupertypes(@TempDir tempDir: Path) {
        // Several direct supertypes at the same level: the walk must pick the single ancestor that
        // declares the nested class, and decline (null) when two unrelated ancestors declare it —
        // neither shadows the other, so the simple name is ambiguous (JLS 8.5).
        val pkgDir = tempDir.resolve("pkg")
        pkgDir.toFile().mkdirs()

        pkgDir.resolve("Left.java").writeText(
            """
            package pkg;
            public interface Left {
                class Inner {}
            }
        """.trimIndent()
        )

        pkgDir.resolve("Right.java").writeText(
            """
            package pkg;
            public interface Right {}
        """.trimIndent()
        )

        pkgDir.resolve("AlsoInner.java").writeText(
            """
            package pkg;
            public interface AlsoInner {
                class Inner {}
            }
        """.trimIndent()
        )

        pkgDir.resolve("Single.java").writeText(
            """
            package pkg;
            public class Single implements Left, Right {}
        """.trimIndent()
        )

        pkgDir.resolve("Ambiguous.java").writeText(
            """
            package pkg;
            public class Ambiguous implements Left, AlsoInner {}
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        val singleId = ClassId(FqName("pkg"), Name.identifier("Single"))
        val resolved = resolveInheritedNestedClass(finder, singleId, "Inner")
        assertEquals(ClassId(FqName("pkg"), FqName("Left.Inner"), isLocal = false), resolved)

        val ambiguousId = ClassId(FqName("pkg"), Name.identifier("Ambiguous"))
        val ambiguous = resolveInheritedNestedClass(finder, ambiguousId, "Inner")
        assertNull(ambiguous, "Inner is declared by two unrelated ancestors, so it must stay unresolved (JLS 8.5)")
    }

    @Test
    fun testQualifiedGenericSupertypeIsNotTruncatedWhenResolvingInheritedNestedClass(@TempDir tempDir: Path) {
        // Regression test for a `substringBefore('<')` truncation bug: `extends B<String>.C` was
        // mis-parsed as if it extended plain `B` (silently dropping `.C`), because the raw-text
        // resolver cut the reference at the first '<' *before* checking whether it was dotted, and
        // "B" alone (post-truncation) has no dot, so it was wrongly treated as a same-package,
        // non-qualified simple name.
        //
        // Asserted positively through the nested class `N` that `Derived` inherits: it is only
        // reachable if the supertype resolved to `a.B.C`. Under the truncation bug the ancestor
        // would be `a.B`, which has no `N`, and the resolution would silently yield null.
        val pkgDir = tempDir.resolve("a")
        pkgDir.toFile().mkdirs()
        pkgDir.resolve("B.java").writeText(
            """
            package a;
            public class B<T> {
                public static class C {
                    public static class N {}
                }
            }
        """.trimIndent()
        )
        pkgDir.resolve("Derived.java").writeText(
            """
            package a;
            public class Derived extends B<String>.C {}
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))
        val derivedId = ClassId(FqName("a"), Name.identifier("Derived"))

        val resolved = resolveInheritedNestedClass(finder, derivedId, "N")
        assertEquals(
            ClassId(FqName("a"), FqName("B.C.N"), isLocal = false),
            resolved,
            "Expected 'extends B<String>.C' to keep the qualified nested supertype, so that N resolves to a.B.C.N"
        )
    }

    @Test
    fun testClassifierAdapterForRoutesSourceBackedClassIdToCanonicalInstance(@TempDir tempDir: Path) {
        // Regression test for the classifierAdapterFor identity-routing fix: a source-backed
        // ClassId reached through the generic ClassId ladder must materialize to the SAME
        // JavaClassOverAst instance already used for direct same-file/cross-file navigation, not a
        // second, non-navigable FirBackedJavaClassAdapter wrapper (FIR matches JavaTypeParameter by
        // object identity, so a second instance would break outer-type-argument substitution).
        val pkgDir = tempDir.resolve("pkg")
        pkgDir.toFile().mkdirs()
        pkgDir.resolve("Target.java").writeText(
            """
            package pkg;
            public class Target {}
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))
        val targetId = ClassId(FqName("pkg"), Name.identifier("Target"))
        val direct = finder.findClass(JavaClassFinder.Request(targetId))
        assertNotNull(direct, "Expected to find pkg.Target")

        val tree = parseJavaToLightTree("package pkg;\nclass Dummy {}", 0)
        val context = JavaResolutionContext.create(tree, createDummyFirSessionForTests(), classFinder = finder)
        val viaAdapter = with(context) { classifierAdapterFor(targetId) }

        assertSame(
            direct,
            viaAdapter,
            "Expected classifierAdapterFor to route the source-backed ClassId to the canonical JavaClassOverAst instance"
        )
    }

    @Test
    fun testNonCanonicalTopLevelClassVisibility(@TempDir tempDir: Path) {
        val pkgDir = tempDir.resolve("pkg")
        pkgDir.toFile().mkdirs()

        pkgDir.resolve("Main.java").writeText(
            """
            package pkg;
            public class Main {}
            class Helper {}
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toFile()))

        // knownClassNamesInPackage should expose only "Main", not "Helper"
        val knownNames = finder.knownClassNamesInPackage(FqName("pkg"))
        assertTrue("Main" in knownNames, "Expected Main in known names, got $knownNames")
        assertFalse(
            "Helper" in knownNames,
            "Helper is a non-canonical class (in Main.java) and must NOT appear in knownClassNamesInPackage, got $knownNames"
        )

        // But Helper should still be findable by direct ClassId lookup
        val helperId = ClassId(FqName("pkg"), Name.identifier("Helper"))
        val helper = finder.findClass(JavaClassFinder.Request(helperId))
        assertNotNull(helper, "Expected to find pkg.Helper by direct ClassId lookup")
        assertEquals("Helper", helper.name.asString())
    }
}
