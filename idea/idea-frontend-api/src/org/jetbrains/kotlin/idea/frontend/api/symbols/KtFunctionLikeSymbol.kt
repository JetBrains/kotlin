/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

public abstract class KtFunctionLikeSymbol : KtCallableSymbol(), KtSymbolWithKind {
    public abstract val valueParameters: List<KtValueParameterSymbol>

    abstract override fun createPointer(): KtSymbolPointer<KtFunctionLikeSymbol>
}

public abstract class KtAnonymousFunctionSymbol : KtFunctionLikeSymbol() {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL
    final override val callableIdIfNonLocal: CallableId? get() = null

    abstract override fun createPointer(): KtSymbolPointer<KtAnonymousFunctionSymbol>
}

public abstract class KtFunctionSymbol : KtFunctionLikeSymbol(),
    KtNamedSymbol,
    KtPossibleMemberSymbol,
    KtSymbolWithTypeParameters,
    KtSymbolWithModality,
    KtSymbolWithVisibility,
    KtAnnotatedSymbol {

    public abstract val isSuspend: Boolean
    public abstract val isOperator: Boolean
    public abstract val isExternal: Boolean
    public abstract val isInline: Boolean
    public abstract val isOverride: Boolean
    public abstract val isInfix: Boolean
    public abstract val isStatic: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtFunctionSymbol>
}

public abstract class KtConstructorSymbol : KtFunctionLikeSymbol(),
    KtPossibleMemberSymbol,
    KtAnnotatedSymbol,
    KtSymbolWithVisibility,
    KtSymbolWithTypeParameters {
    public abstract val isPrimary: Boolean
    public abstract val containingClassIdIfNonLocal: ClassId?

    final override val callableIdIfNonLocal: CallableId? get() = null
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtTypeAndAnnotations? get() = null

    abstract override fun createPointer(): KtSymbolPointer<KtConstructorSymbol>
}