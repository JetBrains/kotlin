/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrTreeSymbolsVisitor
import org.jetbrains.kotlin.ir.util.isExpect
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

class IrNoExpectSymbolsHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    override fun processModule(
        module: TestModule,
        info: IrBackendInput,
    ) {
        val visitor = object : IrTreeSymbolsVisitor() {
            override fun visitSymbol(container: IrElement, symbol: IrSymbol) {
                check(symbol)
            }
        }
        info.irModuleFragment.acceptChildrenVoid(visitor)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    private fun check(symbol: IrSymbol) {
        val owner = symbol.owner as? IrDeclaration ?: return
        if (owner.isExpect) {
            when (owner.origin) {
                IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB,
                IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB -> assertions.fail { "Reference to expect class found: $symbol" }
            }
        }
    }
}

