/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.replSnippetResolveExtensions
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode.ArrayLiteralPosition
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolvedLambdaAtom
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.candidate
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeLocalVariableNoTypeOrInitializer
import org.jetbrains.kotlin.fir.resolve.inference.FirDelegatedPropertyInferenceSession
import org.jetbrains.kotlin.fir.resolve.inference.extractLambdaInfoFromFunctionType
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolver
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.runContractResolveForFunction
import org.jetbrains.kotlin.fir.resolve.transformers.transformVarargTypeToArrayType
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.calls.inference.buildCurrentSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.ProvideDelegateFixationPosition
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

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

        callableMember.contextParameters.forEach {
            it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
        }

        if (callableMember is FirFunction) {
            callableMember.valueParameters.forEach {
                it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
                it.transformVarargTypeToArrayType(transformer.session)
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
        return context.withEnumEntry(enumEntry) {
            (enumEntry.transformChildren(this, data) as FirEnumEntry)
        }
    }

    override fun transformDanglingModifierList(
        danglingModifierList: FirDanglingModifierList,
        data: ResolutionMode
    ): FirDanglingModifierList {
        if (implicitTypeOnly) return danglingModifierList

        @OptIn(PrivateForInline::class)
        context.withContainer(danglingModifierList) {
            danglingModifierList.transformAnnotations(transformer, data)
        }

        return danglingModifierList
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty = whileAnalysing(session, property) {
        require(property !is FirSyntheticProperty) { "Synthetic properties should not be processed by body transformers" }

        // script top level destructuring declaration container variables should be treated as properties here
        // to avoid CFG/DFA complications
        if (property.isLocal && property.origin != FirDeclarationOrigin.Synthetic.ScriptTopLevelDestructuringDeclarationContainer) {
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
        val cannotHaveDeepImplicitTypeRefs = property.backingField?.returnTypeRef !is FirImplicitTypeRef
        if (!property.isConst && implicitTypeOnly && returnTypeRefBeforeResolve !is FirImplicitTypeRef && cannotHaveDeepImplicitTypeRefs) {
            return property
        }

        val shouldResolveEverything = !implicitTypeOnly
        val bodyResolveState = property.bodyResolveState
        return withFullBodyResolve {
            val initializerIsAlreadyResolved = bodyResolveState >= FirPropertyBodyResolveState.INITIALIZER_RESOLVED
            if (!initializerIsAlreadyResolved) {
                dataFlowAnalyzer.enterProperty(property)
            }

            var backingFieldIsAlreadyResolved = false
            context.withProperty(property) {
                // this is required to resolve annotations on properties of local classes
                if (shouldResolveEverything) {
                    property.transformReceiverParameter(transformer, ResolutionMode.ContextIndependent)
                    doTransformTypeParameters(property)
                }

                context.forPropertyInitializer {
                    if (!initializerIsAlreadyResolved) {
                        val resolutionMode = withExpectedType(returnTypeRefBeforeResolve)
                        property.transformReturnTypeRef(transformer, resolutionMode)
                            .transformInitializer(transformer, resolutionMode)
                            .replaceBodyResolveState(FirPropertyBodyResolveState.INITIALIZER_RESOLVED)
                    }

                    if (property.initializer != null) {
                        storeVariableReturnType(property)
                    }

                    val canResolveBackingFieldEarly = property.hasExplicitBackingField || property.returnTypeRef is FirResolvedTypeRef
                    if (!initializerIsAlreadyResolved && canResolveBackingFieldEarly) {
                        property.backingField?.let {
                            transformBackingField(it, withExpectedType(property.returnTypeRef), shouldResolveEverything)
                        }

                        backingFieldIsAlreadyResolved = true
                    }
                }

                // this is required to resolve annotations on properties of local classes
                if (shouldResolveEverything) {
                    property.transformAnnotations(transformer, data)
                    if (initializerIsAlreadyResolved) {
                        property.backingField?.transformAnnotations(transformer, data)
                    }
                }

                val delegate = property.delegate
                if (delegate != null) {
                    if (bodyResolveState == FirPropertyBodyResolveState.ALL_BODIES_RESOLVED) {
                        requireWithAttachment(shouldResolveEverything, { "Invariant is broken" }) {
                            withFirEntry("property", property)
                        }

                        property.resolveAccessors(mayResolveSetterBody = true, shouldResolveEverything = true)
                    } else {
                        transformPropertyAccessorsWithDelegate(property, delegate, shouldResolveEverything)
                        if (property.delegateFieldSymbol != null) {
                            replacePropertyReferenceTypeInDelegateAccessors(property)
                        }

                        property.replaceBodyResolveState(FirPropertyBodyResolveState.ALL_BODIES_RESOLVED)
                    }
                } else {
                    val hasDefaultAccessors =
                        (property.getter == null || property.getter is FirDefaultPropertyAccessor) &&
                                (property.setter == null || property.setter is FirDefaultPropertyAccessor)
                    val mayResolveSetter = shouldResolveEverything || hasDefaultAccessors
                    val propertyTypeRefAfterResolve = property.returnTypeRef
                    val propertyTypeIsKnown = propertyTypeRefAfterResolve is FirResolvedTypeRef
                    val mayResolveGetter = mayResolveSetter || !propertyTypeIsKnown
                    if (mayResolveGetter) {
                        property.resolveAccessors(
                            mayResolveSetterBody = mayResolveSetter,
                            shouldResolveEverything,
                        )
                        property.replaceBodyResolveState(
                            if (mayResolveSetter) FirPropertyBodyResolveState.ALL_BODIES_RESOLVED
                            else FirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED
                        )
                    } else {
                        // Even though we're not going to resolve accessors themselves (so as to avoid resolve cycle, like KT-48634),
                        // we still need to resolve types in accessors (as per IMPLICIT_TYPES_BODY_RESOLVE contract).
                        property.getter?.transformTypeWithPropertyType(propertyTypeRefAfterResolve)
                        property.setter?.transformTypeWithPropertyType(propertyTypeRefAfterResolve)
                        property.setter?.transformReturnTypeRef(transformer, withExpectedType(session.builtinTypes.unitType.coneType))
                    }
                }

                if (!initializerIsAlreadyResolved && !backingFieldIsAlreadyResolved) {
                    property.backingField?.let {
                        transformBackingField(it, withExpectedType(property.returnTypeRef), shouldResolveEverything)
                    }
                }
            }

            if (!initializerIsAlreadyResolved) {
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
            val typeArguments = (type as ConeClassLikeType).typeArguments
            val extensionType = property.receiverParameter?.typeRef?.coneType
            val dispatchType = context.containingRegularClass?.let { containingClass ->
                containingClass.symbol.constructStarProjectedType(containingClass.typeParameters.size)
            }
            propertyReferenceAccess.replaceConeTypeOrNull(
                type.lookupTag.constructClassType(
                    typeArguments.mapIndexed { index, argument ->
                        when (index) {
                            typeArguments.lastIndex -> property.returnTypeRef.coneType
                            0 -> extensionType ?: dispatchType
                            else -> dispatchType
                        } ?: argument
                    }.toTypedArray(),
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
        val delegate = property.delegate
        if (delegate is FirFunctionCall &&
            delegate.calleeReference.name == OperatorNameConventions.PROVIDE_DELEGATE &&
            delegate.source?.kind == KtFakeSourceElementKind.DelegatedPropertyAccessor
        ) {
            delegate.replacePropertyReferenceTypeInDelegateAccessors(property)
        }
    }

    private fun transformPropertyAccessorsWithDelegate(
        property: FirProperty,
        delegateContainer: FirExpression,
        shouldResolveEverything: Boolean,
    ) {
        require(delegateContainer is FirWrappedDelegateExpression)
        dataFlowAnalyzer.enterDelegateExpression()

        // First, resolve delegate expression in dependent context withing existing (possibly Default) inference session
        val delegateExpression =
            // Resolve delegate expression; after that, delegate will contain either expr.provideDelegate or expr
            if (property.isLocal) {
                transformDelegateExpression(delegateContainer)
            } else {
                context.forPropertyInitializer {
                    transformDelegateExpression(delegateContainer)
                }
            }

        // Then, create a root/child inference session based on the resolved delegate expression
        context.withInferenceSession(
            FirDelegatedPropertyInferenceSession(
                resolutionContext,
                callCompleter,
                delegateExpression,
            )
        ) {
            property.replaceDelegate(
                getResolvedProvideDelegateIfSuccessful(delegateContainer.provideDelegateCall, delegateExpression)
                    ?: delegateExpression
            )

            // We don't use inference from setValue calls (i.e., don't resolve setters until the delegate inference is completed)
            // when the property doesn't have an explicit type.
            // It's necessary because we need to supply the property type as the 3rd argument for `setValue` and there might be uninferred
            // variables from `getValue`.
            // The same logic was used at K1 (see org.jetbrains.kotlin.resolve.DelegatedPropertyResolver.inferDelegateTypeFromGetSetValueMethods)
            val isImplicitTypedProperty = property.returnTypeRef is FirImplicitTypeRef
            val currentPropertyTypeRef = when {
                isImplicitTypedProperty -> {
                    property.resolveGetter(shouldResolveEverything)
                    property.getter!!.returnTypeRef
                }

                else -> {
                    property.resolveAccessors(mayResolveSetterBody = true, shouldResolveEverything)
                    property.returnTypeRef
                }
            } as FirResolvedTypeRef

            /**
             * There are two kinds of properties, which may be transformed by this function:
             *  A. top-level/member properties of non-local class ("public" properties)
             *  B. local delegated properties/delegated properties of local classes ("private" properties)
             *
             * Here are some facts for this differences:
             * 1. Property of kind `B` may be written inside scope of some other delegate/PCLA inference (e.g., inside another delegate or
             *    `buildList` lambda), and property of kind `A` are always analyzed in top-level delegate inference session
             * 2. Once written, the resolved return type ref of property of kind `A` can be observed by different threads.
             *    [ReturnTypeCalculatorWithJump] relies on the contract, that if some callable declaration has FirResolvedTypeRef as a return
             *    type, then it is fully resolved, and this type cannot be changed in the future
             * 3. Properties of kind `B` can contain uncompleted type variables in the return type until outer inference session will be
             *    completed, and it's completely valid (see the example below)
             *
             * ```kotlin
             * val x = buildList l@{
             *     val o = object {
             *         val list by lazy { this@l }
             *     }
             *     // at this point type of `o.list` is `MutableList<T>`
             *     println(o.list.size)
             *     add("hello")
             * }
             * ```
             *
             * 4. Resolution to property of kind `B` outside the local scope they were declared (and so access of its type) cannot happen
             *    before containing declaration will be completely resolved and all inference sessions will be completed:
             *    - if the property if just a local variable, it cannot be observed outside of the local scope
             *    - property of local class can be observed outside the local scope in case of private member declaration with implicit type:
             *
             * ```kotlin
             * class Some {
             *     private val x = buildList {
             *         val o = object {
             *             val list by lazy { this@l }
             *         }
             *         add(o)
             *     }
             *
             *     fun test() {
             *         x.first().list
             *     }
             *  }
             *  ```
             *    In this case we indeed can access the type of `object.list`, but for that we need to resolve the type of corresponding `x`
             *    property first, which means, that the PCLA session of `buildList` will be completed and type of `list` won't contain any
             *    type variables. If `x` has explicit return type, then local type won't be accessible, as it is non-denotable
             *
             * Resolutions:
             *   - to satisfy the statement `2` we should never write an uncompleted return type for properties of kind `A`
             *   - to satisfy the statement `3` we can write an uncompleted type for properties of kind `B`
             *
             * To distinguish these two cases, we can use the fact if there any other inference session (PCLA or delegate) or not in the scope.
             *   And write the uncompleted type only if there isn't. With such check we won't write an uncompleted type for some of properties
             *   of kind `B`, but it's fine, as we will complete the property right away and write the completed type immediately
             */
            if (parentSessionIsNonTrivial && isImplicitTypedProperty) {
                /**
                 * Replacement of implicit type ref with resolved type ref effectively means the publication of the property
                 *   for [ReturnTypeCalculatorWithJump]. In the case of multi-thread analysis (e.g., in IDE) it means that if the return
                 *   type was once set, it may be observed by other threads which resolve access to this property. This implies that for
                 *   properties of kind `A` (see comment above) it's needed to write the return type only once
                 */
                property.replaceReturnTypeRef(currentPropertyTypeRef)
            }

            completeSessionOrPostponeIfNonRoot { finalSubstitutor ->
                val typeRef = finalSubstitutor.substituteOrNull(currentPropertyTypeRef.coneType)?.let { substitutedType ->
                    currentPropertyTypeRef.withReplacedConeType(substitutedType)
                } ?: currentPropertyTypeRef

                property.getter?.transformTypeWithPropertyType(typeRef, forceUpdateForNonImplicitTypes = true)
                property.setter?.transformTypeWithPropertyType(typeRef, forceUpdateForNonImplicitTypes = true)

                property.replaceReturnTypeRef(
                    typeRef.approximateDeclarationType(
                        session,
                        property.visibilityForApproximation(),
                        property.isLocal
                    )
                )

                // `isImplicitTypedProperty` means we haven't run setter resolution yet (see its second usage)
                if (isImplicitTypedProperty) {
                    property.resolveSetter(mayResolveSetterBody = true, shouldResolveEverything = shouldResolveEverything)
                }
            }

            dataFlowAnalyzer.exitDelegateExpression(delegateContainer)
        }
    }

    private fun getResolvedProvideDelegateIfSuccessful(
        provideDelegateCall: FirFunctionCall,
        resolvedDelegateExpression: FirExpression,
    ): FirFunctionCall? {
        provideDelegateCall.replaceExplicitReceiver(resolvedDelegateExpression)

        // Resolve call for provideDelegate, without completion
        // TODO: this generates some nodes in the control flow graph which we don't want if we
        //  end up not selecting this option, KT-59684
        transformer.expressionsTransformer?.transformFunctionCallInternal(
            provideDelegateCall, ResolutionMode.ReceiverResolution, FirExpressionsResolveTransformer.CallResolutionMode.PROVIDE_DELEGATE,
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
                substitutor.substituteOrSelf(components.typeFromCallee(provideDelegateCall).coneType)

            provideDelegateCall.replaceConeTypeOrNull(toTypeVariableSubstituted)
            return provideDelegateCall
        }

        val provideDelegateReference = provideDelegateCall.calleeReference
        if (provideDelegateReference is FirResolvedNamedReference && provideDelegateReference !is FirResolvedErrorReference) {
            return provideDelegateCall
        }

        return null
    }

    override fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: ResolutionMode): FirPropertyAccessor {
        return propertyAccessor.also {
            transformProperty(it.propertySymbol.fir, data)
        }
    }

    private fun transformDelegateExpression(delegate: FirWrappedDelegateExpression): FirExpression =
        delegate.expression.transformSingle(transformer, ResolutionMode.Delegate)
            .transformSingle(components.integerLiteralAndOperatorApproximationTransformer, null)

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
            components.typeFromCallee(provideDelegate).coneType
                // Substitut type parameter to type variable
                .let(candidate.substitutor::substituteOrSelf)
                .unwrapTopLevelVariableType() ?: return null
        val typeVariable = returnTypeBasedOnVariable.typeConstructor

        val candidateSystem = candidate.system
        val candidateStorage = candidateSystem.currentStorage()
        val variableWithConstraints =
            candidateSystem.notFixedTypeVariables[typeVariable] ?: error("Not found type variable $typeVariable")

        var resultType: ConeKotlinType? = null

        // Temporary declare all the "outer" variables as proper (i.e., all inner variables as improper)
        // Without that, all variables (both inner and outer ones) would be considered as improper,
        // while we want to fix to assume `Delegate<Tv>` as proper because `Tv` belongs to the outer system
        candidateSystem.withTypeVariablesThatAreCountedAsProperTypes(candidateSystem.outerTypeVariables.orEmpty()) {
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
            ) as? ConeKotlinType ?: return@withTypeVariablesThatAreCountedAsProperTypes


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
            transformPropertyAccessorsWithDelegate(variable, delegate, shouldResolveEverything = true)
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
            variable.resolveAccessors(mayResolveSetterBody = true)
        }

        // We need this return type transformation to resolve annotations from an implicit type
        variable.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
            .transformOtherChildren(transformer, ResolutionMode.ContextIndependent)

        context.storeVariable(variable, session)
        if (variable.origin != FirDeclarationOrigin.ScriptCustomization.Parameter &&
            variable.origin != FirDeclarationOrigin.ScriptCustomization.ParameterFromBaseClass)
        {
            // script parameters should not be added to CFG to avoid graph building compilations
            dataFlowAnalyzer.exitLocalVariableDeclaration(variable, hadExplicitType)
        }
        return variable
    }

    /**
     * Note that this function updates the return type of the property using type from setter, if the property itself had
     *   an implicit return type
     *
     * In IDE there's a need to resolve setter's parameter types on the implicit-resolution stage
     *   See ad183434137939a0c9eeea2f7df9ef522672a18e commit.
     *   But for delegate inference case, we don't need both body of the setter and its parameter resolved (SKIP mode)
     */
    private fun FirProperty.resolveAccessors(
        mayResolveSetterBody: Boolean,
        shouldResolveEverything: Boolean = true,
    ) {
        resolveGetter(shouldResolveEverything)

        if (returnTypeRef is FirImplicitTypeRef) {
            storeVariableReturnType(this) // Here, we expect `this.returnTypeRef` is updated from the getter's return type
            // We need update type of getter for case when its type was approximated
            getter?.transformTypeWithPropertyType(returnTypeRef, forceUpdateForNonImplicitTypes = true)
        }

        resolveSetter(mayResolveSetterBody, shouldResolveEverything)
    }

    private fun FirProperty.resolveGetter(shouldResolveEverything: Boolean) {
        getter?.let { transformAccessor(it, this, shouldResolveEverything) }
    }

    private fun ConeKotlinType.unwrapTopLevelVariableType(): ConeTypeVariableType? = when {
        this is ConeTypeVariableType -> this
        this is ConeFlexibleType -> lowerBound.unwrapTopLevelVariableType()
        this is ConeDefinitelyNotNullType -> original.unwrapTopLevelVariableType()
        else -> null
    }

    private fun FirProperty.resolveSetter(
        mayResolveSetterBody: Boolean,
        shouldResolveEverything: Boolean,
    ) {
        setter?.let {
            it.transformTypeWithPropertyType(returnTypeRef)

            if (mayResolveSetterBody) {
                transformAccessor(it, this, shouldResolveEverything)
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
        owner: FirProperty,
        shouldResolveEverything: Boolean,
    ): Unit = whileAnalysing(session, accessor) {
        context.withPropertyAccessor(owner, accessor, components) {
            val propertyTypeRef = owner.returnTypeRef

            // Currently, this condition might only be true for delegates, because if type is set explicitly for the property,
            // it's been propagated to receivers in the RawFirBuilder
            if (accessor.returnTypeRef is FirImplicitTypeRef && propertyTypeRef !is FirImplicitTypeRef) {
                accessor.replaceReturnTypeRef(propertyTypeRef)
            }

            if (accessor is FirDefaultPropertyAccessor || accessor.body == null) {
                transformFunction(accessor, ResolutionMode.ContextIndependent, shouldResolveEverything)
            } else {
                transformFunctionWithGivenSignature(accessor, shouldResolveEverything)
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
        data: ResolutionMode,
    ): FirRegularClass = whileAnalysing(session, regularClass) {
        context.withContainingClass(regularClass) {
            val isLocal = regularClass.isLocal
            if (isLocal && regularClass !in context.targetedLocalClasses) {
                return regularClass.runAllPhasesForLocalClass(components, data)
            }

            if (isLocal || !implicitTypeOnly) {
                context.withClassHeader(regularClass) {
                    regularClass.transformAnnotations(this, ResolutionMode.ContextIndependent)
                    regularClass.transformTypeParameters(this, ResolutionMode.ContextIndependent)
                    regularClass.transformSuperTypeRefs(this, ResolutionMode.ContextIndependent)
                }
            }

            doTransformRegularClass(regularClass, data)
        }
    }

    open fun withScript(script: FirScript, action: () -> FirScript): FirScript {
        val result = context.withScript(script, components) {
            // see todo in withFile
            dataFlowAnalyzer.enterScript(script, buildGraph = transformer.buildCfgForScripts)
            action()
        }
        val controlFlowGraph = dataFlowAnalyzer.exitScript()
        if (controlFlowGraph != null) {
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(controlFlowGraph))
        }
        return result
    }

    override fun transformScript(script: FirScript, data: ResolutionMode): FirScript = whileAnalysing(session, script) {
        withScript(script) {
            transformDeclarationContent(script, data) as FirScript
        }
    }

    override fun transformReplSnippet(replSnippet: FirReplSnippet, data: ResolutionMode): FirReplSnippet {
        if (!implicitTypeOnly) {
            context.withReplSnippet(replSnippet, components) {
                dataFlowAnalyzer.enterReplSnippet(replSnippet, buildGraph = true)
                replSnippet.transformBody(this, data)
                val returnType = replSnippet.body.statements.lastOrNull()?.let {
                    (it as? FirExpression)?.resolvedType
                } ?:session.builtinTypes.unitType.coneType
                replSnippet.replaceResultTypeRef(
                    returnType.toFirResolvedTypeRef(replSnippet.source?.fakeElement(KtFakeSourceElementKind.ImplicitFunctionReturnType))
                )
                for (resolveExt in session.extensionService.replSnippetResolveExtensions) {
                    resolveExt.updateResolved(replSnippet)
                }
                dataFlowAnalyzer.exitReplSnippet()
            }
        }
        return replSnippet
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
        if (implicitTypeOnly) return typeAlias
        if (typeAlias.isLocal && typeAlias !in context.targetedLocalClasses) {
            return typeAlias.runAllPhasesForLocalClass(components, data)
        }

        @OptIn(PrivateForInline::class)
        context.withContainer(typeAlias) {
            doTransformTypeParameters(typeAlias)
            typeAlias.transformAnnotations(transformer, data)
            typeAlias.transformExpandedTypeRef(transformer, data)
        }

        return typeAlias
    }

    private fun doTransformFile(
        file: FirFile,
        data: ResolutionMode,
    ): FirFile = withFile(file) {
        transformDeclarationContent(file, data) as FirFile
    }

    open fun withFile(
        file: FirFile,
        action: () -> FirFile,
    ): FirFile {
        val result = context.withFile(file, components) {
            // TODO Must be done within 'withFile' as the context - any the analyzer - is cleared as the first step.
            //  yuk. maybe the clear shouldn't happen for `enterFile`? or at maybe separately?
            // also check whether it is applicable in withScript`
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
        context.withContainingClass(anonymousObject) {
            if (anonymousObject !in context.targetedLocalClasses) {
                return anonymousObject.runAllPhasesForLocalClass(components, data)
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
            result
        }
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): FirSimpleFunction = whileAnalysing(session, simpleFunction) {
        val shouldResolveEverything = !implicitTypeOnly
        val returnTypeRef = simpleFunction.returnTypeRef
        if ((returnTypeRef !is FirImplicitTypeRef) && implicitTypeOnly) {
            return simpleFunction
        }

        val containingDeclaration = context.containerIfAny
        return context.withSimpleFunction(simpleFunction, session) {
            // this is required to resolve annotations on functions of local classes
            if (shouldResolveEverything) {
                simpleFunction.transformReceiverParameter(this, data)
                doTransformTypeParameters(simpleFunction)
            }

            if (containingDeclaration != null && containingDeclaration !is FirClass && containingDeclaration !is FirFile && (containingDeclaration !is FirScript || simpleFunction.isLocal)) {
                // For class members everything should be already prepared
                prepareSignatureForBodyResolve(simpleFunction)
                simpleFunction.transformStatus(this, simpleFunction.resolveStatus().mode())

                if (simpleFunction.contractDescription != null) {
                    simpleFunction.runContractResolveForFunction(session, scopeSession, context)
                }
            }

            context.forFunctionBody(simpleFunction, components) {
                withFullBodyResolve {
                    transformFunctionWithGivenSignature(simpleFunction, shouldResolveEverything = shouldResolveEverything)
                }
            }
        }
    }

    private fun <F : FirFunction> transformFunctionWithGivenSignature(function: F, shouldResolveEverything: Boolean): F {
        @Suppress("UNCHECKED_CAST")
        val result = transformFunction(function, ResolutionMode.ContextIndependent, shouldResolveEverything) as F

        val body = result.body
        if (result.returnTypeRef is FirImplicitTypeRef) {
            val simpleFunction = function as? FirSimpleFunction
            val returnExpression = (body?.statements?.singleOrNull() as? FirReturnExpression)?.result
            val expressionType = returnExpression?.resolvedType
            val newSource = result.returnTypeRef.source ?: returnExpression?.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
            val returnTypeRef = expressionType
                ?.toFirResolvedTypeRef(newSource)
                ?.approximateDeclarationType(
                    session,
                    simpleFunction?.visibilityForApproximation(),
                    isLocal = simpleFunction?.isLocal == true,
                    isInlineFunction = simpleFunction?.isInline == true
                )
                ?: buildErrorTypeRef {
                    source = newSource
                    diagnostic = ConeSimpleDiagnostic("empty body", DiagnosticKind.Other)
                }
            result.transformReturnTypeRef(transformer, withExpectedType(returnTypeRef))
        }

        return result
    }

    override fun transformFunction(
        function: FirFunction,
        data: ResolutionMode
    ): FirFunction {
        if (function.bodyResolved) return function

        return transformFunction(function, data, shouldResolveEverything = true)
    }

    private fun transformFunction(
        function: FirFunction,
        data: ResolutionMode,
        shouldResolveEverything: Boolean,
    ): FirFunction = whileAnalysing(session, function) {
        val bodyResolved = function.bodyResolved
        dataFlowAnalyzer.enterFunction(function)

        if (shouldResolveEverything) {
            // Annotations here are required only in the case of a local class member function.
            // Separate annotation transformers are responsible in the case of non-local functions.
            function
                .transformReturnTypeRef(this, data)
                .transformContextParameters(this, data)
                .transformValueParameters(this, data)
                .transformAnnotations(this, data)
        }

        if (!bodyResolved) {
            function.transformBody(this, data)
        }

        if (shouldResolveEverything && function is FirContractDescriptionOwner) {
            function.transformContractDescription(this, data)
        }

        val controlFlowGraphReference = dataFlowAnalyzer.exitFunction(function)
        if (!bodyResolved) {
            function.replaceControlFlowGraphReference(controlFlowGraphReference)
        }

        return function
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

    override fun transformReceiverParameter(
        receiverParameter: FirReceiverParameter,
        data: ResolutionMode,
    ): FirReceiverParameter = whileAnalysing(session, receiverParameter) {
        context.withReceiverParameter(receiverParameter) {
            transformDeclarationContent(receiverParameter, data) as FirReceiverParameter
        }
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: ResolutionMode
    ): FirValueParameter = whileAnalysing(session, valueParameter) {
        dataFlowAnalyzer.enterValueParameter(valueParameter)
        val insideAnnotationConstructorDeclaration =
            (valueParameter.containingDeclarationSymbol as? FirConstructorSymbol)?.resolvedReturnType?.toClassSymbol(session)?.classKind == ClassKind.ANNOTATION_CLASS
        val result = context.withValueParameter(valueParameter, session) {
            transformDeclarationContent(
                valueParameter,
                withExpectedType(
                    valueParameter.returnTypeRef,
                    arrayLiteralPosition = if (insideAnnotationConstructorDeclaration) ArrayLiteralPosition.AnnotationParameter else null
                )
            ) as FirValueParameter
        }

        dataFlowAnalyzer.exitValueParameter(result)?.let { graph ->
            result.replaceControlFlowGraphReference(FirControlFlowGraphReferenceImpl(graph))
        }

        return result
    }

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, anonymousFunctionExpression) {
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        anonymousFunction.transformAnnotations(transformer, ResolutionMode.ContextIndependent)

        anonymousFunction.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
        anonymousFunction.transformReceiverParameter(transformer, ResolutionMode.ContextIndependent)
        anonymousFunction.contextParameters.forEach { it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent) }
        anonymousFunction.valueParameters.forEach { it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent) }

        if (anonymousFunction.contractDescription != null) {
            anonymousFunction.runContractResolveForFunction(session, scopeSession, context)
        }

        return when (data) {
            is ResolutionMode.ContextDependent -> {
                dataFlowAnalyzer.enterAnonymousFunctionExpression(anonymousFunctionExpression)
                context.storeContextForAnonymousFunction(anonymousFunction)
                anonymousFunctionExpression // return the same instance
            }
            is ResolutionMode.WithExpectedType -> {
                transformTopLevelAnonymousFunctionExpression(anonymousFunctionExpression, data.expectedType)
            }


            is ResolutionMode.ContextIndependent,
            is ResolutionMode.AssignmentLValue,
            is ResolutionMode.ReceiverResolution,
            is ResolutionMode.Delegate,
                -> transformTopLevelAnonymousFunctionExpression(anonymousFunctionExpression, null)
            is ResolutionMode.WithStatus -> error("Should not be here in WithStatus mode")
        }
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ResolutionMode
    ): FirAnonymousFunction = whileAnalysing(session, anonymousFunction) {
        error("Transformation of anonymous function should be performed via `transformAnonymousFunctionExpression`")
    }

    /**
     * For lambdas, which are not a part of a value argument list of some call, like
     * `val x: () -> Unit = {}` or `{}.invoke()`
     */
    private fun transformTopLevelAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        expectedType: ConeKotlinType?,
    ): FirStatement = anonymousFunctionExpression.also {
        it.replaceAnonymousFunction(transformTopLevelAnonymousFunction(anonymousFunctionExpression, expectedType))
    }

    private fun transformTopLevelAnonymousFunction(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        expectedType: ConeKotlinType?
    ): FirAnonymousFunction {
        dataFlowAnalyzer.enterAnonymousFunctionExpression(anonymousFunctionExpression)
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        val resolvedLambdaAtom = expectedType?.let {
            extractLambdaInfoFromFunctionType(
                it,
                anonymousFunctionExpression,
                anonymousFunction,
                returnTypeVariable = null,
                components,
                allowCoercionToExtensionReceiver = true,
                sourceForFunctionExpression = null,
            )
        }
        var lambda = anonymousFunction
        val valueParameters = when {
            resolvedLambdaAtom != null -> obtainValueParametersFromResolvedLambdaAtom(resolvedLambdaAtom, lambda)
            else -> obtainValueParametersFromExpectedType(expectedType, lambda)
        }

        lambda.replaceReceiverParameter(
            lambda.receiverParameter?.takeIf { it.typeRef !is FirImplicitTypeRef }
                ?: resolvedLambdaAtom?.receiverType?.takeIf {
                    !resolvedLambdaAtom.coerceFirstParameterToExtensionReceiver
                }?.let { coneKotlinType ->
                    lambda.receiverParameter?.apply {
                        replaceTypeRef(typeRef.resolvedTypeFromPrototype(coneKotlinType))
                    }
                })

        lambda.replaceContextParameters(
            lambda.contextParameters.takeIf { it.isNotEmpty() }
                ?: resolvedLambdaAtom?.contextParameterTypes?.map { receiverType ->
                    buildValueParameter {
                        resolvePhase = FirResolvePhase.BODY_RESOLVE
                        source = lambda.source?.fakeElement(KtFakeSourceElementKind.LambdaContextParameter)
                        containingDeclarationSymbol = lambda.symbol
                        moduleData = session.moduleData
                        origin = FirDeclarationOrigin.Source
                        name = SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
                        symbol = FirValueParameterSymbol(name)
                        returnTypeRef = receiverType
                            .toFirResolvedTypeRef(lambda.source?.fakeElement(KtFakeSourceElementKind.LambdaContextParameter))
                        valueParameterKind = if (session.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters)) {
                            FirValueParameterKind.ContextParameter
                        } else {
                            FirValueParameterKind.LegacyContextReceiver
                        }
                    }
                }.orEmpty()
        )

        lambda.replaceValueParameters(valueParameters)

        lambda = lambda.transformValueParameters(ImplicitToErrorTypeTransformer, null)
        lambda = lambda.transformContextParameters(ImplicitToErrorTypeTransformer, null)

        val initialReturnTypeRef = lambda.returnTypeRef as? FirResolvedTypeRef
        val expectedReturnTypeRef = initialReturnTypeRef
            ?: resolvedLambdaAtom?.returnType?.let { lambda.returnTypeRef.resolvedTypeFromPrototype(it) }
        lambda = transformAnonymousFunctionBody(lambda, expectedReturnTypeRef ?: components.noExpectedType)

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
        val returnType = computeReturnType(
            session,
            expected?.coneType,
            isPassedAsFunctionArgument = false,
            dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(this),
        )
        return returnTypeRef.resolvedTypeFromPrototype(
            returnType,
            fallbackSource = source.takeIf {
                // This is a specific case when we deliberately lose the source to prevent double-reporting the diagnostic
                // It will be anyway reported on a value parameter
                returnType !is ConeErrorType ||
                        (returnType.diagnostic as? ConeSimpleDiagnostic)?.kind != DiagnosticKind.ValueParameterWithNoTypeAnnotation
            }?.fakeElement(KtFakeSourceElementKind.ImplicitFunctionReturnType)
        )
    }

    private fun obtainValueParametersFromResolvedLambdaAtom(
        resolvedLambdaAtom: ConeResolvedLambdaAtom,
        lambda: FirAnonymousFunction,
    ): List<FirValueParameter> {
        val singleParameterType = resolvedLambdaAtom.parameterTypes.singleOrNull()
        return when {
            lambda.valueParameters.isEmpty() && singleParameterType != null -> {
                val name = StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME
                val itParam = buildValueParameter {
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    source = lambda.source?.fakeElement(KtFakeSourceElementKind.ItLambdaParameter)
                    containingDeclarationSymbol = resolvedLambdaAtom.anonymousFunction.symbol
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
                    val receiver = resolvedLambdaAtom.receiverType ?: error("Coercion to an extension function type, but no receiver found")
                    listOf(receiver) + resolvedLambdaAtom.parameterTypes
                } else {
                    resolvedLambdaAtom.parameterTypes
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
            .mapTo(mutableListOf()) { it.type ?: session.builtinTypes.nullableAnyType.coneType }
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

    internal fun doTransformAnonymousFunctionBodyFromCallCompletion(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        // When completer knows which return type is expected.
        // Otherwise, return expressions are resolved in ContextDependent mode.
        expectedReturnTypeFromCallPosition: FirResolvedTypeRef?,
    ) {
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        val expectedReturnTypeRef =
            expectedReturnTypeFromCallPosition
            // For the case of `fun (): ReturnType = ...`
                ?: anonymousFunction.returnTypeRef.takeUnless { it is FirImplicitTypeRef }

        if (expectedReturnTypeFromCallPosition == null) {
            context.withLambdaBeingAnalyzedInDependentContext(anonymousFunction.symbol) {
                transformAnonymousFunctionBody(anonymousFunction, expectedReturnTypeRef)
            }
        } else {
            transformAnonymousFunctionBody(anonymousFunction, expectedReturnTypeRef)
        }
    }

    private fun transformAnonymousFunctionBody(
        anonymousFunction: FirAnonymousFunction,
        expectedReturnTypeRef: FirTypeRef?
    ): FirAnonymousFunction {
        // `transformFunction` will replace both `typeRef` and `returnTypeRef`, so make sure to keep the former.
        val lambdaType = anonymousFunction.typeRef
        return context.withAnonymousFunction(anonymousFunction, components) {
            doTransformTypeParameters(anonymousFunction)
            withFullBodyResolve {
                transformFunction(
                    anonymousFunction,
                    expectedReturnTypeRef?.let(::withExpectedType) ?: ResolutionMode.ContextDependent
                ) as FirAnonymousFunction
            }
        }.apply { replaceTypeRef(lambdaType) }
    }

    override fun transformBackingField(
        backingField: FirBackingField,
        data: ResolutionMode,
    ): FirBackingField = transformBackingField(backingField, data, shouldResolveEverything = true)

    private fun transformBackingField(
        backingField: FirBackingField,
        data: ResolutionMode,
        shouldResolveEverything: Boolean,
    ): FirBackingField = whileAnalysing(session, backingField) {
        val initializerData = when {
            backingField.returnTypeRef is FirResolvedTypeRef -> withExpectedType(backingField.returnTypeRef)
            data is ResolutionMode.WithExpectedType -> data.copy(shouldBeStrictlyEnforced = false)
            data is ResolutionMode.ContextIndependent -> ResolutionMode.ContextIndependent
            else -> ResolutionMode.ContextDependent
        }
        backingField.transformInitializer(transformer, initializerData)
        if (shouldResolveEverything) {
            backingField.transformAnnotations(transformer, data)
        }

        if (
            backingField.returnTypeRef is FirErrorTypeRef ||
            backingField.returnTypeRef is FirResolvedTypeRef
        ) {
            return backingField
        }

        @OptIn(ResolutionMode.WithExpectedType.ExpectedTypeRefAccess::class)
        val inferredType = if (backingField is FirDefaultPropertyBackingField) {
            (data as? ResolutionMode.WithExpectedType)?.expectedTypeRef
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
                    unwrappedInitializer.resolvedType.toFirResolvedTypeRef(
                        unwrappedInitializer.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
                    )
                }
                variable.getter?.body is FirSingleExpressionBlock -> variable.getter?.returnTypeRef
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
                annotations.addAll(this@toExpectedTypeRef.annotations)
            }
            is FirErrorTypeRef -> buildErrorTypeRef {
                diagnostic = this@toExpectedTypeRef.diagnostic
                source = this@toExpectedTypeRef.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
                annotations.addAll(this@toExpectedTypeRef.annotations)
            }
            else -> {
                buildResolvedTypeRef {
                    coneType = this@toExpectedTypeRef.coneType
                    source = this@toExpectedTypeRef.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
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

    private val FirFunction.bodyResolved: Boolean
        get() = body?.isResolved == true
}
