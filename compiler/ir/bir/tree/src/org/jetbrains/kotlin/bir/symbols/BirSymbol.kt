/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.symbols

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.BirReturnableBlock
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

interface BirSymbol {
    val signature: IdSignature?
}


interface BirUntypedPossiblyElementSymbol : BirSymbol
interface BirPossiblyElementSymbol<out E : BirElement> : BirUntypedPossiblyElementSymbol

inline val <reified E : BirElement> BirPossiblyElementSymbol<E>.maybeAsElement: E?
    get() = this as? E

inline val <reified E : BirElement> BirPossiblyElementSymbol<E>.asElement: E
    get() = this as E


interface BirPackageFragmentSymbol : BirSymbol

interface BirFileSymbol : BirPackageFragmentSymbol, BirPossiblyElementSymbol<BirFile>

interface BirExternalPackageFragmentSymbol : BirPackageFragmentSymbol, BirPossiblyElementSymbol<BirExternalPackageFragment>

interface BirAnonymousInitializerSymbol : BirPossiblyElementSymbol<BirAnonymousInitializer>

interface BirEnumEntrySymbol : BirPossiblyElementSymbol<BirEnumEntry>

interface BirFieldSymbol : BirPossiblyElementSymbol<BirField>

interface BirClassifierSymbol : BirSymbol, TypeConstructorMarker

interface BirClassSymbol : BirClassifierSymbol, BirPossiblyElementSymbol<BirClass>

interface BirScriptSymbol : BirClassifierSymbol, BirPossiblyElementSymbol<BirScript>

interface BirTypeParameterSymbol : BirClassifierSymbol, BirPossiblyElementSymbol<BirTypeParameter>, TypeParameterMarker

interface BirValueSymbol : BirSymbol

interface BirValueParameterSymbol : BirValueSymbol, BirPossiblyElementSymbol<BirValueParameter>

interface BirVariableSymbol : BirValueSymbol, BirPossiblyElementSymbol<BirVariable>

interface BirReturnTargetSymbol : BirSymbol

interface BirFunctionSymbol : BirReturnTargetSymbol //todo: , BirPossiblyElementSymbol<BirFunction>

interface BirConstructorSymbol : BirFunctionSymbol, BirPossiblyElementSymbol<BirConstructor>

interface BirSimpleFunctionSymbol : BirFunctionSymbol, BirPossiblyElementSymbol<BirSimpleFunction>

interface BirReturnableBlockSymbol : BirReturnTargetSymbol, BirPossiblyElementSymbol<BirReturnableBlock>

interface BirPropertySymbol : BirPossiblyElementSymbol<BirProperty>

interface BirLocalDelegatedPropertySymbol : BirPossiblyElementSymbol<BirLocalDelegatedProperty>

interface BirTypeAliasSymbol : BirPossiblyElementSymbol<BirTypeAlias>