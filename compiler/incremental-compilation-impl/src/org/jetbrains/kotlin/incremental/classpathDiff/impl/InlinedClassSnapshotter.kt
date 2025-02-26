/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff.impl

import org.jetbrains.kotlin.incremental.impl.InstanceOwnerRecordingClassVisitor
import org.jetbrains.kotlin.incremental.impl.hashToLong
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.ClassReader

/**
 * Manages inlined class interdependencies for [org.jetbrains.kotlin.incremental.classpathDiff.impl.ClassListSnapshotterWithInlinedClassSupport]
 */
internal class InlinedClassSnapshotter() {

    private val knownClassUsages = HashMap<JvmClassName, Set<JvmClassName>>()

    /**
     * The way it's set up, [InlinedClassSnapshotter] stores all information about interdependencies (so the graph is encapsulated),
     * and [ClassListSnapshotterWithInlinedClassSupport] is responsible for calling [extractInlinedSnapshotAndDependenciesFromClassData]
     * with loaded class data.
     *
     * Most of the time we should be dealing with very small class sets and very small dependency graphs,
     * so this part of processing is not optimized for, well, big class sets and deep dependency graphs.
     *
     * @return false if the class set is fully expanded, true if we added something to it
     */
    fun expandClassSetWithTransitiveDependenciesOnce(fullSet: MutableSet<JvmClassName>): Boolean {
        return fullSet.addAll(fullSet.flatMapTo(mutableSetOf()) { knownClassUsages[it] ?: emptySet() })
    }

    /**
     * It's important to catch both bytecode and the constant pool, so in the first implementation we hash the full class
     *
     * In theory with a proper asm setup we can get more precise compile avoidance
     */
    fun extractInlinedSnapshotAndDependenciesFromClassData(classData: ClassFileWithContents): Long {
        //here we want to visit every method, so it's virtually impossible to reuse the classVisitor from regular snapshotting

        //KT-75529 - consider adding extra tests - is visiting methods enough?
        //how else could lambdas depend on each other? what if it's not a lambda?
        val usedClasses = mutableSetOf<JvmClassName>()
        val visitor = InstanceOwnerRecordingClassVisitor(delegateClassVisitor = null, allUsedClassesSet = usedClasses)
        val classReader = ClassReader(classData.contents)
        classReader.accept(visitor, 0)
        knownClassUsages[JvmClassName.byClassId(classData.classInfo.classId)] = usedClasses

        return classData.contents.hashToLong()
    }
}
