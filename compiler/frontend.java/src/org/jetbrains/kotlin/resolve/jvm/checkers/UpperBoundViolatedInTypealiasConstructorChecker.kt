/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.checker.ClassicTypeCheckerState
import org.jetbrains.kotlin.types.checker.ClassicTypeCheckerStateInternals

object UpperBoundViolatedInTypealiasConstructorChecker : CallChecker {
    @OptIn(ClassicTypeCheckerStateInternals::class)
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val resultingDescriptor = resolvedCall.resultingDescriptor as? TypeAliasConstructorDescriptor ?: return
        val callExpression = reportOn.getStrictParentOfType<KtCallExpression>() ?: return
        val underlyingConstructedType = resultingDescriptor.underlyingConstructorDescriptor.returnType
        val underlyingTypeArguments = underlyingConstructedType.arguments
        val underlyingTypeParameters = resultingDescriptor.underlyingConstructorDescriptor.returnType.constructor.parameters
        val state = ClassicTypeCheckerState(isErrorTypeEqualsToAnything = false)
        val substitutor = NewTypeSubstitutorByConstructorMap(
            underlyingTypeParameters.withIndex().associate {
                Pair(it.value.typeConstructor, underlyingTypeArguments[it.index].type.unwrap())
            }
        )
        // Note: necessary only for diagnostic duplication check
        val aliasTypeParameters = resolvedCall.candidateDescriptor.typeParameters
        val originalTypes = resultingDescriptor.typeAliasDescriptor.underlyingType.arguments.map { it.type }
        for ((index, argumentAndParameter) in underlyingTypeArguments.zip(underlyingTypeParameters).withIndex()) {
            val (argument, parameter) = argumentAndParameter
            // To remove duplication of UPPER_BOUND_VIOLATED
            // See createToFreshVariableSubstitutorAndAddInitialConstraints in ResolutionParts.kt, citing:
            // ... if (kotlinType == typeParameter.defaultType) i else null ...
            if (aliasTypeParameters.getOrNull(index)?.defaultType == originalTypes.getOrNull(index)) continue

            for (upperBound in parameter.upperBounds) {
                if (!AbstractTypeChecker.isSubtypeOf(state, argument.type, substitutor.safeSubstitute(upperBound.unwrap()))) {
                    val typeReference = callExpression.typeArguments.getOrNull(index)?.typeReference ?: continue
                    context.trace.report(Errors.UPPER_BOUND_VIOLATED_WARNING.on(typeReference, upperBound, argument.type))
                }
            }
        }
    }
}