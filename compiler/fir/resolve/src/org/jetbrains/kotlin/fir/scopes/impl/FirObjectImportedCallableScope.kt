/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirObjectImportedCallableScope(
    private val importedClassId: ClassId,
    private val objectUseSiteScope: FirScope
) : FirScope() {
    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        objectUseSiteScope.processFunctionsByName(name) wrapper@{ symbol ->
            if (symbol !is FirNamedFunctionSymbol) {
                processor(symbol)
                return@wrapper
            }
            val function = symbol.fir
            val syntheticFunction = buildSimpleFunction {
                source = function.source
                session = function.session
                origin = FirDeclarationOrigin.ImportedFromObject
                returnTypeRef = function.returnTypeRef
                receiverTypeRef = function.receiverTypeRef
                this.name = function.name
                status = function.status
                this.symbol = FirNamedFunctionSymbol(CallableId(importedClassId, name), overriddenSymbol = symbol)
                resolvePhase = function.resolvePhase
                typeParameters.addAll(function.typeParameters)
                valueParameters.addAll(function.valueParameters)
                annotations.addAll(function.annotations)
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
            val syntheticFunction = buildProperty {
                source = property.source
                session = property.session
                origin = FirDeclarationOrigin.ImportedFromObject
                returnTypeRef = property.returnTypeRef
                receiverTypeRef = property.receiverTypeRef
                this.name = property.name
                status = property.status
                isVar = property.isVar
                isLocal = property.isLocal
                this.symbol = FirPropertySymbol(CallableId(importedClassId, name), overriddenSymbol = symbol)
                resolvePhase = property.resolvePhase
                typeParameters.addAll(property.typeParameters)
                annotations.addAll(property.annotations)
            }
            processor(syntheticFunction.symbol)
        }
    }
}