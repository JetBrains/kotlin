/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.containsTypeAliasParameters

@DefaultImplementation(impl = UpperBoundChecker::class)
open class UpperBoundChecker(
    private val typeChecker: KotlinTypeChecker,
) {
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
        diagnosticForTypeAliases: DiagnosticFactory3<KtElement, KotlinType, KotlinType, ClassifierDescriptor> = UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION
    ) {
        if (typeParameterDescriptor.upperBounds.isEmpty()) return

        val diagnosticsReporter =
            UpperBoundViolatedReporter(trace, argumentType, typeParameterDescriptor, diagnosticForTypeAliases = diagnosticForTypeAliases)

        for (bound in typeParameterDescriptor.upperBounds) {
            checkBound(bound, argumentType, argumentReference, substitutor, typeAliasUsageElement, diagnosticsReporter)
        }
    }

    fun checkBoundsInSupertype(
        typeReference: KtTypeReference,
        type: KotlinType,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
    ) {
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
            checkBoundsInSupertype(ktTypeArguments[0], flexibleType.lowerBound, trace, languageVersionSettings)
            checkBoundsInSupertype(ktTypeArguments[1], flexibleType.upperBound, trace, languageVersionSettings)
            return
        }

        if (type is AbbreviatedType) {
            checkBoundsForAbbreviatedSupertype(
                type, trace, typeReference,
                // The errors have been reported previously if ktTypeArguments.size accidentally was equal to the amount of arguments
                // in the expanded type
                reportWarning = ktTypeArguments.size != arguments.size &&
                        !languageVersionSettings.supportsFeature(
                            LanguageFeature.ReportMissingUpperBoundsViolatedErrorOnAbbreviationAtSupertypes
                        )
            )
            return
        }

        // If the numbers of type arguments do not match, the error has been already reported in TypeResolver
        if (ktTypeArguments.size != arguments.size) return

        val substitutor = TypeSubstitutor.create(type)

        for (i in ktTypeArguments.indices) {
            val ktTypeArgument = ktTypeArguments[i] ?: continue
            checkBoundsInSupertype(ktTypeArgument, arguments[i].type, trace, languageVersionSettings)
            checkBounds(ktTypeArgument, arguments[i].type, parameters[i], substitutor, trace)
        }
    }

    private fun checkBoundsForAbbreviatedSupertype(
        type: KotlinType,
        trace: BindingTrace,
        typeReference: KtTypeReference,
        reportWarning: Boolean
    ) {
        val parameters = type.constructor.parameters
        val arguments = type.arguments
        val substitutor = TypeSubstitutor.create(type)

        val diagnostic =
            if (reportWarning)
                UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION_WARNING
            else
                UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION

        for (i in arguments.indices) {
            if (arguments[i].isStarProjection) continue
            val argumentType = arguments[i].type

            checkBoundsForAbbreviatedSupertype(argumentType, trace, typeReference, reportWarning)

            checkBounds(
                argumentReference = null,
                argumentType, parameters[i], substitutor, trace,
                typeAliasUsageElement = typeReference, diagnosticForTypeAliases = diagnostic,
            )
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

        if (!typeChecker.isSubtypeOf(argumentType, substitutedBound)) {
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
