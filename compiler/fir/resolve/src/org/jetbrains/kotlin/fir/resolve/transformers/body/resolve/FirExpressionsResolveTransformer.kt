/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.FirOperation.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.toAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildErrorSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildExplicitSuperReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode.ArrayLiteralPosition
import org.jetbrains.kotlin.fir.resolve.ResolutionMode.ContextIndependent
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolutionAtom
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitDispatchReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.InaccessibleImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ResolvedCallArgument.VarargArgument
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.candidate
import org.jetbrains.kotlin.fir.resolve.calls.findTypesForSuperCandidates
import org.jetbrains.kotlin.fir.resolve.calls.stages.mapArguments
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.replaceLambdaArgumentInvocationKinds
import org.jetbrains.kotlin.fir.resolve.transformers.unwrapAtoms
import org.jetbrains.kotlin.fir.scopes.impl.isWrappedIntegerOperator
import org.jetbrains.kotlin.fir.scopes.impl.isWrappedIntegerOperatorForUnsignedType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.tower.ApplicabilityDetail
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

open class FirExpressionsResolveTransformer(transformer: FirAbstractBodyResolveTransformerDispatcher) :
    FirPartialBodyResolveTransformer(transformer) {
    private inline val builtinTypes: BuiltinTypes get() = session.builtinTypes
    private val arrayOfCallTransformer = FirArrayOfCallTransformer()
    var enableArrayOfCallTransformation: Boolean = false
    var containingSafeCallExpression: FirSafeCallExpression? = null

    private val assignAltererExtensions = session.extensionService.assignAltererExtensions.takeIf { it.isNotEmpty() }
    @OptIn(FirExtensionApiInternals::class)
    private val callRefinementExtensions = session.extensionService.callRefinementExtensions.takeIf { it.isNotEmpty() }

    init {
        @Suppress("LeakingThis")
        components.callResolver.initTransformer(this)
    }

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): FirStatement {
        if (!expression.isResolved && expression !is FirWrappedExpression) {
            expression.resultType = ConeErrorType(
                ConeSimpleDiagnostic(
                    "Type calculating for ${expression::class} is not supported",
                    DiagnosticKind.InferenceError
                )
            )
        }
        return (expression.transformChildren(transformer, data) as FirStatement)
    }

    override fun transformSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: ResolutionMode): FirStatement {
        return smartCastExpression
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ResolutionMode,
    ): FirStatement = whileAnalysing(session, qualifiedAccessExpression) {
        transformQualifiedAccessExpression(qualifiedAccessExpression, data, isUsedAsReceiver = false, isUsedAsGetClassReceiver = false)
    }

    private fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ResolutionMode,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
    ): FirExpression {
        if (qualifiedAccessExpression.isResolved && qualifiedAccessExpression.calleeReference !is FirSimpleNamedReference) {
            return qualifiedAccessExpression
        }

        qualifiedAccessExpression.transformAnnotations(this, data)
        qualifiedAccessExpression.transformTypeArguments(transformer, ResolutionMode.ContextIndependent)

        val result = when (val callee = qualifiedAccessExpression.calleeReference) {
            is FirThisReference -> {
                val labelName = callee.labelName
                val allMatchingImplicitReceivers = implicitValueStorage[labelName]
                val implicitReceiver = allMatchingImplicitReceivers.singleWithoutDuplicatingContextReceiversOrNull() ?: run {
                    val diagnostic = allMatchingImplicitReceivers.ambiguityDiagnosticFor(labelName)
                    qualifiedAccessExpression.resultType = ConeErrorType(diagnostic)
                    callee.replaceDiagnostic(diagnostic)

                    if (diagnostic.kind == DiagnosticKind.AmbiguousLabel) {
                        return qualifiedAccessExpression
                    } else {
                        allMatchingImplicitReceivers.lastOrNull()
                    }
                }
                implicitReceiver?.let {
                    callee.replaceBoundSymbol(it.boundSymbol)
                }
                val implicitType = implicitReceiver?.originalType
                val resultType: ConeKotlinType = when {
                    implicitReceiver is InaccessibleImplicitReceiverValue -> ConeErrorType(ConeInstanceAccessBeforeSuperCall("<this>"))
                    implicitType != null -> implicitType
                    labelName != null -> ConeErrorType(ConeSimpleDiagnostic("Unresolved this@$labelName", DiagnosticKind.UnresolvedLabel))
                    else -> ConeErrorType(ConeSimpleDiagnostic("'this' is not defined in this context", DiagnosticKind.NoThis))
                }
                (resultType as? ConeErrorType)?.diagnostic?.let {
                    callee.replaceDiagnostic(it)
                }

                // For situations like:
                // buildList {
                //    val myList: MutableList<String> = this
                // }
                // We need to add a constraint `MutableList<Ev> <: MutableList<String>`
                data.expectedType?.let { expectedType ->
                    context.inferenceSession.addSubtypeConstraintIfCompatible(
                        lowerType = resultType,
                        upperType = expectedType,
                        qualifiedAccessExpression,
                    )
                }

                qualifiedAccessExpression.resultType = resultType
                qualifiedAccessExpression
            }
            is FirSuperReference -> {
                // NB: Regular case with `super.foo()` is handled at `transformExplicitReceiverOf`
                // Here, it's likely an erroneous `super` in the air
                transformSuperReceiver(
                    qualifiedAccessExpression as FirSuperReceiverExpression,
                    containingSafeCallExpression?.takeIf { qualifiedAccessExpression == it.receiver }?.selector as? FirQualifiedAccessExpression
                )
            }
            is FirDelegateFieldReference -> {
                val delegateFieldSymbol = callee.resolvedSymbol
                qualifiedAccessExpression.resultType = delegateFieldSymbol.fir.delegate!!.resolvedType
                qualifiedAccessExpression
            }
            is FirResolvedNamedReference,
            is FirErrorNamedReference -> {
                if (!qualifiedAccessExpression.isResolved) {
                    storeTypeFromCallee(qualifiedAccessExpression, isLhsOfAssignment = false)
                }
                qualifiedAccessExpression
            }
            else -> {
                val transformedCallee = resolveQualifiedAccessAndSelectCandidate(
                    qualifiedAccessExpression,
                    isUsedAsReceiver,
                    isUsedAsGetClassReceiver,
                    // Set the correct call site. For assignment LHSs, it's the assignment, otherwise it's the qualified access itself.
                    callSite = when (data) {
                        is ResolutionMode.AssignmentLValue -> data.variableAssignment
                        else -> qualifiedAccessExpression
                    },
                    data,
                ).let {
                    runContextSensitiveResolutionIfNeeded(it, data) ?: it
                }

                fun FirExpression.alsoRecordLookup() = also {
                    if (transformedCallee.isResolved) {
                        session.lookupTracker?.recordTypeResolveAsLookup(
                            transformedCallee.resolvedType, callee.source, components.file.source
                        )
                    }
                }

                // NB: here we can get raw expression because of dropped qualifiers (see transform callee),
                // so candidate existence must be checked before calling completion
                if (transformedCallee is FirQualifiedAccessExpression && transformedCallee.candidate() != null) {
                    if (!transformedCallee.isAcceptableResolvedQualifiedAccess()) {
                        return qualifiedAccessExpression.alsoRecordLookup()
                    }
                    callCompleter.completeCall(transformedCallee, data)
                } else {
                    transformedCallee
                }.alsoRecordLookup()
            }
        }

        // If we're resolving the LHS of an assignment, skip DFA to prevent the access being treated as a variable read and
        // smart-casts being applied.
        if (data !is ResolutionMode.AssignmentLValue) {
            return transformExpressionUsingSmartcastInfo(result)
        }
        return result
    }

    private fun transformExpressionUsingSmartcastInfo(original: FirExpression): FirExpression {
        when (val expression = original.unwrapDesugaredAssignmentValueRef()) {
            is FirQualifiedAccessExpression -> dataFlowAnalyzer.exitQualifiedAccessExpression(expression)
            is FirResolvedQualifier -> dataFlowAnalyzer.exitResolvedQualifierNode(expression)
            else -> return original
        }
        val result = components.transformExpressionUsingSmartcastInfo(original)
        if (result is FirSmartCastExpression) {
            dataFlowAnalyzer.exitSmartCastExpression(result)
        }
        return result
    }

    private fun runContextSensitiveResolutionIfNeeded(
        originalExpression: FirExpression,
        data: ResolutionMode,
    ): FirExpression? {
        if (originalExpression !is FirPropertyAccessExpression) return null
        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.ContextSensitiveResolutionUsingExpectedType)) return null

        val expectedType =
            (data as? ResolutionMode.WithExpectedType)?.hintForContextSensitiveResolution ?: data.expectedType ?: return null

        if (!originalExpression.shouldBeResolvedInContextSensitiveMode()) return null

        return components.runContextSensitiveResolutionForPropertyAccess(originalExpression, expectedType)
    }

    override fun transformQualifiedErrorAccessExpression(qualifiedErrorAccessExpression: FirQualifiedErrorAccessExpression, data: ResolutionMode): FirStatement {
        qualifiedErrorAccessExpression.transformAnnotations(this, data)
        qualifiedErrorAccessExpression.transformSelector(this, data)
        qualifiedErrorAccessExpression.replaceReceiver(
            qualifiedErrorAccessExpression.receiver.transformAsExplicitReceiver(
                ResolutionMode.ReceiverResolution,
                isUsedAsGetClassReceiver = false
            )
        )
        return qualifiedErrorAccessExpression
    }

    fun <Q : FirQualifiedAccessExpression> transformExplicitReceiverOf(qualifiedAccessExpression: Q): Q {
        val explicitReceiver = qualifiedAccessExpression.explicitReceiver
        if (explicitReceiver is FirSuperReceiverExpression) {
            transformSuperReceiver(explicitReceiver, qualifiedAccessExpression)
            return qualifiedAccessExpression
        }

        if (explicitReceiver != null) {
            qualifiedAccessExpression.replaceExplicitReceiver(
                explicitReceiver.transformAsExplicitReceiver(ResolutionMode.ReceiverResolution, isUsedAsGetClassReceiver = false)
            )
        }

        return qualifiedAccessExpression
    }

    private fun FirExpression.transformAsExplicitReceiver(
        resolutionMode: ResolutionMode,
        isUsedAsGetClassReceiver: Boolean,
    ): FirExpression {
        return when (this) {
            is FirPropertyAccessExpression -> transformQualifiedAccessExpression(
                this, resolutionMode, isUsedAsReceiver = true, isUsedAsGetClassReceiver = isUsedAsGetClassReceiver
            )
            else -> transformSingle(this@FirExpressionsResolveTransformer, resolutionMode)
        }
    }

    override fun transformPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: ResolutionMode
    ): FirStatement {
        return transformQualifiedAccessExpression(propertyAccessExpression, data)
    }

    protected open fun resolveQualifiedAccessAndSelectCandidate(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
        callSite: FirElement,
        data: ResolutionMode,
    ): FirExpression {
        return callResolver.resolveVariableAccessAndSelectCandidate(
            qualifiedAccessExpression, isUsedAsReceiver, isUsedAsGetClassReceiver, callSite, data
        )
    }

    fun transformSuperReceiver(
        superReferenceContainer: FirSuperReceiverExpression,
        containingCall: FirQualifiedAccessExpression?
    ): FirQualifiedAccessExpression {
        val superReference = superReferenceContainer.calleeReference
        val labelName = superReference.labelName
        val lastDispatchReceiver = implicitValueStorage.lastDispatchReceiver()
        val implicitReceiver =
            // Only report label issues if the label is set and the receiver stack is not empty
            if (labelName != null && lastDispatchReceiver != null) {
                val possibleImplicitReceivers = implicitValueStorage[labelName]
                val labeledReceiver = possibleImplicitReceivers.singleOrNull() as? ImplicitDispatchReceiverValue
                labeledReceiver ?: run {
                    val diagnostic = if (possibleImplicitReceivers.size >= 2) {
                        ConeSimpleDiagnostic("Ambiguous label", DiagnosticKind.AmbiguousLabel)
                    } else {
                        ConeSimpleDiagnostic("Unresolved label", DiagnosticKind.UnresolvedLabel)
                    }
                    return markSuperReferenceError(diagnostic, superReferenceContainer, superReference)
                }
            } else {
                lastDispatchReceiver
            }
        implicitReceiver?.receiverExpression?.let {
            superReferenceContainer.replaceDispatchReceiver(it)
        }
        val superTypeRefs = implicitReceiver?.boundSymbol?.fir?.superTypeRefs
        val superTypeRef = superReference.superTypeRef
        when {
            containingCall == null -> {
                val superNotAllowedDiagnostic = ConeSimpleDiagnostic("Super not allowed", DiagnosticKind.SuperNotAllowed)
                return markSuperReferenceError(superNotAllowedDiagnostic, superReferenceContainer, superReference)
            }
            implicitReceiver == null || superTypeRefs == null || superTypeRefs.isEmpty() -> {
                val diagnostic =
                    if (implicitValueStorage.implicitReceivers.lastOrNull() is InaccessibleImplicitReceiverValue) {
                        ConeInstanceAccessBeforeSuperCall("<super>")
                    } else {
                        ConeSimpleDiagnostic("Super not available", DiagnosticKind.SuperNotAvailable)
                    }
                return markSuperReferenceError(diagnostic, superReferenceContainer, superReference)
            }
            superTypeRef is FirResolvedTypeRef -> {
                superReferenceContainer.resultType = superTypeRef.coneType
            }
            superTypeRef !is FirImplicitTypeRef -> {
                components.typeResolverTransformer.withBareTypes {
                    superReference.transformChildren(transformer, ResolutionMode.ContextIndependent)
                }

                val actualSuperType = (superReference.superTypeRef.coneType as? ConeClassLikeType)
                    ?.fullyExpandedType(session)?.let { superType ->
                        val classId = superType.lookupTag.classId
                        val correspondingDeclaredSuperType = superTypeRefs.firstOrNull {
                            it.coneType.fullyExpandedType(session).classId == classId
                        }?.coneTypeSafe<ConeClassLikeType>()?.fullyExpandedType(session) ?: return@let null

                        if (superType.typeArguments.isEmpty() && correspondingDeclaredSuperType.typeArguments.isNotEmpty() ||
                            superType == correspondingDeclaredSuperType
                        ) {
                            correspondingDeclaredSuperType
                        } else {
                            null
                        }
                    }
                /*
                 * See tests:
                 *   DiagnosticsTestGenerated$Tests$ThisAndSuper.testGenericQualifiedSuperOverridden
                 *   DiagnosticsTestGenerated$Tests$ThisAndSuper.testQualifiedSuperOverridden
                 */
                val actualSuperTypeRef = actualSuperType?.toFirResolvedTypeRef(superTypeRef.source, superTypeRef) ?: buildErrorTypeRef {
                    source = superTypeRef.source
                    diagnostic = ConeSimpleDiagnostic("Not a super type", DiagnosticKind.NotASupertype)
                    annotations = superTypeRef.annotations.toMutableList()
                }
                superReferenceContainer.resultType = actualSuperTypeRef.coneType
                superReference.replaceSuperTypeRef(actualSuperTypeRef)
            }
            else -> {
                val types = components.findTypesForSuperCandidates(superTypeRefs, containingCall)
                val resultType = when (types.size) {
                    0 -> buildErrorTypeRef {
                        source = superReferenceContainer.source
                        // Errors on the callee will be reported, no reason to also report on the error type ref.
                        diagnostic =
                            ConeUnreportedDuplicateDiagnostic(ConeSimpleDiagnostic("Unresolved super method", DiagnosticKind.Other))
                    }
                    1 -> types.single().toFirResolvedTypeRef(superReferenceContainer.source?.fakeElement(KtFakeSourceElementKind.SuperCallImplicitType))
                    else -> buildErrorTypeRef {
                        source = superReferenceContainer.source
                        diagnostic = ConeAmbiguousSuper(types)
                    }
                }
                superReferenceContainer.resultType = resultType.coneType
                superReference.replaceSuperTypeRef(resultType)
            }
        }
        return superReferenceContainer
    }

    private fun markSuperReferenceError(
        superNotAvailableDiagnostic: ConeDiagnostic,
        superReferenceContainer: FirSuperReceiverExpression,
        superReference: FirSuperReference
    ): FirQualifiedAccessExpression {
        val resultType = buildErrorTypeRef {
            diagnostic = superNotAvailableDiagnostic
        }
        superReferenceContainer.resultType = resultType.coneType
        superReference.replaceSuperTypeRef(resultType)
        superReferenceContainer.replaceCalleeReference(
            buildErrorSuperReference {
                source = superReferenceContainer.source?.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
                diagnostic = superNotAvailableDiagnostic
                superTypeRef = superReference.superTypeRef
            }
        )
        return superReferenceContainer
    }

    protected open fun FirQualifiedAccessExpression.isAcceptableResolvedQualifiedAccess(): Boolean {
        return true
    }

    override fun transformSuperReceiverExpression(
        superReceiverExpression: FirSuperReceiverExpression,
        data: ResolutionMode,
    ): FirStatement {
        return transformQualifiedAccessExpression(superReceiverExpression, data)
    }

    override fun transformSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: ResolutionMode
    ): FirStatement {
        whileAnalysing(session, safeCallExpression) {
            withContainingSafeCallExpression(safeCallExpression) {
                safeCallExpression.transformAnnotations(this, ResolutionMode.ContextIndependent)

                safeCallExpression.transformReceiver(this, ResolutionMode.ReceiverResolution)
                safeCallExpression.transformReceiver(components.integerLiteralAndOperatorApproximationTransformer, null)

                val receiver = safeCallExpression.receiver

                dataFlowAnalyzer.enterSafeCallAfterNullCheck(safeCallExpression)

                safeCallExpression.apply {
                    checkedSubjectRef.value.propagateTypeFromOriginalReceiver(receiver, components.session, components.file)
                    transformSelector(this@FirExpressionsResolveTransformer, data)
                    propagateTypeFromQualifiedAccessAfterNullCheck(session, context.file)
                }

                dataFlowAnalyzer.exitSafeCall(safeCallExpression)

                return safeCallExpression
            }
        }
    }

    private inline fun <T> withContainingSafeCallExpression(safeCallExpression: FirSafeCallExpression, block: () -> T): T {
        val old = containingSafeCallExpression
        try {
            containingSafeCallExpression = safeCallExpression
            return block()
        } finally {
            containingSafeCallExpression = old
        }
    }

    override fun transformCheckedSafeCallSubject(
        checkedSafeCallSubject: FirCheckedSafeCallSubject,
        data: ResolutionMode
    ): FirStatement {
        return checkedSafeCallSubject
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ResolutionMode): FirStatement =
        transformFunctionCallInternal(functionCall, data, CallResolutionMode.REGULAR)

    internal enum class CallResolutionMode {
        REGULAR,

        /**
         * For PROVIDE_DELEGATE we skip transforming explicit receiver of the call since it's already been resolved
         * at [org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer.transformPropertyAccessorsWithDelegate]
         */
        PROVIDE_DELEGATE,

        /**
         * When we're resolving an operator like `a += b` we try to resolve it with different options of desugaring like
         * `a = a.plus(b)` and `a.plusAssign(b)` until find something that looks successful.
         * But at this stage, we skip transformation of receiver, arguments and skip completion in any form.
         */
        OPTION_FOR_AUGMENTED_ASSIGNMENT,
    }

    internal fun transformFunctionCallInternal(
        functionCall: FirFunctionCall,
        data: ResolutionMode,
        callResolutionMode: CallResolutionMode,
    ): FirStatement =
        whileAnalysing(session, functionCall) {
            val calleeReference = functionCall.calleeReference
            if (
                (calleeReference is FirResolvedNamedReference || calleeReference is FirErrorNamedReference) &&
                !functionCall.isResolved
            ) {
                storeTypeFromCallee(functionCall, isLhsOfAssignment = false)
            }
            if (calleeReference is FirNamedReferenceWithCandidate) return functionCall
            if (calleeReference !is FirSimpleNamedReference) {
                // The callee reference can be resolved as an error very early, e.g., `super` as a callee during raw FIR creation.
                // We still need to visit/transform other parts, e.g., call arguments, to check if any other errors are there.
                if (calleeReference !is FirResolvedNamedReference) {
                    functionCall.transformChildren(transformer, ResolutionMode.ContextIndependent)
                }
                return functionCall
            }
            functionCall.transformAnnotations(transformer, data)
            functionCall.replaceLambdaArgumentInvocationKinds(session)
            functionCall.transformTypeArguments(transformer, ResolutionMode.ContextIndependent)
            val choosingOptionForAugmentedAssignment = callResolutionMode == CallResolutionMode.OPTION_FOR_AUGMENTED_ASSIGNMENT
            val withTransformedArguments = if (!choosingOptionForAugmentedAssignment) {
                dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.arguments)
                // In provideDelegate mode the explicitReceiver is already resolved
                // E.g. we have val some by someDelegate
                // At 1st stage of delegate inference we resolve someDelegate itself,
                // at 2nd stage in provideDelegate mode we are trying to resolve someDelegate.provideDelegate(),
                // and 'someDelegate' explicit receiver is resolved at 1st stage
                // See also FirDeclarationsResolveTransformer.transformWrappedDelegateExpression
                val withResolvedExplicitReceiver = when (callResolutionMode) {
                    CallResolutionMode.PROVIDE_DELEGATE -> functionCall
                    else -> transformExplicitReceiverOf(functionCall)
                }
                withResolvedExplicitReceiver.also {
                    dataFlowAnalyzer.exitCallExplicitReceiver()

                    if (enableArrayOfCallTransformation && data is ResolutionMode.WithExpectedType) {
                        transformCallArgumentsInsideAnnotationContext(functionCall, data)
                    } else {
                        it.replaceArgumentList(it.argumentList.transform(this, ResolutionMode.ContextDependent))
                    }
                    dataFlowAnalyzer.exitCallArguments()
                }
            } else {
                functionCall
            }

            val resultExpression = callResolver.resolveCallAndSelectCandidate(withTransformedArguments, data)

            if (!choosingOptionForAugmentedAssignment) {
                dataFlowAnalyzer.enterFunctionCall(resultExpression)
            }
            val completeInference = callCompleter.completeCall(
                resultExpression, data,
                skipEvenPartialCompletion = choosingOptionForAugmentedAssignment,
            )
            var result = completeInference.transformToIntegerOperatorCallOrApproximateItIfNeeded(data)
            if (!choosingOptionForAugmentedAssignment) {
                dataFlowAnalyzer.exitFunctionCall(result, data.forceFullCompletion)
            }
            @OptIn(FirExtensionApiInternals::class)
            if (callRefinementExtensions != null) {
                val reference = result.calleeReference
                if (reference is FirResolvedNamedReference) {
                    val callData = reference.resolvedSymbol.fir.originalCallDataForPluginRefinedCall
                    if (callData != null) {
                        result = callData.extension.transform(result, callData.originalSymbol)
                    }
                }
            }

            context.addReceiversFromExtensions(result, components)

            if (enableArrayOfCallTransformation) {
                return arrayOfCallTransformer.transformFunctionCall(result, session)
            }
            return result
        }

    /**
     * Transforms the value arguments of some call inside the context of an annotation call.
     * Typically, we would fine here a nested annotation call `@Foo(Bar())` or an array factory call like `arrayOf`.
     */
    private fun transformCallArgumentsInsideAnnotationContext(call: FirFunctionCall, data: ResolutionMode.WithExpectedType) {
        // Special handling of nested calls inside annotation calls/default values.
        val expectedType = data.expectedType
        if (expectedType.fullyExpandedType(session).toClassSymbol(session)?.classKind == ClassKind.ANNOTATION_CLASS) {
            // Annotation calls inside annotation calls are treated similar to regular annotation calls,
            // mainly so that array literals are resolved with the correct expected type.
            val constructorSymbol = callResolver.getAnnotationConstructorSymbol(expectedType, null)
            transformAnnotationCallArguments(call, constructorSymbol)
        } else {
            // `arrayOf` calls inside annotation calls don't get special treatment, but we track the array element type.
            // This is necessary because the expected type needs to reach nested array literals for them to be resolved properly.
            // Example: @Foo(arrayOf(Bar([1]))
            val expectedArrayElementType = expectedType.arrayElementType()?.let { data.copy(it.toFirResolvedTypeRef()) }
            call.replaceArgumentList(
                call.argumentList.transform(
                    this,
                    expectedArrayElementType ?: ResolutionMode.ContextDependent
                )
            )
        }
    }

    /**
     * Transforms the value arguments of a call to an annotation inside the context of an annotation call.
     * This is either the top-level [FirAnnotationCall] or a nested [FirFunctionCall] to an annotation like in `@Foo(Bar())`.
     */
    fun transformAnnotationCallArguments(call: FirCall, constructorSymbol: FirConstructorSymbol?) {
        if (constructorSymbol != null && call.arguments.isNotEmpty()) {
            // Arguments of annotation calls may contain array literals.
            // To properly resolve array literals and report type mismatches, we need to know the expected type.
            // Because the symbol for an annotation call is already known, we can run argument mapping and extract the expected type
            // from the parameter.
            val mapping = transformer.resolutionContext.bodyResolveComponents.mapArguments(
                call.arguments.map {
                    @OptIn(UnsafeExpressionUtility::class)
                    ConeResolutionAtom.createRawAtomForPotentiallyUnresolvedExpression(it)
                },
                constructorSymbol.fir,
                originScope = null,
                callSiteIsOperatorCall = false,
            )
            val argumentsToParameters = mapping.toArgumentToParameterMapping().unwrapAtoms()

            call.replaceArgumentList(buildArgumentList {
                source = call.argumentList.source
                call.argumentList.arguments.mapTo(arguments) { arg ->
                    val parameter = argumentsToParameters[arg]
                    val expectedType = parameter?.returnTypeRef?.coneTypeOrNull
                    val parameterType = expectedType
                        ?.applyIf(mapping.parameterToCallArgumentMap[parameter] is VarargArgument && arg !is FirWrappedArgumentExpression) {
                            arrayElementType()
                        }
                    val resolutionMode = parameterType?.let {
                        ResolutionMode.WithExpectedType(
                            it.toFirResolvedTypeRef(),
                            forceFullCompletion = false,
                            arrayLiteralPosition = ArrayLiteralPosition.AnnotationArgument,
                        )
                    } ?: ResolutionMode.ContextDependent

                    arg.transformSingle(transformer, resolutionMode)
                }
            })
        } else {
            call.replaceArgumentList(call.argumentList.transform(transformer, ResolutionMode.ContextDependent))
        }
    }

    private fun FirFunctionCall.transformToIntegerOperatorCallOrApproximateItIfNeeded(resolutionMode: ResolutionMode): FirFunctionCall {
        if (!explicitReceiver.isIntegerLiteralOrOperatorCall()) return this
        val resolvedSymbol = calleeReference.toResolvedNamedFunctionSymbol() ?: return this
        if (!resolvedSymbol.isWrappedIntegerOperator()) return this

        val arguments = this.argumentList.arguments
        val argument = when (arguments.size) {
            0 -> null
            1 -> arguments.first()
            else -> return this
        }
        assert(argument?.isIntegerLiteralOrOperatorCall() != false)

        val originalCall = this

        val integerOperatorType = ConeIntegerConstantOperatorTypeImpl(
            isUnsigned = resolvedSymbol.isWrappedIntegerOperatorForUnsignedType(),
            isMarkedNullable = false
        )

        val approximationIsNeeded =
            resolutionMode !is ResolutionMode.ReceiverResolution && resolutionMode !is ResolutionMode.ContextDependent

        val integerOperatorCall = buildIntegerLiteralOperatorCall {
            source = originalCall.source
            coneTypeOrNull = integerOperatorType
            annotations.addAll(originalCall.annotations)
            typeArguments.addAll(originalCall.typeArguments)
            calleeReference = originalCall.calleeReference
            origin = originalCall.origin
            argumentList = originalCall.argumentList
            explicitReceiver = originalCall.explicitReceiver
            dispatchReceiver = originalCall.dispatchReceiver
            extensionReceiver = originalCall.extensionReceiver
        }

        return if (approximationIsNeeded) {
            integerOperatorCall.transformSingle<FirFunctionCall, ConeKotlinType?>(
                components.integerLiteralAndOperatorApproximationTransformer,
                resolutionMode.expectedType
            )
        } else {
            integerOperatorCall
        }
    }

    override fun transformBlock(block: FirBlock, data: ResolutionMode): FirStatement {
        context.forBlock(session) {
            transformBlockInCurrentScope(block, data)
        }
        return block
    }

    internal fun transformBlockInCurrentScope(block: FirBlock, data: ResolutionMode) {
        dataFlowAnalyzer.enterBlock(block)
        val numberOfStatements = block.statements.size

        block.transformStatementsIndexed(transformer) { index ->
            val value =
                if (index == numberOfStatements - 1)
                    if (data is ResolutionMode.WithExpectedType)
                        data.copy(lastStatementInBlock = true)
                    else
                        data
                else
                    ResolutionMode.ContextIndependent

            TransformData.Data(value)
        }
        block.transformOtherChildren(transformer, data)

        if (block is FirContractCallBlock) {
            // Annotations which appear on and within contracts need to be resolved explicitly.
            // The contract function call is resolved by the CONTRACTS phase but leaves all
            // annotations unresolved.
            block.call.acceptChildren(object : FirDefaultVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    element.acceptChildren(this)
                }

                override fun visitAnnotation(annotation: FirAnnotation) {
                    annotation.transformSingle(transformer, ContextIndependent)
                }

                override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
                    visitAnnotation(annotationCall)
                }
            })
        }

        if (data is ResolutionMode.WithExpectedType && data.expectedType.isUnitOrFlexibleUnit) {
            // Unit-coercion
            block.resultType = data.expectedType
            block.replaceIsUnitCoerced(true)
        } else {
            // Bottom-up propagation: from the return type of the last expression in the block to the block type
            block.writeResultType(session)
        }

        dataFlowAnalyzer.exitBlock(block)
    }

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: ResolutionMode,
    ): FirStatement {
        return transformQualifiedAccessExpression(thisReceiverExpression, data)
    }

    override fun transformComparisonExpression(
        comparisonExpression: FirComparisonExpression,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, comparisonExpression) {
        return (comparisonExpression.transformChildren(transformer, ResolutionMode.ContextIndependent) as FirComparisonExpression).also {
            it.resultType = builtinTypes.booleanType.coneType
            dataFlowAnalyzer.exitComparisonExpressionCall(it)
        }
    }

    @OptIn(FirContractViolation::class)
    override fun transformAugmentedAssignment(
        augmentedAssignment: FirAugmentedAssignment,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, augmentedAssignment) {
        val operation = augmentedAssignment.operation
        val fakeSourceKind = operation.toAugmentedAssignSourceKind()
        require(operation != FirOperation.ASSIGN)

        augmentedAssignment.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        dataFlowAnalyzer.enterCallArguments(augmentedAssignment, listOf(augmentedAssignment.rightArgument))
        val leftArgument = augmentedAssignment.leftArgument
            .transformAsExplicitReceiver(ResolutionMode.ReceiverResolution, isUsedAsGetClassReceiver = false)
        val rightArgument = augmentedAssignment.rightArgument.transformSingle(transformer, ResolutionMode.ContextDependent)
        dataFlowAnalyzer.exitCallArguments()

        val generator = GeneratorOfPlusAssignCalls(
            augmentedAssignment,
            augmentedAssignment.toReference(session)?.source,
            operation,
            leftArgument,
            rightArgument
        )

        // x.plusAssign(y)
        val assignOperatorCall = generator.createAssignOperatorCall(fakeSourceKind)
        val resolvedAssignCall = assignOperatorCall.resolveCandidateForAssignmentOperatorCall()
        val assignCallReference = resolvedAssignCall.calleeReference as? FirNamedReferenceWithCandidate
        val assignIsSuccessful = assignCallReference?.isError == false

        // x = x + y
        val simpleOperatorCall = generator.createSimpleOperatorCall(fakeSourceKind)
        val resolvedOperatorCall = simpleOperatorCall.resolveCandidateForAssignmentOperatorCall()
        val operatorCallReference = resolvedOperatorCall.calleeReference as? FirNamedReferenceWithCandidate
        val operatorIsSuccessful = operatorCallReference?.isError == false

        fun operatorReturnTypeMatches(candidate: Candidate): Boolean {
            // After KT-45503, non-assign flavor of operator is checked more strictly: the return type must be assignable to the variable.
            val operatorCallReturnType = resolvedOperatorCall.resolvedType
            val substitutor = candidate.system.currentStorage()
                .buildAbstractResultingSubstitutor(candidate.system.typeSystemContext) as ConeSubstitutor
            return AbstractTypeChecker.isSubtypeOf(
                session.typeContext,
                substitutor.substituteOrSelf(operatorCallReturnType),
                leftArgument.resolvedType
            )
        }
        // following `!!` is safe since `operatorIsSuccessful = true` implies `operatorCallReference != null`
        val operatorReturnTypeMatches = operatorIsSuccessful && operatorReturnTypeMatches(operatorCallReference!!.candidate)

        val lhsReference = leftArgument.toReference(session)
        val lhsSymbol =
            lhsReference?.let {
                it.toResolvedVariableSymbol()
                // In PCLA, calls might be not completed, thus not resolved (please, vote KTIJ-31545 if you dislike eccentric formatting)
                    ?: (it as? FirNamedReferenceWithCandidate)?.candidateSymbol as? FirVariableSymbol<*>
            }

        val lhsVariable = lhsSymbol?.fir
        val lhsIsVar = lhsVariable?.isVar == true

        fun chooseAssign(): FirStatement {
            dataFlowAnalyzer.enterFunctionCall(resolvedAssignCall)
            callCompleter.completeCall(resolvedAssignCall, ResolutionMode.ContextIndependent)
            dataFlowAnalyzer.exitFunctionCall(resolvedAssignCall, callCompleted = true)
            return resolvedAssignCall
        }

        fun chooseOperator(): FirStatement {
            dataFlowAnalyzer.enterFunctionCall(resolvedOperatorCall)
            callCompleter.completeCall(
                resolvedOperatorCall,
                (lhsVariable?.returnTypeRef as? FirResolvedTypeRef)?.let {
                    ResolutionMode.WithExpectedType(it)
                } ?: ResolutionMode.ContextIndependent,
            )
            dataFlowAnalyzer.exitFunctionCall(resolvedOperatorCall, callCompleted = true)

            val leftArgumentDesugaredSource = leftArgument.source?.fakeElement(fakeSourceKind)
            val unwrappedLeftArgument = leftArgument.unwrapSmartcastExpression()

            val assignmentSource = augmentedAssignment.source?.fakeElement(fakeSourceKind)

            val assignmentLeftArgument = buildDesugaredAssignmentValueReferenceExpression {
                expressionRef = FirExpressionRef<FirExpression>().apply { bind(unwrappedLeftArgument) }
                source = leftArgumentDesugaredSource
                    ?: assignmentSource?.fakeElement(KtFakeSourceElementKind.DesugaredAssignmentLValueSourceIsNull)
            }

            val assignment =
                buildVariableAssignment {
                    source = assignmentSource
                    lValue = assignmentLeftArgument
                    rValue = resolvedOperatorCall
                    annotations += augmentedAssignment.annotations
                }

            val receiverTemporaryVariable =
                generateExplicitReceiverTemporaryVariable(session, unwrappedLeftArgument, leftArgumentDesugaredSource)
            return if (receiverTemporaryVariable != null) {
                buildBlock {
                    source = assignmentSource
                    annotations += augmentedAssignment.annotations

                    statements += receiverTemporaryVariable
                    statements += assignment
                }
            } else {
                assignment
            }.transform(transformer, ResolutionMode.ContextIndependent)
        }

        fun chooseResolved(): FirStatement {
            // If neither candidate is successful, choose whichever is resolved, prioritizing assign
            val isAssignResolved = (assignCallReference as? FirErrorReferenceWithCandidate)?.diagnostic !is ConeUnresolvedNameError
            val isOperatorResolved = (operatorCallReference as? FirErrorReferenceWithCandidate)?.diagnostic !is ConeUnresolvedNameError
            return when {
                isAssignResolved -> chooseAssign()
                isOperatorResolved -> chooseOperator()
                else -> chooseAssign()
            }
        }

        fun reportAmbiguity(): FirStatement {
            val operatorCallCandidate = requireNotNull(operatorCallReference?.candidate)
            val assignmentCallCandidate = requireNotNull(assignCallReference?.candidate)
            return buildErrorExpression {
                source = augmentedAssignment.source
                diagnostic = ConeOperatorAmbiguityError(listOf(operatorCallCandidate, assignmentCallCandidate))
            }
        }

        return when {
            assignIsSuccessful && !lhsIsVar -> chooseAssign()
            !assignIsSuccessful && !operatorIsSuccessful -> chooseResolved()
            !assignIsSuccessful && operatorIsSuccessful -> chooseOperator()
            assignIsSuccessful && !operatorIsSuccessful -> chooseAssign()
            leftArgument.resolvedType is ConeDynamicType -> chooseAssign()
            !operatorReturnTypeMatches -> chooseAssign()
            else -> reportAmbiguity()
        }
    }

    @OptIn(FirContractViolation::class)
    override fun transformIncrementDecrementExpression(
        incrementDecrementExpression: FirIncrementDecrementExpression,
        data: ResolutionMode
    ): FirStatement {
        val fakeSourceKind = sourceKindForIncOrDec(incrementDecrementExpression.operationName, incrementDecrementExpression.isPrefix)
        incrementDecrementExpression.transformAnnotations(transformer, ResolutionMode.ContextIndependent)

        val expression = incrementDecrementExpression.expression.transformSingle(transformer, ResolutionMode.ContextIndependent)

        @OptIn(FirImplementationDetail::class)
        if (expression is FirQualifiedAccessExpression) expression.replaceSource(expression.source?.fakeElement(fakeSourceKind))

        val desugaredSource = incrementDecrementExpression.source?.fakeElement(fakeSourceKind)

        fun generateTemporaryVariable(name: Name, initializer: FirExpression): FirProperty = generateTemporaryVariable(
            moduleData = session.moduleData,
            source = desugaredSource,
            name = name,
            initializer = initializer,
            typeRef = initializer.resolvedType.toFirResolvedTypeRef(desugaredSource),
        )

        fun buildAndResolveOperatorCall(
            receiver: FirExpression,
            fakeSourceKind: KtFakeSourceElementKind.DesugaredIncrementOrDecrement,
        ): FirFunctionCall = buildFunctionCall {
            source = incrementDecrementExpression.operationSource
            explicitReceiver = receiver
            calleeReference = buildSimpleNamedReference {
                source = incrementDecrementExpression.operationSource?.fakeElement(fakeSourceKind)
                name = incrementDecrementExpression.operationName
            }
            origin = FirFunctionCallOrigin.Operator
        }.transformSingle(transformer, ResolutionMode.ContextIndependent)

        fun buildAndResolveVariableAssignment(rValue: FirExpression): FirVariableAssignment = buildVariableAssignment {
            source = desugaredSource
            lValue = buildDesugaredAssignmentValueReferenceExpression {
                source = ((expression as? FirErrorExpression)?.expression ?: expression).source
                    ?.fakeElement(fakeSourceKind)
                    ?: desugaredSource?.fakeElement(KtFakeSourceElementKind.DesugaredAssignmentLValueSourceIsNull)
                expressionRef = FirExpressionRef<FirExpression>().apply { bind(expression.unwrapSmartcastExpression()) }
            }
            this.rValue = rValue
        }.transformSingle(transformer, ResolutionMode.ContextIndependent)

        return buildBlock {
            source = desugaredSource
            annotations += incrementDecrementExpression.annotations

            generateExplicitReceiverTemporaryVariable(session, expression, desugaredSource)
                ?.let { statements += it }

            if (incrementDecrementExpression.isPrefix) {
                // a = a.inc()
                statements += buildAndResolveVariableAssignment(buildAndResolveOperatorCall(expression, fakeSourceKind))
                // ^a
                statements += buildDesugaredAssignmentValueReferenceExpression {
                    source = ((expression as? FirErrorExpression)?.expression ?: expression).source
                        ?.fakeElement(fakeSourceKind) ?: desugaredSource?.fakeElement(KtFakeSourceElementKind.DesugaredAssignmentLValueSourceIsNull)
                    expressionRef = FirExpressionRef<FirExpression>().apply { bind(expression.unwrapSmartcastExpression()) }
                }.let {
                    it.transformSingle(transformer, ResolutionMode.ContextIndependent)
                    transformExpressionUsingSmartcastInfo(it)
                }
            } else {
                val unaryVariable = generateTemporaryVariable(SpecialNames.UNARY, expression)
                dataFlowAnalyzer.exitLocalVariableDeclaration(unaryVariable, hadExplicitType = false)

                // val <unary> = a
                statements += unaryVariable
                // a = <unary>.inc()
                statements += buildAndResolveVariableAssignment(
                    buildAndResolveOperatorCall(
                        transformExpressionUsingSmartcastInfo(unaryVariable.toQualifiedAccess()),
                        fakeSourceKind
                    )
                )
                // ^<unary>
                statements += transformExpressionUsingSmartcastInfo(unaryVariable.toQualifiedAccess())
            }
        }.apply {
            replaceConeTypeOrNull((statements.last() as FirExpression).resolvedType)
        }
    }

    override fun transformEqualityOperatorCall(
        equalityOperatorCall: FirEqualityOperatorCall,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, equalityOperatorCall) {
        val arguments = equalityOperatorCall.argumentList.arguments
        require(arguments.size == 2) {
            "Unexpected number of arguments in equality call: ${arguments.size}"
        }
        // In cases like materialize1() == materialize2() we add expected type just for the right argument.
        // One of the reasons is just consistency with K1 and with the desugared form `a.equals(b)`. See KT-47409 for clarifications.
        val leftArgumentTransformed: FirExpression = arguments[0].transform(transformer, ResolutionMode.ContextIndependent)
        dataFlowAnalyzer.exitEqualityOperatorLhs()
        val rightArgumentTransformed: FirExpression =
            arguments[1].transform(
                transformer,
                withExpectedType(
                    // We use `Any?` as a real expected type used for inference and other things
                    builtinTypes.nullableAnyType,
                    // But for context-sensitive resolution cases like myValue == ENUM_ENTRY we use the type of the LHS.
                    // Potentially, we might just use LHS type just as a regular expected type which would be used both
                    // for inference and context-sensitive resolution but that would be a very big shift in the semantics.
                    hintForContextSensitiveResolution = leftArgumentTransformed.resolvedType,
                )
            )

        equalityOperatorCall
            .transformAnnotations(transformer, ResolutionMode.ContextIndependent)
            .replaceArgumentList(buildBinaryArgumentList(leftArgumentTransformed, rightArgumentTransformed))
        equalityOperatorCall.resultType = builtinTypes.booleanType.coneType

        dataFlowAnalyzer.exitEqualityOperatorCall(equalityOperatorCall)
        return equalityOperatorCall
    }

    private fun FirFunctionCall.resolveCandidateForAssignmentOperatorCall(): FirFunctionCall {
        return transformFunctionCallInternal(
            this,
            ResolutionMode.ContextDependent,
            CallResolutionMode.OPTION_FOR_AUGMENTED_ASSIGNMENT
        ) as FirFunctionCall
    }

    private fun FirTypeRef.withTypeArgumentsForBareType(argument: FirExpression, operation: FirOperation): FirTypeRef {
        val type = coneTypeSafe<ConeClassLikeType>() ?: return this
        if (type.typeArguments.isNotEmpty()) return this // TODO: Incorrect for local classes, KT-59686
        // TODO: Check equality of size of arguments and parameters?

        val firClass = type.lookupTag.toSymbol(session)?.fir ?: return this
        if (firClass.typeParameters.isEmpty()) return this

        val originalType = argument.unwrapExpression().resolvedType.let {
            components.context.inferenceSession.getAndSemiFixCurrentResultIfTypeVariable(it) ?: it
        }

        val outerClasses by lazy(LazyThreadSafetyMode.NONE) { firClass.symbol.getClassAndItsOuterClassesWhenLocal(session) }
        val newType = components.computeRepresentativeTypeForBareType(type, originalType)
            ?: if (
                firClass.isLocal && firClass.typeParameters.none { it.symbol.containingDeclarationSymbol in outerClasses } &&
                (operation == NOT_IS || operation == IS || operation == AS || operation == SAFE_AS)
            ) {
                (firClass as FirClass).defaultType()
            } else return buildErrorTypeRef {
                source = this@withTypeArgumentsForBareType.source
                diagnostic = ConeNoTypeArgumentsOnRhsError(firClass.typeParameters.size, firClass.symbol)
            }
        return if (newType.typeArguments.isEmpty()) this else withReplacedConeType(newType)
    }

    override fun transformTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: ResolutionMode,
    ): FirStatement {
        val resolved = components.typeResolverTransformer.withBareTypes {
            if (typeOperatorCall.operation == IS || typeOperatorCall.operation == NOT_IS) {
                components.typeResolverTransformer.withIsOperandOfIsOperator {
                    typeOperatorCall.transformConversionTypeRef(transformer, ResolutionMode.ContextIndependent)
                }
            } else {
                typeOperatorCall.transformConversionTypeRef(transformer, ResolutionMode.ContextIndependent)
            }
        }.transformTypeOperatorCallChildren()

        resolved.resolveConversionTypeRefInContextSensitiveModeIfNecessary()

        val conversionTypeRef = resolved.conversionTypeRef.withTypeArgumentsForBareType(resolved.argument, typeOperatorCall.operation)
        resolved.transformChildren(object : FirDefaultTransformer<Any?>() {
            override fun <E : FirElement> transformElement(element: E, data: Any?): E {
                return element
            }

            override fun transformTypeRef(typeRef: FirTypeRef, data: Any?): FirTypeRef {
                return if (typeRef === resolved.conversionTypeRef) {
                    conversionTypeRef
                } else {
                    typeRef
                }
            }
        }, null)

        when (resolved.operation) {
            IS, NOT_IS -> {
                resolved.resultType = session.builtinTypes.booleanType.coneType
            }
            AS -> {
                resolved.resultType = conversionTypeRef.coneType
            }
            SAFE_AS -> {
                resolved.resultType = conversionTypeRef.coneType.withNullability(
                    nullable = true, session.typeContext,
                )
            }
            else -> error("Unknown type operator: ${resolved.operation}")
        }
        dataFlowAnalyzer.exitTypeOperatorCall(resolved)
        return resolved
    }

    private fun FirTypeOperatorCall.transformTypeOperatorCallChildren(): FirTypeOperatorCall {
        if (operation == AS || operation == SAFE_AS) {
            val argument = argumentList.arguments.singleOrNull() ?: error("Not a single argument: ${this.render()}")

            // For calls in the form of (materialize() as MyClass) we've got a special rule that adds expect type to the `materialize()` call
            // AS operator doesn't add expected type to any other expressions
            // See https://kotlinlang.org/docs/whatsnew12.html#support-for-foo-as-a-shorthand-for-this-foo
            // And limitations at org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleterKt.isFunctionForExpectTypeFromCastFeature(org.jetbrains.kotlin.fir.declarations.FirFunction<?>)
            if (argument is FirFunctionCall || (argument is FirSafeCallExpression && argument.selector is FirFunctionCall)) {
                val expectedType = conversionTypeRef.coneTypeSafe<ConeKotlinType>()?.takeIf {
                    // is not bare type
                    it !is ConeClassLikeType ||
                            it.typeArguments.isNotEmpty() ||
                            it.lookupTag.toSymbol(session)?.fir?.typeParameters?.isEmpty() == true
                }?.let {
                    if (operation == SAFE_AS)
                        it.withNullability(nullable = true, session.typeContext)
                    else
                        it
                }

                if (expectedType != null) {
                    val newMode = ResolutionMode.WithExpectedType(conversionTypeRef.withReplacedConeType(expectedType), fromCast = true)
                    return transformOtherChildren(transformer, newMode)
                }
            }
        }

        return transformOtherChildren(transformer, ResolutionMode.ContextIndependent)
    }

    /**
     * If context-sensitive resolution is necessary and successful, conversionTypeRef will be replaced with an updated result
     */
    private fun FirTypeOperatorCall.resolveConversionTypeRefInContextSensitiveModeIfNecessary() {
        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.ContextSensitiveResolutionUsingExpectedType)) return

        // If the type is resolved correctly, we don't need to re-run the resolution
        val errorTypeRef = conversionTypeRef as? FirErrorTypeRef ?: return

        // Don't re-run resolution if there's some visible class available
        if (!errorTypeRef.diagnostic.meansAbsenceOfVisibleClass()) return

        // We only re-run resolution for simple name qualifiers
        val userTypeRef = errorTypeRef.delegatedTypeRef as? FirUserTypeRef ?: return
        if (userTypeRef.qualifier.size != 1) return

        val argument = argumentList.arguments.singleOrNull() ?: error("Not a single argument: ${this.render()}")

        val classToLookAt =
            argument.resolvedType.getClassRepresentativeForContextSensitiveResolution(session)
                ?.takeIf { it.isSealed }
                ?: return

        val resultingTypeRef = typeResolverTransformer.withBareTypes {
            typeResolverTransformer.transformTypeRef(
                userTypeRef,
                TypeResolutionConfiguration.createForContextSensitiveResolution(
                    context.containingClassDeclarations,
                    context.file,
                    context.topContainerForTypeResolution,
                    sealedClassForContextSensitiveResolution = classToLookAt,
                )
            )
        }

        if (resultingTypeRef is FirErrorTypeRef || resultingTypeRef.coneType is ConeErrorType) return

        replaceConversionTypeRef(resultingTypeRef)
    }

    @OptIn(UnresolvedExpressionTypeAccess::class)
    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ResolutionMode,
    ): FirStatement {
        // Resolve the return type of a call to the synthetic function with signature:
        //   fun <K> checkNotNull(arg: K?): K
        // ...in order to get the not-nullable type of the argument.

        if (checkNotNullCall.calleeReference is FirResolvedNamedReference && checkNotNullCall.isResolved) {
            return checkNotNullCall
        }

        if (checkNotNullCall.arguments.firstOrNull()?.coneTypeOrNull !is ConeDynamicType) {
            dataFlowAnalyzer.enterCheckNotNullCall()
        }

        checkNotNullCall
            .transformAnnotations(transformer, ResolutionMode.ContextIndependent)
            .replaceArgumentList(checkNotNullCall.argumentList.transform(transformer, ResolutionMode.ContextDependent))

        val result = callCompleter.completeCall(
            components.syntheticCallGenerator.generateCalleeForCheckNotNullCall(checkNotNullCall, resolutionContext, data), data
        )

        if (checkNotNullCall.arguments.firstOrNull()?.coneTypeOrNull !is ConeDynamicType) {
            dataFlowAnalyzer.exitCheckNotNullCall(result, data.forceFullCompletion)
        }

        return result
    }


    private fun ConeDiagnostic.meansAbsenceOfVisibleClass(): Boolean {
        // No class found at all
        if (this is ConeUnresolvedTypeQualifierError) return true
        // Only invisible class found
        if (this is ConeVisibilityError) return true
        // It's necessary for context-sensitive resolution to distinguish the situations:
        // - when ambiguity happens among available candidates, so it's not correct to run context-sensitive resolution
        // - when all the candidates are invisible, thus it's safe to run context-sensitive resolution
        //
        // The idea is that even ambiguity among imported invisible classes should behave just
        // like there are none of them exists.
        // Otherwise, introducing a private class might become a source-breaking change.
        @OptIn(ApplicabilityDetail::class)
        if (this is ConeAmbiguityError && !this.applicability.isSuccess) return true

        return false
    }

    override fun transformBooleanOperatorExpression(
        booleanOperatorExpression: FirBooleanOperatorExpression,
        data: ResolutionMode,
    ): FirStatement = whileAnalysing(session, booleanOperatorExpression) {
        val booleanType = builtinTypes.booleanType.coneType.toFirResolvedTypeRef()
        return booleanOperatorExpression.also(dataFlowAnalyzer::enterBooleanOperatorExpression)
            .transformLeftOperand(this, ResolutionMode.WithExpectedType(booleanType))
            .also(dataFlowAnalyzer::exitLeftBooleanOperatorExpressionArgument)
            .transformRightOperand(this, ResolutionMode.WithExpectedType(booleanType))
            .also(dataFlowAnalyzer::exitBooleanOperatorExpression)
            .transformOtherChildren(transformer, ResolutionMode.WithExpectedType(booleanType))
            .also { it.resultType = booleanType.coneType }
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ResolutionMode,
    ): FirStatement = whileAnalysing(session, variableAssignment) {
        variableAssignment.transformAnnotations(transformer, ResolutionMode.ContextIndependent)

        variableAssignment.transformLValue(transformer, ResolutionMode.AssignmentLValue(variableAssignment))

        val resolvedReference = variableAssignment.calleeReference

        if (assignAltererExtensions != null && resolvedReference is FirResolvedNamedReference) {
            val alteredAssignments = assignAltererExtensions.mapNotNull { alterer ->
                alterer.transformVariableAssignment(variableAssignment)?.let { it to alterer }
            }
            when (alteredAssignments.size) {
                0 -> {}
                1 -> {
                    val transformedAssignment = alteredAssignments.first().first
                    return transformedAssignment.transform(transformer, ResolutionMode.ContextIndependent)
                }

                else -> {
                    val extensionNames = alteredAssignments.map { it.second::class.qualifiedName }
                    val errorLValue = buildErrorExpression {
                        expression = variableAssignment.lValue
                        source = variableAssignment.lValue.source?.fakeElement(KtFakeSourceElementKind.AssignmentLValueError)
                        diagnostic = ConeAmbiguousAlteredAssign(extensionNames)
                    }
                    variableAssignment.replaceLValue(errorLValue)
                }
            }
        }

        val result = context.withAssignmentRhs {
            variableAssignment.transformRValue(
                transformer,
                withExpectedType(
                    variableAssignment.lValue.resolvedType.toFirResolvedTypeRef(),
                ),
            )
        }

        // for cases like
        // buildSomething { tVar = "" // Should infer TV from String assignment }
        context.inferenceSession.addSubtypeConstraintIfCompatible(
            lowerType = variableAssignment.rValue.resolvedType,
            upperType = variableAssignment.lValue.resolvedType,
            variableAssignment,
        )

        dataFlowAnalyzer.exitVariableAssignment(result)

        return result
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ResolutionMode,
    ): FirStatement = whileAnalysing(session, callableReferenceAccess) {
        if (callableReferenceAccess.calleeReference is FirResolvedNamedReference) {
            return callableReferenceAccess
        }

        callableReferenceAccess.transformAnnotations(transformer, data)
        val explicitReceiver = callableReferenceAccess.explicitReceiver
        val transformedLHS = explicitReceiver
            ?.transformAsExplicitReceiver(ResolutionMode.ReceiverResolution.ForCallableReference, false)
            .apply {
                if (this is FirResolvedQualifier && callableReferenceAccess.hasQuestionMarkAtLHS) {
                    replaceIsNullableLHSForCallableReference(true)
                }
            }

        transformedLHS?.let { callableReferenceAccess.replaceExplicitReceiver(transformedLHS) }

        return if (data is ResolutionMode.ContextDependent) {
            context.storeCallableReferenceContext(callableReferenceAccess)
            callableReferenceAccess
        } else {
            components.syntheticCallGenerator.resolveCallableReferenceWithSyntheticOuterCall(
                callableReferenceAccess, data.expectedType, resolutionContext
            )
        }.also {
            dataFlowAnalyzer.exitCallableReference(it)
        }
    }

    override fun transformGetClassCall(
        getClassCall: FirGetClassCall,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, getClassCall) {
        getClassCall.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        val arg = getClassCall.argument
        val dataForLhs = if (arg is FirLiteralExpression) {
            withExpectedType(arg.kind.expectedConeType(session).toFirResolvedTypeRef())
        } else {
            ResolutionMode.ContextIndependent
        }

        val transformedGetClassCall = run {
            val argument = getClassCall.argument
            val replacedArgument: FirExpression = argument.transformAsExplicitReceiver(dataForLhs, isUsedAsGetClassReceiver = true)

            getClassCall.argumentList.transformArguments(object : FirTransformer<Nothing?>() {
                @Suppress("UNCHECKED_CAST")
                override fun <E : FirElement> transformElement(element: E, data: Nothing?): E = replacedArgument as E
            }, null)

            getClassCall
        }

        // unwrapSmartCastExpression() shouldn't be here, otherwise we can get FirResolvedQualifier instead of a real receiver
        // see e.g. uselessCastLeadsToRecursiveProblem.kt
        val typeOfExpression = when (val lhs = transformedGetClassCall.argument) {
            is FirResolvedQualifier -> {
                lhs.replaceResolvedToCompanionObject(newResolvedToCompanionObject = false)
                val symbol = lhs.symbol
                val typeArguments: Array<ConeTypeProjection> =
                    if (lhs.typeArguments.isNotEmpty()) {
                        // If type arguments exist, use them to construct the type of the expression.
                        lhs.typeArguments.map { it.toConeTypeProjection() }.toTypedArray()
                    } else {
                        // Otherwise, prepare the star projections as many as the size of type parameters.
                        Array((symbol?.fir as? FirTypeParameterRefsOwner)?.typeParameters?.size ?: 0) {
                            ConeStarProjection
                        }
                    }
                val type = symbol?.constructType(typeArguments)
                if (type != null) {
                    lhs.replaceConeTypeOrNull(
                        type.also {
                            session.lookupTracker?.recordTypeResolveAsLookup(it, getClassCall.source, components.file.source)
                        }
                    )
                    type
                } else {
                    lhs.resolvedType
                }
            }
            is FirResolvedReifiedParameterReference -> {
                val symbol = lhs.symbol
                symbol.constructType()
            }
            else -> {
                if (!shouldComputeTypeOfGetClassCallWithNotQualifierInLhs(getClassCall)) return transformedGetClassCall
                val resultType = lhs.resolvedType
                if (resultType is ConeErrorType) {
                    resultType
                } else {
                    ConeKotlinTypeProjectionOut(resultType)
                }
            }
        }

        transformedGetClassCall.resultType = StandardClassIds.KClass.constructClassLikeType(arrayOf(typeOfExpression), false)
        dataFlowAnalyzer.exitGetClassCall(transformedGetClassCall)
        return transformedGetClassCall
    }

    protected open fun shouldComputeTypeOfGetClassCallWithNotQualifierInLhs(getClassCall: FirGetClassCall): Boolean {
        return true
    }

    override fun transformLiteralExpression(
        literalExpression: FirLiteralExpression,
        data: ResolutionMode,
    ): FirStatement {
        literalExpression.transformAnnotations(transformer, ResolutionMode.ContextIndependent)

        val type = when (val kind = literalExpression.kind) {
            ConstantValueKind.IntegerLiteral, ConstantValueKind.UnsignedIntegerLiteral -> {
                val expressionType = ConeIntegerLiteralConstantTypeImpl.create(
                    literalExpression.value as Long,
                    isTypePresent = { it.lookupTag.toSymbol(session) != null },
                    isUnsigned = kind == ConstantValueKind.UnsignedIntegerLiteral
                )
                val expectedType = data.expectedType
                when {
                    expressionType is ConeErrorType -> {
                        expressionType
                    }
                    expressionType is ConeClassLikeType -> {
                        literalExpression.replaceKind(expressionType.toConstKind() as ConstantValueKind)
                        expressionType
                    }
                    data is ResolutionMode.ReceiverResolution && !data.forCallableReference -> {
                        require(expressionType is ConeIntegerLiteralConstantTypeImpl)
                        ConeIntegerConstantOperatorTypeImpl(expressionType.isUnsigned, isMarkedNullable = false)
                    }
                    data is ResolutionMode.WithExpectedType ||
                            data is ResolutionMode.ContextIndependent ||
                            data is ResolutionMode.AssignmentLValue ||
                            data is ResolutionMode.ReceiverResolution -> {
                        require(expressionType is ConeIntegerLiteralConstantTypeImpl)
                        val coneType = expectedType?.fullyExpandedType(session)
                        val approximatedType = expressionType.getApproximatedType(coneType)
                        literalExpression.replaceKind(approximatedType.toConstKind() as ConstantValueKind)
                        approximatedType
                    }
                    else -> {
                        expressionType
                    }
                }
            }
            else -> kind.expectedConeType(session)
        }

        dataFlowAnalyzer.exitLiteralExpression(literalExpression)
        literalExpression.resultType = type

        return when (val resolvedType = literalExpression.resolvedType) {
            is ConeErrorType -> buildErrorExpression {
                expression = literalExpression
                diagnostic = resolvedType.diagnostic
                source = literalExpression.source
            }

            else -> literalExpression
        }
    }

    override fun transformAnnotation(annotation: FirAnnotation, data: ResolutionMode): FirStatement {
        if (annotation.resolved) return annotation
        annotation.transformAnnotationTypeRef(transformer, ResolutionMode.ContextIndependent)
        return annotation
    }

    override fun transformAnnotationCall(
        annotationCall: FirAnnotationCall,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, annotationCall) {
        if (annotationCall.resolved) return annotationCall
        annotationCall.transformAnnotationTypeRef(transformer, ResolutionMode.ContextIndependent)
        annotationCall.replaceAnnotationResolvePhase(FirAnnotationResolvePhase.Types)
        return withFirArrayOfCallTransformer {
            dataFlowAnalyzer.enterAnnotation()
            val result = callResolver.resolveAnnotationCall(annotationCall)
            dataFlowAnalyzer.exitAnnotation()
            if (result == null) return annotationCall
            callCompleter.completeCall(result, ResolutionMode.ContextIndependent)
            (result.argumentList as FirResolvedArgumentList).let { annotationCall.replaceArgumentMapping((it).toAnnotationArgumentMapping()) }
            annotationCall
        }
    }

    override fun transformErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: ResolutionMode): FirStatement {
        return transformAnnotationCall(errorAnnotationCall, data)
    }

    protected inline fun <T> withFirArrayOfCallTransformer(block: () -> T): T {
        enableArrayOfCallTransformation = true
        return try {
            block()
        } finally {
            enableArrayOfCallTransformation = false
        }
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ResolutionMode,
    ): FirStatement = whileAnalysing(session, delegatedConstructorCall) {
        if (transformer.implicitTypeOnly) return delegatedConstructorCall
        when (delegatedConstructorCall.calleeReference) {
            is FirResolvedNamedReference, is FirErrorNamedReference -> return delegatedConstructorCall
        }
        val containers = components.context.containers
        val containingClass = containers[containers.lastIndex - 1] as FirClass
        val containingConstructor = containers.last() as FirConstructor
        if (delegatedConstructorCall.isSuper && delegatedConstructorCall.constructedTypeRef is FirImplicitTypeRef) {
            val superClass = containingClass.superTypeRefs.firstOrNull {
                if (it !is FirResolvedTypeRef) return@firstOrNull false
                val declaration = extractSuperTypeDeclaration(it) ?: return@firstOrNull false
                val isExternalConstructorWithoutArguments = declaration.isExternal
                        && delegatedConstructorCall.isCallToDelegatedConstructorWithoutArguments
                declaration.classKind == ClassKind.CLASS && !isExternalConstructorWithoutArguments

            } as FirResolvedTypeRef? ?: session.builtinTypes.anyType
            delegatedConstructorCall.replaceConstructedTypeRef(superClass)
            delegatedConstructorCall.replaceCalleeReference(buildExplicitSuperReference {
                source = delegatedConstructorCall.calleeReference.source
                superTypeRef = superClass
            })
        }

        dataFlowAnalyzer.enterCallArguments(delegatedConstructorCall, delegatedConstructorCall.arguments)
        context.forDelegatedConstructorCallChildren(containingConstructor, containingClass as? FirRegularClass, components) {
            delegatedConstructorCall.transformChildren(transformer, ResolutionMode.ContextDependent)
        }
        dataFlowAnalyzer.exitCallArguments()

        val reference = delegatedConstructorCall.calleeReference
        val constructorType: ConeClassLikeType? = when (reference) {
            is FirThisReference -> containingClass.defaultType()
            is FirSuperReference -> reference.superTypeRef
                .coneTypeSafe<ConeClassLikeType>()
                ?.takeIf { it !is ConeErrorType }
                ?.fullyExpandedType(session)
            else -> null
        }

        val resolvedCall = context.forDelegatedConstructorCallResolution {
            callResolver.resolveDelegatingConstructorCall(delegatedConstructorCall, constructorType, containingClass.symbol.toLookupTag())
        }

        if (reference is FirThisReference && reference.boundSymbol == null) {
            resolvedCall.dispatchReceiver?.resolvedType?.classLikeLookupTagIfAny?.toSymbol(session)?.let {
                reference.replaceBoundSymbol(it)
            }
        }

        // it seems that we may leave this code as is
        // without adding `context.withTowerDataContext(context.getTowerDataContextForConstructorResolution())`
        val result = callCompleter.completeCall(resolvedCall, ResolutionMode.ContextIndependent)
        dataFlowAnalyzer.exitDelegatedConstructorCall(result, data.forceFullCompletion)

        // Update source of delegated constructor call when supertype isn't initialized
        val sourceKind = result.source?.kind
        if (containingConstructor.isPrimary &&
            sourceKind is KtFakeSourceElementKind &&
            // Delegated constructor calls of primary constructors with uninitialized supertypes have the whole class as source
            result.source == containingClass.source?.fakeElement(sourceKind)
        ) {
            val superTypeRef = containingClass.superTypeRefs.firstOrNull {
                it.coneType == result.toResolvedCallableSymbol()?.resolvedReturnType
            }
            @OptIn(FirImplementationDetail::class)
            if (superTypeRef?.source?.kind is KtRealSourceElementKind) {
                result.replaceSource(superTypeRef.source?.fakeElement(KtFakeSourceElementKind.DelegatingConstructorCall))
            }
        }

        return result
    }

    override fun transformMultiDelegatedConstructorCall(
        multiDelegatedConstructorCall: FirMultiDelegatedConstructorCall,
        data: ResolutionMode,
    ): FirStatement {
        multiDelegatedConstructorCall.transformChildren(transformer, data)
        return multiDelegatedConstructorCall
    }

    private val FirDelegatedConstructorCall.isCallToDelegatedConstructorWithoutArguments
        get() = source?.kind == KtFakeSourceElementKind.DelegatingConstructorCall

    private fun extractSuperTypeDeclaration(typeRef: FirTypeRef): FirRegularClass? {
        if (typeRef !is FirResolvedTypeRef) return null
        return when (val declaration = typeRef.firClassLike(session)) {
            is FirRegularClass -> declaration
            is FirTypeAlias -> extractSuperTypeDeclaration(declaration.expandedTypeRef)
            else -> null
        }
    }

    private class GeneratorOfPlusAssignCalls(
        val baseElement: FirStatement,
        val referenceSource: KtSourceElement?,
        val operation: FirOperation,
        val lhs: FirExpression,
        val rhs: FirExpression
    ) {
        companion object {
            fun createFunctionCall(
                name: Name,
                source: KtSourceElement?,
                referenceSource: KtSourceElement?,
                annotations: List<FirAnnotation>,
                receiver: FirExpression,
                vararg arguments: FirExpression
            ): FirFunctionCall = buildFunctionCall {
                this.source = source
                explicitReceiver = receiver
                argumentList = when (arguments.size) {
                    0 -> FirEmptyArgumentList
                    1 -> buildUnaryArgumentList(arguments.first())
                    else -> buildArgumentList {
                        this.arguments.addAll(arguments)
                    }
                }
                calleeReference = buildSimpleNamedReference {
                    this.source = referenceSource ?: source
                    this.name = name
                }
                origin = FirFunctionCallOrigin.Operator
                this.annotations.addAll(annotations)
            }
        }

        private fun createFunctionCall(name: Name, fakeSourceElementKind: KtFakeSourceElementKind): FirFunctionCall {
            return createFunctionCall(
                name,
                baseElement.source?.fakeElement(fakeSourceElementKind),
                referenceSource,
                baseElement.annotations,
                lhs,
                rhs,
            )
        }

        fun createAssignOperatorCall(fakeSourceElementKind: KtFakeSourceElementKind): FirFunctionCall {
            return createFunctionCall(FirOperationNameConventions.ASSIGNMENTS.getValue(operation), fakeSourceElementKind)
        }

        fun createSimpleOperatorCall(fakeSourceElementKind: KtFakeSourceElementKind): FirFunctionCall {
            return createFunctionCall(FirOperationNameConventions.ASSIGNMENTS_TO_SIMPLE_OPERATOR.getValue(operation), fakeSourceElementKind)
        }
    }

    override fun transformIndexedAccessAugmentedAssignment(
        indexedAccessAugmentedAssignment: FirIndexedAccessAugmentedAssignment,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, indexedAccessAugmentedAssignment) {
        /*
         * a[b] += c can be desugared to:
         *
         * 1. a.get(b).plusAssign(c)
         * 2. a.set(b, a.get(b).plus(c))
         */

        val operation = indexedAccessAugmentedAssignment.operation
        assert(operation in FirOperation.ASSIGNMENTS)
        assert(operation != FirOperation.ASSIGN)

        val fakeSourceElementKind = operation.toAugmentedAssignSourceKind()

        indexedAccessAugmentedAssignment.transformAnnotations(transformer, data)

        dataFlowAnalyzer.enterCallArguments(indexedAccessAugmentedAssignment, listOf(indexedAccessAugmentedAssignment.rhs))
        // transformedLhsCall: a.get(index)
        val transformedLhsCall = indexedAccessAugmentedAssignment.lhsGetCall.transformSingle(transformer, ResolutionMode.ContextIndependent)
            .also { it.setIndexedAccessAugmentedAssignSource(fakeSourceElementKind) }
        val transformedRhs = indexedAccessAugmentedAssignment.rhs.transformSingle(transformer, ResolutionMode.ContextDependent)
        dataFlowAnalyzer.exitCallArguments()

        val generator = GeneratorOfPlusAssignCalls(
            indexedAccessAugmentedAssignment,
            indexedAccessAugmentedAssignment.calleeReference.source,
            operation,
            transformedLhsCall,
            transformedRhs
        )

        // a.get(b).plusAssign(c)
        val assignOperatorCall = generator.createAssignOperatorCall(operation.toAugmentedAssignSourceKind())
        val resolvedAssignCall = assignOperatorCall.resolveCandidateForAssignmentOperatorCall()
        val assignCallReference = resolvedAssignCall.calleeReference as? FirNamedReferenceWithCandidate
        val assignIsSuccessful = assignCallReference?.isError == false

        fun chooseAssign(): FirFunctionCall {
            dataFlowAnalyzer.enterFunctionCall(resolvedAssignCall)
            callCompleter.completeCall(resolvedAssignCall, ResolutionMode.ContextIndependent)
            dataFlowAnalyzer.exitFunctionCall(resolvedAssignCall, callCompleted = true)
            return resolvedAssignCall
        }

        // prefer a "simpler" variant for dynamics
        if (transformedLhsCall.calleeReference.toResolvedBaseSymbol()?.origin == FirDeclarationOrigin.DynamicScope) {
            return chooseAssign()
        }

        // <array>.set(<index_i>, <array>.get(<index_i>).plus(c))
        val info =
            tryResolveIndexedAccessAugmentedAssignmentAsSetGetBlock(indexedAccessAugmentedAssignment, transformedLhsCall, transformedRhs, fakeSourceElementKind)

        val resolvedOperatorCall = info.operatorCall
        val operatorCallReference = resolvedOperatorCall.calleeReference as? FirNamedReferenceWithCandidate
        val operatorIsSuccessful = operatorCallReference?.isError == false

        // if `plus` call already inapplicable then there is no need to try to resolve `set` call
        if (assignIsSuccessful && !operatorIsSuccessful) {
            return chooseAssign()
        }

        // a.set(b, a.get(b).plus(c))
        val resolvedSetCall = info.setCall
        val setCallReference = resolvedSetCall.calleeReference as? FirNamedReferenceWithCandidate
        val setIsSuccessful = setCallReference?.isError == false

        fun chooseSetOperator(): FirStatement {
            dataFlowAnalyzer.enterFunctionCall(resolvedSetCall)
            callCompleter.completeCall(resolvedSetCall, ResolutionMode.ContextIndependent)
            dataFlowAnalyzer.exitFunctionCall(resolvedSetCall, callCompleted = true)
            return info.toBlock()
        }

        fun reportError(diagnostic: ConeDiagnostic): FirStatement {
            return chooseAssign().also {
                val errorReference = buildErrorNamedReferenceWithNoName(
                    diagnostic,
                    source = indexedAccessAugmentedAssignment.source
                )
                it.replaceCalleeReference(errorReference)
            }
        }

        fun reportAmbiguity(
            firstReference: FirNamedReferenceWithCandidate?,
            secondReference: FirNamedReferenceWithCandidate?
        ): FirStatement {
            val firstCandidate = firstReference?.candidate
            val secondCandidate = secondReference?.candidate
            requireNotNull(firstCandidate)
            requireNotNull(secondCandidate)
            return reportError(ConeOperatorAmbiguityError(listOf(firstCandidate, secondCandidate)))
        }

        fun reportUnresolvedReference(): FirStatement {
            return reportError(ConeUnresolvedNameError(Name.identifier(operation.operator)))
        }

        return when {
            assignIsSuccessful && setIsSuccessful -> reportAmbiguity(assignCallReference, setCallReference)
            assignIsSuccessful -> chooseAssign()
            setIsSuccessful -> chooseSetOperator()
            else -> reportUnresolvedReference()
        }
    }

    /**
     * Desugarings of a[x, y] += z to
     * {
     *     val tmp_a = a
     *     val tmp_x = x
     *     val tmp_y = y
     *     tmp_a.set(tmp_x, tmp_y, tmp_a.get(tmp_x, tmp_y).plus(z))
     * }
     *
     * @return null if `set` or `plus` calls are unresolved
     * @return block defined as described above, otherwise
     */
    private inner class IndexedAccessAugmentedAssignmentDesugaringInfo(
        val indexedAccessAugmentedAssignment: FirIndexedAccessAugmentedAssignment,
        val arrayVariable: FirProperty,
        val indexVariables: List<FirProperty>,
        val operatorCall: FirFunctionCall,
        val setCall: FirFunctionCall
    ) {
        fun toBlock(): FirBlock {
            return buildBlock {
                annotations += indexedAccessAugmentedAssignment.annotations
                statements += arrayVariable
                statements += indexVariables
                statements += setCall
                source = indexedAccessAugmentedAssignment.source?.fakeElement(indexedAccessAugmentedAssignment.operation.toAugmentedAssignSourceKind())
            }.also {
                it.replaceConeTypeOrNull(session.builtinTypes.unitType.coneType)
            }
        }
    }

    private fun tryResolveIndexedAccessAugmentedAssignmentAsSetGetBlock(
        indexedAccessAugmentedAssignment: FirIndexedAccessAugmentedAssignment,
        lhsGetCall: FirFunctionCall,
        transformedRhs: FirExpression,
        fakeSourceElementKind: KtFakeSourceElementKind,
    ): IndexedAccessAugmentedAssignmentDesugaringInfo {
        val initializer = lhsGetCall.explicitReceiver ?: buildErrorExpression {
            source = indexedAccessAugmentedAssignment.source
                ?.fakeElement(fakeSourceElementKind)
            diagnostic = ConeSyntaxDiagnostic("No receiver for array access")
        }
        val arrayVariable = generateTemporaryVariable(
            session.moduleData,
            source = lhsGetCall.explicitReceiver?.source?.fakeElement(fakeSourceElementKind),
            name = SpecialNames.ARRAY,
            initializer = initializer,
            typeRef = initializer.resolvedType.toFirResolvedTypeRef(initializer.source?.fakeElement(fakeSourceElementKind)),
        )

        val flattenedGetCallArguments = buildList {
            for (argument in lhsGetCall.arguments) {
                if (argument is FirVarargArgumentsExpression) {
                    addAll(argument.arguments)
                } else {
                    add(argument)
                }
            }
        }
        val indexVariables = flattenedGetCallArguments.mapIndexed { i, index ->
            // If the get call arguments are SAM converted, unwrap the SAM conversion.
            // Otherwise, we might fail resolution if the get and set operator parameter types are different
            // (different SAM types or one is a SAM type and the other isn't).
            // See testData/ir/irText/expressions/callableReferences/caoWithAdaptationForSam.kt
            val unwrappedSamIndex = (index as? FirSamConversionExpression)?.expression ?: index
            generateTemporaryVariable(
                session.moduleData,
                source = unwrappedSamIndex.source?.fakeElement(fakeSourceElementKind),
                name = SpecialNames.subscribeOperatorIndex(i),
                initializer = unwrappedSamIndex,
                typeRef = unwrappedSamIndex.resolvedType.toFirResolvedTypeRef(
                    source = unwrappedSamIndex.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
                ),
            ).apply {
                // See compiler/testData/codegen/boxInline/reified/kt28234.kt
                replaceBodyResolveState(FirPropertyBodyResolveState.INITIALIZER_RESOLVED)
            }
        }

        arrayVariable.transformSingle(transformer, ResolutionMode.ContextIndependent)
        indexVariables.forEach { it.transformSingle(transformer, ResolutionMode.ContextIndependent) }

        val arrayAccess = arrayVariable.toQualifiedAccess()
        val indicesQualifiedAccess = indexVariables.map { it.toQualifiedAccess() }

        // If the get call arguments were SAM conversions, they were unwrapped for the variable initializers.
        // We need to reapply the SAM conversions here because the get call won't be completed again (where the SAM conversions could be
        // applied automatically).
        // SAM conversions will be applied automatically for the set call during completion.
        val indicesQualifiedAccessForGet = indicesQualifiedAccess.mapIndexed { index, qualifiedAccess ->
            val samConversion = flattenedGetCallArguments[index] as? FirSamConversionExpression ?: return@mapIndexed qualifiedAccess
            buildSamConversionExpressionCopy(samConversion) {
                expression = qualifiedAccess
            }
        }

        val getCall = buildFunctionCall {
            source = indexedAccessAugmentedAssignment.arrayAccessSource?.fakeElement(fakeSourceElementKind)
            explicitReceiver = arrayAccess
            if (lhsGetCall.explicitReceiver == lhsGetCall.dispatchReceiver) {
                dispatchReceiver = arrayAccess
                extensionReceiver = lhsGetCall.extensionReceiver
            } else {
                extensionReceiver = arrayAccess
                dispatchReceiver = lhsGetCall.dispatchReceiver
            }
            calleeReference = lhsGetCall.calleeReference
            var i = 0
            val newMapping = (lhsGetCall.argumentList as FirResolvedArgumentList).mapping.mapKeysTo(LinkedHashMap()) { (argument) ->
                if (argument is FirVarargArgumentsExpression) {
                    buildVarargArgumentsExpression {
                        val varargSize = argument.arguments.size
                        arguments += indicesQualifiedAccessForGet.subList(i, i + varargSize)
                        i += varargSize
                        source = argument.source
                        coneTypeOrNull = argument.resolvedType
                        coneElementTypeOrNull = argument.coneElementTypeOrNull
                    }
                } else {
                    indicesQualifiedAccessForGet[i++]
                }
            }
            argumentList = buildResolvedArgumentList(
                lhsGetCall.argumentList,
                newMapping,
            )
            origin = FirFunctionCallOrigin.Operator
            coneTypeOrNull = lhsGetCall.resolvedType
        }

        val generator = GeneratorOfPlusAssignCalls(
            indexedAccessAugmentedAssignment,
            indexedAccessAugmentedAssignment.calleeReference.source,
            indexedAccessAugmentedAssignment.operation,
            getCall,
            transformedRhs
        )

        val operatorCall = generator.createSimpleOperatorCall(indexedAccessAugmentedAssignment.operation.toAugmentedAssignSourceKind())
        val resolvedOperatorCall = operatorCall.resolveCandidateForAssignmentOperatorCall()

        val setCall = GeneratorOfPlusAssignCalls.createFunctionCall(
            OperatorNameConventions.SET,
            indexedAccessAugmentedAssignment.source?.fakeElement(indexedAccessAugmentedAssignment.operation.toAugmentedAssignSourceKind()),
            indexedAccessAugmentedAssignment.calleeReference.source,
            annotations = indexedAccessAugmentedAssignment.annotations,
            receiver = arrayAccess, // a
            *indicesQualifiedAccess.toTypedArray(), // indices
            resolvedOperatorCall // a.get(b).plus(c)
        )
        val resolvedSetCall = setCall.resolveCandidateForAssignmentOperatorCall()

        return IndexedAccessAugmentedAssignmentDesugaringInfo(
            indexedAccessAugmentedAssignment,
            arrayVariable,
            indexVariables,
            resolvedOperatorCall,
            resolvedSetCall
        )
    }

    override fun transformArrayLiteral(arrayLiteral: FirArrayLiteral, data: ResolutionMode): FirStatement =
        whileAnalysing(session, arrayLiteral) {
            when (data) {
                is ResolutionMode.WithExpectedType if data.arrayLiteralPosition != null -> {
                    // Default value of a constructor parameter inside an annotation class or an argument in an annotation call.
                    arrayLiteral.transformChildren(
                        transformer,
                        data.expectedType.arrayElementType()?.let { withExpectedType(it) }
                            ?: ResolutionMode.ContextDependent,
                    )

                    val call = components.syntheticCallGenerator.generateSyntheticArrayOfCall(
                        arrayLiteral,
                        data.expectedType,
                        resolutionContext,
                        data,
                    )
                    callCompleter.completeCall(call, data)
                    arrayOfCallTransformer.transformFunctionCall(call, session)
                }
                else -> {
                    // Other unsupported usage.
                    arrayLiteral.transformChildren(transformer, ResolutionMode.ContextDependent)
                    // We set the arrayLiteral's type to the expect type or Array<Any>
                    // because arguments need to have a type during resolution of the synthetic call.
                    // We remove the type so that it will be set during completion to the CST of the arguments.
                    arrayLiteral.replaceConeTypeOrNull(
                        (data as? ResolutionMode.WithExpectedType)?.expectedType
                            ?: StandardClassIds.Array.constructClassLikeType(arrayOf(StandardClassIds.Any.constructClassLikeType()))
                    )
                    val syntheticIdCall = components.syntheticCallGenerator.generateSyntheticIdCall(
                        arrayLiteral,
                        resolutionContext,
                        data,
                    )
                    arrayLiteral.replaceConeTypeOrNull(null)
                    callCompleter.completeCall(syntheticIdCall, ResolutionMode.ContextIndependent)
                    arrayLiteral
                }
            }
        }

    override fun transformStringConcatenationCall(
        stringConcatenationCall: FirStringConcatenationCall,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, stringConcatenationCall) {
        dataFlowAnalyzer.enterStringConcatenationCall()
        stringConcatenationCall.transformChildren(transformer, ResolutionMode.ContextIndependent)
        dataFlowAnalyzer.exitStringConcatenationCall(stringConcatenationCall)
        return stringConcatenationCall
    }

    override fun transformAnonymousObjectExpression(
        anonymousObjectExpression: FirAnonymousObjectExpression,
        data: ResolutionMode,
    ): FirStatement {
        anonymousObjectExpression.transformAnonymousObject(transformer, data)
        if (!anonymousObjectExpression.isResolved) {
            anonymousObjectExpression.resultType = anonymousObjectExpression.anonymousObject.defaultType()
        }
        dataFlowAnalyzer.exitAnonymousObjectExpression(anonymousObjectExpression)
        return anonymousObjectExpression
    }

    // ------------------------------------------------------------------------------------------------

    internal fun storeTypeFromCallee(access: FirQualifiedAccessExpression, isLhsOfAssignment: Boolean) {
        val typeFromCallee = components.typeFromCallee(access)
        access.resultType = if (isLhsOfAssignment) {
            // Attempt to use the same TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2
            // in this branch provokes questionable red-to-green change in KT-51045 test (assignToStarProjectedType.kt)
            session.typeApproximator.approximateToSubType(
                typeFromCallee, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
            )
        } else {
            session.typeApproximator.approximateToSuperType(
                typeFromCallee, TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2
            )
        } ?: typeFromCallee
    }

}

private fun FirFunctionCall.setIndexedAccessAugmentedAssignSource(fakeSourceElementKind: KtFakeSourceElementKind) {
    if (calleeReference.isError()) return
    val newSource = source?.fakeElement(fakeSourceElementKind)
    @OptIn(FirImplementationDetail::class)
    replaceSource(newSource)
    val oldCalleeReference = calleeReference as? FirResolvedNamedReference
        ?: error("${FirResolvedNamedReference::class.simpleName} expected, got ${calleeReference.render()}")
    replaceCalleeReference(buildResolvedNamedReference {
        this.name = oldCalleeReference.name
        this.source = newSource
        this.resolvedSymbol = oldCalleeReference.resolvedSymbol
    })
}

/**
 * Adds implicit receivers generated by [FirExpressionResolutionExtension.addNewImplicitReceivers]
 * for this particular [functionCall] to [this] context.
 */
@OptIn(PrivateForInline::class)
fun BodyResolveContext.addReceiversFromExtensions(functionCall: FirFunctionCall, sessionHolder: SessionHolder) {
    val extensions = sessionHolder.session.extensionService.expressionResolutionExtensions.takeIf { it.isNotEmpty() } ?: return
    val boundSymbol = this.containerIfAny?.symbol as? FirCallableSymbol<*> ?: return

    for (extension in extensions) {
        for (receiverValue in extension.addNewImplicitReceivers(functionCall, sessionHolder, boundSymbol)) {
            this.addReceiver(name = null, receiverValue)
        }
    }
}
