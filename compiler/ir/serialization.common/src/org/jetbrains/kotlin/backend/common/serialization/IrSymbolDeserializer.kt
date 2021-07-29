/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.proto.Actual
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature.IdSigCase.*
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature as ProtoAccessorIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.CommonIdSignature as ProtoCommonIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.CompositeSignature as ProtoCompositeSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature as ProtoFileLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileSignature as ProtoFileSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.LocalSignature as ProtoLocalSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.LoweredIdSignature as ProtoLoweredIdSignature

class IrSymbolDeserializer(
    val symbolTable: ReferenceSymbolTable,
    val fileReader: IrLibraryFile,
    val fileSymbol: IrFileSymbol,
    val actuals: List<Actual>,
    val enqueueLocalTopLevelDeclaration: (IdSignature) -> Unit,
    val handleExpectActualMapping: (IdSignature, IrSymbol) -> IrSymbol,
    private val enqueueAllDeclarations: Boolean = false,
    val deserializedSymbols: MutableMap<IdSignature, IrSymbol> = mutableMapOf(), // Per-file signature cache. TODO: do we really need it?
    val deserializePublicSymbol: (IdSignature, BinarySymbolData.SymbolKind) -> IrSymbol,
) {

    private val fileSignature: IdSignature.FileSignature = IdSignature.FileSignature(fileSymbol)

    fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        return deserializedSymbols.getOrPut(idSig) {
            val symbol = referenceDeserializedSymbol(symbolKind, idSig)

            handleExpectActualMapping(idSig, symbol)
        }
    }

    private fun referenceDeserializedSymbol(symbolKind: BinarySymbolData.SymbolKind, idSig: IdSignature): IrSymbol = symbolTable.run {
        when (symbolKind) {
            BinarySymbolData.SymbolKind.ANONYMOUS_INIT_SYMBOL -> IrAnonymousInitializerSymbolImpl()
            BinarySymbolData.SymbolKind.CLASS_SYMBOL -> referenceClassFromLinker(idSig)
            BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> referenceConstructorFromLinker(idSig)
            BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL -> referenceGlobalTypeParameterFromLinker(idSig)
            BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> referenceEnumEntryFromLinker(idSig)
            BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> referenceFieldFromLinker(idSig)
            BinarySymbolData.SymbolKind.FIELD_SYMBOL -> referenceFieldFromLinker(idSig)
            BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunctionFromLinker(idSig)
            BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> referenceTypeAliasFromLinker(idSig)
            BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referencePropertyFromLinker(idSig)
            BinarySymbolData.SymbolKind.VARIABLE_SYMBOL -> IrVariableSymbolImpl()
            BinarySymbolData.SymbolKind.VALUE_PARAMETER_SYMBOL -> IrValueParameterSymbolImpl()
            BinarySymbolData.SymbolKind.RECEIVER_PARAMETER_SYMBOL -> IrValueParameterSymbolImpl()
            BinarySymbolData.SymbolKind.LOCAL_DELEGATED_PROPERTY_SYMBOL ->
                IrLocalDelegatedPropertySymbolImpl()
            BinarySymbolData.SymbolKind.FILE_SYMBOL -> (idSig as IdSignature.FileSignature).fileSymbol
            else -> error("Unexpected classifier symbol kind: $symbolKind for signature $idSig")
        }
    }

    fun referenceLocalIrSymbol(symbol: IrSymbol, signature: IdSignature) {
        deserializedSymbols.putIfAbsent(signature, symbol)
    }

    fun referenceSimpleFunctionByLocalSignature(idSignature: IdSignature) : IrSimpleFunctionSymbol =
        deserializeIrSymbolData(idSignature, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) as IrSimpleFunctionSymbol

    fun referencePropertyByLocalSignature(idSignature: IdSignature): IrPropertySymbol =
        deserializeIrSymbolData(idSignature, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) as IrPropertySymbol

    private fun deserializeIrSymbolData(idSignature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (!idSignature.isPubliclyVisible) {
            return deserializedSymbols.getOrPut(idSignature) {
                if (enqueueAllDeclarations) {
                    enqueueLocalTopLevelDeclaration(idSignature)
                } else if (idSignature.hasTopLevel) {
                    enqueueLocalTopLevelDeclaration(idSignature.topLevelSignature())
                }
                referenceDeserializedSymbol(symbolKind, idSignature)
            }
        }

        return deserializePublicSymbol(idSignature, symbolKind)
    }

    fun deserializeIrSymbolToDeclare(code: Long): Pair<IrSymbol, IdSignature> {
        val symbolData = parseSymbolData(code)
        val signature = deserializeIdSignature(symbolData.signatureId)
        return Pair(deserializeIrSymbolData(signature, symbolData.kind), signature)
    }

    fun parseSymbolData(code: Long): BinarySymbolData = BinarySymbolData.decode(code)

    private val symbolCache = HashMap<Long, IrSymbol>()

    fun deserializeIrSymbol(code: Long): IrSymbol {
        return symbolCache.getOrPut(code) {
            val symbolData = parseSymbolData(code)
            val signature = deserializeIdSignature(symbolData.signatureId)
            deserializeIrSymbolData(signature, symbolData.kind)
        }
    }

    private fun readSignature(index: Int): CodedInputStream =
        fileReader.signature(index).codedInputStream

    private fun loadSignatureProto(index: Int): ProtoIdSignature {
        return ProtoIdSignature.parseFrom(readSignature(index), ExtensionRegistryLite.newInstance())
    }

    private val signatureCache = HashMap<Int, IdSignature>()

    fun deserializeIdSignature(index: Int): IdSignature {
        return signatureCache.getOrPut(index) {
            val sigData = loadSignatureProto(index)
            deserializeSignatureData(sigData)
        }
    }

    /* -------------------------------------------------------------- */

    // TODO: Think about isolating id signature related logic behind corresponding interface

    private fun deserializePublicIdSignature(proto: ProtoCommonIdSignature): IdSignature.CommonSignature {
        val pkg = fileReader.deserializeFqName(proto.packageFqNameList)
        val cls = fileReader.deserializeFqName(proto.declarationFqNameList)
        val memberId = if (proto.hasMemberUniqId()) proto.memberUniqId else null

        return IdSignature.CommonSignature(pkg, cls, memberId, proto.flags)
    }

    private fun deserializeAccessorIdSignature(proto: ProtoAccessorIdSignature): IdSignature.AccessorSignature {
        val propertySignature = deserializeIdSignature(proto.propertySignature)
        require(propertySignature is IdSignature.CommonSignature) { "For public accessor corresponding property supposed to be public as well" }
        val name = fileReader.deserializeString(proto.name)
        val hash = proto.accessorHashId
        val mask = proto.flags

        val accessorSignature =
            IdSignature.CommonSignature(propertySignature.packageFqName, "${propertySignature.declarationFqName}.$name", hash, mask)

        return IdSignature.AccessorSignature(propertySignature, accessorSignature)
    }

    private fun deserializeFileLocalIdSignature(proto: ProtoFileLocalIdSignature): IdSignature {
        return IdSignature.FileLocalSignature(deserializeIdSignature(proto.container), proto.localId)
    }

    private fun deserializeScopeLocalIdSignature(proto: Int): IdSignature {
        return IdSignature.ScopeLocalDeclaration(proto)
    }

    private fun deserializeLoweredDeclarationSignature(proto: ProtoLoweredIdSignature): IdSignature.LoweredDeclarationSignature {
        return IdSignature.LoweredDeclarationSignature(deserializeIdSignature(proto.parentSignature), proto.stage, proto.index)
    }

    private fun deserializeCompositeIdSignature(proto: ProtoCompositeSignature): IdSignature.CompositeSignature {
        val containerSig = deserializeIdSignature(proto.containerSig)
        val innerSig = deserializeIdSignature(proto.innerSig)
        return IdSignature.CompositeSignature(containerSig, innerSig)
    }

    private fun deserializeLocalIdSignature(proto: ProtoLocalSignature): IdSignature.LocalSignature {
        val localFqn = fileReader.deserializeFqName(proto.localFqNameList)
        val localHash = if (proto.hasLocalHash()) proto.localHash else null
        val description = if (proto.hasDebugInfo()) fileReader.deserializeDebugInfo(proto.debugInfo) else null
        return IdSignature.LocalSignature(localFqn, localHash, description)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun deserializeFileIdSignature(proto: ProtoFileSignature): IdSignature.FileSignature = fileSignature

    fun deserializeSignatureData(proto: ProtoIdSignature): IdSignature {
        return when (proto.idSigCase) {
            PUBLIC_SIG -> deserializePublicIdSignature(proto.publicSig)
            ACCESSOR_SIG -> deserializeAccessorIdSignature(proto.accessorSig)
            PRIVATE_SIG -> deserializeFileLocalIdSignature(proto.privateSig)
            SCOPED_LOCAL_SIG -> deserializeScopeLocalIdSignature(proto.scopedLocalSig)
            COMPOSITE_SIG -> deserializeCompositeIdSignature(proto.compositeSig)
            LOCAL_SIG -> deserializeLocalIdSignature(proto.localSig)
            FILE_SIG -> deserializeFileIdSignature(proto.fileSig)
            // IR IC part
            IC_SIG -> deserializeLoweredDeclarationSignature(proto.icSig)
            else -> error("Unexpected IdSignature kind: ${proto.idSigCase}")
        }
    }
}
