/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable

class ExternalDependenciesGenerator(
    private val symbolTable: SymbolTable,
    private val irProviders: List<IrProvider>
) {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun generateUnboundSymbolsAsDependencies() {
        // There should be at most one DeclarationStubGenerator (none in closed world?)
        irProviders.filterIsInstance<DeclarationStubGenerator>().singleOrNull()?.run { unboundSymbolGeneration = true }

        // Deserializing a reference may lead to new unbound references, so we loop until none are left.
        var unbound = emptySet<IrSymbol>()
        do {
            val prevUnbound = unbound
            unbound = symbolTable.descriptorExtension.allUnboundSymbols
            for (symbol in unbound) {
                // Symbol could get bound as a side effect of deserializing other symbols.
                if (!symbol.isBound) {
                    irProviders.firstNotNullOfOrNull { provider -> provider.getDeclaration(symbol) }
                }
            }
            // We wait for the unbound to stabilize on fake overrides.
        } while (unbound != prevUnbound)
    }
}
