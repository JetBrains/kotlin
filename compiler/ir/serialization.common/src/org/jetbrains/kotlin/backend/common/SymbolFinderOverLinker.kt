/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.SymbolFinder
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.toIdSignature
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

@OptIn(InternalSymbolFinderAPI::class)
class SymbolFinderOverLinker(private val linker: KotlinIrLinker) : SymbolFinder() {
    override fun findClass(classId: ClassId): IrClassSymbol? {
        val signature = classId.toIdSignature()
        return linker.getSymbolAndPutIntoQueue(signature, kind = BinarySymbolData.SymbolKind.CLASS_SYMBOL) as? IrClassSymbol
    }

    override fun findFunctions(callableId: CallableId): Iterable<IrSimpleFunctionSymbol> {
        check(callableId.classId == null) { "Function $callableId must be a top level one" }
        val signatures = linker.getAllMatchingSignatures(callableId, BinarySymbolData.SymbolKind.FUNCTION_SYMBOL)
        return signatures.mapNotNull {
            linker.getSymbolAndPutIntoQueue(it, kind = BinarySymbolData.SymbolKind.FUNCTION_SYMBOL) as? IrSimpleFunctionSymbol
        }
    }

    override fun findProperties(callableId: CallableId): Iterable<IrPropertySymbol> {
        check(callableId.classId == null) { "Property $callableId must be a top level one" }
        val signatures = linker.getAllMatchingSignatures(callableId, BinarySymbolData.SymbolKind.PROPERTY_SYMBOL)
        return signatures.mapNotNull {
            linker.getSymbolAndPutIntoQueue(it, kind = BinarySymbolData.SymbolKind.PROPERTY_SYMBOL) as? IrPropertySymbol
        }
    }
}
