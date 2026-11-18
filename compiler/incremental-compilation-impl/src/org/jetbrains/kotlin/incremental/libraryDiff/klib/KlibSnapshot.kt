/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class KlibEntrySnapshot(
    val klibSnapshots: Set<KlibSnapshot>,
)

sealed interface KlibSnapshot {
    val id: String
    val contentHash: Long
}

class PackageFragmentSnapshot(
    override val id: String,
    override val contentHash: Long,
    val packageName: FqName,

    val classInfos: List<KlibClassInfo>,
    val packageMemberNames: Set<String>,

    val packageFragmentProto: List<ByteArray>?,
) : KlibSnapshot

class KlibClassInfo(
    val classId: ClassId,
    val supertypes: List<ClassId>,
)
