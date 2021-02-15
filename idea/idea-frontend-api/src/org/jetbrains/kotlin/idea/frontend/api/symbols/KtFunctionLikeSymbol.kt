/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

abstract class KtFunctionLikeSymbol : KtCallableSymbol(), KtSymbolWithKind {
    abstract val valueParameters: List<KtParameterSymbol>

    abstract override fun createPointer(): KtSymbolPointer<KtFunctionLikeSymbol>
}

abstract class KtAnonymousFunctionSymbol : KtFunctionLikeSymbol(), KtPossibleExtensionSymbol {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL

    abstract override fun createPointer(): KtSymbolPointer<KtAnonymousFunctionSymbol>
}

abstract class KtFunctionSymbol : KtFunctionLikeSymbol(),
    KtNamedSymbol,
    KtPossibleExtensionSymbol,
    KtPossibleMemberSymbol,
    KtSymbolWithTypeParameters,
    KtSymbolWithModality<KtCommonSymbolModality>,
    KtSymbolWithVisibility,
    KtAnnotatedSymbol {
    abstract val callableIdIfNonLocal: FqName?

    abstract val isSuspend: Boolean
    abstract val isOperator: Boolean
    abstract val isExternal: Boolean
    abstract val isInline: Boolean
    abstract val isOverride: Boolean

    abstract override val valueParameters: List<KtFunctionParameterSymbol>

    abstract override fun createPointer(): KtSymbolPointer<KtFunctionSymbol>
}

abstract class KtConstructorSymbol : KtFunctionLikeSymbol(),
    KtPossibleMemberSymbol,
    KtAnnotatedSymbol,
    KtSymbolWithVisibility,
    KtSymbolWithTypeParameters {
    abstract val isPrimary: Boolean
    abstract val containingClassIdIfNonLocal: ClassId?

    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.MEMBER

    abstract override val valueParameters: List<KtConstructorParameterSymbol>

    abstract override fun createPointer(): KtSymbolPointer<KtConstructorSymbol>
}