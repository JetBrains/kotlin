/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.old.binding

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.buildSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.idea.frontend.old.KtSymbolBasedContext
import org.jetbrains.kotlin.idea.frontend.old.toDeclarationDescriptor
import org.jetbrains.kotlin.idea.frontend.old.toKotlinType
import org.jetbrains.kotlin.idea.frontend.old.withAnalysisSession
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.LinkedHashMap

private fun FirExpression?.toExpressionReceiverValue(context: KtSymbolBasedContext): ReceiverValue? {
    if (this == null) return null

    val firSymbolBuilder = (context.ktAnalysisSession as KtFirAnalysisSession).firSymbolBuilder

    if (this is FirThisReceiverExpression) {
        val ktClassSymbol = firSymbolBuilder.classifierBuilder.buildClassLikeSymbol(calleeReference.boundSymbol?.fir as FirClassLikeDeclaration<*>)
        return ImplicitClassReceiver(ktClassSymbol.toDeclarationDescriptor(context) as ClassDescriptor)
    }
    val expression = realPsi.safeAs<KtExpression>() ?: context.implementationPostponed()
    return ExpressionReceiver.create(
        expression,
        firSymbolBuilder.typeBuilder.buildKtType(typeRef).toKotlinType(context),
        context.incorrectImplementation { BindingContext.EMPTY }
    )
}

class FirSimpleWrapperCall(
    val ktCall: KtCallExpression,
    val firCall: FirFunctionCall,
    val context: KtSymbolBasedContext
) : Call {
    override fun getCallOperationNode(): ASTNode = ktCall.node

    override fun getExplicitReceiver(): Receiver? = firCall.explicitReceiver.toExpressionReceiverValue(context)

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

class FirWrapperResolvedCall(val firSimpleWrapperCall: FirSimpleWrapperCall) : ResolvedCall<CallableDescriptor> {
    private val firCall: FirFunctionCall = firSimpleWrapperCall.firCall
    private val context = firSimpleWrapperCall.context
    private val firSymbolBuilder = (context.ktAnalysisSession as KtFirAnalysisSession).firSymbolBuilder

    private val ktFunctionSymbol =
        (firCall.calleeReference as FirResolvedNamedReference).resolvedSymbol.fir.buildSymbol(firSymbolBuilder) as KtFunctionLikeSymbol

    private val firArguments: LinkedHashMap<FirExpression, FirValueParameter> = firCall.argumentMapping ?: context.implementationPostponed()

    private var _typeArguments: Map<TypeParameterDescriptor, KotlinType>? = null
    private var _arguments: Map<ValueParameterDescriptor, ResolvedValueArgument>? = null

    override fun getStatus(): ResolutionStatus =
        if (firCall.calleeReference is FirResolvedNamedReference) ResolutionStatus.SUCCESS else ResolutionStatus.OTHER_ERROR

    override fun getCall(): Call = firSimpleWrapperCall

    override fun getCandidateDescriptor(): CallableDescriptor {
        return ktFunctionSymbol.toDeclarationDescriptor(context)
    }

    override fun getResultingDescriptor(): CallableDescriptor = context.incorrectImplementation { candidateDescriptor }

    override fun getExtensionReceiver(): ReceiverValue? {
        if (firCall.extensionReceiver === FirNoReceiverExpression) return null

        return firCall.extensionReceiver.toExpressionReceiverValue(context)
    }

    override fun getDispatchReceiver(): ReceiverValue? {
        if (firCall.dispatchReceiver === FirNoReceiverExpression) return null

        return firCall.dispatchReceiver.toExpressionReceiverValue(context)
    }

    override fun getExplicitReceiverKind(): ExplicitReceiverKind {
        if (firCall.explicitReceiver === null) return ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        if (firCall.explicitReceiver === firCall.extensionReceiver) return ExplicitReceiverKind.EXTENSION_RECEIVER
        return ExplicitReceiverKind.DISPATCH_RECEIVER
    }

    override fun getValueArguments(): Map<ValueParameterDescriptor, ResolvedValueArgument> {
        _arguments?.let { return it }

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
            val resolvedValueArgument = (parameter as KtFirSymbol<FirValueParameter>).firRef.withFir {
                firParameterToResolvedValueArgument[it]
            } ?: DefaultValueArgument.DEFAULT
            arguments[candidateDescriptor.valueParameters[parameterIndex]] = resolvedValueArgument
        }
        _arguments = arguments
        return arguments
    }

    override fun getValueArgumentsByIndex(): List<ResolvedValueArgument> = valueArguments.values.toList()

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping {
        val argumentExpression = valueArgument.getArgumentExpression() ?: context.implementationPostponed()

        var targetFirParameter: FirValueParameter? = null
        outer@ for ((firExpression, firValueParameter) in firArguments.entries) {
            if (firExpression is FirVarargArgumentsExpression) {
                for (subExpression in firExpression.arguments)
                    if (subExpression.realPsi === argumentExpression) {
                        targetFirParameter = firValueParameter
                        break@outer
                    }
            } else if (firExpression.realPsi === argumentExpression) {
                targetFirParameter = firValueParameter
                break@outer
            }
        }
        if (targetFirParameter == null) return ArgumentUnmapped

        val parameterIndex = ktFunctionSymbol.valueParameters.indexOfFirst {
            (it as KtFirSymbol<FirValueParameter>).firRef.withFir { it === targetFirParameter }
        }
        if (parameterIndex == -1) error("Fir parameter not found :(")

        val parameterDescriptor = candidateDescriptor.valueParameters[parameterIndex]
        return ArgumentMatchImpl(parameterDescriptor)
    }

    override fun getTypeArguments(): Map<TypeParameterDescriptor, KotlinType> {
        if (firCall.typeArguments.isEmpty()) return emptyMap()
        _typeArguments?.let { return it }

        val typeArguments = linkedMapOf<TypeParameterDescriptor, KotlinType>()
        for ((index, parameter) in candidateDescriptor.typeParameters.withIndex()) {
            val firTypeProjectionWithVariance = firCall.typeArguments[index] as FirTypeProjectionWithVariance
            val kotlinType = firSymbolBuilder.typeBuilder.buildKtType(firTypeProjectionWithVariance.typeRef).toKotlinType(context)
            typeArguments[parameter] = kotlinType
        }
        _typeArguments = typeArguments
        return typeArguments
    }

    override fun getDataFlowInfoForArguments(): DataFlowInfoForArguments = context.noImplementation()
    override fun getSmartCastDispatchReceiverType(): KotlinType? = context.noImplementation()
}

class CallAndResolverCallWrappers(bindingContext: KtSymbolBasedBindingContext) {
    private val context = bindingContext.context

    init {
        bindingContext.registerGetterByKey(BindingContext.CALL, this::getCall)
        bindingContext.registerGetterByKey(BindingContext.RESOLVED_CALL, this::getResolvedCall)
        bindingContext.registerGetterByKey(BindingContext.REFERENCE_TARGET, this::getReferenceTarget)
    }

    private fun getCall(element: KtElement): Call {
        val ktFirAnalysisSession = context.ktAnalysisSession as KtFirAnalysisSession
        val firResolveState = ktFirAnalysisSession.firResolveState

        val ktCall = element.parent.safeAs<KtCallExpression>() ?: context.implementationPostponed()
        val firCall = when (val fir = element.getOrBuildFir(firResolveState)) {
            is FirFunctionCall -> fir
            is FirSafeCallExpression -> fir.regularQualifiedAccess as? FirFunctionCall
            else -> null
        }

        if (firCall != null) return FirSimpleWrapperCall(ktCall, firCall, context)

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