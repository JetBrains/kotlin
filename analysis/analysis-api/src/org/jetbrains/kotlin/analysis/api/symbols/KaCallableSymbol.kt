/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.CallableId


@OptIn(KaExperimentalApi::class)
public sealed class KaCallableSymbol :
    @Suppress("DEPRECATION") org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithKind,
    KaDeclarationSymbol,
    KaContextReceiversOwner {
    /**
     * The callable's [CallableId] if it exists, or `null` otherwise (e.g. when the callable is local).
     */
    public abstract val callableId: CallableId?

    @Deprecated("Use `callableId` instead.", ReplaceWith("callableId"))
    public val callableIdIfNonLocal: CallableId? get() = callableId

    public abstract val returnType: KaType

    public abstract val receiverParameter: KaReceiverParameterSymbol?
    public abstract val isExtension: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaCallableSymbol>
}

@Deprecated("Use 'KaCallableSymbol' instead.", ReplaceWith("KaCallableSymbol"))
public typealias KtCallableSymbol = KaCallableSymbol

public val KaCallableSymbol.receiverType: KaType?
    get() = receiverParameter?.returnType