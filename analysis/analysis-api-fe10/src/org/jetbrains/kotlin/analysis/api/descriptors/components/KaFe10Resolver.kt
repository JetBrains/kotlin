/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.signatures.KaFe10FunctionSignature
import org.jetbrains.kotlin.analysis.api.descriptors.signatures.KaFe10VariableSignature
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10ReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.getResolutionScope
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseResolver
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.*
import org.jetbrains.kotlin.analysis.api.impl.base.util.KaNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.idea.references.KtDefaultAnnotationArgumentReference
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.references.fe10.base.KtFe10Reference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.jetbrains.kotlin.resolve.calls.tower.NewAbstractResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.checkWithAttachment

internal class KaFe10Resolver(
    override val analysisSessionProvider: () -> KaFe10Session,
) : KaBaseResolver<KaFe10Session>(), KaFe10SessionComponent {
    override fun KtReference.isImplicitReferenceToCompanion(): Boolean = withPsiValidityAssertion(element) {
        if (this !is KtSimpleNameReference) {
            return false
        }
        val bindingContext = analysisContext.analyze(element, AnalysisMode.PARTIAL)
        return bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element] != null
    }

    override fun KtReference.resolveToSymbols(): Collection<KaSymbol> = withPsiValidityAssertion(element) {
        return doResolveToSymbols(this)
    }

    private fun doResolveToSymbols(reference: KtReference): Collection<KaSymbol> {
        if (reference is KtDefaultAnnotationArgumentReference) {
            return resolveDefaultAnnotationArgumentReference(reference)
        }

        checkWithAttachment(
            reference is KtFe10Reference,
            { "${reference::class.simpleName} is not extends ${KtFe10Reference::class.simpleName}" },
        ) {
            it.withPsiAttachment("reference", reference.element)
        }

        val bindingContext = analysisContext.analyze(reference.element, AnalysisMode.PARTIAL)
        return reference.getTargetDescriptors(bindingContext).mapNotNull { descriptor ->
            descriptor.toKtSymbol(analysisContext)
        }
    }

    override fun doResolveCall(psi: KtElement): KaCallInfo? {
        if (!canBeResolvedAsCall(psi)) return null

        val parentBinaryExpression = psi.parentOfType<KtBinaryExpression>()
        val lhs = KtPsiUtil.deparenthesize(parentBinaryExpression?.left)
        val unwrappedPsi = when (val unwrapped = KtPsiUtil.deparenthesize(psi as? KtExpression) ?: psi) {
            is KtWhenConditionInRange -> unwrapped.operationReference
            else -> unwrapped
        }

        if (parentBinaryExpression != null &&
            parentBinaryExpression.operationToken == KtTokens.EQ &&
            (lhs == unwrappedPsi || (lhs as? KtQualifiedExpression)?.selectorExpression == unwrappedPsi) &&
            unwrappedPsi !is KtArrayAccessExpression
        ) {
            // Specially handle property assignment because FE1.0 resolves LHS of assignment to just the property, which would then be
            // treated as a property read.
            return doResolveCall(parentBinaryExpression)
        }

        when (psi) {
            is KtCallableReferenceExpression -> return doResolveCall(psi.callableReference)
            is KtConstructorDelegationReferenceExpression -> return (psi.parent as? KtElement)?.let(::doResolveCall)
        }

        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)
        return when (unwrappedPsi) {
            is KtBinaryExpression -> {
                handleAsCompoundAssignment(bindingContext, unwrappedPsi)?.let { return it }
                handleAsFunctionCall(bindingContext, unwrappedPsi)
            }

            is KtUnaryExpression -> {
                handleAsIncOrDecOperator(bindingContext, unwrappedPsi)?.let { return it }
                handleAsFunctionCall(bindingContext, unwrappedPsi)
            }

            else -> handleAsFunctionCall(bindingContext, unwrappedPsi)
                ?: handleAsPropertyRead(bindingContext, unwrappedPsi)
        } ?: handleResolveErrors(bindingContext, psi)
    }

    override fun doCollectCallCandidates(psi: KtElement): List<KaCallCandidateInfo> {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)
        val resolvedCall = doResolveCall(psi)
        return doCollectCallCandidates(psi, bindingContext, resolvedCall).ifEmpty {
            resolvedCall.toKaCallCandidateInfos()
        }
    }

    private fun doCollectCallCandidates(
        psi: KtElement,
        bindingContext: BindingContext,
        resolvedCall: KaCallInfo?,
    ): List<KaCallCandidateInfo> {
        val bestCandidateDescriptors =
            resolvedCall?.calls?.filterIsInstance<KaCallableMemberCall<*, *>>()
                ?.mapNotNullTo(mutableSetOf()) { it.descriptor as? CallableDescriptor }
                ?: emptySet()

        val unwrappedPsi = KtPsiUtil.deparenthesize(psi as? KtExpression) ?: psi

        // TODO: Handle ++ or -- operator for KtUnaryExpression

        if (unwrappedPsi is KtBinaryExpression &&
            (unwrappedPsi.operationToken in OperatorConventions.COMPARISON_OPERATIONS ||
                    unwrappedPsi.operationToken in OperatorConventions.EQUALS_OPERATIONS)
        ) {
            // TODO: Handle compound assignment
            handleAsFunctionCall(bindingContext, unwrappedPsi)?.toKaCallCandidateInfos()?.let { return it }
        }

        // The regular mechanism doesn't work, so at least the resolved call should be returned
        when (psi) {
            is KtWhenConditionInRange,
            is KtCollectionLiteralExpression,
            is KtCallableReferenceExpression,
                -> return resolvedCall?.toKaCallCandidateInfos().orEmpty()
        }

        val resolutionScope = unwrappedPsi.getResolutionScope(bindingContext) ?: return emptyList()
        val call = unwrappedPsi.getCall(bindingContext)?.let {
            if (it is CallTransformer.CallForImplicitInvoke) it.outerCall else it
        } ?: return emptyList()
        val dataFlowInfo = bindingContext.getDataFlowInfoBefore(unwrappedPsi)
        val bindingTrace = DelegatingBindingTrace(bindingContext, "Trace for all candidates", withParentDiagnostics = false)
        val dataFlowValueFactory = DataFlowValueFactoryImpl(analysisContext.languageVersionSettings)

        val callResolutionContext = BasicCallResolutionContext.create(
            bindingTrace, resolutionScope, call, TypeUtils.NO_EXPECTED_TYPE, dataFlowInfo,
            ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            /* isAnnotationContext = */ false, analysisContext.languageVersionSettings,
            dataFlowValueFactory
        ).replaceCollectAllCandidates(true)

        val result = analysisContext.callResolver.resolveFunctionCall(callResolutionContext)
        val candidates = result.allCandidates?.let { analysisContext.overloadingConflictResolver.filterOutEquivalentCalls(it) }
            ?: error("allCandidates is null even when collectAllCandidates = true")

        val candidateInfos = candidates.flatMap { candidate ->
            // The current BindingContext does not have the diagnostics for each individual candidate, only for the resolved call.
            // If there are multiple candidates, we can get each one's diagnostics by reporting it to a new BindingTrace.
            val candidateTrace = DelegatingBindingTrace(bindingContext, "Trace for candidate", withParentDiagnostics = false)
            if (candidate is NewAbstractResolvedCall<*>) {
                analysisContext.kotlinToResolvedCallTransformer.reportDiagnostics(
                    callResolutionContext,
                    candidateTrace,
                    candidate,
                    candidate.diagnostics
                )
            }

            val candidateKtCallInfo = handleAsFunctionCall(
                candidateTrace.bindingContext,
                unwrappedPsi,
                candidate,
                candidateTrace.bindingContext.diagnostics
            )

            candidateKtCallInfo.toKaCallCandidateInfos(bestCandidateDescriptors)
        }

        return if (resolvedCall is KaSuccessCallInfo) {
            resolvedCall.toKaCallCandidateInfos() + candidateInfos.filterNot(KaCallCandidateInfo::isInBestCandidates)
        } else {
            candidateInfos
        }
    }

    private val KaCallableMemberCall<*, *>.descriptor: DeclarationDescriptor?
        get() = when (val symbol = symbol) {
            is KaFe10PsiSymbol<*, *> -> symbol.descriptor
            is KaFe10DescSymbol<*> -> symbol.descriptor
            else -> null
        }

    private fun KaCallInfo?.toKaCallCandidateInfos(): List<KaCallCandidateInfo> {
        return when (this) {
            is KaSuccessCallInfo -> listOf(KaBaseApplicableCallCandidateInfo(call, isInBestCandidates = true))
            is KaErrorCallInfo -> candidateCalls.map { KaBaseInapplicableCallCandidateInfo(it, isInBestCandidates = true, diagnostic) }
            null -> emptyList()
        }
    }

    private fun KaCallInfo?.toKaCallCandidateInfos(bestCandidateDescriptors: Set<CallableDescriptor>): List<KaCallCandidateInfo> {
        // TODO: We should prefer to compare symbols instead of descriptors, but we can't do so while symbols are not cached.
        fun KaCall.isInBestCandidates(): Boolean {
            val descriptor = this.safeAs<KaCallableMemberCall<*, *>>()?.descriptor as? CallableDescriptor
            return descriptor != null && bestCandidateDescriptors.any { it ->
                DescriptorEquivalenceForOverrides.areCallableDescriptorsEquivalent(
                    it,
                    descriptor,
                    allowCopiesFromTheSameDeclaration = true,
                    kotlinTypeRefiner = analysisContext.kotlinTypeRefiner
                )
            }
        }

        return when (this) {
            is KaSuccessCallInfo -> {
                listOf(KaBaseApplicableCallCandidateInfo(call, call.isInBestCandidates()))
            }
            is KaErrorCallInfo -> candidateCalls.map {
                KaBaseInapplicableCallCandidateInfo(it, it.isInBestCandidates(), diagnostic)
            }
            null -> emptyList()
        }
    }

    private fun handleAsCompoundAssignment(context: BindingContext, binaryExpression: KtBinaryExpression): KaCallInfo? {
        val left = binaryExpression.left ?: return null
        val right = binaryExpression.right
        val resolvedCalls = mutableListOf<ResolvedCall<*>>()
        return when (binaryExpression.operationToken) {
            KtTokens.EQ -> {
                val resolvedCall = left.getResolvedCall(context) ?: return null
                resolvedCalls += resolvedCall
                val partiallyAppliedSymbol =
                    resolvedCall.toPartiallyAppliedVariableSymbol(context) ?: return null
                KaBaseSimpleVariableAccessCall(
                    partiallyAppliedSymbol,
                    resolvedCall.toTypeArgumentsMapping(partiallyAppliedSymbol),
                    KaBaseSimpleVariableWriteAccess(right),
                )
            }
            in KtTokens.AUGMENTED_ASSIGNMENTS -> {
                if (right == null) return null
                val operatorCall = binaryExpression.getResolvedCall(context) ?: return null
                resolvedCalls += operatorCall
                // This method only handles compound assignment. Other cases like `plusAssign`, `rangeTo`, `contains` are handled by plain
                // `handleAsFunctionCall`
                if (operatorCall.resultingDescriptor.name !in operatorWithAssignmentVariant) return null
                val operatorPartiallyAppliedSymbol =
                    operatorCall.toPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>(context) ?: return null

                val compoundAccess = KaBaseCompoundAssignOperation(
                    operatorPartiallyAppliedSymbol,
                    binaryExpression.getCompoundAssignKind(),
                    right
                )

                if (left is KtArrayAccessExpression) {
                    createCompoundArrayAccessCall(context, left, compoundAccess, resolvedCalls)
                } else {
                    val resolvedCall = left.getResolvedCall(context) ?: return null
                    resolvedCalls += resolvedCall
                    val variableAppliedSymbol = resolvedCall.toPartiallyAppliedVariableSymbol(context) ?: return null
                    KaBaseCompoundVariableAccessCall(variableAppliedSymbol, compoundAccess)
                }
            }
            else -> null
        }?.let { createCallInfo(context, binaryExpression, it, resolvedCalls) }
    }

    private fun handleAsIncOrDecOperator(context: BindingContext, unaryExpression: KtUnaryExpression): KaCallInfo? {
        if (unaryExpression.operationToken !in KtTokens.INCREMENT_AND_DECREMENT) return null
        val operatorCall = unaryExpression.getResolvedCall(context) ?: return null
        val resolvedCalls = mutableListOf(operatorCall)
        val operatorPartiallyAppliedSymbol = operatorCall.toPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>(context) ?: return null
        val baseExpression = unaryExpression.baseExpression
        val kind = unaryExpression.getInOrDecOperationKind()
        val precedence = when (unaryExpression) {
            is KtPostfixExpression -> KaCompoundUnaryOperation.Precedence.POSTFIX
            is KtPrefixExpression -> KaCompoundUnaryOperation.Precedence.PREFIX
            else -> error("unexpected KtUnaryExpression $unaryExpression")
        }

        val compoundAccess = KaBaseCompoundUnaryOperation(operatorPartiallyAppliedSymbol, kind, precedence)
        return if (baseExpression is KtArrayAccessExpression) {
            createCompoundArrayAccessCall(context, baseExpression, compoundAccess, resolvedCalls)
        } else {
            val resolvedCall = baseExpression.getResolvedCall(context)
            val variableAppliedSymbol = resolvedCall?.toPartiallyAppliedVariableSymbol(context) ?: return null
            KaBaseCompoundVariableAccessCall(variableAppliedSymbol, compoundAccess)
        }?.let { createCallInfo(context, unaryExpression, it, resolvedCalls) }
    }

    private fun createCompoundArrayAccessCall(
        context: BindingContext,
        arrayAccessExpression: KtArrayAccessExpression,
        compoundAccess: KaCompoundOperation,
        resolvedCalls: MutableList<ResolvedCall<*>>,
    ): KaCompoundArrayAccessCall? {
        val resolvedGetCall = context[BindingContext.INDEXED_LVALUE_GET, arrayAccessExpression] ?: return null
        resolvedCalls += resolvedGetCall
        val getPartiallyAppliedSymbol = resolvedGetCall.toPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>(context) ?: return null
        val resolvedSetCall = context[BindingContext.INDEXED_LVALUE_SET, arrayAccessExpression] ?: return null
        resolvedCalls += resolvedSetCall
        val setPartiallyAppliedSymbol = resolvedSetCall.toPartiallyAppliedFunctionSymbol<KaNamedFunctionSymbol>(context) ?: return null
        return KaBaseCompoundArrayAccessCall(
            compoundAccess,
            arrayAccessExpression.indexExpressions,
            getPartiallyAppliedSymbol,
            setPartiallyAppliedSymbol
        )
    }

    private fun handleAsFunctionCall(context: BindingContext, element: KtElement): KaCallInfo? {
        return element.getResolvedCall(context)?.let { handleAsFunctionCall(context, element, it) }
    }

    private fun handleAsFunctionCall(
        context: BindingContext,
        element: KtElement,
        resolvedCall: ResolvedCall<*>,
        diagnostics: Diagnostics = context.diagnostics,
    ): KaCallInfo? {
        return if (resolvedCall is VariableAsFunctionResolvedCall) {
            if (element is KtCallExpression || element is KtQualifiedExpression) {
                // TODO: consider demoting extension receiver to the first argument to align with FIR behavior. See test case
                //  analysis/analysis-api/testData/components/callResolver/resolveCall/functionTypeVariableCall_dispatchReceiver.kt:5 where
                //  FIR and FE1.0 behaves differently because FIR unifies extension receiver of functional type as the first argument
                resolvedCall.functionCall.toFunctionKtCall(context)
            } else {
                resolvedCall.variableCall.toPropertyRead(context)
            }?.let { createCallInfo(context, element, it, listOf(resolvedCall), diagnostics) }
        } else {
            resolvedCall.toFunctionKtCall(context)?.let { createCallInfo(context, element, it, listOf(resolvedCall), diagnostics) }
        }
    }

    private fun handleAsPropertyRead(context: BindingContext, element: KtElement): KaCallInfo? {
        val call = element.getResolvedCall(context) ?: return null
        return call.toPropertyRead(context)?.let { createCallInfo(context, element, it, listOf(call)) }
    }

    private fun ResolvedCall<*>.toPropertyRead(context: BindingContext): KaVariableAccessCall? {
        val partiallyAppliedSymbol = toPartiallyAppliedVariableSymbol(context) ?: return null
        return KaBaseSimpleVariableAccessCall(
            partiallyAppliedSymbol,
            toTypeArgumentsMapping(partiallyAppliedSymbol),
            KaBaseSimpleVariableReadAccess,
        )
    }

    private fun ResolvedCall<*>.toFunctionKtCall(context: BindingContext): KaFunctionCall<*>? {
        val partiallyAppliedSymbol = toPartiallyAppliedFunctionSymbol<KaFunctionSymbol>(context) ?: return null
        val argumentMapping = createArgumentMapping(partiallyAppliedSymbol.signature)
        if (partiallyAppliedSymbol.signature.symbol is KaConstructorSymbol) {
            @Suppress("UNCHECKED_CAST")
            val partiallyAppliedConstructorSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>
            when (val callElement = call.callElement) {
                is KtAnnotationEntry -> return KaBaseAnnotationCall(partiallyAppliedSymbol, argumentMapping)
                is KtConstructorDelegationCall -> return KaBaseDelegatedConstructorCall(
                    partiallyAppliedConstructorSymbol,
                    if (callElement.isCallToThis) KaDelegatedConstructorCall.Kind.THIS_CALL else KaDelegatedConstructorCall.Kind.SUPER_CALL,
                    argumentMapping,
                    toTypeArgumentsMapping(partiallyAppliedSymbol)
                )
                is KtSuperTypeCallEntry -> return KaBaseDelegatedConstructorCall(
                    partiallyAppliedConstructorSymbol,
                    KaDelegatedConstructorCall.Kind.SUPER_CALL,
                    argumentMapping,
                    toTypeArgumentsMapping(partiallyAppliedSymbol)
                )
            }
        }

        return KaBaseSimpleFunctionCall(
            partiallyAppliedSymbol,
            argumentMapping,
            toTypeArgumentsMapping(partiallyAppliedSymbol),
            call.callType == Call.CallType.INVOKE
        )
    }

    private fun ResolvedCall<*>.toPartiallyAppliedVariableSymbol(context: BindingContext): KaPartiallyAppliedVariableSymbol<KaVariableSymbol>? {
        val partiallyAppliedSymbol = toPartiallyAppliedSymbol(context) ?: return null
        if (partiallyAppliedSymbol.signature !is KaVariableSignature<*>) return null
        @Suppress("UNCHECKED_CAST")
        return partiallyAppliedSymbol as KaPartiallyAppliedVariableSymbol<KaVariableSymbol>
    }


    private inline fun <reified S : KaFunctionSymbol> ResolvedCall<*>.toPartiallyAppliedFunctionSymbol(context: BindingContext): KaPartiallyAppliedFunctionSymbol<S>? {
        val partiallyAppliedSymbol = toPartiallyAppliedSymbol(context) ?: return null
        if (partiallyAppliedSymbol.symbol !is S) return null
        @Suppress("UNCHECKED_CAST")
        return partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<S>
    }

    private fun ResolvedCall<*>.toPartiallyAppliedSymbol(context: BindingContext): KaPartiallyAppliedSymbol<*, *>? {
        val targetDescriptor = candidateDescriptor
        val symbol = targetDescriptor.toKtCallableSymbol(analysisContext) ?: return null
        val signature = createSignature(symbol, resultingDescriptor) ?: return null
        if (targetDescriptor.isSynthesizedPropertyFromJavaAccessors()) {
            // FE1.0 represents synthesized properties as an extension property of the Java class. Hence we use the extension receiver as
            // the dispatch receiver and always pass null for extension receiver (because in Java there is no way to specify an extension
            // receiver)
            return KaBasePartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = extensionReceiver?.toKtReceiverValue(context, this),
                extensionReceiver = null,
                contextArguments = contextReceivers.mapNotNull { it.toKtReceiverValue(context, this) },
            )
        } else {
            return KaBasePartiallyAppliedSymbol(
                backingSignature = signature,
                dispatchReceiver = dispatchReceiver?.toKtReceiverValue(context, this, smartCastDispatchReceiverType)
                    ?: targetDescriptor.dispatchReceiverForImportedCallables(),
                extensionReceiver = extensionReceiver?.toKtReceiverValue(context, this),
                contextArguments = contextReceivers.mapNotNull { it.toKtReceiverValue(context, this) },
            )
        }
    }

    private fun CallableDescriptor.dispatchReceiverForImportedCallables(): KaReceiverValue? {
        if (this !is ImportedFromObjectCallableDescriptor<*>) return null

        val symbol = containingObject.toKaClassSymbol(analysisContext)
        return KaBaseImplicitReceiverValue(
            backingSymbol = symbol,
            type = containingObject.defaultType.toKtType(analysisContext),
        )
    }

    private fun ReceiverValue.toKtReceiverValue(
        context: BindingContext,
        resolvedCall: ResolvedCall<*>,
        smartCastType: KotlinType? = null,
    ): KaReceiverValue? {
        val ktType = type.toKtType(analysisContext)
        val result = when (this) {
            is ExpressionReceiver -> expression.toExplicitReceiverValue(ktType)
            is ExtensionReceiver -> {
                val extensionReceiverParameter = this.declarationDescriptor.extensionReceiverParameter ?: return null
                KaBaseImplicitReceiverValue(KaFe10ReceiverParameterSymbol(extensionReceiverParameter, analysisContext), ktType)
            }
            is ImplicitReceiver -> {
                val symbol = this.declarationDescriptor.toKtSymbol(analysisContext) ?: return null
                KaBaseImplicitReceiverValue(symbol, ktType)
            }
            else -> null
        }
        var smartCastTypeToUse = smartCastType
        if (smartCastTypeToUse == null) {
            when (result) {
                is KaExplicitReceiverValue -> {
                    smartCastTypeToUse = context[BindingContext.SMARTCAST, result.expression]?.type(resolvedCall.call)
                }
                is KaImplicitReceiverValue -> {
                    smartCastTypeToUse =
                        context[BindingContext.IMPLICIT_RECEIVER_SMARTCAST, resolvedCall.call.calleeExpression]?.receiverTypes?.get(this)
                }
                else -> {}
            }
        }

        return if (smartCastTypeToUse != null && result != null) {
            KaBaseSmartCastedReceiverValue(result, smartCastTypeToUse.toKtType(analysisContext))
        } else {
            result
        }
    }

    private fun createSignature(symbol: KaSymbol, resultingDescriptor: CallableDescriptor): KaCallableSignature<*>? {
        val returnType = if (resultingDescriptor is ValueParameterDescriptor && resultingDescriptor.isVararg) {
            val arrayType = resultingDescriptor.returnType ?: return null
            analysisContext.builtIns.getArrayElementType(arrayType)
        } else {
            resultingDescriptor.returnType
        }
        val ktReturnType = returnType?.toKtType(analysisContext) ?: return null
        val receiverType = if (resultingDescriptor.isSynthesizedPropertyFromJavaAccessors()) {
            // FE1.0 represents synthesized properties as an extension property of the Java class. Hence the extension receiver type should
            // always be null
            null
        } else {
            resultingDescriptor.extensionReceiverParameter?.returnType?.toKtType(analysisContext)
        }

        val contextParameters = (symbol as? KaCallableSymbol)?.contextParameters
            .orEmpty()
            .zip(resultingDescriptor.contextReceiverParameters)
            .map { (symbol, descriptor) ->
                @Suppress("UNCHECKED_CAST")
                createSignature(symbol, descriptor) as KaVariableSignature<KaContextParameterSymbol>
            }

        return when (symbol) {
            is KaVariableSymbol -> KaFe10VariableSignature(
                backingSymbol = symbol,
                backingReturnType = ktReturnType,
                backingReceiverType = receiverType,
                backingContextParameters = contextParameters,
            )

            is KaFunctionSymbol -> KaFe10FunctionSignature(
                backingSymbol = symbol,
                backingReturnType = ktReturnType,
                backingReceiverType = receiverType,
                backingValueParameters = symbol.valueParameters
                    .zip(resultingDescriptor.valueParameters)
                    .map { (symbol, resultingDescriptor) ->
                        @Suppress("UNCHECKED_CAST")
                        createSignature(symbol, resultingDescriptor) as KaVariableSignature<KaValueParameterSymbol>
                    },
                backingContextParameters = contextParameters,
            )

            else -> error("unexpected callable symbol $this")
        }
    }

    private fun CallableDescriptor?.isSynthesizedPropertyFromJavaAccessors() =
        this is PropertyDescriptor && kind == CallableMemberDescriptor.Kind.SYNTHESIZED

    private fun ResolvedCall<*>.createArgumentMapping(signature: KaFunctionSignature<*>): Map<KtExpression, KaVariableSignature<KaValueParameterSymbol>> {
        val parameterSignatureByName = signature.valueParameters.associateBy {
            // ResolvedCall.valueArguments have their names affected by the `@ParameterName` annotations,
            // so we use `name` instead of `symbol.name`
            it.name
        }

        val result = linkedMapOf<KtExpression, KaVariableSignature<KaValueParameterSymbol>>()
        for ((parameter, arguments) in valueArguments) {
            val parameterSymbol = KaFe10DescValueParameterSymbol(parameter, analysisContext)

            for (argument in arguments.arguments) {
                val expression = argument.getArgumentExpression() ?: continue
                result[expression] = parameterSignatureByName[parameterSymbol.name] ?: continue
            }
        }

        return result.ifEmpty { emptyMap() }
    }

    private fun createCallInfo(
        context: BindingContext,
        psi: KtElement,
        ktCall: KaCall,
        resolvedCalls: List<ResolvedCall<*>>,
        diagnostics: Diagnostics = context.diagnostics,
    ): KaCallInfo {
        val failedResolveCall = resolvedCalls.firstOrNull { !it.status.isSuccess } ?: return KaBaseSuccessCallInfo(ktCall)

        val diagnostic = getDiagnosticToReport(context, psi, ktCall, diagnostics)?.let { KaFe10Diagnostic(it, token) }
            ?: KaNonBoundToPsiErrorDiagnostic(
                factoryName = Errors.UNRESOLVED_REFERENCE.name,
                "${failedResolveCall.status} with ${failedResolveCall.resultingDescriptor.name}",
                token
            )

        return KaBaseErrorCallInfo(listOf(ktCall), diagnostic)
    }

    private fun handleResolveErrors(context: BindingContext, psi: KtElement): KaErrorCallInfo? {
        val diagnostic = getDiagnosticToReport(context, psi, null) ?: return null
        val ktDiagnostic = diagnostic.let { KaFe10Diagnostic(it, token) }
        val calls = when (diagnostic.factory) {
            in diagnosticWithResolvedCallsAtPosition1 -> {
                require(diagnostic is DiagnosticWithParameters1<*, *>)
                @Suppress("UNCHECKED_CAST")
                diagnostic.a as Collection<ResolvedCall<*>>
            }
            in diagnosticWithResolvedCallsAtPosition2 -> {
                require(diagnostic is DiagnosticWithParameters2<*, *, *>)
                @Suppress("UNCHECKED_CAST")
                diagnostic.b as Collection<ResolvedCall<*>>
            }
            else -> {
                emptyList()
            }
        }

        return KaBaseErrorCallInfo(calls.mapNotNull { it.toFunctionKtCall(context) ?: it.toPropertyRead(context) }, ktDiagnostic)
    }

    private fun getDiagnosticToReport(
        context: BindingContext,
        psi: KtElement,
        ktCall: KaCall?,
        diagnostics: Diagnostics = context.diagnostics,
    ) = diagnostics.firstOrNull { diagnostic ->
        if (diagnostic.severity != Severity.ERROR) return@firstOrNull false
        if (diagnostic.factory in syntaxErrors) return@firstOrNull true
        val isResolutionError = diagnostic.factory in resolutionFailureErrors
        val isCallArgError = diagnostic.factory in callArgErrors
        val reportedPsi = diagnostic.psiElement
        val reportedPsiParent = reportedPsi.parent
        when {
            // Errors reported on the querying element or the `selectorExpression`/`calleeExpression` of the querying element
            isResolutionError &&
                    (reportedPsi == psi ||
                            psi is KtQualifiedExpression && reportedPsi == psi.selectorExpression ||
                            psi is KtCallElement && reportedPsi.parentsWithSelf.any { it == psi.calleeExpression }) -> true
            // Errors reported on the value argument list or the right most parentheses (not enough argument, for example)
            isResolutionError &&
                    reportedPsi is KtValueArgumentList || reportedPsiParent is KtValueArgumentList && reportedPsi == reportedPsiParent.rightParenthesis -> true
            // errors on call args for normal function calls
            isCallArgError &&
                    reportedPsiParent is KtValueArgument &&
                    (psi is KtQualifiedExpression && psi.selectorExpression?.safeAs<KtCallExpression>()?.valueArguments?.contains(
                        reportedPsiParent
                    ) == true ||
                            psi is KtCallElement && reportedPsiParent in psi.valueArguments) -> true
            // errors on receiver of invoke function calls
            isCallArgError &&
                    (psi is KtQualifiedExpression && reportedPsiParent == psi.selectorExpression ||
                            psi is KtCallElement && reportedPsiParent == psi) -> true
            // errors on index args for array access convention
            isCallArgError &&
                    reportedPsiParent is KtContainerNode && reportedPsiParent.parent is KtArrayAccessExpression -> true
            // errors on lambda args
            isCallArgError &&
                    reportedPsi is KtLambdaExpression || reportedPsi is KtLambdaArgument -> true
            // errors on value to set using array access convention
            isCallArgError &&
                    ktCall is KaSimpleFunctionCall && (reportedPsiParent as? KtBinaryExpression)?.right == reportedPsi -> true
            else -> false
        }
    }

    private fun ResolvedCall<*>.toTypeArgumentsMapping(partiallyAppliedSymbol: KaPartiallyAppliedSymbol<*, *>): Map<KaTypeParameterSymbol, KaType> {
        if (typeArguments.isEmpty()) return emptyMap()

        val typeParameters = partiallyAppliedSymbol.symbol.typeParameters

        val result = mutableMapOf<KaTypeParameterSymbol, KaType>()
        for ((parameter, type) in typeArguments) {
            val ktParameter = typeParameters.getOrNull(parameter.index) ?: return emptyMap()

            // i.e. we were not able to infer some types
            if (type.contains { it: UnwrappedType -> it.constructor is TypeVariableTypeConstructor }) return emptyMap()

            result[ktParameter] = type.toKtType(analysisContext)
        }

        return result
    }

    private companion object {
        private val operatorWithAssignmentVariant = setOf(
            OperatorNameConventions.PLUS,
            OperatorNameConventions.MINUS,
            OperatorNameConventions.TIMES,
            OperatorNameConventions.DIV,
            OperatorNameConventions.REM,
        )

        private val callArgErrors = setOf(
            Errors.ARGUMENT_PASSED_TWICE,
            Errors.MIXING_NAMED_AND_POSITIONED_ARGUMENTS,
            Errors.NAMED_PARAMETER_NOT_FOUND,
            Errors.NAMED_ARGUMENTS_NOT_ALLOWED,
            Errors.VARARG_OUTSIDE_PARENTHESES,
            Errors.SPREAD_OF_NULLABLE,
            Errors.SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE,
            Errors.MANY_LAMBDA_EXPRESSION_ARGUMENTS,
            Errors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE,
            Errors.TOO_MANY_ARGUMENTS,
            Errors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION,
            Errors.REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_ANNOTATION,
            *Errors.TYPE_MISMATCH_ERRORS.toTypedArray(),
        )

        private val resolutionFailureErrors: Set<DiagnosticFactoryWithPsiElement<*, *>> = setOf(
            Errors.INVISIBLE_MEMBER,
            Errors.NO_VALUE_FOR_PARAMETER,
            Errors.MISSING_RECEIVER,
            Errors.NO_RECEIVER_ALLOWED,
            Errors.ILLEGAL_SELECTOR,
            Errors.FUNCTION_EXPECTED,
            Errors.FUNCTION_CALL_EXPECTED,
            Errors.NO_CONSTRUCTOR,
            Errors.OVERLOAD_RESOLUTION_AMBIGUITY,
            Errors.NONE_APPLICABLE,
            Errors.CANNOT_COMPLETE_RESOLVE,
            Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
            Errors.CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY,
            Errors.TYPE_PARAMETER_AS_REIFIED,
            Errors.DEFINITELY_NON_NULLABLE_AS_REIFIED,
            Errors.REIFIED_TYPE_FORBIDDEN_SUBSTITUTION,
            Errors.REIFIED_TYPE_UNSAFE_SUBSTITUTION,
            Errors.CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION,
            Errors.RESOLUTION_TO_CLASSIFIER,
            Errors.RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS,
            Errors.PARENTHESIZED_COMPANION_LHS_DEPRECATION,
            Errors.RESOLUTION_TO_PRIVATE_CONSTRUCTOR_OF_SEALED_CLASS,
            Errors.UNRESOLVED_REFERENCE,
            *Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.factories,
            *Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_IN_AUGMENTED_ASSIGNMENT.factories,
        )

        private val syntaxErrors = setOf(
            Errors.ASSIGNMENT_IN_EXPRESSION_CONTEXT,
        )

        val diagnosticWithResolvedCallsAtPosition1 = setOf(
            Errors.OVERLOAD_RESOLUTION_AMBIGUITY,
            Errors.NONE_APPLICABLE,
            Errors.CANNOT_COMPLETE_RESOLVE,
            Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
            Errors.ASSIGN_OPERATOR_AMBIGUITY,
            Errors.ITERATOR_AMBIGUITY,
        )

        val diagnosticWithResolvedCallsAtPosition2 = setOf(
            Errors.COMPONENT_FUNCTION_AMBIGUITY,
            Errors.DELEGATE_SPECIAL_FUNCTION_AMBIGUITY,
            Errors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE,
            Errors.DELEGATE_PD_METHOD_NONE_APPLICABLE,
        )

        private val DiagnosticFactoryForDeprecation<*, *, *>.factories: Array<DiagnosticFactoryWithPsiElement<*, *>>
            get() = arrayOf(warningFactory, errorFactory)
    }
}
