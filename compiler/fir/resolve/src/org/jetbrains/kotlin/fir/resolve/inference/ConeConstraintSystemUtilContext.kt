/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemUtilContext
import org.jetbrains.kotlin.resolve.calls.inference.components.PostponedArgumentInputTypesResolver
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.FixVariableConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

object ConeConstraintSystemUtilContext : ConstraintSystemUtilContext {
    override fun TypeVariableMarker.shouldBeFlexible(): Boolean {
        // In FIR, there's no need in hack with shouldTryUseDifferentFlexibilityForUpperType
        // See org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext.useRefinedBoundsForTypeVariableInFlexiblePosition
        return false
    }

    override fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean {
        if (this !is ConeTypeParameterBasedTypeVariable) return false
        return typeParameterSymbol.resolvedAnnotationClassIds.any { it == StandardClassIds.Annotations.OnlyInputTypes }
    }

    override fun KotlinTypeMarker.unCapture(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        // TODO, see TypeUtils.kt, KT-59678
        return this
    }

    override fun TypeVariableMarker.isReified(): Boolean {
        return this is ConeTypeParameterBasedTypeVariable && typeParameterSymbol.fir.isReified
    }

    override fun KotlinTypeMarker.refineType(): KotlinTypeMarker {
        return this
    }

    override fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ArgumentConstraintPosition<*> {
        require(argument is PostponedResolvedAtom) {
            "${argument::class}"
        }
        return ConeArgumentConstraintPosition(argument.atom)
    }

    override fun <T> createFixVariableConstraintPosition(variable: TypeVariableMarker, atom: T): FixVariableConstraintPosition<T> {
        require(atom == null)
        @Suppress("UNCHECKED_CAST")
        return ConeFixVariableConstraintPosition(variable) as FixVariableConstraintPosition<T>
    }

    override fun extractLambdaParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<ConeKotlinType?>? {
        require(declaration is PostponedResolvedAtom)
        return when (declaration) {
            is LambdaWithTypeVariableAsExpectedTypeAtom -> {
                val atom = declaration.atom.anonymousFunction
                return if (atom.isLambda) { // lambda - must return null in case of absent parameters
                    if (atom.valueParameters.isNotEmpty())
                        atom.collectDeclaredValueParameterTypes()
                    else null
                } else { // function expression - all types are explicit, shouldn't return null
                    buildList {
                        atom.receiverParameter?.typeRef?.coneType?.let { add(it) }
                        addAll(atom.collectDeclaredValueParameterTypes())
                    }
                }
            }
            else -> null
        }
    }

    private fun FirAnonymousFunction.collectDeclaredValueParameterTypes(): List<ConeKotlinType?> =
        valueParameters.map { it.returnTypeRef.coneTypeSafe() }

    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpression(): Boolean {
        require(this is PostponedResolvedAtom)
        return this is LambdaWithTypeVariableAsExpectedTypeAtom && !this.atom.anonymousFunction.isLambda
    }

    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpressionWithReceiver(): Boolean {
        require(this is PostponedResolvedAtom)
        return this is LambdaWithTypeVariableAsExpectedTypeAtom &&
                !this.atom.anonymousFunction.isLambda &&
                this.atom.anonymousFunction.receiverParameter?.typeRef?.coneType != null
    }

    override fun PostponedAtomWithRevisableExpectedType.isLambda(): Boolean {
        require(this is PostponedResolvedAtom)
        return this is LambdaWithTypeVariableAsExpectedTypeAtom && this.atom.anonymousFunction.isLambda
    }

    override fun createTypeVariableForLambdaReturnType(): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
    }

    override fun createTypeVariableForLambdaParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        return ConeTypeVariableForLambdaParameterType(
            PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + index,
            index
        )
    }

    override fun createTypeVariableForCallableReferenceParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(
            PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE + index
        )
    }

    override fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker {
        return ConeTypeVariableForPostponedAtom(PostponedArgumentInputTypesResolver.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
    }

    override val isForcedConsiderExtensionReceiverFromConstrainsInLambda: Boolean
        get() = true

    override val isForcedAllowForkingInferenceSystem: Boolean
        get() = true
}
