/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns

@OptIn(InternalSymbolFinderAPI::class)
abstract class BaseSymbolsImpl(val irBuiltIns: IrBuiltIns) {
    protected val symbolFinder = irBuiltIns.symbolFinder
}

interface PreSerializationSymbols {

}

abstract class PreSerializationSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationSymbols, BaseSymbolsImpl(irBuiltIns) {

}

interface PreSerializationKlibSymbols : PreSerializationSymbols {

}

abstract class PreSerializationKlibSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationKlibSymbols, PreSerializationSymbolsImpl(irBuiltIns) {

}

interface PreSerializationWebSymbols : PreSerializationKlibSymbols {

}

abstract class PreSerializationWebSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationWebSymbols, PreSerializationKlibSymbolsImpl(irBuiltIns) {

}

interface PreSerializationJsSymbols : PreSerializationWebSymbols {}

open class PreSerializationJsSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationJsSymbols, PreSerializationWebSymbolsImpl(irBuiltIns) {

}

interface PreSerializationWasmSymbols : PreSerializationWebSymbols {}

open class PreSerializationWasmSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationWasmSymbols, PreSerializationWebSymbolsImpl(irBuiltIns) {

}

interface PreSerializationNativeSymbols : PreSerializationKlibSymbols {}

open class PreSerializationNativeSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationNativeSymbols, PreSerializationKlibSymbolsImpl(irBuiltIns) {

}
