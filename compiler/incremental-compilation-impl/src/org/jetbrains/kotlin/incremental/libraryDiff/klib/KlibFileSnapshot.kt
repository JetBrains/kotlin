/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.ClassProtoData
import org.jetbrains.kotlin.incremental.PackagePartProtoData
import org.jetbrains.kotlin.incremental.ProtoData
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class KlibEntrySnapshot(
    val klibFileSnapshots: Set<KlibFileSnapshot>,
)

sealed interface KlibFileSnapshot {
    val id: String
    val contentHash: Long
}

/** [classInfos] and [packageMemberNames] include only accessible classes and non-private package members. */
class PackageFragmentSnapshot(
    override val id: String,
    override val contentHash: Long,
    val packageName: FqName,

    val classInfos: List<KlibClassInfo>,
    val packageMemberNames: Set<String>,

    val classMemberLevelSnapshot: PackageFragmentInfo?,
) : KlibFileSnapshot

class PackageFragmentInfo(
    val packageName: FqName,
    val packageFragmentProto: List<ByteArray>,
) {
    val proto: List<ProtoData> by lazy {
        buildList {
            for (proto in packageFragmentProto) {
                val fragment = parsePackageFragment(proto)
                val nameResolver = NameResolverImpl(fragment.strings, fragment.qualifiedNames)

                for (classProto in fragment.class_List) {
                    add(ClassProtoData(classProto, nameResolver))
                }

                add(PackagePartProtoData(fragment.`package`, nameResolver, packageName))
            }
        }
    }
}

class KlibClassInfo(
    val classId: ClassId,
    val supertypes: List<ClassId>,
)
