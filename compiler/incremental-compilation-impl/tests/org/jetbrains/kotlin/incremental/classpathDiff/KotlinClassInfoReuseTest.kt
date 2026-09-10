/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.incremental.ClassProtoData
import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.ProtoData
import org.jetbrains.kotlin.incremental.classpathDiff.impl.BasicClassInfo
import org.jetbrains.kotlin.incremental.impl.ExtraClassInfoGenerator
import org.jetbrains.kotlin.incremental.storage.fromByteArray
import org.jetbrains.kotlin.incremental.storage.toByteArray
import org.jetbrains.kotlin.inline.InlineFunction
import org.jetbrains.kotlin.inline.InlinePropertyAccessor
import org.jetbrains.kotlin.inline.inlineFunctionsAndAccessors
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.BitEncoding
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.InvalidProtocolBufferException
import org.jetbrains.org.objectweb.asm.ClassReader
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.*

@OptIn(K1Deprecation::class)
class KotlinClassInfoReuseTest {

    @Test
    fun `class and companion snapshots retain constants and non-private inline members`() {
        val fixture = compiledFixture("MetadataReuseFixture.class")
        val info = assertEquivalent(fixture)
        assertEquals(KotlinClassHeader.Kind.CLASS, info.classKind)
        assertNotNull(info.extraInfo.classSnapshotExcludingMembers)
        assertEquals(setOf("VISIBLE_CONSTANT"), info.extraInfo.constantSnapshots.keys)
        assertEquals(
            setOf("visibleInline", "getVisibleProperty", "setVisibleProperty"),
            info.extraInfo.inlineFunctionOrAccessorSnapshots.keys.map { it.jvmMethodSignature.name }.toSet()
        )
        assertEquals(fixture.basicInfo.classId.createNestedClassId(Name.identifier("Factory")), info.companionObject)
        assertNull(info.constantsInCompanionObject)

        val companion = assertEquivalent(compiledFixture($$"MetadataReuseFixture$Factory.class"))
        assertNull(companion.companionObject)
        assertEquals(setOf("VISIBLE_CONSTANT", "HIDDEN_CONSTANT"), companion.constantsInCompanionObject!!.toSet())
        assertEquals(
            setOf("companionInline"),
            companion.extraInfo.inlineFunctionOrAccessorSnapshots.keys.map { it.jvmMethodSignature.name }.toSet()
        )
    }

    @Test
    fun `package snapshot retains Kotlin names JVM signatures and inline accessors`() {
        val info = assertEquivalent(compiledFixture("KotlinClassInfoReuseTestKt.class"))
        assertEquals(KotlinClassHeader.Kind.FILE_FACADE, info.classKind)
        assertNull(info.extraInfo.classSnapshotExcludingMembers)
        assertEquals(setOf("REUSE_PACKAGE_CONSTANT"), info.extraInfo.constantSnapshots.keys)
        assertEquals(
            setOf(
                InlineFunction(JvmMemberSignature.Method("renamedReusePackageInline", "(I)I"), "reusePackageInline"),
                InlinePropertyAccessor(JvmMemberSignature.Method("getReusePackageProperty", "()I"), "reusePackageProperty"),
                InlinePropertyAccessor(JvmMemberSignature.Method("setReusePackageProperty", "(I)V"), "reusePackageProperty"),
            ),
            info.extraInfo.inlineFunctionOrAccessorSnapshots.keys
        )
        assertEquals(info.classId.packageFqName, assertIs<PackagePartProtoData>(info.protoData).packageFqName)
        assertNull(info.companionObject)
        assertNull(info.constantsInCompanionObject)
    }

    @Test
    fun `multifile parts retain package proto and facade rejects proto access`() {
        val testDataDir = ForTestCompileRuntime.transformTestDataPath(
            "compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/" +
                    "ClasspathSnapshotterTest/kotlin/testPackageFacadeClasses/classes/com/example"
        )
        for (part in listOf("MultifileClass__MultifileClass1Kt", "MultifileClass__MultifileClass2Kt")) {
            val info = assertEquivalent(Fixture(File(testDataDir, "$part.class").readBytes()))
            assertEquals(KotlinClassHeader.Kind.MULTIFILE_CLASS_PART, info.classKind)
            assertEquals("com/example/MultifileClass", info.multifileClassName)
            assertIs<PackagePartProtoData>(info.protoData)
        }

        val fixture = Fixture(File(testDataDir, "MultifileClass.class").readBytes())
        val info = assertEquivalent(fixture, compareProto = false)
        assertEquals(KotlinClassHeader.Kind.MULTIFILE_CLASS, info.classKind)
        assertTrue(info.extraInfo.inlineFunctionOrAccessorSnapshots.isEmpty())
        val oldFailure = assertFailsWith<IllegalStateException> { fixture.oldInfo().protoData }
        val newFailure = assertFailsWith<IllegalStateException> { info.protoData }
        assertEquals(oldFailure.message, newFailure.message)
        assertContains(newFailure.message!!, "MULTIFILE_CLASS")
    }

    @Test
    fun `reader factory retains decoded proto while byte array factory keeps proto access lazy`() {
        for (name in listOf("MetadataReuseFixture.class", "KotlinClassInfoReuseTestKt.class")) {
            val fixture = compiledFixture(name)
            val expectedProto = fixture.oldInfo().protoData
            val header = fixture.header.copy(data = fixture.header.data!!.copyOf())
            val oldInfo = fixture.oldInfo(header)
            val newInfo = fixture.newInfo(header)
            assertContentEquals(KotlinClassInfoExternalizer.toByteArray(oldInfo), KotlinClassInfoExternalizer.toByteArray(newInfo))

            // Corrupt the public metadata array before the first proto access: only the reader factory has retained the decoded proto.
            header.data!![0] = malformedData().single()
            assertFailsWith<InvalidProtocolBufferException> { oldInfo.protoData }
            val retainedProto = newInfo.protoData
            assertProtoEquals(expectedProto, retainedProto, fixture.header.strings!!.size)
            assertSame(retainedProto, newInfo.protoData)
        }
    }

    @Test
    fun `incompatible data is serialized and decoded lazily but not used for inline discovery`() {
        for (name in listOf("MetadataReuseFixture.class", "KotlinClassInfoReuseTestKt.class")) {
            val fixture = compiledFixture(name)
            assertTrue(fixture.oldInfo().extraInfo.inlineFunctionOrAccessorSnapshots.isNotEmpty())
            val header = fixture.header.copy(data = null, incompatibleData = fixture.header.data!!.copyOf())
            val info = assertEquivalent(fixture, header)
            assertContentEquals(fixture.header.data, info.classHeaderData)
            assertTrue(info.extraInfo.inlineFunctionOrAccessorSnapshots.isEmpty())
            assertProtoEquals(fixture.oldInfo().protoData, info.protoData, fixture.header.strings!!.size)

            val malformedHeader = header.copy(incompatibleData = malformedData())
            val malformedInfo = assertEquivalent(fixture, malformedHeader, compareProto = false)
            assertFailsWith<InvalidProtocolBufferException> { fixture.oldInfo(malformedHeader).protoData }
            assertFailsWith<InvalidProtocolBufferException> { malformedInfo.protoData }
        }
    }

    @Test
    fun `missing metadata does not trigger eager decoding or inline discovery`() {
        for (name in listOf("MetadataReuseFixture.class", "KotlinClassInfoReuseTestKt.class")) {
            val fixture = compiledFixture(name)
            val missingData = fixture.header.copy(data = null, incompatibleData = null)
            val info = assertEquivalent(fixture, missingData, compareProto = false)
            assertTrue(info.classHeaderData.isEmpty())
            assertTrue(info.extraInfo.inlineFunctionOrAccessorSnapshots.isEmpty())
            val oldFailure = assertFails { fixture.oldInfo(missingData).protoData }
            val newFailure = assertFails { info.protoData }
            assertEquals(oldFailure::class, newFailure::class)

            val missingStrings = fixture.header.copy(strings = null)
            val withoutStrings = assertEquivalent(fixture, missingStrings)
            assertTrue(withoutStrings.classHeaderStrings.isEmpty())
            assertTrue(withoutStrings.extraInfo.inlineFunctionOrAccessorSnapshots.isEmpty())

            val malformedWithoutStrings = fixture.header.copy(data = malformedData(), strings = null)
            val malformedInfo = assertEquivalent(fixture, malformedWithoutStrings, compareProto = false)
            assertFailsWith<InvalidProtocolBufferException> { fixture.oldInfo(malformedWithoutStrings).protoData }
            assertFailsWith<InvalidProtocolBufferException> { malformedInfo.protoData }
        }
    }

    @Test
    fun `malformed compatible metadata fails during creation in both factories`() {
        for (name in listOf("MetadataReuseFixture.class", "KotlinClassInfoReuseTestKt.class")) {
            val fixture = compiledFixture(name)
            val header = fixture.header.copy(data = malformedData())
            assertFailsWith<InvalidProtocolBufferException> { fixture.oldInfo(header) }
            assertFailsWith<InvalidProtocolBufferException> { fixture.newInfo(header) }
        }
    }

    @Test
    fun `extra info factory does not decode malformed metadata before proto access`() {
        val fixture = compiledFixture("MetadataReuseFixture.class")
        val header = fixture.header.copy(data = malformedData())
        val extraInfo = fixture.oldInfo().extraInfo
        val info = KotlinClassInfo.createFrom(fixture.basicInfo.classId, header, extraInfo)
        assertSame(extraInfo, info.extraInfo)
        assertContentEquals(header.data, info.classHeaderData)
        KotlinClassInfoExternalizer.toByteArray(info)
        assertFailsWith<InvalidProtocolBufferException> { info.protoData }
    }

    @Test
    fun `reader factory preserves custom inline hash generation`() {
        val fixture = compiledFixture("MetadataReuseFixture.class")
        val signatures = mutableListOf<JvmMemberSignature.Method>()
        val generator = object : ExtraClassInfoGenerator() {
            override fun calculateInlineMethodHash(
                methodSignature: JvmMemberSignature.Method,
                inlinedClassPrefix: String,
                ownMethodHash: Long
            ): Long {
                signatures.add(methodSignature)
                return ownMethodHash xor 1L
            }
        }
        val oldInfo = fixture.oldInfo()
        val info = KotlinClassInfo.createFrom(fixture.basicInfo.classId, fixture.header, fixture.reader, generator)
        val expectedHashes = oldInfo.extraInfo.inlineFunctionOrAccessorSnapshots.mapValues { it.value xor 1L }
        assertEquals(expectedHashes.keys.map { it.jvmMethodSignature }.toSet(), signatures.toSet())
        assertEquals(expectedHashes.size, signatures.size)
        assertEquals(expectedHashes, info.extraInfo.inlineFunctionOrAccessorSnapshots)
        assertEquals(oldInfo.extraInfo.constantSnapshots, info.extraInfo.constantSnapshots)
        assertEquals(oldInfo.extraInfo.classSnapshotExcludingMembers, info.extraInfo.classSnapshotExcludingMembers)
        assertProtoEquals(oldInfo.protoData, info.protoData, fixture.header.strings!!.size)
        assertSame(info.protoData, info.protoData)
    }

    private fun assertEquivalent(
        fixture: Fixture,
        header: KotlinClassHeader = fixture.header,
        compareProto: Boolean = true
    ): KotlinClassInfo {
        val oldInfo = fixture.oldInfo(header)
        val newInfo = fixture.newInfo(header)
        val oldBytes = KotlinClassInfoExternalizer.toByteArray(oldInfo)
        assertContentEquals(oldBytes, KotlinClassInfoExternalizer.toByteArray(newInfo))
        assertEquals(oldInfo.extraInfo.classSnapshotExcludingMembers, newInfo.extraInfo.classSnapshotExcludingMembers)
        assertEquals(oldInfo.extraInfo.constantSnapshots, newInfo.extraInfo.constantSnapshots)
        assertEquals(oldInfo.extraInfo.inlineFunctionOrAccessorSnapshots, newInfo.extraInfo.inlineFunctionOrAccessorSnapshots)

        val extraInfo = ExtraClassInfoGenerator().getExtraInfo(
            header, fixture.reader, inlineFunctionsAndAccessors(header, excludePrivateMembers = true)
        )
        assertEquals(oldInfo.extraInfo.inlineFunctionOrAccessorSnapshots, extraInfo.inlineFunctionOrAccessorSnapshots)
        assertEquals(oldInfo.extraInfo.constantSnapshots, extraInfo.constantSnapshots)
        assertEquals(oldInfo.extraInfo.classSnapshotExcludingMembers, extraInfo.classSnapshotExcludingMembers)

        assertEquals(oldInfo.protoMapValue.isPackageFacade, newInfo.protoMapValue.isPackageFacade)
        assertContentEquals(oldInfo.protoMapValue.bytes, newInfo.protoMapValue.bytes)
        assertContentEquals(oldInfo.protoMapValue.strings, newInfo.protoMapValue.strings)
        if (compareProto) {
            val retainedProto = newInfo.protoData
            assertSame(retainedProto, newInfo.protoData)
            assertProtoEquals(oldInfo.protoData, retainedProto, newInfo.classHeaderStrings.size)
            val restored = KotlinClassInfoExternalizer.fromByteArray(oldBytes)
            assertProtoEquals(restored.protoData, retainedProto, newInfo.classHeaderStrings.size)
            // Missing strings still permit parsing the proto, but not resolving companion names or constants.
            if (header.strings != null) {
                assertEquals(oldInfo.companionObject, newInfo.companionObject)
                assertEquals(oldInfo.constantsInCompanionObject, newInfo.constantsInCompanionObject)
            }
        }
        assertContentEquals(oldBytes, KotlinClassInfoExternalizer.toByteArray(newInfo))
        return newInfo
    }

    private fun assertProtoEquals(expected: ProtoData, actual: ProtoData, stringCount: Int) {
        val [expectedResolver, actualResolver] = when (expected) {
            is ClassProtoData -> {
                assertIs<ClassProtoData>(actual)
                assertContentEquals(expected.proto.toByteArray(), actual.proto.toByteArray())
                expected.nameResolver to actual.nameResolver
            }
            is PackagePartProtoData -> {
                assertIs<PackagePartProtoData>(actual)
                assertEquals(expected.packageFqName, actual.packageFqName)
                assertContentEquals(expected.proto.toByteArray(), actual.proto.toByteArray())
                expected.nameResolver to actual.nameResolver
            }
        }
        for (index in 0 until stringCount) {
            assertEquals(expectedResolver.getString(index), actualResolver.getString(index), "String table index $index")
        }
    }

    private fun compiledFixture(name: String): Fixture = Fixture(
        assertNotNull(javaClass.getResourceAsStream(name), "Missing compiled fixture $name").use { it.readBytes() }
    )

    // A length-delimited string table containing the invalid protobuf tag zero.
    private fun malformedData(): Array<String> = BitEncoding.encodeBytes(byteArrayOf(1, 0))

    private fun KotlinClassHeader.copy(
        data: Array<String>? = this.data,
        incompatibleData: Array<String>? = this.incompatibleData,
        strings: Array<String>? = this.strings
    ) = KotlinClassHeader(kind, metadataVersion, data, incompatibleData, strings, multifileClassName, extraInt, packageName)

    private class Fixture(val bytes: ByteArray) {
        val reader = ClassReader(bytes)
        val basicInfo = BasicClassInfo.compute(reader)
        val header = assertNotNull(basicInfo.kotlinClassHeader)

        fun oldInfo(header: KotlinClassHeader = this.header): KotlinClassInfo =
            KotlinClassInfo.createFrom(basicInfo.classId, header, bytes)

        fun newInfo(header: KotlinClassHeader = this.header): KotlinClassInfo =
            KotlinClassInfo.createFrom(basicInfo.classId, header, reader)
    }
}

@Suppress("unused", "NOTHING_TO_INLINE")
private class MetadataReuseFixture {
    inline fun visibleInline(value: Int): Int = value + 1
    private inline fun hiddenInline(value: Int): Int = value - 1

    inline var visibleProperty: Int
        get() = 42
        set(value) {
            require(value >= 0)
        }

    private inline var hiddenProperty: Int
        get() = -1
        set(value) {
            require(value < 0)
        }

    companion object Factory {
        const val VISIBLE_CONSTANT = 42
        private const val HIDDEN_CONSTANT = -1
        inline fun companionInline(value: Int): Int = value * 2
    }
}

@Suppress("unused")
internal const val REUSE_PACKAGE_CONSTANT = 7

@Suppress("unused")
private const val REUSE_PRIVATE_PACKAGE_CONSTANT = -7

@Suppress("unused", "NOTHING_TO_INLINE")
@JvmName("renamedReusePackageInline")
internal inline fun reusePackageInline(value: Int): Int = value + 7

@Suppress("unused", "NOTHING_TO_INLINE")
private inline fun reusePrivatePackageInline(value: Int): Int = value - 7

@Suppress("unused")
internal inline var reusePackageProperty: Int
    get() = 7
    set(value) {
        require(value >= 0)
    }

@Suppress("unused")
private inline var reusePrivatePackageProperty: Int
    get() = -7
    set(value) {
        require(value < 0)
    }
