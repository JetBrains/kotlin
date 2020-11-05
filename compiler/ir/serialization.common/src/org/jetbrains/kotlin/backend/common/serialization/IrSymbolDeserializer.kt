/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature.IdsigCase.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature as ProtoAccessorIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature as ProtoFileLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.PublicIdSignature as ProtoPublicIdSignature

internal class IrSymbolDeserializer(
    val fileReader: IrLibraryFile,
    val symbolTable: SymbolTable,
    val fileDeserializer: IrFileDeserializer,
    val expectUniqIdToActualUniqId: MutableMap<IdSignature, IdSignature>,
    val expectSymbols: MutableMap<IdSignature, IrSymbol>,
    val actualSymbols: MutableMap<IdSignature, IrSymbol>,
) {

    val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

    fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        return deserializedSymbols.getOrPut(idSig) {
            val symbol = referenceDeserializedSymbol(symbolKind, idSig)

            handleExpectActualMapping(idSig, symbol)
        }
    }

    private fun handleExpectActualMapping(idSig: IdSignature, rawSymbol: IrSymbol): IrSymbol {
        val referencingSymbol = if (idSig in expectUniqIdToActualUniqId.keys) {
            assert(idSig.run { IdSignature.Flags.IS_EXPECT.test() })
            wrapInDelegatedSymbol(rawSymbol).also { expectSymbols[idSig] = it }
        } else rawSymbol

        if (idSig in expectUniqIdToActualUniqId.values) {
            actualSymbols[idSig] = rawSymbol
        }

        return referencingSymbol
    }

    private fun referenceDeserializedSymbol(symbolKind: BinarySymbolData.SymbolKind, idSig: IdSignature): IrSymbol = symbolTable.run {
        when (symbolKind) {
            BinarySymbolData.SymbolKind.ANONYMOUS_INIT_SYMBOL -> IrAnonymousInitializerSymbolImpl(WrappedClassDescriptor())
            BinarySymbolData.SymbolKind.CLASS_SYMBOL -> referenceClassFromLinker(WrappedClassDescriptor(), idSig)
            BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> referenceConstructorFromLinker(WrappedClassConstructorDescriptor(), idSig)
            BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL -> referenceTypeParameterFromLinker(WrappedTypeParameterDescriptor(), idSig)
            BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> referenceEnumEntryFromLinker(WrappedEnumEntryDescriptor(), idSig)
            BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> referenceFieldFromLinker(WrappedFieldDescriptor(), idSig)
            BinarySymbolData.SymbolKind.FIELD_SYMBOL -> referenceFieldFromLinker(WrappedPropertyDescriptor(), idSig)
            BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunctionFromLinker(WrappedSimpleFunctionDescriptor(), idSig)
            BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> referenceTypeAliasFromLinker(WrappedTypeAliasDescriptor(), idSig)
            BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referencePropertyFromLinker(WrappedPropertyDescriptor(), idSig)
            BinarySymbolData.SymbolKind.VARIABLE_SYMBOL -> IrVariableSymbolImpl(WrappedVariableDescriptor())
            BinarySymbolData.SymbolKind.VALUE_PARAMETER_SYMBOL -> IrValueParameterSymbolImpl(WrappedValueParameterDescriptor())
            BinarySymbolData.SymbolKind.RECEIVER_PARAMETER_SYMBOL -> IrValueParameterSymbolImpl(WrappedReceiverParameterDescriptor())
            BinarySymbolData.SymbolKind.LOCAL_DELEGATED_PROPERTY_SYMBOL ->
                IrLocalDelegatedPropertySymbolImpl(WrappedVariableDescriptorWithAccessor())
            else -> error("Unexpected classifier symbol kind: $symbolKind for signature $idSig")
        }
    }

    fun referenceIrSymbol(symbol: IrSymbol, signature: IdSignature) {
        assert(signature.isLocal)
        deserializedSymbols.putIfAbsent(signature, symbol)
    }

    private fun deserializeIrLocalSymbolData(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        assert(idSig.isLocal)

        if (idSig.hasTopLevel) {
            fileDeserializer.fileLocalDeserializationState.addIdSignature(idSig.topLevelSignature())
        }

        return deserializedSymbols.getOrPut(idSig) {
            referenceDeserializedSymbol(symbolKind, idSig)
        }
    }

    private fun deserializeIrSymbolData(idSignature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (idSignature.isLocal) return deserializeIrLocalSymbolData(idSignature, symbolKind)

        return fileDeserializer.findModuleDeserializer(idSignature).deserializeIrSymbol(idSignature, symbolKind)
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

    // TODO should this delegation logic be here?
    private val delegatedSymbolMap = mutableMapOf<IrSymbol, IrSymbol>()

    internal fun deserializeIrSymbolAndRemap(code: Long): IrSymbol {
        // TODO: could be simplified
        return deserializeIrSymbol(code).let {
            delegatedSymbolMap[it] ?: it
        }
    }

    internal fun recordDelegatedSymbol(symbol: IrSymbol) {
        if (symbol is IrDelegatingSymbol<*, *, *>) {
            delegatedSymbolMap[symbol] = symbol.delegate
        }
    }

    internal fun eraseDelegatedSymbol(symbol: IrSymbol) {
        if (symbol is IrDelegatingSymbol<*, *, *>) {
            delegatedSymbolMap.remove(symbol)
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

    private fun deserializeFileLocalIdSignature(proto: ProtoFileLocalIdSignature): IdSignature.FileLocalSignature {
        return IdSignature.FileLocalSignature(deserializeIdSignature(proto.container), proto.localId)
    }

    private fun deserializeScopeLocalIdSignature(proto: Int): IdSignature.ScopeLocalDeclaration {
        return IdSignature.ScopeLocalDeclaration(proto)
    }

    fun deserializeSignatureData(proto: ProtoIdSignature): IdSignature {
        return when (proto.idsigCase) {
            PUBLIC_SIG -> deserializePublicIdSignature(proto.publicSig)
            ACCESSOR_SIG -> deserializeAccessorIdSignature(proto.accessorSig)
            PRIVATE_SIG -> deserializeFileLocalIdSignature(proto.privateSig)
            SCOPED_LOCAL_SIG -> deserializeScopeLocalIdSignature(proto.scopedLocalSig)
            else -> error("Unexpected IdSignature kind: ${proto.idsigCase}")
        }
    }
}