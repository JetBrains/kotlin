/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildUnitExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitExtensionReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.InaccessibleImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.inference.FirDelegatedPropertyInferenceSession
import org.jetbrains.kotlin.fir.resolve.inference.extractLambdaInfoFromFunctionalType
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
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
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

open class FirDeclarationsResolveTransformer(transformer: FirBodyResolveTransformer) : FirPartialBodyResolveTransformer(transformer) {
    private var containingClass: FirRegularClass? = null

    private inline fun <T> withFirArrayOfCallTransformer(block: () -> T): T {
        transformer.expressionsTransformer.enableArrayOfCallTransformation = true
        return try {
            block()
        } finally {
            transformer.expressionsTransformer.enableArrayOfCallTransformation = false
        }
    }

    private fun transformDeclarationContent(
        declaration: FirDeclaration, data: ResolutionMode
    ): CompositeTransformResult<FirDeclaration> {
        transformer.onBeforeDeclarationContentResolve(declaration)
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

    protected fun createTypeParameterScope(declaration: FirMemberDeclaration): FirMemberTypeParameterScope? {
        if (declaration.typeParameters.isEmpty()) return null

        for (typeParameter in declaration.typeParameters) {
            (typeParameter as? FirTypeParameter)?.replaceResolvePhase(FirResolvePhase.STATUS)
            typeParameter.transformChildren(transformer, ResolutionMode.ContextIndependent)
        }

        return FirMemberTypeParameterScope(declaration)
    }

    protected inline fun <T> withTypeParametersOf(declaration: FirMemberDeclaration, crossinline l: () -> T): T {
        val scope = createTypeParameterScope(declaration)
        return context.withTowerDataCleanup {
            scope?.let { context.addNonLocalTowerDataElement(it.asTowerDataElement(isLocal = false)) }
            l()
        }
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): CompositeTransformResult<FirProperty> {
        if (property is FirSyntheticProperty) {
            val delegate = property.getter.delegate
            if (delegate.origin == FirDeclarationOrigin.IntersectionOverride) {
                val unwrapped = delegate.symbol.overriddenSymbol!!.fir
                transformSimpleFunction(unwrapped, data)
                delegate.replaceReturnTypeRef(unwrapped.returnTypeRef)
            } else {
                transformSimpleFunction(delegate, data)
            }
            return property.compose()
        }

        if (property.isLocal) {
            prepareSignatureForBodyResolve(property)
            property.transformStatus(this, property.resolveStatus(property.status).mode())
            property.getter?.let { it.transformStatus(this, it.resolveStatus(it.status).mode()) }
            property.setter?.let { it.transformStatus(this, it.resolveStatus(it.status).mode()) }
            return transformLocalVariable(property)
        }

        return withTypeParametersOf(property) {
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
                            addLocalScope(context.getPrimaryConstructorParametersScope())
                            transformPropertyWithDelegate(property)
                        } else {
                            withLocalScopeCleanup {
                                addLocalScope(context.getPrimaryConstructorParametersScope())
                                property.transformChildrenWithoutAccessors(returnTypeRef)
                            }
                            if (property.initializer != null) {
                                storeVariableReturnType(property)
                            }
                            withLocalScopeCleanup {
                                if (property.receiverTypeRef == null && property.returnTypeRef !is FirImplicitTypeRef) {
                                    addLocalScope(FirLocalScope().storeBackingField(property))
                                }
                                property.transformAccessors()
                            }
                        }
                    }
                    property.replaceResolvePhase(transformerPhase)
                    dataFlowAnalyzer.exitProperty(property)?.let {
                        property.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(it))
                    }
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
            val extensionType = property.receiverTypeRef?.coneType
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
                                typeArguments.lastIndex -> property.returnTypeRef.coneType
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
            resolutionContext,
            callCompleter.createPostponedArgumentsAnalyzer(resolutionContext)
        )

        context.withInferenceSession(inferenceSession) {
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
        property.transformTypeParameters(transformer, ResolutionMode.ContextIndependent)
            .transformOtherChildren(transformer, ResolutionMode.ContextIndependent)
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
            return wrappedDelegateExpression.expression
                .transformSingle(transformer, ResolutionMode.ContextDependent)
                .approximateIfIsIntegerConst()
                .compose()
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
                .transformTypeParameters(transformer, resolutionMode)
                .transformOtherChildren(transformer, resolutionMode)
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
            .transformTypeParameters(transformer, data)
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
            if (it.valueParameters[0].returnTypeRef is FirImplicitTypeRef) {
                it.valueParameters[0].transformReturnTypeRef(StoreType, enhancedTypeRef)
            }
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
            withLabelAndReceiverType(owner.name, owner, receiverTypeRef.coneType) {
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
        context.storeClassIfNotNested(regularClass)

        if (regularClass.isLocal && regularClass !in context.targetedLocalClasses) {
            return regularClass.runAllPhasesForLocalClass(transformer, components, data).compose()
        }

        return context.withTowerDataCleanup {
            if (!regularClass.isInner && context.containerIfAny is FirRegularClass) {
                context.replaceTowerDataContext(
                    context.getTowerDataContextForStaticNestedClassesUnsafe()
                )
            }

            doTransformRegularClass(regularClass, data)
        }
    }

    private fun doTransformRegularClass(
        regularClass: FirRegularClass,
        data: ResolutionMode
    ): CompositeTransformResult.Single<FirRegularClass> {
        val notAnalyzed = regularClass.resolvePhase < transformerPhase

        if (notAnalyzed) {
            dataFlowAnalyzer.enterClass()
        }

        val oldContainingClass = containingClass
        containingClass = regularClass
        val type = regularClass.defaultType()
        val result = withScopesForClass(regularClass.name, regularClass, type) {
            transformDeclarationContent(regularClass, data).single as FirRegularClass
        }

        if (notAnalyzed) {
            if (!implicitTypeOnly) {
                val controlFlowGraph = dataFlowAnalyzer.exitRegularClass(result)
                result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(controlFlowGraph))
            } else {
                dataFlowAnalyzer.exitClass()
            }
        }

        containingClass = oldContainingClass
        return (@Suppress("UNCHECKED_CAST")
        result.compose())
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
        val labelName =
            if (anonymousObject.classKind == ClassKind.ENUM_ENTRY) {
                anonymousObject.getPrimaryConstructorIfAny()?.symbol?.callableId?.className?.shortName()
            } else null
        val result = withScopesForClass(labelName, anonymousObject, type) {
            transformDeclarationContent(anonymousObject, data).single as FirAnonymousObject
        }
        if (!implicitTypeOnly && result.controlFlowGraphReference == null) {
            val graph = dataFlowAnalyzer.exitAnonymousObject(result)
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(graph))
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
            if (result.returnTypeRef is FirImplicitTypeRef && body != null) {
                result.transformReturnTypeRef(transformer, withExpectedType(body.resultType))
            }
            return result
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

        if (context.containerIfAny !is FirClass<*>) {
            context.storeFunction(simpleFunction)
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
                    withLabelAndReceiverType(simpleFunction.name, simpleFunction, receiverTypeRef.coneType) {
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
        @Suppress("UNCHECKED_CAST")
        val result = transformFunction(function, resolutionMode).single as F

        val body = result.body
        if (result.returnTypeRef is FirImplicitTypeRef) {
            val simpleFunction = function as? FirSimpleFunction
            if (body != null) {
                result.transformReturnTypeRef(
                    transformer,
                    withExpectedType(
                        body.resultType.approximatedIfNeededOrSelf(
                            inferenceComponents.approximator, simpleFunction?.visibility, simpleFunction?.isInline == true
                        )
                    )
                )
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
        return withNewLocalScope {
            val functionIsNotAnalyzed = transformerPhase != function.resolvePhase
            if (functionIsNotAnalyzed) {
                dataFlowAnalyzer.enterFunction(function)
            }
            @Suppress("UNCHECKED_CAST")
            transformDeclarationContent(function, data).also {
                if (functionIsNotAnalyzed) {
                    val result = it.single as FirFunction<*>
                    val controlFlowGraphReference = dataFlowAnalyzer.exitFunction(result)
                    result.replaceControlFlowGraphReference(controlFlowGraphReference)
                }
            } as CompositeTransformResult<FirStatement>
        }
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        if (implicitTypeOnly) return constructor.compose()
        if (constructor.isPrimary && containingClass?.classKind == ClassKind.ANNOTATION_CLASS) {
            return withFirArrayOfCallTransformer {
                @Suppress("UNCHECKED_CAST")
                doTransformConstructor(constructor, data)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return doTransformConstructor(constructor, data)
    }

    private fun doTransformConstructor(constructor: FirConstructor, data: ResolutionMode): CompositeTransformResult<FirConstructor> {
        return context.withContainer(constructor) {
            constructor.replaceResolvePhase(transformerPhase)
            dataFlowAnalyzer.enterFunction(constructor)

            constructor.transformTypeParameters(transformer, data)
                .transformAnnotations(transformer, data)
                .transformReceiverTypeRef(transformer, data)
                .transformReturnTypeRef(transformer, data)

            val containers = context.containers
            val owningClass = containers[containers.lastIndex - 1].safeAs<FirRegularClass>()

            /*
             * Default values of constructor can't access members of constructing class
             */
            context.withTowerDataContext(context.getTowerDataContextForConstructorResolution()) {
                if (owningClass != null && !constructor.isPrimary) {
                    context.addReceiver(
                        null,
                        InaccessibleImplicitReceiverValue(
                            owningClass.symbol,
                            owningClass.defaultType(),
                            session,
                            scopeSession
                        )
                    )
                }
                withNewLocalScope {
                    constructor.transformValueParameters(transformer, data)
                }
            }

            val scopeWithValueParameters = if (constructor.isPrimary) {
                context.getPrimaryConstructorParametersScope()
            } else {
                constructor.scopeWithParameters()
            }

            /*
             * Delegated constructor call is called before constructor body, so we need to
             *   analyze it before body, so body can access smartcasts from that call
             */
            context.withTowerDataCleanup {
                addLocalScope(scopeWithValueParameters)
                constructor.transformDelegatedConstructor(transformer, data)
            }

            if (constructor.body != null) {
                if (constructor.isPrimary) {
                    /*
                     * Primary constructor may have body only if class delegates implementation to some property
                     *   In it's body we don't have this receiver for building class, so we need to use
                     *   special towerDataContext
                     */
                    context.withTowerDataContext(context.getTowerDataContextForConstructorResolution()) {
                        addLocalScope(scopeWithValueParameters)
                        constructor.transformBody(transformer, data)
                    }
                } else {
                    withLocalScopeCleanup {
                        addLocalScope(scopeWithValueParameters)
                        constructor.transformBody(transformer, data)
                    }
                }
            }

            val controlFlowGraphReference = dataFlowAnalyzer.exitFunction(constructor)
            constructor.replaceControlFlowGraphReference(controlFlowGraphReference)
            constructor.compose()
        }
    }


    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode
    ): CompositeTransformResult<FirDeclaration> {
        if (implicitTypeOnly) return anonymousInitializer.compose()
        return withLocalScopeCleanup {
            dataFlowAnalyzer.enterInitBlock(anonymousInitializer)
            addLocalScope(context.getPrimaryConstructorParametersScope())
            addNewLocalScope()
            val result =
                transformDeclarationContent(anonymousInitializer, ResolutionMode.ContextIndependent).single as FirAnonymousInitializer
            val graph = dataFlowAnalyzer.exitInitBlock(result)
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(graph))
            result.compose()
        }
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        context.storeVariable(valueParameter)
        if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
            valueParameter.replaceResolvePhase(transformerPhase)
            return valueParameter.compose()
        }

        dataFlowAnalyzer.enterValueParameter(valueParameter)
        val result = transformDeclarationContent(
            valueParameter,
            withExpectedType(valueParameter.returnTypeRef)
        ).single as FirValueParameter

        dataFlowAnalyzer.exitValueParameter(result)?.let { graph ->
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(graph))
        }

        return result.compose()
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
                var lambda = anonymousFunction
                val valueParameters = when (resolvedLambdaAtom) {
                    null -> lambda.valueParameters
                    else -> {
                        val singleParameterType = resolvedLambdaAtom.parameters.singleOrNull()
                        when {
                            lambda.valueParameters.isEmpty() && singleParameterType != null -> {
                                val name = Name.identifier("it")
                                val itParam = buildValueParameter {
                                    session = this@FirDeclarationsResolveTransformer.session
                                    origin = FirDeclarationOrigin.Source
                                    returnTypeRef = buildResolvedTypeRef { type = singleParameterType }
                                    this.name = name
                                    symbol = FirVariableSymbol(name)
                                    isCrossinline = false
                                    isNoinline = false
                                    isVararg = false
                                }
                                listOf(itParam)
                            }

                            else -> {
                                lambda.valueParameters.mapIndexed { index, param ->
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
                    }
                }
                val returnTypeRefFromResolvedAtom =
                    resolvedLambdaAtom?.returnType?.let { lambda.returnTypeRef.resolvedTypeFromPrototype(it) }
                lambda = lambda.copy(
                    receiverTypeRef = lambda.receiverTypeRef?.takeIf { it !is FirImplicitTypeRef }
                        ?: resolvedLambdaAtom?.receiver?.let { lambda.receiverTypeRef?.resolvedTypeFromPrototype(it) },
                    valueParameters = valueParameters,
                    returnTypeRef = (lambda.returnTypeRef as? FirResolvedTypeRef)
                        ?: returnTypeRefFromResolvedAtom
                        ?: lambda.returnTypeRef
                )
                lambda = lambda.transformValueParameters(ImplicitToErrorTypeTransformer, null)
                val bodyExpectedType = returnTypeRefFromResolvedAtom ?: expectedTypeRef
                val labelName = lambda.label?.name?.let { Name.identifier(it) }
                withLabelAndReceiverType(labelName, lambda, lambda.receiverTypeRef?.coneType) {
                    lambda = transformFunction(lambda, withExpectedType(bodyExpectedType)).single as FirAnonymousFunction
                }
                // To separate function and separate commit
                val writer = FirCallCompletionResultsWriterTransformer(
                    session,
                    ConeSubstitutor.Empty,
                    components.returnTypeCalculator,
                    inferenceComponents.approximator,
                )
                lambda.transformSingle(writer, expectedTypeRef.coneTypeSafe<ConeKotlinType>()?.toExpectedType())
                val returnTypes = dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(lambda)
                    .mapNotNull { (it as? FirExpression)?.resultType?.coneType }
                lambda.replaceReturnTypeRef(
                    lambda.returnTypeRef.resolvedTypeFromPrototype(
                        inferenceComponents.ctx.commonSuperTypeOrNull(returnTypes) ?: session.builtinTypes.unitType.type
                    )
                )
                lambda.replaceTypeRef(
                    lambda.constructFunctionalTypeRef(
                        isSuspend = expectedTypeRef.coneTypeSafe<ConeKotlinType>()?.isSuspendFunctionType(session) == true
                    )
                )
                lambda.addReturn().compose()
            }
            is ResolutionMode.WithStatus -> {
                throw AssertionError("Should not be here in WithStatus mode")
            }
        }
    }

    private fun FirAnonymousFunction.addReturn(): FirAnonymousFunction {
        // If this lambda's resolved, expected return type is Unit, we don't need an explicit return statement.
        // During conversion (to backend IR), the last expression will be coerced to Unit if needed.
        // As per KT-41005, we should not force coercion to Unit for nullable return type, though.
        if (returnTypeRef.isUnit && body?.typeRef?.isMarkedNullable == false) {
            return this
        }
        val lastStatement = body?.statements?.lastOrNull()
        val returnType = (body?.typeRef as? FirResolvedTypeRef) ?: return this
        val returnNothing = returnType.isNothing || returnType.isUnit
        if (lastStatement is FirExpression && !returnNothing) {
            body?.transformChildren(
                object : FirDefaultTransformer<FirExpression>() {
                    override fun <E : FirElement> transformElement(element: E, data: FirExpression): CompositeTransformResult<E> {
                        if (element == lastStatement) {
                            val returnExpression = buildReturnExpression {
                                source = element.source?.fakeElement(FirFakeSourceElementKind.ImplicitReturn)
                                result = lastStatement
                                target = FirFunctionTarget(null, isLambda = this@addReturn.isLambda).also {
                                    it.bind(this@addReturn)
                                }
                            }
                            @Suppress("UNCHECKED_CAST")
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

    private inline fun <T> withScopesForClass(
        labelName: Name?,
        owner: FirClass<*>,
        type: ConeKotlinType,
        block: () -> T
    ): T = context.withTowerDataCleanup {
        val towerElementsForClass = components.collectTowerDataElementsForClass(owner, type)

        val staticsAndCompanion =
            context.towerDataContext
                .addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticsAndCompanionReceivers)
                .run {
                    if (towerElementsForClass.companionReceiver != null)
                        addReceiver(null, towerElementsForClass.companionReceiver)
                    else
                        this
                }
                .addNonLocalScopeIfNotNull(towerElementsForClass.companionStaticScope)
                .addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)

        val typeParameterScope = (owner as? FirRegularClass)?.let(this::createTypeParameterScope)

        val forMembersResolution =
            staticsAndCompanion
                .addReceiver(labelName, towerElementsForClass.thisReceiver)
                .addNonLocalScopeIfNotNull(typeParameterScope)

        val scopeForConstructorHeader =
            staticsAndCompanion.addNonLocalScopeIfNotNull(typeParameterScope)

        val newTowerDataContextForStaticNestedClasses =
            if ((owner as? FirRegularClass)?.classKind?.isSingleton == true)
                forMembersResolution
            else
                staticsAndCompanion

        val constructor = (owner as? FirRegularClass)?.declarations?.firstOrNull { it is FirConstructor } as? FirConstructor
        val primaryConstructorParametersScope =
            if (constructor?.isPrimary == true) {
                constructor.scopeWithParameters()
            } else null

        components.context.replaceTowerDataContext(forMembersResolution)

        val newContexts = FirTowerDataContextsForClassParts(
            newTowerDataContextForStaticNestedClasses,
            scopeForConstructorHeader,
            primaryConstructorParametersScope
        )

        context.withNewTowerDataForClassParts(newContexts) {
            block()
        }
    }

    private fun FirConstructor.scopeWithParameters(): FirLocalScope {
        return valueParameters.fold(FirLocalScope()) { acc, param -> acc.storeVariable(param) }
    }

    protected inline fun <T> withLabelAndReceiverType(
        labelName: Name?,
        owner: FirCallableDeclaration<*>,
        type: ConeKotlinType?,
        block: () -> T
    ): T = context.withTowerDataCleanup {
        if (type != null) {
            val receiver = ImplicitExtensionReceiverValue(
                owner.symbol,
                type,
                components.session,
                components.scopeSession
            )
            context.addReceiver(labelName, receiver)
        }

        block()
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
                        else -> {
                            buildResolvedTypeRef {
                                type = resultType.coneType
                                annotations.addAll(resultType.annotations)
                                resultType.source?.fakeElement(FirFakeSourceElementKind.PropertyFromParameter)?.let {
                                    source = it
                                }
                            }
                        }
                    }
                    variable.transformReturnTypeRef(
                        transformer,
                        withExpectedType(
                            expectedType.approximatedIfNeededOrSelf(inferenceComponents.approximator, (variable as? FirProperty)?.visibility)
                        )
                    )
                }
                variable.getter != null && variable.getter !is FirDefaultPropertyAccessor -> {
                    val expectedType = when (val resultType = variable.getter?.returnTypeRef) {
                        is FirImplicitTypeRef -> buildErrorTypeRef {
                            diagnostic = ConeSimpleDiagnostic("No result type for getter", DiagnosticKind.InferenceError)
                        }
                        else -> {
                            resultType?.let {
                                buildResolvedTypeRef {
                                    type = resultType.coneType
                                    annotations.addAll(resultType.annotations)
                                    resultType.source?.fakeElement(FirFakeSourceElementKind.PropertyFromParameter)?.let {
                                        source = it
                                    }
                                }
                            }
                        }
                    }
                    variable.transformReturnTypeRef(
                        transformer,
                        withExpectedType(
                            expectedType?.approximatedIfNeededOrSelf(inferenceComponents.approximator, (variable as? FirProperty)?.visibility)
                        )
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
                    valueParameter.returnTypeRef.resolvedTypeFromPrototype(
                        ConeKotlinErrorType(ConeSimpleDiagnostic("No type for parameter", DiagnosticKind.NoTypeForTypeParameter))
                    )
                )
            }
            return valueParameter.compose()
        }
    }
}
