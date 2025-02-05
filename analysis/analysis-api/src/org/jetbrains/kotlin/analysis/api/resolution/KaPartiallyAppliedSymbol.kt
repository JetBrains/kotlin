/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol

/**
 * A callable symbol partially applied with receivers and type arguments. Essentially, this is a call that misses some information. For
 * properties, the missing information is the type of access (read, write, or compound access) to this property. For functions, the missing
 * information is the value arguments for the call.
 */
public interface KaPartiallyAppliedSymbol<out S : KaCallableSymbol, out C : KaCallableSignature<S>> : KaLifetimeOwner {
    /**
     * The function or variable declaration.
     */
    public val signature: C

    /**
     * The [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) for this symbol access. A dispatch
     * receiver is available if the callable is declared inside a class or object.
     */
    public val dispatchReceiver: KaReceiverValue?

    /**
     * The [extension receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) for this symbol access. An
     * extension receiver is available if the callable is declared with an extension receiver.
     */
    public val extensionReceiver: KaReceiverValue?

    /**
     * The list of [context parameters](https://github.com/Kotlin/KEEP/issues/367) for this symbol access.
     * The list is available if the callable is declared with context parameters.
     */
    @KaExperimentalApi
    public val contextArguments: List<KaReceiverValue> get() = emptyList()
}

/**
 * The [callable symbol][KaCallableSymbol] which the [KaPartiallyAppliedSymbol] represents. While the information contained in a partially
 * applied symbol is not exhaustive (e.g. applied functions are missing value arguments), the symbol of the callable which is called is
 * definite.
 */
public val <S : KaCallableSymbol, C : KaCallableSignature<S>> KaPartiallyAppliedSymbol<S, C>.symbol: S get() = signature.symbol

public typealias KaPartiallyAppliedFunctionSymbol<S> = KaPartiallyAppliedSymbol<S, KaFunctionSignature<S>>

public typealias KaPartiallyAppliedVariableSymbol<S> = KaPartiallyAppliedSymbol<S, KaVariableSignature<S>>
