// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import java.io.ByteArrayInputStream

class KotlinBuiltInDecompiler : KotlinMetadataDecompiler<BuiltInsBinaryVersion>(
    KotlinBuiltInFileType, { BuiltInSerializerProtocol },
    FlexibleTypeDeserializer.ThrowException, { BuiltInsBinaryVersion.INSTANCE }, { BuiltInsBinaryVersion.INVALID_VERSION },
    KotlinStubVersions.BUILTIN_STUB_VERSION
) {
    override val metadataStubBuilder: KotlinMetadataStubBuilder =
        KotlinBuiltInMetadataStubBuilder(::readFileSafely)

    override fun readFile(bytes: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata? {
        return BuiltInDefinitionFile.read(bytes, file)
    }
}

private class KotlinBuiltInMetadataStubBuilder(
    readFile: (VirtualFile, ByteArray) -> FileWithMetadata?,
) : KotlinMetadataStubBuilder(KotlinStubVersions.BUILTIN_STUB_VERSION, KotlinBuiltInFileType, { BuiltInSerializerProtocol }, readFile) {
    override fun createCallableSource(file: FileWithMetadata.Compatible, filename: String): SourceElement? {
        val fileNameForFacade = when (val withoutExtension = filename.removeSuffix(BuiltInSerializerProtocol.DOT_DEFAULT_EXTENSION)) {
            // this is the filename used in stdlib, others should match
            "kotlin" -> "library"
            else -> withoutExtension
        }

        val facadeFqName = PackagePartClassUtils.getPackagePartFqName(file.packageFqName, fileNameForFacade)
        return JvmPackagePartSource(JvmClassName.byClassId(ClassId.topLevel(facadeFqName)), null, file.proto.`package`, file.nameResolver)
    }
}

class BuiltInDefinitionFile(
    proto: ProtoBuf.PackageFragment,
    version: BuiltInsBinaryVersion,
    val packageDirectory: VirtualFile,
    val isMetadata: Boolean,
) : KotlinMetadataStubBuilder.FileWithMetadata.Compatible(proto, version, BuiltInSerializerProtocol) {
    override val classesToDecompile: List<ProtoBuf.Class>
        get() = super.classesToDecompile.let { classes ->
            if (isMetadata || !FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES) classes
            else classes.filter { classProto ->
                shouldDecompileBuiltInClass(nameResolver.getClassId(classProto.fqName))
            }
        }

    private fun shouldDecompileBuiltInClass(classId: ClassId): Boolean {
        val realJvmClassFileName = classId.shortClassName.asString() + "." + JavaClassFileType.INSTANCE.defaultExtension
        return packageDirectory.findChild(realJvmClassFileName) == null
    }

    companion object {
        var FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES = true
            @TestOnly set

        fun read(contents: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata? {
            val stream = ByteArrayInputStream(contents)

            val version = BuiltInsBinaryVersion.readFrom(stream)
            if (!version.isCompatibleWithCurrentCompilerVersion()) {
                return Incompatible(version)
            }

            val proto = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
            val result =
                BuiltInDefinitionFile(proto, version, file.parent, file.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION)
            val packageProto = result.proto.`package`
            if (result.classesToDecompile.isEmpty() &&
                packageProto.typeAliasCount == 0 && packageProto.functionCount == 0 && packageProto.propertyCount == 0
            ) {
                // No declarations to decompile: should skip this file
                return null
            }

            return result
        }
    }
}
