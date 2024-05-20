/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.utils.relfection.renderAsDataClassToString

/**
 * [KaSymbol] is valid only during read action it was created in
 * To pass the symbol from one read action to another the KtSymbolPointer should be used
 *
 * We can restore the symbol
 *  * for symbol which came from Kotlin source it will be restored based on [com.intellij.psi.SmartPsiElementPointer]
 *  * restoring symbols which came from Java source is not supported yet
 *  * for library symbols:
 *    * for function & property symbol if its signature was not changed
 *    * for local variable symbol if code block it was declared in was not changed
 *    * for class & type alias symbols if its qualified name was not changed
 *    * for package symbol if the package is still exists
 *
 * @see org.jetbrains.kotlin.analysis.api.lifetime.KaReadActionConfinementLifetimeToken
 */
public abstract class KaSymbolPointer<out S : KaSymbol> {
    /**
     * @return restored symbol (possibly the new symbol instance) if one is still valid, `null` otherwise
     *
     * Consider using [org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol]
     */
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
    public abstract fun restoreSymbol(analysisSession: KaSession): S?

    /**
     * @return **true** if [other] pointer can be restored to the same symbol. The operation is symmetric and transitive.
     */
    public open fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other

    override fun toString(): String = renderAsDataClassToString()
}

public typealias KtSymbolPointer<S> = KaSymbolPointer<S>

public inline fun <S : KaSymbol> symbolPointer(crossinline getSymbol: (KaSession) -> S?): KaSymbolPointer<S> =
    object : KaSymbolPointer<S>() {
        @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
        override fun restoreSymbol(analysisSession: KaSession): S? = getSymbol(analysisSession)
    }

public inline fun <T : KaSymbol, R : KaSymbol> symbolPointerDelegator(
    pointer: KaSymbolPointer<T>,
    crossinline transformer: KaSession.(T) -> R?,
): KaSymbolPointer<R> = object : KaSymbolPointer<R>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KaSession): R? = with(analysisSession) {
        val symbol = pointer.restoreSymbol() ?: return null
        transformer(this, symbol)
    }
}
