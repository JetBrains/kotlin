// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
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
    override fun readFile(bytes: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata? {
        return BuiltInDefinitionFile.read(bytes, file)
    }
}

class BuiltInDefinitionFile(
    proto: ProtoBuf.PackageFragment,
    version: BuiltInsBinaryVersion,
    val packageDirectory: VirtualFile,
    val isMetadata: Boolean
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

        private val ALLOWED_METADATA_EXTENSIONS = listOf(
            JvmBuiltInsPackageFragmentProvider.DOT_BUILTINS_METADATA_FILE_EXTENSION,
            MetadataPackageFragment.DOT_METADATA_FILE_EXTENSION
        )

        fun read(contents: ByteArray, file: VirtualFile): KotlinMetadataStubBuilder.FileWithMetadata? {
            val fileName = file.name
            if (ALLOWED_METADATA_EXTENSIONS.none { fileName.endsWith(it) }) {
                return null
            }

            val stream = ByteArrayInputStream(contents)

            val version = BuiltInsBinaryVersion.readFrom(stream)
            if (!version.isCompatibleWithCurrentCompilerVersion()) {
                return Incompatible(version)
            }

            val proto = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
            val isMetadata = file.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION
            val result = BuiltInDefinitionFile(proto, version, file.parent, isMetadata)
            val packageProto = result.proto.`package`

            val isEmpty = result.classesToDecompile.isEmpty()
                    && packageProto.typeAliasCount == 0
                    && packageProto.functionCount == 0
                    && packageProto.propertyCount == 0

            if (isEmpty) {
                // Skip the file is there are no declarations to decompile
                return null
            }

            // '.kotlin_builtins' files sometimes appear shaded in libraries.
            // Here is an additional check that the file is likely to be an actual part of built-ins.
            val nestingLevel = result.packageFqName.pathSegments().size
            val rootPackageDirectory = generateSequence(file) { it.parent }.drop(nestingLevel + 1).firstOrNull() ?: return null
            val metaInfDirectory = rootPackageDirectory.findChild("META-INF") ?: return null

            if (metaInfDirectory.children.none { it.extension == "kotlin_module" }) {
                // Here can be a more strict check.
                // For instance, we can check if the manifest file has a 'Kotlin-Runtime-Component' attribute.
                // It's unclear if it would break use-cases when the standard library is embedded, though.
                return null
            }

            return result
        }
    }
}
