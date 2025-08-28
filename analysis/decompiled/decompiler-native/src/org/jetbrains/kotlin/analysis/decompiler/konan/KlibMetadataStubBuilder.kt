/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.analysis.decompiler.psi.text.createIncompatibleMetadataVersionDecompiledText
import org.jetbrains.kotlin.analysis.decompiler.stub.*
import org.jetbrains.kotlin.analysis.decompiler.stub.file.AnnotationLoaderForStubBuilderImpl
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer.Package
import org.jetbrains.kotlin.serialization.deserialization.getClassId

internal object KlibMetadataStubBuilder : KotlinMetadataStubBuilder() {
    override fun getStubVersion(): Int = KotlinStubVersions.KLIB_STUB_VERSION
    override val supportedFileType: FileType get() = KlibMetaFileType
    override val expectedBinaryVersion: BinaryVersion get() = MetadataVersion.INSTANCE

    override fun readFile(
        virtualFile: VirtualFile,
        content: ByteArray,
    ): FileWithMetadata? {
        val klibMetadataLoadingCache = KlibLoadingMetadataCache.getInstance()
        val (fragment, version) = klibMetadataLoadingCache.getCachedPackageFragmentWithVersion(virtualFile)
        if (fragment == null || version == null) return null
        if (!version.isCompatibleWithCurrentCompilerVersion()) {
            return FileWithMetadata.Incompatible(version)
        }

        return KlibFileWithMetadata(fragment, version)
    }

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val virtualFile = content.file
        assert(FileTypeRegistry.getInstance().isFileOfType(virtualFile, fileType)) { "Unexpected file type ${virtualFile.fileType}" }

        val fileWithMetadata = readFileSafely(virtualFile) ?: return null

        return when (fileWithMetadata) {
            is FileWithMetadata.Incompatible -> createIncompatibleAbiVersionFileStub(
                createIncompatibleMetadataVersionDecompiledText(fileWithMetadata.version)
            )

            is FileWithMetadata.Compatible -> {
                val packageProto = fileWithMetadata.proto.`package`
                val packageFqName = fileWithMetadata.packageFqName
                val nameResolver = fileWithMetadata.nameResolver
                val mainClassDataFinder = ProtoBasedClassDataFinder(fileWithMetadata.proto, nameResolver, fileWithMetadata.version)
                val protocol = fileWithMetadata.serializerProtocol
                val components = ClsStubBuilderComponents(
                    classDataFinder = NearFileClassDataFinder.wrapIfNeeded(mainClassDataFinder, content.file) { readFileSafely(it) },
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

    class KlibFileWithMetadata(
        proto: ProtoBuf.PackageFragment,
        version: BinaryVersion,
    ) : FileWithMetadata.Compatible(
        proto = proto,
        version = version,
        serializerProtocol = KlibMetadataSerializerProtocol,
    ) {
        override val packageFqName: FqName
            get() = FqName(proto.getExtension(KlibMetadataProtoBuf.fqName))
    }
}
