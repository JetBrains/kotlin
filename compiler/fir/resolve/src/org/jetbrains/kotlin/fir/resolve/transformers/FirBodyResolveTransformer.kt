/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import com.google.common.collect.LinkedHashMultimap
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.addImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirTopLevelDeclaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addIfNotNull

open class FirBodyResolveTransformer(val session: FirSession, val implicitTypeOnly: Boolean) : FirTransformer<Any?>() {

    val symbolProvider = session.service<FirSymbolProvider>()

    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    private var packageFqName = FqName.ROOT
    private lateinit var file: FirFile
    private var container: FirDeclaration? = null

    override fun transformFile(file: FirFile, data: Any?): CompositeTransformResult<FirFile> {
        packageFqName = file.packageFqName
        this.file = file
        return withScopeCleanup(scopes) {
            scopes.addImportingScopes(file, session)
            scopes += FirTopLevelDeclaredMemberScope(file, session)
            super.transformFile(file, data)
        }
    }

    private var primaryConstructorParametersScope: FirLocalScope? = null

    override fun transformConstructor(constructor: FirConstructor, data: Any?): CompositeTransformResult<FirDeclaration> {
        if (constructor.isPrimary) {
            primaryConstructorParametersScope = FirLocalScope().apply {
                constructor.valueParameters.forEach { this.storeDeclaration(it) }
            }
        }
        return super.transformConstructor(constructor, data)
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: Any?
    ): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup(localScopes) {
            localScopes.addIfNotNull(primaryConstructorParametersScope)
            super.transformAnonymousInitializer(anonymousInitializer, data)
        }
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Any?): CompositeTransformResult<FirTypeRef> {
        if (data == null)
            return implicitTypeRef.compose()
        require(data is FirTypeRef)
        return data.compose()
    }

    override fun transformFunction(function: FirFunction, data: Any?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup(localScopes) {
            localScopes += FirLocalScope()
            super.transformFunction(function, data)
        }
    }


    override fun transformValueParameter(valueParameter: FirValueParameter, data: Any?): CompositeTransformResult<FirDeclaration> {
        localScopes.lastOrNull()?.storeDeclaration(valueParameter)
        if (valueParameter.returnTypeRef is FirImplicitTypeRef) return valueParameter.compose() // TODO
        return super.transformValueParameter(valueParameter, valueParameter.returnTypeRef)
    }


    private inline fun <T> withLabelAndReceiverType(labelName: Name, owner: FirElement, type: ConeKotlinType, block: () -> T): T {
        labels.put(labelName, type)
        when (owner) {
            is FirRegularClass -> implicitReceiverStack += ImplicitDispatchReceiverValue(owner.symbol, type)
            is FirFunction -> implicitReceiverStack += ImplicitExtensionReceiverValue(type)
            else -> throw IllegalArgumentException("Incorrect label & receiver owner: ${owner.javaClass}")
        }
        val result = block()
        implicitReceiverStack.removeAt(implicitReceiverStack.size - 1)
        labels.remove(labelName, type)
        return result
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup(scopes) {
            val oldConstructorScope = primaryConstructorParametersScope
            primaryConstructorParametersScope = null
            val type = regularClass.defaultType()
            scopes.addIfNotNull(type.scope(session, scopeSession))
            val result = withLabelAndReceiverType(regularClass.name, regularClass, type) {
                super.transformRegularClass(regularClass, data)
            }
            primaryConstructorParametersScope = oldConstructorScope
            result
        }
    }

    override fun transformUncheckedNotNullCast(
        uncheckedNotNullCast: FirUncheckedNotNullCast,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        val notNullCast = super.transformUncheckedNotNullCast(uncheckedNotNullCast, data).single as FirUncheckedNotNullCast
        val resultType = notNullCast.expression.resultType
        notNullCast.resultType =
            resultType.withReplacedConeType(session, resultType.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NOT_NULL))
        return notNullCast.compose()
    }

    override fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Any?): CompositeTransformResult<FirStatement> {
        val symbolProvider = session.service<FirSymbolProvider>()
        val resolved = super.transformTypeOperatorCall(typeOperatorCall, data).single
        when ((resolved as FirTypeOperatorCall).operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                resolved.resultType = FirResolvedTypeRefImpl(
                    session,
                    null,
                    StandardClassIds.Boolean(symbolProvider).constructType(emptyArray(), isNullable = false),
                    emptyList()
                )
            }
            FirOperation.AS -> {
                resolved.resultType = resolved.conversionTypeRef
            }
            FirOperation.SAFE_AS -> {
                resolved.resultType =
                        resolved.conversionTypeRef.withReplacedConeType(
                            session,
                            resolved.conversionTypeRef.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NULLABLE)
                        )
            }
            else -> error("Unknown type operator")
        }
        return resolved.compose()
    }

    protected inline fun <T> withScopeCleanup(scopes: MutableList<*>, crossinline l: () -> T): T {
        val sizeBefore = scopes.size
        val result = l()
        val size = scopes.size
        assert(size >= sizeBefore)
        repeat(size - sizeBefore) {
            scopes.let { it.removeAt(it.size - 1) }
        }
        return result
    }

    val scopeSession = ScopeSession()

    val scopes = mutableListOf<FirScope>()
    private val localScopes = mutableListOf<FirLocalScope>()

    private val labels = LinkedHashMultimap.create<Name, ConeKotlinType>()

    private val implicitReceiverStack = mutableListOf<ImplicitReceiverValue>()

    private val jump = ReturnTypeCalculatorWithJump(session)

    private fun <T> storeTypeFromCallee(access: T) where T : FirQualifiedAccess, T : FirExpression {
        access.resultType = typeFromCallee(access)
    }

    private fun <T> typeFromCallee(access: T): FirResolvedTypeRef where T : FirQualifiedAccess, T : FirExpression {
        return when (val newCallee = access.calleeReference) {
            is FirErrorNamedReference ->
                FirErrorTypeRefImpl(session, access.psi, newCallee.errorReason)
            is FirResolvedCallableReference -> {
                val symbol = newCallee.coneSymbol
                if (symbol is ConeCallableSymbol) {
                    jump.tryCalculateReturnType(symbol.firUnsafe())
                } else if (symbol is ConeClassifierSymbol) {
                    val firUnsafe = symbol.firUnsafe<FirElement>()
                    // TODO: unhack
                    if (firUnsafe is FirEnumEntry) {
                        (firUnsafe.superTypeRefs.firstOrNull() as? FirResolvedTypeRef) ?: FirErrorTypeRefImpl(
                            session,
                            null,
                            "no enum item supertype"
                        )
                    } else
                        FirResolvedTypeRefImpl(
                            session, null, symbol.constructType(emptyArray(), isNullable = false),
                            annotations = emptyList()
                        )
                } else {
                    error("WTF ! $symbol")
                }
            }
            else -> error("Failed to extract type from: $newCallee")
        }
    }

    private val inferenceComponents = InferenceComponents(object : ConeInferenceContext, TypeSystemInferenceExtensionContextDelegate {
        override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? {
            //TODO wtf
            return explicitSupertypes.firstOrNull()
        }

        override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): KotlinTypeMarker {
            TODO("not implemented")
        }

        override val session: FirSession
            get() = this@FirBodyResolveTransformer.session

        override fun KotlinTypeMarker.removeExactAnnotation(): KotlinTypeMarker {
            return this
        }
    }, session)

    private fun <T : FirQualifiedAccess> transformCallee(qualifiedAccess: T): T {
        val callee = qualifiedAccess.calleeReference as? FirSimpleNamedReference ?: return qualifiedAccess

        val qualifiedAccess = qualifiedAccess.transformExplicitReceiver(this, noExpectedType)

        val info = CallInfo(
            CallKind.VariableAccess,
            qualifiedAccess.explicitReceiver,
            emptyList(),
            qualifiedAccess.safe,
            emptyList(),
            session,
            file,
            container!!
        ) { it.resultType }
        val resolver = CallResolver(jump, inferenceComponents)
        resolver.callInfo = info
        resolver.scopes = (scopes + localScopes).asReversed()

        val consumer = createVariableAndObjectConsumer(
            session,
            callee.name,
            info, inferenceComponents
        )
        val result = resolver.runTowerResolver(consumer, implicitReceiverStack.asReversed())

        val nameReference = createResolvedNamedReference(
            callee,
            result.bestCandidates(),
            result.currentApplicability
        )

        val resultExpression =
            qualifiedAccess.transformCalleeReference(StoreNameReference, nameReference) as T
        if (resultExpression is FirExpression) storeTypeFromCallee(resultExpression)
        return resultExpression
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {

        when (val callee = qualifiedAccessExpression.calleeReference) {
            is FirThisReference -> {

                val labelName = callee.labelName
                val types = if (labelName == null) labels.values() else labels[Name.identifier(labelName)]
                val type = types.lastOrNull() ?: ConeKotlinErrorType("Unresolved this@$labelName")
                qualifiedAccessExpression.resultType = FirResolvedTypeRefImpl(session, null, type, emptyList())
            }
            is FirSuperReference -> {
                qualifiedAccessExpression.resultType =
                        callee.superTypeRef as? FirResolvedTypeRef ?:
                        implicitReceiverStack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull()
                            ?.boundSymbol?.fir?.superTypeRefs?.firstOrNull()
                        ?: FirErrorTypeRefImpl(session, qualifiedAccessExpression.psi, "No super type")
            }
            is FirResolvedCallableReference -> {
                if (qualifiedAccessExpression.typeRef !is FirResolvedTypeRef) {
                    qualifiedAccessExpression.resultType =
                            jump.tryCalculateReturnType(callee.coneSymbol.firUnsafe<FirCallableDeclaration>())
                }
            }
        }
        return transformCallee(qualifiedAccessExpression).compose()
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        val variableAssignment = variableAssignment.transformRValue(this, null)
        return transformCallee(variableAssignment).compose()
    }

    override fun transformAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?): CompositeTransformResult<FirDeclaration> {
        if (data == null) return anonymousFunction.compose()
        if (data is LambdaResolution) return transformAnonymousFunction(anonymousFunction, data).compose()
        return super.transformAnonymousFunction(anonymousFunction, data)
    }

    fun transformAnonymousFunction(anonymousFunction: FirAnonymousFunction, lambdaResolution: LambdaResolution): FirAnonymousFunction {
        val receiverTypeRef = anonymousFunction.receiverTypeRef
        fun transform(): FirAnonymousFunction {
            return withScopeCleanup(scopes) {
                scopes.addIfNotNull(receiverTypeRef?.coneTypeSafe<ConeKotlinType>()?.scope(session, scopeSession))
                val result =
                    super.transformAnonymousFunction(
                        anonymousFunction,
                        lambdaResolution.expectedReturnTypeRef ?: anonymousFunction.returnTypeRef
                    ).single as FirAnonymousFunction
                val body = result.body
                if (result.returnTypeRef is FirImplicitTypeRef && body != null) {
                    result.transformReturnTypeRef(this, body.resultType)
                    result
                } else {
                    result
                }
            }
        }

        val label = anonymousFunction.label
        return if (label != null && receiverTypeRef != null) {
            withLabelAndReceiverType(Name.identifier(label.name), anonymousFunction, receiverTypeRef.coneTypeUnsafe()) { transform() }
        } else {
            transform()
        }
    }

    private val noExpectedType = FirImplicitTypeRefImpl(session, null)

    private fun resolveCallAndSelectCandidate(functionCall: FirFunctionCall, expectedTypeRef: FirTypeRef?): FirFunctionCall {

        val functionCall =
            (functionCall.transformExplicitReceiver(this, noExpectedType) as FirFunctionCall)
                .transformArguments(this, null) as FirFunctionCall

        val name = functionCall.calleeReference.name

        val explicitReceiver = functionCall.explicitReceiver
        val arguments = functionCall.arguments
        val typeArguments = functionCall.typeArguments

        val info = CallInfo(
            CallKind.Function,
            explicitReceiver,
            arguments,
            functionCall.safe,
            typeArguments,
            session,
            file,
            container!!
        ) { it.resultType }
        val resolver = CallResolver(jump, inferenceComponents)
        resolver.callInfo = info
        resolver.scopes = (scopes + localScopes).asReversed()

        val consumer = createFunctionConsumer(session, name, info, inferenceComponents)
        val result = resolver.runTowerResolver(consumer, implicitReceiverStack.asReversed())
        val bestCandidates = result.bestCandidates()
        val reducedCandidates = ConeOverloadConflictResolver(TypeSpecificityComparator.NONE, inferenceComponents)
            .chooseMaximallySpecificCandidates(bestCandidates, discriminateGenerics = false)


//        fun isInvoke()
//
//        val resultExpression =
//
//        when {
//            successCandidates.singleOrNull() as? ConeCallableSymbol -> {
//                FirFunctionCallImpl(functionCall.session, functionCall.psi, safe = functionCall.safe).apply {
//                    calleeReference =
//                        functionCall.calleeReference.transformSingle(this@FirBodyResolveTransformer, result.successCandidates())
//                    explicitReceiver =
//                        FirQualifiedAccessExpressionImpl(
//                            functionCall.session,
//                            functionCall.calleeReference.psi,
//                            functionCall.safe
//                        ).apply {
//                            calleeReference = createResolvedNamedReference(
//                                functionCall.calleeReference,
//                                result.variableChecker.successCandidates() as List<ConeCallableSymbol>
//                            )
//                            explicitReceiver = functionCall.explicitReceiver
//                        }
//                }
//            }
//            is ApplicabilityChecker -> {
//                functionCall.transformCalleeReference(this, result.successCandidates())
//            }
//            else -> functionCall
//        }
        val nameReference = createResolvedNamedReference(
            functionCall.calleeReference,
            reducedCandidates,
            result.currentApplicability
        )

        val resultExpression = functionCall.transformCalleeReference(StoreNameReference, nameReference) as FirFunctionCall
        val typeRef = typeFromCallee(functionCall)
        if (typeRef.type is ConeKotlinErrorType) {
            functionCall.resultType = typeRef
        }
        return resultExpression
    }

    data class LambdaResolution(val expectedReturnTypeRef: FirResolvedTypeRef?)

    private fun completeTypeInference(functionCall: FirFunctionCall, expectedTypeRef: FirTypeRef?): FirFunctionCall {
        val typeRef = typeFromCallee(functionCall)
        if (typeRef.type is ConeKotlinErrorType) {
            functionCall.resultType = typeRef
            return functionCall
        }
        val candidate = functionCall.candidate() ?: return functionCall
        val initialSubstitutor = candidate.substitutor

        val initialType = initialSubstitutor.substituteOrSelf(typeRef.type)

        val completionMode = candidate.computeCompletionMode(inferenceComponents, expectedTypeRef, initialType)
        val completer = ConstraintSystemCompleter(inferenceComponents)
        val replacements = mutableMapOf<FirExpression, FirExpression>()

        val analyzer = PostponedArgumentsAnalyzer(object : LambdaAnalyzer {
            override fun analyzeAndGetLambdaReturnArguments(
                lambdaArgument: FirAnonymousFunction,
                isSuspend: Boolean,
                receiverType: ConeKotlinType?,
                parameters: List<ConeKotlinType>,
                expectedReturnType: ConeKotlinType?,
                stubsForPostponedVariables: Map<TypeVariableMarker, StubTypeMarker>
            ): Pair<List<FirExpression>, InferenceSession> {

                val itParam = when {
                    lambdaArgument.valueParameters.isEmpty() && parameters.size == 1 ->
                        FirValueParameterImpl(
                            session,
                            null,
                            Name.identifier("it"),
                            FirResolvedTypeRefImpl(session, null, parameters.single(), emptyList()),
                            defaultValue = null,
                            isCrossinline = false,
                            isNoinline = false,
                            isVararg = false
                        )
                    else -> null
                }

                val newLambdaExpression = lambdaArgument.copy(
                    receiverTypeRef = receiverType?.let { lambdaArgument.receiverTypeRef!!.resolvedTypeFromPrototype(it) },
                    valueParameters = lambdaArgument.valueParameters.mapIndexed { index, parameter ->
                        parameter.transformReturnTypeRef(StoreType, parameter.returnTypeRef.resolvedTypeFromPrototype(parameters[index]))
                        parameter
                    } + listOfNotNull(itParam)
                )


                val expectedReturnTypeRef = expectedReturnType?.let { newLambdaExpression.returnTypeRef.resolvedTypeFromPrototype(it) }
                replacements[lambdaArgument] =
                    newLambdaExpression.transformSingle(this@FirBodyResolveTransformer, LambdaResolution(expectedReturnTypeRef))


                return listOfNotNull(newLambdaExpression.body?.statements?.lastOrNull() as? FirExpression) to InferenceSession.default
            }

        }, { it.resultType }, inferenceComponents)

        completer.complete(candidate.system.asConstraintSystemCompleterContext(), completionMode, listOf(functionCall), initialType) {
            analyzer.analyze(
                candidate.system.asPostponedArgumentsAnalyzerContext(),
                it
//                diagnosticsHolder
            )
        }

        functionCall.transformChildren(ReplaceInArguments, replacements.toMap())


        if (completionMode == KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode.FULL) {
            val finalSubstitutor =
                candidate.system.asReadOnlyStorage().buildAbstractResultingSubstitutor(inferenceComponents.ctx) as ConeSubstitutor
            return functionCall.transformSingle(
                FirCallCompleterTransformer(session, finalSubstitutor, jump),
                null
            )
        }
        return functionCall
    }

    override fun transformTryExpression(tryExpression: FirTryExpression, data: Any?): CompositeTransformResult<FirStatement> {
        val tryExpression = tryExpression.transformChildren(this, data) as FirTryExpression
        if (tryExpression.resultType !is FirResolvedTypeRef) {
            val type = commonSuperType((listOf(tryExpression.tryBlock) + tryExpression.catches.map { it.block }).mapNotNull {
                val expression = it.statements.lastOrNull() as? FirExpression
                if (expression != null) {
                    (expression.resultType as? FirResolvedTypeRef) ?: FirErrorTypeRefImpl(session, null, "No type for when branch result")
                } else {
                    FirImplicitUnitTypeRef(session, null)
                }
            })
            if (type != null) tryExpression.resultType = type
        }
        return tryExpression.compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Any?): CompositeTransformResult<FirStatement> {
        if (functionCall.calleeReference !is FirSimpleNamedReference) return functionCall.compose()
        val expectedTypeRef = data as FirTypeRef?
        val completeInference =
            try {
                val resultExpression = resolveCallAndSelectCandidate(functionCall, expectedTypeRef)
                completeTypeInference(resultExpression, expectedTypeRef)
            } catch (e: Throwable) {
                throw RuntimeException("While resolving call ${functionCall.render()}", e)
            }


        return completeInference.compose()

    }

    private fun describeSymbol(symbol: ConeSymbol): String {
        return when (symbol) {
            is ConeClassLikeSymbol -> symbol.classId.asString()
            is ConeCallableSymbol -> symbol.callableId.toString()
            else -> "$symbol"
        }
    }

    private fun createResolvedNamedReference(
        namedReference: FirNamedReference,
        candidates: Collection<Candidate>,
        applicability: CandidateApplicability
    ): FirNamedReference {
        val name = namedReference.name
        return when {
            candidates.isEmpty() -> FirErrorNamedReference(
                namedReference.session, namedReference.psi, "Unresolved name: $name"
            )
            applicability < CandidateApplicability.SYNTHETIC_RESOLVED -> {
                FirErrorNamedReference(
                    namedReference.session,
                    namedReference.psi,
                    "Inapplicable($applicability): ${candidates.map { describeSymbol(it.symbol) }}",
                    namedReference.name
                )
            }
            candidates.size == 1 -> FirNamedReferenceWithCandidate(
                namedReference.session, namedReference.psi,
                name, candidates.single()
            )
            else -> FirErrorNamedReference(
                namedReference.session, namedReference.psi, "Ambiguity: $name, ${candidates.map { describeSymbol(it.symbol) }}",
                namedReference.name
            )
        }
    }

//    override fun transformNamedReference(namedReference: FirNamedReference, data: Any?): CompositeTransformResult<FirNamedReference> {
//        if (namedReference is FirErrorNamedReference || namedReference is FirResolvedCallableReference) return namedReference.compose()
//        val referents = data as? List<ConeCallableSymbol> ?: return namedReference.compose()
//        return createResolvedNamedReference(namedReference, referents).compose()
//    }


    override fun transformBlock(block: FirBlock, data: Any?): CompositeTransformResult<FirStatement> {
        val block = super.transformBlock(block, data).single as FirBlock
        val statement = block.statements.lastOrNull()

        val resultExpression = when (statement) {
            is FirReturnExpression -> statement.result
            is FirExpression -> statement
            else -> null
        }
        block.resultType = (resultExpression?.resultType as? FirResolvedTypeRef) ?: FirErrorTypeRefImpl(session, null, "No type for block")

        return block.compose()
    }

    private fun commonSuperType(types: List<FirTypeRef>): FirTypeRef? {
        val commonSuperType = with(NewCommonSuperTypeCalculator) {
            with(inferenceComponents.ctx) {
                commonSuperType(types.map { it.coneTypeUnsafe() })
            }
        } as ConeKotlinType
        return FirResolvedTypeRefImpl(session, null, commonSuperType, emptyList())
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Any?): CompositeTransformResult<FirStatement> {
        whenExpression.transformChildren(this, data)
        if (whenExpression.resultType !is FirResolvedTypeRef) {
            val type = commonSuperType(whenExpression.branches.mapNotNull {
                val expression = it.result.statements.lastOrNull() as? FirExpression
                if (expression != null) {
                    (expression.resultType as? FirResolvedTypeRef) ?: FirErrorTypeRefImpl(session, null, "No type for when branch result")
                } else {
                    FirImplicitUnitTypeRef(session, null)
                }
            })
            if (type != null) whenExpression.resultType = type
        }
        return whenExpression.compose()
    }

    override fun transformWhenSubjectExpression(
        whenSubjectExpression: FirWhenSubjectExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        val parentWhen = whenSubjectExpression.whenSubject.whenExpression
        val subjectType = parentWhen.subject?.resultType ?: parentWhen.subjectVariable?.returnTypeRef
        if (subjectType != null) {
            whenSubjectExpression.resultType = subjectType
        }
        return whenSubjectExpression.compose()
    }

    override fun <T> transformConstExpression(constExpression: FirConstExpression<T>, data: Any?): CompositeTransformResult<FirStatement> {
        val expectedType = data as FirTypeRef?

        val kind = constExpression.kind
        if (expectedType == null || expectedType is FirImplicitTypeRef || expectedType == null ||
            kind == IrConstKind.Null || kind == IrConstKind.Boolean || kind == IrConstKind.Char
        ) {
            val symbol = when (kind) {
                IrConstKind.Null -> StandardClassIds.Nothing(symbolProvider)
                IrConstKind.Boolean -> StandardClassIds.Boolean(symbolProvider)
                IrConstKind.Char -> StandardClassIds.Char(symbolProvider)
                IrConstKind.Byte -> StandardClassIds.Byte(symbolProvider)
                IrConstKind.Short -> StandardClassIds.Short(symbolProvider)
                IrConstKind.Int -> StandardClassIds.Int(symbolProvider)
                IrConstKind.Long -> StandardClassIds.Long(symbolProvider)
                IrConstKind.String -> StandardClassIds.String(symbolProvider)
                IrConstKind.Float -> StandardClassIds.Float(symbolProvider)
                IrConstKind.Double -> StandardClassIds.Double(symbolProvider)
            }

            val type = ConeClassTypeImpl(symbol.toLookupTag(), emptyArray(), isNullable = kind == IrConstKind.Null)

            constExpression.resultType = FirResolvedTypeRefImpl(session, null, type, emptyList())
        } else {
            constExpression.resultType = expectedType
        }


        return super.transformConstExpression(constExpression, data)
    }

    private var FirExpression.resultType: FirTypeRef
        get() = typeRef
        set(type) {
            replaceTypeRef(type)
        }


    override fun transformDeclaration(declaration: FirDeclaration, data: Any?): CompositeTransformResult<FirDeclaration> {
        val prevContainer = container
        container = declaration
        val result = super.transformDeclaration(declaration, data)
        container = prevContainer
        return result
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): CompositeTransformResult<FirStatement> {
        annotationCall.resultType = annotationCall.annotationTypeRef
        return (annotationCall.transformChildren(this, data) as FirStatement).compose()
    }

    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Any?): CompositeTransformResult<FirDeclaration> {
        if (namedFunction.returnTypeRef !is FirImplicitTypeRef && implicitTypeOnly) return namedFunction.compose()
        if (namedFunction.returnTypeRef is FirImplicitTypeRef) {
            namedFunction.transformReturnTypeRef(StoreType, FirComputingImplicitTypeRef)
        }

        val receiverTypeRef = namedFunction.receiverTypeRef
        fun transform(): CompositeTransformResult<FirDeclaration> {
            localScopes.lastOrNull()?.storeDeclaration(namedFunction)
            return withScopeCleanup(scopes) {
                scopes.addIfNotNull(receiverTypeRef?.coneTypeSafe<ConeKotlinType>()?.scope(session, scopeSession))


                val result = super.transformNamedFunction(namedFunction, namedFunction.returnTypeRef).single as FirNamedFunction
                val body = result.body
                if (result.returnTypeRef is FirImplicitTypeRef && body != null) {
                    result.transformReturnTypeRef(this, body.resultType)
                    result
                } else {
                    result
                }.compose()
            }
        }

        return if (receiverTypeRef != null) {
            withLabelAndReceiverType(namedFunction.name, namedFunction, receiverTypeRef.coneTypeUnsafe()) { transform() }
        } else {
            transform()
        }
    }

    override fun transformVariable(variable: FirVariable, data: Any?): CompositeTransformResult<FirDeclaration> {
        val variable = super.transformVariable(variable, variable.returnTypeRef).single as FirVariable
        val initializer = variable.initializer
        if (variable.returnTypeRef is FirImplicitTypeRef) {
            when {
                initializer != null -> {
                    variable.transformReturnTypeRef(
                        this,
                        when (val resultType = initializer.resultType) {
                            is FirImplicitTypeRef -> FirErrorTypeRefImpl(
                                session,
                                null,
                                "No result type for initializer"
                            )
                            else -> resultType
                        }
                    )
                }
                variable.delegate != null -> {
                    // TODO: type from delegate
                    variable.transformReturnTypeRef(
                        this,
                        FirErrorTypeRefImpl(
                            session,
                            null,
                            "Not supported: type from delegate"
                        )
                    )
                }
            }
        }
        if (variable !is FirProperty) {
            localScopes.lastOrNull()?.storeDeclaration(variable)
        }
        return variable.compose()
    }

    override fun transformProperty(property: FirProperty, data: Any?): CompositeTransformResult<FirDeclaration> {
        if (property.returnTypeRef !is FirImplicitTypeRef && implicitTypeOnly) return property.compose()
        return withScopeCleanup(localScopes) {
            localScopes.addIfNotNull(primaryConstructorParametersScope)
            transformVariable(property, data)
        }
    }

    override fun transformExpression(expression: FirExpression, data: Any?): CompositeTransformResult<FirStatement> {
        if (expression.resultType is FirImplicitTypeRef) {
            val type = FirErrorTypeRefImpl(session, expression.psi, "Type calculating for ${expression::class} is not supported")
            expression.resultType = type
        }
        return super.transformExpression(expression, data)
    }

    fun <D> FirElement.visitNoTransform(transformer: FirTransformer<D>, data: D) {
        val result = this.transform<FirElement, D>(transformer, data)
        require(result.single === this) { "become ${result.single}: `${result.single.render()}`, was ${this}: `${this.render()}`" }
    }


    override fun transformGetClassCall(getClassCall: FirGetClassCall, data: Any?): CompositeTransformResult<FirStatement> {
        val transformedGetClassCall = super.transformGetClassCall(getClassCall, data).single as FirGetClassCall
        val kClassSymbol = ClassId.fromString("kotlin/reflect/KClass")(session.service())
        transformedGetClassCall.resultType =
                FirResolvedTypeRefImpl(
                    session,
                    null,
                    kClassSymbol.constructType(arrayOf(transformedGetClassCall.argument.resultType.coneTypeUnsafe()), false),
                    emptyList()
                )
        return transformedGetClassCall.compose()
    }
}


class ReturnTypeCalculatorWithJump(val session: FirSession) : ReturnTypeCalculator {


    val storeType = object : FirTransformer<FirTypeRef>() {
        override fun <E : FirElement> transformElement(element: E, data: FirTypeRef): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformImplicitTypeRef(
            implicitTypeRef: FirImplicitTypeRef,
            data: FirTypeRef
        ): CompositeTransformResult<FirTypeRef> {
            return data.compose()
        }
    }

    private fun cycleErrorType(declaration: FirTypedDeclaration): FirResolvedTypeRef? {
        if (declaration.returnTypeRef is FirComputingImplicitTypeRef) {
            declaration.transformReturnTypeRef(storeType, FirErrorTypeRefImpl(session, null, "cycle"))
            return declaration.returnTypeRef as FirResolvedTypeRef
        }
        return null
    }

    override fun tryCalculateReturnType(declaration: FirTypedDeclaration): FirResolvedTypeRef {

        if (declaration is FirValueParameter && declaration.returnTypeRef is FirImplicitTypeRef) {
            // TODO?
            declaration.transformReturnTypeRef(storeType, FirErrorTypeRefImpl(session, null, "Unsupported: implicit VP type"))
        }
        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is FirResolvedTypeRef) return returnTypeRef
        cycleErrorType(declaration)?.let { return it }
        require(declaration is FirCallableMemberDeclaration) { "${declaration::class}: ${declaration.render()}" }


        val symbol = declaration.symbol as ConeCallableSymbol
        val id = symbol.callableId

        val provider = session.service<FirProvider>()

        val file = provider.getFirCallableContainerFile(symbol)

        val outerClasses = generateSequence(id.classId) { classId ->
            classId.outerClassId
        }.mapTo(mutableListOf()) { provider.getFirClassifierByFqName(it) }

        if (file == null || outerClasses.any { it == null }) return FirErrorTypeRefImpl(
            session,
            null,
            "I don't know what todo"
        )

        declaration.transformReturnTypeRef(storeType, FirComputingImplicitTypeRef)

        val transformer = FirDesignatedBodyResolveTransformer(
            (listOf(file) + outerClasses.filterNotNull().asReversed() + listOf(declaration)).iterator(),
            file.session
        )

        file.transform<FirElement, Any?>(transformer, null)


        val newReturnTypeRef = declaration.returnTypeRef
        cycleErrorType(declaration)?.let { return it }
        require(newReturnTypeRef is FirResolvedTypeRef) { declaration.render() }
        return newReturnTypeRef
    }
}


class FirDesignatedBodyResolveTransformer(val designation: Iterator<FirElement>, session: FirSession) :
    FirBodyResolveTransformer(session, implicitTypeOnly = true) {

    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        if (designation.hasNext()) {
            designation.next().visitNoTransform(this, data)
            return element.compose()
        }
        return super.transformElement(element, data)
    }
}


@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirImplicitTypeBodyResolveTransformerAdapter : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val transformer = FirBodyResolveTransformer(file.session, implicitTypeOnly = true)
        return file.transform(transformer, null)
    }
}


@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirBodyResolveTransformerAdapter : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val transformer = FirBodyResolveTransformer(file.session, implicitTypeOnly = false)
        return file.transform(transformer, null)
    }
}


inline fun <reified T : FirElement> ConeSymbol.firUnsafe(): T {
    require(this is FirBasedSymbol<*>) {
        "Not a fir based symbol: ${this}"
    }
    val fir = this.fir
    require(fir is T) {
        "Not an expected fir element type = ${T::class}, symbol = ${this}, fir = ${fir.renderWithType()}"
    }
    return fir
}

inline fun <reified T : FirElement> ConeSymbol.firSafeNullable(): T? {
    if (this !is FirBasedSymbol<*>) return null
    return fir as? T
}


interface ReturnTypeCalculator {
    fun tryCalculateReturnType(declaration: FirTypedDeclaration): FirResolvedTypeRef
}

private object StoreNameReference : FirTransformer<FirNamedReference>() {
    override fun <E : FirElement> transformElement(element: E, data: FirNamedReference): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformNamedReference(
        namedReference: FirNamedReference,
        data: FirNamedReference
    ): CompositeTransformResult<FirNamedReference> {
        return data.compose()
    }
}

internal object StoreType : FirTransformer<FirTypeRef>() {
    override fun <E : FirElement> transformElement(element: E, data: FirTypeRef): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: FirTypeRef): CompositeTransformResult<FirTypeRef> {
        return data.compose()
    }
}

private object ReplaceInArguments : FirTransformer<Map<FirElement, FirElement>>() {
    override fun <E : FirElement> transformElement(element: E, data: Map<FirElement, FirElement>): CompositeTransformResult<E> {
        return ((data[element] ?: element) as E).compose()
    }

    override fun transformFunctionCall(
        functionCall: FirFunctionCall,
        data: Map<FirElement, FirElement>
    ): CompositeTransformResult<FirStatement> {
        return (functionCall.transformChildren(this, data) as FirStatement).compose()
    }

    override fun transformNamedArgumentExpression(
        namedArgumentExpression: FirNamedArgumentExpression,
        data: Map<FirElement, FirElement>
    ): CompositeTransformResult<FirStatement> {
        return (namedArgumentExpression.transformChildren(this, data) as FirStatement).compose()
    }

    override fun transformLambdaArgumentExpression(
        lambdaArgumentExpression: FirLambdaArgumentExpression,
        data: Map<FirElement, FirElement>
    ): CompositeTransformResult<FirStatement> {
        return (lambdaArgumentExpression.transformChildren(this, data) as FirStatement).compose()
    }
}