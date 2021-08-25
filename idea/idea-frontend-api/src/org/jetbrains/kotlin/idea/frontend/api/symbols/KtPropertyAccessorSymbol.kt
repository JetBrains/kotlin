/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer

public sealed class KtPropertyAccessorSymbol : KtFunctionLikeSymbol(),
    KtPossibleMemberSymbol,
    KtSymbolWithModality,
    KtSymbolWithVisibility,
    KtAnnotatedSymbol,
    KtSymbolWithKind {

    final override val isExtension: Boolean get() = false

    public abstract val isDefault: Boolean
    public abstract val isInline: Boolean
    public abstract val isOverride: Boolean
    public abstract val hasBody: Boolean

    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.ACCESSOR

    abstract override fun createPointer(): KtSymbolPointer<KtPropertyAccessorSymbol>
}

public abstract class KtPropertyGetterSymbol : KtPropertyAccessorSymbol() {
    abstract override fun createPointer(): KtSymbolPointer<KtPropertyGetterSymbol>
}

public abstract class KtPropertySetterSymbol : KtPropertyAccessorSymbol() {
    public abstract val parameter: KtValueParameterSymbol

    abstract override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol>
}
