/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.StatementFilter
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.model.LambdaKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.prepareArgumentTypeRegardingCaptureTypes
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo

class SimpleTypeArgumentImpl(
        val typeReference: KtTypeReference,
        override val type: UnwrappedType
): SimpleTypeArgument

// all arguments should be inherited from this class.
// But receivers is not, because for them there is no corresponding valueArgument
abstract class PSIKotlinCallArgument : KotlinCallArgument {
    abstract val valueArgument: ValueArgument
    abstract val dataFlowInfoBeforeThisArgument: DataFlowInfo
    abstract val dataFlowInfoAfterThisArgument: DataFlowInfo

    override fun toString() = valueArgument.getArgumentExpression()?.text?.replace('\n', ' ') ?: valueArgument.toString()
}

val KotlinCallArgument.psiCallArgument: PSIKotlinCallArgument get() {
    assert(this is PSIKotlinCallArgument) {
        "Incorrect KotlinCallArgument: $this. Java class: ${javaClass.canonicalName}"
    }
    return this as PSIKotlinCallArgument
}

val KotlinCallArgument.psiExpression: KtExpression? get() {
    if (this is ReceiverExpressionKotlinCallArgument) {
        return (receiver.receiverValue as? ExpressionReceiver)?.expression
    }
    return psiCallArgument.valueArgument.getArgumentExpression()
}

class ParseErrorKotlinCallArgument(
        override val valueArgument: ValueArgument,
        override val dataFlowInfoAfterThisArgument: DataFlowInfo,
        builtIns: KotlinBuiltIns
): ExpressionKotlinCallArgument, PSIKotlinCallArgument() {
    override val receiver = ReceiverValueWithSmartCastInfo(TransientReceiver(builtIns.nothingType), emptySet(), isStable = true)

    override val isSafeCall: Boolean get() = false

    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
    override val argumentName: Name? get() = valueArgument.getArgumentName()?.asName

    override val dataFlowInfoBeforeThisArgument: DataFlowInfo
        get() = dataFlowInfoAfterThisArgument
}

class LambdaKotlinCallArgumentImpl(
        val outerCallContext: BasicCallResolutionContext,
        override val valueArgument: ValueArgument,
        override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
        val ktLambdaExpression: KtLambdaExpression,
        override val argumentName: Name?,
        override val parametersTypes: Array<UnwrappedType?>?
) : LambdaKotlinCallArgument, PSIKotlinCallArgument() {
    override val dataFlowInfoAfterThisArgument: DataFlowInfo
        get() = dataFlowInfoBeforeThisArgument
}

class FunctionExpressionImpl(
        val outerCallContext: BasicCallResolutionContext,
        override val valueArgument: ValueArgument,
        override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
        val ktFunction: KtNamedFunction,
        override val argumentName: Name?,
        override val receiverType: UnwrappedType?,
        override val parametersTypes: Array<UnwrappedType?>,
        override val returnType: UnwrappedType?
) : FunctionExpression, PSIKotlinCallArgument() {
    override val dataFlowInfoAfterThisArgument: DataFlowInfo
        get() = dataFlowInfoBeforeThisArgument
}

class CallableReferenceKotlinCallArgumentImpl(
        override val valueArgument: ValueArgument,
        override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
        override val dataFlowInfoAfterThisArgument: DataFlowInfo,
        val ktCallableReferenceExpression: KtCallableReferenceExpression,
        override val argumentName: Name?,
        override val lhsResult: LHSResult,
        override val rhsName: Name
) : CallableReferenceKotlinCallArgument, PSIKotlinCallArgument()

class SubKotlinCallArgumentImpl(
        override val valueArgument: ValueArgument,
        override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
        override val dataFlowInfoAfterThisArgument: DataFlowInfo,
        override val receiver: ReceiverValueWithSmartCastInfo,
        override val resolvedCall: ResolvedKotlinCall.OnlyResolvedKotlinCall
): PSIKotlinCallArgument(), SubKotlinCallArgument {
    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
    override val argumentName: Name? get() = valueArgument.getArgumentName()?.asName
    override val isSafeCall: Boolean get() = false
}

class ExpressionKotlinCallArgumentImpl(
        override val valueArgument: ValueArgument,
        override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
        override val dataFlowInfoAfterThisArgument: DataFlowInfo,
        override val receiver: ReceiverValueWithSmartCastInfo
): PSIKotlinCallArgument(), ExpressionKotlinCallArgument {
    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
    override val argumentName: Name? get() = valueArgument.getArgumentName()?.asName
    override val isSafeCall: Boolean get() = false
}

class FakeValueArgumentForLeftCallableReference(val ktExpression: KtCallableReferenceExpression): ValueArgument {
    override fun getArgumentExpression() = ktExpression.receiverExpression

    override fun getArgumentName(): ValueArgumentName? = null
    override fun isNamed(): Boolean = false
    override fun asElement(): KtElement = getArgumentExpression() ?: ktExpression
    override fun getSpreadElement(): LeafPsiElement? = null
    override fun isExternal(): Boolean = false
}

// context here is context for value argument analysis
internal fun createSimplePSICallArgument(
        contextForArgument: BasicCallResolutionContext,
        valueArgument: ValueArgument,
        typeInfoForArgument: KotlinTypeInfo
) = createSimplePSICallArgument(contextForArgument.trace.bindingContext, contextForArgument.statementFilter,
                                contextForArgument.scope.ownerDescriptor, valueArgument,
                                contextForArgument.dataFlowInfo, typeInfoForArgument)

internal fun createSimplePSICallArgument(
        bindingContext: BindingContext,
        statementFilter: StatementFilter,
        ownerDescriptor: DeclarationDescriptor,
        valueArgument: ValueArgument,
        dataFlowInfoBeforeThisArgument: DataFlowInfo,
        typeInfoForArgument: KotlinTypeInfo
): PSIKotlinCallArgument? {

    val ktExpression = KtPsiUtil.getLastElementDeparenthesized(valueArgument.getArgumentExpression(), statementFilter) ?: return null
    val onlyResolvedCall = ktExpression.getCall(bindingContext)?.let {
        bindingContext.get(BindingContext.ONLY_RESOLVED_CALL, it)
    }
    val baseType = onlyResolvedCall?.currentReturnType ?: typeInfoForArgument.type?.unwrap() ?: return null
    val preparedType = prepareArgumentTypeRegardingCaptureTypes(baseType) ?: baseType

    // we should use DFI after this argument, because there can be some useful smartcast. Popular case: if branches.
    val receiverToCast = transformToReceiverWithSmartCastInfo(
            ownerDescriptor, bindingContext,
            typeInfoForArgument.dataFlowInfo,
            ExpressionReceiver.create(ktExpression, baseType, bindingContext)
    ).let {
        ReceiverValueWithSmartCastInfo(it.receiverValue.replaceType(preparedType), it.possibleTypes, it.isStable)
    }

    return if (onlyResolvedCall == null) {
        ExpressionKotlinCallArgumentImpl(valueArgument, dataFlowInfoBeforeThisArgument, typeInfoForArgument.dataFlowInfo, receiverToCast)
    }
    else {
        SubKotlinCallArgumentImpl(valueArgument, dataFlowInfoBeforeThisArgument, typeInfoForArgument.dataFlowInfo, receiverToCast, onlyResolvedCall)
    }

}