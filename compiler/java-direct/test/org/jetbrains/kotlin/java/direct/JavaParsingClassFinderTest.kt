/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.jetbrains.kotlin.java.direct.model.JavaClassOverAst
import org.jetbrains.kotlin.java.direct.parse.parseJavaToLightTree
import org.jetbrains.kotlin.java.direct.resolution.JavaResolutionContext
import org.jetbrains.kotlin.java.direct.resolution.classifierAdapterFor
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class JavaParsingClassFinderTest : JavaParsingTestBase() {

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
        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // Test package with classes - should return class names
        val comExampleClasses = finder.knownClassNamesInPackage(FqName("com.example"))
        assert(comExampleClasses.size == 2) { "Expected 2 classes in com.example, got ${comExampleClasses.size}" }
        assert("ClassA" in comExampleClasses) { "Expected ClassA in com.example" }
        assert("ClassB" in comExampleClasses) { "Expected ClassB in com.example" }

        val testClasses = finder.knownClassNamesInPackage(FqName("test"))
        assert(testClasses.size == 1) { "Expected 1 class in test, got ${testClasses.size}" }
        assert("ClassC" in testClasses) { "Expected ClassC in test" }

        // Test package NOT in our index - should return empty set (not null)
        val kotlinPackageClasses = finder.knownClassNamesInPackage(FqName("kotlin"))
        assert(kotlinPackageClasses.isEmpty()) { "Expected empty set for package kotlin, got $kotlinPackageClasses" }

        val javaLangClasses = finder.knownClassNamesInPackage(FqName("java.lang"))
        assert(javaLangClasses.isEmpty()) { "Expected empty set for package java.lang, got $javaLangClasses" }

        // Test non-existent package - should also return empty set
        val nonExistentClasses = finder.knownClassNamesInPackage(FqName("does.not.exist"))
        assert(nonExistentClasses.isEmpty()) { "Expected empty set for non-existent package" }
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

        val finder = JavaClassFinderOverAstImpl(listOf(helloFile.toVirtualFile()))

        // Try to find example.Hello
        val classId = ClassId(
            FqName("example"),
            Name.identifier("Hello")
        )
        val request = JavaClassFinder.Request(classId)
        val javaClass = finder.findClass(request)

        assert(javaClass != null) { "Expected to find example.Hello class" }
        assert(javaClass?.name?.asString() == "Hello") { "Expected class name 'Hello', got ${javaClass?.name?.asString()}" }
        assert(javaClass?.fqName?.asString() == "example.Hello") { "Expected fqName 'example.Hello', got ${javaClass?.fqName?.asString()}" }
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
        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // Verify NotNull.java is indexed
        val annotationPackageClasses = finder.knownClassNamesInPackage(FqName("org.jetbrains.annotations"))
        assert("NotNull" in annotationPackageClasses) {
            "NotNull should be in org.jetbrains.annotations, found: $annotationPackageClasses"
        }

        // Verify we can find the annotation class
        val notNullClassId = ClassId(FqName("org.jetbrains.annotations"), Name.identifier("NotNull"))
        val notNullClass = finder.findClass(JavaClassFinder.Request(notNullClassId))
        assert(notNullClass != null) { "Should find NotNull class" }
        assert(notNullClass!!.isAnnotationType) { "NotNull should be an annotation type" }

        // Check that NotNull has @Target annotation with TYPE_USE
        val allAnnotations = notNullClass.annotations.toList()
        assert(allAnnotations.isNotEmpty()) {
            "NotNull should have annotations, but found none"
        }

        val targetAnnotation = allAnnotations.find {
            val classId = it.classId
            classId?.shortClassName?.asString() == "Target" ||
                    classId?.asSingleFqName()?.asString() == "java.lang.annotation.Target"
        }
        assert(targetAnnotation != null) {
            "NotNull should have @Target annotation, found: ${allAnnotations.map { it.classId }}"
        }
    }

    @Test
    fun testInheritedInnerClassResolutionCrossFile(@TempDir tempDir: Path) {
        // Cross-file version: all classes in the same package, separate files.
        // The class finder indexes all files so cross-file resolution works via collectInheritedInnerClasses.
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

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // Verify inherited inner class detection works cross-file
        val simpleDescId = ClassId(FqName("test"), Name.identifier("SimpleFunctionDescriptor"))
        val inherited = finder.collectInheritedInnerClasses(simpleDescId)
        assert("CopyBuilder" in inherited) {
            "Expected CopyBuilder in inherited inner classes of SimpleFunctionDescriptor, got ${inherited.keys}"
        }

        // Verify the inner class is found via the class finder's resolution
        val copyBuilderId = inherited["CopyBuilder"]!!.first()
        assert(copyBuilderId.asString() == "test/FunctionDescriptor.CopyBuilder") {
            "Expected CopyBuilder to be from FunctionDescriptor, got $copyBuilderId"
        }
    }

    @Test
    fun testInheritedInnerClassCrossPackage(@TempDir tempDir: Path) {
        // Reproduces the UserDataKey issue: CallableDescriptor (in package "base") declares
        // inner interface UserDataKey. FunctionDescriptor (in "base") extends CallableDescriptor.
        // FunctionDescriptorImpl (in "base.impl") implements FunctionDescriptor via star import.
        // Code in FunctionDescriptorImpl should be able to see UserDataKey through inheritance.
        val basePkgDir = tempDir.resolve("base")
        basePkgDir.toFile().mkdirs()
        val implPkgDir = tempDir.resolve("base/impl")
        implPkgDir.toFile().mkdirs()

        basePkgDir.resolve("CallableDescriptor.java").writeText(
            """
            package base;
            public interface CallableDescriptor {
                interface UserDataKey<V> {}
            }
        """.trimIndent()
        )

        basePkgDir.resolve("FunctionDescriptor.java").writeText(
            """
            package base;
            public interface FunctionDescriptor extends CallableDescriptor {
                // UserDataKey inherited from CallableDescriptor
            }
        """.trimIndent()
        )

        implPkgDir.resolve("FunctionDescriptorImpl.java").writeText(
            """
            package base.impl;
            import base.*;
            public abstract class FunctionDescriptorImpl implements FunctionDescriptor {
                // UserDataKey should be accessible through FunctionDescriptor -> CallableDescriptor
            }
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // Verify inherited inner class detection works cross-package
        val funcDescImplId = ClassId(FqName("base.impl"), Name.identifier("FunctionDescriptorImpl"))
        val inherited = finder.collectInheritedInnerClasses(funcDescImplId)
        assert("UserDataKey" in inherited) {
            "Expected UserDataKey in inherited inner classes of FunctionDescriptorImpl, got ${inherited.keys}"
        }

        val userDataKeyId = inherited["UserDataKey"]!!.first()
        assert(userDataKeyId.asString() == "base/CallableDescriptor.UserDataKey") {
            "Expected UserDataKey to be from CallableDescriptor, got $userDataKeyId"
        }
    }

    @Test
    fun testGetDirectSupertypesUsesCache(@TempDir tempDir: Path) {
        val pkgDir = tempDir.resolve("test")
        pkgDir.toFile().mkdirs()
        pkgDir.resolve("Base.java").writeText(
            """
            package test;
            public class Base {}
        """.trimIndent()
        )
        pkgDir.resolve("Derived.java").writeText(
            """
            package test;
            public class Derived extends Base {}
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // Access the class to populate cache (small files are already cached)
        val derivedId = ClassId(FqName("test"), Name.identifier("Derived"))
        val derivedClass = finder.findClass(JavaClassFinder.Request(derivedId))
        assert(derivedClass != null) { "Expected to find Derived class" }

        // getDirectSupertypes should use the cached class, not reparse
        val baseId = ClassId(FqName("test"), Name.identifier("Base"))
        val supertypes = finder.getDirectSupertypes(derivedId)
        assert(supertypes.contains(baseId)) {
            "Expected supertypes to contain Base, got $supertypes"
        }
    }

    @Test
    fun testCollectInheritedInnerClassesUsesCache(@TempDir tempDir: Path) {
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

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // Access both classes to populate cache
        val parentId = ClassId(FqName("test"), Name.identifier("Parent"))
        val childId = ClassId(FqName("test"), Name.identifier("Child"))
        finder.findClass(JavaClassFinder.Request(parentId))
        finder.findClass(JavaClassFinder.Request(childId))

        // collectInheritedInnerClasses should work using cached data
        val inherited = finder.collectInheritedInnerClasses(childId)
        assert("InnerA" in inherited) { "Expected InnerA in inherited classes, got ${inherited.keys}" }
        assert("InnerB" in inherited) { "Expected InnerB in inherited classes, got ${inherited.keys}" }
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

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // First lookup: Outer
        val outerId = ClassId(FqName("pkg"), Name.identifier("Outer"))
        val outer1 = finder.findClass(JavaClassFinder.Request(outerId))
        assert(outer1 != null) { "Expected to find pkg.Outer" }

        // Second lookup: same ClassId — must be the exact same instance
        val outer2 = finder.findClass(JavaClassFinder.Request(outerId))
        assert(outer1 === outer2) { "Repeated findClass must return the same JavaClassOverAst instance" }

        // Lookup via inner class: navigating Outer.Inner should reference the same Outer
        val innerId = ClassId(FqName("pkg"), FqName("Outer.Inner"), isLocal = false)
        val inner = finder.findClass(JavaClassFinder.Request(innerId))
        assert(inner != null) { "Expected to find pkg.Outer.Inner" }
        assert(inner!!.outerClass === outer1) { "Inner class's outerClass must be the same Outer instance" }

        // Type parameters on both references must be object-identical
        val tp1 = (outer1 as JavaClassOverAst).typeParameters.single()
        val tp2 = (outer2 as JavaClassOverAst).typeParameters.single()
        assert(tp1 === tp2) { "Type parameter instances must be identical (===) across lookups" }
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

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        val subId = ClassId(FqName("pkg"), Name.identifier("Sub"))
        val sub = finder.findClass(JavaClassFinder.Request(subId)) as JavaClassOverAst

        val field = sub.fields.single()
        val fieldType = field.type as JavaClassifierType

        // The simple name "Conflict" should resolve to Base.Conflict (inherited inner),
        // not to the top-level pkg.Conflict.
        val classifier = fieldType.classifier
        assert(classifier != null) { "Conflict should resolve locally" }
        // Inner class's outer class must be Base
        val outerClass = (classifier as? JavaClassOverAst)?.outerClass
        assert(outerClass != null && outerClass.name.asString() == "Base") {
            "Expected Conflict to resolve to Base.Conflict (inner class), " +
                    "but outerClass=${outerClass?.name}"
        }
    }

    @Test
    fun testDiamondInheritanceInnerClasses(@TempDir tempDir: Path) {
        val pkgDir = tempDir.resolve("pkg")
        pkgDir.toFile().mkdirs()

        pkgDir.resolve("A.java").writeText(
            """
            package pkg;
            public class A {
                public static class Inner {}
            }
        """.trimIndent()
        )

        pkgDir.resolve("B.java").writeText(
            """
            package pkg;
            public class B extends A {}
        """.trimIndent()
        )

        pkgDir.resolve("C.java").writeText(
            """
            package pkg;
            public class C extends A {}
        """.trimIndent()
        )

        pkgDir.resolve("D.java").writeText(
            """
            package pkg;
            public class D extends B {
                // Also implements C via the diamond — but Java single inheritance means we
                // can only extend one class. We test the B→A path here; the inherited inner
                // class collector should still find A.Inner exactly once.
            }
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // Force D to be loaded so its supertype graph is built
        val dId = ClassId(FqName("pkg"), Name.identifier("D"))
        val dClass = finder.findClass(JavaClassFinder.Request(dId))
        assert(dClass != null) { "Expected to find pkg.D" }

        // D inherits Inner from A (via B)
        val inherited = finder.collectInheritedInnerClasses(dId)
        assert("Inner" in inherited) {
            "Expected inherited inner class 'Inner' from A, got keys: ${inherited.keys}"
        }
        val innerClassIds = inherited["Inner"]!!
        assert(innerClassIds.size == 1) {
            "Expected exactly 1 ClassId for Inner (A.Inner), got $innerClassIds"
        }
        val innerId = innerClassIds.single()
        assert(innerId == ClassId(FqName("pkg"), FqName("A.Inner"), isLocal = false)) {
            "Expected pkg.A.Inner, got $innerId"
        }
    }

    @Test
    fun testGetDirectSupertypesDoesNotTruncateQualifiedGenericSupertype(@TempDir tempDir: Path) {
        // Regression test for a `substringBefore('<')` truncation bug: `extends B<String>.C` was
        // mis-parsed as if it extended plain `B` (silently dropping `.C`), because the raw-text
        // resolver cut the reference at the first '<' *before* checking whether it was dotted, and
        // "B" alone (post-truncation) has no dot, so it was wrongly treated as a same-package,
        // non-qualified simple name.
        val pkgDir = tempDir.resolve("a")
        pkgDir.toFile().mkdirs()
        pkgDir.resolve("B.java").writeText(
            """
            package a;
            public class B<T> {
                public static class C {}
            }
        """.trimIndent()
        )
        pkgDir.resolve("Derived.java").writeText(
            """
            package a;
            public class Derived extends B<String>.C {}
        """.trimIndent()
        )

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))
        val derivedId = ClassId(FqName("a"), Name.identifier("Derived"))
        finder.findClass(JavaClassFinder.Request(derivedId))

        val supertypes = finder.getDirectSupertypes(derivedId)
        val wrongTruncatedId = ClassId(FqName("a"), Name.identifier("B"))
        assert(wrongTruncatedId !in supertypes) {
            "Expected 'extends B<String>.C' to NOT be truncated to plain 'B', got supertypes $supertypes"
        }
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

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))
        val targetId = ClassId(FqName("pkg"), Name.identifier("Target"))
        val direct = finder.findClass(JavaClassFinder.Request(targetId))
        assert(direct != null) { "Expected to find pkg.Target" }

        val tree = parseJavaToLightTree("package pkg;\nclass Dummy {}", 0)
        val context = JavaResolutionContext.create(tree, createDummyFirSessionForTests(), classFinder = finder)
        val viaAdapter = with(context) { classifierAdapterFor(targetId) }

        assert(viaAdapter === direct) {
            "Expected classifierAdapterFor to route the source-backed ClassId to the canonical " +
                    "JavaClassOverAst instance, got a different object: $viaAdapter"
        }
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

        val finder = JavaClassFinderOverAstImpl(listOf(tempDir.toVirtualFile()))

        // knownClassNamesInPackage should expose only "Main", not "Helper"
        val knownNames = finder.knownClassNamesInPackage(FqName("pkg"))
        assert("Main" in knownNames) { "Expected Main in known names, got $knownNames" }
        assert("Helper" !in knownNames) {
            "Helper is a non-canonical class (in Main.java) and must NOT appear in knownClassNamesInPackage, got $knownNames"
        }

        // But Helper should still be findable by direct ClassId lookup
        val helperId = ClassId(FqName("pkg"), Name.identifier("Helper"))
        val helper = finder.findClass(JavaClassFinder.Request(helperId))
        assert(helper != null) { "Expected to find pkg.Helper by direct ClassId lookup" }
        assert(helper!!.name.asString() == "Helper") { "Expected name 'Helper', got ${helper.name}" }
    }
}
