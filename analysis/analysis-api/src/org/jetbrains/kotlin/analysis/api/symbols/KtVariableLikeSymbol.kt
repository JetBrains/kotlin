/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression

public sealed class KtVariableLikeSymbol : KtCallableSymbol(), KtNamedSymbol, KtSymbolWithKind, KtPossibleMemberSymbol {
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
    override val origin: KtSymbolOrigin get() = KtSymbolOrigin.PROPERTY_BACKING_FIELD
    final override val callableIdIfNonLocal: CallableId? get() = null
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtType? get() = null

    abstract override fun createPointer(): KtSymbolPointer<KtVariableLikeSymbol>

    public companion object {
        private val fieldName = StandardNames.BACKING_FIELD
    }
}


public abstract class KtEnumEntrySymbol : KtVariableLikeSymbol(), KtSymbolWithMembers, KtSymbolWithKind {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.CLASS_MEMBER
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtType? get() = null

    //todo reduntant, remove
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
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.CLASS_MEMBER
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtType? get() = null
    public abstract val isStatic: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtJavaFieldSymbol>
}

public sealed class KtPropertySymbol : KtVariableSymbol(),
    KtPossibleMemberSymbol,
    KtSymbolWithTypeParameters,
    KtSymbolWithModality,
    KtSymbolWithVisibility,
    KtAnnotatedSymbol,
    KtSymbolWithKind {

    public abstract val hasGetter: Boolean
    public abstract val hasSetter: Boolean

    public abstract val getter: KtPropertyGetterSymbol?
    public abstract val setter: KtPropertySetterSymbol?

    public abstract val hasBackingField: Boolean

    public abstract val isDelegatedProperty: Boolean
    public abstract val isFromPrimaryConstructor: Boolean
    public abstract val isOverride: Boolean
    public abstract val isStatic: Boolean

    /**
     * Value which is provided for as property initializer.
     *
     * Possible values:
     * - `null` - no initializer was provided
     * - [KtConstantInitializerValue] - initializer value was provided, and it is a compile-time constant
     * - [KtNonConstantInitializerValue] - initializer value was provided, and it is not a compile-time constant. In case of declaration from source it would include correponding [KtExpression]
     *
     */
    public abstract val initializer: KtInitializerValue?

    abstract override fun createPointer(): KtSymbolPointer<KtPropertySymbol>
}

public abstract class KtKotlinPropertySymbol : KtPropertySymbol() {
    public abstract val isLateInit: Boolean

    public abstract val isConst: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtKotlinPropertySymbol>
}

public abstract class KtSyntheticJavaPropertySymbol : KtPropertySymbol() {
    final override val hasBackingField: Boolean get() = true
    final override val isDelegatedProperty: Boolean get() = false
    final override val hasGetter: Boolean get() = true
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.CLASS_MEMBER

    abstract override val getter: KtPropertyGetterSymbol

    public abstract val javaGetterSymbol: KtFunctionSymbol
    public abstract val javaSetterSymbol: KtFunctionSymbol?

    abstract override fun createPointer(): KtSymbolPointer<KtSyntheticJavaPropertySymbol>
}

public abstract class KtLocalVariableSymbol : KtVariableSymbol(), KtSymbolWithKind {
    final override val callableIdIfNonLocal: CallableId? get() = null
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtType? get() = null

    abstract override fun createPointer(): KtSymbolPointer<KtLocalVariableSymbol>
}

public abstract class KtValueParameterSymbol : KtVariableLikeSymbol(), KtSymbolWithKind, KtAnnotatedSymbol {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL
    final override val callableIdIfNonLocal: CallableId? get() = null
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtType? get() = null

    public abstract val hasDefaultValue: Boolean
    public abstract val isVararg: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol>

    /**
     * The name of the value parameter. For a parameter of `FunctionN.invoke()` functions, the name is taken from the function type
     * notation, if a name is present. For example:
     * ```
     * fun foo(x: (item: Int, String) -> Unit) =
     *   x(1, "") // or `x.invoke(1, "")`
     * ```
     * The names of the value parameters for `invoke()` are "item" and "p2" (its default parameter name).
     */
    abstract override val name: Name
}
