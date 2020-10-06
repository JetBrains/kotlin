/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.SyntheticCallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.types.Variance

class FirSyntheticCallGenerator(
    private val components: BodyResolveComponents
) {
    private val session = components.session

    private val whenSelectFunction: FirSimpleFunction = generateSyntheticSelectFunction(SyntheticCallableId.WHEN)
    private val trySelectFunction: FirSimpleFunction = generateSyntheticSelectFunction(SyntheticCallableId.TRY)
    private val idFunction: FirSimpleFunction = generateSyntheticSelectFunction(SyntheticCallableId.ID)
    private val checkNotNullFunction: FirSimpleFunction = generateSyntheticCheckNotNullFunction()
    private val elvisFunction: FirSimpleFunction = generateSyntheticElvisFunction()

    fun generateCalleeForWhenExpression(whenExpression: FirWhenExpression, context: ResolutionContext): FirWhenExpression? {
        val stubReference = whenExpression.calleeReference
        // TODO: Investigate: assertion failed in ModularizedTest
        // assert(stubReference is FirStubReference)
        if (stubReference !is FirStubReference) return null

        val argumentList = buildArgumentList {
            arguments += whenExpression.branches.map { it.result }
        }
        val reference = generateCalleeReferenceWithCandidate(
            whenSelectFunction,
            argumentList,
            SyntheticCallableId.WHEN.callableName,
            context = context
        ) ?: return null // TODO

        return whenExpression.transformCalleeReference(UpdateReference, reference)
    }

    fun generateCalleeForTryExpression(tryExpression: FirTryExpression, context: ResolutionContext): FirTryExpression? {
        val stubReference = tryExpression.calleeReference
        assert(stubReference is FirStubReference)

        val argumentList = buildArgumentList {
            with(tryExpression) {
                arguments += tryBlock
                catches.forEach {
                    arguments += it.block
                }
            }
        }

        val reference = generateCalleeReferenceWithCandidate(
            trySelectFunction,
            argumentList,
            SyntheticCallableId.TRY.callableName,
            context = context
        ) ?: return null // TODO

        return tryExpression.transformCalleeReference(UpdateReference, reference)
    }

    fun generateCalleeForCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, context: ResolutionContext): FirCheckNotNullCall? {
        val stubReference = checkNotNullCall.calleeReference
        if (stubReference !is FirStubReference) return null

        val reference = generateCalleeReferenceWithCandidate(
            checkNotNullFunction,
            checkNotNullCall.argumentList,
            SyntheticCallableId.CHECK_NOT_NULL.callableName,
            context = context
        ) ?: return null // TODO

        return checkNotNullCall.transformCalleeReference(UpdateReference, reference)
    }

    fun generateCalleeForElvisExpression(elvisExpression: FirElvisExpression, context: ResolutionContext): FirElvisExpression? {
        if (elvisExpression.calleeReference !is FirStubReference) return null

        val argumentList = buildArgumentList {
            arguments += elvisExpression.lhs
            arguments += elvisExpression.rhs
        }
        val reference = generateCalleeReferenceWithCandidate(
            elvisFunction,
            argumentList,
            SyntheticCallableId.ELVIS_NOT_NULL.callableName,
            context = context
        ) ?: return null

        return elvisExpression.transformCalleeReference(UpdateReference, reference)
    }

    fun resolveCallableReferenceWithSyntheticOuterCall(
        callableReferenceAccess: FirCallableReferenceAccess,
        expectedTypeRef: FirTypeRef?,
        context: ResolutionContext
    ): FirCallableReferenceAccess? {
        val argumentList = buildUnaryArgumentList(callableReferenceAccess)

        val reference =
            generateCalleeReferenceWithCandidate(
                idFunction,
                argumentList,
                SyntheticCallableId.ID.callableName,
                CallKind.SyntheticIdForCallableReferencesResolution,
                context
            ) ?: return callableReferenceAccess.transformCalleeReference(
                StoreCalleeReference,
                buildErrorNamedReference {
                    source = callableReferenceAccess.source
                    diagnostic = ConeUnresolvedNameError(callableReferenceAccess.calleeReference.name)
                }
            )
        val fakeCallElement = buildFunctionCall {
            calleeReference = reference
            this.argumentList = argumentList
        }

        val argument = components.callCompleter.completeCall(fakeCallElement, expectedTypeRef).result.argument
        return ((argument as? FirVarargArgumentsExpression)?.arguments?.get(0) ?: argument) as FirCallableReferenceAccess?
    }

    private fun generateCalleeReferenceWithCandidate(
        function: FirSimpleFunction,
        argumentList: FirArgumentList,
        name: Name,
        callKind: CallKind = CallKind.SyntheticSelect,
        context: ResolutionContext
    ): FirNamedReferenceWithCandidate? {
        val callInfo = generateCallInfo(name, argumentList, callKind)
        val candidate = generateCandidate(callInfo, function, context)
        val applicability = components.resolutionStageRunner.processCandidate(candidate, context)
        if (applicability <= CandidateApplicability.INAPPLICABLE) {
            return null
        }

        return FirNamedReferenceWithCandidate(null, name, candidate)
    }

    private fun generateCandidate(callInfo: CallInfo, function: FirSimpleFunction, context: ResolutionContext): Candidate =
        CandidateFactory(context, callInfo).createCandidate(
            symbol = function.symbol,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            scope = null
        )

    private fun generateCallInfo(name: Name, argumentList: FirArgumentList, callKind: CallKind) = CallInfo(
        callKind = callKind,
        name = name,
        explicitReceiver = null,
        argumentList = argumentList,
        isPotentialQualifierPart = false,
        isImplicitInvoke = false,
        typeArguments = emptyList(),
        session = session,
        containingFile = components.file,
        containingDeclarations = components.containingDeclarations
    )

    private fun generateSyntheticSelectTypeParameter(): Pair<FirTypeParameter, FirResolvedTypeRef> {
        val typeParameterSymbol = FirTypeParameterSymbol()
        val typeParameter =
            buildTypeParameter {
                session = this@FirSyntheticCallGenerator.session
                origin = FirDeclarationOrigin.Library
                name = Name.identifier("K")
                symbol = typeParameterSymbol
                variance = Variance.INVARIANT
                isReified = false
                addDefaultBoundIfNecessary()
            }

        val typeParameterTypeRef = buildResolvedTypeRef { type = ConeTypeParameterTypeImpl(typeParameterSymbol.toLookupTag(), false) }
        return typeParameter to typeParameterTypeRef
    }


    private fun generateSyntheticSelectFunction(callableId: CallableId): FirSimpleFunction {
        // Synthetic function signature:
        //   fun <K> select(vararg values: K): K
        val functionSymbol = FirSyntheticFunctionSymbol(callableId)

        val (typeParameter, returnType) = generateSyntheticSelectTypeParameter()

        val argumentType = buildResolvedTypeRef { type = returnType.type.createArrayType() }
        val typeArgument = buildTypeProjectionWithVariance {
            typeRef = returnType
            variance = Variance.INVARIANT
        }

        return generateMemberFunction(functionSymbol, callableId.callableName, typeArgument.typeRef).apply {
            typeParameters += typeParameter
            valueParameters += argumentType.toValueParameter("branches", isVararg = true)
        }.build()
    }

    private fun generateSyntheticCheckNotNullFunction(): FirSimpleFunction {
        // Synthetic function signature:
        //   fun <K> checkNotNull(arg: K?): K
        //
        // Note: The upper bound of `K` cannot be `Any` because of the following case:
        //   fun <X> test(a: X) = a!!
        // `X` is not a subtype of `Any` and hence cannot satisfy `K` if it had an upper bound of `Any`.
        val functionSymbol = FirSyntheticFunctionSymbol(SyntheticCallableId.CHECK_NOT_NULL)
        val (typeParameter, returnType) = generateSyntheticSelectTypeParameter()

        val argumentType = buildResolvedTypeRef {
            type = returnType.type.withNullability(ConeNullability.NULLABLE, session.typeContext)
        }
        val typeArgument = buildTypeProjectionWithVariance {
            typeRef = returnType
            variance = Variance.INVARIANT
        }

        return generateMemberFunction(
            functionSymbol,
            SyntheticCallableId.CHECK_NOT_NULL.callableName,
            typeArgument.typeRef
        ).apply {
            typeParameters += typeParameter
            valueParameters += argumentType.toValueParameter("arg")
        }.build()
    }

    private fun generateSyntheticElvisFunction(): FirSimpleFunction {
        // Synthetic function signature:
        //   fun <K> checkNotNull(x: K?, y: K): @Exact K
        //
        // Note: The upper bound of `K` cannot be `Any` because of the following case:
        //   fun <X> test(a: X, b: X) = a ?: b
        // `X` is not a subtype of `Any` and hence cannot satisfy `K` if it had an upper bound of `Any`.
        val functionSymbol = FirSyntheticFunctionSymbol(SyntheticCallableId.ELVIS_NOT_NULL)
        val (typeParameter, rightArgumentType) = generateSyntheticSelectTypeParameter()

        val leftArgumentType = buildResolvedTypeRef {
            type = rightArgumentType.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NULLABLE, session.typeContext)
        }

        val returnType = rightArgumentType.resolvedTypeFromPrototype(
            rightArgumentType.type.withAttributes(
                ConeAttributes.create(listOf(CompilerConeAttributes.Exact))
            )
        )

        val typeArgument = buildTypeProjectionWithVariance {
            typeRef = returnType
            variance = Variance.INVARIANT
        }

        return generateMemberFunction(
            functionSymbol,
            SyntheticCallableId.ELVIS_NOT_NULL.callableName,
            typeArgument.typeRef
        ).apply {
            typeParameters += typeParameter
            valueParameters += leftArgumentType.toValueParameter("x")
            valueParameters += rightArgumentType.toValueParameter("y")
        }.build()
    }

    private fun generateMemberFunction(
        symbol: FirNamedFunctionSymbol, name: Name, returnType: FirTypeRef
    ): FirSimpleFunctionBuilder {
        return FirSimpleFunctionBuilder().apply {
            session = this@FirSyntheticCallGenerator.session
            origin = FirDeclarationOrigin.Synthetic
            this.symbol = symbol
            this.name = name
            status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                isExpect = false
                isActual = false
                isOverride = false
                isOperator = false
                isInfix = false
                isInline = false
                isTailRec = false
                isExternal = false
                isSuspend = false
            }
            returnTypeRef = returnType
            resolvePhase = FirResolvePhase.BODY_RESOLVE
        }
    }

    private fun FirResolvedTypeRef.toValueParameter(
        nameAsString: String, isVararg: Boolean = false
    ): FirValueParameter {
        val name = Name.identifier(nameAsString)
        return buildValueParameter {
            session = this@FirSyntheticCallGenerator.session
            origin = FirDeclarationOrigin.Library
            this.name = name
            returnTypeRef = this@toValueParameter
            isCrossinline = false
            isNoinline = false
            this.isVararg = isVararg
            symbol = FirVariableSymbol(name)
            resolvePhase = FirResolvePhase.BODY_RESOLVE
        }
    }
}

private object UpdateReference : FirTransformer<FirNamedReferenceWithCandidate>() {
    override fun <E : FirElement> transformElement(element: E, data: FirNamedReferenceWithCandidate): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformReference(reference: FirReference, data: FirNamedReferenceWithCandidate): CompositeTransformResult<FirReference> {
        return data.compose()
    }
}
