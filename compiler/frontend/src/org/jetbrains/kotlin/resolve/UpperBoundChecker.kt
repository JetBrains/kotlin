/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.Errors.UPPER_BOUND_VIOLATED
import org.jetbrains.kotlin.diagnostics.Errors.UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.containsTypeAliasParameters

@DefaultImplementation(impl = UpperBoundChecker::class)
open class UpperBoundChecker {
    open fun checkBoundsOfExpandedTypeAlias(type: KotlinType, expression: KtExpression, trace: BindingTrace) {
        // do nothing in the strict mode as the errors are already reported in the type inference if necessary
    }

    open fun checkBounds(
        argumentReference: KtTypeReference?,
        argumentType: KotlinType,
        typeParameterDescriptor: TypeParameterDescriptor,
        substitutor: TypeSubstitutor,
        trace: BindingTrace,
        typeAliasUsageElement: KtElement? = null,
    ) {
        if (typeParameterDescriptor.upperBounds.isEmpty()) return

        val diagnosticsReporter = UpperBoundViolatedReporter(trace, argumentType, typeParameterDescriptor)

        for (bound in typeParameterDescriptor.upperBounds) {
            checkBound(bound, argumentType, argumentReference, substitutor, typeAliasUsageElement, diagnosticsReporter)
        }
    }

    fun checkBounds(typeReference: KtTypeReference, type: KotlinType, trace: BindingTrace) {
        if (type.isError) return

        val typeElement = typeReference.typeElement ?: return
        val parameters = type.constructor.parameters
        val arguments = type.arguments

        assert(parameters.size == arguments.size)

        val ktTypeArguments = typeElement.typeArgumentsAsTypes

        // A type reference from Kotlin code can yield a flexible type only if it's `ft<T1, T2>`, whose bounds should not be checked
        if (type.isFlexible() && !type.isDynamic()) {
            assert(ktTypeArguments.size == 2) {
                ("Flexible type cannot be denoted in Kotlin otherwise than as ft<T1, T2>, but was: "
                        + typeReference.getElementTextWithContext())
            }
            // it's really ft<Foo, Bar>
            val flexibleType = type.asFlexibleType()
            checkBounds(ktTypeArguments[0], flexibleType.lowerBound, trace)
            checkBounds(ktTypeArguments[1], flexibleType.upperBound, trace)
            return
        }

        // If the numbers of type arguments do not match, the error has been already reported in TypeResolver
        if (ktTypeArguments.size != arguments.size) return

        val substitutor = TypeSubstitutor.create(type)

        for (i in ktTypeArguments.indices) {
            val ktTypeArgument = ktTypeArguments[i] ?: continue
            checkBounds(ktTypeArgument, arguments[i].type, trace)
            checkBounds(ktTypeArgument, arguments[i].type, parameters[i], substitutor, trace)
        }
    }

    protected fun checkBound(
        bound: KotlinType,
        argumentType: KotlinType,
        argumentReference: KtTypeReference?,
        substitutor: TypeSubstitutor,
        typeAliasUsageElement: KtElement? = null,
        upperBoundViolatedReporter: UpperBoundViolatedReporter
    ): Boolean {
        val substitutedBound = substitutor.safeSubstitute(bound, Variance.INVARIANT)

        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(argumentType, substitutedBound)) {
            if (argumentReference != null) {
                upperBoundViolatedReporter.report(argumentReference, substitutedBound)
            } else if (typeAliasUsageElement != null && !substitutedBound.containsTypeAliasParameters() && !argumentType.containsTypeAliasParameters()) {
                upperBoundViolatedReporter.reportForTypeAliasExpansion(typeAliasUsageElement, substitutedBound)
            }
            return false
        }

        return true
    }
}

class UpperBoundViolatedReporter(
    private val trace: BindingTrace,
    private val argumentType: KotlinType,
    private val typeParameterDescriptor: TypeParameterDescriptor,
    private val baseDiagnostic: DiagnosticFactory2<KtTypeReference, KotlinType, KotlinType> = UPPER_BOUND_VIOLATED,
    private val diagnosticForTypeAliases: DiagnosticFactory3<KtElement, KotlinType, KotlinType, ClassifierDescriptor> = UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION
) {
    fun report(typeArgumentReference: KtTypeReference, substitutedBound: KotlinType) {
        trace.reportDiagnosticOnce(baseDiagnostic.on(typeArgumentReference, substitutedBound, argumentType))
    }

    fun reportForTypeAliasExpansion(callElement: KtElement, substitutedBound: KotlinType) {
        trace.reportDiagnosticOnce(diagnosticForTypeAliases.on(callElement, substitutedBound, argumentType, typeParameterDescriptor))
    }
}
