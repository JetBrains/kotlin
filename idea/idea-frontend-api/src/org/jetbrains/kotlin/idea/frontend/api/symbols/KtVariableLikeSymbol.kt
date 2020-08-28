/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

sealed class KtVariableLikeSymbol : KtCallableSymbol(), KtTypedSymbol, KtNamedSymbol, KtSymbolWithKind {
    abstract override fun createPointer(): KtSymbolPointer<KtVariableLikeSymbol>
}

abstract class KtEnumEntrySymbol : KtVariableLikeSymbol(), KtSymbolWithKind {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER
    abstract val containingEnumClassIdIfNonLocal: ClassId?

    abstract override fun createPointer(): KtSymbolPointer<KtEnumEntrySymbol>
}

sealed class KtParameterSymbol : KtVariableLikeSymbol() {
    abstract val hasDefaultValue: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtParameterSymbol>
}

sealed class KtVariableSymbol : KtVariableLikeSymbol() {
    abstract val isVal: Boolean
    abstract override fun createPointer(): KtSymbolPointer<KtVariableSymbol>
}

abstract class KtJavaFieldSymbol : KtVariableSymbol(), KtSymbolWithModality<KtCommonSymbolModality>, KtSymbolWithKind {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER

    abstract val callableIdIfNonLocal: FqName?

    abstract override fun createPointer(): KtSymbolPointer<KtJavaFieldSymbol>
}

// TODO getters & setters
abstract class KtPropertySymbol : KtVariableSymbol(),
    KtPossibleExtensionSymbol,
    KtSymbolWithModality<KtCommonSymbolModality>,
    KtSymbolWithKind {

    abstract val callableIdIfNonLocal: FqName?

    abstract override fun createPointer(): KtSymbolPointer<KtPropertySymbol>
}

abstract class KtLocalVariableSymbol : KtVariableSymbol(), KtSymbolWithKind {
    abstract override fun createPointer(): KtSymbolPointer<KtLocalVariableSymbol>
}

abstract class KtFunctionParameterSymbol : KtParameterSymbol(), KtSymbolWithKind {
    abstract val isVararg: Boolean

    override val symbolKind: KtSymbolKind get() = KtSymbolKind.NON_PROPERTY_PARAMETER

    abstract override fun createPointer(): KtSymbolPointer<KtFunctionParameterSymbol>
}

abstract class KtConstructorParameterSymbol : KtParameterSymbol(), KtNamedSymbol, KtSymbolWithKind {
    abstract val constructorParameterKind: KtConstructorParameterSymbolKind

    abstract override fun createPointer(): KtSymbolPointer<KtConstructorParameterSymbol>
}

enum class KtConstructorParameterSymbolKind {
    VAL_PROPERTY, VAR_PROPERTY, NON_PROPERTY
}