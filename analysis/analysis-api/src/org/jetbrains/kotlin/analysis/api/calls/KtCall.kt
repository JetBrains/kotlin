/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.calls

import org.jetbrains.kotlin.analysis.api.ValidityTokenOwner
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Represents direct or indirect (via invoke) function call from Kotlin code
 */
public sealed class KtCall : ValidityTokenOwner {
    public abstract val isErrorCall: Boolean
    public abstract val argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>
    public abstract val targetFunction: KtCallTarget
    public abstract val substitutor: KtSubstitutor
}

/**
 * Call using `()` of some variable of functional type, e.g.,
 *
 * fun x(f: () -> Int) {
 *    f() // functional type call
 * }
 */
public class KtFunctionalTypeVariableCall(
    private val _target: KtVariableLikeSymbol,
    private val _argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>,
    private val _targetFunction: KtCallTarget,
    private val _substitutor: KtSubstitutor,
    override val token: ValidityToken
) : KtCall() {
    public val target: KtVariableLikeSymbol get() = withValidityAssertion { _target }
    override val isErrorCall: Boolean get() = withValidityAssertion { false }
    override val argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>
        get() = withValidityAssertion { _argumentMapping }
    override val targetFunction: KtCallTarget
        get() = withValidityAssertion { _targetFunction }
    override val substitutor: KtSubstitutor
        get() = withValidityAssertion { _substitutor }
}

/**
 * Direct or indirect call of function declared by user
 */
public sealed class KtDeclaredFunctionCall : KtCall() {
    override val isErrorCall: Boolean
        get() = withValidityAssertion { targetFunction is KtErrorCallTarget }
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
    private val _argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>,
    private val _targetFunction: KtCallTarget,
    private val _substitutor: KtSubstitutor,
    override val token: ValidityToken
) : KtDeclaredFunctionCall() {
    override val argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>
        get() = withValidityAssertion { _argumentMapping }
    override val targetFunction: KtCallTarget
        get() = withValidityAssertion { _targetFunction }
    override val substitutor: KtSubstitutor
        get() = withValidityAssertion { _substitutor }
}

/**
 * Simple function call, e.g.,
 *
 * x.toString() // function call
 */
public class KtFunctionCall(
    private val _argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>,
    private val _targetFunction: KtCallTarget,
    private val _substitutor: KtSubstitutor,
    override val token: ValidityToken
) : KtDeclaredFunctionCall() {
    override val argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>
        get() = withValidityAssertion { _argumentMapping }
    override val targetFunction: KtCallTarget
        get() = withValidityAssertion { _targetFunction }
    override val substitutor: KtSubstitutor
        get() = withValidityAssertion { _substitutor }
}

/**
 * Annotation call, e.g.,
 *
 * @Retention(AnnotationRetention.SOURCE) // annotation call
 * annotation class Ann
 */
public class KtAnnotationCall(
    private val _argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>,
    private val _targetFunction: KtCallTarget,
    override val token: ValidityToken
) : KtDeclaredFunctionCall() {
    override val argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>
        get() = withValidityAssertion { _argumentMapping }
    override val targetFunction: KtCallTarget
        get() = withValidityAssertion { _targetFunction }

    // Type parameter is allowed for an annotation class but not allowed as members. So substitutor is probably never useful.
    override val substitutor: KtSubstitutor = KtSubstitutor.Empty(token)
}
// TODO: Add other properties, e.g., useSiteTarget

/**
 * Delegated constructor call, e.g.,
 *
 * open class A(a: Int)
 * class B(b: Int) : A(b) { // delegated constructor call (kind = SUPER_CALL)
 *   constructor() : this(1) // delegated constructor call (kind = THIS_CALL)
 * }
 */
public class KtDelegatedConstructorCall(
    private val _argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>,
    private val _targetFunction: KtCallTarget,
    public val kind: KtDelegatedConstructorCallKind,
    override val token: ValidityToken
) : KtDeclaredFunctionCall() {
    override val argumentMapping: LinkedHashMap<KtExpression, KtValueParameterSymbol>
        get() = withValidityAssertion { _argumentMapping }
    override val targetFunction: KtCallTarget
        get() = withValidityAssertion { _targetFunction }

    // A delegate constructor call never has any type argument.
    override val substitutor: KtSubstitutor = KtSubstitutor.Empty(token)
}

public enum class KtDelegatedConstructorCallKind { SUPER_CALL, THIS_CALL }

/**
 * Represents function(s) in which call was resolved,
 * Can be success [KtSuccessCallTarget] in this case there only one such function
 * Or erroneous [KtErrorCallTarget] in this case there can be any count of candidates
 */
public sealed class KtCallTarget : ValidityTokenOwner {
    public abstract val candidates: Collection<KtFunctionLikeSymbol>
}

/**
 * Success call of [symbol]
 */
public class KtSuccessCallTarget(private val _symbol: KtFunctionLikeSymbol, override val token: ValidityToken) : KtCallTarget() {
    public val symbol: KtFunctionLikeSymbol get() = withValidityAssertion { _symbol }
    override val candidates: Collection<KtFunctionLikeSymbol> get() = withValidityAssertion { listOf(symbol) }
}

/**
 * Function call with errors, possible candidates are [candidates]
 */
public class KtErrorCallTarget(
    private val _candidates: Collection<KtFunctionLikeSymbol>,
    private val _diagnostic: KtDiagnostic,
    override val token: ValidityToken
) : KtCallTarget() {
    public val diagnostic: KtDiagnostic get() = withValidityAssertion { _diagnostic }
    override val candidates: Collection<KtFunctionLikeSymbol> get() = withValidityAssertion { _candidates }
}