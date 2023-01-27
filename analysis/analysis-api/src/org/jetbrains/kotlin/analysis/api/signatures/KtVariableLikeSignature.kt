/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.signatures

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtConstantAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.annotationsByClassId
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf


/**
 * A signature of a variable-like symbol. This includes properties, enum entries local variables, etc.
 */
public class KtVariableLikeSignature<out S : KtVariableLikeSymbol>(
    private val _symbol: S,
    private val _returnType: KtType,
    private val _receiverType: KtType?,
) : KtCallableSignature<S>() {
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
    public val name: Name
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

    private fun findParameterNameAnnotation(): KtAnnotationApplicationWithArgumentsInfo? {
        val allParameterNameAnnotations = returnType.annotationsByClassId(StandardNames.FqNames.parameterNameClassId)
        val (explicitAnnotations, implicitAnnotations) = allParameterNameAnnotations.partition { it.psi != null }

        return if (explicitAnnotations.isNotEmpty()) {
            explicitAnnotations.first()
        } else {
            implicitAnnotations.singleOrNull()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtVariableLikeSignature<*>

        if (_symbol != other._symbol) return false
        if (_returnType != other._returnType) return false
        if (_receiverType != other._receiverType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _symbol.hashCode()
        result = 31 * result + _returnType.hashCode()
        result = 31 * result + (_receiverType?.hashCode() ?: 0)
        return result
    }
}
