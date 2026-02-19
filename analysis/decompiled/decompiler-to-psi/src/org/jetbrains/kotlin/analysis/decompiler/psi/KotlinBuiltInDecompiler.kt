/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import java.io.ByteArrayInputStream

class KotlinBuiltInDecompiler : KotlinMetadataDecompiler() {
    override fun getStubBuilder(): KotlinMetadataStubBuilder = KotlinBuiltInMetadataStubBuilder
    override fun createFile(viewProvider: KotlinDecompiledFileViewProvider): KtDecompiledFile = KotlinBuiltinsDecompiledFile(viewProvider)
}

class BuiltInDefinitionFile(
    proto: ProtoBuf.PackageFragment,
    version: BuiltInsBinaryVersion,
    /**
     * Directory where the VirtualFile is situated. Can be null in the case when the builtin file is created in the air.
     */
    val packageDirectory: VirtualFile?,
    val isMetadata: Boolean,
    private val filterOutClassesExistingAsClassFiles: Boolean = true,
) : KotlinMetadataStubBuilder.FileWithMetadata.Compatible(proto, version, BuiltInSerializerProtocol) {
    override val classesToDecompile: List<ProtoBuf.Class>
        get() = super.classesToDecompile.let { classes ->
            if (packageDirectory == null) {
                // If a builtin file is created in the air,
                // that means we need all built-in files because there are no .class files to replace them with,
                // see KT-61757
                return@let classes
            }
            if (isMetadata || !FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES || !filterOutClassesExistingAsClassFiles) classes
            else classes.filter { classProto ->
                shouldDecompileBuiltInClass(nameResolver.getClassId(classProto.fqName), packageDirectory)
            }
        }

    private fun shouldDecompileBuiltInClass(classId: ClassId, packageDirectory: VirtualFile): Boolean {
        val realJvmClassFileName = classId.shortClassName.asString() + "." + JavaClassFileType.INSTANCE.defaultExtension
        return packageDirectory.findChild(realJvmClassFileName) == null
    }

    companion object {
        var FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES = true
            @TestOnly set

        @JvmOverloads
        fun read(
            contents: ByteArray, file: VirtualFile,
            filterOutClassesExistingAsClassFiles: Boolean = true
        ): KotlinMetadataStubBuilder.FileWithMetadata? {
            val stream = ByteArrayInputStream(contents)

            val version = BuiltInsBinaryVersion.readFrom(stream)
            if (!version.isCompatibleWithCurrentCompilerVersion()) {
                return Incompatible(version)
            }

            val proto = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
            val result = BuiltInDefinitionFile(
                proto, version, file.parent,
                file.extension == METADATA_FILE_EXTENSION,
                filterOutClassesExistingAsClassFiles
            )

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
