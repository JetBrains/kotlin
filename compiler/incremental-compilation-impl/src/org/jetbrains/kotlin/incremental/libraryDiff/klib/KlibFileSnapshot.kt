/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.incremental.ClassProtoData
import org.jetbrains.kotlin.incremental.PackagePartProtoData
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
 * [packageName] and [sourceFile] (e.g. `Base.kt`) are not unique: one source file may have several package fragments, and same-named
 * source files of a package in different klibs have the same ones. Fragments with the same ones are compared together.
 *
 * [classInfos] and [packageMemberNames] include only accessible classes and non-private package members.
 */
class KlibFileSnapshot(
    val contentHash: Long,
    val packageName: FqName,
    val sourceFile: String,

    val classInfos: List<KlibClassInfo>,
    val packageMemberNames: Set<String>,

    val classMemberLevelSnapshot: KlibFileInfo?,
)

class KlibFileInfo(
    val packageName: FqName,
    val packageFragmentProto: ByteArray,
) {
    private val fragment by lazy { parsePackageFragment(packageFragmentProto) }
    private val nameResolver by lazy { NameResolverImpl(fragment.strings, fragment.qualifiedNames) }

    /** Like [KlibFileSnapshot.classInfos], includes only accessible classes. */
    val classProtoData: List<ClassProtoData> by lazy {
        val classes = fragment.class_List.associateBy { nameResolver.getClassId(it.fqName) }
        classes.filterKeys { it.isAccessibleIn(classes) }.values.map { ClassProtoData(it, nameResolver) }
    }

    val packagePartProtoData: PackagePartProtoData by lazy {
        PackagePartProtoData(fragment.`package`, nameResolver, packageName)
    }
}

class KlibClassInfo(
    val classId: ClassId,
    val supertypes: List<ClassId>,
)

