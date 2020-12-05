/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirObjectImportedCallableScope(
    private val importedClassId: ClassId,
    private val objectUseSiteScope: FirTypeScope
) : FirScope(), FirContainingNamesAwareScope {
    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        objectUseSiteScope.processFunctionsByName(name) wrapper@{ symbol ->
            if (symbol !is FirNamedFunctionSymbol) {
                processor(symbol)
                return@wrapper
            }
            val function = symbol.fir
            val syntheticFunction = buildSimpleFunctionCopy(function) {
                origin = FirDeclarationOrigin.ImportedFromObject
                this.symbol = FirNamedFunctionSymbol(CallableId(importedClassId, name))
            }.apply {
                importedFromObjectData = ImportedFromObjectData(importedClassId, function)
            }
            processor(syntheticFunction.symbol)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        objectUseSiteScope.processPropertiesByName(name) wrapper@{ symbol ->
            if (symbol !is FirPropertySymbol) {
                processor(symbol)
                return@wrapper
            }
            val property = symbol.fir
            val syntheticFunction = buildPropertyCopy(property) {
                origin = FirDeclarationOrigin.ImportedFromObject
                this.symbol = FirPropertySymbol(CallableId(importedClassId, name))
                this.delegateFieldSymbol = null
            }.apply {
                importedFromObjectData = ImportedFromObjectData(importedClassId, property)
            }
            processor(syntheticFunction.symbol)
        }
    }

    override fun getCallableNames(): Set<Name> = objectUseSiteScope.getCallableNames()

    override fun getClassifierNames(): Set<Name> = emptySet()
}

private object ImportedFromObjectClassIdKey : FirDeclarationDataKey()

class ImportedFromObjectData<D : FirCallableDeclaration<*>>(
    val objectClassId: ClassId,
    val original: D,
)

var <D : FirCallableDeclaration<*>>
        D.importedFromObjectData: ImportedFromObjectData<D>? by FirDeclarationDataRegistry.data(ImportedFromObjectClassIdKey)
