/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.resolve.calls.components.CreateFreshVariablesSubstitutor.shouldBeFlexible
import org.jetbrains.kotlin.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.TypeRefinement
import org.jetbrains.kotlin.types.typeUtil.unCapture as unCaptureKotlinType

class ClassicConstraintSystemUtilContext(
    val kotlinTypeRefiner: KotlinTypeRefiner,
    val builtIns: KotlinBuiltIns,
) : ConstraintSystemUtilContext {
    override fun TypeVariableMarker.shouldBeFlexible(): Boolean {
        return this is TypeVariableFromCallableDescriptor && this.originalTypeParameter.shouldBeFlexible()
    }

    override fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean {
        require(this is NewTypeVariable)
        return hasOnlyInputTypesAnnotation()
    }

    override fun KotlinTypeMarker.unCapture(): KotlinTypeMarker {
        require(this is KotlinType)
        return unCaptureKotlinType().unwrap()
    }

    override fun TypeVariableMarker.isReified(): Boolean {
        if (this !is TypeVariableFromCallableDescriptor) return false
        return originalTypeParameter.isReified
    }

    @OptIn(TypeRefinement::class)
    override fun KotlinTypeMarker.refineType(): KotlinTypeMarker {
        require(this is KotlinType)
        return kotlinTypeRefiner.refineType(this)
    }

    override fun createArgumentConstraintPosition(argument: PostponedAtomWithRevisableExpectedType): ArgumentConstraintPosition<*> {
        require(argument is ResolvedAtom)
        return ArgumentConstraintPositionImpl(argument.atom as KotlinCallArgument)
    }

    override fun <T> createFixVariableConstraintPosition(variable: TypeVariableMarker, atom: T): FixVariableConstraintPosition<T> {
        require(atom is ResolvedAtom)
        @Suppress("UNCHECKED_CAST")
        return FixVariableConstraintPositionImpl(variable, atom) as FixVariableConstraintPosition<T>
    }

    override fun extractLambdaParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<KotlinTypeMarker?>? {
        require(declaration is ResolvedAtom)
        return when (val atom = declaration.atom) {
            is FunctionExpression -> {
                val receiverType = atom.receiverType
                if (receiverType != null) listOf(receiverType) + atom.parametersTypes else atom.parametersTypes.toList()
            }
            is LambdaKotlinCallArgument -> atom.parametersTypes?.toList()
            else -> null
        }
    }

    override fun PostponedAtomWithRevisableExpectedType.isAnonymousFunction(): Boolean {
        require(this is ResolvedAtom)
        return this.atom is FunctionExpression
    }

    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpressionWithReceiver(): Boolean {
        require(this is ResolvedAtom)
        val atom = this.atom
        return atom is FunctionExpression && atom.receiverType != null
    }

    override fun createTypeVariableForLambdaReturnType(): TypeVariableMarker {
        return TypeVariableForLambdaReturnType(
            builtIns,
            TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
        )
    }

    override fun createTypeVariableForLambdaParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        require(argument is ResolvedAtom)
        val atom = argument.atom as PostponableKotlinCallArgument
        return TypeVariableForLambdaParameterType(
            atom,
            index,
            builtIns,
            TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + (index + 1)
        )
    }

    override fun createTypeVariableForCallableReferenceParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker {
        return TypeVariableForCallableReferenceParameterType(
            builtIns,
            TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE + (index + 1)
        )
    }

    override fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker {
        return TypeVariableForCallableReferenceReturnType(
            builtIns,
            TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE
        )
    }
}
