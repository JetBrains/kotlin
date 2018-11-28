/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.StatementFilter
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class SimpleTypeArgumentImpl(
    val typeReference: KtTypeReference,
    override val type: UnwrappedType
) : SimpleTypeArgument

// all arguments should be inherited from this class.
// But receivers is not, because for them there is no corresponding valueArgument
abstract class PSIKotlinCallArgument : KotlinCallArgument {
    abstract val valueArgument: ValueArgument
    abstract val dataFlowInfoBeforeThisArgument: DataFlowInfo
    abstract val dataFlowInfoAfterThisArgument: DataFlowInfo

    override fun toString() = valueArgument.getArgumentExpression()?.text?.replace('\n', ' ') ?: valueArgument.toString()
}

abstract class SimplePSIKotlinCallArgument : PSIKotlinCallArgument(), SimpleKotlinCallArgument

val KotlinCallArgument.psiCallArgument: PSIKotlinCallArgument
    get() {
        assert(this is PSIKotlinCallArgument) {
            "Incorrect KotlinCallArgument: $this. Java class: ${javaClass.canonicalName}"
        }
        return this as PSIKotlinCallArgument
    }

val KotlinCallArgument.psiExpression: KtExpression?
    get() {
        return when (this) {
            is ReceiverExpressionKotlinCallArgument -> receiver.receiverValue.safeAs<ExpressionReceiver>()?.expression
            is QualifierReceiverKotlinCallArgument -> receiver.safeAs<Qualifier>()?.expression
            is EmptyLabeledReturn -> null // questionable, maybe unit?
            else -> psiCallArgument.valueArgument.getArgumentExpression()
        }
    }

class ParseErrorKotlinCallArgument(
    override val valueArgument: ValueArgument,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    builtIns: KotlinBuiltIns
) : ExpressionKotlinCallArgument, SimplePSIKotlinCallArgument() {
    override val receiver = ReceiverValueWithSmartCastInfo(
        TransientReceiver(ErrorUtils.createErrorType("Error type for ParseError-argument $valueArgument")),
        possibleTypes = emptySet(),
        isStable = true
    )

    override val isSafeCall: Boolean get() = false

    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
    override val argumentName: Name? get() = valueArgument.getArgumentName()?.asName

    override val dataFlowInfoBeforeThisArgument: DataFlowInfo
        get() = dataFlowInfoAfterThisArgument
}

abstract class PSIFunctionKotlinCallArgument(
    val outerCallContext: BasicCallResolutionContext,
    override val valueArgument: ValueArgument,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val argumentName: Name?
) : LambdaKotlinCallArgument, PSIKotlinCallArgument() {
    override val dataFlowInfoAfterThisArgument: DataFlowInfo // todo drop this and use only lambdaInitialDataFlowInfo
        get() = dataFlowInfoBeforeThisArgument

    abstract val ktFunction: KtFunction
    abstract val expression: KtExpression
    lateinit var lambdaInitialDataFlowInfo: DataFlowInfo
}

class LambdaKotlinCallArgumentImpl(
    outerCallContext: BasicCallResolutionContext,
    valueArgument: ValueArgument,
    dataFlowInfoBeforeThisArgument: DataFlowInfo,
    argumentName: Name?,
    val ktLambdaExpression: KtLambdaExpression,
    val containingBlockForLambda: KtExpression,
    override val parametersTypes: Array<UnwrappedType?>?
) : PSIFunctionKotlinCallArgument(outerCallContext, valueArgument, dataFlowInfoBeforeThisArgument, argumentName) {
    override val ktFunction get() = ktLambdaExpression.functionLiteral
    override val expression get() = containingBlockForLambda
}

class FunctionExpressionImpl(
    outerCallContext: BasicCallResolutionContext,
    valueArgument: ValueArgument,
    dataFlowInfoBeforeThisArgument: DataFlowInfo,
    argumentName: Name?,
    val containingBlockForFunction: KtExpression,
    override val ktFunction: KtNamedFunction,
    override val receiverType: UnwrappedType?,
    override val parametersTypes: Array<UnwrappedType?>,
    override val returnType: UnwrappedType?
) : FunctionExpression, PSIFunctionKotlinCallArgument(outerCallContext, valueArgument, dataFlowInfoBeforeThisArgument, argumentName) {
    override val expression get() = containingBlockForFunction
}

class CallableReferenceKotlinCallArgumentImpl(
    val scopeTowerForResolution: ImplicitScopeTower,
    override val valueArgument: ValueArgument,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    val ktCallableReferenceExpression: KtCallableReferenceExpression,
    override val argumentName: Name?,
    override val lhsResult: LHSResult,
    override val rhsName: Name
) : CallableReferenceKotlinCallArgument, PSIKotlinCallArgument()

class CollectionLiteralKotlinCallArgumentImpl(
    override val valueArgument: ValueArgument,
    override val argumentName: Name?,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    val collectionLiteralExpression: KtCollectionLiteralExpression,
    val outerCallContext: BasicCallResolutionContext
) : CollectionLiteralKotlinCallArgument, PSIKotlinCallArgument() {
    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
}

class SubKotlinCallArgumentImpl(
    override val valueArgument: ValueArgument,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    override val receiver: ReceiverValueWithSmartCastInfo,
    override val callResult: PartialCallResolutionResult
) : SimplePSIKotlinCallArgument(), SubKotlinCallArgument {
    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
    override val argumentName: Name? get() = valueArgument.getArgumentName()?.asName
    override val isSafeCall: Boolean get() = false
}

class ExpressionKotlinCallArgumentImpl(
    override val valueArgument: ValueArgument,
    override val dataFlowInfoBeforeThisArgument: DataFlowInfo,
    override val dataFlowInfoAfterThisArgument: DataFlowInfo,
    override val receiver: ReceiverValueWithSmartCastInfo
) : SimplePSIKotlinCallArgument(), ExpressionKotlinCallArgument {
    override val isSpread: Boolean get() = valueArgument.getSpreadElement() != null
    override val argumentName: Name? get() = valueArgument.getArgumentName()?.asName
    override val isSafeCall: Boolean get() = false
}

class FakeValueArgumentForLeftCallableReference(val ktExpression: KtCallableReferenceExpression) : ValueArgument {
    override fun getArgumentExpression() = ktExpression.receiverExpression

    override fun getArgumentName(): ValueArgumentName? = null
    override fun isNamed(): Boolean = false
    override fun asElement(): KtElement = getArgumentExpression() ?: ktExpression
    override fun getSpreadElement(): LeafPsiElement? = null
    override fun isExternal(): Boolean = false
}

class EmptyLabeledReturn(
    val returnExpression: KtReturnExpression,
    builtIns: KotlinBuiltIns
) : ExpressionKotlinCallArgument {
    override val isSpread: Boolean get() = false
    override val argumentName: Name? get() = null
    override val receiver = ReceiverValueWithSmartCastInfo(TransientReceiver(builtIns.unitType), emptySet(), true)
    override val isSafeCall: Boolean get() = false
}

internal fun KotlinCallArgument.setResultDataFlowInfoIfRelevant(resultDataFlowInfo: DataFlowInfo) {
    if (this is PSIFunctionKotlinCallArgument) {
        lambdaInitialDataFlowInfo = resultDataFlowInfo
    }
}

fun processFunctionalExpression(
    outerCallContext: BasicCallResolutionContext,
    argumentExpression: KtExpression,
    startDataFlowInfo: DataFlowInfo,
    valueArgument: ValueArgument,
    argumentName: Name?,
    builtIns: KotlinBuiltIns,
    typeResolver: TypeResolver
): PSIKotlinCallArgument? {
    val expression = ArgumentTypeResolver.getFunctionLiteralArgumentIfAny(argumentExpression, outerCallContext) ?: return null
    val postponedExpression = if (expression is KtFunctionLiteral) expression.getParentOfType<KtLambdaExpression>(true) else expression

    val lambdaArgument: PSIKotlinCallArgument? = when (postponedExpression) {
        is KtLambdaExpression ->
            LambdaKotlinCallArgumentImpl(
                outerCallContext, valueArgument, startDataFlowInfo, argumentName, postponedExpression, argumentExpression,
                resolveParametersTypes(outerCallContext, postponedExpression.functionLiteral, typeResolver)
            )

        is KtNamedFunction -> {
            val receiverType = resolveType(outerCallContext, postponedExpression.receiverTypeReference, typeResolver)
            val parametersTypes = resolveParametersTypes(outerCallContext, postponedExpression, typeResolver) ?: emptyArray()
            val returnType = resolveType(outerCallContext, postponedExpression.typeReference, typeResolver)
                    ?: if (postponedExpression.hasBlockBody()) builtIns.unitType else null

            FunctionExpressionImpl(
                outerCallContext, valueArgument, startDataFlowInfo, argumentName,
                argumentExpression, postponedExpression, receiverType, parametersTypes, returnType
            )
        }

        else -> return null
    }

    checkNoSpread(outerCallContext, valueArgument)

    return lambdaArgument
}

fun checkNoSpread(context: BasicCallResolutionContext, valueArgument: ValueArgument) {
    valueArgument.getSpreadElement()?.let {
        context.trace.report(Errors.SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE.on(it))
    }
}

private fun resolveParametersTypes(
    context: BasicCallResolutionContext,
    ktFunction: KtFunction,
    typeResolver: TypeResolver
): Array<UnwrappedType?>? {
    val parameterList = ktFunction.valueParameterList ?: return null

    return Array(parameterList.parameters.size) {
        parameterList.parameters[it]?.typeReference?.let { resolveType(context, it, typeResolver) }
    }
}

internal fun resolveType(
    context: BasicCallResolutionContext,
    typeReference: KtTypeReference?,
    typeResolver: TypeResolver
): UnwrappedType? {
    if (typeReference == null) return null

    val type = typeResolver.resolveType(context.scope, typeReference, context.trace, checkBounds = true)
    ForceResolveUtil.forceResolveAllContents(type)
    return type.unwrap()
}


// context here is context for value argument analysis
internal fun createSimplePSICallArgument(
    contextForArgument: BasicCallResolutionContext,
    valueArgument: ValueArgument,
    typeInfoForArgument: KotlinTypeInfo
) = createSimplePSICallArgument(
    contextForArgument.trace.bindingContext, contextForArgument.statementFilter,
    contextForArgument.scope.ownerDescriptor, valueArgument,
    contextForArgument.dataFlowInfo, typeInfoForArgument,
    contextForArgument.languageVersionSettings,
    contextForArgument.dataFlowValueFactory
)

internal fun createSimplePSICallArgument(
    bindingContext: BindingContext,
    statementFilter: StatementFilter,
    ownerDescriptor: DeclarationDescriptor,
    valueArgument: ValueArgument,
    dataFlowInfoBeforeThisArgument: DataFlowInfo,
    typeInfoForArgument: KotlinTypeInfo,
    languageVersionSettings: LanguageVersionSettings,
    dataFlowValueFactory: DataFlowValueFactory
): SimplePSIKotlinCallArgument? {

    val ktExpression = KtPsiUtil.getLastElementDeparenthesized(valueArgument.getArgumentExpression(), statementFilter) ?: return null
    val onlyResolvedCall = ktExpression.getCall(bindingContext)?.let {
        bindingContext.get(BindingContext.ONLY_RESOLVED_CALL, it)
    }
    // todo hack for if expression: sometimes we not write properly type information for branches
    val baseType = typeInfoForArgument.type?.unwrap() ?: onlyResolvedCall?.resultCallAtom?.freshReturnType ?: return null

    // we should use DFI after this argument, because there can be some useful smartcast. Popular case: if branches.
    val receiverToCast = transformToReceiverWithSmartCastInfo(
        ownerDescriptor, bindingContext,
        typeInfoForArgument.dataFlowInfo, // dataFlowInfoBeforeThisArgument cannot be used here, because of if() { if (x != null) return; x }
        ExpressionReceiver.create(ktExpression, baseType, bindingContext),
        languageVersionSettings,
        dataFlowValueFactory
    ).let {
        if (onlyResolvedCall == null) it.prepareReceiverRegardingCaptureTypes() else it
    }

    return if (onlyResolvedCall == null) {
        ExpressionKotlinCallArgumentImpl(valueArgument, dataFlowInfoBeforeThisArgument, typeInfoForArgument.dataFlowInfo, receiverToCast)
    } else {
        SubKotlinCallArgumentImpl(
            valueArgument,
            dataFlowInfoBeforeThisArgument,
            typeInfoForArgument.dataFlowInfo,
            receiverToCast,
            onlyResolvedCall
        )
    }
}
