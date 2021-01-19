/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.old.binding

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.frontend.api.calls.KtCall
import org.jetbrains.kotlin.idea.frontend.api.calls.KtFunctionCall
import org.jetbrains.kotlin.idea.frontend.api.calls.KtSuccessCallTarget
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.old.KtSymbolBasedContext
import org.jetbrains.kotlin.idea.frontend.old.KtSymbolBasedFunctionDescriptor
import org.jetbrains.kotlin.idea.frontend.old.toDeclarationDescriptor
import org.jetbrains.kotlin.idea.frontend.old.withAnalysisSession
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMapping
import org.jetbrains.kotlin.resolve.calls.model.DataFlowInfoForArguments
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirWrapperCall(val ktCall: KtCall, val context: KtSymbolBasedContext) : Call {
    override fun getCallOperationNode(): ASTNode? {
        TODO("Not yet implemented")
    }

    override fun getExplicitReceiver(): Receiver? {
        TODO("Not yet implemented")
    }

    override fun getDispatchReceiver(): ReceiverValue? {
        TODO("Not yet implemented")
    }

    override fun getCalleeExpression(): KtExpression? {
        TODO("Not yet implemented")
    }

    override fun getValueArgumentList(): KtValueArgumentList? {
        TODO("Not yet implemented")
    }

    override fun getValueArguments(): List<ValueArgument> {
        TODO("Not yet implemented")
    }

    override fun getFunctionLiteralArguments(): List<LambdaArgument> {
        TODO("Not yet implemented")
    }

    override fun getTypeArguments(): List<KtTypeProjection> {
        TODO("Not yet implemented")
    }

    override fun getTypeArgumentList(): KtTypeArgumentList? {
        TODO("Not yet implemented")
    }

    override fun getCallElement(): KtElement {
        TODO("Not yet implemented")
    }

    override fun getCallType(): Call.CallType {
        TODO("Not yet implemented")
    }
}

class FirWrapperResolvedCall(val firWrapperCall: FirWrapperCall) : ResolvedCall<CallableDescriptor> {
    private val ktCall = firWrapperCall.ktCall
    private val context = firWrapperCall.context

    override fun getStatus(): ResolutionStatus =
        if (ktCall.isErrorCall) ResolutionStatus.OTHER_ERROR else ResolutionStatus.SUCCESS

    override fun getCall(): Call = firWrapperCall

    override fun getCandidateDescriptor(): CallableDescriptor {
        val ktSymbol = ((ktCall as KtFunctionCall).targetFunction as KtSuccessCallTarget).symbol as KtFunctionSymbol
        return KtSymbolBasedFunctionDescriptor(ktSymbol, firWrapperCall.context)
    }

    override fun getResultingDescriptor(): CallableDescriptor = context.incorrectImplementation { candidateDescriptor }

    override fun getExtensionReceiver(): ReceiverValue? {
        TODO("Not yet implemented")
    }

    override fun getDispatchReceiver(): ReceiverValue? {
        TODO("Not yet implemented")
    }

    override fun getExplicitReceiverKind(): ExplicitReceiverKind {
        TODO("Not yet implemented")
    }

    override fun getValueArguments(): MutableMap<ValueParameterDescriptor, ResolvedValueArgument> {
        TODO("Not yet implemented")
    }

    override fun getValueArgumentsByIndex(): MutableList<ResolvedValueArgument> {
        TODO("Not yet implemented")
    }

    override fun getArgumentMapping(valueArgument: ValueArgument): ArgumentMapping {
        TODO("Not yet implemented")
    }

    override fun getTypeArguments(): MutableMap<TypeParameterDescriptor, KotlinType> {
        TODO("Not yet implemented")
    }

    override fun getDataFlowInfoForArguments(): DataFlowInfoForArguments {
        TODO("Not yet implemented")
    }

    override fun getSmartCastDispatchReceiverType(): KotlinType? {
        TODO("Not yet implemented")
    }

}

class CallAndResolverCallWrappers(bindingContext: KtSymbolBasedBindingContext) {
    private val context = bindingContext.context

    init {
        bindingContext.registerGetterByKey(BindingContext.CALL, this::getCall)
        bindingContext.registerGetterByKey(BindingContext.RESOLVED_CALL, this::getResolvedCall)
        bindingContext.registerGetterByKey(BindingContext.REFERENCE_TARGET, this::getReferenceTarget)
    }

    private fun getCall(element: KtElement): Call {
        with(context.ktAnalysisSession) {
            element.parent?.safeAs<KtCallExpression>()?.resolveCall()?.let { return FirWrapperCall(it, context) }
        }
        TODO("Not implemented case")
    }

    private fun getResolvedCall(call: Call): ResolvedCall<*> {
        check(call is FirWrapperCall) {
            "Incorrect Call type: $call"
        }
        return FirWrapperResolvedCall(call)
    }

    private fun getReferenceTarget(key: KtReferenceExpression): DeclarationDescriptor? {
        val ktSymbol = context.withAnalysisSession { key.mainReference.resolveToSymbols().singleOrNull() } ?: return null
        return ktSymbol.toDeclarationDescriptor(context)
    }
}