/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaContextParameterOwnerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.CallableId

/**
 * [KaCallableSymbol] represents callable declarations, such as functions and variables.
 */
@OptIn(KaExperimentalApi::class)
public sealed class KaCallableSymbol : KaDeclarationSymbol, KaContextReceiversOwner {
    /**
     * The callable's [CallableId] if it exists, or `null` if the declaration is local.
     */
    public abstract val callableId: CallableId?

    /**
     * The callable's return type. For variables, [returnType] is the type of the variable.
     */
    public abstract val returnType: KaType

    /**
     * The [receiver parameter][KaReceiverParameterSymbol] of the callable, or `null` if the callable is not an extension.
     */
    public abstract val receiverParameter: KaReceiverParameterSymbol?

    /**
     * Whether the callable is an [extension function or property](https://kotlinlang.org/docs/extensions.html).
     */
    public abstract val isExtension: Boolean

    abstract override fun createPointer(): KaSymbolPointer<KaCallableSymbol>
}

/**
 * The [receiver parameter][KaCallableSymbol.receiverParameter]'s type, or `null` if the callable is not an extension.
 */
public val KaCallableSymbol.receiverType: KaType?
    get() = receiverParameter?.returnType

/**
 * @return a list of [KaContextParameterSymbol]s directly declared in the symbol.
 */
@KaExperimentalApi
public val KaCallableSymbol.contextParameters: List<KaContextParameterSymbol>
    @OptIn(KaImplementationDetail::class)
    get() = (this as? KaContextParameterOwnerSymbol)?.contextParameters.orEmpty()