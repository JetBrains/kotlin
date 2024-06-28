/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.SerializableStringTable

fun buildKlibPackageFragment(
    packageProto: ProtoBuf.Package,
    classesProto: List<Pair<ProtoBuf.Class, Int>>,
    fqName: FqName,
    isEmpty: Boolean,
    stringTable: SerializableStringTable,
): ProtoBuf.PackageFragment {

    val (stringTableProto, nameTableProto) = stringTable.buildProto()

    return ProtoBuf.PackageFragment.newBuilder()
        .setPackage(packageProto)
        .addAllClass_(classesProto.map { it.first })
        .setStrings(stringTableProto)
        .setQualifiedNames(nameTableProto)
        .also { packageFragment ->
            classesProto.forEach {
                packageFragment.addExtension(KlibMetadataProtoBuf.className, it.second )
            }
            packageFragment.setExtension(KlibMetadataProtoBuf.isEmpty, isEmpty)
            packageFragment.setExtension(KlibMetadataProtoBuf.fqName, fqName.asString())
        }
        .build()
}
