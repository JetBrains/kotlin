/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructorCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildReceiverParameter
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.withReplacedConeType

private object TypeAliasConstructorKey : FirDeclarationDataKey()

var FirConstructor.originalConstructorIfTypeAlias: FirConstructor? by FirDeclarationDataRegistry.data(TypeAliasConstructorKey)
val FirConstructorSymbol.isTypeAliasedConstructor: Boolean
    get() = fir.originalConstructorIfTypeAlias != null

private object TypeAliasForConstructorKey : FirDeclarationDataKey()

var FirConstructor.typeAliasForConstructor: FirTypeAliasSymbol? by FirDeclarationDataRegistry.data(TypeAliasForConstructorKey)
val FirConstructorSymbol.typeAliasForConstructor: FirTypeAliasSymbol?
    get() = fir.typeAliasForConstructor

class TypeAliasConstructorsSubstitutingScope(
    private val typeAliasSymbol: FirTypeAliasSymbol,
    private val delegatingScope: FirScope,
    private val outerType: ConeClassLikeType?,
) : FirScope() {

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        delegatingScope.processDeclaredConstructors wrapper@{ originalConstructorSymbol ->
            val typeParameters = typeAliasSymbol.fir.typeParameters

            processor(
                buildConstructorCopy(originalConstructorSymbol.fir) {
                    symbol = FirConstructorSymbol(originalConstructorSymbol.callableId)
                    origin = FirDeclarationOrigin.Synthetic.TypeAliasConstructor

                    this.typeParameters.clear()
                    typeParameters.mapTo(this.typeParameters) { buildConstructedClassTypeParameterRef { symbol = it.symbol } }

                    if (outerType != null) {
                        // If the matched symbol is a type alias, and the expanded type is a nested class, e.g.,
                        //
                        //   class Outer {
                        //     inner class Inner
                        //   }
                        //   typealias OI = Outer.Inner
                        //   fun foo() { Outer().OI() }
                        //
                        // the chances are that `processor` belongs to [ScopeTowerLevel] (to resolve type aliases at top-level), which treats
                        // the explicit receiver (`Outer()`) as an extension receiver, whereas the constructor of the nested class may regard
                        // the same explicit receiver as a dispatch receiver (hence inconsistent receiver).
                        // Here, we add a copy of the nested class constructor, along with the outer type as an extension receiver, so that it
                        // can be seen as if resolving:
                        //
                        //   fun Outer.OI(): OI = ...
                        //
                        //
                        receiverParameter = originalConstructorSymbol.fir.returnTypeRef.withReplacedConeType(outerType).let {
                            buildReceiverParameter {
                                typeRef = it
                            }
                        }
                    }

                }.apply {
                    originalConstructorIfTypeAlias = originalConstructorSymbol.fir
                    typeAliasForConstructor = typeAliasSymbol
                }.symbol
            )
        }
    }
}
