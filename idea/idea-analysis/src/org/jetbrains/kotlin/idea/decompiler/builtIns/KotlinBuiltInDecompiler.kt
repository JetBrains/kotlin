/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.decompiler.builtIns

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.idea.decompiler.common.FileWithMetadata
import org.jetbrains.kotlin.idea.decompiler.common.KotlinMetadataDecompiler
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment
import java.io.ByteArrayInputStream

class KotlinBuiltInDecompiler : KotlinMetadataDecompiler<BuiltInsBinaryVersion>(
        KotlinBuiltInFileType, TargetPlatform.Common, BuiltInSerializerProtocol,
        FlexibleTypeDeserializer.ThrowException, BuiltInsBinaryVersion.INSTANCE, BuiltInsBinaryVersion.INVALID_VERSION,
        KotlinStubVersions.BUILTIN_STUB_VERSION
) {
    override fun readFile(bytes: ByteArray, file: VirtualFile): FileWithMetadata? {
        return BuiltInDefinitionFile.read(bytes, file)
    }
}

class BuiltInDefinitionFile(
        proto: ProtoBuf.PackageFragment,
        val packageDirectory: VirtualFile,
        val isMetadata: Boolean
) : FileWithMetadata.Compatible(proto, BuiltInSerializerProtocol) {
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

        fun read(contents: ByteArray, file: VirtualFile): FileWithMetadata? {
            val stream = ByteArrayInputStream(contents)

            val version = BuiltInsBinaryVersion.readFrom(stream)
            if (!version.isCompatible()) {
                return FileWithMetadata.Incompatible(version)
            }

            val proto = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
            val result = BuiltInDefinitionFile(proto, file.parent, file.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION)
            val packageProto = result.proto.`package`
            if (result.classesToDecompile.isEmpty() &&
                packageProto.typeAliasCount == 0 && packageProto.functionCount == 0 && packageProto.propertyCount == 0) {
                // No declarations to decompile: should skip this file
                return null
            }

            return result
        }
    }
}
