/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.symbols

import org.jetbrains.kotlin.bir.BirElementBase
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirReturnableBlock
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

interface BirSymbol<out E : BirSymbolOwner> {
    val isBound: Boolean
    val signature: IdSignature?
}

val <E : BirSymbolOwner> BirSymbol<E>.owner: E
    get() {
        return if (this is BirElementBase) {
            this as E
        } else {
            (this as BirSymbolWithOwner<E>).owner
        }
    }

val <E : BirSymbolOwner> BirSymbol<E>.ownerIfBound: E?
    get() = if (isBound) owner else null

interface BirSymbolWithOwner<out E : BirSymbolOwner> : BirSymbol<E> {
    val owner: E
}

interface BirPackageFragmentSymbol : BirSymbol<BirPackageFragment>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirFileSymbol : BirPackageFragmentSymbol, BirSymbol<BirFile>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirExternalPackageFragmentSymbol : BirPackageFragmentSymbol,
    BirSymbol<BirExternalPackageFragment>

interface BirAnonymousInitializerSymbol : BirSymbol<BirAnonymousInitializer>

interface BirEnumEntrySymbol : BirSymbol<BirEnumEntry>

interface BirFieldSymbol : BirSymbol<BirField>

interface BirClassifierSymbol : BirSymbol<BirDeclaration>, TypeConstructorMarker

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirClassSymbol : BirClassifierSymbol, BirSymbol<BirClass>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirScriptSymbol : BirClassifierSymbol, BirSymbol<BirScript>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirTypeParameterSymbol : BirClassifierSymbol, BirSymbol<BirTypeParameter>, TypeParameterMarker

interface BirValueSymbol : BirSymbol<BirValueDeclaration>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirValueParameterSymbol : BirValueSymbol, BirSymbol<BirValueParameter>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirVariableSymbol : BirValueSymbol, BirSymbol<BirVariable>

interface BirReturnTargetSymbol : BirSymbol<BirReturnTarget>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirFunctionSymbol : BirReturnTargetSymbol, BirSymbol<BirFunction>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirConstructorSymbol : BirFunctionSymbol, BirSymbol<BirConstructor>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirSimpleFunctionSymbol : BirFunctionSymbol, BirSymbol<BirSimpleFunction>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface BirReturnableBlockSymbol : BirReturnTargetSymbol,
    BirSymbol<BirReturnableBlock>

interface BirPropertySymbol : BirSymbol<BirProperty>

interface BirLocalDelegatedPropertySymbol : BirSymbol<BirLocalDelegatedProperty>

interface BirTypeAliasSymbol : BirSymbol<BirTypeAlias>


interface AirSymbol<out E : BirSymbolOwner> {
    val owner: E
}

//interface AirReturnTargetSymbol<out E : BirReturnTarget> : AirSymbol<E>
interface AirFunctionSymbol<out E : BirFunction> : AirSymbol<E> //AirReturnTargetSymbol<E>
interface AirConstructorSymbol : AirFunctionSymbol<BirConstructor>
interface AirSimpleFunctionSymbol : AirFunctionSymbol<BirSimpleFunction>

//interface AirReturnTarget : AirReturnTargetSymbol<BirReturnTarget>
//interface AirFunction : AirFunctionSymbol<BirFunction>
//interface AirSimpleFunction : AirFunction, AirSimpleFunctionSymbol

interface AirFunction : AirSymbol<BirFunction>

@Suppress("INCONSISTENT_TYPE_PARAMETER_VALUES")
interface AirSimpleFunction : AirFunction, AirSymbol<BirSimpleFunction>