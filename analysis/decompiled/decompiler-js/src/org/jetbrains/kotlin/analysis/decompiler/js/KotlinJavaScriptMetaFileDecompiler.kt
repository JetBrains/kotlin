/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.js

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.js.JsProtoBuf
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.utils.JsMetadataVersion
import java.io.ByteArrayInputStream

class KotlinJavaScriptMetaFileDecompiler : KotlinMetadataDecompiler() {
    override fun getStubBuilder(): KotlinMetadataStubBuilder = KotlinJavaScriptMetadataStubBuilder
    override fun createFile(viewProvider: KotlinDecompiledFileViewProvider): KtDecompiledFile = KjsmDecompiledFile(viewProvider)
}

private object KotlinJavaScriptMetadataStubBuilder : KotlinMetadataStubBuilder() {
    override fun getStubVersion(): Int = KotlinStubVersions.JS_STUB_VERSION
    override val fileType: FileType get() = KotlinJavaScriptMetaFileType
    override val serializerProtocol: SerializerExtensionProtocol get() = JsSerializerProtocol
    override val expectedBinaryVersion: BinaryVersion get() = JsMetadataVersion.INSTANCE

    override fun readFile(virtualFile: VirtualFile, content: ByteArray): FileWithMetadata {
        val stream = ByteArrayInputStream(content)

        val version = JsMetadataVersion.readFrom(stream)
        if (!version.isCompatibleWithCurrentCompilerVersion()) {
            return FileWithMetadata.Incompatible(version)
        }

        JsProtoBuf.Header.parseDelimitedFrom(stream)

        val proto = ProtoBuf.PackageFragment.parseFrom(stream, JsSerializerProtocol.extensionRegistry)
        return FileWithMetadata.Compatible(proto, version, JsSerializerProtocol)
    }
}
