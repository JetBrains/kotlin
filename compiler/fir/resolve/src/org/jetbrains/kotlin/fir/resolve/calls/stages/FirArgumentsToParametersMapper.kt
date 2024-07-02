/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.stages

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.areNamedArgumentsForbiddenIgnoringOverridden
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.defaultParameterResolver
import org.jetbrains.kotlin.fir.resolve.forbiddenNamedArgumentsTargetOrNull
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ForbiddenNamedArgumentsTarget
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

data class ArgumentMapping(
    // This map should be ordered by arguments as written, e.g.:
    //      fun foo(a: Int, b: Int) {}
    //      foo(b = bar(), a = qux())
    // parameterToCallArgumentMap.values() should be [ 'bar()', 'foo()' ]
    val parameterToCallArgumentMap: LinkedHashMap<FirValueParameter, ResolvedCallArgument<ConeResolutionAtom>>,
    val diagnostics: List<ResolutionDiagnostic>,
) {
    fun toArgumentToParameterMapping(): LinkedHashMap<ConeResolutionAtom, FirValueParameter> {
        val argumentToParameterMapping = linkedMapOf<ConeResolutionAtom, FirValueParameter>()
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
    arguments: List<ConeResolutionAtom>,
    function: FirFunction,
    originScope: FirScope?,
    callSiteIsOperatorCall: Boolean,
): ArgumentMapping {
    if (arguments.isEmpty() && function.valueParameters.isEmpty()) {
        return EmptyArgumentMapping
    }

    val nonLambdaArguments: MutableList<ConeResolutionAtom> = mutableListOf()
    val excessLambdaArguments: MutableList<ConeResolutionAtom> = mutableListOf()
    var externalArgument: ConeResolutionAtom? = null
    for (argument in arguments) {
        val argumentExpression = argument.expression
        if (argumentExpression is FirAnonymousFunctionExpression && argumentExpression.isTrailingLambda) {
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
    val isIndexedSetOperator = callSiteIsOperatorCall
            && function is FirSimpleFunction
            && function.isOperator
            && function.name == OperatorNameConventions.SET
            && function.origin !is FirDeclarationOrigin.DynamicScope

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
    private val isIndexedSetOperator: Boolean,
) {
    private var state = State.POSITION_ARGUMENTS
    private var currentPositionedParameterIndex = 0
    private var varargArguments: MutableList<ConeResolutionAtom>? = null
    private var nameToParameter: Map<Name, FirValueParameter>? = null
    private var namedDynamicArgumentsNamesImpl: MutableSet<Name>? = null
    private val namedDynamicArgumentsNames: MutableSet<Name>
        get() = namedDynamicArgumentsNamesImpl ?: mutableSetOf<Name>().also { namedDynamicArgumentsNamesImpl = it }
    var diagnostics: MutableList<ResolutionDiagnostic>? = null
        private set
    val result: LinkedHashMap<FirValueParameter, ResolvedCallArgument<ConeResolutionAtom>> = LinkedHashMap(function.valueParameters.size)

    val forbiddenNamedArgumentsTarget: ForbiddenNamedArgumentsTarget? by lazy {
        function.forbiddenNamedArgumentsTargetOrNull(originScope as? FirTypeScope)
    }

    private enum class State {
        POSITION_ARGUMENTS,
        VARARG_POSITION,
        NAMED_ONLY_ARGUMENTS
    }

    fun processNonLambdaArguments(arguments: List<ConeResolutionAtom>) {
        for ((argumentIndex, argument) in arguments.withIndex()) {
            processNonLambdaArgument(argument, isLastArgument = argumentIndex == arguments.lastIndex)
        }
        if (state == State.VARARG_POSITION) {
            completeVarargPositionArguments()
        }
    }

    private fun processNonLambdaArgument(argument: ConeResolutionAtom, isLastArgument: Boolean) {
        when {
            // process position argument
            argument !is ConeNamedArgumentAtom -> {
                if (state == State.VARARG_POSITION && isIndexedSetOperator && isLastArgument) {
                    // The last argument of an indexed set operator should be reserved for the last argument (the assigned value).
                    // That's why if vararg presented, they should be completed
                    completeVarargPositionArguments()
                }
                processPositionArgument(argument, isLastArgument)
            }
            // process named argument
            function.origin == FirDeclarationOrigin.DynamicScope -> {
                processPositionArgument(argument.subAtom, isLastArgument)
                if (!namedDynamicArgumentsNames.add(argument.name)) {
                    addDiagnostic(ArgumentPassedTwice(argument.expression))
                }
            }
            else -> {
                if (state == State.VARARG_POSITION) {
                    completeVarargPositionArguments()
                }
                processNamedArgument(argument)
            }
        }
    }

    private fun processPositionArgument(argument: ConeResolutionAtom, isLastArgument: Boolean) {
        if (state == State.NAMED_ONLY_ARGUMENTS) {
            addDiagnostic(MixingNamedAndPositionArguments(argument.expression))
            return
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
            addDiagnostic(TooManyArguments(argument.expression, function))
            return
        }

        return if (!parameter.isVararg) {
            currentPositionedParameterIndex++

            result[parameter] = ResolvedCallArgument.SimpleArgument(argument)
            state = State.POSITION_ARGUMENTS
        }
        // all position arguments will be mapped to current vararg parameter
        else {
            addVarargArgument(argument)
            state = State.VARARG_POSITION
        }
    }

    private fun processNamedArgument(argument: ConeNamedArgumentAtom) {
        forbiddenNamedArgumentsTarget?.let {
            addDiagnostic(NamedArgumentNotAllowed(argument.expression, function, it))
        }

        val stateAllowsMixedNamedAndPositionArguments = state != State.NAMED_ONLY_ARGUMENTS
        state = State.NAMED_ONLY_ARGUMENTS
        val parameter = findParameterByName(argument) ?: return

        result[parameter]?.let {
            addDiagnostic(ArgumentPassedTwice(argument.expression))
            return
        }

        result[parameter] = ResolvedCallArgument.SimpleArgument(argument)

        if (stateAllowsMixedNamedAndPositionArguments && parameters.getOrNull(currentPositionedParameterIndex) == parameter) {
            state = State.POSITION_ARGUMENTS
            currentPositionedParameterIndex++
        }
    }

    fun processExternalArgument(externalArgument: ConeResolutionAtom) {
        val argumentExpression = externalArgument.expression
        val lastParameter = parameters.lastOrNull()
        if (lastParameter == null) {
            addDiagnostic(TooManyArguments(argumentExpression, function))
            return
        }

        if (function.origin != FirDeclarationOrigin.DynamicScope) {
            if (lastParameter.isVararg) {
                addDiagnostic(VarargArgumentOutsideParentheses(argumentExpression, lastParameter))
                return
            }

            val previousOccurrence = result[lastParameter]
            if (previousOccurrence != null) {
                addDiagnostic(TooManyArguments(argumentExpression, function))
                return
            }

            result[lastParameter] = ResolvedCallArgument.SimpleArgument(externalArgument)
        } else {
            val existing = result[lastParameter]
            if (existing == null) {
                result[lastParameter] = ResolvedCallArgument.SimpleArgument(externalArgument)
            } else {
                result[lastParameter] = ResolvedCallArgument.VarargArgument(existing.arguments + externalArgument)
            }
        }
    }

    fun processExcessLambdaArguments(excessLambdaArguments: List<ConeResolutionAtom>) {
        excessLambdaArguments.forEach { arg -> addDiagnostic(ManyLambdaExpressionArguments(arg.expression)) }
    }

    fun processDefaultsAndRunChecks() {
        for ((parameter, resolvedArgument) in result) {
            if (!parameter.isVararg) {
                if (resolvedArgument !is ResolvedCallArgument.SimpleArgument) {
                    errorWithAttachment("Incorrect resolved argument for parameter ${parameter::class.java}: ${resolvedArgument::class.java}") {
                        withFirEntry("parameter", parameter)
                        withEntryGroup("arguments") {
                            for ((index, argument) in resolvedArgument.arguments.withIndex()) {
                                withFirEntry("argument$index", argument.expression)
                            }
                        }
                    }
                } else if (resolvedArgument.callArgument.isSpread) {
                    addDiagnostic(NonVarargSpread(resolvedArgument.callArgument.expression))
                }
            }
        }

        for ((index, parameter) in parameters.withIndex()) {
            if (!result.containsKey(parameter)) {
                when {
                    bodyResolveComponents.session.defaultParameterResolver.declaresDefaultValue(
                        useSiteSession, bodyResolveComponents.scopeSession, function, originScope, index
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
        result[parameter] = ResolvedCallArgument.VarargArgument(varargArguments!!)
    }

    private fun addVarargArgument(argument: ConeResolutionAtom) {
        if (varargArguments == null) {
            varargArguments = ArrayList()
        }
        varargArguments!!.add(argument)
    }

    private fun getParameterByName(name: Name): FirValueParameter? {
        if (nameToParameter == null) {
            // Situation where the immediate function doesn't allow named arguments, but overrides a function that allows them,
            // e.g., Java function override of Kotlin function.
            if (function.areNamedArgumentsForbiddenIgnoringOverridden() && forbiddenNamedArgumentsTarget == null) {
                val symbol = function.symbol as? FirNamedFunctionSymbol
                if (symbol != null) {
                    (originScope as? FirTypeScope)?.processOverriddenFunctions(symbol) { overrideSymbol ->
                        if (overrideSymbol.fir.areNamedArgumentsForbiddenIgnoringOverridden()) {
                            return@processOverriddenFunctions ProcessorAction.NEXT
                        }
                        // Get the parameter names from the first applicable override and associate original parameters with them.
                        // If there are multiple overrides with ambiguous parameter names,
                        // a diagnostic will be reported in findParameterByName.
                        nameToParameter = parameters.withIndex().associateTo(LinkedHashMap()) { (i, p) ->
                            overrideSymbol.fir.valueParameters[i].name to p
                        }
                        ProcessorAction.STOP
                    }
                }
                if (nameToParameter == null) {
                    nameToParameter = emptyMap()
                }
            } else {
                nameToParameter = parameters.associateTo(LinkedHashMap()) { it.name to it }
            }
        }
        return nameToParameter!![name]
    }

    private fun findParameterByName(argument: ConeNamedArgumentAtom): FirValueParameter? {
        var parameter = getParameterByName(argument.name)

        val symbol = function.symbol as? FirNamedFunctionSymbol
        var matchedIndex = -1

        // Note: should be called when parameter != null && matchedIndex != -1
        fun List<FirValueParameterSymbol>.findAndReportValueParameterWithDifferentName(): ProcessorAction {
            val someParameter = getOrNull(matchedIndex)?.fir
            val someName = someParameter?.name
            if (someName != null && someName != argument.name) {
                addDiagnostic(
                    NameForAmbiguousParameter(argument.expression)
                )
                return ProcessorAction.STOP
            }
            return ProcessorAction.NEXT
        }

        if (parameter == null) {
            if (symbol != null && (function.isSubstitutionOrIntersectionOverride || function.isJavaOrEnhancement)) {
                var allowedParameters: List<FirValueParameterSymbol>? = null
                (originScope as? FirTypeScope)?.processOverriddenFunctions(symbol) {
                    if (it.fir.areNamedArgumentsForbiddenIgnoringOverridden()) {
                        return@processOverriddenFunctions ProcessorAction.NEXT
                    }
                    val someParameterSymbols = it.valueParameterSymbols
                    if (matchedIndex != -1) {
                        someParameterSymbols.findAndReportValueParameterWithDifferentName()
                    } else {
                        matchedIndex = someParameterSymbols.indexOfFirst { originalParameter ->
                            originalParameter.name == argument.name
                        }
                        if (matchedIndex != -1) {
                            parameter = parameters[matchedIndex]
                            val someParameter = allowedParameters?.getOrNull(matchedIndex)?.fir
                            if (someParameter != null) {
                                addDiagnostic(
                                    NameForAmbiguousParameter(argument.expression)
                                )
                                ProcessorAction.STOP
                            } else {
                                ProcessorAction.NEXT
                            }
                        } else {
                            allowedParameters = someParameterSymbols
                            ProcessorAction.NEXT
                        }
                    }
                }
            }
            if (parameter == null) {
                addDiagnostic(NameNotFound(argument.expression, function))
            }
        } else {
            if (symbol != null && (function.isSubstitutionOrIntersectionOverride || function.isJavaOrEnhancement)) {
                // We know the map is initialized, and it preserves insertion order.
                matchedIndex = nameToParameter!!.entries.indexOfFirst { it.key == argument.name }
                if (matchedIndex != -1) {
                    (originScope as? FirTypeScope)?.processOverriddenFunctions(symbol) {
                        if (it.fir.areNamedArgumentsForbiddenIgnoringOverridden()) {
                            return@processOverriddenFunctions ProcessorAction.NEXT
                        }
                        it.valueParameterSymbols.findAndReportValueParameterWithDifferentName()
                    }
                }
            }
        }

        return parameter
    }

    private fun addDiagnostic(diagnostic: ResolutionDiagnostic) {
        if (diagnostics == null) {
            diagnostics = mutableListOf()
        }
        diagnostics!!.add(diagnostic)
    }

    private val ConeResolutionAtom.isSpread: Boolean
        get() = when (this) {
            is ConeWrappedExpressionAtom -> isSpread
            else -> false
        }

    private val parameters: List<FirValueParameter>
        get() = function.valueParameters
}
