/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemUtilContext
import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.FixVariableConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.LambdaWithTypeVariableAsExpectedTypeMarker
import org.jetbrains.kotlin.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

object ConeConstraintSystemUtilContext : ConstraintSystemUtilContext {
    override fun TypeVariableMarker.shouldBeFlexible(): Boolean {
        // TODO
        return false
    }

    override fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean {
        // TODO
        return false
    }

    override fun KotlinTypeMarker.unCapture(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        // TODO, see TypeUtils.kt
        return this
    }

    override fun TypeVariableMarker.isReified(): Boolean {
        // TODO
        return false
    }

    override fun KotlinTypeMarker.refineType(): KotlinTypeMarker {
        return this
    }

    override fun KotlinTypeMarker.isFunctionOrKFunctionTypeWithAnySuspendability(): Boolean {
        TODO()
    }

    override fun KotlinTypeMarker.isSuspendFunctionTypeOrSubtype(): Boolean {
        TODO()
    }

    override fun extractFunctionalTypeFromSupertypes(type: KotlinTypeMarker): KotlinTypeMarker {
        TODO()
    }

    override fun KotlinTypeMarker.extractArgumentsForFunctionalTypeOrSubtype(): List<KotlinTypeMarker> {
        TODO()
    }

    override fun <T> createArgumentConstraintPosition(argument: T): ArgumentConstraintPosition<T> {
        TODO()
    }

    override fun <T> createFixVariableConstraintPosition(variable: TypeVariableMarker, atom: T): FixVariableConstraintPosition<T> {
        TODO()
    }

    override fun extractParameterTypesFromDeclaration(declaration: PostponedAtomWithRevisableExpectedType): List<ConeKotlinType?>? {
        TODO()
    }

    override fun KotlinTypeMarker.isExtensionFunctionType(): Boolean {
        TODO()
    }

    override fun getFunctionTypeConstructor(parametersNumber: Int, isSuspend: Boolean): TypeConstructorMarker {
        TODO()
    }

    override fun getKFunctionTypeConstructor(parametersNumber: Int, isSuspend: Boolean): TypeConstructorMarker {
        TODO()
    }

    override fun isAnonymousFunction(argument: PostponedAtomWithRevisableExpectedType): Boolean {
        TODO()
    }

    override fun PostponedAtomWithRevisableExpectedType.isFunctionExpressionWithReceiver(): Boolean {
        TODO()
    }

    override fun createTypeVariableForLambdaReturnType(): TypeVariableMarker {
        TODO()
    }

    override fun createTypeVariableForLambdaParameterType(argument: PostponedAtomWithRevisableExpectedType, index: Int): TypeVariableMarker {
        TODO()
    }

    override fun createTypeVariableForCallableReferenceParameterType(argument: PostponedAtomWithRevisableExpectedType, index: Int): TypeVariableMarker {
        TODO()
    }

    override fun createTypeVariableForCallableReferenceReturnType(): TypeVariableMarker {
        TODO()
    }
}
