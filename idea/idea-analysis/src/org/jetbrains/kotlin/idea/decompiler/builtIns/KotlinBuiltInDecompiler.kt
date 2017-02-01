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
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.common.KotlinMetadataDeserializerForDecompiler
import org.jetbrains.kotlin.idea.decompiler.common.createIncompatibleAbiVersionDecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.ClassDeserializer
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragment
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.ByteArrayInputStream

class KotlinBuiltInDecompiler : ClassFileDecompilers.Full() {
    private val stubBuilder = KotlinBuiltInStubBuilder()

    override fun accepts(file: VirtualFile): Boolean {
        return file.fileType == KotlinBuiltInFileType
    }

    override fun getStubBuilder() = stubBuilder

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider {
        return KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
            if (BuiltInDefinitionFile.read(provider.virtualFile) == null) {
                null
            }
            else {
                KtDecompiledFile(provider) { file ->
                    buildDecompiledTextForBuiltIns(file)
                }
            }
        }
    }
}

private val decompilerRendererForBuiltIns = DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }

fun buildDecompiledTextForBuiltIns(builtInFile: VirtualFile): DecompiledText {
    if (builtInFile.fileType != KotlinBuiltInFileType) {
        error("Unexpected file type ${builtInFile.fileType}")
    }

    val file = BuiltInDefinitionFile.read(builtInFile)
               ?: error("Unexpectedly empty built-in file: $builtInFile")

    when (file) {
        is BuiltInDefinitionFile.Incompatible -> {
            return createIncompatibleAbiVersionDecompiledText(BuiltInsBinaryVersion.INSTANCE, file.version)
        }
        is BuiltInDefinitionFile.Compatible -> {
            val packageFqName = file.packageFqName
            val resolver = KotlinMetadataDeserializerForDecompiler(
                    packageFqName, file.proto, file.nameResolver,
                    TargetPlatform.Default, BuiltInSerializerProtocol, FlexibleTypeDeserializer.ThrowException
            )
            val declarations = arrayListOf<DeclarationDescriptor>()
            declarations.addAll(resolver.resolveDeclarationsInFacade(packageFqName))
            for (classProto in file.classesToDecompile) {
                val classId = file.nameResolver.getClassId(classProto.fqName)
                declarations.addIfNotNull(resolver.resolveTopLevelClass(classId))
            }
            return buildDecompiledText(packageFqName, declarations, decompilerRendererForBuiltIns)
        }
    }
}

sealed class BuiltInDefinitionFile {
    class Incompatible(val version: BuiltInsBinaryVersion) : BuiltInDefinitionFile()

    class Compatible(val proto: ProtoBuf.PackageFragment,
                     val packageDirectory: VirtualFile,
                     val isMetadata: Boolean) : BuiltInDefinitionFile() {
        val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)
        val packageFqName = nameResolver.getPackageFqName(proto.`package`.getExtension(BuiltInsProtoBuf.packageFqName))

        val classesToDecompile =
                proto.class_List.filter { proto ->
                    val classId = nameResolver.getClassId(proto.fqName)
                    !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
                }.let { classes ->
                    if (isMetadata || !FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES) classes
                    else classes.filter { classProto ->
                        shouldDecompileBuiltInClass(nameResolver.getClassId(classProto.fqName))
                    }
                }

        private fun shouldDecompileBuiltInClass(classId: ClassId): Boolean {
            val realJvmClassFileName = classId.shortClassName.asString() + "." + JavaClassFileType.INSTANCE.defaultExtension
            return packageDirectory.findChild(realJvmClassFileName) == null
        }
    }

    companion object {
        var FILTER_OUT_CLASSES_EXISTING_AS_JVM_CLASS_FILES = true
            @TestOnly set

        fun read(file: VirtualFile): BuiltInDefinitionFile? {
            return read(file.contentsToByteArray(), file)
        }

        fun read(contents: ByteArray, file: VirtualFile): BuiltInDefinitionFile? {
            val stream = ByteArrayInputStream(contents)

            val version = BuiltInsBinaryVersion.readFrom(stream)
            if (!version.isCompatible()) {
                return BuiltInDefinitionFile.Incompatible(version)
            }

            val proto = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
            val result = BuiltInDefinitionFile.Compatible(proto, file.parent, file.extension == MetadataPackageFragment.METADATA_FILE_EXTENSION)
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
