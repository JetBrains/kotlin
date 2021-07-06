/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.calls

import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableLikeSymbol

/**
 * Represents direct or indirect (via invoke) function call from Kotlin code
 */
public sealed class KtCall {
    public abstract val isErrorCall: Boolean
    public abstract val targetFunction: KtCallTarget
}

/**
 * Call using `()` of some variable of functional type, e.g.,
 *
 * fun x(f: () -> Int) {
 *    f() // functional type call
 * }
 */
public class KtFunctionalTypeVariableCall(
    public val target: KtVariableLikeSymbol,
    override val targetFunction: KtCallTarget
) : KtCall() {
    override val isErrorCall: Boolean get() = false
}

/**
 * Direct or indirect call of function declared by user
 */
public sealed class KtDeclaredFunctionCall : KtCall() {
    override val isErrorCall: Boolean
        get() = targetFunction is KtErrorCallTarget
}

/**
 * Call using () on variable on some non-functional type, considers that `invoke` function is declared somewhere
 *
 * fun x(y: Int) {
 *    y() // variable with invoke function call
 * }
 *
 * fun Int.invoke() {}
 */
public class KtVariableWithInvokeFunctionCall(
    public val target: KtVariableLikeSymbol,
    override val targetFunction: KtCallTarget
) : KtDeclaredFunctionCall()

/**
 * Simple function call, e.g.,
 *
 * x.toString() // function call
 */
public data class KtFunctionCall(override val targetFunction: KtCallTarget) : KtDeclaredFunctionCall()

/**
 * Represents function(s) in which call was resolved,
 * Can be success [KtSuccessCallTarget] in this case there only one such function
 * Or erroneous [KtErrorCallTarget] in this case there can be any count of candidates
 */
public sealed class KtCallTarget {
    public abstract val candidates: Collection<KtFunctionLikeSymbol>
}

/**
 * Success call of [symbol]
 */
public class KtSuccessCallTarget(public val symbol: KtFunctionLikeSymbol) : KtCallTarget() {
    override val candidates: Collection<KtFunctionLikeSymbol>
        get() = listOf(symbol)
}

/**
 * Function all with errors, possible candidates are [candidates]
 */
public class KtErrorCallTarget(
    override val candidates: Collection<KtFunctionLikeSymbol>,
    public val diagnostic: KtDiagnostic
) : KtCallTarget()