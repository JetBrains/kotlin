/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLambdaArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.render

class FirCallArgumentsProcessor(
    private val function: FirFunction<*>,
    private val arguments: List<FirExpression>
) {
    class Result(val argumentMapping: Map<FirExpression, FirValueParameter>, val isSuccess: Boolean)

    fun process(): Result {
        var currentState: State = State.PositionalOnly(function.valueParameters)
        for (argument in arguments) {
            if (argument is FirNamedArgumentExpression || argument is FirLambdaArgumentExpression) {
                currentState = State.PositionalThenNamed(
                    function.valueParameters,
                    currentState.argumentMap,
                    currentState.usedParameters
                )
            }
            val status = currentState.processArgument(argument)
            if (status != MappingStatus.SUCCESS) {
                // unmapped argument
                return Result(currentState.argumentMap, isSuccess = false)
            }
        }

        for (valueParameter in function.valueParameters) {
            if (valueParameter !in currentState.usedParameters && !valueParameter.isVararg && valueParameter.defaultValue == null) {
                // unmapped parameter
                return Result(currentState.argumentMap, isSuccess = false)
            }
        }
        return Result(currentState.argumentMap, isSuccess = currentState.argumentMap.size == arguments.size)
    }

    private enum class MappingStatus {
        SUCCESS,
        ERROR
    }

    private sealed class State(
        val valueParameters: List<FirValueParameter>,
        val argumentMap: MutableMap<FirExpression, FirValueParameter> = mutableMapOf(),
        val usedParameters: MutableSet<FirValueParameter> = mutableSetOf()
    ) {
        abstract fun processArgument(argument: FirExpression): MappingStatus

        class PositionalOnly(valueParameters: List<FirValueParameter>) : State(valueParameters) {
            var currentParameterIndex: Int = 0

            val currentParameter get() = valueParameters.getOrNull(currentParameterIndex)

            override fun processArgument(argument: FirExpression): MappingStatus {
                require(argument !is FirNamedArgumentExpression) {
                    "Positional-only argument processor state should not receive ${argument.render()}"
                }

                val currentParameter = currentParameter ?: return MappingStatus.ERROR
                argumentMap[argument] = currentParameter
                if (!currentParameter.isVararg ||
                    argument is FirWrappedArgumentExpression && argument.isSpread
                ) {
                    usedParameters += currentParameter
                    currentParameterIndex++
                }

                return MappingStatus.SUCCESS
            }
        }

        class PositionalThenNamed(
            valueParameters: List<FirValueParameter>,
            argumentMap: MutableMap<FirExpression, FirValueParameter>,
            usedParameters: MutableSet<FirValueParameter>
        ) : State(valueParameters, argumentMap, usedParameters) {
            val nameToParameter = valueParameters.associateBy { it.name }

            private fun map(parameter: FirValueParameter, argument: FirExpression): MappingStatus {
                if (parameter in usedParameters) return MappingStatus.ERROR
                argumentMap[argument] = parameter
                usedParameters += parameter
                return MappingStatus.SUCCESS
            }

            override fun processArgument(argument: FirExpression): MappingStatus {
                when (argument) {
                    is FirNamedArgumentExpression -> {
                        val name = argument.name
                        val parameter = nameToParameter[name] ?: return MappingStatus.ERROR
                        return map(parameter, argument)
                    }
                    is FirLambdaArgumentExpression -> {
                        val lastParameter = valueParameters.lastOrNull() ?: return MappingStatus.ERROR
                        return map(lastParameter, argument)
                    }
                    else -> {
                        return MappingStatus.ERROR
                    }
                }
            }
        }
    }
}