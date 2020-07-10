/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

sealed class KtFunctionLikeSymbol : KtTypedSymbol, KtSymbolWithKind {
    abstract val valueParameters: List<KtParameterSymbol>
}

abstract class KtAnonymousFunctionSymbol : KtFunctionLikeSymbol() {
    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.LOCAL
}

abstract class KtFunctionSymbol : KtFunctionLikeSymbol(),
    KtNamedSymbol,
    KtPossibleExtensionSymbol,
    KtSymbolWithTypeParameters,
    KtSymbolWithModality<KtCommonSymbolModality> {

    abstract val isSuspend: Boolean
    abstract val isOperator: Boolean
    abstract val fqName: FqName?

    abstract override val valueParameters: List<KtFunctionParameterSymbol>
}

abstract class KtConstructorSymbol : KtFunctionLikeSymbol() {
    abstract override val valueParameters: List<KtConstructorParameterSymbol>
    abstract val isPrimary: Boolean
    abstract val owner: KtClassOrObjectSymbol
    abstract val ownerClassId: ClassId

    abstract override fun createPointer(): KtSymbolPointer<KtConstructorSymbol>
}