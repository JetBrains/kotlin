/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionWithSmartcastImpl
import org.jetbrains.kotlin.fir.references.FirExplicitThisReference
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowInferenceContext
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.addImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
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
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContextDelegate
import org.jetbrains.kotlin.utils.addIfNotNull

open class FirBodyResolveTransformer(
    final override val session: FirSession,
    phase: FirResolvePhase,
    implicitTypeOnly: Boolean,
    final override val scopeSession: ScopeSession = ScopeSession()
) : FirAbstractPhaseTransformer<Any?>(phase), BodyResolveComponents {
    var implicitTypeOnly: Boolean = implicitTypeOnly
        private set

    final override val returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorWithJump(session, scopeSession)
    final override val noExpectedType = FirImplicitTypeRefImpl(null)
    private inline val builtinTypes: BuiltinTypes get() = session.builtinTypes

    final override val symbolProvider = session.service<FirSymbolProvider>()

    private var packageFqName = FqName.ROOT
    final override lateinit var file: FirFile
        private set

    private var _container: FirDeclaration? = null
    final override var container: FirDeclaration
        get() = _container!!
        private set(value) {
            _container = value
        }

    private val localScopes = mutableListOf<FirLocalScope>()
    private val topLevelScopes = mutableListOf<FirScope>()
    final override val implicitReceiverStack: ImplicitReceiverStackImpl = ImplicitReceiverStackImpl()
    final override val inferenceComponents = inferenceComponents(session, returnTypeCalculator, scopeSession)
    final override val samResolver: FirSamResolver = FirSamResolverImpl(session, scopeSession)

    private var primaryConstructorParametersScope: FirLocalScope? = null

    private val callCompleter: FirCallCompleter = FirCallCompleter(this)
    private val qualifiedResolver: FirQualifiedNameResolver = FirQualifiedNameResolver(this)
    final override val resolutionStageRunner: ResolutionStageRunner = ResolutionStageRunner(inferenceComponents)
    private val callResolver: FirCallResolver = FirCallResolver(
        this,
        topLevelScopes,
        localScopes,
        implicitReceiverStack,
        qualifiedResolver
    )

    private val syntheticCallGenerator: FirSyntheticCallGenerator = FirSyntheticCallGenerator(this)
    private val dataFlowAnalyzer: FirDataFlowAnalyzer = FirDataFlowAnalyzer(this)

    override val <D> AbstractFirBasedSymbol<D>.phasedFir: D where D : FirDeclaration, D : FirSymbolOwner<D>
        get() {
            val requiredPhase = transformerPhase.prev
            return phasedFir(session, requiredPhase)
        }

    override fun transformFile(file: FirFile, data: Any?): CompositeTransformResult<FirFile> {
        packageFqName = file.packageFqName
        this.file = file
        return withScopeCleanup(topLevelScopes) {
            topLevelScopes.addImportingScopes(file, session, scopeSession)
            super.transformFile(file, data)
        }
    }

    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    override fun transformConstructor(constructor: FirConstructor, data: Any?): CompositeTransformResult<FirDeclaration> {
        if (implicitTypeOnly) return constructor.compose()
        return transformFunction(constructor, data)
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: Any?
    ): CompositeTransformResult<FirDeclaration> {
        if (implicitTypeOnly) return anonymousInitializer.compose()
        return withScopeCleanup(localScopes) {
            dataFlowAnalyzer.enterInitBlock(anonymousInitializer)
            localScopes.addIfNotNull(primaryConstructorParametersScope)
            transformDeclaration(anonymousInitializer, data).also {
                dataFlowAnalyzer.exitInitBlock(it.single as FirAnonymousInitializer)
            }
        }
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Any?): CompositeTransformResult<FirTypeRef> {
        if (data == null)
            return implicitTypeRef.compose()
        require(data is FirTypeRef)
        return data.compose()
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: Any?): CompositeTransformResult<FirDeclaration> {
        localScopes.lastOrNull()?.storeDeclaration(valueParameter)
        if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
            valueParameter.resolvePhase = transformerPhase
            return valueParameter.compose() // TODO
        }
        return transformDeclaration(valueParameter, valueParameter.returnTypeRef)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): CompositeTransformResult<FirDeclaration> {
        val oldConstructorScope = primaryConstructorParametersScope
        primaryConstructorParametersScope = null
        val type = regularClass.defaultType()
        val result = withLabelAndReceiverType(regularClass.name, regularClass, type) {
            val constructor = regularClass.declarations.firstOrNull() as? FirConstructor
            if (constructor?.isPrimary == true) {
                primaryConstructorParametersScope = FirLocalScope().apply {
                    constructor.valueParameters.forEach { this.storeDeclaration(it) }
                }
            }
            transformDeclaration(regularClass, data)
        }
        primaryConstructorParametersScope = oldConstructorScope
        return result
    }

    override fun transformUncheckedNotNullCast(
        uncheckedNotNullCast: FirUncheckedNotNullCast,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        val notNullCast = transformExpression(uncheckedNotNullCast, data).single as FirUncheckedNotNullCast
        val resultType = notNullCast.expression.resultType
        notNullCast.resultType =
            resultType.withReplacedConeType(resultType.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NOT_NULL))
        return notNullCast.compose()
    }

    override fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Any?): CompositeTransformResult<FirStatement> {
        val symbolProvider = session.service<FirSymbolProvider>()
        val resolved = transformExpression(typeOperatorCall, data).single
        when ((resolved as FirTypeOperatorCall).operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                resolved.resultType = FirResolvedTypeRefImpl(
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
                        resolved.conversionTypeRef.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NULLABLE)
                    )
            }
            else -> error("Unknown type operator")
        }
        dataFlowAnalyzer.exitTypeOperatorCall(typeOperatorCall)
        return resolved.compose()
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {

        var result = when (val callee = qualifiedAccessExpression.calleeReference) {
            is FirExplicitThisReference -> {
                val labelName = callee.labelName
                val implicitReceiver = implicitReceiverStack[labelName]
                callee.boundSymbol = implicitReceiver?.boundSymbol
                qualifiedAccessExpression.resultType = FirResolvedTypeRefImpl(
                    null, implicitReceiver?.type ?: ConeKotlinErrorType("Unresolved this@$labelName"),
                    emptyList()
                )
                qualifiedAccessExpression
            }
            is FirSuperReference -> {
                if (callee.superTypeRef is FirResolvedTypeRef) {
                    qualifiedAccessExpression.resultType = callee.superTypeRef
                } else {
                    val superTypeRef = implicitReceiverStack.lastDispatchReceiver()
                        ?.boundSymbol?.phasedFir?.superTypeRefs?.firstOrNull()
                        ?: FirErrorTypeRefImpl(qualifiedAccessExpression.psi, "No super type")
                    qualifiedAccessExpression.resultType = superTypeRef
                    callee.replaceSuperTypeRef(superTypeRef)
                }
                qualifiedAccessExpression
            }
            is FirDelegateFieldReference -> {
                val delegateFieldSymbol = callee.resolvedSymbol
                qualifiedAccessExpression.resultType = delegateFieldSymbol.delegate.typeRef
                qualifiedAccessExpression
            }
            is FirResolvedCallableReference -> {
                if (qualifiedAccessExpression.typeRef !is FirResolvedTypeRef) {
                    storeTypeFromCallee(qualifiedAccessExpression)
                }
                qualifiedAccessExpression
            }
            else -> {
                val transformedCallee = callResolver.resolveVariableAccessAndSelectCandidate(qualifiedAccessExpression, file)
                // NB: here we can get raw expression because of dropped qualifiers (see transform callee),
                // so candidate existence must be checked before calling completion
                if (transformedCallee is FirQualifiedAccessExpression && transformedCallee.candidate() != null) {
                    callCompleter.completeCall(transformedCallee, data as? FirTypeRef)
                } else {
                    transformedCallee
                }
            }
        }
        if (result is FirQualifiedAccessExpression) {
            result = transformQualifiedAccessUsingSmartcastInfo(result)
            dataFlowAnalyzer.exitQualifiedAccessExpression(result)
        }
        return result.compose()
    }

    private fun transformQualifiedAccessUsingSmartcastInfo(qualifiedAccessExpression: FirQualifiedAccessExpression): FirQualifiedAccessExpression {
        val typesFromSmartCast = dataFlowAnalyzer.getTypeUsingSmartcastInfo(qualifiedAccessExpression) ?: return qualifiedAccessExpression
        val allTypes = typesFromSmartCast.toMutableList().also {
            it += qualifiedAccessExpression.resultType.coneTypeUnsafe<ConeKotlinType>()
        }
        val intersectedType = ConeTypeIntersector.intersectTypes(inferenceComponents.ctx as ConeInferenceContext, allTypes)
        // TODO: add check that intersectedType is not equal to original type
        return FirExpressionWithSmartcastImpl(qualifiedAccessExpression, typesFromSmartCast).also {
            it.resultType = FirResolvedTypeRefImpl(qualifiedAccessExpression.resultType.psi, intersectedType, qualifiedAccessExpression.resultType.annotations)
        }
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        // val resolvedAssignment = transformCallee(variableAssignment)
        val resolvedAssignment = callResolver.resolveVariableAccessAndSelectCandidate(variableAssignment, file)
        val result = if (resolvedAssignment is FirVariableAssignment) {
            val completeAssignment = callCompleter.completeCall(resolvedAssignment, noExpectedType)
            val expectedType = typeFromCallee(completeAssignment)
            completeAssignment.transformRValue(this, expectedType)
        } else {
            // This can happen in erroneous code only
            resolvedAssignment
        }
        // TODO: maybe replace with FirAbstractAssignment for performance?
        (result as? FirVariableAssignment)?.let { dataFlowAnalyzer.exitVariableAssignment(it) }
        return result.compose()
    }

    override fun transformAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Any?): CompositeTransformResult<FirDeclaration> {
        return when (data) {
            null -> {
                anonymousFunction.compose()
            }
            is LambdaResolution -> {
                transformAnonymousFunctionWithLambdaResolution(anonymousFunction, data).compose()
            }
            is FirTypeRef -> {
                val resolvedLambdaAtom = (data as? FirResolvedTypeRef)?.let {
                    extractLambdaInfoFromFunctionalType(
                        it.type, it, anonymousFunction
                    )
                }
                var af = anonymousFunction
                val valueParameters =
                    if (resolvedLambdaAtom == null) af.valueParameters
                    else {
                        val singleParameterType = resolvedLambdaAtom.parameters.singleOrNull()
                        val itParam = when {
                            af.valueParameters.isEmpty() && singleParameterType != null ->
                                FirValueParameterImpl(
                                    session,
                                    null,
                                    Name.identifier("it"),
                                    FirResolvedTypeRefImpl(null, singleParameterType, emptyList()),
                                    defaultValue = null,
                                    isCrossinline = false,
                                    isNoinline = false,
                                    isVararg = false
                                )
                            else -> null
                        }
                        if (itParam != null) {
                            listOf(itParam)
                        } else {
                            af.valueParameters.mapIndexed { index, param ->
                                if (param.returnTypeRef is FirResolvedTypeRef) {
                                    param
                                } else {
                                    param.transformReturnTypeRef(
                                        StoreType,
                                        param.returnTypeRef.resolvedTypeFromPrototype(
                                            resolvedLambdaAtom.parameters[index]
                                        )
                                    )
                                    param
                                }
                            }
                        }

                    }
                val returnTypeRefFromResolvedAtom = resolvedLambdaAtom?.returnType?.let { af.returnTypeRef.resolvedTypeFromPrototype(it) }
                af = af.copy(
                    receiverTypeRef = af.receiverTypeRef?.takeIf { it !is FirImplicitTypeRef }
                        ?: resolvedLambdaAtom?.receiver?.let { af.receiverTypeRef?.resolvedTypeFromPrototype(it) },
                    valueParameters = valueParameters,
                    returnTypeRef = (af.returnTypeRef as? FirResolvedTypeRef)
                        ?: returnTypeRefFromResolvedAtom
                        ?: af.returnTypeRef
                )
                af = af.transformValueParameters(ImplicitToErrorTypeTransformer, null) as FirAnonymousFunction
                val bodyExpectedType = returnTypeRefFromResolvedAtom ?: data
                af = transformFunction(af, bodyExpectedType).single as FirAnonymousFunction
                af = af.copy(
                    returnTypeRef = af.body?.resultType ?: FirErrorTypeRefImpl(af.psi, "No result type for lambda")
                )
                af.replaceTypeRef(af.constructFunctionalTypeRef(session))
                af.compose()
            }
            else -> {
                transformFunction(anonymousFunction, data)
            }
        }
    }

    private object ImplicitToErrorTypeTransformer : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformValueParameter(valueParameter: FirValueParameter, data: Nothing?): CompositeTransformResult<FirDeclaration> {
            if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
                valueParameter.transformReturnTypeRef(
                    StoreType,
                    valueParameter.returnTypeRef.resolvedTypeFromPrototype(ConeKotlinErrorType("No type for parameter"))
                )
            }
            return valueParameter.compose()
        }
    }

    private fun transformAnonymousFunctionWithLambdaResolution(
        anonymousFunction: FirAnonymousFunction, lambdaResolution: LambdaResolution
    ): FirAnonymousFunction {
        val receiverTypeRef = anonymousFunction.receiverTypeRef
        fun transform(): FirAnonymousFunction {
            val expectedReturnType =
                lambdaResolution.expectedReturnTypeRef ?: anonymousFunction.returnTypeRef.takeUnless { it is FirImplicitTypeRef }
            val result = transformFunction(anonymousFunction, expectedReturnType).single as FirAnonymousFunction
            val body = result.body
            return if (result.returnTypeRef is FirImplicitTypeRef && body != null) {
                result.transformReturnTypeRef(this, body.resultType)
                result
            } else {
                result
            }
        }

        val label = anonymousFunction.label
        return if (label != null && receiverTypeRef != null) {
            withLabelAndReceiverType(Name.identifier(label.name), anonymousFunction, receiverTypeRef.coneTypeUnsafe()) {
                transform()
            }
        } else {
            transform()
        }
    }

    data class LambdaResolution(val expectedReturnTypeRef: FirResolvedTypeRef?)


    override fun transformCatch(catch: FirCatch, data: Any?): CompositeTransformResult<FirCatch> {
        dataFlowAnalyzer.enterCatchClause(catch)
        return withScopeCleanup(localScopes) {
            localScopes += FirLocalScope()
            catch.transformParameter(this, noExpectedType)
            catch.transformBlock(this, null)
        }.also { dataFlowAnalyzer.exitCatchClause(it) }.compose()
    }

    override fun transformTryExpression(tryExpression: FirTryExpression, data: Any?): CompositeTransformResult<FirStatement> {
        if (tryExpression.calleeReference is FirResolvedCallableReference && tryExpression.resultType !is FirImplicitTypeRef) {
            return tryExpression.compose()
        }

        dataFlowAnalyzer.enterTryExpression(tryExpression)
        tryExpression.transformTryBlock(this, null)
        dataFlowAnalyzer.exitTryMainBlock(tryExpression)
        tryExpression.transformCatches(this, null)

        @Suppress("NAME_SHADOWING")
        var result = syntheticCallGenerator.generateCalleeForTryExpression(tryExpression)?.let {
            val expectedTypeRef = data as FirTypeRef?
            callCompleter.completeCall(it, expectedTypeRef)
        } ?: run {
            tryExpression.resultType = FirErrorTypeRefImpl(null, "")
            tryExpression
        }

        result = if (result.finallyBlock != null) {
            result.also(dataFlowAnalyzer::enterFinallyBlock)
                .transformFinallyBlock(this, noExpectedType)
                .also(dataFlowAnalyzer::exitFinallyBlock)
        } else {
            result
        }
        dataFlowAnalyzer.exitTryExpression(result)
        return result.compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Any?): CompositeTransformResult<FirStatement> {
        dataFlowAnalyzer.enterFunctionCall(functionCall)
        if (functionCall.calleeReference is FirResolvedCallableReference && functionCall.resultType is FirImplicitTypeRef) {
            storeTypeFromCallee(functionCall)
        }
        if (functionCall.calleeReference !is FirSimpleNamedReference) return functionCall.compose()
        functionCall.transform<FirFunctionCall, InvocationKind?>(InvocationKindTransformer, null)
        val expectedTypeRef = data as FirTypeRef?
        val completeInference =
            try {
                val initialExplicitReceiver = functionCall.explicitReceiver
                val resultExpression = callResolver.resolveCallAndSelectCandidate(functionCall, expectedTypeRef, file)
                val resultExplicitReceiver = resultExpression.explicitReceiver
                if (initialExplicitReceiver !== resultExplicitReceiver && resultExplicitReceiver is FirQualifiedAccess) {
                    // name.invoke() case
                    callCompleter.completeCall(resultExplicitReceiver, noExpectedType)
                }
                callCompleter.completeCall(resultExpression, expectedTypeRef)
            } catch (e: Throwable) {
                throw RuntimeException("While resolving call ${functionCall.render()}", e)
            }

        dataFlowAnalyzer.exitFunctionCall(completeInference)
        return completeInference.compose()

    }

    //    override fun transformNamedReference(namedReference: FirNamedReference, data: Any?): CompositeTransformResult<FirNamedReference> {
//        if (namedReference is FirErrorNamedReference || namedReference is FirResolvedCallableReference) return namedReference.compose()
//        val referents = data as? List<ConeCallableSymbol> ?: return namedReference.compose()
//        return createResolvedNamedReference(namedReference, referents).compose()
//    }


    override fun transformBlock(block: FirBlock, data: Any?): CompositeTransformResult<FirStatement> {
        dataFlowAnalyzer.enterBlock(block)
        @Suppress("NAME_SHADOWING")
        val block = block.transformChildren(this, data) as FirBlock
        val statement = block.statements.lastOrNull()

        val resultExpression = when (statement) {
            is FirReturnExpression -> statement.result
            is FirExpression -> statement
            else -> null
        }
        block.resultType = if (resultExpression == null) {
            FirImplicitUnitTypeRef(block.psi)
        } else {
            (resultExpression.resultType as? FirResolvedTypeRef) ?: FirErrorTypeRefImpl(null, "No type for block")
        }
        dataFlowAnalyzer.exitBlock(block)
        return block.compose()
    }

    override fun <E : FirTargetElement> transformJump(jump: FirJump<E>, data: Any?): CompositeTransformResult<FirStatement> {
        val result = transformExpression(jump, data)
        dataFlowAnalyzer.exitJump(jump)
        return result
    }

    override fun transformThrowExpression(throwExpression: FirThrowExpression, data: Any?): CompositeTransformResult<FirStatement> {
        return transformExpression(throwExpression, data).also {
            dataFlowAnalyzer.exitThrowExceptionNode(it.single as FirThrowExpression)
        }
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Any?): CompositeTransformResult<FirStatement> {
        if (whenExpression.calleeReference is FirResolvedCallableReference && whenExpression.resultType !is FirImplicitTypeRef) {
            return whenExpression.compose()
        }
        dataFlowAnalyzer.enterWhenExpression(whenExpression)
        return withScopeCleanup(localScopes) with@{
            if (whenExpression.subjectVariable != null) {
                localScopes += FirLocalScope()
            }
            whenExpression.transformSubject(this, noExpectedType)
            if (whenExpression.isOneBranch()) {
                whenExpression.transformBranches(this, noExpectedType)
                whenExpression.resultType = whenExpression.branches.first().result.resultType
                dataFlowAnalyzer.exitWhenExpression(whenExpression)
                return@with whenExpression.compose()
            }
            whenExpression.transformBranches(this, null)

            @Suppress("NAME_SHADOWING")
            val whenExpression = syntheticCallGenerator.generateCalleeForWhenExpression(whenExpression) ?: run {
                dataFlowAnalyzer.exitWhenExpression(whenExpression)
                whenExpression.resultType = FirErrorTypeRefImpl(null, "")
                return@with whenExpression.compose()
            }

            val expectedTypeRef = data as FirTypeRef?
            val result = callCompleter.completeCall(whenExpression, expectedTypeRef)
            dataFlowAnalyzer.exitWhenExpression(result)
            result.compose()
        }
    }

    private fun FirWhenExpression.isOneBranch(): Boolean {
        if (branches.size == 1) return true
        if (branches.size > 2) return false
        val lastBranch = branches.last()
        return lastBranch.condition is FirElseIfTrueCondition && lastBranch.result is FirEmptyExpressionBlock
    }

    override fun transformWhenBranch(whenBranch: FirWhenBranch, data: Any?): CompositeTransformResult<FirWhenBranch> {
        return whenBranch.also { dataFlowAnalyzer.enterWhenBranchCondition(whenBranch) }
            .transformCondition(this, data).also { dataFlowAnalyzer.exitWhenBranchCondition(it) }
            .transformResult(this, data).also { dataFlowAnalyzer.exitWhenBranchResult(it) }
            .compose()
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
        if (expectedType == null || expectedType is FirImplicitTypeRef ||
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

            constExpression.resultType = FirResolvedTypeRefImpl(null, type, emptyList())
        } else {
            constExpression.resultType = if (kind != IrConstKind.Null) {
                expectedType.resolvedTypeFromPrototype(
                    expectedType.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NOT_NULL)
                )
            } else {
                expectedType
            }
        }


        return transformExpression(constExpression, data).also {
            dataFlowAnalyzer.exitConstExpresion(it.single as FirConstExpression<*>)
        }
    }

    override fun transformDeclaration(declaration: FirDeclaration, data: Any?): CompositeTransformResult<FirDeclaration> {
        return withContainer(declaration) {
            super.transformDeclaration(declaration, data)
        }
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: Any?): CompositeTransformResult<FirStatement> {
        dataFlowAnalyzer.enterAnnotationCall(annotationCall)
        return (annotationCall.transformChildren(this, data) as FirAnnotationCall).also {
            dataFlowAnalyzer.exitAnnotationCall(it)
        }.compose()
    }

    override fun <F : FirFunction<F>> transformFunction(function: FirFunction<F>, data: Any?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup(localScopes) {
            localScopes += FirLocalScope()
            dataFlowAnalyzer.enterFunction(function)
            transformDeclaration(function, data).also {
                val result = it.single as FirFunction<*>
                dataFlowAnalyzer.exitFunction(result)?.let { controlFlowGraph ->
                    result.transformControlFlowGraphReference(ControlFlowGraphReferenceTransformer, controlFlowGraph)
                }
            }
        }
    }

    private fun transformFunctionWithGivenSignature(
        function: FirFunction<*>,
        returnTypeRef: FirTypeRef,
        receiverTypeRef: FirTypeRef? = null
    ): CompositeTransformResult<FirDeclaration> {
        if (function is FirNamedFunction) {
            localScopes.lastOrNull()?.storeDeclaration(function)
        }
        val result = transformFunction(function, returnTypeRef).single as FirFunction<*>
        val body = result.body
        return if (result.returnTypeRef is FirImplicitTypeRef && body != null) {
            result.transformReturnTypeRef(this, body.resultType)
            result
        } else {
            result
        }.compose()
    }

    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Any?): CompositeTransformResult<FirDeclaration> {
        val returnTypeRef = namedFunction.returnTypeRef
        if ((returnTypeRef !is FirImplicitTypeRef) && implicitTypeOnly) {
            return namedFunction.compose()
        }
        return withFullBodyResolve {
            if (returnTypeRef is FirImplicitTypeRef) {
                namedFunction.transformReturnTypeRef(StoreType, FirComputingImplicitTypeRef)
            }

            val receiverTypeRef = namedFunction.receiverTypeRef
            if (receiverTypeRef != null) {
                withLabelAndReceiverType(namedFunction.name, namedFunction, receiverTypeRef.coneTypeUnsafe()) {
                    transformFunctionWithGivenSignature(namedFunction, returnTypeRef, receiverTypeRef)
                }
            } else {
                transformFunctionWithGivenSignature(namedFunction, returnTypeRef)
            }
        }
    }

    private fun transformAccessor(
        accessor: FirPropertyAccessor,
        enhancedTypeRef: FirTypeRef,
        owner: FirVariable<*>
    ) {
        if (accessor is FirDefaultPropertyAccessor || accessor.body == null) {
            transformFunction(accessor, enhancedTypeRef)
        }
        val returnTypeRef = accessor.returnTypeRef
        if (returnTypeRef is FirImplicitTypeRef && enhancedTypeRef !is FirResolvedTypeRef) {
            accessor.transformReturnTypeRef(StoreType, FirComputingImplicitTypeRef)
        }
        val expectedReturnTypeRef = if (enhancedTypeRef is FirResolvedTypeRef && returnTypeRef !is FirResolvedTypeRef) {
            enhancedTypeRef
        } else {
            returnTypeRef
        }
        val receiverTypeRef = owner.receiverTypeRef
        if (receiverTypeRef != null) {
            withLabelAndReceiverType(owner.name, owner, receiverTypeRef.coneTypeUnsafe()) {
                transformFunctionWithGivenSignature(accessor, expectedReturnTypeRef, receiverTypeRef)
            }
        } else {
            transformFunctionWithGivenSignature(accessor, expectedReturnTypeRef)
        }
    }

    private fun storeVariableReturnType(variable: FirVariable<*>) {
        val initializer = variable.initializer
        if (variable.returnTypeRef is FirImplicitTypeRef) {
            when {
                initializer != null -> {
                    variable.transformReturnTypeRef(
                        this,
                        when (val resultType = initializer.resultType) {
                            is FirImplicitTypeRef -> FirErrorTypeRefImpl(
                                null,
                                "No result type for initializer"
                            )
                            else -> resultType
                        }
                    )
                }
                variable.getter != null && variable.getter !is FirDefaultPropertyAccessor -> {
                    variable.transformReturnTypeRef(
                        this,
                        when (val resultType = variable.getter?.returnTypeRef) {
                            is FirImplicitTypeRef -> FirErrorTypeRefImpl(
                                null,
                                "No result type for getter"
                            )
                            else -> resultType
                        }
                    )
                }
                else -> {
                    variable.transformReturnTypeRef(
                        this, FirErrorTypeRefImpl(null, "Cannot infer variable type without initializer / getter / delegate")
                    )
                }
            }
            if (variable.getter?.returnTypeRef is FirImplicitTypeRef) {
                variable.getter?.transformReturnTypeRef(this, variable.returnTypeRef)
            }
        }
    }

    private fun <F : FirVariable<F>> FirVariable<F>.transformAccessors() {
        var enhancedTypeRef = returnTypeRef
        getter?.let {
            transformAccessor(it, enhancedTypeRef, this)
        }
        if (returnTypeRef is FirImplicitTypeRef) {
            storeVariableReturnType(this)
            enhancedTypeRef = returnTypeRef
        }
        setter?.let {
            it.valueParameters[0].transformReturnTypeRef(StoreType, enhancedTypeRef)
            transformAccessor(it, enhancedTypeRef, this)
        }
    }

    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        transformExpression(wrappedDelegateExpression, data)
        with(wrappedDelegateExpression) {
            val delegateProviderTypeRef = delegateProvider.typeRef
            val useDelegateProvider = delegateProviderTypeRef is FirResolvedTypeRef &&
                    delegateProviderTypeRef !is FirErrorTypeRef &&
                    delegateProviderTypeRef.type !is ConeKotlinErrorType
            return if (useDelegateProvider) delegateProvider.compose() else expression.compose()
        }
    }

    override fun <F : FirVariable<F>> transformVariable(variable: FirVariable<F>, data: Any?): CompositeTransformResult<FirDeclaration> {
        variable.transformChildrenWithoutAccessors(this, variable.returnTypeRef)
        if (variable.initializer != null) {
            storeVariableReturnType(variable)
        }
        variable.transformAccessors()
        if (variable !is FirProperty) {
            localScopes.lastOrNull()?.storeDeclaration(variable)
        }
        variable.resolvePhase = transformerPhase
        dataFlowAnalyzer.exitVariableDeclaration(variable)
        return variable.compose()
    }

    override fun transformProperty(property: FirProperty, data: Any?): CompositeTransformResult<FirDeclaration> {
        val returnTypeRef = property.returnTypeRef
        if (returnTypeRef !is FirImplicitTypeRef && implicitTypeOnly) return property.compose()
        if (property.resolvePhase == transformerPhase) return property.compose()
        dataFlowAnalyzer.enterProperty(property)
        if (returnTypeRef is FirImplicitTypeRef) {
            property.transformReturnTypeRef(StoreType, FirComputingImplicitTypeRef)
        }
        return withFullBodyResolve {
            withScopeCleanup(localScopes) {
                localScopes.addIfNotNull(primaryConstructorParametersScope)
                withContainer(property) {
                    property.transformChildrenWithoutAccessors(this, returnTypeRef)
                    if (property.initializer != null) {
                        storeVariableReturnType(property)
                    }
                    withScopeCleanup(localScopes) {
                        localScopes.add(FirLocalScope().apply {
                            storeBackingField(property)
                        })
                        property.transformAccessors()
                    }
                }
                property.resolvePhase = transformerPhase
                val controlFlowGraph = dataFlowAnalyzer.exitProperty(property)
                property.transformControlFlowGraphReference(ControlFlowGraphReferenceTransformer, controlFlowGraph)
                property.compose()
            }
        }
    }

    override fun transformExpression(expression: FirExpression, data: Any?): CompositeTransformResult<FirStatement> {
        if (expression.resultType is FirImplicitTypeRef && expression !is FirWrappedExpression) {
            val type = FirErrorTypeRefImpl(expression.psi, "Type calculating for ${expression::class} is not supported")
            expression.resultType = type
        }
        return (expression.transformChildren(this, data) as FirStatement).compose()
    }

    override fun transformGetClassCall(getClassCall: FirGetClassCall, data: Any?): CompositeTransformResult<FirStatement> {
        val transformedGetClassCall = transformExpression(getClassCall, data).single as FirGetClassCall
        val kClassSymbol = ClassId.fromString("kotlin/reflect/KClass")(session.service())

        val typeOfExpression = when (val lhs = transformedGetClassCall.argument) {
            is FirResolvedQualifier -> {
                val classId = lhs.classId
                if (classId != null) {
                    val symbol = symbolProvider.getClassLikeSymbolByFqName(classId)!!
                    // TODO: Unify logic?
                    symbol.constructType(
                        Array(symbol.phasedFir.typeParameters.size) {
                            ConeStarProjection
                        },
                        isNullable = false
                    )
                } else {
                    lhs.resultType.coneTypeUnsafe<ConeKotlinType>()
                }
            }
            else -> lhs.resultType.coneTypeUnsafe<ConeKotlinType>()
        }

        transformedGetClassCall.resultType =
            FirResolvedTypeRefImpl(
                null,
                kClassSymbol.constructType(arrayOf(typeOfExpression), false),
                emptyList()
            )
        return transformedGetClassCall.compose()
    }

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        val booleanType = builtinTypes.booleanType
        return when (binaryLogicExpression.kind) {
            FirBinaryLogicExpression.OperationKind.AND ->
                binaryLogicExpression.also(dataFlowAnalyzer::enterBinaryAnd)
                    .transformLeftOperand(this, booleanType).also(dataFlowAnalyzer::exitLeftBinaryAndArgument)
                    .transformRightOperand(this, booleanType).also(dataFlowAnalyzer::exitBinaryAnd)

            FirBinaryLogicExpression.OperationKind.OR ->
                binaryLogicExpression.also(dataFlowAnalyzer::enterBinaryOr)
                    .transformLeftOperand(this, booleanType).also(dataFlowAnalyzer::exitLeftBinaryOrArgument)
                    .transformRightOperand(this, booleanType).also(dataFlowAnalyzer::exitBinaryOr)

        }.transformRestChildren(this, booleanType).also {
            it.resultType = booleanType
        }.compose()
    }

    override fun transformOperatorCall(operatorCall: FirOperatorCall, data: Any?): CompositeTransformResult<FirStatement> {
        val result = if (operatorCall.operation in FirOperation.BOOLEANS) {
            (operatorCall.transformChildren(this, noExpectedType) as FirOperatorCall).also {
                it.resultType = builtinTypes.booleanType
            }
        } else {
            transformExpression(operatorCall, data).single
        } as FirOperatorCall
        dataFlowAnalyzer.exitOperatorCall(result)
        return result.compose()
    }

    override fun transformWhileLoop(whileLoop: FirWhileLoop, data: Any?): CompositeTransformResult<FirStatement> {
        return whileLoop.also(dataFlowAnalyzer::enterWhileLoop)
            .transformCondition(this, data).also(dataFlowAnalyzer::exitWhileLoopCondition)
            .transformBlock(this, data).also(dataFlowAnalyzer::exitWhileLoop)
            .transformRestChildren(this, data).compose()
    }

    override fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Any?): CompositeTransformResult<FirStatement> {
        return doWhileLoop.also(dataFlowAnalyzer::enterDoWhileLoop)
            .transformBlock(this, data).also(dataFlowAnalyzer::enterDoWhileLoopCondition)
            .transformCondition(this, data).also(dataFlowAnalyzer::exitDoWhileLoop)
            .transformRestChildren(this, data).compose()
    }

    // ----------------------- Util functions -----------------------

    private inline fun <T> withLabelAndReceiverType(
        labelName: Name,
        owner: FirDeclaration,
        type: ConeKotlinType,
        block: () -> T
    ): T {
        val implicitReceiverValue = when (owner) {
            is FirRegularClass -> {
                ImplicitDispatchReceiverValue(owner.symbol, type, symbolProvider, session, scopeSession)
            }
            is FirFunction<*> -> {
                ImplicitExtensionReceiverValue(owner.symbol, type, session, scopeSession)
            }
            is FirVariable<*> -> {
                ImplicitExtensionReceiverValue(owner.symbol, type, session, scopeSession)
            }
            else -> {
                throw IllegalArgumentException("Incorrect label & receiver owner: ${owner.javaClass}")
            }
        }
        implicitReceiverStack.add(labelName, implicitReceiverValue)
        val result = block()
        implicitReceiverStack.pop(labelName)
        return result
    }

    protected inline fun <T> withScopeCleanup(scopes: MutableList<*>, crossinline l: () -> T): T {
        val sizeBefore = scopes.size
        return try {
            l()
        } finally {
            val size = scopes.size
            assert(size >= sizeBefore)
            repeat(size - sizeBefore) {
                scopes.let { it.removeAt(it.size - 1) }
            }
        }
    }

    private inline fun <T> withFullBodyResolve(crossinline l: () -> T): T {
        if (!implicitTypeOnly) return l()
        implicitTypeOnly = false
        return try {
            l()
        } finally {
            implicitTypeOnly = true
        }
    }

    internal fun <T> storeTypeFromCallee(access: T) where T : FirQualifiedAccess, T : FirExpression {
        access.resultType = callCompleter.typeFromCallee(access)
    }

    private fun <T> withContainer(declaration: FirDeclaration, f: () -> T): T {
        val prevContainer = _container
        _container = declaration
        val result = f()
        _container = prevContainer
        return result
    }

    fun <D> FirElement.visitNoTransform(transformer: FirTransformer<D>, data: D) {
        val result = this.transform<FirElement, D>(transformer, data)
        require(result.single === this) { "become ${result.single}: `${result.single.render()}`, was ${this}: `${this.render()}`" }
    }
}

private fun inferenceComponents(session: FirSession, returnTypeCalculator: ReturnTypeCalculator, scopeSession: ScopeSession) =
    InferenceComponents(object : ConeInferenceContext, TypeSystemInferenceExtensionContextDelegate, DataFlowInferenceContext {
        override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? {
            // TODO: implement
            return null
        }

        override fun TypeConstructorMarker.getApproximatedIntegerLiteralType(): KotlinTypeMarker {
            TODO("not implemented")
        }

        override val session: FirSession
            get() = session

        override fun KotlinTypeMarker.removeExactAnnotation(): KotlinTypeMarker {
            return this
        }

        override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
            require(this is ErrorTypeConstructor)
            return ConeClassErrorType(reason)
        }
    }, session, returnTypeCalculator, scopeSession)


class FirDesignatedBodyResolveTransformer(
    private val designation: Iterator<FirElement>,
    session: FirSession,
    scopeSession: ScopeSession = ScopeSession(),
    implicitTypeOnly: Boolean = true
) : FirBodyResolveTransformer(
    session,
    phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    implicitTypeOnly = implicitTypeOnly,
    scopeSession = scopeSession
) {
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
        val transformer = FirBodyResolveTransformer(file.fileSession, phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, implicitTypeOnly = true)
        return file.transform(transformer, null)
    }
}


@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirBodyResolveTransformerAdapter : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        // Despite of real phase is EXPRESSIONS, we state IMPLICIT_TYPES here, because DECLARATIONS previous phase is OK for us
        val transformer = FirBodyResolveTransformer(file.fileSession, phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, implicitTypeOnly = false)
        return file.transform(transformer, null)
    }
}


inline fun <reified T : FirElement> FirBasedSymbol<*>.firUnsafe(): T {
    val fir = this.fir
    require(fir is T) {
        "Not an expected fir element type = ${T::class}, symbol = ${this}, fir = ${fir.renderWithType()}"
    }
    return fir
}

internal inline var FirExpression.resultType: FirTypeRef
    get() = typeRef
    set(type) {
        replaceTypeRef(type)
    }
