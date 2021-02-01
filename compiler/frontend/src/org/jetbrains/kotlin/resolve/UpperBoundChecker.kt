/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

@DefaultImplementation(impl = UpperBoundChecker::class)
interface UpperBoundChecker {
    val languageVersionSettings: LanguageVersionSettings

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

    fun checkBounds(
        jetTypeArgument: KtTypeReference,
        typeArgument: KotlinType,
        typeParameterDescriptor: TypeParameterDescriptor,
        substitutor: TypeSubstitutor,
        trace: BindingTrace
    ) {
        for (bound in typeParameterDescriptor.upperBounds) {
            checkBound(bound, substitutor, trace, jetTypeArgument, typeArgument)
        }
    }

    fun checkBound(
        bound: KotlinType,
        substitutor: TypeSubstitutor,
        trace: BindingTrace,
        jetTypeArgument: KtTypeReference,
        typeArgument: KotlinType
    ): Boolean {
        val substitutedBound = substitutor.safeSubstitute(bound, Variance.INVARIANT)
        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(typeArgument, substitutedBound)) {
            trace.report(Errors.UPPER_BOUND_VIOLATED.on(jetTypeArgument, substitutedBound, typeArgument))
            return false
        }
        return true
    }
}
