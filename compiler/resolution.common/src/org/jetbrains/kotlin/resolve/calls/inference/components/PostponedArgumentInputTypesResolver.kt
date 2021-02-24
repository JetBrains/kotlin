/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

private typealias Context = ConstraintSystemCompletionContext
private typealias ResolvedAtomProvider = (TypeVariableMarker) -> Any?

class PostponedArgumentInputTypesResolver(
    private val resultTypeResolver: ResultTypeResolver,
    private val variableFixationFinder: VariableFixationFinder,
    private val resolutionTypeSystemContext: ConstraintSystemUtilContext,
) {
    private class ParameterTypesInfo(
        val parametersFromDeclaration: List<KotlinTypeMarker?>?,
        val parametersFromDeclarationOfRelatedLambdas: Set<List<KotlinTypeMarker?>>?,
        val parametersFromConstraints: Set<List<TypeWithKind>>?,
        val annotations: List<AnnotationMarker>?,
        val isExtensionFunction: Boolean,
        val isSuspend: Boolean,
        val isNullable: Boolean
    )

    data class TypeWithKind(
        val type: KotlinTypeMarker,
        val direction: ConstraintKind = ConstraintKind.UPPER
    )

    private fun Context.findFunctionalTypesInConstraints(
        variable: VariableWithConstraints,
        variableDependencyProvider: TypeVariableDependencyInformationProvider
    ): List<TypeWithKind>? {
        fun List<Constraint>.extractFunctionalTypes() = mapNotNull { constraint ->
            TypeWithKind(constraint.type.getFunctionalTypeFromSupertypes(), constraint.kind)
        }

        val typeVariableTypeConstructor = variable.typeVariable.freshTypeConstructor()
        val dependentVariables =
            variableDependencyProvider.getShallowlyDependentVariables(typeVariableTypeConstructor).orEmpty() + typeVariableTypeConstructor

        return dependentVariables.flatMap { type ->
            val constraints = notFixedTypeVariables[type]?.constraints ?: return@flatMap emptyList()
            val constraintsWithFunctionalType = constraints.filter { it.type.isBuiltinFunctionalTypeOrSubtype() }
            constraintsWithFunctionalType.extractFunctionalTypes()
        }
    }

    private fun Context.extractParameterTypesInfo(
        argument: PostponedAtomWithRevisableExpectedType,
        postponedArguments: List<PostponedAtomWithRevisableExpectedType>,
        variableDependencyProvider: TypeVariableDependencyInformationProvider
    ): ParameterTypesInfo? {
        val expectedType = argument.expectedType ?: return null
        val variableWithConstraints = notFixedTypeVariables[expectedType.typeConstructor()] ?: return null
        val functionalTypesFromConstraints = findFunctionalTypesInConstraints(variableWithConstraints, variableDependencyProvider)

        // Don't create functional expected type for further error reporting about a different number of arguments
        if (functionalTypesFromConstraints != null && functionalTypesFromConstraints.distinctBy { it.type.argumentsCount() }.size > 1)
            return null

        val parameterTypesFromDeclaration =
            if (argument is LambdaWithTypeVariableAsExpectedTypeMarker) argument.parameterTypesFromDeclaration else null

        val parameterTypesFromConstraints = functionalTypesFromConstraints?.mapTo(SmartSet.create()) { typeWithKind ->
            typeWithKind.type.extractArgumentsForFunctionalTypeOrSubtype().map {
                // We should use opposite kind as lambda's parameters are contravariant
                TypeWithKind(it, typeWithKind.direction.opposite())
            }
        }

        val annotations = functionalTypesFromConstraints?.map { it.type.getAnnotations() }?.flatten()?.distinct()

        // An extension function flag can only come from a declaration of anonymous function: `select({ this + it }, fun Int.(x: Int) = 10)`
        val (parameterTypesFromDeclarationOfRelatedLambdas, isThereExtensionFunctionAmongRelatedLambdas) =
            getDeclaredParametersFromRelatedLambdas(argument, postponedArguments, variableDependencyProvider)

        val extensionFunctionTypePresentInConstraints = functionalTypesFromConstraints?.any { it.type.isExtensionFunctionType() } == true

        var isSuspend = false
        var isNullable = false
        if (!functionalTypesFromConstraints.isNullOrEmpty()) {
            isNullable = true
            for (funType in functionalTypesFromConstraints) {
                if (!isSuspend && funType.type.isSuspendFunctionTypeOrSubtype()) isSuspend = true
                if (isNullable && !funType.type.isMarkedNullable()) isNullable = false
                if (isSuspend && !isNullable) break
            }
        }
        return ParameterTypesInfo(
            parameterTypesFromDeclaration,
            parameterTypesFromDeclarationOfRelatedLambdas,
            parameterTypesFromConstraints,
            annotations = annotations,
            isExtensionFunction = isThereExtensionFunctionAmongRelatedLambdas || extensionFunctionTypePresentInConstraints,
            isSuspend = isSuspend,
            isNullable = isNullable
        )
    }

    private fun Context.getDeclaredParametersFromRelatedLambdas(
        argument: PostponedAtomWithRevisableExpectedType,
        postponedArguments: List<PostponedAtomWithRevisableExpectedType>,
        dependencyProvider: TypeVariableDependencyInformationProvider
    ): Pair<Set<List<KotlinTypeMarker?>>?, Boolean> = with(resolutionTypeSystemContext) {
        val parameterTypesFromDeclarationOfRelatedLambdas = postponedArguments
            .mapNotNull { anotherArgument ->
                when {
                    anotherArgument !is LambdaWithTypeVariableAsExpectedTypeMarker -> null
                    anotherArgument.parameterTypesFromDeclaration == null || anotherArgument == argument -> null
                    else -> {
                        val argumentExpectedTypeConstructor = argument.expectedType?.typeConstructor() ?: return@mapNotNull null
                        val anotherArgumentExpectedTypeConstructor =
                            anotherArgument.expectedType?.typeConstructor() ?: return@mapNotNull null
                        val areTypeVariablesRelated = dependencyProvider.areVariablesDependentShallowly(
                            argumentExpectedTypeConstructor, anotherArgumentExpectedTypeConstructor
                        )
                        val isAnonymousExtensionFunction = anotherArgument.isFunctionExpressionWithReceiver()
                        val parameterTypesFromDeclarationOfRelatedLambda = anotherArgument.parameterTypesFromDeclaration

                        if (areTypeVariablesRelated && parameterTypesFromDeclarationOfRelatedLambda != null) {
                            parameterTypesFromDeclarationOfRelatedLambda to isAnonymousExtensionFunction
                        } else null
                    }
                }
            }

        return parameterTypesFromDeclarationOfRelatedLambdas.run { mapTo(SmartSet.create()) { it.first } to any { it.second } }
    }

    private fun Context.createTypeVariableForReturnType(argument: PostponedAtomWithRevisableExpectedType): TypeVariableMarker =
        with(resolutionTypeSystemContext) {
            return when (argument) {
                is LambdaWithTypeVariableAsExpectedTypeMarker -> createTypeVariableForLambdaReturnType()
                is PostponedCallableReferenceMarker -> createTypeVariableForCallableReferenceReturnType()
                else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
            }.also { getBuilder().registerVariable(it) }
        }

    private fun Context.createTypeVariableForParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): TypeVariableMarker = with(resolutionTypeSystemContext) {
        return when (argument) {
            is LambdaWithTypeVariableAsExpectedTypeMarker -> createTypeVariableForLambdaParameterType(argument, index)
            is PostponedCallableReferenceMarker -> createTypeVariableForCallableReferenceParameterType(argument, index)
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }.also { getBuilder().registerVariable(it) }
    }

    private fun Context.createTypeVariablesForParameters(
        argument: PostponedAtomWithRevisableExpectedType,
        parameterTypes: List<List<TypeWithKind?>>
    ): List<TypeArgumentMarker> = with(resolutionTypeSystemContext) {
        val csBuilder = getBuilder()
        val allGroupedParameterTypes = parameterTypes.first().indices.map { i -> parameterTypes.map { it.getOrNull(i) } }

        return allGroupedParameterTypes.mapIndexed { index, types ->
            val parameterTypeVariable = createTypeVariableForParameterType(argument, index)
            val typeVariableConstructor = parameterTypeVariable.freshTypeConstructor()

            for (typeWithKind in types) {
                if (typeVariableConstructor in fixedTypeVariables) break
                if (typeWithKind == null) continue

                when (typeWithKind.direction) {
                    ConstraintKind.EQUALITY -> csBuilder.addEqualityConstraint(
                        parameterTypeVariable.defaultType(), typeWithKind.type, createArgumentConstraintPosition(argument)
                    )
                    ConstraintKind.UPPER -> csBuilder.addSubtypeConstraint(
                        parameterTypeVariable.defaultType(), typeWithKind.type, createArgumentConstraintPosition(argument)
                    )
                    ConstraintKind.LOWER -> csBuilder.addSubtypeConstraint(
                        typeWithKind.type, parameterTypeVariable.defaultType(), createArgumentConstraintPosition(argument)
                    )
                }
            }

            val resultType = fixedTypeVariables[typeVariableConstructor] ?: parameterTypeVariable.defaultType()

            resultType.asTypeArgument()
        }
    }

    private fun Context.computeResultingFunctionalConstructor(
        argument: PostponedAtomWithRevisableExpectedType,
        parametersNumber: Int,
        isSuspend: Boolean,
        resultTypeResolver: ResultTypeResolver
    ): TypeConstructorMarker {
        val expectedType = argument.expectedType
            ?: throw IllegalStateException("Postponed argument's expected type must not be null")

        val expectedTypeConstructor = expectedType.typeConstructor()

        return when (argument) {
            is LambdaWithTypeVariableAsExpectedTypeMarker ->
                getFunctionTypeConstructor(parametersNumber, isSuspend)
            is PostponedCallableReferenceMarker -> {
                val computedResultType = resultTypeResolver.findResultType(
                    this@computeResultingFunctionalConstructor,
                    notFixedTypeVariables.getValue(expectedTypeConstructor),
                    TypeVariableDirectionCalculator.ResolveDirection.TO_SUPERTYPE
                )

                // Avoid KFunction<...>/Function<...> types
                if (computedResultType.isBuiltinFunctionalTypeOrSubtype() && computedResultType.argumentsCount() > 1) {
                    computedResultType.typeConstructor()
                } else {
                    getKFunctionTypeConstructor(parametersNumber, isSuspend)
                }
            }
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }
    }

    private fun Context.computeTypeVariablePathInsideGivenType(
        type: KotlinTypeMarker,
        targetVariable: TypeConstructorMarker,
        path: Stack<Pair<TypeConstructorMarker, Int>> = Stack()
    ): List<Pair<TypeConstructorMarker, Int>>? {
        val typeConstructor = type.typeConstructor()

        if (typeConstructor == targetVariable)
            return emptyList()

        for (i in 0 until type.argumentsCount()) {
            val argumentType = type.getArgument(i).getType()

            if (argumentType.typeConstructor() == targetVariable) {
                return path.toList() + (typeConstructor to i)
            } else if (argumentType.argumentsCount() != 0) {
                path.push(typeConstructor to i)
                computeTypeVariablePathInsideGivenType(argumentType, targetVariable, path)?.let { return it }
                path.pop()
            }
        }

        return null
    }

    private fun Context.selectFirstRelatedVariable(
        variables: Set<TypeVariableTypeConstructorMarker>,
        targetVariable: TypeConstructorMarker,
        variableDependencyProvider: TypeVariableDependencyInformationProvider
    ): TypeVariableTypeConstructorMarker? {
        val relatedVariables = variableDependencyProvider.getDeeplyDependentVariables(targetVariable).orEmpty() +
                variableDependencyProvider.getShallowlyDependentVariables(targetVariable)

        return variables.firstOrNull { it in relatedVariables && it in notFixedTypeVariables }
    }

    private fun Context.buildNewFunctionalExpectedType(
        argument: PostponedAtomWithRevisableExpectedType,
        parameterTypesInfo: ParameterTypesInfo,
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
        topLevelTypeVariables: Set<TypeVariableTypeConstructorMarker>
    ): KotlinTypeMarker? = with(resolutionTypeSystemContext) {
        val expectedType = argument.expectedType ?: return null
        val expectedTypeConstructor = expectedType.typeConstructor()

        if (expectedTypeConstructor !in notFixedTypeVariables) return null

        val relatedTopLevelVariable = selectFirstRelatedVariable(topLevelTypeVariables, expectedTypeConstructor, variableDependencyProvider)
        val pathFromRelatedTopLevelVariable = if (relatedTopLevelVariable != null) {
            val constraintTypes = notFixedTypeVariables.getValue(relatedTopLevelVariable).constraints.map { it.type }.toSet()
            val containingType = constraintTypes.find { constraintType ->
                constraintType.contains { it.typeConstructor() == expectedTypeConstructor }
            }
            if (containingType != null) {
                computeTypeVariablePathInsideGivenType(containingType, expectedTypeConstructor)
            } else null
        } else null

        if (pathFromRelatedTopLevelVariable != null && relatedTopLevelVariable != null) {
            // try to take from the cache of functional types by paths from a top level type variable
            getBuilder().getBuiltFunctionalExpectedTypeForPostponedArgument(relatedTopLevelVariable, pathFromRelatedTopLevelVariable)
                ?.let { return it }
        } else {
            // try to take from the cache of functional types by expected types
            getBuilder().getBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeConstructor)
                ?.let { return it }
        }

        val parametersFromConstraints = parameterTypesInfo.parametersFromConstraints
        val parametersFromDeclaration = getDeclaredParametersConsideringExtensionFunctionsPresence(parameterTypesInfo)
        val areAllParameterTypesSpecified = !parametersFromDeclaration.isNullOrEmpty() && parametersFromDeclaration.all { it != null }
        val isExtensionFunction = parameterTypesInfo.isExtensionFunction
        val parametersFromDeclarations = parameterTypesInfo.parametersFromDeclarationOfRelatedLambdas.orEmpty() + parametersFromDeclaration

        /*
         * We shouldn't create synthetic functional type if all lambda's parameter types are specified explicitly
         *
         * TODO: regarding anonymous functions: see info about need for analysis in partial mode in `collectParameterTypesAndBuildNewExpectedTypes`
         */
        if (areAllParameterTypesSpecified && !isExtensionFunction && !argument.isAnonymousFunction())
            return null

        val allParameterTypes =
            (parametersFromConstraints.orEmpty() + parametersFromDeclarations.map { parameters ->
                parameters?.map { it.wrapToTypeWithKind() }
            }).filterNotNull()

        if (allParameterTypes.isEmpty())
            return null

        val variablesForParameterTypes = createTypeVariablesForParameters(argument, allParameterTypes)
        val variableForReturnType = createTypeVariableForReturnType(argument)
        val functionalConstructor = computeResultingFunctionalConstructor(
            argument,
            variablesForParameterTypes.size,
            parameterTypesInfo.isSuspend,
            resultTypeResolver
        )

        val areParametersNumberInDeclarationAndConstraintsEqual =
            !parametersFromDeclaration.isNullOrEmpty() && !parametersFromConstraints.isNullOrEmpty()
                    && parametersFromDeclaration.size == parametersFromConstraints.first().size

        /*
         * We need to exclude further considering a postponed argument as an extension function
         * to support cases with explicitly specified receiver as a value parameter (only if all parameter types are specified)
         *
         * Example: `val x: String.() -> Int = id { x: String -> 42 }`
         */
        val shouldDiscriminateExtensionFunctionAnnotation =
            isExtensionFunction && areAllParameterTypesSpecified && areParametersNumberInDeclarationAndConstraintsEqual

        /*
         * We need to add an extension function annotation for anonymous functions with an explicitly specified receiver
         *
         * Example: `val x = id(fun String.() = this)`
         */

        val newExpectedType = createSimpleType(
            functionalConstructor,
            variablesForParameterTypes + variableForReturnType.defaultType().asTypeArgument(),
            parameterTypesInfo.isNullable,
            isExtensionFunction = when {
                shouldDiscriminateExtensionFunctionAnnotation -> false
                argument.isFunctionExpressionWithReceiver() -> true
                else -> parameterTypesInfo.isExtensionFunction
            },
            annotations = parameterTypesInfo.annotations
        )

        getBuilder().addSubtypeConstraint(
            newExpectedType,
            expectedType,
            createArgumentConstraintPosition(argument)
        )

        if (pathFromRelatedTopLevelVariable != null && relatedTopLevelVariable != null) {
            getBuilder().putBuiltFunctionalExpectedTypeForPostponedArgument(relatedTopLevelVariable, pathFromRelatedTopLevelVariable, newExpectedType)
        } else {
            getBuilder().putBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeConstructor, newExpectedType)
        }

        return newExpectedType
    }

    fun collectParameterTypesAndBuildNewExpectedTypes(
        c: Context,
        postponedArguments: List<PostponedAtomWithRevisableExpectedType>,
        completionMode: ConstraintSystemCompletionMode,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        topLevelTypeVariables: Set<TypeVariableTypeConstructorMarker>
    ): Boolean = with(resolutionTypeSystemContext) {
        // We can collect parameter types from declaration in any mode, they can't change during completion.
        for (argument in postponedArguments) {
            if (argument !is LambdaWithTypeVariableAsExpectedTypeMarker) continue
            if (argument.parameterTypesFromDeclaration != null) continue
            argument.updateParameterTypesFromDeclaration(extractLambdaParameterTypesFromDeclaration(argument))
        }

        return postponedArguments.any { argument ->
            /*
             * We can build new functional expected types in partial mode only for anonymous functions,
             * because more exact type can't appear from constraints in full mode (anonymous functions have fully explicit declaration).
             * It can be so for lambdas: for instance, an extension function type can appear in full mode (it may not be known in partial mode).
             *
             * TODO: investigate why we can't do it for anonymous functions in full mode always (see `diagnostics/tests/resolve/resolveWithSpecifiedFunctionLiteralWithId.kt`)
             */
            if (completionMode == ConstraintSystemCompletionMode.PARTIAL && !argument.isAnonymousFunction())
                return@any false
            if (argument.revisedExpectedType != null) return@any false
            val parameterTypesInfo =
                c.extractParameterTypesInfo(argument, postponedArguments, dependencyProvider) ?: return@any false
            val newExpectedType =
                c.buildNewFunctionalExpectedType(argument, parameterTypesInfo, dependencyProvider, topLevelTypeVariables) ?: return@any false

            argument.reviseExpectedType(newExpectedType)

            true
        }
    }

    private fun Context.getAllDeeplyRelatedTypeVariables(
        type: KotlinTypeMarker,
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
    ): Collection<TypeVariableTypeConstructorMarker> {
        val collectedVariables = mutableSetOf<TypeVariableTypeConstructorMarker>()
        getAllDeeplyRelatedTypeVariables(type, variableDependencyProvider, collectedVariables)
        return collectedVariables
    }

    private fun Context.getAllDeeplyRelatedTypeVariables(
        type: KotlinTypeMarker,
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
        typeVariableCollector: MutableSet<TypeVariableTypeConstructorMarker>
    ) {
        val typeConstructor = type.typeConstructor()

        when {
            typeConstructor is TypeVariableTypeConstructorMarker -> {
                val relatedVariables = variableDependencyProvider.getDeeplyDependentVariables(typeConstructor).orEmpty()
                typeVariableCollector.add(typeConstructor)
                typeVariableCollector.addAll(relatedVariables.filterIsInstance<TypeVariableTypeConstructorMarker>())
            }
            type.argumentsCount() > 0 -> {
                for (typeArgument in type.lowerBoundIfFlexible().asArgumentList()) {
                    if (!typeArgument.isStarProjection()) {
                        getAllDeeplyRelatedTypeVariables(typeArgument.getType(), variableDependencyProvider, typeVariableCollector)
                    }
                }
            }
        }
    }

    private fun getDeclaredParametersConsideringExtensionFunctionsPresence(parameterTypesInfo: ParameterTypesInfo): List<KotlinTypeMarker?>? =
        with(parameterTypesInfo) {

            if (parametersFromConstraints.isNullOrEmpty() || parametersFromDeclaration.isNullOrEmpty())
                parametersFromDeclaration
            else {
                val oneLessParameterInDeclarationThanInConstraints =
                    parametersFromConstraints.first().size == parametersFromDeclaration.size + 1

                if (oneLessParameterInDeclarationThanInConstraints && isExtensionFunction) {
                    listOf(null) + parametersFromDeclaration
                } else {
                    parametersFromDeclaration
                }
            }
        }

    fun fixNextReadyVariableForParameterTypeIfNeeded(
        c: Context,
        argument: PostponedResolvedAtomMarker,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        topLevelType: KotlinTypeMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        resolvedAtomProvider: ResolvedAtomProvider
    ): Boolean = with(c) {
        val expectedType = argument.run { safeAs<PostponedAtomWithRevisableExpectedType>()?.revisedExpectedType ?: expectedType }

        if (expectedType != null && expectedType.isFunctionOrKFunctionWithAnySuspendability()) {
            val wasFixedSomeVariable = c.fixNextReadyVariableForParameterType(
                expectedType,
                postponedArguments,
                topLevelType,
                dependencyProvider,
                resolvedAtomProvider
            )

            if (wasFixedSomeVariable)
                return true
        }

        return false
    }

    private fun Context.fixNextReadyVariableForParameterType(
        type: KotlinTypeMarker,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        topLevelType: KotlinTypeMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        resolvedAtomByTypeVariableProvider: ResolvedAtomProvider,
    ): Boolean = with(resolutionTypeSystemContext) {
        val relatedVariables = type.extractArgumentsForFunctionalTypeOrSubtype()
            .flatMap { getAllDeeplyRelatedTypeVariables(it, dependencyProvider) }
        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            this@fixNextReadyVariableForParameterType, relatedVariables, postponedArguments, ConstraintSystemCompletionMode.FULL, topLevelType
        )

        if (variableForFixation == null || !variableForFixation.hasProperConstraint)
            return false

        val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
        val resultType =
            resultTypeResolver.findResultType(
                this@fixNextReadyVariableForParameterType,
                variableWithConstraints,
                TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
            )
        val variable = variableWithConstraints.typeVariable

        fixVariable(
            variable,
            resultType,
            createFixVariableConstraintPosition(variable, resolvedAtomByTypeVariableProvider(variable))
        )

        return true
    }

    private fun KotlinTypeMarker?.wrapToTypeWithKind() = this?.let { TypeWithKind(it) }

    companion object {
        const val TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE = "_RP"
        const val TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE = "_R"
        const val TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE = "_QP"
        const val TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE = "_Q"
    }
}