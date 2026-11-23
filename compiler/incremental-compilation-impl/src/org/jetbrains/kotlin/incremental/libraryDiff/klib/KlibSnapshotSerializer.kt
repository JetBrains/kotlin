/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.classpathDiff.ClassIdExternalizerWithInterning
import org.jetbrains.kotlin.incremental.classpathDiff.FqNameExternalizerWithInterning
import org.jetbrains.kotlin.incremental.storage.ByteArrayExternalizer
import org.jetbrains.kotlin.incremental.storage.ClassIdExternalizer
import org.jetbrains.kotlin.incremental.storage.DelegateDataExternalizer
import org.jetbrains.kotlin.incremental.storage.FqNameExternalizer
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.LongExternalizer
import org.jetbrains.kotlin.incremental.storage.NullableValueExternalizer
import org.jetbrains.kotlin.incremental.storage.SetExternalizer
import org.jetbrains.kotlin.incremental.storage.StringExternalizer
import java.io.DataInput
import java.io.DataOutput

internal object KlibEntrySnapshotExternalizer : DataExternalizer<KlibEntrySnapshot> {

    override fun save(output: DataOutput, snapshot: KlibEntrySnapshot) {
        SetExternalizer(KlibSnapshotExternalizer).save(output, snapshot.klibFileSnapshots)
    }

    override fun read(input: DataInput): KlibEntrySnapshot {
        return KlibEntrySnapshot(
            klibFileSnapshots = SetExternalizer(KlibSnapshotExternalizer).read(input),
        )
    }
}

internal object KlibSnapshotExternalizer : DataExternalizer<KlibFileSnapshot> by DelegateDataExternalizer(
    types = listOf(PackageFragmentSnapshot::class.java),
    typesExternalizers = listOf(PackageFragmentSnapshotExternalizer),
)

internal object PackageFragmentSnapshotExternalizer : DataExternalizer<PackageFragmentSnapshot> {

    override fun save(output: DataOutput, snapshot: PackageFragmentSnapshot) {
        StringExternalizer.save(output, snapshot.id)
        LongExternalizer.save(output, snapshot.contentHash)
        FqNameExternalizer.save(output, snapshot.packageName)
        ListExternalizer(KlibClassInfoExternalizer).save(output, snapshot.classInfos)
        SetExternalizer(StringExternalizer).save(output, snapshot.packageMemberNames)
        // The package name of `classMemberLevelSnapshot` is the same as the snapshot's, so only the fragment bytes are saved
        NullableValueExternalizer(ListExternalizer(ByteArrayExternalizer)).save(output, snapshot.classMemberLevelSnapshot?.packageFragmentProto)
    }

    override fun read(input: DataInput): PackageFragmentSnapshot {
        val id = StringExternalizer.read(input)
        val contentHash = LongExternalizer.read(input)
        // To reduce memory usage, apply object interning to the package name as it is commonly shared
        val packageName = FqNameExternalizerWithInterning.read(input)
        return PackageFragmentSnapshot(
            id = id,
            contentHash = contentHash,
            packageName = packageName,
            classInfos = ListExternalizer(KlibClassInfoExternalizer).read(input),
            packageMemberNames = SetExternalizer(StringExternalizer).read(input),
            classMemberLevelSnapshot = NullableValueExternalizer(ListExternalizer(ByteArrayExternalizer)).read(input)
                ?.let { PackageFragmentInfo(packageName, it) },
        )
    }
}

internal object KlibClassInfoExternalizer : DataExternalizer<KlibClassInfo> {

    override fun save(output: DataOutput, info: KlibClassInfo) {
        ClassIdExternalizer.save(output, info.classId)
        ListExternalizer(ClassIdExternalizer).save(output, info.supertypes)
    }

    override fun read(input: DataInput): KlibClassInfo {
        return KlibClassInfo(
            // To reduce memory usage, apply object interning to package names of class ids as they are commonly shared
            classId = ClassIdExternalizerWithInterning.read(input),
            supertypes = ListExternalizer(ClassIdExternalizerWithInterning).read(input),
        )
    }
}
