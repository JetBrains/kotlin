// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.ClassDeserializer
import org.jetbrains.kotlin.serialization.deserialization.getClassId

sealed class FileWithMetadata {
    class Incompatible(val version: BinaryVersion) : FileWithMetadata()

    open class Compatible(val proto: ProtoBuf.PackageFragment, val version: BinaryVersion) : FileWithMetadata() {
        val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)
        val packageFqName = FqName(proto.getExtension(KlibMetadataProtoBuf.fqName))

        open val classesToDecompile: List<ProtoBuf.Class> =
            proto.class_List.filter { proto ->
                val classId = nameResolver.getClassId(proto.fqName)
                !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
            }
    }

    companion object {
        fun forPackageFragment(packageFragment: VirtualFile): FileWithMetadata? {
            val klibMetadataLoadingCache = KlibLoadingMetadataCache.getInstance()
            val (fragment, version) = klibMetadataLoadingCache.getCachedPackageFragmentWithVersion(packageFragment)
            if (fragment == null || version == null) return null
            return Compatible(fragment, version) //todo: check version compatibility
        }
    }
}
