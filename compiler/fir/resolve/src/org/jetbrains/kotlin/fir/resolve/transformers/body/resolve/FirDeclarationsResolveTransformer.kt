/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunctionCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildContextReceiver
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyBlock
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeLocalVariableNoTypeOrInitializer
import org.jetbrains.kotlin.fir.resolve.inference.FirStubTypeTransformer
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedLambdaAtom
import org.jetbrains.kotlin.fir.resolve.inference.extractLambdaInfoFromFunctionalType
import org.jetbrains.kotlin.fir.resolve.substitution.createTypeSubstitutorByTypeConstructor
import org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolver
import org.jetbrains.kotlin.fir.resolve.transformers.toExpectedType
import org.jetbrains.kotlin.fir.resolve.transformers.transformVarargTypeToArrayType
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.symbols.constructStarProjectedType
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name

open class FirDeclarationsResolveTransformer(transformer: FirAbstractBodyResolveTransformerDispatcher) : FirPartialBodyResolveTransformer(transformer) {
    private val statusResolver: FirStatusResolver = FirStatusResolver(session, scopeSession)

    private fun FirDeclaration.visibilityForApproximation(): Visibility {
        val container = context.containers.getOrNull(context.containers.size - 2)
        return visibilityForApproximation(container)
    }

    private inline fun <T> withFirArrayOfCallTransformer(block: () -> T): T {
        transformer.expressionsTransformer.enableArrayOfCallTransformation = true
        return try {
            block()
        } finally {
            transformer.expressionsTransformer.enableArrayOfCallTransformation = false
        }
    }

    protected fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration {
        transformer.firTowerDataContextCollector?.addDeclarationContext(declaration, context.towerDataContext)
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
        callableMember.transformReceiverParameter(transformer, ResolutionMode.ContextIndependent)
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

    protected fun doTransformTypeParameters(declaration: FirMemberDeclaration) {
        for (typeParameter in declaration.typeParameters) {
            typeParameter.transformChildren(transformer, ResolutionMode.ContextIndependent)
        }
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: ResolutionMode): FirEnumEntry {
        if (implicitTypeOnly || enumEntry.initializerResolved) return enumEntry
        return context.forEnumEntry {
            (enumEntry.transformChildren(this, data) as FirEnumEntry)
        }
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty = whileAnalysing(session, property) {
        require(property !is FirSyntheticProperty) { "Synthetic properties should not be processed by body transformers" }

        if (property.isLocal) {
            prepareSignatureForBodyResolve(property)
            property.transformStatus(this, property.resolveStatus().mode())
            property.getter?.let { it.transformStatus(this, it.resolveStatus(containingProperty = property).mode()) }
            property.setter?.let { it.transformStatus(this, it.resolveStatus(containingProperty = property).mode()) }
            property.backingField?.let { it.transformStatus(this, it.resolveStatus(containingProperty = property).mode()) }
            context.withProperty(property) {
                doTransformTypeParameters(property)
            }
            return transformLocalVariable(property)
        }

        val returnTypeRefBeforeResolve = property.returnTypeRef
        val bodyResolveState = property.bodyResolveState
        if (bodyResolveState == FirPropertyBodyResolveState.EVERYTHING_RESOLVED) return property

        val canHaveDeepImplicitTypeRefs = property.hasExplicitBackingField

        if (returnTypeRefBeforeResolve !is FirImplicitTypeRef && implicitTypeOnly && !canHaveDeepImplicitTypeRefs) {
            return property
        }

        property.transformReceiverParameter(transformer, ResolutionMode.ContextIndependent)
        doTransformTypeParameters(property)
        val shouldResolveEverything = !implicitTypeOnly
        return withFullBodyResolve {
            val initializerIsAlreadyResolved = bodyResolveState >= FirPropertyBodyResolveState.INITIALIZER_RESOLVED
            if (!initializerIsAlreadyResolved) {
                dataFlowAnalyzer.enterProperty(property)
            }
            var backingFieldIsAlreadyResolved = false
            context.withProperty(property) {
                context.forPropertyInitializer {
                    if (!initializerIsAlreadyResolved) {
                        property.transformChildrenWithoutComponents(returnTypeRefBeforeResolve)
                        property.replaceBodyResolveState(FirPropertyBodyResolveState.INITIALIZER_RESOLVED)
                    }
                    if (property.initializer != null) {
                        storeVariableReturnType(property)
                    }
                    val canResolveBackingFieldEarly = property.hasExplicitBackingField || property.returnTypeRef is FirResolvedTypeRef
                    if (!initializerIsAlreadyResolved && canResolveBackingFieldEarly) {
                        property.transformBackingField(transformer, withExpectedType(property.returnTypeRef))
                        backingFieldIsAlreadyResolved = true
                    }
                }
                val delegate = property.delegate
                if (delegate != null) {
                    transformPropertyAccessorsWithDelegate(property, delegate)
                    if (property.delegateFieldSymbol != null) {
                        replacePropertyReferenceTypeInDelegateAccessors(property)
                    }
                    property.replaceBodyResolveState(FirPropertyBodyResolveState.EVERYTHING_RESOLVED)
                } else {
                    val hasNonDefaultAccessors = property.getter != null && property.getter !is FirDefaultPropertyAccessor ||
                            property.setter != null && property.setter !is FirDefaultPropertyAccessor
                    val mayResolveSetter = shouldResolveEverything || !hasNonDefaultAccessors
                    val propertyTypeRefAfterResolve = property.returnTypeRef
                    val propertyTypeIsKnown = propertyTypeRefAfterResolve is FirResolvedTypeRef
                    val mayResolveGetter = mayResolveSetter || !propertyTypeIsKnown
                    if (mayResolveGetter) {
                        property.transformAccessors(mayResolveSetter)
                        property.replaceBodyResolveState(
                            if (mayResolveSetter) FirPropertyBodyResolveState.EVERYTHING_RESOLVED
                            else FirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED
                        )
                    } else {
                        // Even though we're not going to resolve accessors themselves (so as to avoid resolve cycle, like KT-48634),
                        // we still need to resolve types in accessors (as per IMPLICIT_TYPES_BODY_RESOLVE contract).
                        property.getter?.transformTypeWithPropertyType(propertyTypeRefAfterResolve)
                        property.setter?.transformTypeWithPropertyType(propertyTypeRefAfterResolve)
                        property.setter?.transformReturnTypeRef(transformer, withExpectedType(session.builtinTypes.unitType.type))
                    }
                }
            }
            if (!initializerIsAlreadyResolved) {
                if (!backingFieldIsAlreadyResolved) {
                    property.transformBackingField(transformer, withExpectedType(property.returnTypeRef))
                }
                dataFlowAnalyzer.exitProperty(property)?.let {
                    property.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(it))
                }
            }
            property
        }
    }

    fun FirProperty.getDefaultAccessorStatus(): FirDeclarationStatus {
        // Downward propagation of `inline` and `external` modifiers (from property to its accessors)
        return FirDeclarationStatusImpl(this.visibility, this.modality).apply {
            isInline = this@getDefaultAccessorStatus.isInline
            isExternal = this@getDefaultAccessorStatus.isExternal
        }
    }

    override fun transformField(field: FirField, data: ResolutionMode): FirField = whileAnalysing(session, field) {
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
            dataFlowAnalyzer.exitField(field)?.let {
                field.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(it))
            }
            field
        }
    }

    private fun FirFunctionCall.replacePropertyReferenceTypeInDelegateAccessors(property: FirProperty) {
        // var someProperty: SomeType
        //     get() = delegate.getValue(thisRef, kProperty: KProperty0/1/2<..., SomeType>)
        //     set() = delegate.getValue(thisRef, kProperty: KProperty0/1/2<..., SomeType>, value)
        val propertyReferenceAccess = resolvedArgumentMapping?.keys?.toList()?.getOrNull(1) as? FirCallableReferenceAccess ?: return
        val typeRef = propertyReferenceAccess.typeRef
        if (typeRef is FirResolvedTypeRef && property.returnTypeRef is FirResolvedTypeRef) {
            val typeArguments = (typeRef.type as ConeClassLikeType).typeArguments
            val extensionType = property.receiverParameter?.typeRef?.coneType
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
                    session.lookupTracker?.recordTypeResolveAsLookup(it, propertyReferenceAccess.source ?: source, components.file.source)
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

    private fun transformPropertyAccessorsWithDelegate(property: FirProperty, delegate: FirExpression) {
        context.forPropertyDelegateAccessors(property, resolutionContext, callCompleter) {
            dataFlowAnalyzer.enterDelegateExpression()
            // Resolve delegate expression, after that, delegate will contain either expr.provideDelegate or expr
            if (property.isLocal) {
                property.transformDelegate(transformer, ResolutionMode.ContextDependentDelegate)
            } else {
                context.forPropertyInitializer {
                    property.transformDelegate(transformer, ResolutionMode.ContextDependentDelegate)
                }
            }

            property.transformAccessors()
            val completedCalls = completeCandidates()

            val finalSubstitutor = createFinalSubstitutor()

            val stubTypeCompletionResultsWriter = FirStubTypeTransformer(finalSubstitutor)
            property.transformSingle(stubTypeCompletionResultsWriter, null)
            property.replaceReturnTypeRef(property.returnTypeRef.approximateDeclarationType(session, property.visibilityForApproximation(), property.isLocal))

            val callCompletionResultsWriter = callCompleter.createCompletionResultsWriter(
                finalSubstitutor,
                mode = FirCallCompletionResultsWriterTransformer.Mode.DelegatedPropertyCompletion
            )
            completedCalls.forEach {
                it.transformSingle(callCompletionResultsWriter, null)
            }

            dataFlowAnalyzer.exitDelegateExpression(delegate)
            property
        }
    }

    override fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: ResolutionMode): FirStatement {
        return propertyAccessor.also {
            transformProperty(it.propertySymbol.fir, data)
        }
    }

    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: ResolutionMode,
    ): FirStatement {
        // First, resolve delegate expression in dependent context
        val delegateExpression = wrappedDelegateExpression.expression.transformSingle(transformer, ResolutionMode.ContextDependent)
            .transformSingle(components.integerLiteralAndOperatorApproximationTransformer, null)

        // Second, replace result type of delegate expression with stub type if delegate not yet resolved
        if (delegateExpression is FirQualifiedAccess) {
            val calleeReference = delegateExpression.calleeReference
            if (calleeReference is FirNamedReferenceWithCandidate) {
                val system = calleeReference.candidate.system
                system.notFixedTypeVariables.forEach {
                    system.markPostponedVariable(it.value.typeVariable)
                }
                val typeVariableTypeToStubType = context.inferenceSession.createSyntheticStubTypes(system)

                val substitutor = createTypeSubstitutorByTypeConstructor(
                    typeVariableTypeToStubType, session.typeContext, approximateIntegerLiterals = true
                )
                val delegateExpressionTypeRef = delegateExpression.typeRef
                val stubTypeSubstituted = substitutor.substituteOrNull(delegateExpressionTypeRef.coneType)
                delegateExpression.replaceTypeRef(delegateExpressionTypeRef.withReplacedConeType(stubTypeSubstituted))
            }
        }

        val provideDelegateCall = wrappedDelegateExpression.delegateProvider as FirFunctionCall

        // Resolve call for provideDelegate, without completion
        // TODO: this generates some nodes in the control flow graph which we don't want if we
        //  end up not selecting this option.
        provideDelegateCall.transformSingle(this, ResolutionMode.ContextIndependent)

        // If we got successful candidate for provideDelegate, let's select it
        val provideDelegateCandidate = provideDelegateCall.candidate()
        if (provideDelegateCandidate != null && provideDelegateCandidate.isSuccessful) {
            val system = provideDelegateCandidate.system
            system.notFixedTypeVariables.forEach {
                system.markPostponedVariable(it.value.typeVariable)
            }
            val typeVariableTypeToStubType = context.inferenceSession.createSyntheticStubTypes(system)
            val substitutor = createTypeSubstitutorByTypeConstructor(
                typeVariableTypeToStubType, session.typeContext, approximateIntegerLiterals = true
            )

            val stubTypeSubstituted = substitutor.substituteOrSelf(
                provideDelegateCandidate.substitutor.substituteOrSelf(
                    components.typeFromCallee(provideDelegateCall).type
                )
            )

            provideDelegateCall.replaceTypeRef(provideDelegateCall.typeRef.resolvedTypeFromPrototype(stubTypeSubstituted))
            return provideDelegateCall
        }

        val provideDelegateReference = provideDelegateCall.calleeReference
        if (provideDelegateReference is FirResolvedNamedReference && provideDelegateReference !is FirResolvedErrorReference) {
            return provideDelegateCall
        }

        // Select delegate expression otherwise
        return delegateExpression
    }

    private fun transformLocalVariable(variable: FirProperty): FirProperty = whileAnalysing(session, variable) {
        assert(variable.isLocal)
        val delegate = variable.delegate

        val hadExplicitType = variable.returnTypeRef !is FirImplicitTypeRef

        if (delegate != null) {
            transformPropertyAccessorsWithDelegate(variable, delegate)
            if (variable.delegateFieldSymbol != null) {
                replacePropertyReferenceTypeInDelegateAccessors(variable)
            }
            // This ensures there's no ImplicitTypeRef
            // left in the backingField (witch is always present).
            variable.transformBackingField(transformer, withExpectedType(variable.returnTypeRef))
        } else {
            val resolutionMode = withExpectedType(variable.returnTypeRef)
            if (variable.initializer != null) {
                variable.transformInitializer(transformer, resolutionMode)
                storeVariableReturnType(variable)
            }
            variable.transformBackingField(transformer, withExpectedType(variable.returnTypeRef))
            variable.transformAccessors()
        }
        variable.transformOtherChildren(transformer, ResolutionMode.ContextIndependent)
        context.storeVariable(variable, session)
        dataFlowAnalyzer.exitLocalVariableDeclaration(variable, hadExplicitType)
        return variable
    }

    private fun FirProperty.transformChildrenWithoutComponents(returnTypeRef: FirTypeRef): FirProperty {
        val data = withExpectedType(returnTypeRef)
        return transformReturnTypeRef(transformer, data)
            .transformInitializer(transformer, data)
            .transformTypeParameters(transformer, data)
            .transformOtherChildren(transformer, data)
    }

    private fun FirProperty.transformAccessors(mayResolveSetter: Boolean = true) {
        var enhancedTypeRef = returnTypeRef
        if (bodyResolveState < FirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED) {
            getter?.let {
                transformAccessor(it, enhancedTypeRef, this)
            }
        }
        if (returnTypeRef is FirImplicitTypeRef) {
            storeVariableReturnType(this)
            enhancedTypeRef = returnTypeRef
            // We need update type of getter for case when its type was approximated
            getter?.transformTypeWithPropertyType(enhancedTypeRef, forceUpdateForNonImplicitTypes = true)
        }
        setter?.let {
            it.transformTypeWithPropertyType(enhancedTypeRef)

            if (mayResolveSetter) {
                transformAccessor(it, enhancedTypeRef, this)
            }
        }
    }

    private fun FirPropertyAccessor.transformTypeWithPropertyType(
        propertyTypeRef: FirTypeRef,
        forceUpdateForNonImplicitTypes: Boolean = false
    ) {
        when {
            isGetter -> {
                if (returnTypeRef is FirImplicitTypeRef || forceUpdateForNonImplicitTypes) {
                    replaceReturnTypeRef(propertyTypeRef.copyWithNewSource(returnTypeRef.source))
                }
            }
            isSetter -> {
                val valueParameter = valueParameters.firstOrNull() ?: return
                if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
                    valueParameter.replaceReturnTypeRef(propertyTypeRef.copyWithNewSource(returnTypeRef.source))
                }
            }
        }
    }

    private fun transformAccessor(
        accessor: FirPropertyAccessor,
        enhancedTypeRef: FirTypeRef,
        owner: FirProperty
    ): Unit = whileAnalysing(session, accessor) {
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
                val resolutionMode = if (owner.delegate == null || expectedReturnTypeRef.coneTypeSafe<ConeKotlinType>()?.isUnit == true) {
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

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirStatement = whileAnalysing(session, regularClass) {
        return context.withContainingClass(regularClass) {
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
            doTransformRegularClass(regularClass, data)
        }
    }

    override fun transformScript(script: FirScript, data: ResolutionMode): FirScript {
        if (implicitTypeOnly) return script
        dataFlowAnalyzer.enterScript(script)
        val result = context.withScopesForScript(script, components) {
            transformDeclarationContent(script, data) as FirScript
        }

        dataFlowAnalyzer.exitScript(script)

        return result
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias = whileAnalysing(session, typeAlias) {
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
        return typeAlias
    }

    private fun doTransformRegularClass(
        regularClass: FirRegularClass,
        data: ResolutionMode
    ): FirRegularClass {
        dataFlowAnalyzer.enterClass(regularClass, !implicitTypeOnly)
        val result = context.withRegularClass(regularClass, components) {
            transformDeclarationContent(regularClass, data) as FirRegularClass
        }
        val controlFlowGraph = dataFlowAnalyzer.exitClass()
        if (controlFlowGraph != null) {
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(controlFlowGraph))
        }
        return result
    }

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, anonymousObject) {
        if (anonymousObject !in context.targetedLocalClasses) {
            return anonymousObject.runAllPhasesForLocalClass(
                transformer,
                components,
                data,
                transformer.firTowerDataContextCollector,
                transformer.firProviderInterceptor
            )
        }
        // TODO: why would there be a graph already?
        val buildGraph = !implicitTypeOnly && anonymousObject.controlFlowGraphReference == null
        dataFlowAnalyzer.enterClass(anonymousObject, buildGraph)
        val result = context.withAnonymousObject(anonymousObject, components) {
            transformDeclarationContent(anonymousObject, data) as FirAnonymousObject
        }
        val graph = dataFlowAnalyzer.exitClass()
        if (graph != null) {
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(graph))
        }
        return result
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): FirSimpleFunction = whileAnalysing(session, simpleFunction) {
        if (simpleFunction.bodyResolved) {
            return simpleFunction
        }
        val returnTypeRef = simpleFunction.returnTypeRef
        if ((returnTypeRef !is FirImplicitTypeRef) && implicitTypeOnly) {
            return simpleFunction
        }

        val containingDeclaration = context.containerIfAny
        return context.withSimpleFunction(simpleFunction, session) {
            doTransformTypeParameters(simpleFunction)

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
            if (returnExpression?.typeRef is FirResolvedTypeRef) {
                result.transformReturnTypeRef(
                    transformer,
                    withExpectedType(
                        returnExpression.resultType.approximateDeclarationType(
                            session,
                            simpleFunction?.visibilityForApproximation(),
                            isLocal = simpleFunction?.isLocal == true,
                            isInlineFunction = simpleFunction?.isInline == true
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
    ): FirStatement = whileAnalysing(session, function) {
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

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor = whileAnalysing(session, constructor) {
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

        dataFlowAnalyzer.enterFunction(constructor)

        context.withConstructor(constructor) {
            constructor.transformTypeParameters(transformer, data)
                .transformAnnotations(transformer, data)
                .transformReceiverParameter(transformer, data)
                .transformReturnTypeRef(transformer, data)

            context.forConstructorParameters(constructor, owningClass, components) {
                constructor.transformValueParameters(transformer, data)
            }
            constructor.transformDelegatedConstructor(transformer, data)
            context.forConstructorBody(constructor, session) {
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
    ): FirAnonymousInitializer = whileAnalysing(session, anonymousInitializer) {
        if (implicitTypeOnly) return anonymousInitializer
        dataFlowAnalyzer.enterInitBlock(anonymousInitializer)
        return context.withAnonymousInitializer(anonymousInitializer, session) {
            val result =
                transformDeclarationContent(anonymousInitializer, ResolutionMode.ContextIndependent) as FirAnonymousInitializer
            val graph = dataFlowAnalyzer.exitInitBlock(result)
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(graph))
            result
        }
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, valueParameter) {
        dataFlowAnalyzer.enterValueParameter(valueParameter)
        val result = context.withValueParameter(valueParameter, session) {
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
    ): FirStatement = whileAnalysing(session, anonymousFunction) {
        // Either ContextDependent, ContextIndependent or WithExpectedType could be here
        if (data !is ResolutionMode.LambdaResolution) {
            anonymousFunction.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
            anonymousFunction.transformReceiverParameter(transformer, ResolutionMode.ContextIndependent)
            anonymousFunction.valueParameters.forEach { it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent) }
        }
        return when (data) {
            is ResolutionMode.ContextDependent, is ResolutionMode.ContextDependentDelegate -> {
                context.withAnonymousFunction(anonymousFunction, components, data) {
                    anonymousFunction
                }
            }
            is ResolutionMode.LambdaResolution -> {
                val expectedReturnTypeRef =
                    data.expectedReturnTypeRef ?: anonymousFunction.returnTypeRef.takeUnless { it is FirImplicitTypeRef }
                transformAnonymousFunctionBody(anonymousFunction, expectedReturnTypeRef, data)
            }
            is ResolutionMode.WithExpectedType ->
                transformAnonymousFunctionWithExpectedType(anonymousFunction, data.expectedTypeRef, data)
            is ResolutionMode.WithSuggestedType ->
                transformAnonymousFunctionWithExpectedType(anonymousFunction, data.suggestedTypeRef, data)
            is ResolutionMode.ContextIndependent, is ResolutionMode.ReceiverResolution ->
                transformAnonymousFunctionWithExpectedType(anonymousFunction, buildImplicitTypeRef(), data)
            is ResolutionMode.WithStatus, is ResolutionMode.WithExpectedTypeFromCast ->
                throw AssertionError("Should not be here in WithStatus/WithExpectedTypeFromCast mode")
        }
    }


    private fun transformAnonymousFunctionBody(
        anonymousFunction: FirAnonymousFunction,
        expectedReturnTypeRef: FirTypeRef?,
        data: ResolutionMode
    ): FirAnonymousFunction {
        // `transformFunction` will replace both `typeRef` and `returnTypeRef`, so make sure to keep the former.
        val lambdaType = anonymousFunction.typeRef
        return context.withAnonymousFunction(anonymousFunction, components, data) {
            withFullBodyResolve {
                transformFunction(anonymousFunction, withExpectedType(expectedReturnTypeRef)) as FirAnonymousFunction
            }
        }.apply { replaceTypeRef(lambdaType) }
    }

    private fun transformAnonymousFunctionWithExpectedType(
        anonymousFunction: FirAnonymousFunction,
        expectedTypeRef: FirTypeRef,
        data: ResolutionMode
    ): FirAnonymousFunction {
        val resolvedLambdaAtom = (expectedTypeRef as? FirResolvedTypeRef)?.let {
            extractLambdaInfoFromFunctionalType(
                it.type, it, anonymousFunction, returnTypeVariable = null, components, candidate = null, duringCompletion = false
            )
        }
        var lambda = anonymousFunction
        val valueParameters = when {
            resolvedLambdaAtom != null -> obtainValueParametersFromResolvedLambdaAtom(resolvedLambdaAtom, lambda)
            else -> obtainValueParametersFromExpectedType(expectedTypeRef.coneTypeSafe(), lambda)
        }
        lambda = buildAnonymousFunctionCopy(lambda) {
            receiverParameter = lambda.receiverParameter?.takeIf { it.typeRef !is FirImplicitTypeRef }
                ?: resolvedLambdaAtom?.receiver?.let { coneKotlinType ->
                    lambda.receiverParameter?.apply {
                        replaceTypeRef(typeRef.resolvedTypeFromPrototype(coneKotlinType))
                    }
                }

            contextReceivers.clear()
            contextReceivers.addAll(
                lambda.contextReceivers.takeIf { it.isNotEmpty() }
                    ?: resolvedLambdaAtom?.contextReceivers?.map { receiverType ->
                        buildContextReceiver {
                            this.typeRef = buildResolvedTypeRef {
                                type = receiverType
                            }
                        }
                    }.orEmpty()
            )

            this.valueParameters.clear()
            this.valueParameters.addAll(valueParameters)
        }.transformValueParameters(ImplicitToErrorTypeTransformer, null)

        val initialReturnTypeRef = lambda.returnTypeRef as? FirResolvedTypeRef
        val expectedReturnTypeRef = initialReturnTypeRef
            ?: resolvedLambdaAtom?.returnType?.let { lambda.returnTypeRef.resolvedTypeFromPrototype(it) }
        lambda = transformAnonymousFunctionBody(lambda, expectedReturnTypeRef ?: components.noExpectedType, data)

        if (initialReturnTypeRef == null) {
            lambda.replaceReturnTypeRef(lambda.computeReturnTypeRef(expectedReturnTypeRef))
            session.lookupTracker?.recordTypeResolveAsLookup(lambda.returnTypeRef, lambda.source, context.file.source)
        }

        lambda.replaceTypeRef(lambda.constructFunctionalTypeRef(resolvedLambdaAtom?.isSuspend == true))
        session.lookupTracker?.recordTypeResolveAsLookup(lambda.typeRef, lambda.source, context.file.source)
        lambda.addReturnToLastStatementIfNeeded()
        return lambda
    }

    private fun FirAnonymousFunction.computeReturnTypeRef(expected: FirResolvedTypeRef?): FirResolvedTypeRef {
        // Any lambda expression assigned to `(...) -> Unit` returns Unit
        if (isLambda && expected?.type?.isUnit == true) return expected
        // `lambda@ { return@lambda }` always returns Unit
        val returnExpressions = dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(this)
        if (shouldReturnUnit(returnExpressions)) return session.builtinTypes.unitType
        // Here is a questionable moment where we could prefer the expected type over an inferred one.
        // In correct code this doesn't matter, as all return expression types should be subtypes of the expected type.
        // In incorrect code, this would change diagnostics: we can get errors either on the entire lambda, or only on its
        // return statements. The former kind of makes more sense, but the latter is more readable.
        val inferredFromReturnExpressions = session.typeContext.commonSuperTypeOrNull(returnExpressions.map { it.resultType.coneType })
        return inferredFromReturnExpressions?.let { returnTypeRef.resolvedTypeFromPrototype(it) }
            ?: session.builtinTypes.unitType // Empty lambda returns Unit
    }

    private fun obtainValueParametersFromResolvedLambdaAtom(
        resolvedLambdaAtom: ResolvedLambdaAtom,
        lambda: FirAnonymousFunction,
    ): List<FirValueParameter> {
        val singleParameterType = resolvedLambdaAtom.parameters.singleOrNull()
        return when {
            lambda.valueParameters.isEmpty() && singleParameterType != null -> {
                val name = Name.identifier("it")
                val itParam = buildValueParameter {
                    source = lambda.source?.fakeElement(KtFakeSourceElementKind.ItLambdaParameter)
                    containingFunctionSymbol = resolvedLambdaAtom.atom.symbol
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

            else -> obtainValueParametersFromExpectedParameterTypes(resolvedLambdaAtom.parameters, lambda)
        }
    }

    private fun obtainValueParametersFromExpectedType(
        expectedType: ConeKotlinType?,
        lambda: FirAnonymousFunction
    ): List<FirValueParameter> {
        if (expectedType == null) return lambda.valueParameters
        if (!expectedType.isFunctionalOrSuspendFunctionalType(session)) return lambda.valueParameters
        val parameterTypes = expectedType.typeArguments
            .mapTo(mutableListOf()) { it.type ?: session.builtinTypes.nullableAnyType.type }
            .also { it.removeLastOrNull() }
        if (expectedType.isExtensionFunctionType) {
            parameterTypes.removeFirstOrNull()
        }
        return obtainValueParametersFromExpectedParameterTypes(parameterTypes, lambda)
    }

    private fun obtainValueParametersFromExpectedParameterTypes(
        expectedTypeParameterTypes: List<ConeKotlinType>,
        lambda: FirAnonymousFunction
    ): List<FirValueParameter> {
        return lambda.valueParameters.mapIndexed { index, param ->
            if (param.returnTypeRef is FirResolvedTypeRef) {
                param
            } else {
                val resolvedType =
                    param.returnTypeRef.resolvedTypeFromPrototype(expectedTypeParameterTypes[index])
                param.replaceReturnTypeRef(resolvedType)
                param
            }
        }
    }

    override fun transformBackingField(
        backingField: FirBackingField,
        data: ResolutionMode,
    ): FirStatement = whileAnalysing(session, backingField) {
        val propertyType = data.expectedType
        val initializerData = if (backingField.returnTypeRef is FirResolvedTypeRef) {
            withExpectedType(backingField.returnTypeRef)
        } else if (propertyType != null) {
            ResolutionMode.WithSuggestedType(propertyType)
        } else {
            ResolutionMode.ContextDependent
        }
        backingField.transformInitializer(transformer, initializerData)
        if (
            backingField.returnTypeRef is FirErrorTypeRef ||
            backingField.returnTypeRef is FirResolvedTypeRef
        ) {
            return backingField
        }
        val inferredType = if (backingField is FirDefaultPropertyBackingField) {
            propertyType
        } else {
            backingField.initializer?.unwrapSmartcastExpression()?.typeRef
        }
        val resultType = inferredType
            ?: return backingField.transformReturnTypeRef(
                transformer,
                withExpectedType(
                    buildErrorTypeRef {
                        diagnostic = ConeSimpleDiagnostic(
                            "Cannot infer variable type without an initializer",
                            DiagnosticKind.InferenceError,
                        )
                    },
                )
            )
        val expectedType = resultType.toExpectedTypeRef()
        return backingField.transformReturnTypeRef(
            transformer,
            withExpectedType(
                expectedType.approximateDeclarationType(session, backingField.visibilityForApproximation(), isLocal = false)
            )
        )
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

            variable.transformReturnTypeRef(
                transformer,
                withExpectedType(
                    resultType?.let {
                        val expectedType = it.toExpectedTypeRef()
                        expectedType.approximateDeclarationType(session, variable.visibilityForApproximation(), variable.isLocal)
                    } ?: buildErrorTypeRef {
                        diagnostic = ConeLocalVariableNoTypeOrInitializer(variable)
                        source = variable.source
                    }
                )
            )
            if (variable.getter?.returnTypeRef is FirImplicitTypeRef) {
                variable.getter?.transformReturnTypeRef(transformer, withExpectedType(variable.returnTypeRef))
            }
        }
    }

    private val FirVariable.isLocal: Boolean
        get() = when (this) {
            is FirProperty -> this.isLocal
            is FirValueParameter -> true
            else -> false
        }

    private fun FirTypeRef.toExpectedTypeRef(): FirResolvedTypeRef {
        return when (this) {
            is FirImplicitTypeRef -> buildErrorTypeRef {
                diagnostic = ConeSimpleDiagnostic("No result type for initializer", DiagnosticKind.InferenceError)
            }
            is FirErrorTypeRef -> buildErrorTypeRef {
                diagnostic = this@toExpectedTypeRef.diagnostic
                this@toExpectedTypeRef.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)?.let {
                    source = it
                }
            }
            else -> {
                buildResolvedTypeRef {
                    type = this@toExpectedTypeRef.coneType
                    annotations.addAll(this@toExpectedTypeRef.annotations)
                }
            }
        }
    }

    private object ImplicitToErrorTypeTransformer : FirTransformer<Any?>() {
        override fun <E : FirElement> transformElement(element: E, data: Any?): E {
            return element
        }

        override fun transformValueParameter(valueParameter: FirValueParameter, data: Any?): FirStatement = whileAnalysing(valueParameter.moduleData.session, valueParameter) {
            if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
                valueParameter.replaceReturnTypeRef(
                    valueParameter.returnTypeRef.resolvedTypeFromPrototype(
                        ConeErrorType(
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

    protected val FirFunction.bodyResolved: Boolean
        get() = body !is FirLazyBlock && body?.typeRef is FirResolvedTypeRef

}
