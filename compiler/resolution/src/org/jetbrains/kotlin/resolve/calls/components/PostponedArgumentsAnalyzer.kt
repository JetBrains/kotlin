/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubsystemForArgument
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.LambdaArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns

class PostponedArgumentsAnalyzer(
        private val callableReferenceResolver: CallableReferenceResolver
) {
    interface Context {
        fun buildCurrentSubstitutor(): NewTypeSubstitutor

        // type can be proper if it not contains not fixed type variables
        fun canBeProper(type: UnwrappedType): Boolean

        // mutable operations
        fun addOtherSystem(otherSystem: ConstraintStorage)
        fun getBuilder(): ConstraintSystemBuilder
    }

    fun analyze(c: Context, resolutionCallbacks: KotlinResolutionCallbacks, argument: ResolvedAtom) {
        when (argument) {
            is ResolvedLambdaAtom -> analyzeLambda(c, resolutionCallbacks, argument)
            is LambdaWithTypeVariableAsExpectedTypeAtom -> analyzeLambda(c, resolutionCallbacks, argument.transformToResolvedLambda(c.getBuilder()))
            is ResolvedCallableReferenceAtom -> callableReferenceResolver.processCallableReferenceArgument(c.getBuilder(), argument)
            is ResolvedCollectionLiteralAtom -> TODO("Not supported")
            else -> error("Unexpected resolved primitive: ${argument.javaClass.canonicalName}")
        }
    }

    private fun analyzeLambda(c: Context, resolutionCallbacks: KotlinResolutionCallbacks, lambda: ResolvedLambdaAtom) {
        val currentSubstitutor = c.buildCurrentSubstitutor()
        fun substitute(type: UnwrappedType) = currentSubstitutor.safeSubstitute(type)

        val receiver = lambda.receiver?.let(::substitute)
        val parameters = lambda.parameters.map(::substitute)
        val expectedType = lambda.returnType.takeIf { c.canBeProper(it) }?.let(::substitute)

        val resultArguments = resolutionCallbacks.analyzeAndGetLambdaResultArguments(lambda.atom, lambda.isSuspend, receiver, parameters, expectedType)

        resultArguments.forEach { c.addSubsystemForArgument(it) }

        val diagnosticHolder = KotlinDiagnosticsHolder.SimpleHolder()

        val subResolvedKtPrimitives = resultArguments.map {
            checkSimpleArgument(c.getBuilder(), it, lambda.returnType.let(::substitute), diagnosticHolder, isReceiver = false)
        }

        if (resultArguments.isEmpty()) {
            val unitType = lambda.returnType.builtIns.unitType
            c.getBuilder().addSubtypeConstraint(lambda.returnType.let(::substitute), unitType, LambdaArgumentConstraintPosition(lambda))
        }

        lambda.setAnalyzedResults(resultArguments, subResolvedKtPrimitives, diagnosticHolder.getDiagnostics())
    }
}