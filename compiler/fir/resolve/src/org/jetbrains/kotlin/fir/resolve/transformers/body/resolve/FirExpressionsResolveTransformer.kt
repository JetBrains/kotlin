/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.toAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.assignAltererExtensions
import org.jetbrains.kotlin.fir.extensions.expressionResolutionExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.*
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.inference.FirStubInferenceSession
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.replaceLambdaArgumentInvocationKinds
import org.jetbrains.kotlin.fir.scopes.impl.isWrappedIntegerOperator
import org.jetbrains.kotlin.fir.scopes.impl.isWrappedIntegerOperatorForUnsignedType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.TransformData
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.util.OperatorNameConventions

open class FirExpressionsResolveTransformer(transformer: FirAbstractBodyResolveTransformerDispatcher) :
    FirPartialBodyResolveTransformer(transformer) {
    private inline val builtinTypes: BuiltinTypes get() = session.builtinTypes
    private val arrayOfCallTransformer = FirArrayOfCallTransformer()
    var enableArrayOfCallTransformation = false
    var containingSafeCallExpression: FirSafeCallExpression? = null

    private val expressionResolutionExtensions = session.extensionService.expressionResolutionExtensions.takeIf { it.isNotEmpty() }
    private val assignAltererExtensions = session.extensionService.assignAltererExtensions.takeIf { it.isNotEmpty() }

    init {
        @Suppress("LeakingThis")
        components.callResolver.initTransformer(this)
    }

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): FirStatement {
        if (expression.resultType is FirImplicitTypeRef && expression !is FirWrappedExpression) {
            val type = buildErrorTypeRef {
                source = expression.source
                diagnostic = ConeSimpleDiagnostic(
                    "Type calculating for ${expression::class} is not supported",
                    DiagnosticKind.InferenceError
                )
            }
            expression.resultType = type
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
        transformQualifiedAccessExpression(qualifiedAccessExpression, data, isUsedAsReceiver = false)
    }

    fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ResolutionMode,
        isUsedAsReceiver: Boolean,
    ): FirStatement {
        if (qualifiedAccessExpression.typeRef is FirResolvedTypeRef && qualifiedAccessExpression.calleeReference !is FirSimpleNamedReference) {
            return qualifiedAccessExpression
        }

        qualifiedAccessExpression.transformAnnotations(this, data)
        qualifiedAccessExpression.transformTypeArguments(transformer, ResolutionMode.ContextIndependent)

        var result = when (val callee = qualifiedAccessExpression.calleeReference) {
            // TODO: there was FirExplicitThisReference
            is FirThisReference -> {
                val labelName = callee.labelName
                val implicitReceiver = implicitReceiverStack[labelName]
                implicitReceiver?.let {
                    callee.replaceBoundSymbol(it.boundSymbol)
                    if (it is ContextReceiverValue) {
                        callee.replaceContextReceiverNumber(it.contextReceiverNumber)
                    }
                }
                val implicitType = implicitReceiver?.originalType
                qualifiedAccessExpression.resultType = when {
                    implicitReceiver is InaccessibleImplicitReceiverValue -> buildErrorTypeRef {
                        source = qualifiedAccessExpression.source
                        diagnostic = ConeInstanceAccessBeforeSuperCall("<this>")
                    }
                    implicitType != null -> implicitType.toFirResolvedTypeRef(callee.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef))
                    labelName != null -> buildErrorTypeRef {
                        source = qualifiedAccessExpression.source
                        diagnostic = ConeSimpleDiagnostic("Unresolved this@$labelName", DiagnosticKind.UnresolvedLabel)
                    }
                    else -> buildErrorTypeRef {
                        source = qualifiedAccessExpression.source
                        diagnostic = ConeSimpleDiagnostic("'this' is not defined in this context", DiagnosticKind.NoThis)
                    }
                }
                qualifiedAccessExpression
            }
            is FirSuperReference -> {
                transformSuperReceiver(
                    callee,
                    qualifiedAccessExpression,
                    containingSafeCallExpression?.takeIf { qualifiedAccessExpression == it.receiver }?.selector as? FirQualifiedAccessExpression
                )
            }
            is FirDelegateFieldReference -> {
                val delegateFieldSymbol = callee.resolvedSymbol
                qualifiedAccessExpression.resultType = delegateFieldSymbol.fir.delegate!!.typeRef
                qualifiedAccessExpression
            }
            is FirResolvedNamedReference,
            is FirErrorNamedReference -> {
                if (qualifiedAccessExpression.typeRef !is FirResolvedTypeRef) {
                    storeTypeFromCallee(qualifiedAccessExpression, isLhsOfAssignment = false)
                }
                qualifiedAccessExpression
            }
            else -> {
                val transformedCallee = resolveQualifiedAccessAndSelectCandidate(
                    qualifiedAccessExpression,
                    isUsedAsReceiver,
                    // Set the correct call site. For assignment LHSs, it's the assignment, otherwise it's the qualified access itself.
                    callSite = when (data) {
                        is ResolutionMode.AssignmentLValue -> data.variableAssignment
                        else -> qualifiedAccessExpression
                    }
                )
                // NB: here we can get raw expression because of dropped qualifiers (see transform callee),
                // so candidate existence must be checked before calling completion
                if (transformedCallee is FirQualifiedAccessExpression && transformedCallee.candidate() != null) {
                    if (!transformedCallee.isAcceptableResolvedQualifiedAccess()) {
                        return qualifiedAccessExpression
                    }
                    callCompleter.completeCall(transformedCallee, data).result
                } else {
                    transformedCallee
                }
            }
        }

        // If we're resolving the LHS of an assignment, skip DFA to prevent the access being treated as a variable read and
        // smart-casts being applied.
        if (data !is ResolutionMode.AssignmentLValue) {
            when (result) {
                is FirQualifiedAccessExpression -> {
                    dataFlowAnalyzer.exitQualifiedAccessExpression(result)
                    result = components.transformQualifiedAccessUsingSmartcastInfo(result)
                    if (result is FirSmartCastExpression) {
                        dataFlowAnalyzer.exitSmartCastExpression(result)
                    }
                }
                is FirResolvedQualifier -> {
                    dataFlowAnalyzer.exitResolvedQualifierNode(result)
                }
            }
        }
        return result
    }

    fun <Q : FirQualifiedAccessExpression> transformExplicitReceiver(qualifiedAccessExpression: Q): Q {
        val explicitReceiver = qualifiedAccessExpression.explicitReceiver as? FirQualifiedAccessExpression
        if (explicitReceiver is FirQualifiedAccessExpression) {
            val superReference = explicitReceiver.calleeReference as? FirSuperReference
            if (superReference != null) {
                transformSuperReceiver(superReference, explicitReceiver, qualifiedAccessExpression)
                return qualifiedAccessExpression
            }
        }
        if (explicitReceiver is FirPropertyAccessExpression) {
            qualifiedAccessExpression.replaceExplicitReceiver(
                transformQualifiedAccessExpression(
                    explicitReceiver, ResolutionMode.ReceiverResolution, isUsedAsReceiver = true
                ) as FirExpression
            )
            return qualifiedAccessExpression
        }
        @Suppress("UNCHECKED_CAST")
        return qualifiedAccessExpression.transformExplicitReceiver(transformer, ResolutionMode.ReceiverResolution) as Q
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
        callSite: FirElement,
    ): FirStatement {
        return callResolver.resolveVariableAccessAndSelectCandidate(qualifiedAccessExpression, isUsedAsReceiver, callSite)
    }

    fun transformSuperReceiver(
        superReference: FirSuperReference,
        superReferenceContainer: FirQualifiedAccessExpression,
        containingCall: FirQualifiedAccessExpression?
    ): FirQualifiedAccessExpression {
        val labelName = superReference.labelName
        val lastDispatchReceiver = implicitReceiverStack.lastDispatchReceiver()
        val implicitReceiver =
            // Only report label issues if the label is set and the receiver stack is not empty
            if (labelName != null && lastDispatchReceiver != null) {
                val labeledReceiver = implicitReceiverStack[labelName] as? ImplicitDispatchReceiverValue
                if (labeledReceiver == null) {
                    return markSuperReferenceError(
                        ConeSimpleDiagnostic("Unresolved label", DiagnosticKind.UnresolvedLabel),
                        superReferenceContainer,
                        superReference
                    )
                }
                labeledReceiver
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
                    if (implicitReceiverStack.lastOrNull() is InaccessibleImplicitReceiverValue) {
                        ConeInstanceAccessBeforeSuperCall("<super>")
                    } else {
                        ConeSimpleDiagnostic("Super not available", DiagnosticKind.SuperNotAvailable)
                    }
                return markSuperReferenceError(diagnostic, superReferenceContainer, superReference)
            }
            superTypeRef is FirResolvedTypeRef -> {
                superReferenceContainer.resultType = superTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.SuperCallExplicitType)
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
                }
                superReferenceContainer.resultType =
                    actualSuperTypeRef.copyWithNewSourceKind(KtFakeSourceElementKind.SuperCallExplicitType)
                superReference.replaceSuperTypeRef(actualSuperTypeRef)
            }
            else -> {
                val types = components.findTypesForSuperCandidates(superTypeRefs, containingCall)
                val resultType = when (types.size) {
                    0 -> buildErrorTypeRef {
                        source = superReferenceContainer.source
                        // Report stub error so that it won't surface up. Instead, errors on the callee would be reported.
                        diagnostic = ConeStubDiagnostic(ConeSimpleDiagnostic("Unresolved super method", DiagnosticKind.Other))
                    }
                    1 -> types.single().toFirResolvedTypeRef(superReferenceContainer.source?.fakeElement(KtFakeSourceElementKind.SuperCallImplicitType))
                    else -> buildErrorTypeRef {
                        source = superReferenceContainer.source
                        diagnostic = ConeAmbiguousSuper(types)
                    }
                }
                superReferenceContainer.resultType =
                    resultType.copyWithNewSourceKind(KtFakeSourceElementKind.SuperCallExplicitType)
                superReference.replaceSuperTypeRef(resultType)
            }
        }
        return superReferenceContainer
    }

    private fun markSuperReferenceError(
        superNotAvailableDiagnostic: ConeDiagnostic,
        superReferenceContainer: FirQualifiedAccessExpression,
        superReference: FirSuperReference
    ): FirQualifiedAccessExpression {
        val resultType = buildErrorTypeRef {
            diagnostic = superNotAvailableDiagnostic
        }
        superReferenceContainer.resultType = resultType
        superReference.replaceSuperTypeRef(resultType)
        superReferenceContainer.replaceCalleeReference(buildErrorNamedReference {
            source = superReferenceContainer.source?.fakeElement(KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess)
            diagnostic = superNotAvailableDiagnostic
        })
        return superReferenceContainer
    }

    protected open fun FirQualifiedAccessExpression.isAcceptableResolvedQualifiedAccess(): Boolean {
        return true
    }

    override fun transformSafeCallExpression(
        safeCallExpression: FirSafeCallExpression,
        data: ResolutionMode
    ): FirStatement {
        whileAnalysing(session, safeCallExpression) {
            withContainingSafeCallExpression(safeCallExpression) {
                safeCallExpression.transformAnnotations(this, ResolutionMode.ContextIndependent)
                safeCallExpression.transformReceiver(this, ResolutionMode.ContextIndependent)

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
        whileAnalysing(session, functionCall) {
            val calleeReference = functionCall.calleeReference
            if (
                (calleeReference is FirResolvedNamedReference || calleeReference is FirErrorNamedReference) &&
                functionCall.resultType is FirImplicitTypeRef
            ) {
                storeTypeFromCallee(functionCall, isLhsOfAssignment = false)
            }
            if (calleeReference is FirNamedReferenceWithCandidate) return functionCall
            if (calleeReference !is FirSimpleNamedReference) {
                // The callee reference can be resolved as an error very early, e.g., `super` as a callee during raw FIR creation.
                // We still need to visit/transform other parts, e.g., call arguments, to check if any other errors are there.
                if (calleeReference !is FirResolvedNamedReference) {
                    functionCall.transformChildren(transformer, data)
                }
                return functionCall
            }
            functionCall.transformAnnotations(transformer, data)
            functionCall.replaceLambdaArgumentInvocationKinds(session)
            functionCall.transformTypeArguments(transformer, ResolutionMode.ContextIndependent)
            val (completeInference, callCompleted) =
                run {
                    val initialExplicitReceiver = functionCall.explicitReceiver
                    val withTransformedArguments = if (!resolvingAugmentedAssignment) {
                        dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.arguments)
                        transformExplicitReceiver(functionCall).also {
                            it.replaceArgumentList(it.argumentList.transform(this, ResolutionMode.ContextDependent))
                            dataFlowAnalyzer.exitCallArguments()
                        }
                    } else {
                        functionCall
                    }
                    val resultExpression = callResolver.resolveCallAndSelectCandidate(withTransformedArguments)
                    val resultExplicitReceiver = resultExpression.explicitReceiver?.unwrapSmartcastExpression()
                    if (initialExplicitReceiver !== resultExplicitReceiver && resultExplicitReceiver is FirQualifiedAccessExpression) {
                        // name.invoke() case
                        callCompleter.completeCall(resultExplicitReceiver, ResolutionMode.ContextIndependent)
                    }
                    callCompleter.completeCall(resultExpression, data)
                }
            val result = completeInference.transformToIntegerOperatorCallOrApproximateItIfNeeded(data)
            if (!resolvingAugmentedAssignment) {
                dataFlowAnalyzer.exitFunctionCall(result, callCompleted)
            }

            addReceiversFromExtensions(result)

            if (callCompleted) {
                if (enableArrayOfCallTransformation) {
                    return arrayOfCallTransformer.transformFunctionCall(result, null)
                }
            }
            return result
        }

    @OptIn(PrivateForInline::class)
    private fun addReceiversFromExtensions(functionCall: FirFunctionCall) {
        val extensions = expressionResolutionExtensions ?: return
        val boundSymbol = context.containerIfAny?.symbol as? FirCallableSymbol<*> ?: return
        for (extension in extensions) {
            for (receiverType in extension.addNewImplicitReceivers(functionCall)) {
                val receiverValue = ImplicitExtensionReceiverValue(
                    boundSymbol,
                    receiverType,
                    session,
                    scopeSession
                )
                context.addReceiver(name = null, receiverValue)
            }
        }
    }

    private fun FirFunctionCall.transformToIntegerOperatorCallOrApproximateItIfNeeded(resolutionMode: ResolutionMode): FirFunctionCall {
        if (!explicitReceiver.isIntegerLiteralOrOperatorCall()) return this
        val resolvedSymbol = calleeReference.toResolvedFunctionSymbol() ?: return this
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
            ConeNullability.NOT_NULL
        )

        val approximationIsNeeded =
            resolutionMode !is ResolutionMode.ReceiverResolution && resolutionMode !is ResolutionMode.ContextDependent

        val integerOperatorCall = buildIntegerLiteralOperatorCall {
            source = originalCall.source
            typeRef = originalCall.typeRef.resolvedTypeFromPrototype(integerOperatorType)
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
                resolutionMode.expectedType?.coneTypeSafe()
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
                        data.copy(mayBeCoercionToUnitApplied = true)
                    else
                        data
                else
                    ResolutionMode.ContextIndependent
            transformer.firTowerDataContextCollector?.addStatementContext(block.statements[index], context.towerDataContext)
            TransformData.Data(value)
        }
        block.transformOtherChildren(transformer, data)
        if (data is ResolutionMode.WithExpectedType && data.expectedTypeRef.coneTypeSafe<ConeKotlinType>()?.isUnitOrFlexibleUnit == true) {
            // Unit-coercion
            block.resultType = data.expectedTypeRef
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
            it.resultType = comparisonExpression.typeRef.resolvedTypeFromPrototype(builtinTypes.booleanType.type)
            dataFlowAnalyzer.exitComparisonExpressionCall(it)
        }
    }

    @OptIn(FirContractViolation::class)
    override fun transformAssignmentOperatorStatement(
        assignmentOperatorStatement: FirAssignmentOperatorStatement,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, assignmentOperatorStatement) {
        val operation = assignmentOperatorStatement.operation
        require(operation != FirOperation.ASSIGN)

        assignmentOperatorStatement.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        dataFlowAnalyzer.enterCallArguments(assignmentOperatorStatement, listOf(assignmentOperatorStatement.rightArgument))
        val leftArgument = assignmentOperatorStatement.leftArgument.transformSingle(transformer, ResolutionMode.ContextIndependent)
        val rightArgument = assignmentOperatorStatement.rightArgument.transformSingle(transformer, ResolutionMode.ContextDependent)
        dataFlowAnalyzer.exitCallArguments()

        val generator = GeneratorOfPlusAssignCalls(assignmentOperatorStatement, operation, leftArgument, rightArgument)

        // x.plusAssign(y)
        val assignOperatorCall = generator.createAssignOperatorCall()
        val resolvedAssignCall = resolveCandidateForAssignmentOperatorCall {
            assignOperatorCall.transformSingle(this, ResolutionMode.ContextDependent)
        }
        val assignCallReference = resolvedAssignCall.calleeReference as? FirNamedReferenceWithCandidate
        val assignIsSuccessful = assignCallReference?.isError == false

        // x = x + y
        val simpleOperatorCall = generator.createSimpleOperatorCall()
        val resolvedOperatorCall = resolveCandidateForAssignmentOperatorCall {
            simpleOperatorCall.transformSingle(this, ResolutionMode.ContextDependent)
        }
        val operatorCallReference = resolvedOperatorCall.calleeReference as? FirNamedReferenceWithCandidate
        val operatorIsSuccessful = operatorCallReference?.isError == false

        fun operatorReturnTypeMatches(candidate: Candidate): Boolean {
            // After KT-45503, non-assign flavor of operator is checked more strictly: the return type must be assignable to the variable.
            val operatorCallReturnType = resolvedOperatorCall.typeRef.coneType
            val substitutor = candidate.system.currentStorage()
                .buildAbstractResultingSubstitutor(candidate.system.typeSystemContext) as ConeSubstitutor
            return AbstractTypeChecker.isSubtypeOf(
                session.typeContext,
                substitutor.substituteOrSelf(operatorCallReturnType),
                leftArgument.typeRef.coneType
            )
        }
        // following `!!` is safe since `operatorIsSuccessful = true` implies `operatorCallReference != null`
        val operatorReturnTypeMatches = operatorIsSuccessful && operatorReturnTypeMatches(operatorCallReference!!.candidate)

        val lhsReference = leftArgument.toReference()
        val lhsSymbol = lhsReference?.toResolvedVariableSymbol()
        val lhsVariable = lhsSymbol?.fir
        val lhsIsVar = lhsVariable?.isVar == true

        fun chooseAssign(): FirStatement {
            callCompleter.completeCall(resolvedAssignCall, ResolutionMode.ContextIndependent)
            dataFlowAnalyzer.exitFunctionCall(resolvedAssignCall, callCompleted = true)
            return resolvedAssignCall
        }

        fun chooseOperator(): FirStatement {
            callCompleter.completeCall(
                resolvedOperatorCall,
                (lhsVariable?.returnTypeRef as? FirResolvedTypeRef)?.let {
                    ResolutionMode.WithExpectedType(it, expectedTypeMismatchIsReportedInChecker = true)
                } ?: ResolutionMode.ContextIndependent,
            )
            dataFlowAnalyzer.exitFunctionCall(resolvedOperatorCall, callCompleted = true)

            val unwrappedLeftArgument = leftArgument.unwrapSmartcastExpression()
            val assignmentLeftArgument = buildDesugaredAssignmentValueReferenceExpression {
                expressionRef = FirExpressionRef<FirExpression>().apply { bind(unwrappedLeftArgument) }
                source = leftArgument.source?.fakeElement(KtFakeSourceElementKind.DesugaredCompoundAssignment)
            }

            val assignment =
                buildVariableAssignment {
                    source = assignmentOperatorStatement.source
                    lValue = assignmentLeftArgument
                    rValue = resolvedOperatorCall
                    annotations += assignmentOperatorStatement.annotations
                }
            return assignment.transform(transformer, ResolutionMode.ContextIndependent)
        }

        fun reportAmbiguity(): FirStatement {
            val operatorCallCandidate = operatorCallReference?.candidate
            val assignmentCallCandidate = assignCallReference?.candidate

            requireNotNull(operatorCallCandidate)
            requireNotNull(assignmentCallCandidate)

            return buildErrorExpression {
                source = assignmentOperatorStatement.source
                diagnostic = ConeOperatorAmbiguityError(listOf(operatorCallCandidate, assignmentCallCandidate))
            }
        }

        return when {
            assignIsSuccessful && !lhsIsVar -> chooseAssign()
            !assignIsSuccessful && !operatorIsSuccessful -> {
                // If neither candidate is successful, choose whichever is resolved, prioritizing assign
                val isAssignResolved = (assignCallReference as? FirErrorReferenceWithCandidate)?.diagnostic !is ConeUnresolvedNameError
                val isOperatorResolved = (operatorCallReference as? FirErrorReferenceWithCandidate)?.diagnostic !is ConeUnresolvedNameError
                when {
                    isAssignResolved -> chooseAssign()
                    isOperatorResolved -> chooseOperator()
                    else -> chooseAssign()
                }
            }
            !assignIsSuccessful && operatorIsSuccessful -> chooseOperator()
            assignIsSuccessful && !operatorIsSuccessful -> chooseAssign()
            leftArgument.typeRef.coneType is ConeDynamicType -> chooseAssign()
            !operatorReturnTypeMatches -> chooseAssign()
            else -> reportAmbiguity()
        }
    }

    @OptIn(FirContractViolation::class)
    override fun transformIncrementDecrementExpression(
        incrementDecrementExpression: FirIncrementDecrementExpression,
        data: ResolutionMode
    ): FirStatement {
        incrementDecrementExpression.transformAnnotations(transformer, ResolutionMode.ContextIndependent)

        val originalExpression = incrementDecrementExpression.expression.transformSingle(transformer, ResolutionMode.ContextIndependent)
        val expression = when (originalExpression) {
            is FirSafeCallExpression -> originalExpression.selector as? FirExpression ?: buildErrorExpression {
                source = originalExpression.source
                diagnostic = ConeSimpleDiagnostic("Safe call selector expected to be an expression here", DiagnosticKind.Syntax)
            }
            else -> originalExpression
        }

        val desugaredSource = incrementDecrementExpression.source?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement)

        fun generateTemporaryVariable(name: Name, initializer: FirExpression): FirProperty = generateTemporaryVariable(
            moduleData = session.moduleData,
            source = desugaredSource,
            name = name,
            initializer = initializer,
            typeRef = initializer.typeRef.copyWithNewSource(desugaredSource),
        )

        fun buildAndResolveOperatorCall(receiver: FirExpression): FirFunctionCall = buildFunctionCall {
            source = incrementDecrementExpression.operationSource
            explicitReceiver = receiver
            calleeReference = buildSimpleNamedReference {
                val referenceSourceKind = when {
                    incrementDecrementExpression.isPrefix -> KtFakeSourceElementKind.DesugaredPrefixNameReference
                    else -> KtFakeSourceElementKind.DesugaredPostfixNameReference
                }
                source = incrementDecrementExpression.operationSource?.fakeElement(referenceSourceKind)
                name = incrementDecrementExpression.operationName
            }
            origin = FirFunctionCallOrigin.Operator
        }.transformSingle(transformer, ResolutionMode.ContextIndependent)

        fun buildAndResolveVariableAssignment(rValue: FirExpression): FirVariableAssignment = buildVariableAssignment {
            source = desugaredSource
            lValue = buildDesugaredAssignmentValueReferenceExpression {
                source = ((expression as? FirErrorExpression)?.expression ?: expression).source
                    ?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement)
                expressionRef = FirExpressionRef<FirExpression>().apply { bind(expression.unwrapSmartcastExpression()) }
            }
            this.rValue = rValue
        }.transformSingle(transformer, ResolutionMode.ContextIndependent)

        val block = buildBlock {
            source = desugaredSource
            annotations += incrementDecrementExpression.annotations

            (expression as? FirQualifiedAccessExpression)?.explicitReceiver
                // If a receiver x exists, write it to a temporary variable to prevent multiple calls to it.
                // Exceptions: ResolvedQualifiers and ThisReceivers as they can't have side effects when called.
                ?.takeIf { it is FirQualifiedAccessExpression && it !is FirThisReceiverExpression }
                ?.let { receiver ->
                    // val <receiver> = x
                    statements += generateTemporaryVariable(SpecialNames.RECEIVER, receiver).also { property ->
                        // Change the expression from x.a to <receiver>.a
                        val newReceiverAccess =
                            property.toQualifiedAccess(fakeSource = receiver.source?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement))

                        if (expression.explicitReceiver == expression.dispatchReceiver) {
                            expression.replaceDispatchReceiver(newReceiverAccess)
                        } else {
                            expression.replaceExtensionReceiver(newReceiverAccess)
                        }
                        expression.replaceExplicitReceiver(newReceiverAccess)
                    }
                }

            if (incrementDecrementExpression.isPrefix) {
                // a = a.inc()
                statements += buildAndResolveVariableAssignment(buildAndResolveOperatorCall(expression))
                // ^a
                statements += buildDesugaredAssignmentValueReferenceExpression {
                    source = ((expression as? FirErrorExpression)?.expression ?: expression).source
                        ?.fakeElement(KtFakeSourceElementKind.DesugaredIncrementOrDecrement)
                    expressionRef = FirExpressionRef<FirExpression>().apply { bind(expression.unwrapSmartcastExpression()) }
                }.let {
                    it.transform<FirStatement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
                    components.transformDesugaredAssignmentValueUsingSmartcastInfo(it)
                }
            } else {
                val unaryVariable = generateTemporaryVariable(SpecialNames.UNARY, expression)

                // val <unary> = a
                statements += unaryVariable
                // a = <unary>.inc()
                statements += buildAndResolveVariableAssignment(buildAndResolveOperatorCall(unaryVariable.toQualifiedAccess()))
                // ^<unary>
                statements += unaryVariable.toQualifiedAccess()
            }
        }.apply {
            replaceTypeRef((statements.last() as FirExpression).typeRef.copyWithNewSource(null))
        }

        return if (originalExpression is FirSafeCallExpression) {
            originalExpression.replaceSelector(block)
            originalExpression
        } else {
            block
        }
    }

    override fun transformEqualityOperatorCall(
        equalityOperatorCall: FirEqualityOperatorCall,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, equalityOperatorCall) {
        // Currently, we use expectedType=Any? for both operands
        // In FE1.0, it's only used for the right
        // But it seems a bit inconsistent (see KT-47409)
        // Also it's kind of complicated to transform different arguments with different expectType considering current FIR structure
        equalityOperatorCall
            .transformAnnotations(transformer, ResolutionMode.ContextIndependent)
            .replaceArgumentList(equalityOperatorCall.argumentList.transform(transformer, withExpectedType(builtinTypes.nullableAnyType)))
        equalityOperatorCall.resultType = equalityOperatorCall.typeRef.resolvedTypeFromPrototype(builtinTypes.booleanType.type)

        dataFlowAnalyzer.exitEqualityOperatorCall(equalityOperatorCall)
        return equalityOperatorCall
    }

    private var resolvingAugmentedAssignment: Boolean = false

    private inline fun <T> resolveCandidateForAssignmentOperatorCall(block: () -> T): T {
        assert(!resolvingAugmentedAssignment)
        resolvingAugmentedAssignment = true
        return try {
            context.withInferenceSession(InferenceSessionForAssignmentOperatorCall) {
                block()
            }
        } finally {
            resolvingAugmentedAssignment = false
        }
    }

    private object InferenceSessionForAssignmentOperatorCall : FirStubInferenceSession() {
        override fun <T> shouldRunCompletion(call: T): Boolean where T : FirStatement, T : FirResolvable = false
    }

    private fun FirTypeRef.withTypeArgumentsForBareType(argument: FirExpression, operation: FirOperation): FirTypeRef {
        val type = coneTypeSafe<ConeClassLikeType>() ?: return this
        if (type.typeArguments.isNotEmpty()) return this // TODO: Incorrect for local classes.
        // TODO: Check equality of size of arguments and parameters?

        val firClass = type.lookupTag.toSymbol(session)?.fir ?: return this
        if (firClass.typeParameters.isEmpty()) return this

        val originalType = argument.unwrapSmartcastExpression().typeRef.coneTypeSafe<ConeKotlinType>() ?: return this
        val newType = components.computeRepresentativeTypeForBareType(type, originalType)
            ?: if (firClass.isLocal && (operation == FirOperation.AS || operation == FirOperation.SAFE_AS)) {
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
            if (typeOperatorCall.operation == FirOperation.IS || typeOperatorCall.operation == FirOperation.NOT_IS) {
                components.typeResolverTransformer.withIsOperandOfIsOperator {
                    typeOperatorCall.transformConversionTypeRef(transformer, ResolutionMode.ContextIndependent)
                }
            } else {
                typeOperatorCall.transformConversionTypeRef(transformer, ResolutionMode.ContextIndependent)
            }
        }.transformTypeOperatorCallChildren()

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
            FirOperation.IS, FirOperation.NOT_IS -> {
                resolved.resultType = session.builtinTypes.booleanType
            }
            FirOperation.AS -> {
                resolved.resultType = buildResolvedTypeRef {
                    source = conversionTypeRef.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
                    type = conversionTypeRef.coneType
                    annotations += conversionTypeRef.annotations
                }
            }
            FirOperation.SAFE_AS -> {
                resolved.resultType =
                    conversionTypeRef.withReplacedConeType(
                        conversionTypeRef.coneType.withNullability(
                            ConeNullability.NULLABLE, session.typeContext,
                        ),
                    )
            }
            else -> error("Unknown type operator: ${resolved.operation}")
        }
        dataFlowAnalyzer.exitTypeOperatorCall(resolved)
        return resolved
    }

    private fun FirTypeOperatorCall.transformTypeOperatorCallChildren(): FirTypeOperatorCall {
        if (operation == FirOperation.AS || operation == FirOperation.SAFE_AS) {
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
                            (it.lookupTag.toSymbol(session)?.fir as? FirTypeParameterRefsOwner)?.typeParameters?.isEmpty() == true
                }?.let {
                    if (operation == FirOperation.SAFE_AS)
                        it.withNullability(ConeNullability.NULLABLE, session.typeContext)
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

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ResolutionMode,
    ): FirStatement {
        // Resolve the return type of a call to the synthetic function with signature:
        //   fun <K> checkNotNull(arg: K?): K
        // ...in order to get the not-nullable type of the argument.

        if (checkNotNullCall.calleeReference is FirResolvedNamedReference && checkNotNullCall.resultType !is FirImplicitTypeRef) {
            return checkNotNullCall
        }

        dataFlowAnalyzer.enterCheckNotNullCall()
        checkNotNullCall
            .transformAnnotations(transformer, ResolutionMode.ContextIndependent)
            .replaceArgumentList(checkNotNullCall.argumentList.transform(transformer, ResolutionMode.ContextDependent))

        var callCompleted = false
        val result = components.syntheticCallGenerator.generateCalleeForCheckNotNullCall(checkNotNullCall, resolutionContext)?.let {
            val completionResult = callCompleter.completeCall(it, data)
            callCompleted = completionResult.callCompleted
            completionResult.result
        } ?: run {
            checkNotNullCall.resultType =
                buildErrorTypeRef {
                    diagnostic = ConeSimpleDiagnostic("Can't resolve !! operator call", DiagnosticKind.InferenceError)
                }
            callCompleted = true
            checkNotNullCall
        }
        dataFlowAnalyzer.exitCheckNotNullCall(result, callCompleted)
        return result
    }

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: ResolutionMode,
    ): FirStatement = whileAnalysing(session, binaryLogicExpression) {
        val booleanType = binaryLogicExpression.typeRef.resolvedTypeFromPrototype(builtinTypes.booleanType.type)
        return binaryLogicExpression.also(dataFlowAnalyzer::enterBinaryLogicExpression)
            .transformLeftOperand(this, ResolutionMode.WithExpectedType(booleanType))
            .also(dataFlowAnalyzer::exitLeftBinaryLogicExpressionArgument)
            .transformRightOperand(this, ResolutionMode.WithExpectedType(booleanType))
            .also(dataFlowAnalyzer::exitBinaryLogicExpression)
            .transformOtherChildren(transformer, ResolutionMode.WithExpectedType(booleanType))
            .also { it.resultType = booleanType }
    }

    override fun transformDesugaredAssignmentValueReferenceExpression(
        desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression,
        data: ResolutionMode
    ): FirStatement {
        val referencedExpression = desugaredAssignmentValueReferenceExpression.expressionRef.value
        if (referencedExpression is FirQualifiedAccessExpression) {
            val typeFromCallee = components.typeFromCallee(referencedExpression)
            desugaredAssignmentValueReferenceExpression.resultType = typeFromCallee.withReplacedConeType(
                session.typeApproximator.approximateToSubType(
                    typeFromCallee.type,
                    TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
                )
            )
        } else {
            desugaredAssignmentValueReferenceExpression.resultType =
                referencedExpression.resultType.copyWithNewSourceKind(KtFakeSourceElementKind.ImplicitTypeRef)
        }
        return desugaredAssignmentValueReferenceExpression
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

        val result = variableAssignment.transformRValue(
            transformer,
            withExpectedType(variableAssignment.lValue.typeRef, expectedTypeMismatchIsReportedInChecker = true),
        )

        (result as? FirVariableAssignment)?.let { dataFlowAnalyzer.exitVariableAssignment(it) }

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
        val transformedLHS = when (explicitReceiver) {
            is FirPropertyAccessExpression ->
                transformQualifiedAccessExpression(
                    explicitReceiver, ResolutionMode.ContextIndependent, isUsedAsReceiver = true
                ) as FirExpression
            else ->
                explicitReceiver?.transformSingle(this, ResolutionMode.ContextIndependent)
        }.apply {
            if (this is FirResolvedQualifier && callableReferenceAccess.hasQuestionMarkAtLHS) {
                replaceIsNullableLHSForCallableReference(true)
            }
        }

        transformedLHS?.let { callableReferenceAccess.replaceExplicitReceiver(transformedLHS) }

        return when (data) {
            is ResolutionMode.ContextDependent -> {
                context.storeCallableReferenceContext(callableReferenceAccess)
                callableReferenceAccess
            }

            else -> {
                components.syntheticCallGenerator.resolveCallableReferenceWithSyntheticOuterCall(
                    callableReferenceAccess, data.expectedType, resolutionContext,
                ) ?: callableReferenceAccess
            }
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
        val dataForLhs = if (arg is FirConstExpression<*>) {
            withExpectedType(arg.typeRef.resolvedTypeFromPrototype(arg.kind.expectedConeType(session)))
        } else {
            ResolutionMode.ContextIndependent
        }

        val transformedGetClassCall = run {
            val argument = getClassCall.argument
            val replacedArgument: FirExpression =
                if (argument is FirPropertyAccessExpression)
                    transformQualifiedAccessExpression(argument, dataForLhs, isUsedAsReceiver = true) as FirExpression
                else
                    argument.transform(this, dataForLhs)

            getClassCall.argumentList.transformArguments(object : FirTransformer<Nothing?>() {
                @Suppress("UNCHECKED_CAST")
                override fun <E : FirElement> transformElement(element: E, data: Nothing?): E = replacedArgument as E
            }, null)

            getClassCall
        }

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
                val typeRef = symbol?.constructType(typeArguments, isNullable = false)
                if (typeRef != null) {
                    lhs.replaceTypeRef(
                        buildResolvedTypeRef { type = typeRef }.also {
                            session.lookupTracker?.recordTypeResolveAsLookup(it, getClassCall.source, components.file.source)
                        }
                    )
                    typeRef
                } else {
                    lhs.resultType.coneType
                }
            }
            is FirResolvedReifiedParameterReference -> {
                val symbol = lhs.symbol
                symbol.constructType(emptyArray(), isNullable = false)
            }
            else -> {
                if (!shouldComputeTypeOfGetClassCallWithNotQualifierInLhs(getClassCall)) return transformedGetClassCall
                val resultType = lhs.resultType
                if (resultType is FirErrorTypeRef) {
                    resultType.coneType
                } else {
                    ConeKotlinTypeProjectionOut(resultType.coneType)
                }
            }
        }

        transformedGetClassCall.resultType =
            buildResolvedTypeRef {
                type = StandardClassIds.KClass.constructClassLikeType(arrayOf(typeOfExpression), false)
            }
        dataFlowAnalyzer.exitGetClassCall(transformedGetClassCall)
        return transformedGetClassCall
    }

    protected open fun shouldComputeTypeOfGetClassCallWithNotQualifierInLhs(getClassCall: FirGetClassCall): Boolean {
        return true
    }

    override fun <T> transformConstExpression(
        constExpression: FirConstExpression<T>,
        data: ResolutionMode,
    ): FirStatement {
        constExpression.transformAnnotations(transformer, ResolutionMode.ContextIndependent)

        val type = when (val kind = constExpression.kind) {
            ConstantValueKind.IntegerLiteral, ConstantValueKind.UnsignedIntegerLiteral -> {
                val expressionType = ConeIntegerLiteralConstantTypeImpl.create(
                    constExpression.value as Long,
                    isTypePresent = { it.lookupTag.toSymbol(session) != null },
                    isUnsigned = kind == ConstantValueKind.UnsignedIntegerLiteral
                )
                val expectedTypeRef = data.expectedType
                @Suppress("UNCHECKED_CAST")
                when {
                    expressionType is ConeErrorType -> {
                        expressionType
                    }
                    expressionType is ConeClassLikeType -> {
                        constExpression.replaceKind(expressionType.toConstKind() as ConstantValueKind<T>)
                        expressionType
                    }
                    data is ResolutionMode.ReceiverResolution -> {
                        require(expressionType is ConeIntegerLiteralConstantTypeImpl)
                        ConeIntegerConstantOperatorTypeImpl(expressionType.isUnsigned, ConeNullability.NOT_NULL)
                    }
                    expectedTypeRef != null -> {
                        require(expressionType is ConeIntegerLiteralConstantTypeImpl)
                        val coneType = expectedTypeRef.coneTypeSafe<ConeKotlinType>()?.fullyExpandedType(session)
                        val approximatedType = expressionType.getApproximatedType(coneType)
                        constExpression.replaceKind(approximatedType.toConstKind() as ConstantValueKind<T>)
                        approximatedType
                    }
                    else -> {
                        expressionType
                    }
                }
            }
            else -> kind.expectedConeType(session)
        }

        dataFlowAnalyzer.exitConstExpression(constExpression as FirConstExpression<*>)
        constExpression.resultType = constExpression.resultType.resolvedTypeFromPrototype(type)

        return when (val resolvedType = constExpression.resultType.coneType) {
            is ConeErrorType -> buildErrorExpression {
                expression = constExpression
                diagnostic = resolvedType.diagnostic
                source = constExpression.source
            }

            else -> constExpression
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
        return context.forAnnotation {
            withFirArrayOfCallTransformer {
                dataFlowAnalyzer.enterAnnotation()
                val result = callResolver.resolveAnnotationCall(annotationCall)
                dataFlowAnalyzer.exitAnnotation()
                if (result == null) return annotationCall
                callCompleter.completeCall(result, ResolutionMode.ContextIndependent)
                (result.argumentList as FirResolvedArgumentList).let { annotationCall.replaceArgumentMapping((it).toAnnotationArgumentMapping()) }
                annotationCall
            }
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
        val lastDispatchReceiver = implicitReceiverStack.lastDispatchReceiver()
        context.forDelegatedConstructorCall(containingConstructor, containingClass as? FirRegularClass, components) {
            delegatedConstructorCall.transformChildren(transformer, ResolutionMode.ContextDependent)
        }
        dataFlowAnalyzer.exitCallArguments()

        val reference = delegatedConstructorCall.calleeReference
        val constructorType: ConeClassLikeType? = when (reference) {
            is FirThisReference -> lastDispatchReceiver?.type as? ConeClassLikeType
            is FirSuperReference -> reference.superTypeRef
                .coneTypeSafe<ConeClassLikeType>()
                ?.takeIf { it !is ConeErrorType }
                ?.fullyExpandedType(session)
            else -> null
        }

        val resolvedCall = callResolver
            .resolveDelegatingConstructorCall(delegatedConstructorCall, constructorType, containingClass.symbol.toLookupTag())

        if (reference is FirThisReference && reference.boundSymbol == null) {
            resolvedCall.dispatchReceiver.typeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.toSymbol(session)?.let {
                reference.replaceBoundSymbol(it)
            }
        }

        // it seems that we may leave this code as is
        // without adding `context.withTowerDataContext(context.getTowerDataContextForConstructorResolution())`
        val (result, callCompleted) = callCompleter.completeCall(resolvedCall, ResolutionMode.ContextIndependent)
        dataFlowAnalyzer.exitDelegatedConstructorCall(result, callCompleted)
        return result
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
        val operation: FirOperation,
        val lhs: FirExpression,
        val rhs: FirExpression
    ) {
        companion object {
            fun createFunctionCall(
                name: Name,
                source: KtSourceElement?,
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
                    // TODO: Use source of operator for callee reference source
                    this.source = source
                    this.name = name
                }
                origin = FirFunctionCallOrigin.Operator
            }
        }

        private fun createFunctionCall(name: Name): FirFunctionCall {
            return createFunctionCall(name, baseElement.source?.fakeElement(KtFakeSourceElementKind.DesugaredCompoundAssignment), lhs, rhs)
        }

        fun createAssignOperatorCall(): FirFunctionCall {
            return createFunctionCall(FirOperationNameConventions.ASSIGNMENTS.getValue(operation))
        }

        fun createSimpleOperatorCall(): FirFunctionCall {
            return createFunctionCall(FirOperationNameConventions.ASSIGNMENTS_TO_SIMPLE_OPERATOR.getValue(operation))
        }
    }

    override fun transformAugmentedArraySetCall(
        augmentedArraySetCall: FirAugmentedArraySetCall,
        data: ResolutionMode
    ): FirStatement = whileAnalysing(session, augmentedArraySetCall) {
        /*
         * a[b] += c can be desugared to:
         *
         * 1. a.get(b).plusAssign(c)
         * 2. a.set(b, a.get(b).plus(c))
         */

        val operation = augmentedArraySetCall.operation
        assert(operation in FirOperation.ASSIGNMENTS)
        assert(operation != FirOperation.ASSIGN)

        augmentedArraySetCall.transformAnnotations(transformer, data)

        dataFlowAnalyzer.enterCallArguments(augmentedArraySetCall, listOf(augmentedArraySetCall.rhs))
        // transformedLhsCall: a.get(index)
        val transformedLhsCall = augmentedArraySetCall.lhsGetCall.transformSingle(transformer, ResolutionMode.ContextIndependent)
        val transformedRhs = augmentedArraySetCall.rhs.transformSingle(transformer, ResolutionMode.ContextDependent)
        dataFlowAnalyzer.exitCallArguments()

        val generator = GeneratorOfPlusAssignCalls(augmentedArraySetCall, operation, transformedLhsCall, transformedRhs)

        // a.get(b).plusAssign(c)
        val assignOperatorCall = generator.createAssignOperatorCall()
        val resolvedAssignCall = resolveCandidateForAssignmentOperatorCall {
            assignOperatorCall.transformSingle(this, ResolutionMode.ContextDependent)
        }
        val assignCallReference = resolvedAssignCall.calleeReference as? FirNamedReferenceWithCandidate
        val assignIsSuccessful = assignCallReference?.isError == false

        fun chooseAssign(): FirFunctionCall {
            callCompleter.completeCall(resolvedAssignCall, ResolutionMode.ContextIndependent)
            dataFlowAnalyzer.exitFunctionCall(resolvedAssignCall, callCompleted = true)
            return resolvedAssignCall
        }

        // prefer a "simpler" variant for dynamics
        if (transformedLhsCall.calleeReference.toResolvedBaseSymbol()?.origin == FirDeclarationOrigin.DynamicScope) {
            return chooseAssign()
        }

        // <array>.set(<index_i>, <array>.get(<index_i>).plus(c))
        val info = tryResolveAugmentedArraySetCallAsSetGetBlock(augmentedArraySetCall, transformedLhsCall, transformedRhs)

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
            callCompleter.completeCall(resolvedSetCall, ResolutionMode.ContextIndependent)
            dataFlowAnalyzer.exitFunctionCall(resolvedSetCall, callCompleted = true)
            return info.toBlock()
        }

        fun reportError(diagnostic: ConeDiagnostic): FirStatement {
            return chooseAssign().also {
                val errorReference = buildErrorNamedReference {
                    source = augmentedArraySetCall.source
                    this.diagnostic = diagnostic
                }
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
    private inner class AugmentedArraySetAsGetSetCallDesugaringInfo(
        val augmentedArraySetCall: FirAugmentedArraySetCall,
        val arrayVariable: FirProperty,
        val indexVariables: List<FirProperty>,
        val operatorCall: FirFunctionCall,
        val setCall: FirFunctionCall
    ) {
        fun toBlock(): FirBlock {
            return buildBlock {
                annotations += augmentedArraySetCall.annotations
                statements += arrayVariable
                statements += indexVariables
                statements += setCall
            }.also {
                it.replaceTypeRef(
                    buildResolvedTypeRef {
                        type = session.builtinTypes.unitType.type
                    }
                )
            }
        }
    }

    private fun tryResolveAugmentedArraySetCallAsSetGetBlock(
        augmentedArraySetCall: FirAugmentedArraySetCall,
        lhsGetCall: FirFunctionCall,
        transformedRhs: FirExpression
    ): AugmentedArraySetAsGetSetCallDesugaringInfo {
        val arrayVariable = generateTemporaryVariable(
            session.moduleData,
            source = lhsGetCall.explicitReceiver?.source?.fakeElement(KtFakeSourceElementKind.DesugaredCompoundAssignment),
            name = SpecialNames.ARRAY,
            initializer = lhsGetCall.explicitReceiver ?: buildErrorExpression {
                source = augmentedArraySetCall.source
                    ?.fakeElement(KtFakeSourceElementKind.DesugaredCompoundAssignment)
                diagnostic = ConeSimpleDiagnostic("No receiver for array access", DiagnosticKind.Syntax)
            }
        )

        val indexVariables = lhsGetCall.arguments.flatMap {
            if (it is FirVarargArgumentsExpression)
                it.arguments
            else
                listOf(it)
        }.mapIndexed { i, index ->
            generateTemporaryVariable(
                session.moduleData,
                source = index.source?.fakeElement(KtFakeSourceElementKind.DesugaredCompoundAssignment),
                name = SpecialNames.subscribeOperatorIndex(i),
                initializer = index,
                typeRef = index.typeRef,
            )
        }

        arrayVariable.transformSingle(transformer, ResolutionMode.ContextIndependent)
        indexVariables.forEach { it.transformSingle(transformer, ResolutionMode.ContextIndependent) }

        val arrayAccess = arrayVariable.toQualifiedAccess()
        val indicesQualifiedAccess = indexVariables.map { it.toQualifiedAccess() }

        val getCall = buildFunctionCall {
            source = augmentedArraySetCall.arrayAccessSource?.fakeElement(KtFakeSourceElementKind.DesugaredCompoundAssignment)
            explicitReceiver = arrayAccess
            if (lhsGetCall.explicitReceiver == lhsGetCall.dispatchReceiver) {
                dispatchReceiver = arrayAccess
                extensionReceiver = lhsGetCall.extensionReceiver
            } else {
                extensionReceiver = arrayAccess
                dispatchReceiver = lhsGetCall.dispatchReceiver
            }
            calleeReference = lhsGetCall.calleeReference
            argumentList = buildArgumentList {
                var i = 0
                for (argument in lhsGetCall.argumentList.arguments) {
                    arguments += if (argument is FirVarargArgumentsExpression) {
                        buildVarargArgumentsExpression {
                            val varargSize = argument.arguments.size
                            arguments += indicesQualifiedAccess.subList(i, i + varargSize)
                            i += varargSize
                            source = argument.source
                            typeRef = argument.typeRef
                            varargElementType = argument.varargElementType
                        }
                    } else {
                        indicesQualifiedAccess[i++]
                    }
                }
            }
            origin = FirFunctionCallOrigin.Operator
            typeRef = lhsGetCall.typeRef
        }

        val generator = GeneratorOfPlusAssignCalls(augmentedArraySetCall, augmentedArraySetCall.operation, getCall, transformedRhs)

        val operatorCall = generator.createSimpleOperatorCall()
        val resolvedOperatorCall = resolveCandidateForAssignmentOperatorCall {
            operatorCall.transformSingle(this, ResolutionMode.ContextDependent)
        }

        val setCall = GeneratorOfPlusAssignCalls.createFunctionCall(
            OperatorNameConventions.SET,
            augmentedArraySetCall.source,
            receiver = arrayAccess, // a
            *indicesQualifiedAccess.toTypedArray(), // indices
            resolvedOperatorCall // a.get(b).plus(c)
        )
        val resolvedSetCall = resolveCandidateForAssignmentOperatorCall {
            setCall.transformSingle(this, ResolutionMode.ContextDependent)
        }

        return AugmentedArraySetAsGetSetCallDesugaringInfo(
            augmentedArraySetCall,
            arrayVariable,
            indexVariables,
            resolvedOperatorCall,
            resolvedSetCall
        )
    }

    override fun transformArrayOfCall(arrayOfCall: FirArrayOfCall, data: ResolutionMode): FirStatement =
        whileAnalysing(session, arrayOfCall) {
            if (data is ResolutionMode.ContextDependent) {
                arrayOfCall.transformChildren(transformer, data)
                return arrayOfCall
            }
            val syntheticIdCall = components.syntheticCallGenerator.generateSyntheticCallForArrayOfCall(arrayOfCall, resolutionContext)
            arrayOfCall.transformChildren(transformer, ResolutionMode.ContextDependent)
            callCompleter.completeCall(syntheticIdCall, data)
            return arrayOfCall
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
        data: ResolutionMode
    ): FirStatement {
        anonymousObjectExpression.transformAnonymousObject(transformer, data)
        if (anonymousObjectExpression.typeRef !is FirResolvedTypeRef) {
            anonymousObjectExpression.resultType = buildResolvedTypeRef {
                source = anonymousObjectExpression.source?.fakeElement(KtFakeSourceElementKind.ImplicitTypeRef)
                this.type = anonymousObjectExpression.anonymousObject.defaultType()
            }
        }
        dataFlowAnalyzer.exitAnonymousObjectExpression(anonymousObjectExpression)
        return anonymousObjectExpression
    }

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression,
        data: ResolutionMode
    ): FirStatement {
        dataFlowAnalyzer.enterAnonymousFunctionExpression(anonymousFunctionExpression)
        return anonymousFunctionExpression.transformAnonymousFunction(transformer, data)
    }

    // ------------------------------------------------------------------------------------------------

    internal fun storeTypeFromCallee(access: FirQualifiedAccessExpression, isLhsOfAssignment: Boolean) {
        val typeFromCallee = components.typeFromCallee(access)
        access.resultType = typeFromCallee.withReplacedConeType(
            if (isLhsOfAssignment) {
                session.typeApproximator.approximateToSubType(
                    typeFromCallee.type, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
                )
            } else {
                session.typeApproximator.approximateToSuperType(
                    typeFromCallee.type, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
                )
            }
        )
    }

}
