/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.fe10.binding

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.idea.fir.fe10.*
import org.jetbrains.kotlin.idea.fir.fe10.FirWeakReference
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

private fun FirExpression?.toExpressionReceiverValue(context: FE10BindingContext): ReceiverValue? {
    if (this == null) return null

    if (this is FirThisReceiverExpression) {
        val ktClassSymbol =
            context.ktAnalysisSessionFacade.buildClassLikeSymbol(calleeReference.boundSymbol?.fir as FirClassLikeDeclaration<*>)
        return ImplicitClassReceiver(ktClassSymbol.toDeclarationDescriptor(context) as ClassDescriptor)
    }
    val expression = realPsi.safeAs<KtExpression>() ?: context.implementationPostponed()
    return ExpressionReceiver.create(
        expression,
        context.ktAnalysisSessionFacade.buildKtType(typeRef).toKotlinType(context),
        context.incorrectImplementation { BindingContext.EMPTY }
    )
}

internal class FirSimpleWrapperCall(
    val ktCall: KtCallExpression,
    val firCall: FirWeakReference<FirFunctionCall>,
    val context: FE10BindingContext
) : Call {
    override fun getCallOperationNode(): ASTNode = ktCall.node

    override fun getExplicitReceiver(): Receiver? = firCall.withFir { it.explicitReceiver.toExpressionReceiverValue(context) }

    override fun getDispatchReceiver(): ReceiverValue? = null

    override fun getCalleeExpression(): KtExpression? = ktCall.calleeExpression

    override fun getValueArgumentList(): KtValueArgumentList? = ktCall.valueArgumentList

    override fun getValueArguments(): List<ValueArgument> = ktCall.valueArguments

    override fun getFunctionLiteralArguments(): List<LambdaArgument> = ktCall.lambdaArguments

    override fun getTypeArguments(): List<KtTypeProjection> = ktCall.typeArguments

    override fun getTypeArgumentList(): KtTypeArgumentList? = ktCall.typeArgumentList

    override fun getCallElement(): KtElement = ktCall

    override fun getCallType(): Call.CallType = Call.CallType.DEFAULT
}

internal class FirWrapperResolvedCall(val firSimpleWrapperCall: FirSimpleWrapperCall) : ResolvedCall<CallableDescriptor> {
    private val firCall get() = firSimpleWrapperCall.firCall
    private val context get() = firSimpleWrapperCall.context

    private val ktFunctionSymbol: KtFunctionLikeSymbol = firCall.withFir {
        when (val calleeReference = it.calleeReference) {
            is FirResolvedNamedReference -> context.ktAnalysisSessionFacade.buildSymbol(calleeReference.resolvedSymbol.fir) as KtFunctionLikeSymbol
            is FirErrorNamedReference -> context.ktAnalysisSessionFacade.buildSymbol(calleeReference.candidateSymbol!!.fir) as KtFunctionLikeSymbol
            else -> context.noImplementation("calleeReferenceType: ${calleeReference::class.java}")
        }
    }

    private val _typeArguments: Map<TypeParameterDescriptor, KotlinType> by lazy(LazyThreadSafetyMode.PUBLICATION) {

        if (firCall.getFir().typeArguments.isEmpty()) return@lazy emptyMap()

        val typeArguments = linkedMapOf<TypeParameterDescriptor, KotlinType>()
        for ((index, parameter) in candidateDescriptor.typeParameters.withIndex()) {
            val firTypeProjectionWithVariance = firCall.getFir().typeArguments[index] as FirTypeProjectionWithVariance
            val kotlinType = context.ktAnalysisSessionFacade.buildKtType(firTypeProjectionWithVariance.typeRef).toKotlinType(context)
            typeArguments[parameter] = kotlinType
        }
        typeArguments
    }

    private val arguments: Map<ValueParameterDescriptor, ResolvedValueArgument> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val firArguments = firCall.withFir { it.argumentMapping } ?: context.implementationPostponed()

        val firParameterToResolvedValueArgument = hashMapOf<FirValueParameter, ResolvedValueArgument>()
        val allArguments = firSimpleWrapperCall.valueArguments + firSimpleWrapperCall.functionLiteralArguments
        var argumentIndex = 0
        for ((firExpression, firValueParameter) in firArguments.entries) {
            if (firExpression is FirVarargArgumentsExpression) {
                val varargArguments = mutableListOf<ValueArgument>()
                for (subExpression in firExpression.arguments) {
                    val currentArgument = allArguments[argumentIndex]; argumentIndex++
                    check(currentArgument.getArgumentExpression() === subExpression.realPsi) {
                        "Different psi: ${currentArgument.getArgumentExpression()} !== ${subExpression.realPsi}"
                    }
                    varargArguments.add(currentArgument)
                }
                firParameterToResolvedValueArgument[firValueParameter] = VarargValueArgument(varargArguments)
            } else {
                val currentArgument = allArguments[argumentIndex]; argumentIndex++
                check(currentArgument.getArgumentExpression() === firExpression.realPsi) {
                    "Different psi: ${currentArgument.getArgumentExpression()} !== ${firExpression.realPsi}"
                }
                firParameterToResolvedValueArgument[firValueParameter] = ExpressionValueArgument(currentArgument)
            }
        }

        val arguments = linkedMapOf<ValueParameterDescriptor, ResolvedValueArgument>()
        for ((parameterIndex, parameter) in ktFunctionSymbol.valueParameters.withIndex()) {
            val resolvedValueArgument = context.ktAnalysisSessionFacade.withFir(parameter) { it: FirValueParameter ->
                firParameterToResolvedValueArgument[it]
            } ?: DefaultValueArgument.DEFAULT
            arguments[candidateDescriptor.valueParameters[parameterIndex]] = resolvedValueArgument
        }
        arguments
    }

    override fun getStatus(): ResolutionStatus =
        if (firCall.getFir().calleeReference is FirResolvedNamedReference) ResolutionStatus.SUCCESS else ResolutionStatus.OTHER_ERROR

    override fun getCall(): Call = firSimpleWrapperCall

    override fun getCandidateDescriptor(): CallableDescriptor {
        return ktFunctionSymbol.toDeclarationDescriptor(context)
    }

    override fun getResultingDescriptor(): CallableDescriptor = context.incorrectImplementation { candidateDescriptor }

    override fun getExtensionReceiver(): ReceiverValue? {
        if (firCall.getFir().extensionReceiver === FirNoReceiverExpression) return null

        return firCall.getFir().extensionReceiver.toExpressionReceiverValue(context)
    }

    override fun getDispatchReceiver(): ReceiverValue? {
        if (firCall.getFir().dispatchReceiver === FirNoReceiverExpression) return null

        return firCall.getFir().dispatchReceiver.toExpressionReceiverValue(context)
    }

    override fun getExplicitReceiverKind(): ExplicitReceiverKind {
        if (firCall.getFir().explicitReceiver === null) return ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        if (firCall.getFir().explicitReceiver === firCall.getFir().extensionReceiver) return ExplicitReceiverKind.EXTENSION_RECEIVER
        return ExplicitReceiverKind.DISPATCH_RECEIVER
    }

    override fun getValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> = arguments

    override fun getValueArgumentsByIndex(): List<ResolvedValueArgument> = valueArguments.values.toList()

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping {
        val firArguments = firCall.withFir { it.argumentMapping } ?: context.implementationPostponed()
        val argumentExpression = valueArgument.getArgumentExpression() ?: context.implementationPostponed()

        fun FirExpression.isMyArgument() = realPsi === valueArgument || realPsi === argumentExpression

        var targetFirParameter: FirValueParameter? = null
        outer@ for ((firExpression, firValueParameter) in firArguments.entries) {
            if (firExpression is FirVarargArgumentsExpression) {
                for (subExpression in firExpression.arguments)
                    if (subExpression.isMyArgument()) {
                        targetFirParameter = firValueParameter
                        break@outer
                    }
            } else if (firExpression.isMyArgument()) {
                targetFirParameter = firValueParameter
                break@outer
            }
        }
        if (targetFirParameter == null) return ArgumentUnmapped

        val parameterIndex = ktFunctionSymbol.valueParameters.indexOfFirst {
            context.ktAnalysisSessionFacade.withFir(it) { it: FirValueParameter -> it === targetFirParameter }
        }
        if (parameterIndex == -1) error("Fir parameter not found :(")

        val parameterDescriptor = candidateDescriptor.valueParameters[parameterIndex]
        return ArgumentMatchImpl(parameterDescriptor)
    }

    override fun getTypeArguments(): Map<TypeParameterDescriptor, KotlinType> = _typeArguments

    override fun getDataFlowInfoForArguments(): DataFlowInfoForArguments = context.noImplementation()
    override fun getSmartCastDispatchReceiverType(): KotlinType? = context.noImplementation()
    override fun getContextReceivers(): MutableList<ReceiverValue> = context.noImplementation()
}

class CallAndResolverCallWrappers(bindingContext: KtSymbolBasedBindingContext) {
    private val context = bindingContext.context

    init {
        bindingContext.registerGetterByKey(BindingContext.CALL, this::getCall)
        bindingContext.registerGetterByKey(BindingContext.RESOLVED_CALL, this::getResolvedCall)
        bindingContext.registerGetterByKey(BindingContext.REFERENCE_TARGET, this::getReferenceTarget)
    }

    private fun getCall(element: KtElement): Call {
        val ktCall = element.parent.safeAs<KtCallExpression>() ?: context.implementationPostponed()
        val firCall = when (val fir = ktCall.getOrBuildFir(context.ktAnalysisSessionFacade.firResolveState)) {
            is FirFunctionCall -> fir
            is FirSafeCallExpression -> fir.regularQualifiedAccess as? FirFunctionCall
            else -> null
        }

        if (firCall != null) return FirSimpleWrapperCall(ktCall, FirWeakReference(firCall, context.ktAnalysisSessionFacade.analysisSession.token), context)

        // Call for property
        context.implementationPostponed()
    }

    private fun getResolvedCall(call: Call): ResolvedCall<*> {
        check(call is FirSimpleWrapperCall) {
            "Incorrect Call type: $call"
        }
        return FirWrapperResolvedCall(call)
    }

    private fun getReferenceTarget(key: KtReferenceExpression): DeclarationDescriptor? {
        val ktSymbol = context.withAnalysisSession { key.mainReference.resolveToSymbols().singleOrNull() } ?: return null
        return ktSymbol.toDeclarationDescriptor(context)
    }
}