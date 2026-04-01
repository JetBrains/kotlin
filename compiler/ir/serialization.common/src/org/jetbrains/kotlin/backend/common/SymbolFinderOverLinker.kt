/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.SymbolFinder
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

@OptIn(InternalSymbolFinderAPI::class)
class SymbolFinderOverLinker(private val linker: KotlinIrLinker) : SymbolFinder() {
    override fun findClass(classId: ClassId): IrClassSymbol? {
        TODO("Not yet implemented")
    }

    override fun findFunctions(callableId: CallableId): Iterable<IrSimpleFunctionSymbol> {
        TODO("Not yet implemented")
    }

    override fun findProperties(callableId: CallableId): Iterable<IrPropertySymbol> {
        TODO("Not yet implemented")
    }
}