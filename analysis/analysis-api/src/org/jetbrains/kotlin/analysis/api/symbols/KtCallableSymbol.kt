/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.base.KaContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaPossibleMemberSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.CallableId

public sealed class KaCallableSymbol : KaSymbolWithKind, KaPossibleMemberSymbol, KaDeclarationSymbol, KaContextReceiversOwner {
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

public typealias KtCallableSymbol = KaCallableSymbol

public val KaCallableSymbol.receiverType: KaType?
    get() = receiverParameter?.type

/**
 * Symbol for a receiver parameter of a function or property. For example, consider code `fun String.foo() {...}`, the declaration of
 * `String` receiver parameter is such a symbol.
 */
public abstract class KaReceiverParameterSymbol : KaAnnotatedSymbol, KaParameterSymbol {
    public abstract val type: KaType

    /**
     * Link to the corresponding function or property.
     * In terms of the example above -- this is link to the function foo.
     */
    public abstract val owningCallableSymbol: KaCallableSymbol

    abstract override fun createPointer(): KaSymbolPointer<KaReceiverParameterSymbol>
}

public typealias KtReceiverParameterSymbol = KaReceiverParameterSymbol