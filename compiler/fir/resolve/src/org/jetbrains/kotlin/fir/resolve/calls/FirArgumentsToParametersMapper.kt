/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildNamedArgumentExpression
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.defaultParameterResolver
import org.jetbrains.kotlin.fir.resolve.getAsForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

data class ArgumentMapping(
    // This map should be ordered by arguments as written, e.g.:
    //      fun foo(a: Int, b: Int) {}
    //      foo(b = bar(), a = qux())
    // parameterToCallArgumentMap.values() should be [ 'bar()', 'foo()' ]
    val parameterToCallArgumentMap: LinkedHashMap<FirValueParameter, ResolvedCallArgument>,
    val diagnostics: List<ResolutionDiagnostic>
) {
    fun toArgumentToParameterMapping(): LinkedHashMap<FirExpression, FirValueParameter> {
        val argumentToParameterMapping = linkedMapOf<FirExpression, FirValueParameter>()
        parameterToCallArgumentMap.forEach { (valueParameter, resolvedArgument) ->
            when (resolvedArgument) {
                is ResolvedCallArgument.SimpleArgument -> argumentToParameterMapping[resolvedArgument.callArgument] = valueParameter
                is ResolvedCallArgument.VarargArgument -> resolvedArgument.arguments.forEach {
                    argumentToParameterMapping[it] = valueParameter
                }
                ResolvedCallArgument.DefaultArgument -> {
                }
            }
        }
        return argumentToParameterMapping
    }

    fun numDefaults(): Int {
        return parameterToCallArgumentMap.values.count { it == ResolvedCallArgument.DefaultArgument }
    }
}

private val EmptyArgumentMapping = ArgumentMapping(linkedMapOf(), emptyList())

fun BodyResolveComponents.mapArguments(
    arguments: List<FirExpression>,
    function: FirFunction,
    originScope: FirScope?,
): ArgumentMapping {
    if (arguments.isEmpty() && function.valueParameters.isEmpty()) {
        return EmptyArgumentMapping
    }

    val nonLambdaArguments: MutableList<FirExpression> = mutableListOf()
    val excessLambdaArguments: MutableList<FirExpression> = mutableListOf()
    var externalArgument: FirExpression? = null
    for (argument in arguments) {
        if (argument is FirLambdaArgumentExpression) {
            if (externalArgument == null) {
                externalArgument = argument
            } else {
                excessLambdaArguments.add(argument)
            }
        } else {
            nonLambdaArguments.add(argument)
        }
    }

    // If this is an indexed access set operator, it could have default values or a vararg parameter in the middle.
    // For proper argument mapping, wrap the last one, which is supposed to be the updated value, as a named argument.
    val isIndexedSetOperator = function is FirSimpleFunction
            && function.isOperator
            && function.name == OperatorNameConventions.SET
            && function.origin !is FirDeclarationOrigin.DynamicScope

    if (isIndexedSetOperator &&
        function.valueParameters.any { it.defaultValue != null || it.isVararg }
    ) {
        val v = nonLambdaArguments.last()
        if (v !is FirNamedArgumentExpression) {
            val namedV = buildNamedArgumentExpression {
                source = v.source
                expression = v
                isSpread = false
                name = function.valueParameters.last().name
            }
            nonLambdaArguments.removeAt(nonLambdaArguments.size - 1)
            nonLambdaArguments.add(namedV)
        }
    }

    val processor = FirCallArgumentsProcessor(session, function, this, originScope, isIndexedSetOperator)
    processor.processNonLambdaArguments(nonLambdaArguments)
    if (externalArgument != null) {
        processor.processExternalArgument(externalArgument)
    }
    processor.processExcessLambdaArguments(excessLambdaArguments)
    processor.processDefaultsAndRunChecks()

    return ArgumentMapping(processor.result, processor.diagnostics ?: emptyList())
}

private class FirCallArgumentsProcessor(
    private val useSiteSession: FirSession,
    private val function: FirFunction,
    private val bodyResolveComponents: BodyResolveComponents,
    private val originScope: FirScope?,
    private val isIndexedSetOperator: Boolean
) {
    private var state = State.POSITION_ARGUMENTS
    private var currentPositionedParameterIndex = 0
    private var varargArguments: MutableList<FirExpression>? = null
    private var nameToParameter: Map<Name, FirValueParameter>? = null
    var diagnostics: MutableList<ResolutionDiagnostic>? = null
        private set
    val result: LinkedHashMap<FirValueParameter, ResolvedCallArgument> = LinkedHashMap(function.valueParameters.size)

    val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget? by lazy {
        function.getAsForbiddenNamedArgumentsTarget(useSiteSession)
    }

    private enum class State {
        POSITION_ARGUMENTS,
        VARARG_POSITION,
        NAMED_ONLY_ARGUMENTS
    }

    fun processNonLambdaArguments(arguments: List<FirExpression>) {
        for ((argumentIndex, argument) in arguments.withIndex()) {
            if (argument is FirVarargArgumentsExpression) {
                // If the argument list was already resolved, any arguments for a vararg parameter will be in a FirVarargArgumentsExpression.
                // This can happen when getting all the candidates for an already resolved function call.
                val varargArguments = argument.arguments
                for ((varargArgumentIndex, varargArgument) in varargArguments.withIndex()) {
                    processNonLambdaArgument(
                        varargArgument,
                        isLastArgument = argumentIndex == arguments.lastIndex && varargArgumentIndex == varargArguments.lastIndex
                    )
                }
            } else {
                processNonLambdaArgument(argument, isLastArgument = argumentIndex == arguments.lastIndex)
            }
        }
        if (state == State.VARARG_POSITION) {
            completeVarargPositionArguments()
        }
    }

    private fun processNonLambdaArgument(argument: FirExpression, isLastArgument: Boolean) {
        // process position argument
        if (argument !is FirNamedArgumentExpression) {
            if (processPositionArgument(argument, isLastArgument)) {
                state = State.VARARG_POSITION
            }
        }
        // process named argument
        else {
            if (state == State.VARARG_POSITION) {
                completeVarargPositionArguments()
            }

            processNamedArgument(argument)
        }
    }

    // return true, if it was mapped to vararg parameter
    private fun processPositionArgument(argument: FirExpression, isLastArgument: Boolean): Boolean {
        if (state == State.NAMED_ONLY_ARGUMENTS) {
            addDiagnostic(MixingNamedAndPositionArguments(argument))
            return false
        }

        // The last parameter of an indexed set operator should be reserved for the last argument (the assigned value).
        // We don't want the assigned value mapped to an index parameter if some of the index arguments are absent.
        val assignedParameterIndex = if (isIndexedSetOperator) {
            val lastParameterIndex = parameters.lastIndex
            when {
                isLastArgument -> lastParameterIndex
                currentPositionedParameterIndex >= lastParameterIndex -> {
                    // This is an extra index argument that should NOT be mapped to the parameter for the assigned value.
                    -1
                }
                else -> {
                    // This is an index argument that can be properly mapped.
                    currentPositionedParameterIndex
                }
            }
        } else {
            currentPositionedParameterIndex
        }
        val parameter = parameters.getOrNull(assignedParameterIndex)
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

    private fun processNamedArgument(argument: FirNamedArgumentExpression) {
        forbiddenNamedArgumentsTarget?.let {
            addDiagnostic(NamedArgumentNotAllowed(argument, function, it))
        }

        val stateAllowsMixedNamedAndPositionArguments = state != State.NAMED_ONLY_ARGUMENTS
        state = State.NAMED_ONLY_ARGUMENTS
        val parameter = findParameterByName(argument) ?: return

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

    fun processExcessLambdaArguments(excessLambdaArguments: List<FirExpression>) {
        excessLambdaArguments.forEach { arg -> addDiagnostic(ManyLambdaExpressionArguments(arg)) }
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
                when {
                    bodyResolveComponents.session.defaultParameterResolver.declaresDefaultValue(
                        useSiteSession, bodyResolveComponents.scopeSession, parameter, function, originScope, index
                    ) ->
                        result[parameter] = ResolvedCallArgument.DefaultArgument
                    parameter.isVararg ->
                        result[parameter] = ResolvedCallArgument.VarargArgument(emptyList())
                    else ->
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

    private fun findParameterByName(argument: FirNamedArgumentExpression): FirValueParameter? {
        val parameter = getParameterByName(argument.name)

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
        get() = this is FirWrappedArgumentExpression && isSpread

    private val parameters: List<FirValueParameter>
        get() = function.valueParameters
}
