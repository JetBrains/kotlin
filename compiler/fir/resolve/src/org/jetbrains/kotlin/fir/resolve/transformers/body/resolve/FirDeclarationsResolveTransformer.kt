/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildUnitExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirEmptyControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.inference.FirDelegatedPropertyInferenceSession
import org.jetbrains.kotlin.fir.resolve.inference.extractLambdaInfoFromFunctionalType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.symbols.constructStarProjectedType
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name

open class FirDeclarationsResolveTransformer(transformer: FirBodyResolveTransformer) : FirPartialBodyResolveTransformer(transformer) {
    private var primaryConstructorParametersScope: FirLocalScope? = null

    private var containingClass: FirRegularClass? = null

    private fun transformDeclarationContent(
        declaration: FirDeclaration, data: ResolutionMode
    ): CompositeTransformResult<FirDeclaration> {
        return context.withContainer(declaration) {
            declaration.replaceResolvePhase(transformerPhase)
            transformer.transformDeclarationContent(declaration, data)
        }
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: ResolutionMode
    ): CompositeTransformResult<FirDeclarationStatus> {
        return ((data as? ResolutionMode.WithStatus)?.status ?: declarationStatus).compose()
    }

    private fun prepareSignatureForBodyResolve(callableMember: FirCallableMemberDeclaration<*>) {
        callableMember.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
        callableMember.transformReceiverTypeRef(transformer, ResolutionMode.ContextIndependent)
        if (callableMember is FirFunction<*>) {
            callableMember.valueParameters.forEach {
                it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
                it.transformVarargTypeToArrayType()
            }
        }
    }

    protected inline fun <T> withTypeParametersOf(declaration: FirMemberDeclaration, crossinline l: () -> T): T {
        if (declaration.typeParameters.isEmpty()) return l()

        for (typeParameter in declaration.typeParameters) {
            (typeParameter as? FirTypeParameter)?.replaceResolvePhase(FirResolvePhase.STATUS)
            typeParameter.transformChildren(transformer, ResolutionMode.ContextIndependent)
        }

        val before = context.typeParametersScopes
        @OptIn(PrivateForInline::class)
        context.typeParametersScopes = context.typeParametersScopes.add(FirMemberTypeParameterScope(declaration))
        return try {
            l()
        } finally {
            @OptIn(PrivateForInline::class)
            context.typeParametersScopes = before
        }
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): CompositeTransformResult<FirProperty> {
        if (property is FirSyntheticProperty) {
            transformSimpleFunction(property.getter.delegate, data)
            property.replaceReturnTypeRef(property.getter.delegate.returnTypeRef)
            return property.compose()
        }
        return withTypeParametersOf(property) {
            if (property.isLocal) {
                prepareSignatureForBodyResolve(property)
                property.transformStatus(this, property.resolveStatus(property.status).mode())
                property.getter?.let { it.transformStatus(this, it.resolveStatus(it.status).mode()) }
                property.setter?.let { it.transformStatus(this, it.resolveStatus(it.status).mode()) }
                return@withTypeParametersOf transformLocalVariable(property)
            }
            val returnTypeRef = property.returnTypeRef
            if (returnTypeRef !is FirImplicitTypeRef && implicitTypeOnly) return@withTypeParametersOf property.compose()
            if (property.resolvePhase == transformerPhase) return@withTypeParametersOf property.compose()
            if (property.resolvePhase == FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE && transformerPhase == FirResolvePhase.BODY_RESOLVE) {
                property.replaceResolvePhase(transformerPhase)
                return@withTypeParametersOf property.compose()
            }
            dataFlowAnalyzer.enterProperty(property)
            withFullBodyResolve {
                withLocalScopeCleanup {
                    context.withContainer(property) {
                        if (property.delegate != null) {
                            addLocalScope(primaryConstructorParametersScope)
                            transformPropertyWithDelegate(property)
                        } else {
                            withLocalScopeCleanup {
                                addLocalScope(primaryConstructorParametersScope)
                                property.transformChildrenWithoutAccessors(returnTypeRef)
                                property.transformInitializer(integerLiteralTypeApproximator, null)
                            }
                            if (property.initializer != null) {
                                storeVariableReturnType(property)
                            }
                            withLocalScopeCleanup {
                                if (property.receiverTypeRef == null) {
                                    addLocalScope(FirLocalScope().storeBackingField(property))
                                }
                                property.transformAccessors()
                            }
                        }
                    }
                    property.replaceResolvePhase(transformerPhase)
                    val controlFlowGraph = dataFlowAnalyzer.exitProperty(property)
                    property.transformControlFlowGraphReference(ControlFlowGraphReferenceTransformer, controlFlowGraph)
                    property.compose()
                }
            }
        }
    }

    private fun FirFunctionCall.replacePropertyReferenceTypeInDelegateAccessors(property: FirProperty) {
        // var someProperty: SomeType
        //     get() = delegate.getValue(thisRef, kProperty: KProperty0/1/2<..., SomeType>)
        //     set() = delegate.getValue(thisRef, kProperty: KProperty0/1/2<..., SomeType>, value)
        val propertyReferenceAccess = argumentMapping?.keys?.toList()?.getOrNull(1) as? FirCallableReferenceAccess ?: return
        val typeRef = propertyReferenceAccess.typeRef
        if (typeRef is FirResolvedTypeRef && property.returnTypeRef is FirResolvedTypeRef) {
            val typeArguments = (typeRef.type as ConeClassLikeType).typeArguments
            val extensionType = property.receiverTypeRef?.coneTypeSafe<ConeKotlinType>()
            val dispatchType = containingClass?.let { containingClass ->
                containingClass.symbol.constructStarProjectedType(containingClass.typeParameters.size)
            }
            propertyReferenceAccess.replaceTypeRef(
                buildResolvedTypeRef {
                    source = typeRef.source
                    annotations.addAll(typeRef.annotations)
                    type = (typeRef.type as ConeClassLikeType).lookupTag.constructClassType(
                        typeArguments.mapIndexed { index, argument ->
                            when (index) {
                                typeArguments.lastIndex -> property.returnTypeRef.coneTypeUnsafe()
                                0 -> extensionType ?: dispatchType
                                else -> dispatchType
                            } ?: argument
                        }.toTypedArray(),
                        isNullable = false
                    )
                }
            )
        }
    }

    private fun replacePropertyReferenceTypeInDelegateAccessors(property: FirProperty) {
        (property.getter?.body?.statements?.singleOrNull() as? FirReturnExpression)?.let { returnExpression ->
            (returnExpression.result as? FirFunctionCall)?.replacePropertyReferenceTypeInDelegateAccessors(property)
        }
        (property.setter?.body?.statements?.singleOrNull() as? FirFunctionCall)?.replacePropertyReferenceTypeInDelegateAccessors(property)
        (property.delegate as? FirFunctionCall)?.replacePropertyReferenceTypeInDelegateAccessors(property)
    }

    private fun transformPropertyWithDelegate(property: FirProperty) {
        property.transformDelegate(transformer, ResolutionMode.ContextDependent)

        val delegateExpression = property.delegate!!

        val inferenceSession = FirDelegatedPropertyInferenceSession(
            property,
            delegateExpression,
            components,
            callCompleter.createPostponedArgumentsAnalyzer()
        )

        components.inferenceComponents.withInferenceSession(inferenceSession) {
            property.transformAccessors()
            val completedCalls = inferenceSession.completeCandidates()
            val finalSubstitutor = inferenceSession.createFinalSubstitutor()
            val callCompletionResultsWriter = components.callCompleter.createCompletionResultsWriter(
                finalSubstitutor,
                mode = FirCallCompletionResultsWriterTransformer.Mode.DelegatedPropertyCompletion
            )
            completedCalls.forEach {
                it.transformSingle(callCompletionResultsWriter, null)
            }
            val declarationCompletionResultsWriter = FirDeclarationCompletionResultsWriter(finalSubstitutor)
            property.transformSingle(declarationCompletionResultsWriter, null)
        }
        if (property.delegateFieldSymbol != null) {
            replacePropertyReferenceTypeInDelegateAccessors(property)
        }
        property.transformOtherChildren(transformer, ResolutionMode.ContextIndependent)
    }

    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: ResolutionMode,
    ): CompositeTransformResult<FirStatement> {
        dataFlowAnalyzer.enterDelegateExpression()
        try {
            val delegateProvider = wrappedDelegateExpression.delegateProvider.transformSingle(transformer, ResolutionMode.ContextDependent)
            when (val calleeReference = (delegateProvider as FirResolvable).calleeReference) {
                is FirResolvedNamedReference -> return delegateProvider.compose()
                is FirNamedReferenceWithCandidate -> {
                    val candidate = calleeReference.candidate
                    if (!candidate.system.hasContradiction) {
                        return delegateProvider.compose()
                    }
                }
            }

            (delegateProvider as? FirFunctionCall)?.let { dataFlowAnalyzer.dropSubgraphFromCall(it) }
            return wrappedDelegateExpression.expression.transform(transformer, ResolutionMode.ContextDependent)
        } finally {
            dataFlowAnalyzer.exitDelegateExpression()
        }
    }

    private fun transformLocalVariable(variable: FirProperty): CompositeTransformResult<FirProperty> {
        assert(variable.isLocal)
        if (variable.delegate != null) {
            transformPropertyWithDelegate(variable)
        } else {
            val resolutionMode = withExpectedType(variable.returnTypeRef)
            variable.transformInitializer(transformer, resolutionMode)
                .transformDelegate(transformer, resolutionMode)
                .transformOtherChildren(transformer, resolutionMode)
                .transformInitializer(integerLiteralTypeApproximator, null)
            if (variable.initializer != null) {
                storeVariableReturnType(variable)
            }
            variable.transformAccessors()
        }
        context.storeVariable(variable)
        variable.replaceResolvePhase(transformerPhase)
        dataFlowAnalyzer.exitLocalVariableDeclaration(variable)
        return variable.compose()
    }

    private fun FirProperty.transformChildrenWithoutAccessors(returnTypeRef: FirTypeRef): FirProperty {
        val data = withExpectedType(returnTypeRef)
        return transformReturnTypeRef(transformer, data)
            .transformInitializer(transformer, data)
            .transformDelegate(transformer, data)
            .transformOtherChildren(transformer, data)
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

    private fun transformAccessor(
        accessor: FirPropertyAccessor,
        enhancedTypeRef: FirTypeRef,
        owner: FirVariable<*>
    ) {
        if (accessor is FirDefaultPropertyAccessor || accessor.body == null) {
            transformFunction(accessor, withExpectedType(enhancedTypeRef))
            return
        }
        val returnTypeRef = accessor.returnTypeRef
        val expectedReturnTypeRef = if (enhancedTypeRef is FirResolvedTypeRef && returnTypeRef !is FirResolvedTypeRef) {
            enhancedTypeRef
        } else {
            returnTypeRef
        }
        val resolutionMode = if (expectedReturnTypeRef.coneTypeSafe<ConeKotlinType>() == session.builtinTypes.unitType.type) {
            ResolutionMode.ContextIndependent
        } else {
            withExpectedType(expectedReturnTypeRef)
        }

        val receiverTypeRef = owner.receiverTypeRef
        if (receiverTypeRef != null) {
            withLabelAndReceiverType(owner.name, owner, receiverTypeRef.coneTypeUnsafe()) {
                transformFunctionWithGivenSignature(accessor, resolutionMode)
            }
        } else {
            transformFunctionWithGivenSignature(accessor, resolutionMode)
        }
    }

    private fun FirDeclaration.resolveStatus(status: FirDeclarationStatus, containingClass: FirClass<*>? = null): FirDeclarationStatus {
        val containingDeclaration = context.containerIfAny
        return resolveStatus(
            status, containingClass as? FirRegularClass, isLocal = containingDeclaration != null && containingClass == null
        )
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        context.storeClass(regularClass)

        if (regularClass.isLocal && regularClass !in context.targetedLocalClasses) {
            return regularClass.runAllPhasesForLocalClass(transformer, components, data).compose()
        }

        val notAnalyzed = regularClass.resolvePhase < transformerPhase

        return withTypeParametersOf(regularClass) {
            if (notAnalyzed) {
                dataFlowAnalyzer.enterClass()
            }
            val oldConstructorScope = primaryConstructorParametersScope
            val oldContainingClass = containingClass
            primaryConstructorParametersScope = null
            containingClass = regularClass
            val type = regularClass.defaultType()
            val result = withLabelAndReceiverType(regularClass.name, regularClass, type) {
                val constructor = regularClass.declarations.firstOrNull() as? FirConstructor
                if (constructor?.isPrimary == true) {
                    primaryConstructorParametersScope = FirLocalScope().let {
                        var scope = it
                        constructor.valueParameters.forEach { scope = scope.storeVariable(it) }
                        scope
                    }
                }
                transformDeclarationContent(regularClass, data).single as FirRegularClass
            }
            if (notAnalyzed) {
                if (!implicitTypeOnly) {
                    val controlFlowGraph = dataFlowAnalyzer.exitRegularClass(result)
                    result.transformControlFlowGraphReference(ControlFlowGraphReferenceTransformer, controlFlowGraph)
                } else {
                    dataFlowAnalyzer.exitClass()
                }
            }
            containingClass = oldContainingClass
            primaryConstructorParametersScope = oldConstructorScope
            @Suppress("UNCHECKED_CAST")
            result.compose()
        }
    }

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        if (anonymousObject !in context.targetedLocalClasses) {
            return anonymousObject.runAllPhasesForLocalClass(transformer, components, data).compose()
        }
        dataFlowAnalyzer.enterClass()
        val type = anonymousObject.defaultType()
        if (anonymousObject.typeRef !is FirResolvedTypeRef) {
            anonymousObject.resultType = buildResolvedTypeRef {
                source = anonymousObject.source
                this.type = type
            }
        }
        var result = withLabelAndReceiverType(null, anonymousObject, type) {
            transformDeclarationContent(anonymousObject, data).single as FirAnonymousObject
        }
        if (!implicitTypeOnly && result.controlFlowGraphReference == FirEmptyControlFlowGraphReference) {
            val graph = dataFlowAnalyzer.exitAnonymousObject(result)
            result = result.transformControlFlowGraphReference(ControlFlowGraphReferenceTransformer, graph)
        } else {
            dataFlowAnalyzer.exitClass()
        }
        return result.compose()
    }

    private fun transformAnonymousFunctionWithLambdaResolution(
        anonymousFunction: FirAnonymousFunction, lambdaResolution: ResolutionMode.LambdaResolution
    ): FirAnonymousFunction {
        val receiverTypeRef = anonymousFunction.receiverTypeRef
        fun transform(): FirAnonymousFunction {
            val expectedReturnType =
                lambdaResolution.expectedReturnTypeRef ?: anonymousFunction.returnTypeRef.takeUnless { it is FirImplicitTypeRef }
            val result = transformFunction(anonymousFunction, withExpectedType(expectedReturnType)).single as FirAnonymousFunction
            val body = result.body
            return if (result.returnTypeRef is FirImplicitTypeRef && body != null) {
                result.transformReturnTypeRef(transformer, withExpectedType(body.resultType))
                result
            } else {
                result
            }
        }

        val label = anonymousFunction.label
        return if (label != null || receiverTypeRef is FirResolvedTypeRef) {
            withLabelAndReceiverType(label?.name?.let { Name.identifier(it) }, anonymousFunction, receiverTypeRef?.coneTypeSafe()) {
                transform()
            }
        } else {
            transform()
        }
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): CompositeTransformResult<FirSimpleFunction> {
        if (simpleFunction.resolvePhase == transformerPhase) return simpleFunction.compose()
        if (simpleFunction.resolvePhase == FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE && transformerPhase == FirResolvePhase.BODY_RESOLVE) {
            simpleFunction.replaceResolvePhase(transformerPhase)
            return simpleFunction.compose()
        }
        val returnTypeRef = simpleFunction.returnTypeRef
        if ((returnTypeRef !is FirImplicitTypeRef) && implicitTypeOnly) {
            return simpleFunction.compose()
        }

        return withTypeParametersOf(simpleFunction) {
            val containingDeclaration = context.containerIfAny
            if (containingDeclaration != null && containingDeclaration !is FirClass<*>) {
                // For class members everything should be already prepared
                prepareSignatureForBodyResolve(simpleFunction)
                simpleFunction.transformStatus(this, simpleFunction.resolveStatus(simpleFunction.status).mode())
            }

            withFullBodyResolve {
                val receiverTypeRef = simpleFunction.receiverTypeRef
                if (receiverTypeRef != null) {
                    withLabelAndReceiverType(simpleFunction.name, simpleFunction, receiverTypeRef.coneTypeUnsafe()) {
                        transformFunctionWithGivenSignature(simpleFunction, ResolutionMode.ContextIndependent)
                    }
                } else {
                    transformFunctionWithGivenSignature(simpleFunction, ResolutionMode.ContextIndependent)
                }
            }
        }
    }

    private fun <F : FirFunction<F>> transformFunctionWithGivenSignature(
        function: F,
        resolutionMode: ResolutionMode,
    ): CompositeTransformResult<F> {
        if (function is FirSimpleFunction && context.containerIfAny !is FirClass<*>) {
            context.storeFunction(function)
        }

        @Suppress("UNCHECKED_CAST")
        val result = transformFunction(function, resolutionMode).single as F

        val body = result.body
        if (result.returnTypeRef is FirImplicitTypeRef) {
            if (body != null) {
                result.transformReturnTypeRef(transformer, withExpectedType(body.resultType))
            } else {
                result.transformReturnTypeRef(
                    transformer,
                    withExpectedType(buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("empty body", DiagnosticKind.Other) })
                )
            }
        }

        return result.compose()
    }

    override fun <F : FirFunction<F>> transformFunction(
        function: FirFunction<F>,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return withLocalScopeCleanup {
            addLocalScope(FirLocalScope())
            val functionIsNotAnalyzed = transformerPhase != function.resolvePhase
            if (functionIsNotAnalyzed) {
                dataFlowAnalyzer.enterFunction(function)
            }
            @Suppress("UNCHECKED_CAST")
            transformDeclarationContent(function, data).also {
                if (functionIsNotAnalyzed) {
                    val result = it.single as FirFunction<*>
                    val controlFlowGraph = dataFlowAnalyzer.exitFunction(result)
                    result.transformControlFlowGraphReference(ControlFlowGraphReferenceTransformer, controlFlowGraph)
                }
            } as CompositeTransformResult<FirStatement>
        }
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        if (implicitTypeOnly) return constructor.compose()
        @Suppress("UNCHECKED_CAST")
        return transformFunction(constructor, data) as CompositeTransformResult<FirDeclaration>
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode
    ): CompositeTransformResult<FirDeclaration> {
        if (implicitTypeOnly) return anonymousInitializer.compose()
        return withLocalScopeCleanup {
            dataFlowAnalyzer.enterInitBlock(anonymousInitializer)
            addLocalScope(primaryConstructorParametersScope)
            addLocalScope(FirLocalScope())
            val result = transformDeclarationContent(anonymousInitializer, ResolutionMode.ContextIndependent).single as FirAnonymousInitializer
            val graph = dataFlowAnalyzer.exitInitBlock(result)
            result.transformControlFlowGraphReference(ControlFlowGraphReferenceTransformer, graph).compose()
        }
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        context.storeVariable(valueParameter)
        if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
            valueParameter.replaceResolvePhase(transformerPhase)
            return valueParameter.compose() // TODO
        }
        val transformedValueParameter =
            valueParameter.transformInitializer(integerLiteralTypeApproximator, valueParameter.returnTypeRef.coneTypeSafe())
        return (transformDeclarationContent(
            transformedValueParameter, withExpectedType(transformedValueParameter.returnTypeRef)
        ).single as FirStatement).compose()
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        // Either ContextDependent, ContextIndependent or WithExpectedType could be here
        if (data !is ResolutionMode.LambdaResolution) {
            anonymousFunction.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
            anonymousFunction.transformReceiverTypeRef(transformer, ResolutionMode.ContextIndependent)
            anonymousFunction.valueParameters.forEach { it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent) }
            context.saveContextForAnonymousFunction(anonymousFunction)
        }
        return when (data) {
            ResolutionMode.ContextDependent -> {
                dataFlowAnalyzer.visitPostponedAnonymousFunction(anonymousFunction)
                anonymousFunction.addReturn().compose()
            }
            is ResolutionMode.LambdaResolution -> {
                transformAnonymousFunctionWithLambdaResolution(anonymousFunction, data).addReturn().compose()
            }
            is ResolutionMode.WithExpectedType, is ResolutionMode.ContextIndependent -> {
                val expectedTypeRef = (data as? ResolutionMode.WithExpectedType)?.expectedTypeRef ?: buildImplicitTypeRef()
                val resolvedLambdaAtom = (expectedTypeRef as? FirResolvedTypeRef)?.let {
                    extractLambdaInfoFromFunctionalType(
                        it.type, it, anonymousFunction, returnTypeVariable = null, components, candidate = null
                    )
                }
                var af = anonymousFunction
                val valueParameters =
                    if (resolvedLambdaAtom == null) af.valueParameters
                    else {
                        val singleParameterType = resolvedLambdaAtom.parameters.singleOrNull()
                        val itParam = when {
                            af.valueParameters.isEmpty() && singleParameterType != null -> {
                                val name = Name.identifier("it")
                                buildValueParameter {
                                    session = this@FirDeclarationsResolveTransformer.session
                                    returnTypeRef = buildResolvedTypeRef { type = singleParameterType }
                                    this.name = name
                                    symbol = FirVariableSymbol(name)
                                    isCrossinline = false
                                    isNoinline = false
                                    isVararg = false
                                }
                            }
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
                af = af.transformValueParameters(ImplicitToErrorTypeTransformer, null)
                val bodyExpectedType = returnTypeRefFromResolvedAtom ?: expectedTypeRef
                val labelName = af.label?.name?.let { Name.identifier(it) }
                withLabelAndReceiverType(labelName, af, af.receiverTypeRef?.coneTypeSafe()) {
                    af = transformFunction(af, withExpectedType(bodyExpectedType)).single as FirAnonymousFunction
                }
                // To separate function and separate commit
                val writer = FirCallCompletionResultsWriterTransformer(
                    session,
                    ConeSubstitutor.Empty,
                    components.returnTypeCalculator,
                    inferenceComponents.approximator,
                    integerOperatorsTypeUpdater,
                    integerLiteralTypeApproximator
                )
                af.transformSingle(writer, expectedTypeRef.coneTypeSafe<ConeKotlinType>()?.toExpectedType())
                val returnTypes = dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(af)
                    .mapNotNull { (it as? FirExpression)?.resultType?.coneTypeUnsafe() }
                af.replaceReturnTypeRef(
                    af.returnTypeRef.resolvedTypeFromPrototype(
                        inferenceComponents.ctx.commonSuperTypeOrNull(returnTypes) ?: session.builtinTypes.unitType.coneTypeUnsafe()
                    )
                )
                af.replaceTypeRef(af.constructFunctionalTypeRef(session))
                af.addReturn().compose()
            }
            is ResolutionMode.WithStatus -> {
                throw AssertionError("Should not be here in WithStatus mode")
            }
        }
    }

    private fun FirAnonymousFunction.addReturn(): FirAnonymousFunction {
        val lastStatement = body?.statements?.lastOrNull()
        val returnType = (body?.typeRef as? FirResolvedTypeRef) ?: return this
        val returnNothing = returnType.isNothing || returnType.isUnit
        if (lastStatement is FirExpression && !returnNothing) {
            body?.transformChildren(
                object : FirDefaultTransformer<FirExpression>() {
                    override fun <E : FirElement> transformElement(element: E, data: FirExpression): CompositeTransformResult<E> {
                        if (element == lastStatement) {
                            val returnExpression = buildReturnExpression {
                                source = element.source
                                result = lastStatement
                                target = FirFunctionTarget(null, isLambda = this@addReturn.isLambda).also {
                                    it.bind(this@addReturn)
                                }
                            }
                            return (returnExpression as E).compose()
                        }
                        return element.compose()
                    }

                    override fun transformReturnExpression(
                        returnExpression: FirReturnExpression,
                        data: FirExpression
                    ): CompositeTransformResult<FirStatement> {
                        return returnExpression.compose()
                    }
                },
                buildUnitExpression()
            )
        }
        return this
    }

    protected inline fun <T> withLabelAndReceiverType(
        labelName: Name?,
        owner: FirDeclaration,
        type: ConeKotlinType?,
        block: () -> T
    ): T {
        val (implicitReceiverValue, implicitCompanionValues) = components.collectImplicitReceivers(type, owner)
        implicitCompanionValues.forEach { value ->
            context.implicitReceiverStack.add(null, value)
        }
        implicitReceiverValue?.let { context.implicitReceiverStack.add(labelName, it) }

        try {
            return block()
        } finally {
            if (type != null) {
                context.implicitReceiverStack.pop(labelName)
                for (i in implicitCompanionValues.indices)
                    context.implicitReceiverStack.pop(null)
            }
        }
    }

    private fun storeVariableReturnType(variable: FirVariable<*>) {
        val initializer = variable.initializer
        if (variable.returnTypeRef is FirImplicitTypeRef) {
            when {
                initializer != null -> {
                    val expectedType = when (val resultType = initializer.resultType) {
                        is FirImplicitTypeRef -> buildErrorTypeRef {
                            diagnostic = ConeSimpleDiagnostic("No result type for initializer", DiagnosticKind.InferenceError)
                        }
                        else -> resultType
                    }
                    variable.transformReturnTypeRef(
                        transformer,
                        withExpectedType(expectedType)
                    )
                }
                variable.getter != null && variable.getter !is FirDefaultPropertyAccessor -> {
                    val expectedType = when (val resultType = variable.getter?.returnTypeRef) {
                        is FirImplicitTypeRef -> buildErrorTypeRef {
                            diagnostic = ConeSimpleDiagnostic("No result type for getter", DiagnosticKind.InferenceError)
                        }
                        else -> resultType
                    }
                    variable.transformReturnTypeRef(
                        transformer,
                        withExpectedType(expectedType)
                    )
                }
                else -> {
                    variable.transformReturnTypeRef(
                        transformer,
                        withExpectedType(
                            buildErrorTypeRef {
                                diagnostic = ConeSimpleDiagnostic(
                                    "Cannot infer variable type without initializer / getter / delegate",
                                    DiagnosticKind.InferenceError,
                                )
                            },
                        )
                    )
                }
            }
            if (variable.getter?.returnTypeRef is FirImplicitTypeRef) {
                variable.getter?.transformReturnTypeRef(transformer, withExpectedType(variable.returnTypeRef))
            }
        }
    }

    private object ImplicitToErrorTypeTransformer : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformValueParameter(valueParameter: FirValueParameter, data: Nothing?): CompositeTransformResult<FirStatement> {
            if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
                valueParameter.transformReturnTypeRef(
                    StoreType,
                    valueParameter.returnTypeRef.resolvedTypeFromPrototype(ConeKotlinErrorType("No type for parameter"))
                )
            }
            return valueParameter.compose()
        }
    }
}
