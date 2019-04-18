/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NEXT
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.STOP
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConeVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirClassDeclaredMemberScope(private val klass: FirRegularClass) : FirScope {
    override fun processFunctionsByName(name: Name, processor: (ConeFunctionSymbol) -> ProcessorAction) =
        processCallables(name, processor)

    override fun processPropertiesByName(name: Name, processor: (ConeVariableSymbol) -> ProcessorAction) =
        processCallables(name, processor)

    private inline fun <reified T : ConeCallableSymbol> processCallables(
        name: Name,
        processor: (T) -> ProcessorAction
    ): ProcessorAction {
        for (declaration in klass.declarations) {
            if (declaration !is FirCallableMemberDeclaration) continue

            val symbol = declaration.symbol as? T ?: continue
            if (symbol.callableId.callableName != name) continue

            if (processor(symbol) == STOP) return STOP
        }

        return NEXT
    }
}
