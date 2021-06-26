/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.expectedType
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.jetbrains.kotlin.fir.resolve.initialTypeOfCandidate
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.InvocationKindTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.typeFromCallee
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.StubTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirCallCompleter(
    private val transformer: FirBodyResolveTransformer,
    private val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents
) {
    private val session = components.session
    val completer = ConstraintSystemCompleter(components)
    private val inferenceSession
        get() = transformer.context.inferenceSession

    data class CompletionResult<T>(val result: T, val callCompleted: Boolean)

    fun <T> completeCall(
        call: T,
        expectedTypeRef: FirTypeRef?,
        expectedTypeMismatchIsReportedInChecker: Boolean = false,
    ): CompletionResult<T> where T : FirResolvable, T : FirStatement =
        completeCall(call, expectedTypeRef, mayBeCoercionToUnitApplied = false, expectedTypeMismatchIsReportedInChecker, isFromCast = false)

    fun <T> completeCall(call: T, data: ResolutionMode): CompletionResult<T> where T : FirResolvable, T : FirStatement =
        completeCall(
            call,
            data.expectedType(components, allowFromCast = true),
            (data as? ResolutionMode.WithExpectedType)?.mayBeCoercionToUnitApplied == true,
            (data as? ResolutionMode.WithExpectedType)?.expectedTypeMismatchIsReportedInChecker == true,
            isFromCast = data is ResolutionMode.WithExpectedTypeFromCast,
        )

    private fun <T> completeCall(
        call: T, expectedTypeRef: FirTypeRef?,
        mayBeCoercionToUnitApplied: Boolean,
        expectedTypeMismatchIsReportedInChecker: Boolean,
        isFromCast: Boolean,
    ): CompletionResult<T>
            where T : FirResolvable, T : FirStatement {
        val typeRef = components.typeFromCallee(call)

        if (call is FirVariableAssignment) {
            call.replaceLValueTypeRef(typeRef)
        }

        val reference = call.calleeReference as? FirNamedReferenceWithCandidate ?: return CompletionResult(call, true)

        val candidate = reference.candidate
        val initialType = components.initialTypeOfCandidate(candidate, call)

        if (call is FirExpression) {
            val resolvedTypeRef = typeRef.resolvedTypeFromPrototype(initialType)
            call.resultType = resolvedTypeRef
            session.lookupTracker?.recordTypeResolveAsLookup(resolvedTypeRef, call.source, null)
        }

        if (expectedTypeRef is FirResolvedTypeRef) {
            if (isFromCast) {
                if (candidate.isFunctionForExpectTypeFromCastFeature()) {
                    candidate.system.addSubtypeConstraint(
                        initialType, expectedTypeRef.type,
                        ConeExpectedTypeConstraintPosition(expectedTypeMismatchIsReportedInChecker = false),
                    )
                }
            } else {
                val expectedTypeConstraintPosition = ConeExpectedTypeConstraintPosition(expectedTypeMismatchIsReportedInChecker)
                if (expectedTypeRef.coneType.isUnitOrFlexibleUnit && mayBeCoercionToUnitApplied) {
                    if (candidate.system.notFixedTypeVariables.isNotEmpty()) {
                        candidate.system.addSubtypeConstraintIfCompatible(
                            initialType, expectedTypeRef.type, expectedTypeConstraintPosition
                        )
                    }
                } else {
                    candidate.system.addSubtypeConstraint(initialType, expectedTypeRef.type, expectedTypeConstraintPosition)
                }
            }
        }

        val completionMode = candidate.computeCompletionMode(session.inferenceComponents, expectedTypeRef, initialType)

        val analyzer = createPostponedArgumentsAnalyzer(transformer.resolutionContext)
        call.transformSingle(InvocationKindTransformer, null)

        return when (completionMode) {
            ConstraintSystemCompletionMode.FULL -> {
                if (inferenceSession.shouldRunCompletion(call)) {
                    runCompletionForCall(candidate, completionMode, call, initialType, analyzer)
                    val finalSubstitutor = candidate.system.asReadOnlyStorage()
                        .buildAbstractResultingSubstitutor(session.inferenceComponents.ctx) as ConeSubstitutor
                    val completedCall = call.transformSingle(
                        FirCallCompletionResultsWriterTransformer(
                            session, finalSubstitutor, components.returnTypeCalculator,
                            session.inferenceComponents.approximator,
                            components.dataFlowAnalyzer,
                        ),
                        null
                    )
                    inferenceSession.addCompetedCall(completedCall, candidate)
                    CompletionResult(completedCall, true)
                } else {
                    inferenceSession.addPartiallyResolvedCall(call)
                    CompletionResult(call, false)
                }
            }

            ConstraintSystemCompletionMode.PARTIAL -> {
                runCompletionForCall(candidate, completionMode, call, initialType, analyzer)
                inferenceSession.addPartiallyResolvedCall(call)
                CompletionResult(call, false)
            }

            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA -> throw IllegalStateException()
        }
    }

    fun <T> runCompletionForCall(
        candidate: Candidate,
        completionMode: ConstraintSystemCompletionMode,
        call: T,
        initialType: ConeKotlinType,
        analyzer: PostponedArgumentsAnalyzer? = null
    ) where T : FirResolvable, T : FirStatement {
        @Suppress("NAME_SHADOWING")
        val analyzer = analyzer ?: createPostponedArgumentsAnalyzer(transformer.resolutionContext)
        completer.complete(
            candidate.system.asConstraintSystemCompleterContext(),
            completionMode,
            listOf(call),
            initialType,
            transformer.resolutionContext
        ) {
            analyzer.analyze(candidate.system.asPostponedArgumentsAnalyzerContext(), it, candidate, completionMode)
        }
    }

    fun prepareLambdaAtomForFactoryPattern(
        atom: ResolvedLambdaAtom,
        candidate: Candidate
    ) {
        val returnVariable = ConeTypeVariableForLambdaReturnType(atom.atom, "_R")
        val csBuilder = candidate.system.getBuilder()
        csBuilder.registerVariable(returnVariable)
        val functionalType = csBuilder.buildCurrentSubstitutor()
            .safeSubstitute(csBuilder.asConstraintSystemCompleterContext(), atom.expectedType!!) as ConeClassLikeType
        val size = functionalType.typeArguments.size
        val expectedType = ConeClassLikeTypeImpl(
            functionalType.lookupTag,
            Array(size) { index -> if (index != size - 1) functionalType.typeArguments[index] else returnVariable.defaultType },
            isNullable = functionalType.isNullable,
            functionalType.attributes
        )
        csBuilder.addSubtypeConstraint(expectedType, functionalType, ConeArgumentConstraintPosition(atom.atom))
        atom.replaceExpectedType(expectedType, returnVariable.defaultType)
        atom.replaceTypeVariableForLambdaReturnType(returnVariable)
    }

    fun createCompletionResultsWriter(
        substitutor: ConeSubstitutor,
        mode: FirCallCompletionResultsWriterTransformer.Mode = FirCallCompletionResultsWriterTransformer.Mode.Normal
    ): FirCallCompletionResultsWriterTransformer {
        return FirCallCompletionResultsWriterTransformer(
            session, substitutor, components.returnTypeCalculator,
            session.inferenceComponents.approximator,
            components.dataFlowAnalyzer,
            mode
        )
    }

    fun createPostponedArgumentsAnalyzer(context: ResolutionContext): PostponedArgumentsAnalyzer {
        val lambdaAnalyzer = LambdaAnalyzerImpl()
        return PostponedArgumentsAnalyzer(
            context,
            lambdaAnalyzer,
            session.inferenceComponents,
            transformer.components.callResolver
        )
    }

    private inner class LambdaAnalyzerImpl : LambdaAnalyzer {
        override fun analyzeAndGetLambdaReturnArguments(
            lambdaAtom: ResolvedLambdaAtom,
            receiverType: ConeKotlinType?,
            parameters: List<ConeKotlinType>,
            expectedReturnType: ConeKotlinType?,
            stubsForPostponedVariables: Map<TypeVariableMarker, StubTypeMarker>
        ): ReturnArgumentsAnalysisResult {
            val lambdaArgument: FirAnonymousFunction = lambdaAtom.atom
            val needItParam = lambdaArgument.valueParameters.isEmpty() && parameters.size == 1

            val itParam = when {
                needItParam -> {
                    val name = Name.identifier("it")
                    val itType = parameters.single()
                    buildValueParameter {
                        source = lambdaAtom.atom.source?.fakeElement(FirFakeSourceElementKind.ItLambdaParameter)
                        moduleData = session.moduleData
                        origin = FirDeclarationOrigin.Source
                        returnTypeRef = itType.approximateLambdaInputType().toFirResolvedTypeRef()
                        this.name = name
                        symbol = FirValueParameterSymbol(name)
                        defaultValue = null
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }
                }
                else -> null
            }

            val expectedReturnTypeRef = expectedReturnType?.let { lambdaArgument.returnTypeRef.resolvedTypeFromPrototype(it) }

            lambdaArgument.replaceReceiverTypeRef(
                receiverType?.approximateLambdaInputType()?.let {
                    lambdaArgument.receiverTypeRef?.resolvedTypeFromPrototype(it)
                }
            )

            val lookupTracker = session.lookupTracker
            lambdaArgument.valueParameters.forEachIndexed { index, parameter ->
                val newReturnType = parameters[index].approximateLambdaInputType()
                val newReturnTypeRef = if (parameter.returnTypeRef is FirImplicitTypeRef) {
                    newReturnType.toFirResolvedTypeRef(parameter.source)
                } else parameter.returnTypeRef.resolvedTypeFromPrototype(newReturnType)
                parameter.replaceReturnTypeRef(newReturnTypeRef)
                lookupTracker?.recordTypeResolveAsLookup(newReturnTypeRef, parameter.source, null)
            }

            lambdaArgument.replaceValueParameters(lambdaArgument.valueParameters + listOfNotNull(itParam))
            lambdaArgument.replaceReturnTypeRef(
                expectedReturnTypeRef?.also {
                    lookupTracker?.recordTypeResolveAsLookup(it, lambdaArgument.source, null)
                } ?: components.noExpectedType
            )

            val builderInferenceSession = runIf(stubsForPostponedVariables.isNotEmpty()) {
                @Suppress("UNCHECKED_CAST")
                FirBuilderInferenceSession(transformer.resolutionContext, stubsForPostponedVariables as Map<ConeTypeVariable, ConeStubType>)
            }

            transformer.context.withAnonymousFunctionTowerDataContext(lambdaArgument.symbol) {
                if (builderInferenceSession != null) {
                    transformer.context.withInferenceSession(builderInferenceSession) {
                        lambdaArgument.transformSingle(transformer, ResolutionMode.LambdaResolution(expectedReturnTypeRef))
                    }
                } else {
                    lambdaArgument.transformSingle(transformer, ResolutionMode.LambdaResolution(expectedReturnTypeRef))
                }
            }
            transformer.context.dropContextForAnonymousFunction(lambdaArgument)

            val returnArguments = components.dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(lambdaArgument)

            return ReturnArgumentsAnalysisResult(returnArguments, builderInferenceSession)
        }
    }

    private fun ConeKotlinType.approximateLambdaInputType(): ConeKotlinType =
        session.inferenceComponents.approximator.approximateToSuperType(
            this, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
        ) ?: this
}

private fun Candidate.isFunctionForExpectTypeFromCastFeature(): Boolean {
    if (typeArgumentMapping != TypeArgumentMapping.NoExplicitArguments) return false
    val fir = symbol.fir as? FirFunction ?: return false

    return fir.isFunctionForExpectTypeFromCastFeature()
}

// Expect type is only being added to calls in a position of cast argument: foo() as R
// And that call should be resolved to something materialize()-like: it returns its single generic parameter and doesn't have value parameters
// fun <T> materialize(): T
fun FirFunction.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameter = typeParameters.singleOrNull() ?: return false

    val returnType = returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return false

    if ((returnType.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag != typeParameter.symbol.toLookupTag()) return false

    fun FirTypeRef.isBadType() =
        coneTypeSafe<ConeKotlinType>()
            ?.contains { (it.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag == typeParameter.symbol.toLookupTag() } != false

    if (valueParameters.any { it.returnTypeRef.isBadType() } || receiverTypeRef?.isBadType() == true) return false

    return true
}

