/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.file

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.analysis.decompiler.stub.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.ClassDeserializer
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getClassId

open class KotlinMetadataStubBuilder(
    private val version: Int,
    private val fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    private val readFile: (VirtualFile, ByteArray) -> FileWithMetadata?
) : ClsStubBuilder() {
    override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + version

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val virtualFile = content.file
        assert(virtualFile.extension == fileType.defaultExtension || virtualFile.fileType == fileType) { "Unexpected file type ${virtualFile.fileType.name}" }
        val file = readFile(virtualFile, content.content) ?: return null

        when (file) {
            is FileWithMetadata.Incompatible -> {
                return createIncompatibleAbiVersionFileStub()
            }
            is FileWithMetadata.Compatible -> {
                val packageProto = file.proto.`package`
                val packageFqName = file.packageFqName
                val nameResolver = file.nameResolver
                val protocol = serializerProtocol()
                val components = ClsStubBuilderComponents(
                    ProtoBasedClassDataFinder(file.proto, nameResolver, file.version),
                    AnnotationLoaderForStubBuilderImpl(protocol),
                    virtualFile,
                    protocol
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
                        fileStub, classProto, nameResolver, nameResolver.getClassId(classProto.fqName), source = null, context = context
                    )
                }
                return fileStub
            }
        }
    }

    protected open fun createCallableSource(file: FileWithMetadata.Compatible, filename: String): SourceElement? = null

    sealed class FileWithMetadata {
        class Incompatible(val version: BinaryVersion) : FileWithMetadata()

        open class Compatible(
            val proto: ProtoBuf.PackageFragment,
            val version: BinaryVersion,
            serializerProtocol: SerializerExtensionProtocol
        ) : FileWithMetadata() {
            val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)
            val packageFqName = FqName(nameResolver.getPackageFqName(proto.`package`.getExtension(serializerProtocol.packageFqName)))

            open val classesToDecompile: List<ProtoBuf.Class> =
                proto.class_List.filter { proto ->
                    val classId = nameResolver.getClassId(proto.fqName)
                    !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
                }
        }
    }
}

