/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol

/**
 * `KtSymbol` is valid only during read action it was created in
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
 * @see org.jetbrains.kotlin.analysis.api.lifetime.KtReadActionConfinementLifetimeToken
 */
public abstract class KtSymbolPointer<out S : KtSymbol> {
    /**
     * @return restored symbol (possibly the new symbol instance) if one is still valid, `null` otherwise
     *
     * Consider using [org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol]
     */
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    public abstract fun restoreSymbol(analysisSession: KtAnalysisSession): S?

    /**
     * @return **true** if [other] pointer can be restored to the same symbol. The operation is symmetric and transitive.
     */
    public open fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other
}

public inline fun <S : KtSymbol> symbolPointer(crossinline getSymbol: (KtAnalysisSession) -> S?): KtSymbolPointer<S> =
    object : KtSymbolPointer<S>() {
        @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
        override fun restoreSymbol(analysisSession: KtAnalysisSession): S? = getSymbol(analysisSession)
    }

public inline fun <T : KtSymbol, R : KtSymbol> symbolPointerDelegator(
    pointer: KtSymbolPointer<T>,
    crossinline transformer: context(KtAnalysisSession) (T) -> R?,
): KtSymbolPointer<R> = object : KtSymbolPointer<R>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    override fun restoreSymbol(analysisSession: KtAnalysisSession): R? = with(analysisSession) {
        val symbol = pointer.restoreSymbol() ?: return null
        transformer(this, symbol)
    }
}
