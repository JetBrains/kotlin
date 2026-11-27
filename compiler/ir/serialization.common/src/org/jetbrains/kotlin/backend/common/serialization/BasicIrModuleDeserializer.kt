/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrDeserializationSettings.DeserializeFunctionBodies
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.KlibIrComponent
import org.jetbrains.kotlin.library.components.irOrFail
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.utils.putToMultiMap
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

/**
 * @property allowErrorNodes Whether error nodes are allowed during IR deserialization and initialization.
 *   Caution: This setting is not safe to use, as it can lead to crashes in the frontend or backend.
 *   The only legal case for using this setting is the `dump-ir` command of the `klib` command-line tool.
 * @property deserializeTypeAliases Whether to deserialize [org.jetbrains.kotlin.ir.declarations.IrTypeAlias] declarations.
 *   Type aliases don't need to be (de)serialized anymore. See KT-86632.
 *   The only case for enabling it is the klib tool and its ability to dump (even older) KLIBs.
 */
@OptIn(IrLinkerInternalApi::class)
abstract class BasicIrModuleDeserializer(
    val linker: KotlinIrLinker,
    moduleFragment: IrModuleFragment,
    override val klib: KotlinLibrary,
    override val strategyResolver: DeserializationStrategyResolver,
    libraryAbiVersion: KotlinAbiVersion,
    private val allowErrorNodes: Boolean = false,
    private val deserializeTypeAliases: Boolean = false,
) : IrModuleDeserializer(moduleFragment, libraryAbiVersion) {

    internal val deserializationQueue = IrModuleWithFilesDeserializationQueue(parent = linker.deserializationQueue)

    /**
     * The cache of all [IrFileDeserializer]s by [IrFile].
     */
    final override val fileDeserializers: Map<IrFile, IrFileDeserializer>
        field = mutableMapOf<IrFile, IrFileDeserializer>()

    /**
     * The cache of all [IrFileDeserializer]s by their package [FqName]. The list of deserializers retrieved for a certain
     * package [FqName] follows the order of [FileDeserializationState]s in [fileDeserializationStates].
     */
    private val fileDeserializersByPackage: MutableMap<FqName, MutableList<IrFileDeserializer>> = hashMapOf()

    final override val contributedPackageNames: Set<FqName>
        get() = fileDeserializersByPackage.keys

    protected val fileDeserializationStates: List<FileDeserializationState>

    protected val moduleReversedFileIndex: Map<IdSignature, FileDeserializationState>
        field = hashMapOf()

    // TODO(KT-91417): This is a temporary workaround. The deserialization queue needs to be reworked.
    internal fun getFileDeserializationState(signature: IdSignature): FileDeserializationState? = moduleReversedFileIndex[signature]

    protected val ir: KlibIrComponent get() = klib.irOrFail

    init {
        val fileCount = ir.irFileCount
        fileDeserializationStates = buildList {
            for (i in 0 until fileCount) {
                val fileStream = ir.irFile(i).codedInputStream
                val fileProto = ProtoFile.parseFrom(fileStream, ExtensionRegistryLite.getEmptyRegistry())
                val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(ir, i))
                val file = fileReader.createFile(moduleFragment, fileProto, linker.fileEntryDeserializer)

                this += deserializeIrFile(
                    fileProto,
                    file,
                    fileReader,
                    i,
                    this@BasicIrModuleDeserializer,
                    allowErrorNodes,
                    deserializeTypeAliases
                )

                if (!strategyResolver(file.fileEntry.name).onDemand)
                    moduleFragment.files.add(file)
            }
        }
    }

    override fun collectAllMatchingSignatures(
        topLevelCallableId: TopLevelCallableIdAndKind,
        output: MutableCollection<IdSignature>,
    ) {
        fileDeserializersByPackage[topLevelCallableId.packageFqName]?.forEach { fileDeserializer ->
            fileDeserializer.collectAllMatchingSignatures(topLevelCallableId, output)
        }
    }

    override fun finalizeInitialization() {
        for (fileState in fileDeserializationStates) {
            fileState.enqueueDeclarationsToFinalizeInitialization()
        }
    }

    override fun findAndEnqueueSymbolOrNull(signatureAndKind: SignatureAndKind): IrSymbol? {
        val topLevel = signatureAndKind.topLevelOrNull() ?: return null

        val fileLocalDeserializationState = moduleReversedFileIndex[topLevel.signature] ?: return null
        fileLocalDeserializationState.deserializationQueue.enqueue(topLevel)

        return fileLocalDeserializationState.fileDeserializer.symbolDeserializer.deserializeSymbolWithOwnerInCurrentFile(signatureAndKind.signature, signatureAndKind.kind)
    }

    // TODO: fix to topLevel checker
    override fun contains(idSig: IdSignature): Boolean = idSig.topLevelSignature() in moduleReversedFileIndex

    private fun deserializeIrFile(
        fileProto: ProtoFile, file: IrFile, fileReader: IrLibraryFileFromBytes,
        fileIndex: Int, moduleDeserializer: BasicIrModuleDeserializer, allowErrorNodes: Boolean, deserializeTypeAliases: Boolean,
    ): FileDeserializationState {
        val fileStrategy = strategyResolver(file.fileEntry.name)

        val fileDeserializationState = FileDeserializationState(
            linker,
            fileIndex,
            file,
            fileReader,
            fileProto,
            IrDeserializationSettings(
                allowErrorNodes = allowErrorNodes,
                deserializeFunctionBodies = when {
                    fileStrategy.needBodies -> DeserializeFunctionBodies.ALL
                    fileStrategy.needOnlyInlineBodies -> DeserializeFunctionBodies.ONLY_INLINE
                    else -> DeserializeFunctionBodies.NONE
                },
                fixSwappedKProperty2TypeParameterOrder = moduleDeserializer.compatibilityMode.swappedKProperty2TypeParameterOrder,
                deserializeTypeAliases = deserializeTypeAliases,
                eagerlyDeserializeExplicitlyExportedDeclarations = fileStrategy.explicitlyExported,
                deserializeAllTopLevelDeclarations = fileStrategy.theWholeWorld,
            ),
            moduleDeserializer,
        )

        fileDeserializers[file] = fileDeserializationState.fileDeserializer
        fileDeserializersByPackage.putToMultiMap(file.packageFqName, fileDeserializationState.fileDeserializer)

        for ([signature, _] in fileDeserializationState.fileDeserializer.topLevelDeclarationIndex.keys) {
            moduleReversedFileIndex[signature] = fileDeserializationState
        }

        return fileDeserializationState
    }
}

val ByteArray.codedInputStream: CodedInputStream
    get() {
        val codedInputStream = CodedInputStream.newInstance(this)
        codedInputStream.setRecursionLimit(65535) // The default 64 is blatantly not enough for IR.
        return codedInputStream
    }
