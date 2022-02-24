/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME

/** Computes changes between two lists of [JavaClassSnapshot]s .*/
object JavaClassChangesComputer {

    /**
     * Computes changes between two lists of [JavaClassSnapshot]s.
     *
     * NOTE: Each list of classes must not contain duplicates (having the same [JvmClassName]/[ClassId]).
     */
    fun compute(
        currentJavaClassSnapshots: List<JavaClassSnapshot>,
        previousJavaClassSnapshots: List<JavaClassSnapshot>
    ): ProgramSymbolSet {
        val currentClasses: Map<ClassId, JavaClassSnapshot> = currentJavaClassSnapshots.associateBy { it.classId }
        val previousClasses: Map<ClassId, JavaClassSnapshot> = previousJavaClassSnapshots.associateBy { it.classId }

        // Note: Added classes can also impact recompilation.
        // For example, suppose a source file uses `SomeClass` through `*` imports:
        //     import foo.* // `foo.SomeClass` is present in both the first build and the second build
        //     import bar.* // `bar.SomeClass` is added in the second build
        // The addition of `bar.SomeClass` will require the source file to be recompiled as `SomeClass` will be ambiguous. (In this example,
        // the recompilation will fail, but recompilation needs to happen.)
        val addedClasses = currentClasses.keys - previousClasses.keys
        val removedClasses = previousClasses.keys - currentClasses.keys
        val unchangedOrModifiedClasses = currentClasses.keys - addedClasses

        return ProgramSymbolSet.Collector().run {
            addClasses(addedClasses)
            addClasses(removedClasses)
            unchangedOrModifiedClasses.forEach {
                collectClassChanges(currentClasses[it]!!, previousClasses[it]!!, this)
            }
            getResult()
        }
    }

    /**
     * Collects changes between two [JavaClassSnapshot]s.
     *
     * The two classes must have the same [ClassId].
     */
    private fun collectClassChanges(
        currentClassSnapshot: JavaClassSnapshot,
        previousClassSnapshot: JavaClassSnapshot,
        changes: ProgramSymbolSet.Collector
    ) {
        if (currentClassSnapshot.classAbiHash == previousClassSnapshot.classAbiHash) return

        val classId = currentClassSnapshot.classId.also { check(it == previousClassSnapshot.classId) }
        if (currentClassSnapshot.classMemberLevelSnapshot != null && previousClassSnapshot.classMemberLevelSnapshot != null) {
            if (currentClassSnapshot.classMemberLevelSnapshot.classAbiExcludingMembers.abiHash
                != previousClassSnapshot.classMemberLevelSnapshot.classAbiExcludingMembers.abiHash
            ) {
                changes.addClass(classId)
            } else {
                collectClassMemberChanges(
                    classId,
                    currentClassSnapshot.classMemberLevelSnapshot.fieldsAbi,
                    previousClassSnapshot.classMemberLevelSnapshot.fieldsAbi,
                    changes
                )
                collectClassMemberChanges(
                    classId,
                    currentClassSnapshot.classMemberLevelSnapshot.methodsAbi,
                    previousClassSnapshot.classMemberLevelSnapshot.methodsAbi,
                    changes
                )
            }
        } else {
            changes.addClass(classId)
        }
    }

    /** Collects changes between two lists of fields/methods within a class. */
    private fun collectClassMemberChanges(
        classId: ClassId,
        currentMemberSnapshots: List<JavaElementSnapshot>,
        previousMemberSnapshots: List<JavaElementSnapshot>,
        changes: ProgramSymbolSet.Collector
    ) {
        val currentMemberHashes: Map<Long, JavaElementSnapshot> = currentMemberSnapshots.associateBy { it.abiHash }
        val previousMemberHashes: Map<Long, JavaElementSnapshot> = previousMemberSnapshots.associateBy { it.abiHash }

        val addedMembers = currentMemberHashes.keys - previousMemberHashes.keys
        val removedMembers = previousMemberHashes.keys - currentMemberHashes.keys

        // Note:
        //   - Added members can also impact recompilation. For example, suppose a source file calls `foo(1)` where `foo` is defined as:
        //         fun foo(x: Any) { } // Present in both the first build and the second build
        //         fun foo(x: Int) { } // Added in the second build
        //     The addition of `foo(x: Int)` will require the source file to be recompiled as `foo(1)` will now be resolved to `foo(Int)`
        //     instead of `foo(Any)`.
        //   - Modified members will appear in both addedMembers and removedMembers.
        //   - Multiple members may have the same name (but never the same signature (name + desc) or ABI hash). It's okay to report the
        //     same name multiple times.
        changes.addClassMembers(classId, addedMembers.map { currentMemberHashes[it]!!.name })
        changes.addClassMembers(classId, removedMembers.map { previousMemberHashes[it]!!.name })

        // TODO: Check whether the condition to add SAM_LOOKUP_NAME below is too broad, and correct it if necessary.
        // Currently, it matches the logic in ChangesCollector.getDirtyData in buildUtil.kt.
        if (addedMembers.isNotEmpty() || removedMembers.isNotEmpty()) {
            changes.addClassMember(classId, SAM_LOOKUP_NAME.asString())
        }
    }
}
