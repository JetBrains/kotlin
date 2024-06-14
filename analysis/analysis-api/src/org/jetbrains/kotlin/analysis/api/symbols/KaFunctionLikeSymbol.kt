/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

public sealed class KaFunctionLikeSymbol : KaCallableSymbol(), @Suppress("DEPRECATION") KaSymbolWithKind {
    public abstract val valueParameters: List<KaValueParameterSymbol>

    /**
     * Kotlin functions always have stable parameter names that can be reliably used when calling them with named arguments.
     * Functions loaded from platform definitions (e.g. Java binaries or JS) may have unstable parameter names that vary from
     * one platform installation to another. These names can not be used reliably for calls with named arguments.
     */
    public abstract val hasStableParameterNames: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaFunctionLikeSymbol>
}

@Deprecated("Use 'KaFunctionLikeSymbol' instead", ReplaceWith("KaFunctionLikeSymbol"))
public typealias KtFunctionLikeSymbol = KaFunctionLikeSymbol

public abstract class KaAnonymousFunctionSymbol : KaFunctionLikeSymbol() {
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.LOCAL }
    final override val callableId: CallableId? get() = withValidityAssertion { null }

    final override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    abstract override fun createPointer(): KaSymbolPointer<KaAnonymousFunctionSymbol>
}

@Deprecated("Use 'KaAnonymousFunctionSymbol' instead", ReplaceWith("KaAnonymousFunctionSymbol"))
public typealias KtAnonymousFunctionSymbol = KaAnonymousFunctionSymbol

public abstract class KaSamConstructorSymbol : KaFunctionLikeSymbol(), KaNamedSymbol {
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.TOP_LEVEL }

    abstract override fun createPointer(): KaSymbolPointer<KaSamConstructorSymbol>
}

@Deprecated("Use 'KaSamConstructorSymbol' instead", ReplaceWith("KaSamConstructorSymbol"))
public typealias KtSamConstructorSymbol = KaSamConstructorSymbol

public abstract class KaFunctionSymbol : KaFunctionLikeSymbol(),
    KaNamedSymbol,
    KaPossibleMemberSymbol,
    KaPossibleMultiplatformSymbol,
    KaSymbolWithModality,
    KaSymbolWithVisibility {

    public abstract val isSuspend: Boolean
    public abstract val isOperator: Boolean
    public abstract val isExternal: Boolean
    public abstract val isInline: Boolean
    public abstract val isOverride: Boolean
    public abstract val isInfix: Boolean
    public abstract val isStatic: Boolean

    /**
     * This variable represents whether a function symbol is tail-recursive or not.
     *
     * @return true if the function is tail-recursive, false otherwise
     */
    public abstract val isTailRec: Boolean
    public abstract val contractEffects: List<KaContractEffectDeclaration>

    /**
     * Whether this symbol is the `invoke` method defined on the Kotlin builtin functional type.
     */
    public abstract val isBuiltinFunctionInvoke: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaFunctionSymbol>
}

@Deprecated("Use 'KaFunctionSymbol' instead", ReplaceWith("KaFunctionSymbol"))
public typealias KtFunctionSymbol = KaFunctionSymbol

public abstract class KaConstructorSymbol : KaFunctionLikeSymbol(),
    KaPossibleMemberSymbol,
    KaPossibleMultiplatformSymbol,
    KaSymbolWithVisibility {

    public abstract val isPrimary: Boolean

    /**
     * The [ClassId] of the containing class, or `null` if the class is local.
     */
    public abstract val containingClassId: ClassId?

    @Deprecated("Use `containingClassId` instead.", ReplaceWith("containingClassId"))
    public val containingClassIdIfNonLocal: ClassId? get() = containingClassId

    final override val callableId: CallableId? get() = withValidityAssertion { null }
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.CLASS }
    final override val isExtension: Boolean get() = withValidityAssertion { false }
    final override val receiverParameter: KaReceiverParameterSymbol? get() = withValidityAssertion { null }
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }

    abstract override fun createPointer(): KaSymbolPointer<KaConstructorSymbol>
}

@Deprecated("Use 'KaConstructorSymbol' instead", ReplaceWith("KaConstructorSymbol"))
public typealias KtConstructorSymbol = KaConstructorSymbol