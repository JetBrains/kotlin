/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

/** Snapshot of a classpath. It consists of a list of [ClasspathEntrySnapshot]s. */
class ClasspathSnapshot(val classpathEntrySnapshots: List<ClasspathEntrySnapshot>)

/**
 * Snapshot of a classpath entry (directory or jar). It consists of a list of [ClassSnapshot]s.
 *
 * NOTE: It's important that the path to the classpath entry is not part of this snapshot. The reason is that classpath entries produced by
 * different builds or on different machines but having the same contents should be considered the same for better build performance.
 */
class ClasspathEntrySnapshot(

    /**
     * Maps (Unix-style) relative paths of classes to their snapshots. The paths are relative to the containing classpath entry (directory
     * or jar).
     */
    val classSnapshots: LinkedHashMap<String, ClassSnapshot>
)

/**
 * Snapshot of a class. It contains minimal information about a class to compute the source files that need to be recompiled during an
 * incremental run of the `KotlinCompile` task.
 *
 * It's important that this class contain only the minimal required information, as it will be part of the classpath snapshot of the
 * `KotlinCompile` task and the task needs to support compile avoidance. For example, this class should contain public method signatures,
 * and should not contain private method signatures, or method implementations.
 */
sealed class ClassSnapshot

/** [ClassSnapshot] of an accessible class. See [InaccessibleClassSnapshot] for info on the accessibility of a class. */
sealed class AccessibleClassSnapshot : ClassSnapshot() {
    abstract val classId: ClassId

    /** The hash of the class's ABI. */
    abstract val classAbiHash: Long

    override fun toString() = classId.toString()
}

/** [ClassSnapshot] of a Kotlin class. */
sealed class KotlinClassSnapshot : AccessibleClassSnapshot() {

    /** Snapshot of this class when [ClassSnapshotGranularity] == [CLASS_MEMBER_LEVEL], null otherwise. */
    abstract val classMemberLevelSnapshot: KotlinClassInfo?
}

/** [KotlinClassSnapshot] where class kind == [CLASS]. */
class RegularKotlinClassSnapshot(
    override val classId: ClassId,
    override val classAbiHash: Long,
    override val classMemberLevelSnapshot: KotlinClassInfo?,
    val supertypes: List<JvmClassName>
) : KotlinClassSnapshot()

/** [KotlinClassSnapshot] where class kind == [FILE_FACADE] or [MULTIFILE_CLASS_PART]. */
class PackageFacadeKotlinClassSnapshot(
    override val classId: ClassId,
    override val classAbiHash: Long,
    override val classMemberLevelSnapshot: KotlinClassInfo?,
    val packageMembers: PackageMemberSet
) : KotlinClassSnapshot()

/**
 * [KotlinClassSnapshot] where class kind == [MULTIFILE_CLASS].
 *
 * NOTE: We have to handle [MULTIFILE_CLASS] differently from [FILE_FACADE] and [MULTIFILE_CLASS_PART] because [MULTIFILE_CLASS] classes
 * don't contain proto data. Except for constants (see below), it is actually okay to ignore [MULTIFILE_CLASS] because any change in a
 * [MULTIFILE_CLASS] will have an associated change in one of its [MULTIFILE_CLASS_PART]s, so the change will be detected when we analyze
 * the [MULTIFILE_CLASS_PART]s.
 *
 * However, if there is a constant is defined in a [MULTIFILE_CLASS], that constant will have a declared value in the [MULTIFILE_CLASS] but
 * not in its [MULTIFILE_CLASS_PART]s. Therefore, we'll need to track constants for [MULTIFILE_CLASS]. (We don't have to do this for inline
 * functions or other package members as those are defined in [MULTIFILE_CLASS_PART]s.)
 */
class MultifileClassKotlinClassSnapshot(
    override val classId: ClassId,
    override val classAbiHash: Long,
    override val classMemberLevelSnapshot: KotlinClassInfo?,
    val constants: PackageMemberSet
) : KotlinClassSnapshot()

/** [ClassSnapshot] of a Java class. */
class JavaClassSnapshot(
    override val classId: ClassId,
    override val classAbiHash: Long,
    /** Snapshot of this class when [ClassSnapshotGranularity] == [CLASS_MEMBER_LEVEL], null otherwise. */
    val classMemberLevelSnapshot: JavaClassMemberLevelSnapshot?,
    val supertypes: List<JvmClassName>
) : AccessibleClassSnapshot()

/** Snapshot of a Java class when [ClassSnapshotGranularity] == [CLASS_MEMBER_LEVEL]. */
class JavaClassMemberLevelSnapshot(
    /** [JavaElementSnapshot] of the class excluding its fields and methods. */
    val classAbiExcludingMembers: JavaElementSnapshot,

    /** [JavaElementSnapshot]s of the class's fields. */
    val fieldsAbi: List<JavaElementSnapshot>,

    /** [JavaElementSnapshot]s of the class's methods. */
    val methodsAbi: List<JavaElementSnapshot>
)

/** Snapshot of a Java class or a Java class member (field or method). */
open class JavaElementSnapshot(

    /** The name of the Java element. It is part of the Java element's ABI. */
    val name: String,

    /** The hash of the Java element's ABI. */
    val abiHash: Long
)

/** TEST-ONLY: A [JavaElementSnapshot] that is used for testing only and must not be used in production code. */
class JavaElementSnapshotForTests(
    name: String,
    abiHash: Long,

    /** The Java element's ABI, captured in a [String]. */
    @Suppress("unused") // Used by Gson reflection
    val abiValue: String

) : JavaElementSnapshot(name, abiHash)

/**
 * [ClassSnapshot] of an inaccessible class.
 *
 * For example, a local class is inaccessible as it can't be referenced from other source files and therefore any changes in a local class
 * will not require recompilation of other source files.
 */
object InaccessibleClassSnapshot : ClassSnapshot()

/** The granularity of a [ClassSnapshot]. */
enum class ClassSnapshotGranularity {

    /**
     * Snapshotting level that allows tracking whether a .class file has changed without tracking what specific parts of the .class file
     * (e.g., fields or methods) have changed.
     */
    CLASS_LEVEL,

    /**
     * Snapshotting level that allows tracking not only whether a .class file has changed but also what specific parts of the .class file
     * (e.g., fields or methods) have changed.
     */
    CLASS_MEMBER_LEVEL
}
