/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.io.File

/**
 * Splits a serialized [ProtoBuf.PackageFragment] of a single source file into the [ProtoData] of every class it declares
 * plus one [PackagePartProtoData] for its top-level declarations, keyed by [ClassId].
 *
 * The package part gets a synthetic `<FileName>Kt` [ClassId] so that all entries share one map; the key is only used to
 * pair old and new versions of the same source file.
 */
internal class ProtoDataProvider(private val serializerProtocol: SerializerExtensionProtocol) {
    operator fun invoke(sourceFile: File, metadata: ByteArray): Map<ClassId, ProtoData> {
        val classes = hashMapOf<ClassId, ProtoData>()
        val proto = ProtoBuf.PackageFragment.parseFrom(metadata, serializerProtocol.extensionRegistry)
        val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

        proto.class_List.forEach {
            val classId = nameResolver.getClassId(it.fqName)
            classes[classId] = ClassProtoData(it, nameResolver)
        }

        proto.`package`.apply {
            val packageNameId = getExtensionOrNull(serializerProtocol.packageFqName)
            val packageFqName = packageNameId?.let { FqName(nameResolver.getPackageFqName(it)) } ?: FqName.ROOT
            val packagePartClassId = ClassId(packageFqName, Name.identifier(sourceFile.nameWithoutExtension.capitalizeAsciiOnly() + "Kt"))
            classes[packagePartClassId] = PackagePartProtoData(this, nameResolver, packageFqName)
        }

        return classes
    }
}
