/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.BasicIrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.FileDeserializationState
import org.jetbrains.kotlin.backend.common.serialization.IrSymbolDeserializer
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary

@OptIn(ObsoleteDescriptorBasedAPI::class)
class KonanPartialModuleDeserializer(
    kotlinIrLinker: KotlinIrLinker,
    moduleDescriptor: ModuleDescriptor,
    override val klib: KotlinLibrary,
    strategyResolver: (String) -> DeserializationStrategy,
    private val cacheDeserializationStrategy: CacheDeserializationStrategy,
) : BasicIrModuleDeserializer(
    linker = kotlinIrLinker,
    moduleDescriptor = moduleDescriptor,
    klib = klib,
    strategyResolver = { fileName -> if (cacheDeserializationStrategy.contains(fileName)) strategyResolver(fileName) else DeserializationStrategy.ON_DEMAND },
    libraryAbiVersion = klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT,
) {
    private val descriptorSignatures = mutableMapOf<DeclarationDescriptor, IdSignature>()

    fun getIdSignature(descriptor: DeclarationDescriptor): IdSignature? = descriptorSignatures[descriptor]

    val files by lazy { fileDeserializationStates.map { it.file } }

    fun getDeserializationStates(): List<FileDeserializationState> =
        fileDeserializationStates.toList()

    fun getFileDeserializationState(fileSignature: IdSignature) =
        moduleReversedFileIndex[fileSignature] ?: error("No file deserializer for ${fileSignature.render()}")

    private val idSignatureToFile by lazy {
        buildMap {
            fileDeserializationStates.forEach { fileDeserializationState ->
                fileDeserializationState.fileDeserializer.reversedSignatureIndex.keys.forEach { idSig ->
                    put(idSig, fileDeserializationState.file)
                }
            }
        }
    }

    private val fileReferenceToFileDeserializationState by lazy {
        fileDeserializationStates.associateBy { SerializedFileReference(it.file.packageFqName.asString(), it.file.path) }
    }

    val SerializedFileReference.deserializationState
        get() = fileReferenceToFileDeserializationState[this] ?: error("Unknown file $this")

    private tailrec fun IdSignature.fileSignature(): IdSignature.FileSignature? = when (this) {
        is IdSignature.FileSignature -> this
        is IdSignature.CompositeSignature -> this.container.fileSignature()
        else -> null
    }

    fun getFileNameOf(declaration: IrDeclaration): String {
        fun IrDeclaration.getSignature() = symbol.signature ?: descriptorSignatures[descriptor]

        val idSig = declaration.getSignature()
            ?: (declaration.parent as? IrDeclaration)?.getSignature()
            ?: error("Can't find signature of ${declaration.render()}")
        val topLevelIdSig = idSig.topLevelSignature()
        return topLevelIdSig.fileSignature()?.fileName
            ?: idSignatureToFile[topLevelIdSig]?.path
            ?: error("No file for $idSig")
    }

    fun getKlibFileIndexOf(irFile: IrFile) = fileDeserializationStates.first { it.file == irFile }.fileIndex

    private val descriptorByIdSignatureFinder = DescriptorByIdSignatureFinderImpl(
        moduleDescriptor, KonanManglerDesc,
        DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_ONLY
    )

    override fun contains(idSig: IdSignature): Boolean =
        super.contains(idSig) ||
                cacheDeserializationStrategy != CacheDeserializationStrategy.WholeModule
                && idSig.isPubliclyVisible && descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) != null
}