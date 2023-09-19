/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.io.InputStream

class MetadataClassDataFinder(val finder: KotlinMetadataFinder) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassData? {
        val topLevelClassId = generateSequence(classId, ClassId::outerClassId).last()
        val stream = finder.findMetadata(topLevelClassId) ?: return null
        val (message, nameResolver, version) = readProto(stream)
        return message.class_List.firstOrNull { classProto ->
            nameResolver.getClassId(classProto.fqName) == classId
        }?.let { classProto ->
            ClassData(nameResolver, classProto, version, SourceElement.NO_SOURCE)
        }
    }
}

fun readProto(stream: InputStream): Triple<ProtoBuf.PackageFragment, NameResolverImpl, BuiltInsBinaryVersion> {
    val version = BuiltInsBinaryVersion.readFrom(stream)

    if (!version.isCompatibleWithCurrentCompilerVersion()) {
        // TODO: report a proper diagnostic
        throw UnsupportedOperationException(
            "Kotlin metadata definition format version is not supported: " +
                    "expected ${BuiltInsBinaryVersion.INSTANCE}, actual $version. " +
                    "Please update Kotlin"
        )
    }

    val message = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
    val nameResolver = NameResolverImpl(message.strings, message.qualifiedNames)
    return Triple(message, nameResolver, version)
}
