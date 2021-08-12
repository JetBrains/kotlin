/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.util.Ref
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * Information about the name of a compiled Java class regarding its nesting level.
 *
 * A [JavaClassName] can be:
 *   - [TopLevelClass]
 *   - [NestedClass] (https://docs.oracle.com/javase/tutorial/java/javaOO/nested.html)
 *
 * A [NestedClass] can be:
 *   - [NestedNonLocalClass]
 *   - [LocalClass] (https://docs.oracle.com/javase/tutorial/java/javaOO/localclasses.html)
 */
sealed class JavaClassName(

    /** The full name of this class (e.g., "com/example/Foo$Bar"). */
    val name: String
) {

    /** The package name of this class (e.g., "com/example"). */
    val packageName: String
        get() = name.substringBeforeLast('/', "")

    companion object {

        fun compute(classContents: ByteArray): JavaClassName {
            val nameRef = Ref.create<String>()
            val isTopLevelRef = Ref.create<Boolean>()
            val outerNameRef = Ref.create<String>()

            ClassReader(classContents).accept(object : ClassVisitor(Opcodes.API_VERSION) {
                override fun visit(
                    version: Int, access: Int, name: String,
                    signature: String?, superName: String?, interfaces: Array<String?>?
                ) {
                    nameRef.set(name)
                }

                override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
                    if (name == nameRef.get()!!) {
                        isTopLevelRef.set(false)
                        outerNameRef.set(outerName)
                    }
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

            val isTopLevel = isTopLevelRef.get() ?: true
            val name = nameRef.get()!!
            val outerName = outerNameRef.get()

            return when {
                isTopLevel -> TopLevelClass(name)
                outerName != null -> NestedNonLocalClass(name, outerName)
                else -> LocalClass(name)
            }
        }
    }
}

/** See [JavaClassName]. */
class TopLevelClass(name: String) : JavaClassName(name) {

    /**
     * The simple name of this class (e.g., the simple name of class "com/example/Foo" is "Foo", the simple name of class
     * "com/example/ClassWith$Sign" is "ClassWith$Sign").
     */
    val simpleName: String
        get() = name.substringAfterLast('/')
}

/** See [JavaClassName]. */
sealed class NestedClass(name: String) : JavaClassName(name)

/** See [JavaClassName]. */
class NestedNonLocalClass(
    name: String,

    /**
     * The full name of the outer class of this class (e.g., the outer name of "com/example/OuterClass$NestedClass" is
     * "com/example/OuterClass", the outer name of class "com/example/OuterClassWith$Sign$NestedClassWith$Sign" is
     * "com/example/OuterClassWith$Sign").
     *
     * The outer class can be of any type ([TopLevelClass], [NestedNonLocalClass], or [LocalClass]).
     */
    val outerName: String,
) : NestedClass(name) {

    init {
        check(name.startsWith("$outerName\$"))
    }

    /**
     * The simple name of this class (e.g., the simple name of "com/example/OuterClass$NestedClass" is "NestedClass", the simple name of
     * class "com/example/OuterClassWith$Sign$NestedClassWith$Sign" is "NestedClassWith$Sign").
     *
     * Note: [simpleName] is not `null` and not empty even for anonymous classes (see [isPossiblyAnonymous]).
     */
    val simpleName: String
        get() = name.substring("$outerName\$".length)

    /**
     * Whether this class is possibly an anonymous class.
     *
     * An anonymous class has no name in the source code, but it always has a (not-null, not-empty) name in the compiled class. Note that
     * the compiled class can also be directly generated without any source code.
     *
     * The [simpleName] of an anonymous class is usually a number. However, it is also possible that the [simpleName] of an anonymous class
     * is not a number, and the [simpleName] of a non-anonymous class is a number (e.g., if the class was directly generated).
     *
     * Therefore, the following property only indicates that this class was likely compiled from an anonymous class, but there is no
     * guarantee.
     */
    val isPossiblyAnonymous: Boolean
        get() = simpleName.toIntOrNull() != null
}

/** See [JavaClassName]. */
class LocalClass(name: String) : NestedClass(name)

/**
 * Computes [ClassId]s of the given Java classes.
 *
 * Note that creating a [ClassId] for a nested class will require accessing the outer class for 2 reasons:
 *   - To disambiguate any '$' characters in the class name (e.g., "com/example/OuterClassWith$Sign$NestedClassWith$Sign").
 *   - To determine whether a class is a local class (a nested class of a local class is also considered local, see [ClassId]'s kdoc).
 *
 * Therefore, outer classes and nested classes must be passed together in one invocation of this method.
 */
fun computeJavaClassIds(javaClassNames: List<JavaClassName>): List<ClassId> {
    val nameToJavaClassName: Map<String, JavaClassName> = javaClassNames.associateBy { it.name }
    val nameToClassId: MutableMap<String, ClassId?> = nameToJavaClassName.mapValues { null }.toMutableMap()

    fun getOrCreateClassId(className: String): ClassId {
        val classInfo = nameToJavaClassName[className] ?: error("Class name not found: $className")
        val computedClassId = nameToClassId[className]
        if (computedClassId != null) {
            return computedClassId
        }

        val packageName = FqName(classInfo.packageName.replace('/', '.'))
        val classId = when (classInfo) {
            is TopLevelClass -> {
                ClassId(packageName, FqName(classInfo.simpleName), /* local */ false)
            }
            is NestedNonLocalClass -> {
                val outerClassId = getOrCreateClassId(classInfo.outerName)
                val relativeClassName = FqName(outerClassId.relativeClassName.asString() + "." + classInfo.simpleName)
                // For ClassId, a nested non-local class of a local class is also considered local (see ClassId's kdoc).
                val isLocal = outerClassId.isLocal

                ClassId(packageName, relativeClassName, /* local */ isLocal)
            }
            is LocalClass -> {
                // Note: The following computation for the relative class name of a local class does not exactly match the description given
                // in ClassId's kdoc, which currently says "In the case of a local class, relativeClassName consists of a single name
                // including all callables' and class' names all the way up to the package, separated by dollar signs."
                //
                // For example, consider this Java source:
                //     package com.example;
                //     class Foo {
                //         void someMethod() {
                //             class Bar {
                //             }
                //         }
                //     }
                // The above class will compile into class "com/example/Foo" and "com/example/Foo$1Bar" (or
                // "com/example/Foo$SomeOtherArbitraryUniqueName which need not contain the string "Bar").
                //
                // Given that class, the difference between the computed ClassId and the expected ClassId is as follows:
                //                         Value computed below        Expected value as defined in ClassId's kdoc
                // relativeClassName            Foo$1Bar                          Foo$someMethod$Bar
                // classId                com.example.Foo$1Bar              com.example.Foo$someMethod$Bar
                //
                // Note: While they don't match, the ClassId computed below is still unique, so it can still be used as an identifier.
                //
                // TODO: Compute ClassID to match the expected value. It will require collecting information about the enclosing class,
                // enclosing method, and simple class name (as written in source code). There will still be a challenge if the class is an
                // anonymous class: The simple class name is not available, and it's not clear what the expected ClassID is.
                //
                // Alternatively, check if we can safely adjust the definition of ClassId for a local class and update any related code
                // accordingly.
                val relativeClassName = FqName(classInfo.name.substringAfterLast('/'))
                ClassId(packageName, relativeClassName, /* local */ true)
            }
        }

        return classId.also {
            nameToClassId[className] = it
        }
    }

    return javaClassNames.map { getOrCreateClassId(it.name) }
}
