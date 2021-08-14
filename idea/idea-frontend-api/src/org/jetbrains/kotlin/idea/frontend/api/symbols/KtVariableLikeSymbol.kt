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

public sealed class KtVariableLikeSymbol : KtCallableSymbol(), KtNamedSymbol, KtSymbolWithKind {
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
public abstract class KtBackingFieldSymbol : KtVariableLikeSymbol() {
    public abstract val owningProperty: KtKotlinPropertySymbol

    final override val name: Name get() = fieldName
    final override val psi: PsiElement? get() = null
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL
    final override val origin: KtSymbolOrigin get() = KtSymbolOrigin.PROPERTY_BACKING_FIELD
    final override val callableIdIfNonLocal: CallableId? get() = null
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtTypeAndAnnotations? get() = null

    abstract override fun createPointer(): KtSymbolPointer<KtVariableLikeSymbol>

    public companion object {
        private val fieldName = Name.identifier("field")
    }
}


public abstract class KtEnumEntrySymbol : KtVariableLikeSymbol(), KtSymbolWithMembers, KtSymbolWithKind {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtTypeAndAnnotations? get() = null
    public abstract val containingEnumClassIdIfNonLocal: ClassId?


    abstract override fun createPointer(): KtSymbolPointer<KtEnumEntrySymbol>
}


public sealed class KtVariableSymbol : KtVariableLikeSymbol() {
    public abstract val isVal: Boolean
    abstract override fun createPointer(): KtSymbolPointer<KtVariableSymbol>
}

public abstract class KtJavaFieldSymbol :
    KtVariableSymbol(),
    KtSymbolWithModality,
    KtSymbolWithVisibility,
    KtSymbolWithKind {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtTypeAndAnnotations? get() = null
    public abstract val isStatic: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtJavaFieldSymbol>
}

public sealed class KtPropertySymbol : KtVariableSymbol(),
    KtPossibleMemberSymbol,
    KtSymbolWithModality,
    KtSymbolWithVisibility,
    KtAnnotatedSymbol,
    KtSymbolWithKind {

    public abstract val hasGetter: Boolean
    public abstract val hasSetter: Boolean

    public abstract val getter: KtPropertyGetterSymbol?
    public abstract val setter: KtPropertySetterSymbol?

    public abstract val hasBackingField: Boolean

    public abstract val isFromPrimaryConstructor: Boolean
    public abstract val isOverride: Boolean
    public abstract val isStatic: Boolean

    public abstract val initializer: KtConstantValue?

    abstract override fun createPointer(): KtSymbolPointer<KtPropertySymbol>
}

public abstract class KtKotlinPropertySymbol : KtPropertySymbol() {
    public abstract val isLateInit: Boolean

    public abstract val isConst: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtKotlinPropertySymbol>
}

public abstract class KtSyntheticJavaPropertySymbol : KtPropertySymbol() {
    final override val hasBackingField: Boolean get() = true
    final override val hasGetter: Boolean get() = true
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER

    abstract override val getter: KtPropertyGetterSymbol

    public abstract val javaGetterSymbol: KtFunctionSymbol
    public abstract val javaSetterSymbol: KtFunctionSymbol?

    abstract override fun createPointer(): KtSymbolPointer<KtSyntheticJavaPropertySymbol>
}

public abstract class KtLocalVariableSymbol : KtVariableSymbol(), KtSymbolWithKind {
    final override val callableIdIfNonLocal: CallableId? get() = null
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtTypeAndAnnotations? get() = null

    abstract override fun createPointer(): KtSymbolPointer<KtLocalVariableSymbol>
}

public abstract class KtValueParameterSymbol : KtVariableLikeSymbol(), KtSymbolWithKind, KtAnnotatedSymbol {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL
    final override val callableIdIfNonLocal: CallableId? get() = null
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtTypeAndAnnotations? get() = null

    public abstract val hasDefaultValue: Boolean
    public abstract val isVararg: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol>
}
