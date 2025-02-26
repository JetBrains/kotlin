/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldFakeOverrideSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFunctionFakeOverrideSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrLocalDelegatedPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertyFakeOverrideSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReturnableBlockSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable

class IrSymbolDeserializer(
    val symbolTable: ReferenceSymbolTable,
    val libraryFile: IrLibraryFile,
    val fileSymbol: IrFileSymbol,
    val enqueueLocalTopLevelDeclaration: (IdSignature) -> Unit,
    irInterner: IrInterningService,
    val symbolProcessor: IrSymbolDeserializer.(IrSymbol, IdSignature) -> IrSymbol = { s, _ -> s },
    fileSignature: IdSignature.FileSignature = IdSignature.FileSignature(fileSymbol),
    private val createFOSymbolForLocalFO: Boolean = false,
    val deserializePublicSymbol: (IdSignature, BinarySymbolData.SymbolKind) -> IrSymbol,
) {
    val deserializedSymbols: MutableMap<IdSignature, IrSymbol> = hashMapOf()

    fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        return deserializedSymbols.getOrPut(idSig) {
            referenceDeserializedSymbol(symbolKind, idSig)
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
                if (createFOSymbolForLocalFO && idSignature is IdSignature.LocalFakeOverrideSignature) {
                    // Normally we create regular symbols for FOs, local and non-local, later on create actual fake overrides
                    // for those symbols, and bind them together.
                    // This branch is for a case when fake overrides cannot be built (that happens when inlining external function on first
                    // stage of Klib compilation). The symbol created here won't ever be bound, so we at least pack the useful information
                    // about the member's class into this IrFakeOverrideSymbol, which would otherwise be lost in a regular IrSymbolImpl.
                    val foSymbol = referenceDeserializedSymbol(symbolKind, idSignature)
                    val classSymbol = deserializeIrSymbol(idSignature.containingClass, BinarySymbolData.SymbolKind.CLASS_SYMBOL) as IrClassSymbol
                    when (foSymbol) {
                        is IrSimpleFunctionSymbol -> IrFunctionFakeOverrideSymbol(foSymbol, classSymbol, null)
                        is IrPropertySymbol -> IrPropertyFakeOverrideSymbol(foSymbol, classSymbol, null)
                        else -> error("This kind of symbol cannot have fake overrides: $foSymbol")
                    }
                } else {
                    if (idSignature.hasTopLevel) {
                        enqueueLocalTopLevelDeclaration(idSignature.topLevelSignature())
                    }
                    referenceDeserializedSymbol(symbolKind, idSignature)
                }
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