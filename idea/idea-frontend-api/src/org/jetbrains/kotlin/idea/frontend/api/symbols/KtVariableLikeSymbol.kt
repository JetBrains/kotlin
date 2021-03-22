/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class KtVariableLikeSymbol : KtCallableSymbol(), KtNamedSymbol, KtSymbolWithKind {
    abstract override fun createPointer(): KtSymbolPointer<KtVariableLikeSymbol>
}

/**
 * Backing field of some member property
 *
 * E.g,
 * ```
 * val x: Int = 10
 *    get() = field<caret>
 * ```
 *
 * Symbol at caret will be resolved to a [KtBackingFieldSymbol]
 */
abstract class KtBackingFieldSymbol : KtVariableLikeSymbol() {
    abstract val owningProperty: KtKotlinPropertySymbol

    final override val name: Name get() = fieldName
    final override val psi: PsiElement? get() = null
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL
    final override val origin: KtSymbolOrigin get() = KtSymbolOrigin.PROPERTY_BACKING_FIELD
    final override val callableIdIfNonLocal: CallableId? get() = null

    abstract override fun createPointer(): KtSymbolPointer<KtVariableLikeSymbol>

    companion object {
        private val fieldName = Name.identifier("field")
    }
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

    abstract val hasBackingField: Boolean

    abstract val isOverride: Boolean

    abstract val initializer: KtConstantValue?

    abstract override fun createPointer(): KtSymbolPointer<KtPropertySymbol>
}

abstract class KtKotlinPropertySymbol : KtPropertySymbol() {
    abstract val isLateInit: Boolean

    abstract val isConst: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtKotlinPropertySymbol>
}

abstract class KtSyntheticJavaPropertySymbol : KtPropertySymbol() {
    final override val hasBackingField: Boolean get() = true
    final override val hasGetter: Boolean get() = true
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER

    abstract override val getter: KtPropertyGetterSymbol

    abstract val javaGetterName: Name
    abstract val javaSetterName: Name?

    abstract override fun createPointer(): KtSymbolPointer<KtSyntheticJavaPropertySymbol>
}

abstract class KtLocalVariableSymbol : KtVariableSymbol(), KtSymbolWithKind {
    final override val callableIdIfNonLocal: CallableId? get() = null

    abstract override fun createPointer(): KtSymbolPointer<KtLocalVariableSymbol>
}

abstract class KtValueParameterSymbol : KtVariableLikeSymbol(), KtSymbolWithKind, KtAnnotatedSymbol {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL
    final override val callableIdIfNonLocal: CallableId? get() = null

    abstract val hasDefaultValue: Boolean
    abstract val isVararg: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol>
}