/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.analysis.decompiler.psi.text.createIncompatibleMetadataVersionDecompiledText
import org.jetbrains.kotlin.analysis.decompiler.stub.*
import org.jetbrains.kotlin.analysis.decompiler.stub.file.AnnotationLoaderForStubBuilderImpl
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer.Package
import org.jetbrains.kotlin.serialization.deserialization.getClassId

class KlibMetadataStubBuilder(
    private val version: Int,
    private val fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    private val readFile: (VirtualFile) -> FileWithMetadata?,
) : ClsStubBuilder() {
    override fun getStubVersion(): Int = version

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val virtualFile = content.file
        assert(FileTypeRegistry.getInstance().isFileOfType(virtualFile, fileType)) { "Unexpected file type ${virtualFile.fileType}" }

        val fileWithMetadata = readFile(virtualFile) ?: return null

        return when (fileWithMetadata) {
            is FileWithMetadata.Incompatible -> createIncompatibleAbiVersionFileStub(
                createIncompatibleMetadataVersionDecompiledText(fileWithMetadata.version)
            )

            is FileWithMetadata.Compatible -> {
                val packageProto = fileWithMetadata.proto.`package`
                val packageFqName = fileWithMetadata.packageFqName
                val nameResolver = fileWithMetadata.nameResolver
                val mainClassDataFinder = ProtoBasedClassDataFinder(fileWithMetadata.proto, nameResolver, fileWithMetadata.version)
                val protocol = serializerProtocol()
                val components = ClsStubBuilderComponents(
                    classDataFinder = NearFileClassDataFinder.wrapIfNeeded(mainClassDataFinder, content.file, readFile),
                    annotationLoader = AnnotationLoaderForStubBuilderImpl(protocol),
                    virtualFileForDebug = content.file,
                    serializationProtocol = protocol,
                )

                val context = components.createContext(nameResolver, packageFqName, TypeTable(packageProto.typeTable))
                val fileStub = createFileStub(packageFqName, isScript = false)
                createPackageDeclarationsStubs(
                    parentStub = fileStub, outerContext = context,
                    protoContainer = Package(packageFqName, context.nameResolver, context.typeTable, source = null),
                    packageProto = packageProto,
                )

                for (classProto in fileWithMetadata.classesToDecompile) {
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
}
