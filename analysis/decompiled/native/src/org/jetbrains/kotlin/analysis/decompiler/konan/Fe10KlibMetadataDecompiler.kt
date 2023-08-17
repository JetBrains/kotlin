/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.analysis.decompiler.konan

import com.intellij.openapi.fileTypes.FileType
import org.jetbrains.kotlin.analysis.decompiler.psi.text.DecompiledText
import org.jetbrains.kotlin.analysis.decompiler.psi.text.buildDecompiledText
import org.jetbrains.kotlin.analysis.decompiler.psi.text.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.FlexibleTypeDeserializer
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.utils.addIfNotNull

abstract class Fe10KlibMetadataDecompiler<out V : BinaryVersion>(
    fileType: FileType,
    serializerProtocol: () -> SerializerExtensionProtocol,
    flexibleTypeDeserializer: FlexibleTypeDeserializer,
    expectedBinaryVersion: () -> V,
    invalidBinaryVersion: () -> V,
    stubVersion: Int
) : KlibMetadataDecompiler<V>(
    fileType,
    serializerProtocol,
    flexibleTypeDeserializer,
    expectedBinaryVersion,
    invalidBinaryVersion
) {
    private val renderer: DescriptorRenderer by lazy {
        DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }
    }

    override val metadataStubBuilder: KlibMetadataStubBuilder by lazy {
        Fe10KlibMetadataStubBuilder(stubVersion, fileType, serializerProtocol, ::readFileSafely)
    }

    override fun getDecompiledText(
        file: FileWithMetadata.Compatible,
        serializerProtocol: SerializerExtensionProtocol,
        flexibleTypeDeserializer: FlexibleTypeDeserializer
    ): DecompiledText {
        return decompiledText(file, serializerProtocol, flexibleTypeDeserializer, renderer)
    }
}

/**
 * This function is extracted for [Fe10KlibMetadataDecompiler], [Fe10KlibMetadataStubBuilder] and [K2KlibMetadataDecompiler].
 * TODO: K2 shouldn't use descriptor renderer for building decompiled text.
 * Note that decompiled text is not used for building stubs in K2.
 * That's why in K2 it is important to preserve declaration order during deserialization to not get PSI vs. stubs mismatch.
 */
internal fun decompiledText(
    file: FileWithMetadata.Compatible,
    serializerProtocol: SerializerExtensionProtocol,
    flexibleTypeDeserializer: FlexibleTypeDeserializer,
    renderer: DescriptorRenderer,
    deserializationConfiguration: DeserializationConfiguration = DeserializationConfiguration.Default
): DecompiledText {
    val packageFqName = file.packageFqName
    val resolver = KlibMetadataDeserializerForDecompiler(
        packageFqName, file.proto, file.nameResolver,
        serializerProtocol, flexibleTypeDeserializer,
        deserializationConfiguration,
    )
    val declarations = arrayListOf<DeclarationDescriptor>()
    declarations.addAll(resolver.resolveDeclarationsInFacade(packageFqName))
    for (classProto in file.classesToDecompile) {
        val classId = file.nameResolver.getClassId(classProto.fqName)
        declarations.addIfNotNull(resolver.resolveTopLevelClass(classId))
    }
    return buildDecompiledText(packageFqName, declarations, renderer)
}