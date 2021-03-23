/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.proto.Actual
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature.IdsigCase.*
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
import org.jetbrains.kotlin.backend.common.serialization.proto.CompositeSignature as ProtoCompositeSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature as ProtoFileLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileSignature as ProtoFileSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.LocalSignature as ProtoLocalSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.PublicIdSignature as ProtoPublicIdSignature

class IrSymbolDeserializer(
    val symbolTable: ReferenceSymbolTable,
    val fileReader: IrLibraryFile,
    fileSymbol: IrFileSymbol,
    val actuals: List<Actual>,
    val enqueueLocalTopLevelDeclaration: (IdSignature) -> Unit,
    val handleExpectActualMapping: (IdSignature, IrSymbol) -> IrSymbol,
    val deserializePublicSymbol: (IdSignature, BinarySymbolData.SymbolKind) -> IrSymbol,
) {

    val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

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
            BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL -> referenceTypeParameterFromLinker(idSig)
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
            else -> error("Unexpected classifier symbol kind: $symbolKind for signature $idSig")
        }
    }

    fun referenceLocalIrSymbol(symbol: IrSymbol, signature: IdSignature) {
//        assert(signature.isLocal)
        deserializedSymbols.putIfAbsent(signature, symbol)
    }

    fun referenceSimpleFunctionByLocalSignature(idSignature: IdSignature) : IrSimpleFunctionSymbol =
        deserializeIrSymbolData(idSignature, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) as IrSimpleFunctionSymbol

    fun referencePropertyByLocalSignature(idSignature: IdSignature): IrPropertySymbol =
        deserializeIrSymbolData(idSignature, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) as IrPropertySymbol

    private fun deserializeIrSymbolData(idSignature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (idSignature.isLocal) {
            if (idSignature.hasTopLevel) {
                enqueueLocalTopLevelDeclaration(idSignature.topLevelSignature())
            }
            return deserializedSymbols.getOrPut(idSignature) {
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

    fun deserializeIrSymbol(code: Long): IrSymbol {
        val symbolData = parseSymbolData(code)
        val signature = deserializeIdSignature(symbolData.signatureId)
        return deserializeIrSymbolData(signature, symbolData.kind)
    }

    private fun readSignature(index: Int): CodedInputStream =
        fileReader.signature(index).codedInputStream

    private fun loadSignatureProto(index: Int): ProtoIdSignature {
        return ProtoIdSignature.parseFrom(readSignature(index), ExtensionRegistryLite.newInstance())
    }

    fun deserializeIdSignature(index: Int): IdSignature {
        val sigData = loadSignatureProto(index)
        return deserializeSignatureData(sigData)
    }

    /* -------------------------------------------------------------- */

    // TODO: Think about isolating id signature related logic behind corresponding interface

    private fun deserializePublicIdSignature(proto: ProtoPublicIdSignature): IdSignature.PublicSignature {
        val pkg = fileReader.deserializeFqName(proto.packageFqNameList)
        val cls = fileReader.deserializeFqName(proto.declarationFqNameList)
        val memberId = if (proto.hasMemberUniqId()) proto.memberUniqId else null

        return IdSignature.PublicSignature(pkg, cls, memberId, proto.flags)
    }

    private fun deserializeAccessorIdSignature(proto: ProtoAccessorIdSignature): IdSignature.AccessorSignature {
        val propertySignature = deserializeIdSignature(proto.propertySignature)
        require(propertySignature is IdSignature.PublicSignature) { "For public accessor corresponding property supposed to be public as well" }
        val name = fileReader.deserializeString(proto.name)
        val hash = proto.accessorHashId
        val mask = proto.flags

        val accessorSignature =
            IdSignature.PublicSignature(propertySignature.packageFqName, "${propertySignature.declarationFqName}.$name", hash, mask)

        return IdSignature.AccessorSignature(propertySignature, accessorSignature)
    }

    private fun deserializeFileLocalIdSignature(proto: ProtoFileLocalIdSignature): IdSignature.FileLocalSignature {
        return IdSignature.FileLocalSignature(deserializeIdSignature(proto.container), proto.localId)
    }

    private fun deserializeScopeLocalIdSignature(proto: Int): IdSignature.ScopeLocalDeclaration {
        return IdSignature.ScopeLocalDeclaration(proto)
    }

    private fun deserializeCompositeIdSignature(proto: ProtoCompositeSignature): IdSignature.CompositeSignature {
        val containerSig = deserializeIdSignature(proto.containerSig)
        val innerSig = deserializeIdSignature(proto.innerSig)
        return IdSignature.CompositeSignature(containerSig, innerSig)
    }

    private fun deserializeLocalIdSignature(proto: ProtoLocalSignature): IdSignature.LocalSignature {
        val localFqn = fileReader.deserializeFqName(proto.localFqNameList)
        val localHash = if (proto.hasLocalHash()) proto.localHash else null
        val description = if (proto.hasDescription()) fileReader.deserializeString(proto.description) else null
        return IdSignature.LocalSignature(localFqn, localHash, description)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun deserializeFileIdSignature(proto: ProtoFileSignature): IdSignature.FileSignature = fileSignature

    fun deserializeSignatureData(proto: ProtoIdSignature): IdSignature {
        return when (proto.idsigCase) {
            PUBLIC_SIG -> deserializePublicIdSignature(proto.publicSig)
            ACCESSOR_SIG -> deserializeAccessorIdSignature(proto.accessorSig)
            PRIVATE_SIG -> deserializeFileLocalIdSignature(proto.privateSig)
            SCOPED_LOCAL_SIG -> deserializeScopeLocalIdSignature(proto.scopedLocalSig)
            COMPOSITE_SIG -> deserializeCompositeIdSignature(proto.compositeSig)
            LOCAL_SIG -> deserializeLocalIdSignature(proto.localSig)
            FILE_SIG -> deserializeFileIdSignature(proto.fileSig)
            else -> error("Unexpected IdSignature kind: ${proto.idsigCase}")
        }
    }
}