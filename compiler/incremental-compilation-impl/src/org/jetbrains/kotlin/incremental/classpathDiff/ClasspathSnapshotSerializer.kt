/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import java.io.DataInput
import java.io.DataOutput
import java.io.File

/** Utility to serialize a [ClasspathSnapshot]. */
object CachedClasspathSnapshotSerializer {

    // Note: This cache is shared across builds, so we need to be careful if the snapshot file's path hasn't changed but its contents have
    // changed. Luckily, each snapshot file is currently the output of a Gradle (non-incremental) transform, so that case will not happen.
    // TODO: Make this code safer (not relying on how the snapshot files are produced and whether Gradle maintains the above guarantee). For
    // example, if the transform is incremental, the above case may happen (the output directory of am incremental transform is unchanged
    // even though its inputs/outputs have changed). Potential solutions: Write the file's content hash in the file's
    // name or to another file next to it.
    private val cache = InMemoryCacheWithEviction<File, ClasspathEntrySnapshot>(maxTimePeriods = 20, maxMemoryUsageRatio = 0.8)

    fun load(classpathEntrySnapshotFiles: List<File>, reporter: ClasspathSnapshotBuildReporter): ClasspathSnapshot {
        cache.newTimePeriod()
        reporter.reportVerbose {
            val counts = cache.countCacheEntriesForDebug()
            @Suppress("SpellCheckingInspection")
            "Load classpath snapshot, cache size = ${counts.first + counts.second + counts.third}" +
                    " (${counts.first} strong refs, ${counts.second + counts.third} soft refs, ${counts.third} are gc'd)"
        }

        var cacheMisses: Long = 0
        val classpathSnapshot = ClasspathSnapshot(classpathEntrySnapshotFiles.map { snapshotFile ->
            cache.computeIfAbsent(snapshotFile) {
                cacheMisses++
                ClasspathEntrySnapshotExternalizer.loadFromFile(it)
            }
        })

        cache.evictEntries()
        reporter.addMetric(BuildPerformanceMetric.LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES, cacheMisses)
        reporter.reportVerbose { "Loaded classpath snapshot, cache misses = $cacheMisses / ${classpathEntrySnapshotFiles.size}" }

        return classpathSnapshot
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
        SetExternalizer(StringExternalizer).save(output, snapshot.packageMemberNames)
    }

    override fun read(input: DataInput): PackageFacadeKotlinClassSnapshot {
        return PackageFacadeKotlinClassSnapshot(
            classId = ClassIdExternalizer.read(input),
            classAbiHash = LongExternalizer.read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(KotlinClassInfoExternalizer).read(input),
            packageMemberNames = SetExternalizer(StringExternalizer).read(input)
        )
    }
}

object MultifileClassKotlinClassSnapshotExternalizer : DataExternalizer<MultifileClassKotlinClassSnapshot> {

    override fun save(output: DataOutput, snapshot: MultifileClassKotlinClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        LongExternalizer.save(output, snapshot.classAbiHash)
        NullableValueExternalizer(KotlinClassInfoExternalizer).save(output, snapshot.classMemberLevelSnapshot)
        SetExternalizer(StringExternalizer).save(output, snapshot.constantNames)
    }

    override fun read(input: DataInput): MultifileClassKotlinClassSnapshot {
        return MultifileClassKotlinClassSnapshot(
            classId = ClassIdExternalizer.read(input),
            classAbiHash = LongExternalizer.read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(KotlinClassInfoExternalizer).read(input),
            constantNames = SetExternalizer(StringExternalizer).read(input)
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
