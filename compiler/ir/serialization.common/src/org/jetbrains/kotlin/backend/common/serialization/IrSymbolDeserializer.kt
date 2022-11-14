/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.proto.Actual
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable

class IrSymbolDeserializer(
    val symbolTable: ReferenceSymbolTable,
    val libraryFile: IrLibraryFile,
    val fileSymbol: IrFileSymbol,
    val actuals: List<Actual>,
    val enqueueLocalTopLevelDeclaration: (IdSignature) -> Unit,
    val handleExpectActualMapping: (IdSignature, IrSymbol) -> IrSymbol,
    val symbolProcessor: IrSymbolDeserializer.(IrSymbol, IdSignature) -> IrSymbol = { s, _ -> s },
    fileSignature: IdSignature.FileSignature = IdSignature.FileSignature(fileSymbol),
    val deserializePublicSymbol: (IdSignature, BinarySymbolData.SymbolKind) -> IrSymbol
) {
    val deserializedSymbols: MutableMap<IdSignature, IrSymbol> = mutableMapOf()

    fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        return deserializedSymbols.getOrPut(idSig) {
            val symbol = referenceDeserializedSymbol(symbolKind, idSig)

            handleExpectActualMapping(idSig, symbol)
        }
    }

    private fun referenceDeserializedSymbol(symbolKind: BinarySymbolData.SymbolKind, idSig: IdSignature): IrSymbol {
        return symbolProcessor(referenceDeserializedSymbol(symbolTable, fileSymbol, symbolKind, idSig), idSig)
    }

    fun referenceLocalIrSymbol(symbol: IrSymbol, signature: IdSignature) {
        deserializedSymbols.put(signature, symbol)
    }

    fun referenceSimpleFunctionByLocalSignature(idSignature: IdSignature) : IrSimpleFunctionSymbol =
        deserializeIrSymbolData(idSignature, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) as IrSimpleFunctionSymbol

    fun referencePropertyByLocalSignature(idSignature: IdSignature): IrPropertySymbol =
        deserializeIrSymbolData(idSignature, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) as IrPropertySymbol

    private fun deserializeIrSymbolData(idSignature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (!idSignature.isPubliclyVisible) {
            return deserializedSymbols.getOrPut(idSignature) {
                if (idSignature.hasTopLevel) {
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

    val signatureDeserializer = IdSignatureDeserializer(libraryFile, fileSignature)

    fun deserializeIdSignature(index: Int): IdSignature {
        return signatureDeserializer.deserializeIdSignature(index)
    }
}

internal fun referenceDeserializedSymbol(
    symbolTable: ReferenceSymbolTable,
    fileSymbol: IrFileSymbol?,
    symbolKind: BinarySymbolData.SymbolKind,
    idSig: IdSignature
): IrSymbol = symbolTable.run {
    when (symbolKind) {
        BinarySymbolData.SymbolKind.ANONYMOUS_INIT_SYMBOL -> IrAnonymousInitializerSymbolImpl()
        BinarySymbolData.SymbolKind.CLASS_SYMBOL -> referenceClass(idSig)
        BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> referenceConstructor(idSig)
        BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL -> referenceTypeParameter(idSig)
        BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> referenceEnumEntry(idSig)
        BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> referenceField(idSig)
        BinarySymbolData.SymbolKind.FIELD_SYMBOL -> referenceField(idSig)
        BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunction(idSig)
        BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> referenceTypeAlias(idSig)
        BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referenceProperty(idSig)
        BinarySymbolData.SymbolKind.VARIABLE_SYMBOL -> IrVariableSymbolImpl()
        BinarySymbolData.SymbolKind.VALUE_PARAMETER_SYMBOL -> IrValueParameterSymbolImpl()
        BinarySymbolData.SymbolKind.RECEIVER_PARAMETER_SYMBOL -> IrValueParameterSymbolImpl()
        BinarySymbolData.SymbolKind.LOCAL_DELEGATED_PROPERTY_SYMBOL -> IrLocalDelegatedPropertySymbolImpl()
        BinarySymbolData.SymbolKind.FILE_SYMBOL -> fileSymbol ?: error("File symbol is not provided")
        else -> error("Unexpected classifier symbol kind: $symbolKind for signature $idSig")
    }
}