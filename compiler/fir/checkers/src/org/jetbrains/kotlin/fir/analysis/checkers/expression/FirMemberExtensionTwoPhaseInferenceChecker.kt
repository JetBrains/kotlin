/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isExplicit
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirValueParameterKind
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.createConeSubstitutorFromTypeArguments
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeDeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeReceiverConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.asCone
import org.jetbrains.kotlin.fir.types.classLikeLookupTagIfAny
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.contains
import org.jetbrains.kotlin.fir.types.create
import org.jetbrains.kotlin.fir.types.doUnify
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isRaw
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.renderReadable
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.upperBoundIfFlexible
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.typeConstructor

object FirMemberExtensionTwoPhaseInferenceChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val symbol = expression.calleeReference.toResolvedFunctionSymbol() ?: return
        val receiverParameterType = symbol.receiverParameterSymbol?.resolvedType ?: return
        val typeParameters = symbol.typeParameterSymbols
        val receiverMentionsTypeParameter = receiverParameterType.contains {
            it is ConeTypeParameterType && it.lookupTag.typeParameterSymbol in typeParameters
        }
        if (!receiverMentionsTypeParameter) return

        val diagnostic = DiagnosticData(
            callableId = symbol.callableId.asSingleFqName().asString(),
            signature = buildSignature(
                typeParameters,
                receiverParameterType,
                symbol.valueParameterSymbols.map { it.resolvedReturnType }
            ),
            normalInference = normalInference(expression, symbol),
            twoPhaseInference = analyzeOrIgnore(expression, symbol),
        )

        reporter.reportOn(
            expression.calleeReference.source ?: expression.source,
            FirErrors.MEMBER_EXTENSION_TWO_PHASE_INFERENCE,
            diagnostic.toJson(),
        )
    }

    context(context: CheckerContext)
    private fun analyzeOrIgnore(expression: FirFunctionCall, symbol: FirFunctionSymbol<*>): AnalysisResult {
        val typeParameters = symbol.typeParameterSymbols
        require(typeParameters.isNotEmpty())
        val extensionReceiver = requireNotNull(expression.extensionReceiver)

        if (expression.origin == FirFunctionCallOrigin.StdlibCollectionLiteral) {
            return AnalysisResult.Error("stdlib collection literal call origin")
        }
        if (expression.contextArguments.isNotEmpty()) return AnalysisResult.Error("context arguments")
        if (expression.dispatchReceiver != null) return AnalysisResult.Error("dispatch receiver")
        if (expression.explicitReceiver is FirResolvedQualifier) return AnalysisResult.Error("qualifier receiver")

        val argumentList = expression.argumentList as? FirResolvedArgumentList
            ?: return AnalysisResult.Error("unresolved argument list")
        val valueParameters = symbol.valueParameterSymbols
        if (argumentList.mapping.values.any { it.valueParameterKind != FirValueParameterKind.Regular }) {
            return AnalysisResult.Error("non-regular value parameter")
        }
        if (valueParameters.any { it.isVararg }) return AnalysisResult.Error("vararg parameter")
        if (argumentList.mapping.size != valueParameters.size) return AnalysisResult.Error("default, missing, or extra arguments")
        if (argumentList.mapping.values.toSet().size != valueParameters.size) return AnalysisResult.Error("unsupported argument mapping")
        if (extensionReceiver.resolvedType.containsErrorType()) return AnalysisResult.Error("error receiver type")
        if (argumentList.mapping.keys.any { it.resolvedType.containsErrorType() }) return AnalysisResult.Error("error argument type")

        return runCatching {
            runTwoPhaseInference(expression, argumentList, symbol)
        }.getOrElse {
            AnalysisResult.Error(
                "internal two-phase inference failure: ${it::class.simpleName}${it.message?.let { message -> ": $message" } ?: ""}"
            )
        }
    }

    context(context: CheckerContext)
    private fun runTwoPhaseInference(
        expression: FirFunctionCall,
        argumentList: FirResolvedArgumentList,
        symbol: FirFunctionSymbol<*>,
    ): AnalysisResult {
        // Consider a call to `fun <T> T.consume(value: T)`. The declaration uses the type-parameter
        // symbol `T`, but inference must not attach constraints to that reusable declaration symbol.
        // It creates a fresh, call-specific variable for every type parameter instead.
        val typeParameters = symbol.typeParameterSymbols
        val freshVariables = typeParameters.map(::ConeTypeParameterBasedTypeVariable)
        val freshVariableByName = freshVariables.associateBy { it.typeParameterSymbol.name.asString() }

        // The constraint system collects relations such as `String <: fresh T` and eventually chooses
        // a concrete type for each fresh variable. The builder is the API used to add those relations.
        val system = context.session.inferenceComponents.createConstraintSystem()
        val builder = system.getBuilder()

        // `defaultType` is the ConeKotlinType that represents a fresh variable inside types. It is not
        // a default type argument or an inferred result. For example, this substitutor rewrites the
        // declaration type `List<T>` to `List<fresh T>`, so constraints affect this call's variable.
        //
        // A type-alias constructor has an additional substitution from the expanded constructor's type
        // parameters to the alias parameters. Chain it before replacing those parameters with fresh ones.
        val toFreshVariables = substitutorByMap(freshVariables.associate { it.typeParameterSymbol to it.defaultType }, context.session)
            .let {
                val typeAliasConstructorSubstitutor = (symbol as? FirConstructorSymbol)?.typeAliasConstructorInfo?.substitutor
                if (typeAliasConstructorSubstitutor != null) ChainedSubstitutor(typeAliasConstructorSubstitutor, it) else it
            }

        // Registration tells the system which unknowns it is allowed to solve. Bounds such as `T : Any`
        // become constraints on `fresh T`; an explicit type argument such as `consume<String>` adds
        // the equality constraint `fresh T == String`.
        for (freshVariable in freshVariables) {
            builder.registerVariable(freshVariable)
        }
        addDeclaredUpperBoundConstraints(
            builder,
            typeParameters,
            freshVariables,
            toFreshVariables::substituteOrSelf
        )
        addExplicitTypeArgumentConstraints(expression, freshVariables, builder)

        val receiverParameterType = requireNotNull(symbol.receiverParameterSymbol).resolvedType
        val receiverType = requireNotNull(expression.extensionReceiver).resolvedType
        val receiverVariables = typeParameters.mapIndexedNotNull { index, typeParameter ->
            val freshVariable = freshVariables[index]
            freshVariable.takeIf {
                receiverParameterType.contains { type ->
                    type is ConeTypeParameterType && type.lookupTag.typeParameterSymbol == typeParameter
                }
            }
        }

        // Phase 1 uses only the extension receiver. For `"x".consume(anotherValue)`, this adds
        // `String <: fresh T`, because an actual argument type must be a subtype of its parameter type.
        val freshReceiverParameterType = toFreshVariables.substituteOrSelf(receiverParameterType)
        builder.addSubtypeConstraint(
            prepareReceiverTypeForConstraint(receiverType, freshReceiverParameterType),
            freshReceiverParameterType,
            ConeReceiverConstraintPosition(expression.extensionReceiver!!, expression.extensionReceiver!!.source),
        )

        // Fix only variables mentioned directly in the declared extension-receiver type. A bound may
        // connect one of them to another variable, for example `C : R` on receiver `C`, but R must stay
        // unfixed until phase 2 because it is not itself part of the receiver type. Fixing replaces an
        // unknown such as `fresh C` with the selected result type, such as `String`.
        val completionContext = system.asConstraintSystemCompleterContext()
        val receiverPhase = completionContext.fixReadyVariables(receiverVariables)
        val unfixedReceiverVariables = receiverVariables.filter {
            it.typeConstructor !in completionContext.fixedTypeVariables
        }
        val receiverNames = receiverPhase.toPhaseNames(freshVariableByName)

        // Receiver fixation is a hard phase boundary. If any receiver-mentioned variable could not be
        // fixed, arguments must not get a chance to infer it: that would no longer model receiver-first
        // inference. Return immediately with the `failed` outcome instead of running phase 2.
        if (system.hasContradiction || unfixedReceiverVariables.isNotEmpty()) {
            val finalTypes = freshVariables.associate { variable ->
                val type = completionContext.fixedTypeVariables[variable.typeConstructor]
                variable.typeParameterSymbol.name.asString() to (type?.asCone()?.renderReadable() ?: "<not fixed>")
            }
            return AnalysisResult.Success(
                outcome = TwoPhaseOutcome.RECEIVER_INFERENCE_FAILED,
                inferredTypes = finalTypes,
                receiverPhaseFixed = receiverNames,
                receiverPhaseUnfixed = unfixedReceiverVariables.map { it.typeParameterSymbol.name.asString() },
                argumentPhaseFixed = emptyList(),
            )
        }

        // This substitutor reflects variables fixed in phase 1. Apply it to value-parameter types before
        // adding argument constraints: if the parameter was `T` and phase 1 fixed T to String, its type
        // here is String. `safeSubstitute` also knows about the active constraint system and avoids
        // producing an invalid type if substitution encounters an inference problem.
        //
        // Lambda and callable-reference argument types are intentionally reused from normal inference.
        // Their resolved types may depend on the expected parameter type selected by normal inference,
        // so this is not an independent replay of postponed-argument analysis. That is sufficient here:
        // the experiment asks whether those already-resolved argument types remain applicable after the
        // extension receiver's type variables have been fixed first.
        val currentSubstitutor = system.buildCurrentSubstitutor()
        for ((argument = key, parameter = value) in argumentList.mapping) {
            val argumentType = argument.resolvedType
            val parameterType = currentSubstitutor.safeSubstitute(
                system,
                toFreshVariables.substituteOrSelf(parameter.symbol.resolvedReturnType),
            ).asCone()
            builder.addSubtypeConstraint(
                prepareArgumentTypeForConstraint(argumentType, parameterType),
                parameterType,
                ConeArgumentConstraintPosition(argument),
            )
        }

        // Phase 2 now fixes variables that needed ordinary arguments (or became ready after their
        // constraints were added). A contradiction means that the receiver-phase choices and argument
        // constraints cannot all hold at once.
        val argumentPhase = completionContext.fixReadyVariables(freshVariables)
        val finalTypes = freshVariables.associate { variable ->
            val type = completionContext.fixedTypeVariables[variable.typeConstructor]
            variable.typeParameterSymbol.name.asString() to (type?.asCone()?.renderReadable() ?: "<not fixed>")
        }
        val argumentNames = argumentPhase.toPhaseNames(freshVariableByName)
        val allVariablesFixed = freshVariables.all { it.typeConstructor in completionContext.fixedTypeVariables }
        val phaseSucceeded = !system.hasContradiction && allVariablesFixed
        return AnalysisResult.Success(
            outcome = if (phaseSucceeded) {
                TwoPhaseOutcome.RECEIVER_FIXED_ARGUMENTS_SUCCEEDED
            } else {
                TwoPhaseOutcome.RECEIVER_FIXED_ARGUMENTS_FAILED
            },
            inferredTypes = finalTypes,
            receiverPhaseFixed = receiverNames,
            receiverPhaseUnfixed = emptyList(),
            argumentPhaseFixed = argumentNames,
        )
    }

    context(context: CheckerContext)
    private fun prepareReceiverTypeForConstraint(
        receiverType: ConeKotlinType,
        expectedType: ConeKotlinType,
    ): ConeKotlinType {
        // Expand a type-parameter receiver through its relevant bound, then capture projections.
        // Normal resolution repeats these operations defensively at multiple layers, but both are
        // idempotent for the types handled here, so one pass is sufficient.
        val typeFromUpperBound = captureFromTypeParameterUpperBoundIfNeeded(receiverType, expectedType, context.session)
        return prepareCapturedType(typeFromUpperBound, context.session)
    }

    context(context: CheckerContext)
    private fun prepareArgumentTypeForConstraint(
        argumentType: ConeKotlinType,
        expectedType: ConeKotlinType,
    ): ConeKotlinType {
        // Ordinary arguments enter at resolvePlainArgumentType: capture projections first, then
        // replace a type-parameter argument with a relevant captured upper bound when necessary.
        val capturedType = prepareCapturedType(argumentType, context.session)
        return captureFromTypeParameterUpperBoundIfNeeded(capturedType, expectedType, context.session)
    }

    private fun prepareCapturedType(argumentType: ConeKotlinType, session: FirSession): ConeKotlinType {
        if (argumentType.isRaw()) return argumentType
        return session.typeContext.captureFromExpression(argumentType.fullyExpandedType(session)) ?: argumentType
    }

    private fun captureFromTypeParameterUpperBoundIfNeeded(
        argumentType: ConeKotlinType,
        expectedType: ConeKotlinType,
        session: FirSession,
    ): ConeKotlinType {
        val simplifiedArgumentType = argumentType.lowerBoundIfFlexible() as? ConeTypeParameterType ?: return argumentType
        val expectedConstructor = expectedType.upperBoundIfFlexible().typeConstructor(session.typeContext)
        val typeCheckerState = session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false,
        )
        val chosenSupertype = simplifiedArgumentType.collectUpperBounds(session.typeContext).singleOrNull { upperBound ->
            AbstractTypeChecker.findCorrespondingSupertypes(
                typeCheckerState,
                upperBound.lowerBoundIfFlexible(),
                expectedConstructor,
            ).isNotEmpty()
        } ?: return argumentType
        val capturedType = session.typeContext.captureFromExpression(chosenSupertype) ?: return argumentType
        return if (argumentType is ConeDefinitelyNotNullType) {
            ConeDefinitelyNotNullType.create(capturedType, session.typeContext) ?: capturedType
        } else {
            capturedType
        }
    }

    context(context: CheckerContext)
    private fun NewConstraintSystemImpl.fixReadyVariables(
        freshVariables: List<ConeTypeParameterBasedTypeVariable>,
    ): List<String> {
        val inferenceComponents = context.session.inferenceComponents
        val fixed = mutableListOf<String>()

        // Completion may make variables ready one at a time. Fixing one variable can make another
        // variable's constraints conclusive, so keep asking the standard fixation machinery until
        // there is no ready variable left.
        while (true) {
            // A contradictory system has no valid solution. There is no useful variable to fix after
            // this point; the caller will report the contradiction as the phase result.
            if (hasContradiction) break

            // The constraint system indexes variables by their type constructors. Restrict completion
            // to this call's fresh variables and discard those that were already fixed in this or an
            // earlier phase.
            val notFixedVariables = freshVariables.map { it.typeConstructor }.filter { it in notFixedTypeVariables }
            if (notFixedVariables.isEmpty()) break

            // The fixation finder applies the compiler's normal ordering rules to select the next
            // variable. FULL completion asks for a final type rather than a provisional partial result.
            // nullableAnyType is the top-level expected type used when there is no narrower expectation
            // from an outer call.
            val variableForFixation = with(this) {
                inferenceComponents.variableFixationFinder.findFirstVariableForFixation(
                    notFixedVariables,
                    emptyList(),
                    ConstraintSystemCompletionMode.FULL,
                    context.session.builtinTypes.nullableAnyType.coneType,
                )
            } ?: break

            // A selected variable may still depend on constraints that have not been added yet. This is
            // exactly where receiver-only completion stops and leaves the variable for the argument phase.
            if (!variableForFixation.isReady) break
            val variableWithConstraints = notFixedTypeVariables[variableForFixation.variable] ?: break

            // Choose the concrete type that best satisfies the variable's accumulated lower and upper
            // constraints. UNKNOWN direction lets the result-type resolver derive the direction from
            // those constraints instead of forcing preference for a subtype or supertype.
            val resultType = with(this) {
                inferenceComponents.resultTypeResolver.findResultType(
                    variableWithConstraints,
                    TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN,
                )
            }
            val typeVariable = variableWithConstraints.typeVariable

            // Commit the chosen type to the constraint system. Subsequent substitutions see this concrete
            // type instead of the fresh variable, and the new fact may make another variable ready.
            fixVariable(typeVariable, resultType, ConeFixVariableConstraintPosition(typeVariable))

            // This list is only diagnostic bookkeeping: remember which declared type parameter was fixed
            // during this phase. It does not participate in inference.
            fixed += freshVariables.firstOrNull { it.typeConstructor == typeVariable.freshTypeConstructor() }
                ?.typeParameterSymbol?.name?.asString()
                ?: typeVariable.toString()
        }
        return fixed
    }

    private fun addDeclaredUpperBoundConstraints(
        builder: NewConstraintSystemImpl,
        typeParameters: List<FirTypeParameterSymbol>,
        freshVariables: List<ConeTypeParameterBasedTypeVariable>,
        substitute: (ConeKotlinType) -> ConeKotlinType,
    ) {
        for ((index, typeParameter = value) in typeParameters.withIndex()) {
            val freshVariable = freshVariables.getOrNull(index) ?: continue
            for (bound in typeParameter.resolvedBounds) {
                val substitutedBound = substitute(bound.coneType)
                if (substitutedBound.isImplicitNullableAnyBound()) continue
                builder.addSubtypeConstraint(freshVariable.defaultType, substitutedBound, ConeDeclaredUpperBoundConstraintPosition())
            }
        }
    }

    private fun addExplicitTypeArgumentConstraints(
        expression: FirFunctionCall,
        freshVariables: List<ConeTypeParameterBasedTypeVariable>,
        builder: NewConstraintSystemImpl,
    ) {
        for ((index, typeArgument = value) in expression.typeArguments.withIndex()) {
            if (!typeArgument.isExplicit) continue
            val explicitTypeArgument = typeArgument as? FirTypeProjectionWithVariance ?: continue
            val freshVariable = freshVariables.getOrNull(index) ?: continue
            builder.addEqualityConstraint(
                freshVariable.defaultType,
                explicitTypeArgument.typeRef.coneType,
                ConeExplicitTypeParameterConstraintPosition(explicitTypeArgument),
            )
        }
    }

    context(context: CheckerContext)
    private fun normalInference(
        expression: FirFunctionCall,
        symbol: FirFunctionSymbol<*>,
    ): NormalInferenceData {
        val inferredTypeByParameter = symbol.typeParameterSymbols.mapIndexedNotNull { index, typeParameter ->
            val typeArgument = expression.typeArguments.getOrNull(index) as? FirTypeProjectionWithVariance
            typeArgument?.typeRef?.coneType?.let { typeParameter to it }
        }.toMap()
        val inferredTypes = symbol.typeParameterSymbols.associate { typeParameter ->
            typeParameter.name.asString() to (inferredTypeByParameter[typeParameter]?.renderReadable() ?: "<missing>")
        }

        val actualReceiverType = expression.extensionReceiver?.resolvedType
        val declaredReceiverType = symbol.receiverParameterSymbol?.resolvedType
        val inferredReceiverType = when {
            declaredReceiverType == null -> null
            symbol.typeParameterSymbols.isEmpty() -> declaredReceiverType
            else -> expression.createConeSubstitutorFromTypeArguments(context.session)?.substituteOrSelf(declaredReceiverType)
        }
        val receiverComparison = ReceiverComparison(
            actualType = actualReceiverType?.renderReadable(),
            inferredType = inferredReceiverType?.renderReadable(),
            relation = compareReceiverTypes(actualReceiverType, inferredReceiverType),
        )
        val receiverTypeParameters = compareReceiverTypeParameters(
            actualReceiverType,
            declaredReceiverType,
            symbol.typeParameterSymbols,
            inferredTypeByParameter,
        )
        return NormalInferenceData(inferredTypes, receiverComparison, receiverTypeParameters)
    }

    context(context: CheckerContext)
    private fun compareReceiverTypeParameters(
        actualReceiverType: ConeKotlinType?,
        declaredReceiverType: ConeKotlinType?,
        typeParameters: List<FirTypeParameterSymbol>,
        inferredTypeByParameter: Map<FirTypeParameterSymbol, ConeKotlinType>,
    ): Map<String, ReceiverComparison> {
        // Only report callable type parameters that occur somewhere inside the declared receiver.
        // For `<T, R> Iterable<List<T>>.foo(R)`, this selects T but not R. `contains` walks the
        // complete type tree, so type parameters nested inside other type arguments are included.
        val receiverTypeParameters = typeParameters.filter { typeParameter ->
            declaredReceiverType?.contains {
                it is ConeTypeParameterType && it.lookupTag.typeParameterSymbol == typeParameter
            } == true
        }

        // Missing receiver information should not discard the selected parameter names. Keep them in
        // the JSON and explicitly mark their comparison unavailable instead.
        if (actualReceiverType == null || declaredReceiverType == null) {
            return receiverTypeParameters.associate { it.name.asString() to ReceiverComparison(null, null, "unavailable") }
        }

        val typeContext = context.session.typeContext
        val declaredLowerBound = declaredReceiverType.lowerBoundIfFlexible()

        // The actual and declared receivers frequently have different constructors. For example:
        //
        //     actual receiver:   Set<List<String>>
        //     declared receiver: Iterable<List<T>>
        //
        // These shapes cannot be unified directly. First find the actual receiver viewed as the
        // corresponding declared supertype; in this example that is `Iterable<List<String>>`.
        // If the entire declared receiver is just T, there is no class constructor to match, and the
        // actual receiver type itself is the receiver-derived value of T.
        val actualAsDeclaredType = if (declaredLowerBound is ConeTypeParameterType) {
            actualReceiverType.lowerBoundIfFlexible()
        } else {
            AbstractTypeChecker.findCorrespondingSupertypes(
                typeContext.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = false),
                actualReceiverType.lowerBoundIfFlexible(),
                declaredLowerBound.typeConstructor(typeContext),
            ).firstOrNull() as? ConeKotlinType
        }

        // Recursively unify the corresponding actual shape with the declared shape:
        //
        //     Iterable<List<String>>  ~  Iterable<List<T>>
        //
        // FIR's type unifier descends through arbitrary nesting and records `T -> String`. Restrict
        // its variables to parameters mentioned in the receiver, so unrelated callable parameters
        // cannot accidentally acquire a receiver-derived type here.
        val receiverDerivedTypes = mutableMapOf<FirTypeParameterSymbol, ConeTypeProjection>()
        if (actualAsDeclaredType != null) {
            context.session.doUnify(
                actualAsDeclaredType,
                declaredLowerBound,
                receiverTypeParameters.toSet(),
                receiverDerivedTypes,
            )
        }

        // Finally compare each type obtained from the receiver alone with the type selected by normal
        // inference from the whole call. Thus `String` versus `CharSequence` is over-approximated,
        // while `String` versus `String` is exact even if the receiver itself changed from Set to Iterable.
        return receiverTypeParameters.associate { typeParameter ->
            val receiverDerivedType = receiverDerivedTypes[typeParameter]?.type
            val inferredType = inferredTypeByParameter[typeParameter]
            typeParameter.name.asString() to ReceiverComparison(
                actualType = receiverDerivedType?.renderReadable(),
                inferredType = inferredType?.renderReadable(),
                relation = compareReceiverTypes(receiverDerivedType, inferredType),
            )
        }
    }

    context(context: CheckerContext)
    private fun compareReceiverTypes(actualType: ConeKotlinType?, inferredType: ConeKotlinType?): String {
        if (actualType == null || inferredType == null) return "unavailable"
        val typeContext = context.session.typeContext
        if (AbstractTypeChecker.equalTypes(typeContext, actualType, inferredType, stubTypesEqualToAnything = false)) {
            return "exact"
        }
        return when {
            AbstractTypeChecker.isSubtypeOf(typeContext, actualType, inferredType, stubTypesEqualToAnything = false) ->
                "over_approximated"
            AbstractTypeChecker.isSubtypeOf(typeContext, inferredType, actualType, stubTypesEqualToAnything = false) ->
                "under_approximated"
            else -> "incomparable"
        }
    }

    private fun buildSignature(
        typeParameters: List<FirTypeParameterSymbol>,
        receiverType: ConeKotlinType?,
        valueParameterTypes: List<ConeKotlinType>,
    ): String {
        val typeParameterList = typeParameters.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "<", postfix = "> ") { it.renderForSignature() }
            ?: ""
        val receiver = receiverType?.renderReadable()?.let { "$it." } ?: ""
        val parameters = valueParameterTypes.joinToString(prefix = "(", postfix = ")") { it.renderReadable() }
        return "$typeParameterList$receiver$parameters"
    }

    private fun FirTypeParameterSymbol.renderForSignature(): String {
        val variance = when (variance) {
            Variance.IN_VARIANCE -> "in "
            Variance.OUT_VARIANCE -> "out "
            Variance.INVARIANT -> ""
        }
        val bounds = resolvedBounds.map { it.coneType }.filterNot { it.isImplicitNullableAnyBound() }
        val renderedBounds = bounds.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = " : ", separator = " & ") { it.renderReadable() }
            ?: ""
        return "$variance${name.asString()}$renderedBounds"
    }

    private fun ConeKotlinType.isImplicitNullableAnyBound(): Boolean =
        lowerBoundIfFlexible().classLikeLookupTagIfAny?.classId == StandardClassIds.Any &&
                upperBoundIfFlexible().isMarkedNullable

    private fun ConeKotlinType.containsErrorType(): Boolean = contains { it is ConeErrorType }

    private fun List<String>.toPhaseNames(freshVariableByName: Map<String, ConeTypeParameterBasedTypeVariable>): List<String> {
        return map { name ->
            freshVariableByName[name]?.typeParameterSymbol?.name?.asString() ?: name
        }
    }

    private data class DiagnosticData(
        val callableId: String,
        val signature: String,
        val normalInference: NormalInferenceData,
        val twoPhaseInference: AnalysisResult,
    ) {
        fun toJson(): String = jsonObject(
            "callableId" to callableId.toJsonString(),
            "signature" to signature.toJsonString(),
            "normalInference" to normalInference.toJson(),
            "twoPhaseInference" to twoPhaseInference.toJson(),
        )
    }

    private data class NormalInferenceData(
        val inferredTypes: Map<String, String>,
        val receiver: ReceiverComparison,
        val receiverTypeParameters: Map<String, ReceiverComparison>,
    ) {
        fun toJson(): String = jsonObject(
            "inferredTypes" to inferredTypes.toJson(),
            "receiver" to receiver.toJson(),
            "receiverTypeParameters" to receiverTypeParameters.toJson { it.toJson() },
        )
    }

    private data class ReceiverComparison(
        val actualType: String?,
        val inferredType: String?,
        val relation: String,
    ) {
        fun toJson(): String = jsonObject(
            "actualType" to actualType?.toJsonString().orJsonNull(),
            "inferredType" to inferredType?.toJsonString().orJsonNull(),
            "relation" to relation.toJsonString(),
        )
    }

    private sealed interface AnalysisResult {
        fun toJson(): String

        data class Success(
            val outcome: TwoPhaseOutcome,
            val inferredTypes: Map<String, String>,
            val receiverPhaseFixed: List<String>,
            val receiverPhaseUnfixed: List<String>,
            val argumentPhaseFixed: List<String>,
        ) : AnalysisResult {
            override fun toJson(): String = jsonObject(
                "result" to "success".toJsonString(),
                "outcome" to outcome.serializedName.toJsonString(),
                "inferredTypes" to inferredTypes.toJson(),
                "receiverPhaseFixed" to receiverPhaseFixed.toJson(),
                "receiverPhaseUnfixed" to receiverPhaseUnfixed.toJson(),
                "argumentPhaseFixed" to argumentPhaseFixed.toJson(),
            )
        }

        data class Error(val reason: String) : AnalysisResult {
            override fun toJson(): String = jsonObject(
                "result" to "error".toJsonString(),
                "reason" to reason.toJsonString(),
            )
        }
    }

    private enum class TwoPhaseOutcome(val serializedName: String) {
        RECEIVER_FIXED_ARGUMENTS_SUCCEEDED("successful"),
        RECEIVER_FIXED_ARGUMENTS_FAILED("inapplicable"),
        RECEIVER_INFERENCE_FAILED("failed"),
    }

    private fun jsonObject(vararg entries: Pair<String, String>): String =
        entries.joinToString(prefix = "{", postfix = "}") { entry ->
            "${entry.first.toJsonString()}:${entry.second}"
        }

    private fun Map<String, String>.toJson(): String =
        entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "${key.toJsonString()}:${value.toJsonString()}" }

    private fun <T> Map<String, T>.toJson(renderValue: (T) -> String): String =
        entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "${key.toJsonString()}:${renderValue(value)}" }

    private fun List<String>.toJson(): String = joinToString(prefix = "[", postfix = "]") { it.toJsonString() }

    private fun String?.orJsonNull(): String = this ?: "null"

    private fun String.toJsonString(): String = buildString(length + 2) {
        append('"')
        for (character in this@toJsonString) {
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character < ' ') append("\\u%04x".format(character.code)) else append(character)
            }
        }
        append('"')
    }
}
