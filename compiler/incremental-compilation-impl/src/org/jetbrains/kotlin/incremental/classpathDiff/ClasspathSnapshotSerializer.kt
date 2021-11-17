/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.JavaClassProtoMapValueExternalizer
import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Utility to serialize a [ClasspathSnapshot]. */
object CachedClasspathSnapshotSerializer {
    private val cache = ConcurrentHashMap<File, ClasspathEntrySnapshot>()
    private const val RECOMMENDED_MAX_CACHE_SIZE = 100

    fun load(classpathEntrySnapshotFiles: List<File>): ClasspathSnapshot {
        return ClasspathSnapshot(classpathEntrySnapshotFiles.map { snapshotFile ->
            cache.computeIfAbsent(snapshotFile) {
                ClasspathEntrySnapshotExternalizer.loadFromFile(it)
            }
        }).also {
            handleCacheEviction(recentlyReferencedKeys = classpathEntrySnapshotFiles)
        }
    }

    private fun handleCacheEviction(recentlyReferencedKeys: List<File>) {
        if (cache.size > RECOMMENDED_MAX_CACHE_SIZE) {
            // Remove old entries.
            // Note:
            //   - The cache entries after eviction = recently-referenced entries + some other entries (so that
            //     size = RECOMMENDED_MAX_CACHE_SIZE)
            //       + Removed entries don't have to be the oldest (for simplicity).
            //       + If recentlyReferencedKeys.size > RECOMMENDED_MAX_CACHE_SIZE, all of them will be kept. The reason is that
            //         recently-referenced entries will likely be used again, so we keep them even if the cache is larger than recommended.
            //   - It's okay to have race condition in this method.
            val oldKeys = cache.keys - recentlyReferencedKeys.toSet()
            for (oldKey in oldKeys) {
                cache.remove(oldKey)
                if (cache.size <= RECOMMENDED_MAX_CACHE_SIZE) break
            }
        }
    }
}

object ClasspathEntrySnapshotExternalizer : DataExternalizer<ClasspathEntrySnapshot> {

    override fun save(output: DataOutput, snapshot: ClasspathEntrySnapshot) {
        LinkedHashMapExternalizer(StringExternalizer, ClassSnapshotWithHashExternalizer).save(output, snapshot.classSnapshots)
    }

    override fun read(input: DataInput): ClasspathEntrySnapshot {
        return ClasspathEntrySnapshot(
            classSnapshots = LinkedHashMapExternalizer(StringExternalizer, ClassSnapshotWithHashExternalizer).read(input)
        )
    }
}

object ClassSnapshotExternalizer : DataExternalizer<ClassSnapshot> {

    override fun save(output: DataOutput, snapshot: ClassSnapshot) {
        when (snapshot) {
            is KotlinClassSnapshot -> {
                output.writeString(KotlinClassSnapshot::class.java.name)
                KotlinClassSnapshotExternalizer.save(output, snapshot)
            }
            is JavaClassSnapshot -> {
                output.writeString(JavaClassSnapshot::class.java.name)
                JavaClassSnapshotExternalizer.save(output, snapshot)
            }
            is InaccessibleClassSnapshot -> {
                output.writeString(InaccessibleClassSnapshot::class.java.name)
                InaccessibleClassSnapshotExternalizer.save(output, snapshot)
            }
        }
    }

    override fun read(input: DataInput): ClassSnapshot {
        return when (val className = input.readString()) {
            KotlinClassSnapshot::class.java.name -> KotlinClassSnapshotExternalizer.read(input)
            JavaClassSnapshot::class.java.name -> JavaClassSnapshotExternalizer.read(input)
            InaccessibleClassSnapshot::class.java.name -> InaccessibleClassSnapshotExternalizer.read(input)
            else -> error("Unrecognized class name: $className")
        }
    }
}

object ClassSnapshotWithHashExternalizer : DataExternalizer<ClassSnapshotWithHash> {

    override fun save(output: DataOutput, snapshot: ClassSnapshotWithHash) {
        ClassSnapshotExternalizer.save(output, snapshot.classSnapshot)
        LongExternalizer.save(output, snapshot.hash)
    }

    override fun read(input: DataInput): ClassSnapshotWithHash {
        return ClassSnapshotWithHash(
            classSnapshot = ClassSnapshotExternalizer.read(input),
            hash = LongExternalizer.read(input)
        )
    }
}

object KotlinClassSnapshotExternalizer : DataExternalizer<KotlinClassSnapshot> {

    override fun save(output: DataOutput, snapshot: KotlinClassSnapshot) {
        KotlinClassInfoExternalizer.save(output, snapshot.classInfo)
        ListExternalizer(JvmClassNameExternalizer).save(output, snapshot.supertypes)
    }

    override fun read(input: DataInput): KotlinClassSnapshot {
        return KotlinClassSnapshot(
            classInfo = KotlinClassInfoExternalizer.read(input),
            supertypes = ListExternalizer(JvmClassNameExternalizer).read(input)
        )
    }
}

object KotlinClassInfoExternalizer : DataExternalizer<KotlinClassInfo> {

    override fun save(output: DataOutput, info: KotlinClassInfo) {
        ClassIdExternalizer.save(output, info.classId)
        output.writeInt(info.classKind.id)
        ListExternalizer(StringExternalizer).save(output, info.classHeaderData.toList())
        ListExternalizer(StringExternalizer).save(output, info.classHeaderStrings.toList())
        NullableValueExternalizer(StringExternalizer).save(output, info.multifileClassName)
        LinkedHashMapExternalizer(StringExternalizer, ConstantExternalizer).save(output, info.constantsMap)
        LinkedHashMapExternalizer(StringExternalizer, LongExternalizer).save(output, info.inlineFunctionsMap)
    }

    override fun read(input: DataInput): KotlinClassInfo {
        return KotlinClassInfo(
            classId = ClassIdExternalizer.read(input),
            classKind = KotlinClassHeader.Kind.getById(input.readInt()),
            classHeaderData = ListExternalizer(StringExternalizer).read(input).toTypedArray(),
            classHeaderStrings = ListExternalizer(StringExternalizer).read(input).toTypedArray(),
            multifileClassName = NullableValueExternalizer(StringExternalizer).read(input),
            constantsMap = LinkedHashMapExternalizer(StringExternalizer, ConstantExternalizer).read(input),
            inlineFunctionsMap = LinkedHashMapExternalizer(StringExternalizer, LongExternalizer).read(input)
        )
    }
}

object JavaClassSnapshotExternalizer : DataExternalizer<JavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: JavaClassSnapshot) {
        output.writeString(snapshot.javaClass.name)
        when (snapshot) {
            is RegularJavaClassSnapshot -> RegularJavaClassSnapshotExternalizer.save(output, snapshot)
            is ProtoBasedJavaClassSnapshot -> ProtoBasedJavaClassSnapshotExternalizer.save(output, snapshot)
            is ContentHashJavaClassSnapshot -> ContentHashJavaClassSnapshotExternalizer.save(output, snapshot)
        }
    }

    override fun read(input: DataInput): JavaClassSnapshot {
        return when (val className = input.readString()) {
            RegularJavaClassSnapshot::class.java.name -> RegularJavaClassSnapshotExternalizer.read(input)
            ProtoBasedJavaClassSnapshot::class.java.name -> ProtoBasedJavaClassSnapshotExternalizer.read(input)
            ContentHashJavaClassSnapshot::class.java.name -> ContentHashJavaClassSnapshotExternalizer.read(input)
            else -> error("Unrecognized class name: $className")
        }
    }
}

object RegularJavaClassSnapshotExternalizer : DataExternalizer<RegularJavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: RegularJavaClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        ListExternalizer(JvmClassNameExternalizer).save(output, snapshot.supertypes)
        AbiSnapshotExternalizer.save(output, snapshot.classAbiExcludingMembers)
        ListExternalizer(AbiSnapshotExternalizer).save(output, snapshot.fieldsAbi)
        ListExternalizer(AbiSnapshotExternalizer).save(output, snapshot.methodsAbi)
    }

    override fun read(input: DataInput): RegularJavaClassSnapshot {
        return RegularJavaClassSnapshot(
            classId = ClassIdExternalizer.read(input),
            supertypes = ListExternalizer(JvmClassNameExternalizer).read(input),
            classAbiExcludingMembers = AbiSnapshotExternalizer.read(input),
            fieldsAbi = ListExternalizer(AbiSnapshotExternalizer).read(input),
            methodsAbi = ListExternalizer(AbiSnapshotExternalizer).read(input)
        )
    }
}

object AbiSnapshotExternalizer : DataExternalizer<AbiSnapshot> {

    override fun save(output: DataOutput, value: AbiSnapshot) {
        output.writeString(value.name)
        LongExternalizer.save(output, value.abiHash)
    }

    override fun read(input: DataInput): AbiSnapshot {
        return AbiSnapshot(name = input.readString(), abiHash = LongExternalizer.read(input))
    }
}

object ProtoBasedJavaClassSnapshotExternalizer : DataExternalizer<ProtoBasedJavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: ProtoBasedJavaClassSnapshot) {
        JavaClassProtoMapValueExternalizer.save(output, snapshot.serializedJavaClass)
    }

    override fun read(input: DataInput): ProtoBasedJavaClassSnapshot {
        return ProtoBasedJavaClassSnapshot(serializedJavaClass = JavaClassProtoMapValueExternalizer.read(input))
    }
}

object InaccessibleClassSnapshotExternalizer : DataExternalizer<InaccessibleClassSnapshot> {

    override fun save(output: DataOutput, snapshot: InaccessibleClassSnapshot) {
        // Nothing to save
    }

    override fun read(input: DataInput): InaccessibleClassSnapshot {
        return InaccessibleClassSnapshot
    }
}

object ContentHashJavaClassSnapshotExternalizer : DataExternalizer<ContentHashJavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: ContentHashJavaClassSnapshot) {
        LongExternalizer.save(output, snapshot.contentHash)
    }

    override fun read(input: DataInput): ContentHashJavaClassSnapshot {
        return ContentHashJavaClassSnapshot(contentHash = LongExternalizer.read(input))
    }
}
