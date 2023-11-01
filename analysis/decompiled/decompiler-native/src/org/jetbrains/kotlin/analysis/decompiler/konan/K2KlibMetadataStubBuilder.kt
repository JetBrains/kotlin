/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.analysis.decompiler.stub.ClsStubBuilderComponents
import org.jetbrains.kotlin.analysis.decompiler.stub.createClassStub
import org.jetbrains.kotlin.analysis.decompiler.stub.createFileStub
import org.jetbrains.kotlin.analysis.decompiler.stub.createPackageDeclarationsStubs
import org.jetbrains.kotlin.analysis.decompiler.stub.file.AnnotationLoaderForStubBuilderImpl
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getClassId

internal class K2KlibMetadataStubBuilder(
    private val version: Int,
    fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    readFile: (VirtualFile) -> FileWithMetadata?,
) : KlibMetadataStubBuilder(version, fileType, readFile) {
    override fun getStubVersion(): Int = version

    override fun buildMetadataFileStub(fileWithMetadata: FileWithMetadata.Compatible, fileContent: FileContent): PsiFileStub<*> {
        val packageProto = fileWithMetadata.proto.`package`
        val packageFqName = fileWithMetadata.packageFqName
        val nameResolver = fileWithMetadata.nameResolver
        val protocol = serializerProtocol()
        val components = ClsStubBuilderComponents(
            ProtoBasedClassDataFinder(fileWithMetadata.proto, nameResolver, fileWithMetadata.version),
            AnnotationLoaderForStubBuilderImpl(protocol),
            fileContent.file,
            protocol
        )
        val context = components.createContext(nameResolver, packageFqName, TypeTable(packageProto.typeTable))

        val fileStub = createFileStub(packageFqName, isScript = false)
        createPackageDeclarationsStubs(
            fileStub, context,
            ProtoContainer.Package(packageFqName, context.nameResolver, context.typeTable, source = null),
            packageProto
        )
        for (classProto in fileWithMetadata.classesToDecompile) {
            createClassStub(
                fileStub, classProto, nameResolver, nameResolver.getClassId(classProto.fqName), source = null, context = context
            )
        }
        return fileStub
    }
}
