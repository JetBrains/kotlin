/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunctionCopy
import org.jetbrains.kotlin.fir.declarations.builder.buildContextReceiver
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeLocalVariableNoTypeOrInitializer
import org.jetbrains.kotlin.fir.resolve.inference.FirDelegatedPropertyInferenceSession
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedLambdaAtom
import org.jetbrains.kotlin.fir.resolve.inference.extractLambdaInfoFromFunctionType
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolver
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.runContractResolveForFunction
import org.jetbrains.kotlin.fir.resolve.transformers.transformVarargTypeToArrayType
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.resolve.calls.inference.buildCurrentSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.ProvideDelegateFixationPosition
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

open class FirDeclarationsResolveTransformer(
    transformer: FirAbstractBodyResolveTransformerDispatcher
) : FirPartialBodyResolveTransformer(transformer) {
    private val statusResolver: FirStatusResolver = FirStatusResolver(session, scopeSession)

    private fun FirDeclaration.visibilityForApproximation(): Visibility {
        val container = context.containers.getOrNull(context.containers.size - 2)
        return visibilityForApproximation(container)
    }

    private inline fun <T> withFirArrayOfCallTransformer(block: () -> T): T {
        transformer.expressionsTransformer?.enableArrayOfCallTransformation = true
        return try {
            block()
        } finally {
            transformer.expressionsTransformer?.enableArrayOfCallTransformation = false
        }
    }

    protected fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration {
        transformer.firResolveContextCollector?.addDeclarationContext(declaration, context)
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

        val cannotHaveDeepImplicitTypeRefs = property.backingField?.returnTypeRef !is FirImplicitTypeRef
        if (implicitTypeOnly && returnTypeRefBeforeResolve !is FirImplicitTypeRef && cannotHaveDeepImplicitTypeRefs) {
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
                        val resolutionMode = withExpectedType(returnTypeRefBeforeResolve)
                        property.transformReturnTypeRef(transformer, resolutionMode)
                            .transformInitializer(transformer, resolutionMode)
                            .transformTypeParameters(transformer, resolutionMode)
                            .replaceBodyResolveState(FirPropertyBodyResolveState.INITIALIZER_RESOLVED)
                    }
                    // Return type needs to be resolved before resolving annotations (transformOtherChildren) because of a possible cycle
                    // @Ann(myConst) const val myConst = ""
                    if (property.initializer != null) {
                        storeVariableReturnType(property)
                    }
                    if (!initializerIsAlreadyResolved) {
                        property.transformOtherChildren(transformer, data)
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
                    val hasDefaultAccessors =
                        (property.getter == null || property.getter is FirDefaultPropertyAccessor) &&
                                (property.setter == null || property.setter is FirDefaultPropertyAccessor)
                    val mayResolveSetter = shouldResolveEverything || hasDefaultAccessors
                    val propertyTypeRefAfterResolve = property.returnTypeRef
                    val propertyTypeIsKnown = propertyTypeRefAfterResolve is FirResolvedTypeRef
                    val mayResolveGetter = mayResolveSetter || !propertyTypeIsKnown
                    if (mayResolveGetter) {
                        property.transformAccessors(
                            if (mayResolveSetter) SetterResolutionMode.FULLY_RESOLVE else SetterResolutionMode.ONLY_IMPLICIT_PARAMETER_TYPE
                        )
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
        val type = propertyReferenceAccess.resolvedType
        if (property.returnTypeRef is FirResolvedTypeRef) {
            val typeArguments = (type.type as ConeClassLikeType).typeArguments
            val extensionType = property.receiverParameter?.typeRef?.coneType
            val dispatchType = context.containingClass?.let { containingClass ->
                containingClass.symbol.constructStarProjectedType(containingClass.typeParameters.size)
            }
            propertyReferenceAccess.replaceConeTypeOrNull(
                (type.type as ConeClassLikeType).lookupTag.constructClassType(
                    typeArguments.mapIndexed { index, argument ->
                        when (index) {
                            typeArguments.lastIndex -> property.returnTypeRef.coneType
                            0 -> extensionType ?: dispatchType
                            else -> dispatchType
                        } ?: argument
                    }.toTypedArray(),
                    isNullable = false
                ).also {
                    session.lookupTracker?.recordTypeResolveAsLookup(
                        it,
                        propertyReferenceAccess.source ?: source,
                        components.file.source
                    )
                }
            )
        }
    }

    private fun replacePropertyReferenceTypeInDelegateAccessors(property: FirProperty) {
        (property.getter?.body?.statements?.singleOrNull() as? FirReturnExpression)?.let { returnExpression ->
            (returnExpression.result as? FirFunctionCall)?.replacePropertyReferenceTypeInDelegateAccessors(property)
        }
        (property.setter?.body?.statements?.singleOrNull() as? FirReturnExpression)?.let { returnExpression ->
            (returnExpression.result as? FirFunctionCall)?.replacePropertyReferenceTypeInDelegateAccessors(property)
        }
        (property.delegate as? FirFunctionCall)?.replacePropertyReferenceTypeInDelegateAccessors(property)
    }

    private fun transformPropertyAccessorsWithDelegate(property: FirProperty, delegate: FirExpression) {
        val isImplicitTypedProperty = property.returnTypeRef is FirImplicitTypeRef

        context.forPropertyDelegateAccessors(property, resolutionContext, callCompleter) {
            dataFlowAnalyzer.enterDelegateExpression()
            // Resolve delegate expression, after that, delegate will contain either expr.provideDelegate or expr
            if (property.isLocal) {
                property.transformDelegate(transformer, ResolutionMode.ContextDependent.Delegate)
            } else {
                context.forPropertyInitializer {
                    property.transformDelegate(transformer, ResolutionMode.ContextDependent.Delegate)
                }
            }

            // We don't use inference from setValue calls (i.e. don't resolve setters until the delegate inference is completed),
            // when property doesn't have explicit type.
            // It's necessary because we need to supply the property type as the 3rd argument for `setValue` and there might be uninferred
            // variables from `getValue`.
            // The same logic was used at K1 (see org.jetbrains.kotlin.resolve.DelegatedPropertyResolver.inferDelegateTypeFromGetSetValueMethods)
            property.transformAccessors(if (isImplicitTypedProperty) SetterResolutionMode.SKIP else SetterResolutionMode.FULLY_RESOLVE)
            val completedCalls = completeCandidates()

            val finalSubstitutor = createFinalSubstitutor()

            finalSubstitutor.substituteOrNull(property.returnTypeRef.coneType)?.let { substitutedType ->
                property.replaceReturnTypeRef(property.returnTypeRef.withReplacedConeType(substitutedType))
            }
            property.getter?.transformTypeWithPropertyType(property.returnTypeRef, forceUpdateForNonImplicitTypes = true)
            property.setter?.transformTypeWithPropertyType(property.returnTypeRef, forceUpdateForNonImplicitTypes = true)
            property.replaceReturnTypeRef(
                property.returnTypeRef.approximateDeclarationType(
                    session,
                    property.visibilityForApproximation(),
                    property.isLocal
                )
            )

            val callCompletionResultsWriter = callCompleter.createCompletionResultsWriter(
                finalSubstitutor,
                mode = FirCallCompletionResultsWriterTransformer.Mode.DelegatedPropertyCompletion
            )
            completedCalls.forEach {
                it.transformSingle(callCompletionResultsWriter, null)
            }
        }

        // `isImplicitTypedProperty` means we haven't run setter resolution yet (see its second usage)
        if (isImplicitTypedProperty) {
            property.resolveSetter(mayResolveSetterBody = true)
        }

        dataFlowAnalyzer.exitDelegateExpression(delegate)
    }

    override fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: ResolutionMode): FirPropertyAccessor {
        return propertyAccessor.also {
            transformProperty(it.propertySymbol.fir, data)
        }
    }

    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: ResolutionMode,
    ): FirStatement {
        // First, resolve delegate expression in dependent context, and add potentially partially resolved call to inference session
        // (that is why we use ResolutionMode.ContextDependent.Delegate instead of plain ContextDependent)
        val delegateExpression = wrappedDelegateExpression.expression.transformSingle(transformer, ResolutionMode.ContextDependent.Delegate)
            .transformSingle(components.integerLiteralAndOperatorApproximationTransformer, null)

        val provideDelegateCall = wrappedDelegateExpression.delegateProvider as FirFunctionCall
        provideDelegateCall.replaceExplicitReceiver(delegateExpression)

        // Resolve call for provideDelegate, without completion
        // TODO: this generates some nodes in the control flow graph which we don't want if we
        //  end up not selecting this option, KT-59684
        transformer.expressionsTransformer?.transformFunctionCallInternal(
            provideDelegateCall, ResolutionMode.ContextDependent, provideDelegate = true
        )

        // If we got successful candidate for provideDelegate, let's select it
        val provideDelegateCandidate = provideDelegateCall.candidate()
        if (provideDelegateCandidate != null && provideDelegateCandidate.isSuccessful) {
            val additionalBinding = findResultTypeForInnerVariableIfNeeded(provideDelegateCall, provideDelegateCandidate)

            val substitutor = ChainedSubstitutor(
                provideDelegateCandidate.substitutor,
                (context.inferenceSession as FirDelegatedPropertyInferenceSession).currentConstraintStorage.buildCurrentSubstitutor(
                    session.typeContext, additionalBinding?.let(::mapOf) ?: emptyMap()
                ) as ConeSubstitutor
            )

            val toTypeVariableSubstituted =
                substitutor.substituteOrSelf(components.typeFromCallee(provideDelegateCall).type)

            provideDelegateCall.replaceConeTypeOrNull(toTypeVariableSubstituted)
            return provideDelegateCall
        }

        val provideDelegateReference = provideDelegateCall.calleeReference
        if (provideDelegateReference is FirResolvedNamedReference && provideDelegateReference !is FirResolvedErrorReference) {
            return provideDelegateCall
        }

        // Select delegate expression otherwise
        return delegateExpression
    }

    /**
     * For supporting the case when `provideDelegate` has a signature with type variable as a return type, like
     *  fun <K> K.provideDelegate(receiver: Any?, property: kotlin.reflect.KProperty<*>): K = this
     *
     * Here, if delegate expression returns something like `Delegate<Tv>` where Tv is a variable and the `Delegate` class contains
     * the member `getValue`, we need to fix `K` into `Delegate<Tv>`, so that resulting `provideDelegate()` expression would have the type,
     * so we could look into its member scope (as we can't look into the member scope of `K` type variable).
     *
     * On another hand, we can't just actually fix `K` variable (or just run FULL completion there) as the current result might refer
     * other not fixed yet type variables, and we would break the contract that fixation results should not contain other type variables.
     *
     * Thus, to support exactly the case when we had to look into the member scope of `K`, we just pretend like we fixing it
     *
     * @see compiler/testData/diagnostics/tests/delegatedProperty/provideDelegate/provideDelegateResolutionWithStubTypes.kt
     *
     * In K1, it was working because we used stub types that are not counted as actual type variables, and we've been completing
     * `provideDelegate` FULLy in the context where outer type variables were stubs (thus counted as proper types).
     *
     * But in K2, we decided to get rid of the stub type concept and just stick to the type variables.
     *
     * @return K to Delegate<Tv> or null in case return type of `provideDelegate` is not a type variable.
     *
     * TODO: reconsider the place where the function belong and it necessity after PCLA is implemented (KT-61740 for tracking)
     */
    private fun findResultTypeForInnerVariableIfNeeded(
        provideDelegate: FirFunctionCall,
        candidate: Candidate
    ): Pair<TypeConstructorMarker, ConeKotlinType>? {
        // We're only interested in the case when `provideDelegate` candidate returns a type variable
        // because in other cases we could look into the member scope of the type.
        val returnTypeBasedOnVariable =
            components.typeFromCallee(provideDelegate).type
                // Substitut type parameter to type variable
                .let(candidate.substitutor::substituteOrSelf)
                .unwrapTopLevelVariableType() ?: return null
        val typeVariable = returnTypeBasedOnVariable.lookupTag

        val candidateSystem = candidate.system
        val candidateStorage = candidateSystem.currentStorage()
        val allTypeVariables = candidateStorage.allTypeVariables.keys.toList()
        // Subset of type variables obtained from the `provideDelegate` call
        val typeVariablesRelatedToProvideDelegate =
            allTypeVariables.subList(candidateStorage.outerSystemVariablesPrefixSize, allTypeVariables.size).toSet()

        check(typeVariable in typeVariablesRelatedToProvideDelegate) {
            "Return type of provideDelegate is expected to be one of the type variables of a candidate, but $typeVariable was found"
        }

        val variableWithConstraints =
            candidateSystem.notFixedTypeVariables[typeVariable] ?: error("Not found type variable $typeVariable")

        var resultType: ConeKotlinType? = null

        // Temporary declare all the "outer" variables as proper (i.e., all inner variables as improper)
        // Without that, all variables (both inner and outer ones) would be considered as improper,
        // while we want to fix to assume `Delegate<Tv>` as proper because `Tv` belongs to the outer system
        candidateSystem.withTypeVariablesThatAreNotCountedAsProperTypes(typeVariablesRelatedToProvideDelegate) {
            // TODO: reconsider the approach here (KT-61781 for tracking)
            // Actually, this code might fail with an exception in some rare cases (see KT-61781)
            // The problem is that in the issue example, when fixing T type variable, it has two upper bounds: X and Delegate<Y>
            // So, when ResultTypeResolver builds result type, it tries to intersect them and fails with an exception,
            // because both TypeIntersector and CommonSuperTypeCalculator are not ready to the situation
            // where the part of an input is a type variable.
            //
            // Just for the inspiration, take a look at ResultTypeResolver.Context.buildNotFixedVariablesToStubTypesSubstitutor usages:
            // it seems like they do something relevant.
            resultType = inferenceComponents.resultTypeResolver.findResultTypeOrNull(
                candidateSystem, variableWithConstraints, TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
            ) as? ConeKotlinType ?: return@withTypeVariablesThatAreNotCountedAsProperTypes


            check(!candidateStorage.hasContradiction) { "We only should try fixing variables on successful provideDelegate candidate" }
            // We don't actually fix it, but add an equality constraint as approximation
            candidateSystem.addEqualityConstraint(returnTypeBasedOnVariable, resultType!!, ProvideDelegateFixationPosition)

            check(!candidateStorage.hasContradiction) {
                "Currently, we see no cases when contradiction might happen after adding equality constraint like that." +
                        "But if you see the message, please report your case to https://youtrack.jetbrains.com/newIssue?project=KT"
            }
        }

        return resultType?.let {
            typeVariable to it
        }
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

    // In IDE there's a need to resolve setter's parameter types on the implicit-resolution stage
    // See ad183434137939a0c9eeea2f7df9ef522672a18e commit.
    // But for delegate inference case, we don't need both body of the setter and its parameter resolved (SKIP mode)
    private enum class SetterResolutionMode {
        FULLY_RESOLVE, ONLY_IMPLICIT_PARAMETER_TYPE, SKIP
    }

    private fun FirProperty.transformAccessors(
        setterResolutionMode: SetterResolutionMode = SetterResolutionMode.FULLY_RESOLVE
    ) {
        if (bodyResolveState < FirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED) {
            getter?.let {
                transformAccessor(it, this)
            }
        }
        if (returnTypeRef is FirImplicitTypeRef) {
            storeVariableReturnType(this) // Here, we expect `this.returnTypeRef` is updated from the getter's return type
            // We need update type of getter for case when its type was approximated
            getter?.transformTypeWithPropertyType(returnTypeRef, forceUpdateForNonImplicitTypes = true)
        }

        if (setterResolutionMode != SetterResolutionMode.SKIP) {
            resolveSetter(mayResolveSetterBody = setterResolutionMode == SetterResolutionMode.FULLY_RESOLVE)
        }
    }

    private fun ConeKotlinType.unwrapTopLevelVariableType(): ConeTypeVariableType? = when {
        this is ConeTypeVariableType -> this
        this is ConeFlexibleType -> lowerBound.unwrapTopLevelVariableType()
        this is ConeDefinitelyNotNullType -> original.unwrapTopLevelVariableType()
        else -> null
    }

    private fun FirProperty.resolveSetter(
        mayResolveSetterBody: Boolean,
    ) {
        setter?.let {
            it.transformTypeWithPropertyType(returnTypeRef)

            if (mayResolveSetterBody) {
                transformAccessor(it, this)
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
                if (valueParameter.returnTypeRef is FirImplicitTypeRef || forceUpdateForNonImplicitTypes) {
                    valueParameter.replaceReturnTypeRef(propertyTypeRef.copyWithNewSource(returnTypeRef.source))
                }
            }
        }
    }

    private fun transformAccessor(
        accessor: FirPropertyAccessor,
        owner: FirProperty
    ): Unit = whileAnalysing(session, accessor) {
        context.withPropertyAccessor(owner, accessor, components) {
            val propertyTypeRef = owner.returnTypeRef

            // Currently, this condition might only be true for delegates, because if type is set explicitly for the property,
            // it's been propagated to receivers in the RawFirBuilder
            if (accessor.returnTypeRef is FirImplicitTypeRef && propertyTypeRef !is FirImplicitTypeRef) {
                accessor.replaceReturnTypeRef(propertyTypeRef)
            }

            if (accessor is FirDefaultPropertyAccessor || accessor.body == null) {
                transformFunction(accessor, ResolutionMode.ContextIndependent)
            } else {
                transformFunctionWithGivenSignature(accessor)
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

    override fun transformFile(
        file: FirFile,
        data: ResolutionMode,
    ): FirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            doTransformFile(file, data)
        }
    }

    override fun transformRegularClass(
        regularClass: FirRegularClass,
        data: ResolutionMode
    ): FirRegularClass =
        whileAnalysing(session, regularClass) {
            return context.withContainingClass(regularClass) {
                if (regularClass.isLocal && regularClass !in context.targetedLocalClasses) {
                    return regularClass.runAllPhasesForLocalClass(transformer, components, data, transformer.firResolveContextCollector)
                }

                doTransformTypeParameters(regularClass)
                doTransformRegularClass(regularClass, data)
            }
        }

    fun withScript(script: FirScript, action: () -> FirScript): FirScript {
        dataFlowAnalyzer.enterScript(script)
        val result = context.withScript(script, components) {
            action()
        }
        dataFlowAnalyzer.exitScript() // TODO: FirScript should be a FirControlFlowGraphOwner, KT-59683
        return result
    }

    override fun transformScript(script: FirScript, data: ResolutionMode): FirScript = withScript(script) {
        transformDeclarationContent(script, data) as FirScript
    }

    override fun transformCodeFragment(codeFragment: FirCodeFragment, data: ResolutionMode): FirCodeFragment {
        dataFlowAnalyzer.enterCodeFragment(codeFragment)
        context.withCodeFragment(codeFragment, components) {
            transformBlock(codeFragment.block, data)
        }
        dataFlowAnalyzer.exitCodeFragment()
        return codeFragment
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias = whileAnalysing(session, typeAlias) {
        if (typeAlias.isLocal && typeAlias !in context.targetedLocalClasses) {
            return typeAlias.runAllPhasesForLocalClass(transformer, components, data, transformer.firResolveContextCollector)
        }
        doTransformTypeParameters(typeAlias)
        typeAlias.transformAnnotations(transformer, data)
        transformer.firResolveContextCollector?.addDeclarationContext(typeAlias, context)
        typeAlias.transformExpandedTypeRef(transformer, data)
        return typeAlias
    }

    private fun doTransformFile(
        file: FirFile,
        data: ResolutionMode,
    ): FirFile = withFile(file) {
        transformer.firResolveContextCollector?.addFileContext(file, context.towerDataContext)

        transformDeclarationContent(file, data) as FirFile
    }

    open fun withFile(
        file: FirFile,
        action: () -> FirFile,
    ): FirFile {
        val result = context.withFile(file, components) {
            // TODO Must be done within 'withFile' as the context - any the analyzer - is cleared as the first step.
            //  yuk. maybe the clear shouldn't happen for `enterFile`? or at maybe separately?
            dataFlowAnalyzer.enterFile(file, buildGraph = transformer.buildCfgForFiles)

            action()
        }

        val controlFlowGraph = dataFlowAnalyzer.exitFile()
        if (controlFlowGraph != null) {
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(controlFlowGraph))
        }

        return result
    }

    protected fun doTransformRegularClass(
        regularClass: FirRegularClass,
        data: ResolutionMode
    ): FirRegularClass = withRegularClass(regularClass) {
        transformDeclarationContent(regularClass, data) as FirRegularClass
    }

    open fun withRegularClass(
        regularClass: FirRegularClass,
        action: () -> FirRegularClass
    ): FirRegularClass {
        dataFlowAnalyzer.enterClass(regularClass, buildGraph = transformer.preserveCFGForClasses)
        val result = context.withRegularClass(regularClass, components) {
            action()
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
    ): FirAnonymousObject = whileAnalysing(session, anonymousObject) {
        if (anonymousObject !in context.targetedLocalClasses) {
            return anonymousObject.runAllPhasesForLocalClass(transformer, components, data, transformer.firResolveContextCollector)
        }
        require(anonymousObject.controlFlowGraphReference == null)
        val buildGraph = !implicitTypeOnly
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

            if (containingDeclaration != null && containingDeclaration !is FirClass) {
                // For class members everything should be already prepared
                prepareSignatureForBodyResolve(simpleFunction)
                simpleFunction.transformStatus(this, simpleFunction.resolveStatus().mode())

                if (simpleFunction.contractDescription != FirEmptyContractDescription) {
                    simpleFunction.runContractResolveForFunction(session, scopeSession, context)
                }
            }
            context.forFunctionBody(simpleFunction, components) {
                withFullBodyResolve {
                    transformFunctionWithGivenSignature(simpleFunction)
                }
            }
        }
    }

    private fun <F : FirFunction> transformFunctionWithGivenSignature(function: F): F {
        @Suppress("UNCHECKED_CAST")
        val result = transformFunction(function, ResolutionMode.ContextIndependent) as F

        val body = result.body
        if (result.returnTypeRef is FirImplicitTypeRef) {
            val simpleFunction = function as? FirSimpleFunction
            val returnExpression = (body?.statements?.singleOrNull() as? FirReturnExpression)?.result
            val expressionType = returnExpression?.resolvedType
            val returnTypeRef = expressionType
                ?.toFirResolvedTypeRef(result.returnTypeRef.source)
                ?.approximateDeclarationType(
                    session,
                    simpleFunction?.visibilityForApproximation(),
                    isLocal = simpleFunction?.isLocal == true,
                    isInlineFunction = simpleFunction?.isInline == true
                )
                ?: buildErrorTypeRef {
                    source = result.returnTypeRef.source
                    diagnostic = ConeSimpleDiagnostic("empty body", DiagnosticKind.Other)
                }
            result.transformReturnTypeRef(transformer, withExpectedType(returnTypeRef))
        }

        return result
    }

    override fun transformFunction(
        function: FirFunction,
        data: ResolutionMode
    ): FirFunction = whileAnalysing(session, function) {
        if (function.bodyResolved) return function
        dataFlowAnalyzer.enterFunction(function)
        return transformDeclarationContent(function, data).also {
            val result = it as FirFunction
            val controlFlowGraphReference = dataFlowAnalyzer.exitFunction(result)
            result.replaceControlFlowGraphReference(controlFlowGraphReference)
        } as FirFunction
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor =
        whileAnalysing(session, constructor) {
            if (implicitTypeOnly) return constructor
            val container = context.containerIfAny as? FirRegularClass
            if (constructor.isPrimary && container?.classKind == ClassKind.ANNOTATION_CLASS) {
                return withFirArrayOfCallTransformer {

                    doTransformConstructor(constructor, data)
                }
            }

            return doTransformConstructor(constructor, data)
        }

    override fun transformErrorPrimaryConstructor(
        errorPrimaryConstructor: FirErrorPrimaryConstructor,
        data: ResolutionMode,
    ): FirErrorPrimaryConstructor = transformConstructor(errorPrimaryConstructor, data) as FirErrorPrimaryConstructor

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

    override fun transformMultiDelegatedConstructorCall(
        multiDelegatedConstructorCall: FirMultiDelegatedConstructorCall,
        data: ResolutionMode,
    ): FirStatement {
        multiDelegatedConstructorCall.transformChildren(this, data)
        return super.transformMultiDelegatedConstructorCall(multiDelegatedConstructorCall, data)
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
            val graph = dataFlowAnalyzer.exitInitBlock()
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(graph))
            result
        }
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: ResolutionMode
    ): FirValueParameter = whileAnalysing(session, valueParameter) {
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
    ): FirAnonymousFunction = whileAnalysing(session, anonymousFunction) {
        // Either ContextDependent, ContextIndependent or WithExpectedType could be here
        anonymousFunction.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        if (data !is ResolutionMode.LambdaResolution) {
            anonymousFunction.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
            anonymousFunction.transformReceiverParameter(transformer, ResolutionMode.ContextIndependent)
            anonymousFunction.valueParameters.forEach { it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent) }
        }

        if (anonymousFunction.contractDescription != FirEmptyContractDescription) {
            anonymousFunction.runContractResolveForFunction(session, scopeSession, context)
        }

        return when (data) {
            is ResolutionMode.ContextDependent -> {
                context.storeContextForAnonymousFunction(anonymousFunction)
                anonymousFunction
            }
            is ResolutionMode.LambdaResolution -> {
                val expectedReturnTypeRef =
                    data.expectedReturnTypeRef ?: anonymousFunction.returnTypeRef.takeUnless { it is FirImplicitTypeRef }
                transformAnonymousFunctionBody(anonymousFunction, expectedReturnTypeRef, data)
            }
            is ResolutionMode.WithExpectedType ->
                transformAnonymousFunctionWithExpectedType(anonymousFunction, data.expectedTypeRef, data)
            is ResolutionMode.ContextIndependent, is ResolutionMode.AssignmentLValue, is ResolutionMode.ReceiverResolution ->
                transformAnonymousFunctionWithExpectedType(anonymousFunction, FirImplicitTypeRefImplWithoutSource, data)
            is ResolutionMode.WithStatus ->
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
                transformFunction(
                    anonymousFunction,
                    expectedReturnTypeRef?.let(::withExpectedType) ?: ResolutionMode.ContextDependent
                ) as FirAnonymousFunction
            }
        }.apply { replaceTypeRef(lambdaType) }
    }

    private fun transformAnonymousFunctionWithExpectedType(
        anonymousFunction: FirAnonymousFunction,
        expectedTypeRef: FirTypeRef,
        data: ResolutionMode
    ): FirAnonymousFunction {
        val resolvedLambdaAtom = (expectedTypeRef as? FirResolvedTypeRef)?.let {
            extractLambdaInfoFromFunctionType(
                it.type, anonymousFunction, returnTypeVariable = null, components, candidate = null,
                allowCoercionToExtensionReceiver = true,
            )
        }
        var lambda = anonymousFunction
        val valueParameters = when {
            resolvedLambdaAtom != null -> obtainValueParametersFromResolvedLambdaAtom(resolvedLambdaAtom, lambda)
            else -> obtainValueParametersFromExpectedType(expectedTypeRef.coneTypeSafe(), lambda)
        }
        lambda = buildAnonymousFunctionCopy(lambda) {
            receiverParameter = lambda.receiverParameter?.takeIf { it.typeRef !is FirImplicitTypeRef }
                ?: resolvedLambdaAtom?.receiver?.takeIf {
                    !resolvedLambdaAtom.coerceFirstParameterToExtensionReceiver
                }?.let { coneKotlinType ->
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

        lambda.replaceTypeRef(lambda.constructFunctionTypeRef(session, resolvedLambdaAtom?.expectedFunctionTypeKind))
        session.lookupTracker?.recordTypeResolveAsLookup(lambda.typeRef, lambda.source, context.file.source)
        lambda.addReturnToLastStatementIfNeeded(session)
        return lambda
    }

    private fun FirAnonymousFunction.computeReturnTypeRef(expected: FirResolvedTypeRef?): FirResolvedTypeRef {
        val returnExpressions = dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(this)
        // Any lambda expression assigned to `(...) -> Unit` returns Unit if all return expressions are implicit
        // `lambda@ { return@lambda }` always returns Unit
        if (isLambda && expected?.type?.isUnit == true && returnExpressions.all { !it.isExplicit }) return expected
        if (shouldReturnUnit(returnExpressions.map { it.expression })) return session.builtinTypes.unitType
        // Here is a questionable moment where we could prefer the expected type over an inferred one.
        // In correct code this doesn't matter, as all return expression types should be subtypes of the expected type.
        // In incorrect code, this would change diagnostics: we can get errors either on the entire lambda, or only on its
        // return statements. The former kind of makes more sense, but the latter is more readable.
        val inferredFromReturnExpressions = session.typeContext.commonSuperTypeOrNull(returnExpressions.map { it.expression.resolvedType })
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
                val name = StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME
                val itParam = buildValueParameter {
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
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

            else -> {
                val parameters = if (resolvedLambdaAtom.coerceFirstParameterToExtensionReceiver) {
                    val receiver = resolvedLambdaAtom.receiver ?: error("Coercion to an extension function type, but no receiver found")
                    listOf(receiver) + resolvedLambdaAtom.parameters
                } else {
                    resolvedLambdaAtom.parameters
                }

                obtainValueParametersFromExpectedParameterTypes(parameters, lambda)
            }
        }
    }

    private fun obtainValueParametersFromExpectedType(
        expectedType: ConeKotlinType?,
        lambda: FirAnonymousFunction
    ): List<FirValueParameter> {
        if (expectedType == null) return lambda.valueParameters
        if (!expectedType.isNonReflectFunctionType(session)) return lambda.valueParameters
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
                    param.returnTypeRef.resolvedTypeFromPrototype(
                        expectedTypeParameterTypes[index],
                        param.source?.fakeElement(KtFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter)
                    )
                param.replaceReturnTypeRef(resolvedType)
                param
            }
        }
    }

    override fun transformBackingField(
        backingField: FirBackingField,
        data: ResolutionMode,
    ): FirBackingField = whileAnalysing(session, backingField) {
        val propertyType = data.expectedType
        val initializerData = when {
            backingField.returnTypeRef is FirResolvedTypeRef -> withExpectedType(backingField.returnTypeRef)

            propertyType is FirResolvedTypeRef ->
                ResolutionMode.WithExpectedType(propertyType, shouldBeStrictlyEnforced = false)

            propertyType != null -> ResolutionMode.ContextIndependent

            else -> ResolutionMode.ContextDependent
        }
        backingField.transformInitializer(transformer, initializerData)
        backingField.transformAnnotations(transformer, data)
        if (
            backingField.returnTypeRef is FirErrorTypeRef ||
            backingField.returnTypeRef is FirResolvedTypeRef
        ) {
            return backingField
        }
        val inferredType = if (backingField is FirDefaultPropertyBackingField) {
            propertyType
        } else {
            backingField.initializer?.unwrapSmartcastExpression()?.resolvedType?.toFirResolvedTypeRef()
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
                    unwrappedInitializer.resolvedType.toFirResolvedTypeRef()
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

        override fun transformValueParameter(
            valueParameter: FirValueParameter,
            data: Any?
        ): FirStatement =
            whileAnalysing(valueParameter.moduleData.session, valueParameter) {
                if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
                    valueParameter.replaceReturnTypeRef(
                        valueParameter.returnTypeRef.resolvedTypeFromPrototype(
                            ConeErrorType(
                                ConeSimpleDiagnostic(
                                    "No type for parameter",
                                    DiagnosticKind.ValueParameterWithNoTypeAnnotation
                                )
                            ),
                            fallbackSource = valueParameter.source?.fakeElement(
                                KtFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter,
                            ),
                        )
                    )
                }
                return valueParameter
            }
    }

    private val FirVariable.initializerResolved: Boolean
        get() {
            val initializer = initializer ?: return false
            return initializer.isResolved && initializer !is FirErrorExpression
        }

    protected val FirFunction.bodyResolved: Boolean
        get() = body !is FirLazyBlock && body?.isResolved == true

}
