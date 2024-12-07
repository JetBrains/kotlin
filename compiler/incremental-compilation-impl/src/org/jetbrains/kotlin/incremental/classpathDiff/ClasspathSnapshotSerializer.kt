/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import com.intellij.util.containers.Interner
import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.incremental.KotlinClassInfo
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.DataInput
import java.io.DataOutput
import java.io.File

/** Utility to serialize a [ClasspathSnapshot]. */
object CachedClasspathSnapshotSerializer {

    // Note: This cache is shared across builds, so we need to be careful if the snapshot file's path hasn't changed but its contents have
    // changed. Luckily, each snapshot file is currently the output of a Gradle (non-incremental) transform, so that case will not happen.
    // TODO: Make this code safer (not relying on how the snapshot files are produced and whether Gradle maintains the above guarantee). For
    // example, if the transform is incremental, the above case may happen (the output directory of an incremental transform is unchanged
    // even though its inputs/outputs have changed). Potential solutions: Write the file's content hash in the file's name or to another
    // file next to it, or check that its timestamp and size haven't changed (we'll need to deal with directories too).
    private val cache = InMemoryCacheWithEviction<File, ClasspathEntrySnapshot>(
        maxTimePeriodsToKeepStrongReferences = 20,
        maxTimePeriodsToKeepSoftReferences = 1000,
        maxMemoryUsageRatioToKeepStrongReferences = 0.8
    )

    fun load(classpathEntrySnapshotFiles: List<File>, reporter: ClasspathSnapshotBuildReporter): ClasspathSnapshot {
        cache.newTimePeriod()

        var cacheMisses = 0L
        val classpathSnapshot = ClasspathSnapshot(classpathEntrySnapshotFiles.map { snapshotFile ->
            cache.computeIfAbsent(snapshotFile) {
                cacheMisses++
                ClasspathEntrySnapshotExternalizer.loadFromFile(it)
            }
        })

        cache.evictEntries()
        reporter.addMetric(GradleBuildPerformanceMetric.LOAD_CLASSPATH_SNAPSHOT_EXECUTION_COUNT, 1)
        reporter.addMetric(GradleBuildPerformanceMetric.LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_HITS, classpathEntrySnapshotFiles.size - cacheMisses)
        reporter.addMetric(GradleBuildPerformanceMetric.LOAD_CLASSPATH_ENTRY_SNAPSHOT_CACHE_MISSES, cacheMisses)

        return classpathSnapshot
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

internal object ClassSnapshotExternalizer : DataExternalizer<ClassSnapshot> by DelegateDataExternalizer(
    types = listOf(AccessibleClassSnapshot::class.java, InaccessibleClassSnapshot::class.java),
    typesExternalizers = listOf(AccessibleClassSnapshotExternalizer, InaccessibleClassSnapshotExternalizer)
)

internal object AccessibleClassSnapshotExternalizer : DataExternalizer<AccessibleClassSnapshot> by DelegateDataExternalizer(
    types = listOf(KotlinClassSnapshot::class.java, JavaClassSnapshot::class.java),
    typesExternalizers = listOf(KotlinClassSnapshotExternalizer, JavaClassSnapshotExternalizer)
)

private object KotlinClassSnapshotExternalizer : DataExternalizer<KotlinClassSnapshot> by DelegateDataExternalizer(
    types = listOf(
        RegularKotlinClassSnapshot::class.java,
        PackageFacadeKotlinClassSnapshot::class.java,
        MultifileClassKotlinClassSnapshot::class.java
    ),
    typesExternalizers = listOf(
        RegularKotlinClassSnapshotExternalizer,
        PackageFacadeKotlinClassSnapshotExternalizer,
        MultifileClassKotlinClassSnapshotExternalizer
    )
)

private object RegularKotlinClassSnapshotExternalizer : DataExternalizer<RegularKotlinClassSnapshot> {

    override fun save(output: DataOutput, snapshot: RegularKotlinClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        LongExternalizer.save(output, snapshot.classAbiHash)
        NullableValueExternalizer(KotlinClassInfoExternalizer).save(output, snapshot.classMemberLevelSnapshot)
        ListExternalizer(JvmClassNameExternalizer).save(output, snapshot.supertypes)
        NullableValueExternalizer(StringExternalizer).save(output, snapshot.companionObjectName)
        NullableValueExternalizer(ListExternalizer(StringExternalizer)).save(output, snapshot.constantsInCompanionObject)
    }

    override fun read(input: DataInput): RegularKotlinClassSnapshot {
        return RegularKotlinClassSnapshot(
            // To reduce memory usage, apply object interning to classId's package name and supertypes as they are commonly shared
            classId = ClassIdExternalizerWithInterning.read(input),
            classAbiHash = LongExternalizer.read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(KotlinClassInfoExternalizer).read(input),
            supertypes = ListExternalizer(JvmClassNameExternalizerWithInterning).read(input),
            companionObjectName = NullableValueExternalizer(StringExternalizer).read(input),
            constantsInCompanionObject = NullableValueExternalizer(ListExternalizer(StringExternalizer)).read(input)
        )
    }
}

private object PackageFacadeKotlinClassSnapshotExternalizer : DataExternalizer<PackageFacadeKotlinClassSnapshot> {

    override fun save(output: DataOutput, snapshot: PackageFacadeKotlinClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        LongExternalizer.save(output, snapshot.classAbiHash)
        NullableValueExternalizer(KotlinClassInfoExternalizer).save(output, snapshot.classMemberLevelSnapshot)
        SetExternalizer(StringExternalizer).save(output, snapshot.packageMemberNames)
    }

    override fun read(input: DataInput): PackageFacadeKotlinClassSnapshot {
        return PackageFacadeKotlinClassSnapshot(
            // To reduce memory usage, apply object interning to classId's package name as they are commonly shared
            classId = ClassIdExternalizerWithInterning.read(input),
            classAbiHash = LongExternalizer.read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(KotlinClassInfoExternalizer).read(input),
            packageMemberNames = SetExternalizer(StringExternalizer).read(input)
        )
    }
}

private object MultifileClassKotlinClassSnapshotExternalizer : DataExternalizer<MultifileClassKotlinClassSnapshot> {

    override fun save(output: DataOutput, snapshot: MultifileClassKotlinClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        LongExternalizer.save(output, snapshot.classAbiHash)
        NullableValueExternalizer(KotlinClassInfoExternalizer).save(output, snapshot.classMemberLevelSnapshot)
        SetExternalizer(StringExternalizer).save(output, snapshot.constantNames)
    }

    override fun read(input: DataInput): MultifileClassKotlinClassSnapshot {
        return MultifileClassKotlinClassSnapshot(
            // To reduce memory usage, apply object interning to classId's package name as they are commonly shared
            classId = ClassIdExternalizerWithInterning.read(input),
            classAbiHash = LongExternalizer.read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(KotlinClassInfoExternalizer).read(input),
            constantNames = SetExternalizer(StringExternalizer).read(input)
        )
    }
}

internal object KotlinClassInfoExternalizer : DataExternalizer<KotlinClassInfo> {

    override fun save(output: DataOutput, info: KotlinClassInfo) {
        ClassIdExternalizer.save(output, info.classId)
        IntExternalizer.save(output, info.classKind.id)
        ListExternalizer(StringExternalizer).save(output, info.classHeaderData.toList())
        ListExternalizer(StringExternalizer).save(output, info.classHeaderStrings.toList())
        NullableValueExternalizer(StringExternalizer).save(output, info.multifileClassName)
        ExtraInfoExternalizer.save(output, info.extraInfo)
    }

    override fun read(input: DataInput): KotlinClassInfo {
        return KotlinClassInfo(
            // To reduce memory usage, apply object interning to classId's package name as they are commonly shared
            classId = ClassIdExternalizerWithInterning.read(input),
            classKind = KotlinClassHeader.Kind.getById(IntExternalizer.read(input)),
            classHeaderData = ListExternalizer(StringExternalizer).read(input).toTypedArray(),
            classHeaderStrings = ListExternalizer(StringExternalizer).read(input).toTypedArray(),
            multifileClassName = NullableValueExternalizer(StringExternalizer).read(input),
            extraInfo = ExtraInfoExternalizer.read(input)
        )
    }
}

private object ExtraInfoExternalizer : DataExternalizer<KotlinClassInfo.ExtraInfo> {

    override fun save(output: DataOutput, info: KotlinClassInfo.ExtraInfo) {
        NullableValueExternalizer(LongExternalizer).save(output, info.classSnapshotExcludingMembers)
        MapExternalizer(StringExternalizer, LongExternalizer).save(output, info.constantSnapshots)
        MapExternalizer(InlineFunctionOrAccessorExternalizer, LongExternalizer).save(output, info.inlineFunctionOrAccessorSnapshots)
    }

    override fun read(input: DataInput): KotlinClassInfo.ExtraInfo {
        return KotlinClassInfo.ExtraInfo(
            classSnapshotExcludingMembers = NullableValueExternalizer(LongExternalizer).read(input),
            constantSnapshots = MapExternalizer(StringExternalizer, LongExternalizer).read(input),
            inlineFunctionOrAccessorSnapshots = MapExternalizer(InlineFunctionOrAccessorExternalizer, LongExternalizer).read(input)
        )
    }
}

private object JavaClassSnapshotExternalizer : DataExternalizer<JavaClassSnapshot> {

    override fun save(output: DataOutput, snapshot: JavaClassSnapshot) {
        ClassIdExternalizer.save(output, snapshot.classId)
        LongExternalizer.save(output, snapshot.classAbiHash)
        NullableValueExternalizer(JavaClassMemberLevelSnapshotExternalizer).save(output, snapshot.classMemberLevelSnapshot)
        ListExternalizer(JvmClassNameExternalizer).save(output, snapshot.supertypes)
    }

    override fun read(input: DataInput): JavaClassSnapshot {
        return JavaClassSnapshot(
            // To reduce memory usage, apply object interning to classId's package name and supertypes as they are commonly shared
            classId = ClassIdExternalizerWithInterning.read(input),
            classAbiHash = LongExternalizer.read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(JavaClassMemberLevelSnapshotExternalizer).read(input),
            supertypes = ListExternalizer(JvmClassNameExternalizerWithInterning).read(input)
        )
    }
}

internal object JavaClassMemberLevelSnapshotExternalizer : DataExternalizer<JavaClassMemberLevelSnapshot> {

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

private object JavaElementSnapshotExternalizer : DataExternalizer<JavaElementSnapshot> {

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

private object InaccessibleClassSnapshotExternalizer : DataExternalizer<InaccessibleClassSnapshot> {

    override fun save(output: DataOutput, snapshot: InaccessibleClassSnapshot) {
        // Nothing to save
    }

    override fun read(input: DataInput): InaccessibleClassSnapshot {
        return InaccessibleClassSnapshot
    }
}

private object ClassIdExternalizerWithInterning : DataExternalizer<ClassId> by ClassIdExternalizer {

    override fun read(input: DataInput): ClassId {
        return ClassId(
            // To reduce memory usage, apply object interning to package name as they are commonly shared.
            // (Don't apply object interning to relative class name as they are not commonly shared.)
            packageFqName = FqNameExternalizerWithInterning.read(input),
            relativeClassName = FqNameExternalizer.read(input),
            isLocal = input.readBoolean()
        )
    }
}

private object FqNameExternalizerWithInterning : DataExternalizer<FqName> by FqNameExternalizer {

    private val fqNameInterner by lazy { Interner.createWeakInterner<FqName>() }

    override fun read(input: DataInput): FqName {
        return fqNameInterner.intern(FqNameExternalizer.read(input))
    }
}

private object JvmClassNameExternalizerWithInterning : DataExternalizer<JvmClassName> by JvmClassNameExternalizer {

    private val jvmClassNameInterner by lazy { Interner.createWeakInterner<JvmClassName>() }

    override fun read(input: DataInput): JvmClassName {
        return jvmClassNameInterner.intern(JvmClassNameExternalizer.read(input))
    }
}
