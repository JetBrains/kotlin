/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.symbols

import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirReturnableBlock
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

interface BirSymbol {
    val owner: BirSymbolOwner
    val isBound: Boolean
    val signature: IdSignature?
}

interface BirTypedSymbol<out E : BirSymbolOwner> : BirSymbol {
    override val owner: E
}

val BirSymbol.ownerIfBound: BirSymbolOwner?
    get() = if (isBound) owner else null

val <E : BirSymbolOwner> BirTypedSymbol<E>.ownerIfBound: E?
    get() = if (isBound) owner else null

interface BirPackageFragmentSymbol : BirSymbol

interface BirFileSymbol : BirPackageFragmentSymbol, BirTypedSymbol<BirFile>

interface BirExternalPackageFragmentSymbol : BirPackageFragmentSymbol, BirTypedSymbol<BirExternalPackageFragment>

interface BirAnonymousInitializerSymbol : BirTypedSymbol<BirAnonymousInitializer>

interface BirEnumEntrySymbol : BirTypedSymbol<BirEnumEntry>

interface BirFieldSymbol : BirTypedSymbol<BirField>

interface BirClassifierSymbol : BirSymbol, TypeConstructorMarker

interface BirClassSymbol : BirClassifierSymbol, BirTypedSymbol<BirClass>

interface BirScriptSymbol : BirClassifierSymbol, BirTypedSymbol<BirScript>

interface BirTypeParameterSymbol : BirClassifierSymbol, BirTypedSymbol<BirTypeParameter>, TypeParameterMarker

interface BirValueSymbol : BirSymbol {
    override val owner: BirValueDeclaration
}

interface BirValueParameterSymbol : BirValueSymbol, BirTypedSymbol<BirValueParameter>

interface BirVariableSymbol : BirValueSymbol, BirTypedSymbol<BirVariable>

interface BirReturnTargetSymbol : BirSymbol

interface BirFunctionSymbol : BirReturnTargetSymbol {
    override val owner: BirFunction
}

interface BirConstructorSymbol : BirFunctionSymbol, BirTypedSymbol<BirConstructor>

interface BirSimpleFunctionSymbol : BirFunctionSymbol, BirTypedSymbol<BirSimpleFunction>

interface BirReturnableBlockSymbol : BirReturnTargetSymbol, BirTypedSymbol<BirReturnableBlock>

interface BirPropertySymbol : BirTypedSymbol<BirProperty>

interface BirLocalDelegatedPropertySymbol : BirTypedSymbol<BirLocalDelegatedProperty>

interface BirTypeAliasSymbol : BirTypedSymbol<BirTypeAlias>