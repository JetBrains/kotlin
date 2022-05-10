/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtConstantAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.annotationsByClassId
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * A signature for a callable symbol. Comparing to a `KtCallableSymbol`, a signature can carry use-site type information. For example
 * ```
 * fun test(l: List<String>) {
 *   l.get(1) // The symbol `get` has type `(Int) -> T` where is the type parameter declared in `List`. On the other hand, a KtSignature
 *            // carries instantiated type information `(Int) -> String`.
 * }
 * ```
 *
 * Equality of [KtSignature] is derived from its content.
 */
public sealed class KtSignature<out S : KtCallableSymbol> : KtLifetimeOwner {
    /**
     * The original symbol for this signature.
     */
    public abstract val symbol: S

    /**
     * The use-site-substituted return type.
     */
    public abstract val returnType: KtType

    /**
     * The use-site-substituted extension receiver type.
     */
    public abstract val receiverType: KtType?

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

/**
 * A signature of a function-like symbol.
 */
public data class KtFunctionLikeSignature<out S : KtFunctionLikeSymbol>(
    private val _symbol: S,
    private val _returnType: KtType,
    private val _receiverType: KtType?,
    private val _valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>,
) : KtSignature<S>() {
    override val token: KtLifetimeToken
        get() = _symbol.token
    override val symbol: S
        get() = withValidityAssertion { _symbol }
    override val returnType: KtType
        get() = withValidityAssertion { _returnType }
    override val receiverType: KtType?
        get() = withValidityAssertion { _receiverType }

    /**
     * The use-site-substituted value parameters.
     */
    public val valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>>
        get() = withValidityAssertion { _valueParameters }
}

/**
 * A signature of a variable-like symbol.
 */
public data class KtVariableLikeSignature<out S : KtVariableLikeSymbol>(
    private val _symbol: S,
    private val _returnType: KtType,
    private val _receiverType: KtType?,
) : KtSignature<S>() {
    override val token: KtLifetimeToken
        get() = _symbol.token
    override val symbol: S
        get() = withValidityAssertion { _symbol }
    override val returnType: KtType
        get() = withValidityAssertion { _returnType }
    override val receiverType: KtType?
        get() = withValidityAssertion { _receiverType }

    /**
     * A name of the variable with respect to the `@ParameterName` annotation. Can be different from the [KtVariableLikeSymbol.name].
     *
     * Some variables can have their names changed by special annotations like `@ParameterName(name = "newName")`. This is used to preserve
     * the names of the lambda parameters in the situations like this:
     *
     * ```
     * // compiled library
     * fun foo(): (bar: String) -> Unit { ... }
     *
     * // source code
     * fun test() {
     *   val action = foo()
     *   action("") // this call
     * }
     * ```
     *
     * Unfortunately, [symbol] for the `action("")` call will be pointing to the `Function1<P1, R>.invoke(p1: P1): R`, because we
     * intentionally unwrap use-site substitution overrides. Because of this, `symbol.name` will yield `"p1"`, and not `"bar"`.
     *
     * To overcome this problem, [name] property is introduced: it allows to get the intended name of the parameter,
     * with respect to `@ParameterName` annotation.
     *
     * @see org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder.unwrapUseSiteSubstitutionOverride
     */
    val name: Name
        get() = withValidityAssertion {
            // The case where PSI is null is when calling `invoke()` on a variable with functional type, e.g. `x(1)` below:
            //
            //   fun foo(x: (item: Int) -> Unit) { x(1) }
            //   fun bar(x: Function1<@ParameterName("item") Int, Unit>) { x(1) }
            val nameCanBeDeclaredInAnnotation = _symbol.psi == null

            runIf(nameCanBeDeclaredInAnnotation) { getValueFromParameterNameAnnotation() } ?: _symbol.name
        }

    private fun getValueFromParameterNameAnnotation(): Name? {
        val resultingAnnotation = findParameterNameAnnotation() ?: return null
        val parameterNameArgument = resultingAnnotation.arguments
            .singleOrNull { it.name == StandardClassIds.Annotations.ParameterNames.parameterNameName }

        val constantArgumentValue = parameterNameArgument?.expression as? KtConstantAnnotationValue ?: return null

        return (constantArgumentValue.constantValue.value as? String)?.let(Name::identifier)
    }

    private fun findParameterNameAnnotation(): KtAnnotationApplication? {
        val allParameterNameAnnotations = returnType.annotationsByClassId(StandardNames.FqNames.parameterNameClassId)
        val (explicitAnnotations, implicitAnnotations) = allParameterNameAnnotations.partition { it.psi != null }

        return if (explicitAnnotations.isNotEmpty()) {
            explicitAnnotations.first()
        } else {
            implicitAnnotations.singleOrNull()
        }
    }
}

public fun <S : KtCallableSymbol> S.toSignature(substitutor: KtSubstitutor = KtSubstitutor.Empty(token)): KtSignature<S> {
    return when (this) {
        is KtVariableLikeSymbol -> toSignature(substitutor)
        is KtFunctionLikeSymbol -> toSignature(substitutor)
        else -> error("unexpected callable symbol $this")
    }
}

/**
 * Creates a signature with the same type as the given symbol.
 */
public fun <S : KtVariableLikeSymbol> S.toSignature(substitutor: KtSubstitutor = KtSubstitutor.Empty(token)): KtVariableLikeSignature<S> {
    return KtVariableLikeSignature(this, returnType, receiverType).substitute(substitutor)
}

/**
 * Creates a signature with the same type as the given symbol.
 */
public fun <S : KtFunctionLikeSymbol> S.toSignature(substitutor: KtSubstitutor = KtSubstitutor.Empty(token)): KtFunctionLikeSignature<S> {
    return KtFunctionLikeSignature(this, returnType, receiverType, valueParameters.map { it.toSignature() }).substitute(substitutor)
}

/**
 * Applies a substitutor to the given signature and return a new signature with substituted types.
 */
@Suppress("UNCHECKED_CAST")
public fun <C : KtSignature<S>, S : KtCallableSymbol> C.substitute(substitutor: KtSubstitutor): C {
    if (substitutor is KtSubstitutor.Empty) return this
    return when (this) {
        is KtFunctionLikeSignature<*> -> KtFunctionLikeSignature(
            symbol,
            substitutor.substituteOrSelf(returnType),
            receiverType?.let { substitutor.substituteOrSelf(it) },
            valueParameters.map { it.substitute(substitutor) }
        ) as C
        is KtVariableLikeSignature<*> -> KtVariableLikeSignature(
            symbol,
            substitutor.substituteOrSelf(returnType),
            receiverType?.let { substitutor.substituteOrSelf(it) },
        ) as C
        else -> error("impossible since KtSignature is sealed")
    }
}