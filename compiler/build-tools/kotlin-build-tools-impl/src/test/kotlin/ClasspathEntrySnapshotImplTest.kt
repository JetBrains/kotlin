/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.internal.AccessibleClassSnapshotImpl
import org.jetbrains.kotlin.buildtools.internal.ClasspathEntrySnapshotImpl
import org.jetbrains.kotlin.buildtools.internal.InaccessibleClassSnapshotImpl
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.InaccessibleClassSnapshot
import org.jetbrains.kotlin.incremental.classpathDiff.JavaClassSnapshot
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ClasspathEntrySnapshotImplTest {
    @Test
    fun testEqualSnapshotsHaveSameHashCode() {
        val snapshot1 = createSnapshot(mapOf("com/example/A.class" to 42L, "com/example/B.class" to 100L))
        val snapshot2 = createSnapshot(mapOf("com/example/A.class" to 42L, "com/example/B.class" to 100L))

        assertEquals(snapshot1, snapshot2)
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode())
    }

    @Test
    fun testDifferentAbiHashProducesDifferentSnapshot() {
        val snapshot1 = createSnapshot(mapOf("com/example/A.class" to 42L))
        val snapshot2 = createSnapshot(mapOf("com/example/A.class" to 99L))

        assertNotEquals(snapshot1, snapshot2)
    }

    @Test
    fun testDifferentClassPathsProducesDifferentSnapshot() {
        val snapshot1 = createSnapshot(mapOf("com/example/A.class" to 42L))
        val snapshot2 = createSnapshot(mapOf("com/example/B.class" to 42L))

        assertNotEquals(snapshot1, snapshot2)
    }

    @Test
    fun testEmptySnapshots() {
        val snapshot1 = createSnapshot(emptyMap())
        val snapshot2 = createSnapshot(emptyMap())

        assertEquals(snapshot1, snapshot2)
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode())
    }

    @Test
    fun testSnapshotWithInaccessibleClasses() {
        val origin1 = ClasspathEntrySnapshot(linkedMapOf(
            "com/example/A.class" to InaccessibleClassSnapshot,
        ))
        val origin2 = ClasspathEntrySnapshot(linkedMapOf(
            "com/example/A.class" to InaccessibleClassSnapshot,
        ))

        val snapshot1 = ClasspathEntrySnapshotImpl(origin1)
        val snapshot2 = ClasspathEntrySnapshotImpl(origin2)

        assertEquals(snapshot1, snapshot2)
        assertEquals(snapshot1.hashCode(), snapshot2.hashCode())
    }

    @Test
    fun testToString() {
        val snapshot = createSnapshot(mapOf("com/example/A.class" to 42L, "com/example/B.class" to 100L))
        assertEquals("ClasspathEntrySnapshot(classCount=2)", snapshot.toString())
    }

    @Test
    fun testAccessibleClassSnapshotImplEquals() {
        val a = AccessibleClassSnapshotImpl(42L)
        val b = AccessibleClassSnapshotImpl(42L)
        val c = AccessibleClassSnapshotImpl(99L)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }

    @Test
    fun testAccessibleClassSnapshotImplToString() {
        assertEquals("AccessibleClassSnapshot(classAbiHash=42)", AccessibleClassSnapshotImpl(42L).toString())
    }

    @Test
    fun testInaccessibleClassSnapshotImplEquals() {
        assertEquals(InaccessibleClassSnapshotImpl, InaccessibleClassSnapshotImpl)
    }

    @Test
    fun testInaccessibleClassSnapshotImplHashCode() {
        assertEquals(0, InaccessibleClassSnapshotImpl.hashCode())
    }

    @Test
    fun testInaccessibleClassSnapshotImplToString() {
        assertEquals("InaccessibleClassSnapshot", InaccessibleClassSnapshotImpl.toString())
    }

    private fun createSnapshot(classes: Map<String, Long>): org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot {
        val classSnapshots = linkedMapOf<String, org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshot>()
        for ([path, abiHash] in classes) {
            val classId = ClassId(FqName(path.substringBeforeLast("/").replace("/", ".")), FqName(path.substringAfterLast("/").removeSuffix(".class")), false)
            classSnapshots[path] = JavaClassSnapshot(classId, abiHash, null, emptyList())
        }
        return ClasspathEntrySnapshotImpl(ClasspathEntrySnapshot(classSnapshots))
    }
}
