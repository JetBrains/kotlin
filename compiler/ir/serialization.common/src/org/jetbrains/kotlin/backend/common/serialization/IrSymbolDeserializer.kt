/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.backend.common.serialization.proto.Actual
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature.IdsigCase.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature as ProtoAccessorIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature as ProtoFileLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.ScopeLocalIdSignature as ProtoScopeLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.PublicIdSignature as ProtoPublicIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.LoweredIdSignature as ProtoLoweredIdSignature

class IrSymbolDeserializer(
    val symbolTable: ReferenceSymbolTable,
    val fileReader: IrLibraryFile,
    val fileSymbol: IrFileSymbol,
    val actuals: List<Actual>,
    val enqueueLocalTopLevelDeclaration: (IdSignature) -> Unit,
    val handleExpectActualMapping: (IdSignature, IrSymbol) -> IrSymbol,
    private val enqueueAllDeclarations: Boolean = false,
    private val useGlobalSignatures: Boolean = false,
    val deserializedSymbols: MutableMap<IdSignature, IrSymbol> = mutableMapOf(), // Per-file signature cache. TODO: do we really need it?
    val deserializePublicSymbol: (IdSignature, BinarySymbolData.SymbolKind) -> IrSymbol,
) {

    fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        return deserializedSymbols.getOrPut(idSig) {
            val symbol = referenceDeserializedSymbol(symbolKind, idSig)

            handleExpectActualMapping(idSig, symbol)
        }
    }

    private fun referenceDeserializedSymbol(symbolKind: BinarySymbolData.SymbolKind, idSig: IdSignature): IrSymbol = symbolTable.run {
        when (symbolKind) {
            BinarySymbolData.SymbolKind.ANONYMOUS_INIT_SYMBOL -> if (useGlobalSignatures) IrAnonymousInitializerPublicSymbolImpl(idSig) else IrAnonymousInitializerSymbolImpl()
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
            BinarySymbolData.SymbolKind.VALUE_PARAMETER_SYMBOL -> if (useGlobalSignatures) IrValueParameterPublicSymbolImpl(idSig) else IrValueParameterSymbolImpl()
            BinarySymbolData.SymbolKind.RECEIVER_PARAMETER_SYMBOL -> if (useGlobalSignatures) IrValueParameterPublicSymbolImpl(idSig) else IrValueParameterSymbolImpl()
            BinarySymbolData.SymbolKind.LOCAL_DELEGATED_PROPERTY_SYMBOL ->
                IrLocalDelegatedPropertySymbolImpl()
            BinarySymbolData.SymbolKind.FILE_SYMBOL -> (idSig as IdSignature.FileSignature).symbol
            else -> error("Unexpected classifier symbol kind: $symbolKind for signature $idSig")
        }
    }

    fun referenceLocalIrSymbol(symbol: IrSymbol, signature: IdSignature) {
//        assert(signature.isLocal)
        deserializedSymbols.putIfAbsent(signature, symbol)
        if (enqueueAllDeclarations) enqueueLocalTopLevelDeclaration(signature)
    }

    fun referenceSimpleFunctionByLocalSignature(idSignature: IdSignature) : IrSimpleFunctionSymbol =
        deserializeIrSymbolData(idSignature, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) as IrSimpleFunctionSymbol

    fun referencePropertyByLocalSignature(idSignature: IdSignature): IrPropertySymbol =
        deserializeIrSymbolData(idSignature, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) as IrPropertySymbol

    private fun deserializeIrSymbolData(idSignature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (idSignature.isLocal) {
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

    private fun deserializeFileLocalIdSignature(proto: ProtoFileLocalIdSignature): IdSignature {
        if (useGlobalSignatures) {
            val fp = if (proto.hasFile()) fileReader.deserializeString(proto.file) else fileSymbol.owner.path
            return IdSignature.GlobalFileLocalSignature(deserializeIdSignature(proto.container), proto.localId, fp)
        } else {
            return IdSignature.FileLocalSignature(deserializeIdSignature(proto.container), proto.localId)
        }
    }

    private fun deserializeScopeLocalIdSignature(proto: Int): IdSignature {
        if (useGlobalSignatures) {
            return IdSignature.GlobalScopeLocalDeclaration(proto, filePath = fileSymbol.owner.path)
        } else {
            return IdSignature.ScopeLocalDeclaration(proto)
        }
    }

    private fun deserializeExternalScopeLocalIdSignature(proto: ProtoScopeLocalIdSignature): IdSignature {
        if (useGlobalSignatures) {
            return IdSignature.GlobalScopeLocalDeclaration(proto.id, filePath = fileReader.deserializeString(proto.file))
        } else {
            return IdSignature.ScopeLocalDeclaration(proto.id)
        }
    }

    private fun deserializeLoweredDeclarationSignature(proto: ProtoLoweredIdSignature): IdSignature.LoweredDeclarationSignature {
        return IdSignature.LoweredDeclarationSignature(deserializeIdSignature(proto.parentSignature), proto.stage, proto.index)
    }

    fun deserializeSignatureData(proto: ProtoIdSignature): IdSignature {
        return when (proto.idsigCase) {
            PUBLIC_SIG -> deserializePublicIdSignature(proto.publicSig)
            ACCESSOR_SIG -> deserializeAccessorIdSignature(proto.accessorSig)
            PRIVATE_SIG -> deserializeFileLocalIdSignature(proto.privateSig)
            SCOPED_LOCAL_SIG -> deserializeScopeLocalIdSignature(proto.scopedLocalSig)
            IC_SIG -> deserializeLoweredDeclarationSignature(proto.icSig)
            FILE_SIG -> IdSignature.FileSignature(fileSymbol)
            EXTERNAL_SCOPED_LOCAL_SIG -> deserializeExternalScopeLocalIdSignature(proto.externalScopedLocalSig)
            else -> error("Unexpected IdSignature kind: ${proto.idsigCase}")
        }
    }
}

// TODO: make internal and move to deserializer
private class IrAnonymousInitializerPublicSymbolImpl(sig: IdSignature, descriptor: ClassDescriptor? = null) :
    IrBindablePublicSymbolBase<ClassDescriptor, IrAnonymousInitializer>(sig, descriptor),
    IrAnonymousInitializerSymbol

private class IrValueParameterPublicSymbolImpl(sig: IdSignature, descriptor: ParameterDescriptor? = null) :
    IrBindablePublicSymbolBase<ParameterDescriptor, IrValueParameter>(sig, descriptor),
    IrValueParameterSymbol
