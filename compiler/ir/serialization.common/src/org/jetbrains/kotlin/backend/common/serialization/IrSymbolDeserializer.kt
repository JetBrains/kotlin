/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable

class IrSymbolDeserializer(
    private val symbolTable: ReferenceSymbolTable,
    private val libraryFile: IrLibraryFile,
    private val fileSymbol: IrFileSymbol,
    private val enqueueLocalTopLevelDeclaration: (IdSignature) -> Unit,
    irInterner: IrInterningService,
    private val deserializedSymbolPostProcessor: (IrSymbol, IdSignature, IrFileSymbol) -> IrSymbol = { s, _, _ -> s },
    fileSignature: IdSignature.FileSignature = IdSignature.FileSignature(fileSymbol),
    private val deserializePublicSymbolWithOwnerInUnknownFile: (IdSignature, BinarySymbolData.SymbolKind) -> IrSymbol
) {
    /** The deserialized symbols of declarations belonging only to the current file, [libraryFile]. */
    val deserializedSymbolsWithOwnersInCurrentFile: Map<IdSignature, IrSymbol>
        field = hashMapOf()

    private val symbolCache = HashMap<Long, IrSymbol>()

    /** Deserializes a symbol known to belong to the current file, [libraryFile]. */
    fun deserializeSymbolWithOwnerInCurrentFile(signature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        return deserializedSymbolsWithOwnersInCurrentFile.getOrPut(signature) {
            referenceDeserializedSymbol(symbolKind, signature)
        }
    }

    /**
     * This function helps [IrDeclarationDeserializer] to deserialize symbols of deserialized declarations.
     * So, it is always called for the symbols belonging to the current file, [libraryFile].
     */
    fun deserializeSymbolToDeclareInCurrentFile(code: Long): Pair<IrSymbol, IdSignature> {
        val symbolData = parseSymbolData(code)
        val signature = deserializeIdSignature(symbolData.signatureId)
        val symbol = deserializeSymbolWithOwnerInCurrentFile(signature, symbolData.kind)

        symbolCache[code] = symbol

        return symbol to signature
    }

    /**
     * Deserializes a symbol that may belong to the current file (typically that's a symbol of a declaration being deserialized right now),
     * or belongs to another file (e.g., a symbol in a [IrMemberAccessExpression] being deserialized right now).
     */
    fun deserializeSymbolWithOwnerMaybeInOtherFile(code: Long): IrSymbol {
        return symbolCache.getOrPut(code) {
            val symbolData = parseSymbolData(code)
            val signature = deserializeIdSignature(symbolData.signatureId)
            deserializeSymbolWithOwnerMaybeInOtherFile(signature, symbolData.kind)
        }
    }

    private fun deserializeSymbolWithOwnerMaybeInOtherFile(signature: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        if (!signature.isPubliclyVisible) {
            return deserializedSymbolsWithOwnersInCurrentFile.getOrPut(signature) {
                if (signature.hasTopLevel) {
                    enqueueLocalTopLevelDeclaration(signature.topLevelSignature())
                }
                referenceDeserializedSymbol(symbolKind, signature)
            }
        }

        return deserializePublicSymbolWithOwnerInUnknownFile(signature, symbolKind)
    }

    private fun referenceDeserializedSymbol(symbolKind: BinarySymbolData.SymbolKind, signature: IdSignature): IrSymbol {
        val referencedSymbol = referenceDeserializedSymbol(symbolTable, fileSymbol, symbolKind, signature)
        val postProcessedSymbol = deserializedSymbolPostProcessor(referencedSymbol, signature, fileSymbol)
        return postProcessedSymbol
    }

    /** Notify [IrSymbolDeserializer] about a known symbol that belongs to the current file, [libraryFile]. */
    fun referenceLocalIrSymbol(symbol: IrSymbol, signature: IdSignature) {
        deserializedSymbolsWithOwnersInCurrentFile.put(signature, symbol)
    }

    fun referenceSimpleFunctionByLocalSignature(signature: IdSignature): IrSimpleFunctionSymbol =
        deserializeSymbolWithOwnerMaybeInOtherFile(signature, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) as IrSimpleFunctionSymbol

    fun referencePropertyByLocalSignature(signature: IdSignature): IrPropertySymbol =
        deserializeSymbolWithOwnerMaybeInOtherFile(signature, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) as IrPropertySymbol

    fun parseSymbolData(code: Long): BinarySymbolData = BinarySymbolData.decode(code)

    val signatureDeserializer = IdSignatureDeserializer(libraryFile, fileSignature, irInterner)

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
        BinarySymbolData.SymbolKind.RETURNABLE_BLOCK_SYMBOL -> IrReturnableBlockSymbolImpl()
        BinarySymbolData.SymbolKind.FILE_SYMBOL -> fileSymbol ?: error("File symbol is not provided")
    }
}