/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.isOperator
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLambdaArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildNamedArgumentExpression
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.defaultParameterResolver
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.name.Name
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

data class ArgumentMapping(
    // This map should be ordered by arguments as written, e.g.:
    //      fun foo(a: Int, b: Int) {}
    //      foo(b = bar(), a = qux())
    // parameterToCallArgumentMap.values() should be [ 'bar()', 'foo()' ]
    val parameterToCallArgumentMap: Map<FirValueParameter, ResolvedCallArgument>,
    val diagnostics: List<ResolutionDiagnostic>
) {
    fun toArgumentToParameterMapping(): Map<FirExpression, FirValueParameter> {
        val argumentToParameterMapping = mutableMapOf<FirExpression, FirValueParameter>()
        parameterToCallArgumentMap.forEach { (valueParameter, resolvedArgument) ->
            when (resolvedArgument) {
                is ResolvedCallArgument.SimpleArgument -> argumentToParameterMapping[resolvedArgument.callArgument] = valueParameter
                is ResolvedCallArgument.VarargArgument -> resolvedArgument.arguments.forEach {
                    argumentToParameterMapping[it] = valueParameter
                }
            }
        }
        return argumentToParameterMapping
    }

    fun numDefaults(): Int {
        return parameterToCallArgumentMap.values.count { it == ResolvedCallArgument.DefaultArgument }
    }
}

private val EmptyArgumentMapping = ArgumentMapping(emptyMap(), emptyList())

fun BodyResolveComponents.mapArguments(
    arguments: List<FirExpression>,
    function: FirFunction<*>,
    originScope: FirScope?,
): ArgumentMapping {
    if (arguments.isEmpty() && function.valueParameters.isEmpty()) {
        return EmptyArgumentMapping
    }
    val externalArgument: FirExpression? = arguments.lastOrNull { it is FirLambdaArgumentExpression }
    var argumentsInParenthesis: List<FirExpression> = if (externalArgument == null) {
        arguments
    } else {
        arguments.subList(0, arguments.size - 1)
    }

    // If this is an overloading indexed access operator, it could have default values in the middle.
    // For proper argument mapping, wrap the last one, which is supposed to be the updated value, as a named argument.
    if ((function as? FirSimpleFunction)?.isOperator == true &&
        function.name == Name.identifier("set") &&
        function.valueParameters.any { it.defaultValue != null }
    ) {
        val v = argumentsInParenthesis.last()
        if (v !is FirNamedArgumentExpression) {
            val namedV = buildNamedArgumentExpression {
                source = v.source
                expression = v
                isSpread = false
                name = function.valueParameters.last().name
            }
            argumentsInParenthesis = argumentsInParenthesis.dropLast(1) + listOf(namedV)
        }
    }

    val processor = FirCallArgumentsProcessor(function, this, originScope)
    processor.processArgumentsInParenthesis(argumentsInParenthesis)
    if (externalArgument != null) {
        processor.processExternalArgument(externalArgument)
    }
    processor.processDefaultsAndRunChecks()

    return ArgumentMapping(processor.result, processor.diagnostics ?: emptyList())
}

private class FirCallArgumentsProcessor(
    private val function: FirFunction<*>,
    private val bodyResolveComponents: BodyResolveComponents,
    private val originScope: FirScope?,
) {
    private var state = State.POSITION_ARGUMENTS
    private var currentPositionedParameterIndex = 0
    private var varargArguments: MutableList<FirExpression>? = null
    private var nameToParameter: Map<Name, FirValueParameter>? = null
    var diagnostics: MutableList<ResolutionDiagnostic>? = null
        private set
    val result: MutableMap<FirValueParameter, ResolvedCallArgument> = LinkedHashMap()

    private enum class State {
        POSITION_ARGUMENTS,
        VARARG_POSITION,
        NAMED_ONLY_ARGUMENTS
    }

    fun processArgumentsInParenthesis(arguments: List<FirExpression>) {
        for (argument in arguments) {
            val argumentName = argument.argumentName

            // process position argument
            if (argumentName == null) {
                if (processPositionArgument(argument)) {
                    state = State.VARARG_POSITION
                }
            }
            // process named argument
            else {
                if (state == State.VARARG_POSITION) {
                    completeVarargPositionArguments()
                }

                processNamedArgument(argument, argumentName)
            }
        }
        if (state == State.VARARG_POSITION) {
            completeVarargPositionArguments()
        }
    }

    // return true, if it was mapped to vararg parameter
    private fun processPositionArgument(argument: FirExpression): Boolean {
        if (state == State.NAMED_ONLY_ARGUMENTS) {
            addDiagnostic(MixingNamedAndPositionArguments(argument))
            return false
        }

        val parameter = parameters.getOrNull(currentPositionedParameterIndex)
        if (parameter == null) {
            addDiagnostic(TooManyArguments(argument, function))
            return false
        }

        return if (!parameter.isVararg) {
            currentPositionedParameterIndex++

            result[parameter] = ResolvedCallArgument.SimpleArgument(argument)
            false
        }
        // all position arguments will be mapped to current vararg parameter
        else {
            addVarargArgument(argument)
            true
        }
    }

    private fun processNamedArgument(argument: FirExpression, name: Name) {
        if (!function.hasStableParameterNames) {
            addDiagnostic(NamedArgumentNotAllowed(argument, function))
        }

        val stateAllowsMixedNamedAndPositionArguments = state != State.NAMED_ONLY_ARGUMENTS
        state = State.NAMED_ONLY_ARGUMENTS
        val parameter = findParameterByName(argument, name) ?: return

        result[parameter]?.let {
            addDiagnostic(ArgumentPassedTwice(argument, parameter, it))
            return
        }

        result[parameter] = ResolvedCallArgument.SimpleArgument(argument)

        if (stateAllowsMixedNamedAndPositionArguments && parameters.getOrNull(currentPositionedParameterIndex) == parameter) {
            state = State.POSITION_ARGUMENTS
            currentPositionedParameterIndex++
        }
    }

    fun processExternalArgument(externalArgument: FirExpression) {
        val lastParameter = parameters.lastOrNull()
        if (lastParameter == null) {
            addDiagnostic(TooManyArguments(externalArgument, function))
            return
        }

        if (lastParameter.isVararg) {
            addDiagnostic(VarargArgumentOutsideParentheses(externalArgument, lastParameter))
            return
        }

        val previousOccurrence = result[lastParameter]
        if (previousOccurrence != null) {
            addDiagnostic(TooManyArguments(externalArgument, function))
            return
        }


        result[lastParameter] = ResolvedCallArgument.SimpleArgument(externalArgument)
    }

    fun processDefaultsAndRunChecks() {
        for ((parameter, resolvedArgument) in result) {
            if (!parameter.isVararg) {
                if (resolvedArgument !is ResolvedCallArgument.SimpleArgument) {
                    error("Incorrect resolved argument for parameter $parameter :$resolvedArgument")
                } else if (resolvedArgument.callArgument.isSpread) {
                    addDiagnostic(NonVarargSpread(resolvedArgument.callArgument))
                }
            }
        }

        for ((index, parameter) in parameters.withIndex()) {
            if (!result.containsKey(parameter)) {
                if (bodyResolveComponents.session.defaultParameterResolver.declaresDefaultValue(parameter, function, originScope, index)) {
                    result[parameter] = ResolvedCallArgument.DefaultArgument
                } else if (parameter.isVararg) {
                    result[parameter] = ResolvedCallArgument.VarargArgument(emptyList())
                } else {
                    addDiagnostic(NoValueForParameter(parameter, function))
                }
            }
        }
    }


    private fun completeVarargPositionArguments() {
        assert(state == State.VARARG_POSITION) { "Incorrect state: $state" }
        val parameter = parameters[currentPositionedParameterIndex]
        result.put(parameter, ResolvedCallArgument.VarargArgument(varargArguments!!))
    }

    private fun addVarargArgument(argument: FirExpression) {
        if (varargArguments == null) {
            varargArguments = ArrayList()
        }
        varargArguments!!.add(argument)
    }

    private fun getParameterByName(name: Name): FirValueParameter? {
        if (nameToParameter == null) {
            nameToParameter = parameters.associateBy { it.name }
        }
        return nameToParameter!![name]
    }

    private fun findParameterByName(argument: FirExpression, name: Name): FirValueParameter? {
        val parameter = getParameterByName(name)

        // TODO
//        if (descriptor is CallableMemberDescriptor && descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
//            if (parameter == null) {
//                for (valueParameter in descriptor.valueParameters) {
//                    val matchedParameter = valueParameter.overriddenDescriptors.firstOrNull {
//                        it.containingDeclaration.hasStableParameterNames() && it.name == name
//                    }
//                    if (matchedParameter != null) {
//                        addDiagnostic(NamedArgumentReference(argument, valueParameter))
//                        addDiagnostic(NameForAmbiguousParameter(argument, valueParameter, matchedParameter))
//                        return valueParameter
//                    }
//                }
//            } else {
//                parameter.getOverriddenParameterWithOtherName()?.let {
//                    addDiagnostic(NameForAmbiguousParameter(argument, parameter, it))
//                }
//            }
//        }
//
        if (parameter == null) addDiagnostic(NameNotFound(argument, function))

        return parameter
    }

    private fun addDiagnostic(diagnostic: ResolutionDiagnostic) {
        if (diagnostics == null) {
            diagnostics = mutableListOf()
        }
        diagnostics!!.add(diagnostic)
    }

    private val FirExpression.isSpread: Boolean
        get() = this is FirSpreadArgumentExpression && isSpread

    private val parameters: List<FirValueParameter>
        get() = function.valueParameters

    private val FirExpression.argumentName: Name?
        get() = (this as? FirNamedArgumentExpression)?.name

    // TODO: handle functions with non-stable parameter names, see also
    //  org.jetbrains.kotlin.fir.serialization.FirElementSerializer.functionProto
    //  org.jetbrains.kotlin.fir.serialization.FirElementSerializer.constructorProto
    private val FirFunction<*>.hasStableParameterNames: Boolean
        get() = true
}
