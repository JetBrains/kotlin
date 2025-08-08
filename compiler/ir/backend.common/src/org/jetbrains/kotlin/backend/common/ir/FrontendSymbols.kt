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

interface FrontendSymbols {

}

abstract class FrontendSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendSymbols, BaseSymbolsImpl(irBuiltIns) {

}

interface FrontendKlibSymbols : FrontendSymbols {

}

abstract class FrontendKlibSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendKlibSymbols, FrontendSymbolsImpl(irBuiltIns) {

}

interface FrontendWebSymbols : FrontendKlibSymbols {

}

abstract class FrontendWebSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendWebSymbols, FrontendKlibSymbolsImpl(irBuiltIns) {

}

interface FrontendJsSymbols : FrontendWebSymbols {}

open class FrontendJsSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendJsSymbols, FrontendWebSymbolsImpl(irBuiltIns) {

}

interface FrontendWasmSymbols : FrontendWebSymbols {}

open class FrontendWasmSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendWasmSymbols, FrontendWebSymbolsImpl(irBuiltIns) {

}

interface FrontendNativeSymbols : FrontendKlibSymbols {}

open class FrontendNativeSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendNativeSymbols, FrontendKlibSymbolsImpl(irBuiltIns) {

}
