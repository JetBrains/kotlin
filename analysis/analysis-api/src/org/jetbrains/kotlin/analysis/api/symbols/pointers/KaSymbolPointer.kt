/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.utils.relfection.renderAsDataClassToString

/**
 * [KaSymbolPointer] allows to point to a [KaSymbol] and later retrieve it in another [KaSession]. A pointer is necessary because
 * [KaSymbol]s cannot be shared past the boundaries of the [KaSession] they were created in, as they are valid only there.
 *
 * We can restore the symbol in the following cases:
 *
 *  - When the symbol came from Kotlin sources, it will be restored based on [SmartPsiElementPointer][com.intellij.psi.SmartPsiElementPointer].
 *  - Restoring symbols originating from Java sources is not supported yet.
 *  - Library symbols:
 *      - Function & property symbols can be restored if their signature was not changed.
 *      - Local variable symbols can be restored if their containing code block was not changed.
 *      - Class & type alias symbols can be restored if their qualified name was not changed.
 *      - Package symbols can be restored if their package still exists.
 *
 * @see org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
 */
public abstract class KaSymbolPointer<out S : KaSymbol> {
    /**
     * Returns the restored [KaSymbol] (possibly a new symbol instance) if the pointer is still valid, `null` otherwise.
     *
     * Consider using [org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol].
     */
    @KaImplementationDetail
    public abstract fun restoreSymbol(analysisSession: KaSession): S?

    /**
     * Returns `true` if the [other] pointer can be restored to the same symbol. The operation is symmetric and transitive.
     */
    public open fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other

    override fun toString(): String = renderAsDataClassToString()
}

public inline fun <S : KaSymbol> symbolPointer(crossinline getSymbol: (KaSession) -> S?): KaSymbolPointer<S> =
    object : KaSymbolPointer<S>() {
        @KaImplementationDetail
        override fun restoreSymbol(analysisSession: KaSession): S? = getSymbol(analysisSession)
    }

public inline fun <T : KaSymbol, R : KaSymbol> symbolPointerDelegator(
    pointer: KaSymbolPointer<T>,
    crossinline transformer: KaSession.(T) -> R?,
): KaSymbolPointer<R> = object : KaSymbolPointer<R>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): R? = with(analysisSession) {
        val symbol = pointer.restoreSymbol() ?: return null
        transformer(this, symbol)
    }
}
