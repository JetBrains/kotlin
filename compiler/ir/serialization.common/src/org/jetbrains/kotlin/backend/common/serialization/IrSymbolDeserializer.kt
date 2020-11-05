/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature.IdsigCase.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.AccessorIdSignature as ProtoAccessorIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.FileLocalIdSignature as ProtoFileLocalIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.IdSignature as ProtoIdSignature
import org.jetbrains.kotlin.backend.common.serialization.proto.PublicIdSignature as ProtoPublicIdSignature

internal class IrSymbolDeserializer(
    val fileReader: IrLibraryFile,
    val haveSeen: MutableSet<IrSymbol>,
    val fileDeserializer: IrFileDeserializer,
) {

    val deserializedSymbols = mutableMapOf<IdSignature, IrSymbol>()

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
            fileDeserializer.referenceDeserializedSymbol(symbolKind, idSig)
        }
    }

    private fun deserializeIrSymbolData(idSignature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (idSignature.isLocal) return deserializeIrLocalSymbolData(idSignature, symbolKind)

        return fileDeserializer.findModuleDeserializer(idSignature).deserializeIrSymbol(idSignature, symbolKind).also {
            haveSeen.add(it)
        }
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