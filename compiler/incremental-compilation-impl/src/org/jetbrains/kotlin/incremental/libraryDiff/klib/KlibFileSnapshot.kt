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
import org.jetbrains.kotlin.serialization.deserialization.getClassId

class KlibEntrySnapshot(
    val klibFileSnapshots: List<KlibFileSnapshot>,
)

/**
 * A snapshot of one package fragment of a klib.
 *
 * [packageName] is not unique: a package may have several fragments, also across klibs. Fine-grained changes are computed per package.
 *
 * [classInfos] and [packageMemberNames] include only accessible classes and non-private package members.
 */
class KlibFileSnapshot(
    val packageName: FqName,
    val contentHash: Long,

    val classInfos: List<KlibClassInfo>,
    val packageMemberNames: Set<String>,

    val classMemberLevelSnapshot: KlibFileInfo?,
)

class KlibFileInfo(
    val packageName: FqName,
    val packageFragmentProto: ByteArray,
) {
    /** Like [KlibFileSnapshot.classInfos], includes only accessible classes. */
    val proto: List<ProtoData> by lazy {
        buildList {
            val fragment = parsePackageFragment(packageFragmentProto)
            val nameResolver = NameResolverImpl(fragment.strings, fragment.qualifiedNames)

            val classes = fragment.class_List.associateBy { nameResolver.getClassId(it.fqName) }
            for ([classId, classProto] in classes) {
                if (classId.isAccessibleIn(classes)) {
                    add(ClassProtoData(classProto, nameResolver))
                }
            }

            add(PackagePartProtoData(fragment.`package`, nameResolver, packageName))
        }
    }
}

class KlibClassInfo(
    val classId: ClassId,
    val supertypes: List<ClassId>,
)

