/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class KtVariableLikeSymbol : KtCallableSymbol(), KtNamedSymbol, KtSymbolWithKind {
    abstract override fun createPointer(): KtSymbolPointer<KtVariableLikeSymbol>
}

abstract class KtEnumEntrySymbol : KtVariableLikeSymbol(), KtSymbolWithMembers, KtSymbolWithKind {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER
    abstract val containingEnumClassIdIfNonLocal: ClassId?

    abstract override fun createPointer(): KtSymbolPointer<KtEnumEntrySymbol>
}


sealed class KtVariableSymbol : KtVariableLikeSymbol() {
    abstract val isVal: Boolean
    abstract override fun createPointer(): KtSymbolPointer<KtVariableSymbol>
}

abstract class KtJavaFieldSymbol :
    KtVariableSymbol(),
    KtSymbolWithModality<KtCommonSymbolModality>,
    KtSymbolWithVisibility,
    KtSymbolWithKind {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER

    abstract val callableIdIfNonLocal: FqName?

    abstract override fun createPointer(): KtSymbolPointer<KtJavaFieldSymbol>
}

sealed class KtPropertySymbol : KtVariableSymbol(),
    KtPossibleExtensionSymbol,
    KtPossibleMemberSymbol,
    KtSymbolWithModality<KtCommonSymbolModality>,
    KtSymbolWithVisibility,
    KtAnnotatedSymbol,
    KtSymbolWithKind {

    abstract val hasGetter: Boolean
    abstract val hasSetter: Boolean

    abstract val getter: KtPropertyGetterSymbol?
    abstract val setter: KtPropertySetterSymbol?

    abstract val callableIdIfNonLocal: FqName?

    abstract val hasBackingField: Boolean

    abstract val isOverride: Boolean

    abstract val initializer: KtConstantValue?

    abstract override fun createPointer(): KtSymbolPointer<KtPropertySymbol>
}

abstract class KtKotlinPropertySymbol : KtPropertySymbol() {
    abstract val isLateInit: Boolean

    abstract val isConst: Boolean
}

abstract class KtSyntheticJavaPropertySymbol : KtPropertySymbol() {
    final override val hasBackingField: Boolean get() = true
    final override val hasGetter: Boolean get() = true
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER

    abstract override val getter: KtPropertyGetterSymbol

    abstract val javaGetterName: Name
    abstract val javaSetterName: Name?
}

abstract class KtLocalVariableSymbol : KtVariableSymbol(), KtSymbolWithKind {
    abstract override fun createPointer(): KtSymbolPointer<KtLocalVariableSymbol>
}

sealed class KtParameterSymbol : KtVariableLikeSymbol(), KtSymbolWithKind, KtAnnotatedSymbol {
    abstract val hasDefaultValue: Boolean

    abstract val isVararg: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtParameterSymbol>
}

abstract class KtFunctionParameterSymbol : KtParameterSymbol() {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.NON_PROPERTY_PARAMETER

    abstract override fun createPointer(): KtSymbolPointer<KtFunctionParameterSymbol>
}

abstract class KtConstructorParameterSymbol : KtParameterSymbol() {
    abstract val constructorParameterKind: KtConstructorParameterSymbolKind

    abstract override fun createPointer(): KtSymbolPointer<KtConstructorParameterSymbol>
}


enum class KtConstructorParameterSymbolKind {
    VAL_PROPERTY, VAR_PROPERTY, NON_PROPERTY
}