/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import com.intellij.util.io.DataExternalizer
import org.jetbrains.kotlin.incremental.storage.DelegateDataExternalizer
import java.io.DataInput
import java.io.DataOutput

// The klib counterpart of `ClasspathSnapshotSerializer.kt`.

internal object KlibEntrySnapshotExternalizer : DataExternalizer<KlibEntrySnapshot> {
    override fun save(output: DataOutput, snapshot: KlibEntrySnapshot): Unit = TODO("KT-90559: save KlibEntrySnapshot")
    override fun read(input: DataInput): KlibEntrySnapshot = TODO("KT-90559: read KlibEntrySnapshot")
}

internal object KlibSnapshotExternalizer : DataExternalizer<KlibSnapshot> by DelegateDataExternalizer(
    types = listOf(PackageFragmentSnapshot::class.java),
    typesExternalizers = listOf(PackageFragmentSnapshotExternalizer),
)

internal object PackageFragmentSnapshotExternalizer : DataExternalizer<PackageFragmentSnapshot> {
    override fun save(output: DataOutput, snapshot: PackageFragmentSnapshot): Unit = TODO("KT-90559: save PackageFragmentSnapshot")
    override fun read(input: DataInput): PackageFragmentSnapshot = TODO("KT-90559: read PackageFragmentSnapshot")
}

internal object KlibClassInfoExternalizer : DataExternalizer<KlibClassInfo> {
    override fun save(output: DataOutput, info: KlibClassInfo): Unit = TODO("KT-90559: save KlibClassInfo")
    override fun read(input: DataInput): KlibClassInfo = TODO("KT-90559: read KlibClassInfo")
}
