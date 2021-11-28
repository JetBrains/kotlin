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
sealed class JavaClassName {

    /** The full name of this class (e.g., "com/example/Foo$Bar"). */
    abstract val name: String

    /**
     * Whether this class is an anonymous class.
     *
     * Note: Even though an anonymous class has no name in the source code, it always has a (not-null, not-empty) name in the compiled
     * class (e.g., "com/example/Foo$1").
     */
    abstract val isAnonymous: Boolean

    /** Whether this class is a synthetic class. */
    abstract val isSynthetic: Boolean

    /** The package name of this class (e.g., "com/example"). */
    val packageName: String
        get() = name.substringBeforeLast('/', missingDelimiterValue = "")

    /** The part of the full name of this class after [packageName] (e.g., "Foo$Bar"). */
    val relativeClassName: String
        get() = name.substringAfterLast('/', missingDelimiterValue = name)

    companion object {

        /** Computes the [JavaClassName] of a compiled Java class given its contents. */
        fun compute(classContents: ByteArray): JavaClassName {
            val nameRef = Ref.create<String>()
            val isSyntheticRef = Ref.create<Boolean>()
            val isTopLevelRef = Ref.create<Boolean>()
            val outerNameRef = Ref.create<String>()
            val isAnonymousRef = Ref.create<Boolean>()

            ClassReader(classContents).accept(object : ClassVisitor(Opcodes.API_VERSION) {
                override fun visit(
                    version: Int, access: Int, name: String,
                    signature: String?, superName: String?, interfaces: Array<String?>?
                ) {
                    nameRef.set(name)
                    isSyntheticRef.set((access and Opcodes.ACC_SYNTHETIC) != 0)
                }

                override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
                    if (name == nameRef.get()!!) {
                        isTopLevelRef.set(false)
                        outerNameRef.set(outerName)
                        isAnonymousRef.set(innerName == null)
                    }
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

            val name = nameRef.get()!!
            val isSynthetic = isSyntheticRef.get()!!
            val isTopLevel = isTopLevelRef.get() ?: true
            val outerName = outerNameRef.get()
            val isAnonymous = isAnonymousRef.get()

            return when {
                isTopLevel -> TopLevelClass(name, isSynthetic)
                outerName != null -> NestedNonLocalClass(name, outerName, isAnonymous!!, isSynthetic)
                else -> LocalClass(name, isAnonymous!!, isSynthetic)
            }
        }
    }
}

/** See [JavaClassName]. */
class TopLevelClass(
    override val name: String,
    override val isSynthetic: Boolean
) : JavaClassName() {
    override val isAnonymous: Boolean = false // A top-level class is never anonymous
}

/** See [JavaClassName]. */
sealed class NestedClass : JavaClassName()

/** See [JavaClassName]. */
class NestedNonLocalClass(
    override val name: String,

    /**
     * The full name of the outer class of this class (e.g., the outer name of "com/example/OuterClass$NestedClass" is
     * "com/example/OuterClass", the outer name of "com/example/OuterClassWith$Sign$NestedClassWith$Sign" is
     * "com/example/OuterClassWith$Sign").
     *
     * The outer class can be of any type ([TopLevelClass], [NestedNonLocalClass], or [LocalClass]).
     */
    val outerName: String,

    override val isAnonymous: Boolean,
    override val isSynthetic: Boolean
) : NestedClass() {

    /**
     * The simple name of this class (e.g., the simple name of "com/example/OuterClass$NestedClass" is "NestedClass", the simple name of
     * class "com/example/OuterClassWith$Sign$NestedClassWith$Sign" is "NestedClassWith$Sign").
     *
     * Note: [simpleName] is not `null` and not empty even for anonymous classes (see [JavaClassName.isAnonymous]).
     */
    val simpleName: String
        get() = run {
            check(name.startsWith("$outerName\$"))
            name.substring("$outerName\$".length).also { check(it.isNotEmpty()) }
        }
}

/** See [JavaClassName]. */
class LocalClass(
    override val name: String,
    override val isAnonymous: Boolean,
    override val isSynthetic: Boolean
) : NestedClass()

/**
 * Computes [ClassId]s of the given Java classes.
 *
 * Note that creating a [ClassId] for a nested class will require accessing the outer class for 2 reasons:
 *   - To disambiguate any '$' characters in the (outer) class name (e.g., "com/example/OuterClassWith$Sign$NestedClassWith$Sign").
 *   - To determine whether a class is a local class (a nested class of a local class is also considered local, see [ClassId]'s kdoc).
 *
 * Therefore, outer classes and nested classes must be passed together in one invocation of this method.
 */
fun computeJavaClassIds(classNames: List<JavaClassName>): List<ClassId> {
    val classNameToClassId: MutableMap<JavaClassName, ClassId> = HashMap(classNames.size)
    val nameToClassName: Map<String, JavaClassName> = classNames.associateBy { it.name }

    fun JavaClassName.getClassId(): ClassId {
        classNameToClassId[this]?.let { return it }

        val packageName = FqName(packageName.replace('/', '.'))
        val classId = when (this) {
            is TopLevelClass -> {
                ClassId(packageName, FqName(relativeClassName), /* local */ false)
            }
            is NestedNonLocalClass -> {
                // JavaClassName.relativeClassName can contain '$' but not '.', whereas ClassId.relativeClassName can contain both '$' and
                // '.' (e.g., "com/example/OuterClassWith$Sign$NestedClassWith$Sign" has JavaClassName.relativeClassName
                // "OuterClassWith$Sign$NestedClassWith$Sign", but its ClassId.relativeClassName will be
                // "OuterClassWith$Sign.NestedClassWith$Sign". To disambiguate '$' in the (outer) class name, we need to get the ClassId of
                // the outer class first.
                val outerClassId = nameToClassName[outerName]?.getClassId() ?: error("Can't find outer class '$outerName' of class '$name'")
                val relativeClassName = FqName(outerClassId.relativeClassName.asString() + "." + simpleName)
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
                // The above source will compile into class "com/example/Foo" and "com/example/Foo$1Bar" (or
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
                ClassId(packageName, FqName(relativeClassName), /* local */ true)
            }
        }

        return classId.also {
            classNameToClassId[this] = it
        }
    }

    return classNames.map { it.getClassId() }
}
