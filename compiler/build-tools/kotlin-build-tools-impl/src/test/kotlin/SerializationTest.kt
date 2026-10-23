@file:OptIn(ExperimentalSerializationApi::class, ExperimentalSerializationApi::class)

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetType
import org.jetbrains.kotlin.buildtools.api.jvm.AccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathEntrySnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.InaccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.internal.AccessibleClassSnapshotImpl
import org.jetbrains.kotlin.buildtools.internal.ClasspathEntrySnapshotImpl
import org.jetbrains.kotlin.buildtools.internal.DaemonExecutionPolicyImpl
import org.jetbrains.kotlin.buildtools.internal.InProcessExecutionPolicyImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.JvmCompilerArgumentsImpl
import org.jetbrains.kotlin.buildtools.internal.arguments.enums.JvmDefaultMode
import org.jetbrains.kotlin.buildtools.internal.serializability.ClasspathEntrySnapshotSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.KlibTargetIdSerializer
import org.jetbrains.kotlin.incremental.classpathDiff.JavaClassSnapshot
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.Path
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class SerializationTest {

    interface Serializer {
        fun <T : Any> serializeAndDeserialize(value: T, clazz: KClass<T>): T

        object Protobuf : Serializer {
            override fun <T : Any> serializeAndDeserialize(value: T, clazz: KClass<T>): T {
                val byteArray = ProtoBuf.encodeToByteArray(serializer(clazz.java), value)
                @Suppress("UNCHECKED_CAST")
                return ProtoBuf.decodeFromByteArray(serializer(clazz.java), byteArray) as T
            }
        }
    }

    inline fun <reified T : Any> Serializer.serializeAndDeserialize(value: T): T = serializeAndDeserialize(value, T::class)

    val serializer = Serializer.Protobuf

    @Test
    @DisplayName("Argument classes are serialized correctly")
    fun argumentClasses() {
        val jvmArguments = JvmCompilerArgumentsImpl()
        jvmArguments.jdkHome = Path("/someBuildFile.xml")
        jvmArguments.jvmDefault = JvmDefaultMode.NO_COMPATIBILITY.stringValue
        val jvmArguments2 = serializer.serializeAndDeserialize<JvmCompilerArgumentsImpl>(jvmArguments)
        assertEquals(jvmArguments.jdkHome, jvmArguments2.jdkHome)
        assertEquals(jvmArguments.jvmDefault, jvmArguments2.jvmDefault)
        assertEquals(jvmArguments.toArgumentStrings(), jvmArguments2.toArgumentStrings())
    }

    @Test
    @DisplayName("ExecutionPolicy classes are serialized correctly")
    fun executionPolicies() {
        val inProcess = InProcessExecutionPolicyImpl
        val inProcess2 = serializer.serializeAndDeserialize<InProcessExecutionPolicyImpl>(inProcess)
        assertEquals(inProcess, inProcess2)

        val withDaemon = DaemonExecutionPolicyImpl()
        withDaemon.logsPath = Path("/someBuildFile.xml")
        withDaemon.jvmArguments = listOf("abc")
        val withDaemon2 = serializer.serializeAndDeserialize<DaemonExecutionPolicyImpl>(withDaemon)
        assertEquals(withDaemon, withDaemon2)
    }

    @Test
    @DisplayName("KlibTargetId is serialized correctly")
    fun klibTargetId() {
        val targetId = KlibTargetId(KlibTargetType.IOS_ARM64, "iosArm64Custom")
        val byteArray = ProtoBuf.encodeToByteArray(KlibTargetIdSerializer, targetId)
        val deserialized = ProtoBuf.decodeFromByteArray(KlibTargetIdSerializer, byteArray)
        assertEquals(targetId, deserialized)
    }

    @Test
    @DisplayName("ClasspathEntrySnapshot is serialized correctly")
    fun classpathEntrySnapshot() {
        val origin = org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshot(
            linkedMapOf(
                "com/example/A.class" to JavaClassSnapshot(
                    ClassId(FqName("com.example"), FqName("A"), false),
                    42L,
                    null,
                    emptyList()
                ),
                "com/example/B.class" to org.jetbrains.kotlin.incremental.classpathDiff.InaccessibleClassSnapshot,
            )
        )
        val snapshot: ClasspathEntrySnapshot = ClasspathEntrySnapshotImpl(origin)

        val byteArray = ProtoBuf.encodeToByteArray(ClasspathEntrySnapshotSerializer, snapshot)
        val deserialized = ProtoBuf.decodeFromByteArray(ClasspathEntrySnapshotSerializer, byteArray)

        assertEquals(snapshot, deserialized)
        val classSnapshots = deserialized.classSnapshots
        assertEquals(2, classSnapshots.size)
        val classA = classSnapshots["com/example/A.class"]
        val classB = classSnapshots["com/example/B.class"]
        assertEquals(AccessibleClassSnapshotImpl(42L), classA)
        assertEquals(42L, (classA as AccessibleClassSnapshot).classAbiHash)
        assertEquals(true, classB is InaccessibleClassSnapshot)
    }
}
