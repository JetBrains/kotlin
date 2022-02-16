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
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.StringSignature

class IrSymbolDeserializer(
    val symbolTable: ReferenceSymbolTable,
    val libraryFile: IrLibraryFile,
    val fileSymbol: IrFileSymbol,
    val actuals: List<Actual>,
//    val enqueueLocalTopLevelDeclaration: (StringSignature) -> Unit,
    val handleExpectActualMapping: (StringSignature, IrSymbol) -> IrSymbol,
    val symbolProcessor: IrSymbolDeserializer.(IrSymbol, StringSignature) -> IrSymbol = { s, _ -> s },
//    private val fileSignature: IdSignature.FileSignature = IdSignature.FileSignature(fileSymbol),
    val deserializePublicSymbol: (StringSignature, BinarySymbolData.SymbolKind) -> IrSymbol
) {

    val deserializedSymbols: MutableMap<StringSignature, IrSymbol> = mutableMapOf()

    val signatureCache = mutableMapOf<Int, StringSignature>()

    fun deserializeIrSymbol(signature: StringSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        return deserializedSymbols.getOrPut(signature) {
            val symbol = referenceDeserializedSymbol(symbolKind, signature)

            handleExpectActualMapping(signature, symbol)
        }
    }

    private fun referenceDeserializedSymbol(symbolKind: BinarySymbolData.SymbolKind, signature: StringSignature): IrSymbol {
        return symbolProcessor(referenceDeserializedSymbol(symbolTable, fileSymbol, symbolKind, signature), signature)
    }

    fun referenceLocalIrSymbol(symbol: IrSymbol, signature: StringSignature) {
        deserializedSymbols.put(signature, symbol)
    }

    fun referenceSimpleFunctionByLocalSignature(signature: StringSignature): IrSimpleFunctionSymbol =
        deserializeIrSymbolData(signature, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) as IrSimpleFunctionSymbol

    fun referencePropertyByLocalSignature(signature: StringSignature): IrPropertySymbol =
        deserializeIrSymbolData(signature, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) as IrPropertySymbol

    private fun deserializeIrSymbolData(signature: StringSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (signature.isLocal) {
            return deserializedSymbols.getOrPut(signature) {
                referenceDeserializedSymbol(symbolKind, signature)
            }
        }

        return deserializePublicSymbol(signature, symbolKind)
    }

    fun deserializeIrSymbolToDeclare(code: Long): Pair<IrSymbol, StringSignature> {
        val symbolData = parseSymbolData(code)
        val signature = deserializeSignature(symbolData.signatureId)
        return Pair(deserializeIrSymbolData(signature, symbolData.kind), signature)
    }

    fun parseSymbolData(code: Long): BinarySymbolData = BinarySymbolData.decode(code)

    private val symbolCache = HashMap<Long, IrSymbol>()

    fun deserializeIrSymbol(code: Long): IrSymbol {
        return symbolCache.getOrPut(code) {
            val symbolData = parseSymbolData(code)
            val signature = deserializeSignature(symbolData.signatureId)
            deserializeIrSymbolData(signature, symbolData.kind)
        }
    }

    fun deserializeSignature(index: Int): StringSignature {
        return signatureCache.getOrPut(index) {
            StringSignature(libraryFile.string(index))
        }
//        val r = StringSignature(libraryFile.string(index))
//        return r
    }

//    val signatureDeserializer = IdSignatureDeserializer(libraryFile, fileSignature)
//
//    fun deserializeIdSignature(index: Int): IdSignature {
//        return signatureDeserializer.deserializeIdSignature(index)
//    }
}

internal fun referenceDeserializedSymbol(
    symbolTable: ReferenceSymbolTable,
    fileSymbol: IrFileSymbol?,
    symbolKind: BinarySymbolData.SymbolKind,
    signature: StringSignature
): IrSymbol = symbolTable.run {
    when (symbolKind) {
        BinarySymbolData.SymbolKind.ANONYMOUS_INIT_SYMBOL -> IrAnonymousInitializerSymbolImpl()
        BinarySymbolData.SymbolKind.CLASS_SYMBOL -> referenceClass(signature, false)
        BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> referenceConstructor(signature, false)
        BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL -> referenceTypeParameter(signature, false)
        BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> referenceEnumEntry(signature, false)
        BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> referenceField(signature, false)
        BinarySymbolData.SymbolKind.FIELD_SYMBOL -> referenceField(signature, false)
        BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> referenceSimpleFunction(signature, false)
        BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> referenceTypeAlias(signature, false)
        BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> referenceProperty(signature, false)
        BinarySymbolData.SymbolKind.VARIABLE_SYMBOL -> IrVariableSymbolImpl()
        BinarySymbolData.SymbolKind.VALUE_PARAMETER_SYMBOL -> IrValueParameterSymbolImpl()
        BinarySymbolData.SymbolKind.RECEIVER_PARAMETER_SYMBOL -> IrValueParameterSymbolImpl()
        BinarySymbolData.SymbolKind.LOCAL_DELEGATED_PROPERTY_SYMBOL ->
            IrLocalDelegatedPropertySymbolImpl()
        BinarySymbolData.SymbolKind.FILE_SYMBOL -> fileSymbol ?: error("File symbol is not provided")
        else -> error("Unexpected classifier symbol kind: $symbolKind for signature $signature")
    }
}