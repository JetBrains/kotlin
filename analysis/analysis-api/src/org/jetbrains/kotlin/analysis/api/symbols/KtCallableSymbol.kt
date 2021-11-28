/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.CallableId

public abstract class KtCallableSymbol : KtSymbol, KtSymbolWithKind {
    public abstract val callableIdIfNonLocal: CallableId?
    public abstract val returnType: KtType

    public abstract val receiverType: KtType?
    public abstract val isExtension: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtCallableSymbol>
}

/**
 * Symbol for a receiver parameter of a function or property. For example, consider code `fun String.foo() {...}`, the declaration of
 * `String` receiver parameter is such a symbol.
 */
public abstract class KtReceiverParameterSymbol : KtSymbol {
    public abstract val type: KtType
}