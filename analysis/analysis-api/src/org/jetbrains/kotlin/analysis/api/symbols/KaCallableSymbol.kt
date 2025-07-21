/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
@OptIn(KaExperimentalApi::class, KaImplementationDetail::class)
public sealed class KaCallableSymbol : KaDeclarationSymbol, KaContextReceiversOwner {
    /**
     * The callable's [CallableId] if it exists, or `null` if the declaration is local.
     */
    public abstract val callableId: CallableId?

    /**
     * The callable's return type. For variables, [returnType] is the type of the variable.
     *
     * Note: For a `vararg foo: T` parameter, the resulting type is the vararg element `T` type (unlike
     * [KtDeclaration.returnType][org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider.returnType] from
     * [KaExpressionTypeProvider][org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider.returnType],
     * which returns the full `Array<out T>` type).
     *
     * The reasoning behind this is that [KaCallableSymbol.returnType] sees the parameter from the declaration's semantic perspective,
     * representing the signature of the parameter, which contains just the element type. In this paradigm, `vararg` arrays are
     * constructed separately under the hood.
     *
     * At the same time [KtDeclaration.returnType][org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider.returnType] from
     * [KaExpressionTypeProvider][org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider.returnType] represents a
     * use-site perspective, which has to desugar `vararg` parameters because they are consumed as array types.
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