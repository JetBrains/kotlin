/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.psi.text.DecompiledText
import org.jetbrains.kotlin.analysis.decompiler.psi.text.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer

abstract class K2KlibMetadataDecompiler<out V : BinaryVersion>(
    private val fileType: FileType,
    private val serializerProtocol: () -> SerializerExtensionProtocol,
    flexibleTypeDeserializer: FlexibleTypeDeserializer,
    expectedBinaryVersion: () -> V,
    invalidBinaryVersion: () -> V,
    private val stubVersion: Int,
) : KlibMetadataDecompiler<V>(fileType, serializerProtocol, flexibleTypeDeserializer, expectedBinaryVersion, invalidBinaryVersion) {
    private val renderer: DescriptorRenderer by lazy {
        DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }
    }

    override val metadataStubBuilder: KlibMetadataStubBuilder by lazy {
        K2KlibMetadataStubBuilder(stubVersion, fileType, serializerProtocol, ::readFileSafely)
    }

    override fun doReadFile(file: VirtualFile): FileWithMetadata? {
        return FileWithMetadata.forPackageFragment(file)
    }

    override fun getDecompiledText(
        file: FileWithMetadata.Compatible,
        serializerProtocol: SerializerExtensionProtocol,
        flexibleTypeDeserializer: FlexibleTypeDeserializer
    ): DecompiledText {
        val deserializationConfiguration = object : DeserializationConfiguration {
            // K2 stubs are built from protobuf directly, declarations there are sorted
            // Breaking the order causes psi vs stub mismatch
            override val preserveDeclarationsOrdering: Boolean get() = true
        }
        return decompiledText(file, serializerProtocol, flexibleTypeDeserializer, renderer, deserializationConfiguration)
    }
}
