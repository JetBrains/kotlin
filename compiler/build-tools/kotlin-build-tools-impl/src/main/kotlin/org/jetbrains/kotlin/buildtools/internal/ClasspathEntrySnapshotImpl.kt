/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.AccessibleClassSnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotExternalizer
import org.jetbrains.kotlin.incremental.classpathDiff.InaccessibleClassSnapshot
import org.jetbrains.kotlin.incremental.storage.saveToFile
import java.io.File

internal class ClasspathEntrySnapshotImpl(
    private val origin: org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshot,
) :
    ClasspathEntrySnapshot {
    override val classSnapshots: Map<String, ClassSnapshot>
        get() = origin.classSnapshots.mapValues {
            when (val snapshot = it.value) {
                is AccessibleClassSnapshot -> AccessibleClassSnapshotImpl(snapshot.classAbiHash)
                is InaccessibleClassSnapshot -> InaccessibleClassSnapshotImpl
            }
        }

    override fun saveSnapshot(path: File) {
        ClasspathEntrySnapshotExternalizer.saveToFile(path, origin)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClasspathEntrySnapshotImpl) return false
        return contentEquals(origin.classSnapshots, other.origin.classSnapshots)
    }

    override fun hashCode(): Int = contentHashCode(origin.classSnapshots)

    override fun toString(): String = "ClasspathEntrySnapshot(classCount=${origin.classSnapshots.size})"
}

/**
 * Computes content-based equality for internal class snapshot maps without creating API wrapper objects.
 * Two snapshots are equal if they have the same keys and corresponding values have the same type and ABI hash.
 */
private fun contentEquals(
    a: LinkedHashMap<String, org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshot>,
    b: LinkedHashMap<String, org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshot>,
): Boolean {
    if (a.size != b.size) return false
    for ([key, valueA] in a) {
        val valueB = b[key] ?: return false
        if (!snapshotEquals(valueA, valueB)) return false
    }
    return true
}

/**
 * Computes content-based hash code for internal class snapshot maps without creating API wrapper objects.
 */
private fun contentHashCode(map: LinkedHashMap<String, org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshot>): Int {
    var result = 0
    for ([key, value] in map) {
        result += key.hashCode() xor snapshotHashCode(value)
    }
    return result
}

private fun snapshotEquals(
    a: org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshot,
    b: org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshot,
): Boolean = when {
    a is AccessibleClassSnapshot && b is AccessibleClassSnapshot -> a.classAbiHash == b.classAbiHash
    a is InaccessibleClassSnapshot && b is InaccessibleClassSnapshot -> true
    else -> false
}

private fun snapshotHashCode(snapshot: org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshot): Int = when (snapshot) {
    is AccessibleClassSnapshot -> snapshot.classAbiHash.hashCode()
    is InaccessibleClassSnapshot -> 0
}
