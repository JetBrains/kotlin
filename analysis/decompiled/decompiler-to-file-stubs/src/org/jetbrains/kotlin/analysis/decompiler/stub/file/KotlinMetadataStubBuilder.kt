/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.file

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.analysis.decompiler.psi.text.createIncompatibleMetadataVersionDecompiledText
import org.jetbrains.kotlin.analysis.decompiler.stub.*
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ClassDeserializer
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withVirtualFileEntry
import java.io.IOException

abstract class KotlinMetadataStubBuilder : ClsStubBuilder() {
    protected abstract val supportedFileType: FileType
    protected abstract val expectedBinaryVersion: BinaryVersion
    protected abstract fun readFile(virtualFile: VirtualFile, content: ByteArray?): FileWithMetadata?

    /**
     * Whether [readFile] is expected to have a not null result
     */
    protected open fun hasMetadata(virtualFile: VirtualFile): Boolean = readFile(virtualFile, null) != null

    /**
     * Whether the [file] is supported, so it might have a stub
     */
    fun isSupported(file: VirtualFile): Boolean {
        val supportedType = supportedFileType
        return file.extension == supportedType.defaultExtension || file.fileType == supportedType
    }

    /**
     * Whether the [file] would have a stub as the result of [buildFileStub]
     */
    fun hasStub(file: VirtualFile): Boolean = isSupported(file) && file.readSafely { hasMetadata(file) } == true

    fun readFileSafely(file: VirtualFile, content: ByteArray? = null): FileWithMetadata? = file.readSafely {
        readFile(file, content)
    }

    final override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val virtualFile = content.file
        requireWithAttachment(isSupported(virtualFile), { "Unexpected file type" }) {
            withVirtualFileEntry("file", virtualFile)
        }

        val file = readFileSafely(virtualFile, content.content) ?: return null

        return when (file) {
            is FileWithMetadata.Incompatible -> createIncompatibleAbiVersionFileStub(
                createIncompatibleMetadataVersionDecompiledText(
                    expectedVersion = expectedBinaryVersion,
                    actualVersion = file.version,
                )
            )

            is FileWithMetadata.Compatible -> {
                val packageProto = file.proto.`package`
                val packageFqName = file.packageFqName
                val nameResolver = file.nameResolver
                val protocol = file.serializerProtocol
                val components = ClsStubBuilderComponents(
                    classDataFinder = classDataFinder(
                        original = ProtoBasedClassDataFinder(file.proto, nameResolver, file.version),
                        file = virtualFile,
                    ),
                    annotationLoader = AnnotationLoaderForStubBuilderImpl(protocol),
                    virtualFileForDebug = virtualFile,
                    serializationProtocol = protocol,
                )

                val context = components.createContext(nameResolver, packageFqName, TypeTable(packageProto.typeTable))

                val fileStub = createFileStub(packageFqName, isScript = false)
                createPackageDeclarationsStubs(
                    fileStub, context,
                    ProtoContainer.Package(
                        packageFqName,
                        context.nameResolver,
                        context.typeTable,
                        source = createCallableSource(file, content.fileName)
                    ),
                    packageProto
                )

                for (classProto in file.classesToDecompile) {
                    createClassStub(
                        parent = fileStub,
                        classProto = classProto,
                        nameResolver = nameResolver,
                        classId = nameResolver.getClassId(classProto.fqName),
                        source = null,
                        context = context,
                    )
                }

                fileStub
            }
        }
    }

    protected open fun createCallableSource(file: FileWithMetadata.Compatible, filename: String): SourceElement? = null
    protected open fun classDataFinder(original: ClassDataFinder, file: VirtualFile): ClassDataFinder = original

    sealed class FileWithMetadata {
        class Incompatible(val version: BinaryVersion) : FileWithMetadata()

        open class Compatible(
            val proto: ProtoBuf.PackageFragment,
            val version: BinaryVersion,
            val serializerProtocol: SerializerExtensionProtocol
        ) : FileWithMetadata() {
            val nameResolver: NameResolverImpl = NameResolverImpl(proto.strings, proto.qualifiedNames)

            open val packageFqName: FqName
                get() = FqName(nameResolver.getPackageFqName(proto.`package`.getExtension(serializerProtocol.packageFqName)))

            open val classesToDecompile: List<ProtoBuf.Class>
                get() = proto.class_List.filter { proto ->
                    val classId = nameResolver.getClassId(proto.fqName)
                    !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
                }
        }
    }
}

private inline fun <T> VirtualFile.readSafely(action: () -> T): T? = try {
    if (isValid) {
        action()
    } else {
        null
    }
} catch (_: IOException) {
    // This is needed because sometimes we're given VirtualFile instances that point to non-existent .jar entries.
    // Such files are valid (isValid() returns true), but an attempt to read their contents results in a FileNotFoundException.
    // Note that although calling "refresh()" instead of catching an exception would seem more correct here,
    // it's not always allowed and also is likely to degrade performance
    null
}
