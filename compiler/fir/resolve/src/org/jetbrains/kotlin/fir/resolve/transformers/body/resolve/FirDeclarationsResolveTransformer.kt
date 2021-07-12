/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildUnitExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.constructFunctionalTypeRef
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.resolve.inference.extractLambdaInfoFromFunctionalType
import org.jetbrains.kotlin.fir.resolve.inference.isSuspendFunctionType
import org.jetbrains.kotlin.fir.resolve.mode
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.resolve.withExpectedType
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.symbols.constructStarProjectedType
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitUnitTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name

open class FirDeclarationsResolveTransformer(transformer: FirBodyResolveTransformer) : FirPartialBodyResolveTransformer(transformer) {
    private val statusResolver: FirStatusResolver = FirStatusResolver(session, scopeSession)

    private fun FirDeclaration.visibilityForApproximation(): Visibility {
        if (this !is FirMemberDeclaration) return Visibilities.Local
        val container = context.containers.getOrNull(context.containers.size - 2)
        val containerVisibility =
            if (container == null) Visibilities.Public
            else (container as? FirRegularClass)?.visibility ?: Visibilities.Local
        if (containerVisibility == Visibilities.Local || visibility == Visibilities.Local) return Visibilities.Local
        if (containerVisibility == Visibilities.Private) return Visibilities.Private
        return visibility
    }

    private inline fun <T> withFirArrayOfCallTransformer(block: () -> T): T {
        transformer.expressionsTransformer.enableArrayOfCallTransformation = true
        return try {
            block()
        } finally {
            transformer.expressionsTransformer.enableArrayOfCallTransformation = false
        }
    }

    private fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration {
        transformer.firTowerDataContextCollector?.addDeclarationContext(declaration, context.towerDataContext)
        transformer.replaceDeclarationResolvePhaseIfNeeded(declaration, transformerPhase)
        return transformer.transformDeclarationContent(declaration, data)
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: ResolutionMode
    ): FirDeclarationStatus {
        return ((data as? ResolutionMode.WithStatus)?.status ?: declarationStatus)
    }

    private fun prepareSignatureForBodyResolve(callableMember: FirCallableDeclaration) {
        callableMember.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
        callableMember.transformReceiverTypeRef(transformer, ResolutionMode.ContextIndependent)
        if (callableMember is FirFunction) {
            callableMember.valueParameters.forEach {
                it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
                it.transformVarargTypeToArrayType()
            }
        }
    }

    protected fun createTypeParameterScope(declaration: FirMemberDeclaration): FirMemberTypeParameterScope? {
        if (declaration.typeParameters.isEmpty()) return null
        doTransformTypeParameters(declaration)
        return FirMemberTypeParameterScope(declaration)
    }

    private fun doTransformTypeParameters(declaration: FirMemberDeclaration) {
        for (typeParameter in declaration.typeParameters) {
            (typeParameter as? FirTypeParameter)?.let { transformer.replaceDeclarationResolvePhaseIfNeeded(it, FirResolvePhase.STATUS) }
            typeParameter.transformChildren(transformer, ResolutionMode.ContextIndependent)
        }
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: ResolutionMode): FirEnumEntry {
        if (implicitTypeOnly || enumEntry.initializerResolved) return enumEntry
        transformer.replaceDeclarationResolvePhaseIfNeeded(enumEntry, transformerPhase)
        return context.forEnumEntry {
            (enumEntry.transformChildren(this, data) as FirEnumEntry)
        }
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
        require(property !is FirSyntheticProperty) { "Synthetic properties should not be processed by body transformers" }

        if (property.isLocal) {
            prepareSignatureForBodyResolve(property)
            property.transformStatus(this, property.resolveStatus().mode())
            property.getter?.let { it.transformStatus(this, it.resolveStatus(containingProperty = property).mode()) }
            property.setter?.let { it.transformStatus(this, it.resolveStatus(containingProperty = property).mode()) }
            return transformLocalVariable(property)
        }

        val returnTypeRef = property.returnTypeRef
        if (property.initializerAndAccessorsAreResolved) return property
        if (returnTypeRef !is FirImplicitTypeRef && implicitTypeOnly) return property

        property.transformReceiverTypeRef(transformer, ResolutionMode.ContextIndependent)
        dataFlowAnalyzer.enterProperty(property)
        doTransformTypeParameters(property)
        return withFullBodyResolve {
            context.withProperty(property) {
                context.forPropertyInitializer {
                    property.transformDelegate(transformer, ResolutionMode.ContextDependentDelegate)
                    property.transformChildrenWithoutAccessors(returnTypeRef)
                    if (property.initializer != null) {
                        storeVariableReturnType(property)
                    }
                }
                val delegate = property.delegate
                if (delegate != null) {
                    transformPropertyAccessorsWithDelegate(property, delegate)
                    if (property.delegateFieldSymbol != null) {
                        replacePropertyReferenceTypeInDelegateAccessors(property)
                    }
                } else {
                    property.transformAccessors()
                }
            }
            transformer.replaceDeclarationResolvePhaseIfNeeded(property, transformerPhase)
            dataFlowAnalyzer.exitProperty(property)?.let {
                property.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(it))
            }
            property.replaceInitializerAndAccessorsAreResolved(true)
            property
        }
    }

    override fun transformField(field: FirField, data: ResolutionMode): FirField {
        val returnTypeRef = field.returnTypeRef
        if (implicitTypeOnly) return field
        if (field.initializerResolved) return field

        dataFlowAnalyzer.enterField(field)
        return withFullBodyResolve {
            context.withField(field) {
                field.transformChildren(transformer, withExpectedType(returnTypeRef))
            }
            if (field.initializer != null) {
                storeVariableReturnType(field)
            }
            transformer.replaceDeclarationResolvePhaseIfNeeded(field, transformerPhase)
            dataFlowAnalyzer.exitField(field)
            field
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
            val dispatchType = context.containingClass?.let { containingClass ->
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
                }.also {
                    session.lookupTracker?.recordTypeResolveAsLookup(it, propertyReferenceAccess.source ?: source, null)
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

    private fun transformPropertyAccessorsWithDelegate(property: FirProperty, delegateExpression: FirExpression) {
        context.forPropertyDelegateAccessors(property, delegateExpression, resolutionContext, callCompleter) {
            property.transformAccessors()
            val completedCalls = completeCandidates()
            val finalSubstitutor = createFinalSubstitutor()
            val callCompletionResultsWriter = callCompleter.createCompletionResultsWriter(
                finalSubstitutor,
                mode = FirCallCompletionResultsWriterTransformer.Mode.DelegatedPropertyCompletion
            )
            completedCalls.forEach {
                it.transformSingle(callCompletionResultsWriter, null)
            }
            val declarationCompletionResultsWriter = FirDeclarationCompletionResultsWriter(finalSubstitutor)
            property.transformSingle(declarationCompletionResultsWriter, null)
        }
    }

    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: ResolutionMode,
    ): FirStatement {
        dataFlowAnalyzer.enterDelegateExpression()
        try {
            val delegateProvider = wrappedDelegateExpression.delegateProvider.transformSingle(transformer, data)
            when (val calleeReference = (delegateProvider as FirResolvable).calleeReference) {
                is FirResolvedNamedReference -> return delegateProvider
                is FirNamedReferenceWithCandidate -> {
                    val candidate = calleeReference.candidate
                    if (!candidate.system.hasContradiction) {
                        return delegateProvider
                    }
                }
            }

            (delegateProvider as? FirFunctionCall)?.let { dataFlowAnalyzer.dropSubgraphFromCall(it) }
            return wrappedDelegateExpression.expression
                .transformSingle(transformer, data)
                .approximateIfIsIntegerConst()
        } finally {
            dataFlowAnalyzer.exitDelegateExpression()
        }
    }

    private fun transformLocalVariable(variable: FirProperty): FirProperty {
        assert(variable.isLocal)
        variable.transformDelegate(transformer, ResolutionMode.ContextDependentDelegate)
        val delegate = variable.delegate
        if (delegate != null) {
            transformPropertyAccessorsWithDelegate(variable, delegate)
            if (variable.delegateFieldSymbol != null) {
                replacePropertyReferenceTypeInDelegateAccessors(variable)
            }
        } else {
            val resolutionMode = withExpectedType(variable.returnTypeRef)
            if (variable.initializer != null) {
                variable.transformInitializer(transformer, resolutionMode)
                storeVariableReturnType(variable)
            }
            variable.transformAccessors()
        }
        variable.transformOtherChildren(transformer, ResolutionMode.ContextIndependent)
        context.storeVariable(variable)
        transformer.replaceDeclarationResolvePhaseIfNeeded(variable, transformerPhase)
        dataFlowAnalyzer.exitLocalVariableDeclaration(variable)
        return variable
    }

    private fun FirProperty.transformChildrenWithoutAccessors(returnTypeRef: FirTypeRef): FirProperty {
        val data = withExpectedType(returnTypeRef)
        return transformReturnTypeRef(transformer, data)
            .transformInitializer(transformer, data)
            .transformDelegate(transformer, data)
            .transformTypeParameters(transformer, data)
            .transformOtherChildren(transformer, data)
    }

    private fun FirProperty.transformAccessors() {
        var enhancedTypeRef = returnTypeRef
        getter?.let {
            transformAccessor(it, enhancedTypeRef, this)
        }
        if (returnTypeRef is FirImplicitTypeRef) {
            storeVariableReturnType(this)
            enhancedTypeRef = returnTypeRef
            // We need update type of getter for case when its type was approximated
            getter?.replaceReturnTypeRef(enhancedTypeRef)
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
        owner: FirProperty
    ) {
        context.withPropertyAccessor(owner, accessor, components) {
            if (accessor is FirDefaultPropertyAccessor || accessor.body == null) {
                transformFunction(accessor, withExpectedType(enhancedTypeRef))
            } else {
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

                transformFunctionWithGivenSignature(accessor, resolutionMode)
            }
        }
    }

    private fun FirDeclaration.resolveStatus(
        containingClass: FirClass? = null,
        containingProperty: FirProperty? = null,
    ): FirDeclarationStatus {
        val containingDeclaration = context.containerIfAny
        return statusResolver.resolveStatus(
            this,
            containingClass as? FirRegularClass,
            containingProperty,
            isLocal = containingDeclaration != null && containingClass == null
        )
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirStatement {
        if (regularClass.isLocal && regularClass !in context.targetedLocalClasses) {
            return regularClass.runAllPhasesForLocalClass(
                transformer,
                components,
                data,
                transformer.firTowerDataContextCollector,
                transformer.firProviderInterceptor
            )
        }

        doTransformTypeParameters(regularClass)
        return doTransformRegularClass(regularClass, data)
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias {
        if (typeAlias.isLocal && typeAlias !in context.targetedLocalClasses) {
            return typeAlias.runAllPhasesForLocalClass(
                transformer,
                components,
                data,
                transformer.firTowerDataContextCollector,
                transformer.firProviderInterceptor
            )
        }
        doTransformTypeParameters(typeAlias)
        typeAlias.transformAnnotations(transformer, data)
        transformer.firTowerDataContextCollector?.addDeclarationContext(typeAlias, context.towerDataContext)
        typeAlias.transformExpandedTypeRef(transformer, data)
        transformer.replaceDeclarationResolvePhaseIfNeeded(typeAlias, transformerPhase)
        return typeAlias
    }

    private fun doTransformRegularClass(
        regularClass: FirRegularClass,
        data: ResolutionMode
    ): FirRegularClass {
        dataFlowAnalyzer.enterClass()

        val result = context.withRegularClass(regularClass, components) {
            transformDeclarationContent(regularClass, data) as FirRegularClass
        }

        if (!implicitTypeOnly) {
            val controlFlowGraph = dataFlowAnalyzer.exitRegularClass(result)
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(controlFlowGraph))
        } else {
            dataFlowAnalyzer.exitClass()
        }

        return result
    }

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: ResolutionMode
    ): FirStatement {
        if (anonymousObject !in context.targetedLocalClasses) {
            return anonymousObject.runAllPhasesForLocalClass(
                transformer,
                components,
                data,
                transformer.firTowerDataContextCollector,
                transformer.firProviderInterceptor
            )
        }
        dataFlowAnalyzer.enterClass()
        val result = context.withAnonymousObject(anonymousObject, components) {
            transformDeclarationContent(anonymousObject, data) as FirAnonymousObject
        }
        if (!implicitTypeOnly && result.controlFlowGraphReference == null) {
            val graph = dataFlowAnalyzer.exitAnonymousObject(result)
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(graph))
        } else {
            dataFlowAnalyzer.exitClass()
        }
        return result
    }

    private fun transformAnonymousFunctionWithLambdaResolution(
        anonymousFunction: FirAnonymousFunction, lambdaResolution: ResolutionMode.LambdaResolution
    ): FirAnonymousFunction {
        val expectedReturnType =
            lambdaResolution.expectedReturnTypeRef ?: anonymousFunction.returnTypeRef.takeUnless { it is FirImplicitTypeRef }
        val result = transformFunction(anonymousFunction, withExpectedType(expectedReturnType)) as FirAnonymousFunction
        val body = result.body
        if (result.returnTypeRef is FirImplicitTypeRef && body != null) {
            // TODO: This part seems unnecessary because for lambdas in dependent context will be completed and their type
            //  should be replaced there properly
            val returnType =
                dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(result)
                    .firstNotNullOfOrNull { (it as? FirExpression)?.resultType?.coneTypeSafe() }

            if (returnType != null) {
                result.transformReturnTypeRef(transformer, withExpectedType(returnType))
            } else {
                result.transformReturnTypeRef(
                    transformer,
                    withExpectedType(buildErrorTypeRef {
                        diagnostic =
                            ConeSimpleDiagnostic("Unresolved lambda return type", DiagnosticKind.InferenceError)
                    })
                )
            }
        }

        return result
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): FirSimpleFunction {
        if (simpleFunction.bodyResolved) {
            return simpleFunction
        }
        val returnTypeRef = simpleFunction.returnTypeRef
        if ((returnTypeRef !is FirImplicitTypeRef) && implicitTypeOnly) {
            return simpleFunction
        }

        doTransformTypeParameters(simpleFunction)

        val containingDeclaration = context.containerIfAny
        return context.withSimpleFunction(simpleFunction) {
            // TODO: I think it worth creating something like runAllPhasesForLocalFunction
            if (containingDeclaration != null && containingDeclaration !is FirClass) {
                // For class members everything should be already prepared
                prepareSignatureForBodyResolve(simpleFunction)
                simpleFunction.transformStatus(this, simpleFunction.resolveStatus().mode())
            }
            context.forFunctionBody(simpleFunction, components) {
                withFullBodyResolve {
                    transformFunctionWithGivenSignature(simpleFunction, ResolutionMode.ContextIndependent)
                }
            }
        }
    }

    private fun <F : FirFunction> transformFunctionWithGivenSignature(
        function: F,
        resolutionMode: ResolutionMode,
    ): F {
        @Suppress("UNCHECKED_CAST")
        val result = transformFunction(function, resolutionMode) as F

        val body = result.body
        if (result.returnTypeRef is FirImplicitTypeRef) {
            val simpleFunction = function as? FirSimpleFunction
            val returnExpression = (body?.statements?.single() as? FirReturnExpression)?.result
            if (returnExpression != null && returnExpression.typeRef is FirResolvedTypeRef) {
                result.transformReturnTypeRef(
                    transformer,
                    withExpectedType(
                        returnExpression.resultType.approximatedIfNeededOrSelf(
                            inferenceComponents.approximator,
                            simpleFunction?.visibilityForApproximation(),
                            transformer.session.typeContext,
                            simpleFunction?.isInline == true
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

        return result
    }

    override fun transformFunction(
        function: FirFunction,
        data: ResolutionMode
    ): FirStatement {
        val functionIsNotAnalyzed = !function.bodyResolved
        if (functionIsNotAnalyzed) {
            dataFlowAnalyzer.enterFunction(function)
        }
        @Suppress("UNCHECKED_CAST")
        return transformDeclarationContent(function, data).also {
            if (functionIsNotAnalyzed) {
                val result = it as FirFunction
                val controlFlowGraphReference = dataFlowAnalyzer.exitFunction(result)
                result.replaceControlFlowGraphReference(controlFlowGraphReference)
            }
        } as FirStatement
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor {
        if (implicitTypeOnly) return constructor
        val container = context.containerIfAny as? FirRegularClass
        if (constructor.isPrimary && container?.classKind == ClassKind.ANNOTATION_CLASS) {
            return withFirArrayOfCallTransformer {
                @Suppress("UNCHECKED_CAST")
                doTransformConstructor(constructor, data)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return doTransformConstructor(constructor, data)
    }

    private fun doTransformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor {
        val owningClass = context.containerIfAny as? FirRegularClass

        transformer.replaceDeclarationResolvePhaseIfNeeded(constructor, transformerPhase)
        dataFlowAnalyzer.enterFunction(constructor)

        constructor.transformTypeParameters(transformer, data)
            .transformAnnotations(transformer, data)
            .transformReceiverTypeRef(transformer, data)
            .transformReturnTypeRef(transformer, data)

        context.withConstructor(constructor) {
            context.forConstructorParameters(constructor, owningClass, components) {
                constructor.transformValueParameters(transformer, data)
            }
            constructor.transformDelegatedConstructor(transformer, data)
            context.forConstructorBody(constructor) {
                constructor.transformBody(transformer, data)
            }
        }

        val controlFlowGraphReference = dataFlowAnalyzer.exitFunction(constructor)
        constructor.replaceControlFlowGraphReference(controlFlowGraphReference)
        return constructor
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode
    ): FirAnonymousInitializer {
        if (implicitTypeOnly) return anonymousInitializer
        dataFlowAnalyzer.enterInitBlock(anonymousInitializer)
        return context.withAnonymousInitializer(anonymousInitializer) {
            val result =
                transformDeclarationContent(anonymousInitializer, ResolutionMode.ContextIndependent) as FirAnonymousInitializer
            val graph = dataFlowAnalyzer.exitInitBlock(result)
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(graph))
            result
        }
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): FirStatement {
        if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
            transformer.replaceDeclarationResolvePhaseIfNeeded(valueParameter, transformerPhase)
            valueParameter.replaceReturnTypeRef(
                valueParameter.returnTypeRef.errorTypeFromPrototype(
                    ConeSimpleDiagnostic("No type for parameter", DiagnosticKind.ValueParameterWithNoTypeAnnotation)
                )
            )
            return valueParameter
        }

        dataFlowAnalyzer.enterValueParameter(valueParameter)
        val result = context.withValueParameter(valueParameter) {
            transformDeclarationContent(
                valueParameter,
                withExpectedType(valueParameter.returnTypeRef)
            ) as FirValueParameter
        }

        dataFlowAnalyzer.exitValueParameter(result)?.let { graph ->
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(graph))
        }

        return result
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ResolutionMode
    ): FirStatement {
        // Either ContextDependent, ContextIndependent or WithExpectedType could be here
        if (data !is ResolutionMode.LambdaResolution) {
            anonymousFunction.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
            anonymousFunction.transformReceiverTypeRef(transformer, ResolutionMode.ContextIndependent)
            anonymousFunction.valueParameters.forEach { it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent) }
        }
        return when (data) {
            is ResolutionMode.ContextDependent, is ResolutionMode.ContextDependentDelegate -> {
                context.withAnonymousFunction(anonymousFunction, components, data) {
                    anonymousFunction.addReturn()
                }
            }
            is ResolutionMode.LambdaResolution -> {
                context.withAnonymousFunction(anonymousFunction, components, data) {
                    withFullBodyResolve {
                        transformAnonymousFunctionWithLambdaResolution(anonymousFunction, data).addReturn()
                    }
                }
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
                                    source = lambda.source?.fakeElement(FirFakeSourceElementKind.ItLambdaParameter)
                                    moduleData = session.moduleData
                                    origin = FirDeclarationOrigin.Source
                                    returnTypeRef = singleParameterType.toFirResolvedTypeRef()
                                    this.name = name
                                    symbol = FirValueParameterSymbol(name)
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
                                        val resolvedType =
                                            param.returnTypeRef.resolvedTypeFromPrototype(resolvedLambdaAtom.parameters[index])
                                        param.replaceReturnTypeRef(resolvedType)
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
                context.withAnonymousFunction(lambda, components, data) {
                    withFullBodyResolve {
                        lambda = transformFunction(lambda, withExpectedType(bodyExpectedType)) as FirAnonymousFunction
                    }
                }
                // To separate function and separate commit
                val writer = FirCallCompletionResultsWriterTransformer(
                    session,
                    ConeSubstitutor.Empty,
                    components.returnTypeCalculator,
                    inferenceComponents.approximator,
                    dataFlowAnalyzer,
                )
                lambda.transformSingle(writer, expectedTypeRef.coneTypeSafe<ConeKotlinType>()?.toExpectedType())

                val returnStatements = dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(lambda)
                val returnExpressionsExceptLast =
                    if (returnStatements.size > 1)
                        returnStatements - lambda.body?.statements?.lastOrNull()
                    else
                        returnStatements
                val implicitReturns = returnExpressionsExceptLast.filter {
                    (it as? FirExpression)?.typeRef is FirImplicitUnitTypeRef
                }
                val returnType =
                    if (implicitReturns.isNotEmpty()) {
                        // i.e., early return, e.g., l@{ ... return@l ... }
                        // Note that the last statement will be coerced to Unit if needed.
                        session.builtinTypes.unitType.type
                    } else {
                        // Otherwise, compute the common super type of all possible return expressions
                        inferenceComponents.ctx.commonSuperTypeOrNull(
                            returnStatements.mapNotNull { (it as? FirExpression)?.resultType?.coneType }
                        ) ?: session.builtinTypes.unitType.type
                    }
                lambda.replaceReturnTypeRef(
                    lambda.returnTypeRef.resolvedTypeFromPrototype(returnType).also {
                        session.lookupTracker?.recordTypeResolveAsLookup(it, lambda.source, null)
                    }
                )
                lambda.replaceTypeRef(
                    lambda.constructFunctionalTypeRef(
                        isSuspend = expectedTypeRef.coneTypeSafe<ConeKotlinType>()?.isSuspendFunctionType(session) == true
                    ).also {
                        session.lookupTracker?.recordTypeResolveAsLookup(it, lambda.source, null)
                    }
                )
                lambda.addReturn()
            }
            is ResolutionMode.WithStatus, is ResolutionMode.WithExpectedTypeFromCast -> {
                throw AssertionError("Should not be here in WithStatus/WithExpectedTypeFromCast mode")
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
                    override fun <E : FirElement> transformElement(element: E, data: FirExpression): E {
                        if (element == lastStatement) {
                            val returnExpression = buildReturnExpression {
                                source = element.source?.fakeElement(FirFakeSourceElementKind.ImplicitReturn)
                                result = lastStatement
                                target = FirFunctionTarget(null, isLambda = this@addReturn.isLambda).also {
                                    it.bind(this@addReturn)
                                }
                            }
                            @Suppress("UNCHECKED_CAST")
                            return (returnExpression as E)
                        }
                        return element
                    }

                    override fun transformReturnExpression(
                        returnExpression: FirReturnExpression,
                        data: FirExpression
                    ): FirStatement {
                        return returnExpression
                    }
                },
                buildUnitExpression()
            )
        }
        return this
    }

    private fun storeVariableReturnType(variable: FirVariable) {
        val initializer = variable.initializer
        if (variable.returnTypeRef is FirImplicitTypeRef) {
            val resultType = when {
                initializer != null -> {
                    val unwrappedInitializer = initializer.unwrapSmartcastExpression()
                    unwrappedInitializer.resultType
                }
                variable.getter != null && variable.getter !is FirDefaultPropertyAccessor -> variable.getter?.returnTypeRef
                else -> null
            }
            if (resultType != null) {
                val expectedType = when (resultType) {
                    is FirImplicitTypeRef -> buildErrorTypeRef {
                        diagnostic = ConeSimpleDiagnostic("No result type for initializer", DiagnosticKind.InferenceError)
                    }
                    is FirErrorTypeRef -> buildErrorTypeRef {
                        diagnostic = resultType.diagnostic
                        resultType.source?.fakeElement(FirFakeSourceElementKind.ImplicitTypeRef)?.let {
                            source = it
                        }
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
                        expectedType.approximatedIfNeededOrSelf(
                            inferenceComponents.approximator,
                            variable.visibilityForApproximation(),
                            inferenceComponents.session.typeContext,
                        )
                    )
                )
            } else {
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
            if (variable.getter?.returnTypeRef is FirImplicitTypeRef) {
                variable.getter?.transformReturnTypeRef(transformer, withExpectedType(variable.returnTypeRef))
            }
        }
    }

    private object ImplicitToErrorTypeTransformer : FirTransformer<Any?>() {
        override fun <E : FirElement> transformElement(element: E, data: Any?): E {
            return element
        }

        override fun transformValueParameter(valueParameter: FirValueParameter, data: Any?): FirStatement {
            if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
                valueParameter.transformReturnTypeRef(
                    StoreType,
                    valueParameter.returnTypeRef.resolvedTypeFromPrototype(
                        ConeKotlinErrorType(
                            ConeSimpleDiagnostic(
                                "No type for parameter",
                                DiagnosticKind.ValueParameterWithNoTypeAnnotation
                            )
                        )
                    )
                )
            }
            return valueParameter
        }
    }

    private val FirVariable.initializerResolved: Boolean
        get() = initializer?.typeRef is FirResolvedTypeRef

    private val FirFunction.bodyResolved: Boolean
        get() = body?.typeRef is FirResolvedTypeRef
}
