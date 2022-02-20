/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.intellij.util.io.DataExternalizer
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

open class DataExternalizerForSealedClass<T>(
    val baseClass: Class<T>,
    val inheritorClasses: List<Class<out T>>,
    val inheritorExternalizers: List<DataExternalizer<*>>
) : DataExternalizer<T> {

    override fun save(output: DataOutput, objectToExternalize: T) {
        val inheritorClassIndex =
            inheritorClasses.indexOfFirst { it.isAssignableFrom(objectToExternalize!!::class.java) }.also { check(it != -1) }
        output.writeInt(inheritorClassIndex)
        @Suppress("UNCHECKED_CAST")
        (inheritorExternalizers[inheritorClassIndex] as DataExternalizer<T>).save(output, objectToExternalize)
    }

    override fun read(input: DataInput): T {
        val inheritorClassIndex = input.readInt()
        @Suppress("UNCHECKED_CAST")
        return inheritorExternalizers[inheritorClassIndex].read(input) as T
    }
}

object ClasspathEntrySnapshotExternalizer : DataExternalizer<ClasspathEntrySnapshot> {

    override fun save(output: DataOutput, snapshot: ClasspathEntrySnapshot) {
        LinkedHashMapExternalizer(StringExternalizer, ClassSnapshotExternalizer).save(output, snapshot.classSnapshots)
    }

    override fun read(input: DataInput): ClasspathEntrySnapshot {
        return ClasspathEntrySnapshot(
            classSnapshots = LinkedHashMapExternalizer(StringExternalizer, ClassSnapshotExternalizer).read(input)
        )
    }
}

object ClassSnapshotExternalizer : DataExternalizerForSealedClass<ClassSnapshot>(
    baseClass = ClassSnapshot::class.java,
    inheritorClasses = listOf(AccessibleClassSnapshot::class.java, InaccessibleClassSnapshot::class.java),
    inheritorExternalizers = listOf(AccessibleClassSnapshotExternalizer, InaccessibleClassSnapshotExternalizer)
)

object AccessibleClassSnapshotExternalizer : DataExternalizerForSealedClass<AccessibleClassSnapshot>(
    baseClass = AccessibleClassSnapshot::class.java,
    inheritorClasses = listOf(KotlinClassSnapshot::class.java, JavaClassSnapshot::class.java),
    inheritorExternalizers = listOf(KotlinClassSnapshotExternalizer, JavaClassSnapshotExternalizer)
)

object KotlinClassSnapshotExternalizer : DataExternalizerForSealedClass<KotlinClassSnapshot>(
    baseClass = KotlinClassSnapshot::class.java,
    inheritorClasses = listOf(
        RegularKotlinClassSnapshot::class.java,
        PackageFacadeKotlinClassSnapshot::class.java,
        MultifileClassKotlinClassSnapshot::class.java
    ),
    inheritorExternalizers = listOf(
        RegularKotlinClassSnapshotExternalizer,
        PackageFacadeKotlinClassSnapshotExternalizer,
        MultifileClassKotlinClassSnapshotExternalizer
    )
)

object RegularKotlinClassSnapshotExternalizer : DataExternalizer<RegularKotlinClassSnapshot> {

    override fun save(output: DataOutput, snapshot: RegularKotlinClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        LongExternalizer.save(output, snapshot.classAbiHash)
        NullableValueExternalizer(KotlinClassInfoExternalizer).save(output, snapshot.classMemberLevelSnapshot)
        ListExternalizer(JvmClassNameExternalizer).save(output, snapshot.supertypes)
    }

    override fun read(input: DataInput): RegularKotlinClassSnapshot {
        return RegularKotlinClassSnapshot(
            classId = ClassIdExternalizer.read(input),
            classAbiHash = LongExternalizer.read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(KotlinClassInfoExternalizer).read(input),
            supertypes = ListExternalizer(JvmClassNameExternalizer).read(input)
        )
    }
}

object PackageFacadeKotlinClassSnapshotExternalizer : DataExternalizer<PackageFacadeKotlinClassSnapshot> {

    override fun save(output: DataOutput, snapshot: PackageFacadeKotlinClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        LongExternalizer.save(output, snapshot.classAbiHash)
        NullableValueExternalizer(KotlinClassInfoExternalizer).save(output, snapshot.classMemberLevelSnapshot)
        PackageMemberSetExternalizer.save(output, snapshot.packageMembers)
    }

    override fun read(input: DataInput): PackageFacadeKotlinClassSnapshot {
        return PackageFacadeKotlinClassSnapshot(
            classId = ClassIdExternalizer.read(input),
            classAbiHash = LongExternalizer.read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(KotlinClassInfoExternalizer).read(input),
            packageMembers = PackageMemberSetExternalizer.read(input)
        )
    }
}

object MultifileClassKotlinClassSnapshotExternalizer : DataExternalizer<MultifileClassKotlinClassSnapshot> {

    override fun save(output: DataOutput, snapshot: MultifileClassKotlinClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        LongExternalizer.save(output, snapshot.classAbiHash)
        NullableValueExternalizer(KotlinClassInfoExternalizer).save(output, snapshot.classMemberLevelSnapshot)
        PackageMemberSetExternalizer.save(output, snapshot.constants)
    }

    override fun read(input: DataInput): MultifileClassKotlinClassSnapshot {
        return MultifileClassKotlinClassSnapshot(
            classId = ClassIdExternalizer.read(input),
            classAbiHash = LongExternalizer.read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(KotlinClassInfoExternalizer).read(input),
            constants = PackageMemberSetExternalizer.read(input)
        )
    }
}

object KotlinClassInfoExternalizer : DataExternalizer<KotlinClassInfo> {

    override fun save(output: DataOutput, info: KotlinClassInfo) {
        ClassIdExternalizer.save(output, info.classId)
        IntExternalizer.save(output, info.classKind.id)
        ListExternalizer(StringExternalizer).save(output, info.classHeaderData.toList())
        ListExternalizer(StringExternalizer).save(output, info.classHeaderStrings.toList())
        NullableValueExternalizer(StringExternalizer).save(output, info.multifileClassName)
        LinkedHashMapExternalizer(StringExternalizer, ConstantExternalizer).save(output, info.constantsMap)
        LinkedHashMapExternalizer(StringExternalizer, LongExternalizer).save(output, info.inlineFunctionsMap)
    }

    override fun read(input: DataInput): KotlinClassInfo {
        return KotlinClassInfo(
            classId = ClassIdExternalizer.read(input),
            classKind = KotlinClassHeader.Kind.getById(IntExternalizer.read(input)),
            classHeaderData = ListExternalizer(StringExternalizer).read(input).toTypedArray(),
            classHeaderStrings = ListExternalizer(StringExternalizer).read(input).toTypedArray(),
            multifileClassName = NullableValueExternalizer(StringExternalizer).read(input),
            constantsMap = LinkedHashMapExternalizer(StringExternalizer, ConstantExternalizer).read(input),
            inlineFunctionsMap = LinkedHashMapExternalizer(StringExternalizer, LongExternalizer).read(input)
        )
    }
}

object PackageMemberSetExternalizer : DataExternalizer<PackageMemberSet> {

    override fun save(output: DataOutput, set: PackageMemberSet) {
        MapExternalizer(FqNameExternalizer, SetExternalizer(StringExternalizer)).save(output, set.packageToMembersMap)
    }

    override fun read(input: DataInput): PackageMemberSet {
        return PackageMemberSet(
            packageToMembersMap = MapExternalizer(FqNameExternalizer, SetExternalizer(StringExternalizer)).read(input)
        )
    }
}

object JavaClassSnapshotExternalizer : DataExternalizer<JavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: JavaClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        LongExternalizer.save(output, snapshot.classAbiHash)
        NullableValueExternalizer(JavaClassMemberLevelSnapshotExternalizer).save(output, snapshot.classMemberLevelSnapshot)
        ListExternalizer(JvmClassNameExternalizer).save(output, snapshot.supertypes)
    }

    override fun read(input: DataInput): JavaClassSnapshot {
        return JavaClassSnapshot(
            classId = ClassIdExternalizer.read(input),
            classAbiHash = LongExternalizer.read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(JavaClassMemberLevelSnapshotExternalizer).read(input),
            supertypes = ListExternalizer(JvmClassNameExternalizer).read(input)
        )
    }
}

object JavaClassMemberLevelSnapshotExternalizer : DataExternalizer<JavaClassMemberLevelSnapshot> {

    override fun save(output: DataOutput, snapshot: JavaClassMemberLevelSnapshot) {
        JavaElementSnapshotExternalizer.save(output, snapshot.classAbiExcludingMembers)
        ListExternalizer(JavaElementSnapshotExternalizer).save(output, snapshot.fieldsAbi)
        ListExternalizer(JavaElementSnapshotExternalizer).save(output, snapshot.methodsAbi)
    }

    override fun read(input: DataInput): JavaClassMemberLevelSnapshot {
        return JavaClassMemberLevelSnapshot(
            classAbiExcludingMembers = JavaElementSnapshotExternalizer.read(input),
            fieldsAbi = ListExternalizer(JavaElementSnapshotExternalizer).read(input),
            methodsAbi = ListExternalizer(JavaElementSnapshotExternalizer).read(input)
        )
    }
}

object JavaElementSnapshotExternalizer : DataExternalizer<JavaElementSnapshot> {

    override fun save(output: DataOutput, value: JavaElementSnapshot) {
        StringExternalizer.save(output, value.name)
        LongExternalizer.save(output, value.abiHash)
    }

    override fun read(input: DataInput): JavaElementSnapshot {
        return JavaElementSnapshot(
            name = StringExternalizer.read(input),
            abiHash = LongExternalizer.read(input)
        )
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
