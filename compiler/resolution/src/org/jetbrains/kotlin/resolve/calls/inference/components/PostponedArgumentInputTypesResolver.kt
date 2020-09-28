/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.components.transformToResolvedLambda
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private typealias Context = ConstraintSystemCompletionContext

class PostponedArgumentInputTypesResolver(
    private val resultTypeResolver: ResultTypeResolver,
    private val variableFixationFinder: VariableFixationFinder
) {
    private class ParameterTypesInfo(
        val parametersFromDeclaration: List<UnwrappedType?>?,
        val parametersFromDeclarationOfRelatedLambdas: Set<List<UnwrappedType?>>?,
        val parametersFromConstraints: Set<List<TypeWithKind>>?,
        val annotations: Annotations,
        val isSuspend: Boolean,
        val isNullable: Boolean
    )

    data class TypeWithKind(
        val type: KotlinType,
        val direction: ConstraintKind = ConstraintKind.UPPER
    )

    private fun Context.findFunctionalTypesInConstraints(
        variable: VariableWithConstraints,
        variableDependencyProvider: TypeVariableDependencyInformationProvider
    ): List<TypeWithKind>? {
        fun List<Constraint>.extractFunctionalTypes() = mapNotNull { constraint ->
            val type = constraint.type as? KotlinType ?: return@mapNotNull null
            TypeWithKind(type.extractFunctionalTypeFromSupertypes(), constraint.kind)
        }

        val typeVariableTypeConstructor = variable.typeVariable.freshTypeConstructor() as? TypeVariableTypeConstructor ?: return null
        val dependentVariables =
            variableDependencyProvider.getShallowlyDependentVariables(typeVariableTypeConstructor).orEmpty() + typeVariableTypeConstructor

        return dependentVariables.flatMap { type ->
            val constraints = notFixedTypeVariables[type]?.constraints ?: return@flatMap emptyList()
            val constraintsWithFunctionalType = constraints.filter { (it.type as? KotlinType)?.isBuiltinFunctionalTypeOrSubtype == true }
            constraintsWithFunctionalType.extractFunctionalTypes()
        }
    }

    private fun extractParameterTypesFromDeclaration(atom: ResolutionAtom) =
        when (atom) {
            is FunctionExpression -> {
                val receiverType = atom.receiverType
                if (receiverType != null) listOf(receiverType) + atom.parametersTypes else atom.parametersTypes.toList()
            }
            is LambdaKotlinCallArgument -> atom.parametersTypes?.toList()
            else -> null
        }

    private fun Context.extractParameterTypesInfo(
        argument: PostponedAtomWithRevisableExpectedType,
        postponedArguments: List<PostponedAtomWithRevisableExpectedType>,
        variableDependencyProvider: TypeVariableDependencyInformationProvider
    ): ParameterTypesInfo? {
        val expectedType = argument.expectedType ?: return null
        val variableWithConstraints = notFixedTypeVariables[expectedType.constructor] ?: return null
        val functionalTypesFromConstraints = findFunctionalTypesInConstraints(variableWithConstraints, variableDependencyProvider)

        // Don't create functional expected type for further error reporting about a different number of arguments
        if (functionalTypesFromConstraints != null && functionalTypesFromConstraints.distinctBy { it.type.argumentsCount() }.size > 1)
            return null

        val parameterTypesFromDeclaration =
            if (argument is LambdaWithTypeVariableAsExpectedTypeAtom) argument.parameterTypesFromDeclaration else null

        val parameterTypesFromConstraints = functionalTypesFromConstraints?.mapTo(SmartSet.create()) { typeWithKind ->
            typeWithKind.type.getPureArgumentsForFunctionalTypeOrSubtype().map {
                // We should use opposite kind as lambda's parameters are contravariant
                TypeWithKind(it, typeWithKind.direction.opposite())
            }
        }

        // An extension function flag can only come from a declaration of anonymous function: `select({ this + it }, fun Int.(x: Int) = 10)`
        val (parameterTypesFromDeclarationOfRelatedLambdas, isThereExtensionFunctionAmongRelatedLambdas) =
            getDeclaredParametersFromRelatedLambdas(argument, postponedArguments, variableDependencyProvider)

        val annotationsFromConstraints = functionalTypesFromConstraints?.run {
            Annotations.create(flatMapTo(SmartSet.create()) { it.type.annotations }.toList())
        } ?: Annotations.EMPTY

        val annotations = if (isThereExtensionFunctionAmongRelatedLambdas) {
            annotationsFromConstraints.withExtensionFunctionAnnotation(expectedType.builtIns)
        } else annotationsFromConstraints

        var isSuspend = false
        var isNullable = false
        if (!functionalTypesFromConstraints.isNullOrEmpty()) {
            isNullable = true
            for (funType in functionalTypesFromConstraints) {
                if (!isSuspend && funType.type.isSuspendFunctionTypeOrSubtype) isSuspend = true
                if (isNullable && !funType.type.isMarkedNullable) isNullable = false
                if (isSuspend && !isNullable) break
            }
        }
        return ParameterTypesInfo(
            parameterTypesFromDeclaration,
            parameterTypesFromDeclarationOfRelatedLambdas,
            parameterTypesFromConstraints,
            annotations,
            isSuspend = isSuspend,
            isNullable = isNullable
        )
    }

    private fun Context.getDeclaredParametersFromRelatedLambdas(
        argument: PostponedAtomWithRevisableExpectedType,
        postponedArguments: List<PostponedAtomWithRevisableExpectedType>,
        dependencyProvider: TypeVariableDependencyInformationProvider
    ): Pair<Set<List<UnwrappedType?>>?, Boolean> {
        val parameterTypesFromDeclarationOfRelatedLambdas = postponedArguments
            .mapNotNull { anotherArgument ->
                when {
                    anotherArgument !is LambdaWithTypeVariableAsExpectedTypeAtom -> null
                    anotherArgument.parameterTypesFromDeclaration == null || anotherArgument == argument -> null
                    else -> {
                        val argumentExpectedTypeConstructor = argument.expectedType?.typeConstructor() ?: return@mapNotNull null
                        val anotherArgumentExpectedTypeConstructor = anotherArgument.expectedType.typeConstructor()
                        val areTypeVariablesRelated = dependencyProvider.areVariablesDependentShallowly(
                            argumentExpectedTypeConstructor, anotherArgumentExpectedTypeConstructor
                        )
                        val anotherAtom = anotherArgument.atom
                        val isAnonymousExtensionFunction = anotherAtom is FunctionExpression && anotherAtom.receiverType != null
                        val parameterTypesFromDeclarationOfRelatedLambda = anotherArgument.parameterTypesFromDeclaration

                        if (areTypeVariablesRelated && parameterTypesFromDeclarationOfRelatedLambda != null) {
                            parameterTypesFromDeclarationOfRelatedLambda to isAnonymousExtensionFunction
                        } else null
                    }
                }
            }

        return parameterTypesFromDeclarationOfRelatedLambdas.run { mapTo(SmartSet.create()) { it.first } to any { it.second } }
    }

    private fun Context.createTypeVariableForReturnType(argument: PostponedAtomWithRevisableExpectedType): NewTypeVariable {
        val expectedType = argument.expectedType
            ?: throw IllegalStateException("Postponed argument's expected type must not be null")

        val variable = getBuilder().currentStorage().allTypeVariables[expectedType.constructor]
        if (variable != null) {
            val revisedVariableForReturnType = getBuilder().getRevisedVariableForReturnType(variable)
            if (revisedVariableForReturnType != null) return revisedVariableForReturnType as NewTypeVariable
        }

        return when (argument) {
            is LambdaWithTypeVariableAsExpectedTypeAtom -> TypeVariableForLambdaReturnType(
                expectedType.builtIns,
                TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
            )
            is PostponedCallableReferenceAtom -> TypeVariableForCallableReferenceReturnType(
                expectedType.builtIns,
                TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE
            )
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }.also {
            if (variable != null) getBuilder().putRevisedVariableForReturnType(variable, it)

            getBuilder().registerVariable(it)
        }
    }

    private fun Context.createTypeVariableForParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int
    ): NewTypeVariable {
        val expectedType = argument.expectedType
            ?: throw IllegalStateException("Postponed argument's expected type must not be null")

        val variable = getBuilder().currentStorage().allTypeVariables[expectedType.constructor]
        if (variable != null) {
            val revisedVariableForParameter = getBuilder().getRevisedVariableForParameter(variable, index)
            if (revisedVariableForParameter != null) return revisedVariableForParameter as NewTypeVariable
        }

        return when (argument) {
            is LambdaWithTypeVariableAsExpectedTypeAtom -> TypeVariableForLambdaParameterType(
                argument.atom,
                index,
                expectedType.builtIns,
                TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + (index + 1)
            )
            is PostponedCallableReferenceAtom -> TypeVariableForCallableReferenceParameterType(
                expectedType.builtIns,
                TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE + (index + 1)
            )
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }.also {
            if (variable != null) getBuilder().putRevisedVariableForParameter(variable, index, it)

            getBuilder().registerVariable(it)
        }
    }

    private fun Context.createTypeVariablesForParameters(
        argument: PostponedAtomWithRevisableExpectedType,
        parameterTypes: List<List<TypeWithKind?>>
    ): List<TypeProjection> {
        val atom = argument.atom
        val csBuilder = getBuilder()
        val allGroupedParameterTypes = parameterTypes.first().indices.map { i -> parameterTypes.map { it.getOrNull(i) } }

        return allGroupedParameterTypes.mapIndexed { index, types ->
            val parameterTypeVariable = createTypeVariableForParameterType(argument, index)

            for (typeWithKind in types) {
                if (typeWithKind == null) continue
                when (typeWithKind.direction) {
                    ConstraintKind.EQUALITY -> csBuilder.addEqualityConstraint(
                        parameterTypeVariable.defaultType, typeWithKind.type, ArgumentConstraintPositionImpl(atom)
                    )
                    ConstraintKind.UPPER -> csBuilder.addSubtypeConstraint(
                        parameterTypeVariable.defaultType, typeWithKind.type, ArgumentConstraintPositionImpl(atom)
                    )
                    ConstraintKind.LOWER -> csBuilder.addSubtypeConstraint(
                        typeWithKind.type, parameterTypeVariable.defaultType, ArgumentConstraintPositionImpl(atom)
                    )
                }
            }

            parameterTypeVariable.defaultType.asTypeProjection()
        }
    }

    private fun Context.computeResultingFunctionalConstructor(
        argument: PostponedAtomWithRevisableExpectedType,
        parametersNumber: Int,
        isSuspend: Boolean,
        resultTypeResolver: ResultTypeResolver
    ): TypeConstructor {
        val expectedType = argument.expectedType
            ?: throw IllegalStateException("Postponed argument's expected type must not be null")

        val expectedTypeConstructor = expectedType.constructor

        return when (argument) {
            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                getFunctionDescriptor(expectedTypeConstructor.builtIns, parametersNumber, isSuspend).typeConstructor
            is PostponedCallableReferenceAtom -> {
                val computedResultType = resultTypeResolver.findResultType(
                    this,
                    notFixedTypeVariables.getValue(expectedTypeConstructor),
                    TypeVariableDirectionCalculator.ResolveDirection.TO_SUPERTYPE
                )

                // Avoid KFunction<...>/Function<...> types
                if (computedResultType.isBuiltinFunctionalTypeOrSubtype() && computedResultType.argumentsCount() > 1) {
                    computedResultType.typeConstructor() as TypeConstructor
                } else {
                    getKFunctionDescriptor(expectedTypeConstructor.builtIns, parametersNumber, isSuspend).typeConstructor
                }
            }
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }
    }

    private fun Context.buildNewFunctionalExpectedType(
        argument: PostponedAtomWithRevisableExpectedType,
        parameterTypesInfo: ParameterTypesInfo
    ): UnwrappedType? {
        val expectedType = argument.expectedType

        if (expectedType == null || expectedType.constructor !in notFixedTypeVariables)
            return null

        val atom = argument.atom
        val parametersFromConstraints = parameterTypesInfo.parametersFromConstraints
        val parametersFromDeclaration = getDeclaredParametersConsideringExtensionFunctionsPresence(parameterTypesInfo)
        val areAllParameterTypesSpecified = !parametersFromDeclaration.isNullOrEmpty() && parametersFromDeclaration.all { it != null }
        val isExtensionFunction = parameterTypesInfo.annotations.hasExtensionFunctionAnnotation()
        val parametersFromDeclarations = parameterTypesInfo.parametersFromDeclarationOfRelatedLambdas.orEmpty() + parametersFromDeclaration

        /*
         * We shouldn't create synthetic functional type if all lambda's parameter types are specified explicitly
         *
         * TODO: regarding anonymous functions: see info about need for analysis in partial mode in `collectParameterTypesAndBuildNewExpectedTypes`
         */
        if (areAllParameterTypesSpecified && !isExtensionFunction && !isAnonymousFunction(argument))
            return null

        val allParameterTypes =
            (parametersFromConstraints.orEmpty() + parametersFromDeclarations.map { parameters -> parameters?.map { it.wrapToTypeWithKind() } }).filterNotNull()

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

        val isExtensionFunctionType = parameterTypesInfo.annotations.hasExtensionFunctionAnnotation()
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
            isExtensionFunctionType && areAllParameterTypesSpecified && areParametersNumberInDeclarationAndConstraintsEqual

        /*
         * We need to add an extension function annotation for anonymous functions with an explicitly specified receiver
         *
         * Example: `val x = id(fun String.() = this)`
         */
        val shouldAddExtensionFunctionAnnotation = atom is FunctionExpression && atom.receiverType != null

        val annotations = when {
            shouldDiscriminateExtensionFunctionAnnotation ->
                parameterTypesInfo.annotations.withoutExtensionFunctionAnnotation()
            shouldAddExtensionFunctionAnnotation ->
                parameterTypesInfo.annotations.withExtensionFunctionAnnotation(expectedType.builtIns)
            else -> parameterTypesInfo.annotations
        }

        val newExpectedType = KotlinTypeFactory.simpleType(
            annotations,
            functionalConstructor,
            variablesForParameterTypes + variableForReturnType.defaultType.asTypeProjection(),
            parameterTypesInfo.isNullable
        )

        getBuilder().addSubtypeConstraint(
            newExpectedType,
            expectedType,
            ArgumentConstraintPositionImpl(argument.atom)
        )

        return newExpectedType
    }

    fun collectParameterTypesAndBuildNewExpectedTypes(
        c: Context,
        postponedArguments: List<PostponedAtomWithRevisableExpectedType>,
        completionMode: ConstraintSystemCompletionMode,
        dependencyProvider: TypeVariableDependencyInformationProvider
    ): Boolean {
        // We can collect parameter types from declaration in any mode, they can't change during completion.
        for (argument in postponedArguments) {
            if (argument !is LambdaWithTypeVariableAsExpectedTypeAtom) continue
            if (argument.parameterTypesFromDeclaration != null) continue
            argument.parameterTypesFromDeclaration = extractParameterTypesFromDeclaration(argument.atom)
        }

        return postponedArguments.any { argument ->
            /*
             * We can build new functional expected types in partial mode only for anonymous functions,
             * because more exact type can't appear from constraints in full mode (anonymous functions have fully explicit declaration).
             * It can be so for lambdas: for instance, an extension function type can appear in full mode (it may not be known in partial mode).
             *
             * TODO: investigate why we can't do it for anonymous functions in full mode always (see `diagnostics/tests/resolve/resolveWithSpecifiedFunctionLiteralWithId.kt`)
             */
            if (completionMode == ConstraintSystemCompletionMode.PARTIAL && !isAnonymousFunction(argument))
                return@any false
            if (argument.revisedExpectedType != null) return@any false
            val parameterTypesInfo =
                c.extractParameterTypesInfo(argument, postponedArguments, dependencyProvider) ?: return@any false
            val newExpectedType =
                c.buildNewFunctionalExpectedType(argument, parameterTypesInfo) ?: return@any false

            argument.revisedExpectedType = newExpectedType

            true
        }
    }

    fun transformToAtomWithNewFunctionalExpectedType(
        c: Context,
        argument: PostponedAtomWithRevisableExpectedType,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Boolean {
        val revisedExpectedType = argument.revisedExpectedType?.takeIf { it.isFunctionOrKFunctionTypeWithAnySuspendability } ?: return false

        when (argument) {
            is PostponedCallableReferenceAtom ->
                CallableReferenceWithRevisedExpectedTypeAtom(argument.atom, revisedExpectedType).also {
                    argument.setAnalyzedResults(null, listOf(it))
                }
            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                argument.transformToResolvedLambda(c.getBuilder(), diagnosticsHolder, revisedExpectedType)
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }

        return true
    }

    private fun getAllDeeplyRelatedTypeVariables(
        type: KotlinType,
        variableDependencyProvider: TypeVariableDependencyInformationProvider
    ): List<TypeVariableTypeConstructor> {
        val typeConstructor = type.constructor

        return when {
            typeConstructor is TypeVariableTypeConstructor -> {
                val relatedVariables = variableDependencyProvider.getDeeplyDependentVariables(typeConstructor).orEmpty()
                listOf(typeConstructor) + relatedVariables.filterIsInstance<TypeVariableTypeConstructor>()
            }
            type.arguments.isNotEmpty() -> {
                type.arguments.flatMap {
                    if (it.isStarProjection) emptyList() else getAllDeeplyRelatedTypeVariables(it.type, variableDependencyProvider)
                }
            }
            else -> emptyList()
        }
    }

    private fun getDeclaredParametersConsideringExtensionFunctionsPresence(parameterTypesInfo: ParameterTypesInfo): List<UnwrappedType?>? =
        with (parameterTypesInfo) {

            if (parametersFromConstraints.isNullOrEmpty() || parametersFromDeclaration.isNullOrEmpty())
                parametersFromDeclaration
            else {
                val oneLessParameterInDeclarationThanInConstraints =
                    parametersFromConstraints.first().size == parametersFromDeclaration.size + 1

                if (oneLessParameterInDeclarationThanInConstraints && annotations.hasExtensionFunctionAnnotation()) {
                    listOf(null) + parametersFromDeclaration
                } else {
                    parametersFromDeclaration
                }
            }
        }

    fun fixNextReadyVariableForParameterTypeIfNeeded(
        c: Context,
        argument: PostponedResolvedAtom,
        postponedArguments: List<PostponedResolvedAtom>,
        topLevelType: UnwrappedType,
        topLevelAtoms: List<ResolvedAtom>,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): Boolean {
        val expectedType = argument.run { safeAs<PostponedAtomWithRevisableExpectedType>()?.revisedExpectedType ?: expectedType }

        if (expectedType != null && expectedType.isFunctionOrKFunctionTypeWithAnySuspendability) {
            val wasFixedSomeVariable =
                c.fixNextReadyVariableForParameterType(
                    expectedType,
                    postponedArguments,
                    topLevelType,
                    topLevelAtoms,
                    dependencyProvider,
                )

            if (wasFixedSomeVariable)
                return true
        }

        return false
    }

    private fun Context.fixNextReadyVariableForParameterType(
        type: KotlinType,
        postponedArguments: List<PostponedResolvedAtom>,
        topLevelType: UnwrappedType,
        topLevelAtoms: List<ResolvedAtom>,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): Boolean {
        val relatedVariables = type.getPureArgumentsForFunctionalTypeOrSubtype()
            .flatMap { getAllDeeplyRelatedTypeVariables(it, dependencyProvider) }
        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            this, relatedVariables, postponedArguments, ConstraintSystemCompletionMode.FULL, topLevelType
        )

        if (variableForFixation == null || !variableForFixation.hasProperConstraint)
            return false

        val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
        val resultType =
            resultTypeResolver.findResultType(this, variableWithConstraints, TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN)
        val variable = variableWithConstraints.typeVariable
        val resolvedAtom = KotlinConstraintSystemCompleter.findResolvedAtomBy(variable, topLevelAtoms)
            ?: topLevelAtoms.firstOrNull()

        fixVariable(variable, resultType, FixVariableConstraintPositionImpl(variable, resolvedAtom))

        return true
    }

    private fun KotlinType?.wrapToTypeWithKind() = this?.let { TypeWithKind(it) }

    private fun isAnonymousFunction(argument: PostponedAtomWithRevisableExpectedType) = argument.atom is FunctionExpression

    companion object {
        private const val TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE = "_RP"
        private const val TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE = "_R"
        private const val TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE = "_QP"
        private const val TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE = "_Q"
    }
}
