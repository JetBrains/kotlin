/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedErrorReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.*
import org.jetbrains.kotlin.fir.resolve.calls.stages.listOfNothingConeType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SyntheticCallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.ArrayFqNames
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
    private val arrayOfSymbolCache: FirCache<Name, FirNamedFunctionSymbol?, Nothing?> = session.firCachesFactory.createCache(::getArrayOfSymbol)

    private fun assertSyntheticResolvableReferenceIsNotResolved(resolvable: FirResolvable) {
        // All synthetic calls (FirWhenExpression, FirTryExpression, FirElvisExpression, FirCheckNotNullCall)
        // contains FirStubReference on creation.
        // generateCallee... functions below replace these references with resolved references.
        // This check ensures that we don't enter their resolve twice.
        assert(resolvable.calleeReference is FirStubReference)
    }

    fun generateCalleeForWhenExpression(
        whenExpression: FirWhenExpression,
        context: ResolutionContext,
        resolutionMode: ResolutionMode,
    ): FirWhenExpression {
        assertSyntheticResolvableReferenceIsNotResolved(whenExpression)

        val argumentList = buildArgumentList {
            arguments += whenExpression.branches.map { it.result }
        }
        val reference = generateCalleeReferenceWithCandidate(
            whenExpression,
            whenSelectFunction.symbol,
            argumentList,
            SyntheticCallableId.WHEN.callableName,
            context = context,
            resolutionMode = resolutionMode,
        )

        return whenExpression.transformCalleeReference(UpdateReference, reference)
    }

    fun generateCalleeForTryExpression(
        tryExpression: FirTryExpression,
        context: ResolutionContext,
        resolutionMode: ResolutionMode,
    ): FirTryExpression {
        assertSyntheticResolvableReferenceIsNotResolved(tryExpression)

        val argumentList = buildArgumentList {
            with(tryExpression) {
                arguments += tryBlock
                catches.forEach {
                    arguments += it.block
                }
            }
        }

        val reference = generateCalleeReferenceWithCandidate(
            tryExpression,
            trySelectFunction.symbol,
            argumentList,
            SyntheticCallableId.TRY.callableName,
            context = context,
            resolutionMode = resolutionMode,
        )

        return tryExpression.transformCalleeReference(UpdateReference, reference)
    }

    fun generateCalleeForCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        context: ResolutionContext,
        resolutionMode: ResolutionMode,
    ): FirCheckNotNullCall {
        assertSyntheticResolvableReferenceIsNotResolved(checkNotNullCall)

        val reference = generateCalleeReferenceWithCandidate(
            checkNotNullCall,
            checkNotNullFunction.symbol,
            checkNotNullCall.argumentList,
            SyntheticCallableId.CHECK_NOT_NULL.callableName,
            context = context,
            resolutionMode = resolutionMode,
        )

        return checkNotNullCall.transformCalleeReference(UpdateReference, reference)
    }

    fun generateCalleeForElvisExpression(
        elvisExpression: FirElvisExpression,
        context: ResolutionContext,
        resolutionMode: ResolutionMode,
    ): FirElvisExpression {
        assertSyntheticResolvableReferenceIsNotResolved(elvisExpression)

        val argumentList = buildArgumentList {
            arguments += elvisExpression.lhs
            arguments += elvisExpression.rhs
        }
        val reference = generateCalleeReferenceWithCandidate(
            elvisExpression,
            elvisFunction.symbol,
            argumentList,
            SyntheticCallableId.ELVIS_NOT_NULL.callableName,
            context = context,
            resolutionMode = resolutionMode,
        )

        return elvisExpression.transformCalleeReference(UpdateReference, reference)
    }

    fun generateSyntheticIdCall(arrayLiteral: FirExpression, context: ResolutionContext, resolutionMode: ResolutionMode): FirFunctionCall {
        val argumentList = buildArgumentList {
            arguments += arrayLiteral
        }
        return buildFunctionCall {
            this.argumentList = argumentList
            calleeReference = generateCalleeReferenceWithCandidate(
                arrayLiteral,
                idFunction.symbol,
                argumentList,
                SyntheticCallableId.ID.callableName,
                context = context,
                resolutionMode = resolutionMode,
            )
        }
    }

    fun generateCollectionCall(
        arrayLiteral: FirArrayLiteral,
        expectedTypeConeType: ConeKotlinType,
        context: ResolutionContext,
        resolutionMode: ResolutionMode,
    ): FirFunctionCall {
        return when {
            // expectedTypeConeType.isList -> generateCollectionOfCall(Name.identifier("listOf"), arrayLiteral, context, resolutionMode)
            // expectedTypeConeType.isMutableList -> generateCollectionOfCall(Name.identifier("listOf"), arrayLiteral, context, resolutionMode)
            // expectedTypeConeType.isSet -> generateCollectionOfCall(Name.identifier("setOf"), arrayLiteral, context, resolutionMode)
            // expectedTypeConeType.isMutableSet -> generateCollectionOfCall(Name.identifier("mutableSetOf"), arrayLiteral, context, resolutionMode)
            // expectedTypeConeType.isArrayType -> generateArrayOfCall(arrayLiteral, expectedTypeConeType, context, resolutionMode)
            else -> {
                val toSymbol = expectedTypeConeType.toSymbol(session) ?: error("todo: expectedTypeConeType.toSymbol == null")
                val klass = toSymbol.fir as? FirRegularClass ?: error("todo ${toSymbol.fir::class} is not FirRegularClass")
                val classId = klass.classId
                val companionObjectSymbol = klass.companionObjectSymbol
                val scope = companionObjectSymbol?.unsubstitutedScope(
                    session,
                    components.scopeSession,
                    withForcedTypeCalculator = false,
                    FirResolvePhase.BODY_RESOLVE
                )
                val name = Name.identifier("of")
                // todo drop?
                val ofFunction = scope?.getFunctions(name)?.singleOrNull { it.valueParameterSymbols.singleOrNull()?.isVararg == true }
                if (ofFunction == null) {
                    if (listOfNothingConeType.isSubtypeOf(expectedTypeConeType, session)) {
                        return generateCollectionOfCall(Name.identifier("listOf"), arrayLiteral, context, resolutionMode)
                    } else {
                        error("todo report a diagnostic: Can't find of function in the class")
                    }
                }

                buildFunctionCall {
                    source = arrayLiteral.source
                    argumentList = arrayLiteral.argumentList

                    val receiver = buildResolvedQualifier {
                        source = arrayLiteral.source
                        symbol = companionObjectSymbol
                        packageFqName = classId.packageFqName
                        relativeClassFqName = classId.relativeClassName
                        coneTypeOrNull = companionObjectSymbol.defaultType()
                        canBeValue = true
                    }

                    explicitReceiver = receiver
                    dispatchReceiver = receiver // todo not needed?

                    calleeReference = buildSimpleNamedReference {
                        source = arrayLiteral.source
                        this.name = name
                    }

                    // calleeReference = generateCalleeReferenceWithCandidate(
                    //     arrayLiteral,
                    //     ofFunction.fir.symbol,
                    //     argumentList,
                    //     name,
                    //     callKind = CallKind.Function,
                    //     context,
                    //     resolutionMode,
                    //     dispatchReceiver = receiver
                    // )

                    // origin = FirFunctionCallOrigin.Operator
                }
            }
        }
    }

    fun generateCollectionOfCall(
        name: Name,
        arrayLiteral: FirArrayLiteral,
        context: ResolutionContext,
        resolutionMode: ResolutionMode,
    ): FirFunctionCall {
        val argumentList = arrayLiteral.argumentList
        val symbol = session.symbolProvider
            .getTopLevelFunctionSymbols(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, name)
            .single { it.valueParameterSymbols.singleOrNull()?.isVararg == true }
        return buildFunctionCall {
            this.argumentList = argumentList
            calleeReference = generateCalleeReferenceWithCandidate(
                arrayLiteral,
                symbol,
                argumentList,
                name,
                callKind = CallKind.Function,
                context = context,
                resolutionMode,
            )
            source = arrayLiteral.source
        }
    }

    fun generateArrayOfCall(
        arrayLiteral: FirArrayLiteral,
        expectedTypeConeType: ConeKotlinType,
        context: ResolutionContext,
        resolutionMode: ResolutionMode,
    ): FirFunctionCall {
        val argumentList = arrayLiteral.argumentList
        val arrayOfSymbol = calculateArrayOfSymbol(expectedTypeConeType)
        return buildFunctionCall {
            this.argumentList = argumentList
            coneTypeOrNull = expectedTypeConeType
            calleeReference = arrayOfSymbol?.let {
                generateCalleeReferenceWithCandidate(
                    arrayLiteral,
                    it,
                    argumentList,
                    ArrayFqNames.ARRAY_OF_FUNCTION,
                    callKind = CallKind.Function,
                    context = context,
                    resolutionMode,
                )
            } ?: buildErrorNamedReference {
                diagnostic = ConeUnresolvedNameError(ArrayFqNames.ARRAY_OF_FUNCTION)
            }
            source = arrayLiteral.source
        }.also {
            if (arrayOfSymbol == null) {
                it.resultType = components.typeFromCallee(it).coneType
            }
        }
    }

    private fun calculateArrayOfSymbol(expectedTypeConeType: ConeKotlinType,): FirNamedFunctionSymbol? {
        val coneType = expectedTypeConeType.fullyExpandedType(session)
        val arrayCallName = when {
            coneType.isPrimitiveArray -> {
                val arrayElementClassId = coneType.arrayElementType()!!.classId
                val primitiveType = PrimitiveType.getByShortName(arrayElementClassId!!.shortClassName.asString())
                ArrayFqNames.PRIMITIVE_TYPE_TO_ARRAY[primitiveType]!!
            }
            coneType.isUnsignedArray -> {
                val arrayElementClassId = coneType.arrayElementType()!!.classId
                ArrayFqNames.UNSIGNED_TYPE_TO_ARRAY[arrayElementClassId!!.asSingleFqName()]!!
            }
            else -> {
                ArrayFqNames.ARRAY_OF_FUNCTION
            }
        }
        return arrayOfSymbolCache.getValue(arrayCallName)
    }

    private fun getArrayOfSymbol(arrayOfName: Name): FirNamedFunctionSymbol? {
        return session.symbolProvider
            .getTopLevelFunctionSymbols(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, arrayOfName)
            .firstOrNull() // TODO: it should be single() after KTIJ-26465 is fixed
    }

    fun resolveCallableReferenceWithSyntheticOuterCall(
        callableReferenceAccess: FirCallableReferenceAccess,
        expectedType: ConeKotlinType?,
        context: ResolutionContext,
        resolutionMode: ResolutionMode,
    ): FirCallableReferenceAccess {
        val argumentList = buildUnaryArgumentList(callableReferenceAccess)

        val parameterType =
            when {
                expectedType != null && !expectedType.isUnitOrFlexibleUnit -> expectedType
                else -> context.session.builtinTypes.anyType.coneType
            }

        var reference = generateCalleeReferenceWithCandidate(
            callableReferenceAccess,
            argumentList,
            parameterType,
            context,
            resolutionMode,
        )
        var initialCallWasUnresolved = false

        if (reference is FirErrorReferenceWithCandidate && reference.diagnostic is ConeInapplicableCandidateError) {
            // If the callable reference cannot be resolved with the expected type, let's try to resolve it with any type and report
            // something like INITIALIZER_TYPE_MISMATCH or NONE_APPLICABLE instead of UNRESOLVED_REFERENCE.

            check(callableReferenceAccess.calleeReference is FirSimpleNamedReference && !callableReferenceAccess.isResolved) {
                "Expected FirCallableReferenceAccess to be unresolved."
            }

            reference =
                generateCalleeReferenceWithCandidate(
                    callableReferenceAccess,
                    argumentList,
                    context.session.builtinTypes.anyType.coneType,
                    context,
                    resolutionMode,
                )
            initialCallWasUnresolved = true
        }

        val fakeCall = buildFunctionCall {
            calleeReference = reference
            this.argumentList = argumentList
        }

        components.callCompleter.completeCall(fakeCall, ResolutionMode.ContextIndependent)

        return callableReferenceAccess.apply { updateErrorsIfNecessary(fakeCall, initialCallWasUnresolved) }
    }

    private fun FirCallableReferenceAccess.updateErrorsIfNecessary(fakeCall: FirFunctionCall, initialCallWasUnresolved: Boolean) {
        val fakeCallCalleeReference = fakeCall.calleeReference
        val calleeReference = calleeReference

        if (fakeCallCalleeReference.isError()) {
            (calleeReference as? FirNamedReferenceWithCandidate)
                ?.toErrorReference(fakeCallCalleeReference.diagnostic)
                ?.let { replaceCalleeReference(it) }

            if (!calleeReference.isError()) {
                val resolvedReference = calleeReference as? FirResolvedCallableReference
                    ?: error("By this time the actual callable reference must have already been resolved")

                replaceCalleeReference(
                    buildResolvedErrorReference {
                        this.name = resolvedReference.name
                        this.source = resolvedReference.source
                        this.resolvedSymbol = resolvedReference.resolvedSymbol
                        this.diagnostic = fakeCallCalleeReference.diagnostic
                    }
                )
            }
        } else if (initialCallWasUnresolved && calleeReference is FirErrorNamedReference) {
            // If the initial call was unresolved, we tried to resolve with target type Any.
            // If there are multiple applicable overloads, the applicability of the error reference is set to RESOLVED meaning we would
            // report OVERLOAD_RESOLUTION_AMBIGUITY.
            // This would be misleading since the opposite is actually true - no overloads were applicable.
            // To fix this, we manually set the applicability to INAPPLICABLE.
            (calleeReference.diagnostic as? ConeAmbiguityError)?.let {
                val newCalleeReference = buildErrorNamedReference {
                    source = calleeReference.source
                    diagnostic = ConeAmbiguityError(
                        it.name,
                        CandidateApplicability.INAPPLICABLE,
                        it.candidates,
                    )
                }
                replaceCalleeReference(newCalleeReference)
            }
        }
    }

    private fun generateCalleeReferenceWithCandidate(
        callableReferenceAccess: FirCallableReferenceAccess,
        argumentList: FirArgumentList,
        parameterType: ConeKotlinType,
        context: ResolutionContext,
        resolutionMode: ResolutionMode,
    ): FirNamedReferenceWithCandidate {
        val callableId = SyntheticCallableId.ACCEPT_SPECIFIC_TYPE
        val functionSymbol = FirSyntheticFunctionSymbol(callableId)
        // fun accept(p: <parameterTypeRef>): Unit
        val function =
            generateMemberFunction(functionSymbol, callableId.callableName, returnType = context.session.builtinTypes.unitType).apply {
                valueParameters += parameterType.toValueParameter("reference", functionSymbol, isVararg = false)
            }.build()

        return generateCalleeReferenceWithCandidate(
            callableReferenceAccess,
            function.symbol,
            argumentList,
            callableId.callableName,
            CallKind.SyntheticIdForCallableReferencesResolution,
            context,
            resolutionMode,
        )
    }

    private fun generateCalleeReferenceWithCandidate(
        callSite: FirExpression,
        function: FirBasedSymbol<*>,
        argumentList: FirArgumentList,
        name: Name,
        callKind: CallKind = CallKind.SyntheticSelect,
        context: ResolutionContext,
        resolutionMode: ResolutionMode,
        dispatchReceiver: FirExpression? = null,
    ): FirNamedReferenceWithCandidate {
        val callInfo = generateCallInfo(callSite, name, argumentList, callKind, resolutionMode)
        val candidate = generateCandidate(callInfo, function, dispatchReceiver, context)
        val applicability = components.resolutionStageRunner.processCandidate(candidate, context)
        val source = callSite.source?.fakeElement(KtFakeSourceElementKind.SyntheticCall)
        if (!candidate.isSuccessful) {
            return createErrorReferenceWithExistingCandidate(
                candidate,
                ConeInapplicableCandidateError(applicability, candidate),
                source,
                context,
                components.resolutionStageRunner
            )
        }

        return FirNamedReferenceWithCandidate(source, name, candidate)
    }

    private fun generateCandidate(
        callInfo: CallInfo,
        function: FirBasedSymbol<*>,
        dispatchReceiver: FirExpression?,
        context: ResolutionContext,
    ): Candidate {
        val candidateFactory = CandidateFactory(context, callInfo)
        return candidateFactory.createCandidate(
            callInfo,
            symbol = function,
            explicitReceiverKind = if (dispatchReceiver != null) ExplicitReceiverKind.DISPATCH_RECEIVER else ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            dispatchReceiver = dispatchReceiver,
            scope = null
        )
    }

    private fun generateCallInfo(
        callSite: FirExpression,
        name: Name,
        argumentList: FirArgumentList,
        callKind: CallKind,
        resolutionMode: ResolutionMode,
    ) = CallInfo(
        callSite = callSite,
        callKind = callKind,
        name = name,
        explicitReceiver = null,
        argumentList = argumentList,
        isUsedAsGetClassReceiver = false,
        typeArguments = emptyList(),
        session = session,
        containingFile = components.file,
        containingDeclarations = components.containingDeclarations,
        resolutionMode = resolutionMode,
        implicitInvokeMode = ImplicitInvokeMode.None,
    )

    private fun generateSyntheticSelectTypeParameter(
        functionSymbol: FirSyntheticFunctionSymbol,
        isNullableBound: Boolean = true,
    ): Pair<FirTypeParameter, ConeKotlinType> {
        val typeParameterSymbol = FirTypeParameterSymbol()
        val typeParameter =
            buildTypeParameter {
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Synthetic.FakeFunction
                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                name = Name.identifier("K")
                symbol = typeParameterSymbol
                containingDeclarationSymbol = functionSymbol
                variance = Variance.INVARIANT
                isReified = false

                if (!isNullableBound) {
                    bounds += moduleData.session.builtinTypes.anyType
                } else {
                    addDefaultBoundIfNecessary()
                }
            }

        val typeParameterType = ConeTypeParameterTypeImpl(typeParameterSymbol.toLookupTag(), false)
        return typeParameter to typeParameterType
    }


    private fun generateSyntheticSelectFunction(callableId: CallableId): FirSimpleFunction {
        // Synthetic function signature:
        //   fun <K> select(vararg values: K): K
        val functionSymbol = FirSyntheticFunctionSymbol(callableId)

        val (typeParameter, returnType) = generateSyntheticSelectTypeParameter(functionSymbol)

        val typeArgument = buildTypeProjectionWithVariance {
            typeRef = returnType.toFirResolvedTypeRef()
            variance = Variance.INVARIANT
        }

        return generateMemberFunction(functionSymbol, callableId.callableName, typeArgument.typeRef).apply {
            typeParameters += typeParameter
            valueParameters += returnType.createArrayType().toValueParameter("branches", functionSymbol, isVararg = true)
        }.build()
    }

    private fun generateSyntheticCheckNotNullFunction(): FirSimpleFunction {
        // Synthetic function signature:
        //   fun <K> checkNotNull(arg: K?): K & Any
        val functionSymbol = FirSyntheticFunctionSymbol(SyntheticCallableId.CHECK_NOT_NULL)
        val (typeParameter, typeParameterType) = generateSyntheticSelectTypeParameter(functionSymbol, isNullableBound = true)

        return generateMemberFunction(
            functionSymbol,
            SyntheticCallableId.CHECK_NOT_NULL.callableName,
            returnType = typeParameterType.makeConeTypeDefinitelyNotNullOrNotNull(
                session.typeContext,
                // No checks are necessary because we're sure that the type parameter has default (nullable) upper bound.
                // At the same time, not having `avoidComprehensiveCheck = true` might lead to plugin initialization issues.
                avoidComprehensiveCheck = true,
            ).toFirResolvedTypeRef(),
        ).apply {
            typeParameters += typeParameter
            valueParameters += typeParameterType
                .withNullability(nullable = true, session.typeContext).toValueParameter("arg", functionSymbol)
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
        val (typeParameter, rightArgumentType) = generateSyntheticSelectTypeParameter(functionSymbol)

        val returnType = rightArgumentType
            .withAttributes(ConeAttributes.create(listOf(CompilerConeAttributes.Exact)))
            .toFirResolvedTypeRef()

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
            valueParameters += rightArgumentType
                .withNullability(nullable = true, session.typeContext)
                .toValueParameter("x", functionSymbol)
            valueParameters += rightArgumentType
                .toValueParameter("y", functionSymbol)
        }.build()
    }

    private fun generateMemberFunction(
        symbol: FirNamedFunctionSymbol, name: Name, returnType: FirTypeRef
    ): FirSimpleFunctionBuilder {
        return FirSimpleFunctionBuilder().apply {
            moduleData = session.moduleData
            origin = FirDeclarationOrigin.Synthetic.FakeFunction
            this.symbol = symbol
            this.name = name
            status = FirDeclarationStatusImpl(Visibilities.Public, Modality.FINAL)
            returnTypeRef = returnType
            resolvePhase = FirResolvePhase.BODY_RESOLVE
        }
    }

    private fun ConeKotlinType.toValueParameter(
        nameAsString: String, functionSymbol: FirFunctionSymbol<*>, isVararg: Boolean = false
    ): FirValueParameter {
        val name = Name.identifier(nameAsString)
        return buildValueParameter {
            moduleData = session.moduleData
            containingDeclarationSymbol = functionSymbol
            origin = FirDeclarationOrigin.Synthetic.FakeFunction
            this.name = name
            returnTypeRef = this@toValueParameter.toFirResolvedTypeRef()
            isCrossinline = false
            isNoinline = false
            this.isVararg = isVararg
            symbol = FirValueParameterSymbol(name)
            resolvePhase = FirResolvePhase.BODY_RESOLVE
        }
    }
}

private object UpdateReference : FirTransformer<FirNamedReferenceWithCandidate>() {
    override fun <E : FirElement> transformElement(element: E, data: FirNamedReferenceWithCandidate): E {
        return element
    }

    override fun transformReference(reference: FirReference, data: FirNamedReferenceWithCandidate): FirReference {
        return data
    }
}
