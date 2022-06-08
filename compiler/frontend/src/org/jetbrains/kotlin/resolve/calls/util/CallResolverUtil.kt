/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.ComposedSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.EmptySubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isParameterOfAnnotation
import org.jetbrains.kotlin.resolve.scopes.HierarchicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.collectSyntheticConstructors
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.resolve.scopes.receivers.SuperCallReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.canBeResolvedWithoutDeprecation
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.buildNotFixedVariablesToPossibleResultType
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.compactIfPossible

enum class ResolveArgumentsMode {
    RESOLVE_FUNCTION_ARGUMENTS,
    SHAPE_FUNCTION_ARGUMENTS
}

fun isOrOverridesSynthesized(descriptor: CallableMemberDescriptor): Boolean {
    if (descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED) {
        return true
    }
    if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return descriptor.overriddenDescriptors.all(::isOrOverridesSynthesized)
    }
    return false
}

fun isBinaryRemOperator(call: Call): Boolean {
    val callElement = call.callElement as? KtBinaryExpression ?: return false
    val operator = callElement.operationToken
    if (operator !is KtToken) return false

    val name = OperatorConventions.getNameForOperationSymbol(operator, true, true) ?: return false
    return name in OperatorConventions.REM_TO_MOD_OPERATION_NAMES.keys
}

fun isConventionCall(call: Call): Boolean {
    if (call is CallTransformer.CallForImplicitInvoke) return true
    val callElement = call.callElement
    if (callElement is KtArrayAccessExpression || callElement is KtDestructuringDeclarationEntry) return true
    val calleeExpression = call.calleeExpression as? KtOperationReferenceExpression ?: return false
    return calleeExpression.isConventionOperator()
}

fun isInfixCall(call: Call): Boolean {
    val operationRefExpression = call.calleeExpression as? KtOperationReferenceExpression ?: return false
    val binaryExpression = operationRefExpression.parent as? KtBinaryExpression ?: return false
    return binaryExpression.operationReference === operationRefExpression && operationRefExpression.operationSignTokenType == null
}

fun isSuperOrDelegatingConstructorCall(call: Call): Boolean =
    call.calleeExpression.let { it is KtConstructorCalleeExpression || it is KtConstructorDelegationReferenceExpression }

fun isInvokeCallOnVariable(call: Call): Boolean {
    if (call.callType !== Call.CallType.INVOKE) return false
    val dispatchReceiver = call.dispatchReceiver
    //calleeExpressionAsDispatchReceiver for invoke is always ExpressionReceiver, see CallForImplicitInvoke
    val expression = (dispatchReceiver as ExpressionReceiver).expression
    return expression is KtSimpleNameExpression
}

fun getSuperCallExpression(call: Call): KtSuperExpression? {
    return (call.explicitReceiver as? ExpressionReceiver)?.expression as? KtSuperExpression
}

fun getEffectiveExpectedType(
    parameterDescriptor: ValueParameterDescriptor,
    resolvedArgument: ResolvedValueArgument,
    languageVersionSettings: LanguageVersionSettings,
    trace: BindingTrace
): KotlinType {
    val argument = resolvedArgument.arguments.singleOrNull()
    return if (argument != null)
        getEffectiveExpectedTypeForSingleArgument(parameterDescriptor, argument, languageVersionSettings, trace)
    else
        getExpectedType(parameterDescriptor)
}

fun getEffectiveExpectedType(
    parameterDescriptor: ValueParameterDescriptor,
    argument: ValueArgument,
    context: ResolutionContext<*>
): KotlinType {
    return getEffectiveExpectedTypeForSingleArgument(parameterDescriptor, argument, context.languageVersionSettings, context.trace)
}

fun getEffectiveExpectedTypeForSingleArgument(
    parameterDescriptor: ValueParameterDescriptor,
    argument: ValueArgument,
    languageVersionSettings: LanguageVersionSettings,
    trace: BindingTrace
): KotlinType {
    if (argument.getSpreadElement() != null) {
        // Spread argument passed to a non-vararg parameter, an error is already reported by ValueArgumentsToParametersMapper
        return if (parameterDescriptor.varargElementType == null) DONT_CARE else parameterDescriptor.type
    }

    if (
        arrayAssignmentToVarargInNamedFormInAnnotation(parameterDescriptor, argument, languageVersionSettings, trace) ||
        arrayAssignmentToVarargInNamedFormInFunction(parameterDescriptor, argument, languageVersionSettings, trace)
    ) {
        return parameterDescriptor.type
    }

    return getExpectedType(parameterDescriptor)
}

private fun getExpectedType(parameterDescriptor: ValueParameterDescriptor): KotlinType {
    return parameterDescriptor.varargElementType ?: parameterDescriptor.type
}

private fun arrayAssignmentToVarargInNamedFormInAnnotation(
    parameterDescriptor: ValueParameterDescriptor,
    argument: ValueArgument,
    languageVersionSettings: LanguageVersionSettings,
    trace: BindingTrace
): Boolean {
    if (!languageVersionSettings.supportsFeature(LanguageFeature.AssigningArraysToVarargsInNamedFormInAnnotations)) return false

    val isAllowedAssigningSingleElementsToVarargsInNamedForm =
        !languageVersionSettings.supportsFeature(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm)

    if (isAllowedAssigningSingleElementsToVarargsInNamedForm && !isArrayOrArrayLiteral(argument, trace)) return false

    return isParameterOfAnnotation(parameterDescriptor) && argument.isNamed() && parameterDescriptor.isVararg
}

private fun arrayAssignmentToVarargInNamedFormInFunction(
    parameterDescriptor: ValueParameterDescriptor,
    argument: ValueArgument,
    languageVersionSettings: LanguageVersionSettings,
    trace: BindingTrace
): Boolean {
    if (!languageVersionSettings.supportsFeature(LanguageFeature.AllowAssigningArrayElementsToVarargsInNamedFormForFunctions)) return false

    val isAllowedAssigningSingleElementsToVarargsInNamedForm =
        !languageVersionSettings.supportsFeature(LanguageFeature.ProhibitAssigningSingleElementsToVarargsInNamedForm)

    if (isAllowedAssigningSingleElementsToVarargsInNamedForm && !isArrayOrArrayLiteral(argument, trace)) return false

    return argument.isNamed() && parameterDescriptor.isVararg
}

fun isArrayOrArrayLiteral(argument: ValueArgument, trace: BindingTrace): Boolean {
    val argumentExpression = argument.getArgumentExpression() ?: return false
    if (argumentExpression is KtCollectionLiteralExpression) return true

    val type = trace.getType(argumentExpression) ?: return false
    return KotlinBuiltIns.isArrayOrPrimitiveArray(type)
}

private fun computeConstructorDispatchReceiver(
    containingClass: ClassDescriptor,
    scope: LexicalScope,
    substitutor: TypeSubstitutor?
): ReceiverValue? {
    return runIf(containingClass.isInner) {
        val outerClassType = (containingClass.containingDeclaration as? ClassDescriptor)?.defaultType ?: return null
        val substitutedOuterClassType = substitutor?.substitute(outerClassType, Variance.INVARIANT) ?: outerClassType
        scope.getImplicitReceiversHierarchy().firstOrNull {
            KotlinTypeChecker.DEFAULT.isSubtypeOf(it.type, substitutedOuterClassType)
        }?.value
    }
}

private fun computeConstructorDescriptorsToResolve(
    containingClass: ClassDescriptor,
    typeAliasDescriptorIfAny: TypeAliasDescriptor?,
    syntheticScopes: SyntheticScopes
): Collection<ConstructorDescriptor> {
    val simpleConstructors =
        typeAliasDescriptorIfAny?.constructors?.mapNotNull(TypeAliasConstructorDescriptor::withDispatchReceiver)
            ?: containingClass.constructors
    val syntheticConstructors = simpleConstructors.flatMap { syntheticScopes.collectSyntheticConstructors(it) }
    return simpleConstructors + syntheticConstructors
}

fun resolveConstructorCallWithGivenDescriptors(
    PSICallResolver: PSICallResolver,
    context: BasicCallResolutionContext,
    constructorType: KotlinType,
    useKnownTypeSubstitutor: Boolean,
    syntheticScopes: SyntheticScopes,
    tracingStrategy: TracingStrategy
): OverloadResolutionResults<ConstructorDescriptor> {
    val containingClass = constructorType.constructor.declarationDescriptor as ClassDescriptor

    @Suppress("NAME_SHADOWING")
    val constructorType = constructorType.unwrap()
    val constructorTypeAbbreviation = (constructorType as? AbbreviatedType)?.abbreviation
    val knownSubstitutor = runIf(useKnownTypeSubstitutor) {
        TypeSubstitutor.create(constructorTypeAbbreviation ?: constructorType)
    }
    val typeAliasDescriptor = constructorTypeAbbreviation?.constructor?.declarationDescriptor as? TypeAliasDescriptor

    val receiver = computeConstructorDispatchReceiver(containingClass, context.scope, knownSubstitutor)
    val allConstructors = runIf(!containingClass.isInner || receiver != null) {
        computeConstructorDescriptorsToResolve(containingClass, typeAliasDescriptor, syntheticScopes)
    }.orEmpty()

    val resolutionResults = PSICallResolver.runResolutionAndInferenceForGivenDescriptors<ConstructorDescriptor>(
        context,
        allConstructors,
        tracingStrategy,
        KotlinCallKind.FUNCTION,
        knownSubstitutor,
        receiver?.let { context.transformToReceiverWithSmartCastInfo(it) }
    )

    if (resolutionResults.isSingleResult) {
        context.trace.record(BindingContext.RESOLVED_CALL, context.call, resolutionResults.resultingCall)
        context.trace.record(BindingContext.CALL, context.call.calleeExpression ?: context.call.callElement, context.call)
    }

    return resolutionResults
}

internal fun PsiElement.reportOnElement() =
    (this as? KtConstructorDelegationCall)
        ?.takeIf { isImplicit }
        ?.let { getStrictParentOfType<KtSecondaryConstructor>()!! }
        ?: this

internal fun List<KotlinCallArgument>.replaceTypes(
    context: BasicCallResolutionContext,
    resolutionCallbacks: KotlinResolutionCallbacks,
    replace: (Int, UnwrappedType) -> UnwrappedType?,
): List<KotlinCallArgument> = mapIndexed { i, argument ->
    if (argument !is SimpleKotlinCallArgument) return@mapIndexed argument

    val psiExpression = argument.psiExpression ?: return@mapIndexed argument
    val argumentSubstitutor = if (argument is SubKotlinCallArgument) {
        val notFixedVariablesSubstitutor =
            argument.callResult.constraintSystem.buildNotFixedVariablesToPossibleResultType(resolutionCallbacks) as NewTypeSubstitutor
        val fixedVariablesSubstitutor =
            argument.callResult.constraintSystem.getBuilder().buildCurrentSubstitutor() as NewTypeSubstitutor

        ComposedSubstitutor(notFixedVariablesSubstitutor, fixedVariablesSubstitutor)
    } else EmptySubstitutor

    val newType = replace(i, argumentSubstitutor.safeSubstitute(argument.receiver.receiverValue.type.unwrap()))
        ?: return@mapIndexed argument

    ExpressionKotlinCallArgumentImpl(
        argument.psiCallArgument.valueArgument,
        argument.psiCallArgument.dataFlowInfoBeforeThisArgument,
        argument.psiCallArgument.dataFlowInfoAfterThisArgument,
        ReceiverValueWithSmartCastInfo(
            ExpressionReceiver.create(psiExpression, newType, context.trace.bindingContext),
            typesFromSmartCasts = emptySet(),
            isStable = true
        )
    )
}

internal fun PSIKotlinCall.replaceArguments(
    newArguments: List<KotlinCallArgument>,
    newReceiverArgument: ReceiverKotlinCallArgument? = null,
): PSIKotlinCall = PSIKotlinCallImpl(
    callKind, psiCall, tracingStrategy, newReceiverArgument, dispatchReceiverForInvokeExtension, name, typeArguments, newArguments,
    externalArgument, startingDataFlowInfo, resultDataFlowInfo, dataFlowInfoForArguments, isForImplicitInvoke
)

fun checkForConstructorCallOnFunctionalType(
    typeReference: KtTypeReference?,
    context: BasicCallResolutionContext
) {
    if (typeReference?.typeElement is KtFunctionType) {
        val factory = when (context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitConstructorCallOnFunctionalSupertype)) {
            true -> Errors.NO_CONSTRUCTOR
            false -> Errors.NO_CONSTRUCTOR_WARNING
        }
        context.trace.report(factory.on(context.call.getValueArgumentListOrElement()))
    }
}

fun transformToReceiverWithSmartCastInfo(
    containingDescriptor: DeclarationDescriptor,
    bindingContext: BindingContext,
    dataFlowInfo: DataFlowInfo,
    receiver: ReceiverValue,
    languageVersionSettings: LanguageVersionSettings,
    dataFlowValueFactory: DataFlowValueFactory
): ReceiverValueWithSmartCastInfo {
    val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiver, bindingContext, containingDescriptor)
    return ReceiverValueWithSmartCastInfo(
        receiver,
        dataFlowInfo.getCollectedTypes(dataFlowValue, languageVersionSettings).compactIfPossible(),
        dataFlowValue.isStable
    )
}

fun ResolutionContext<*>.transformToReceiverWithSmartCastInfo(receiver: ReceiverValue) = transformToReceiverWithSmartCastInfo(
    scope.ownerDescriptor, trace.bindingContext, dataFlowInfo, receiver, languageVersionSettings, dataFlowValueFactory
)

internal fun Call.isCallWithSuperReceiver(): Boolean = explicitReceiver is SuperCallReceiverValue

internal fun reportResolvedUsingDeprecatedVisibility(
    call: Call,
    candidateDescriptor: CallableDescriptor,
    resultingDescriptor: CallableDescriptor,
    diagnostic: ResolvedUsingDeprecatedVisibility,
    trace: BindingTrace
) {
    trace.record(
        BindingContext.DEPRECATED_SHORT_NAME_ACCESS,
        call.calleeExpression
    )

    val descriptorToLookup: DeclarationDescriptor = when (candidateDescriptor) {
        is ClassConstructorDescriptor -> candidateDescriptor.containingDeclaration
        is FakeCallableDescriptorForObject -> candidateDescriptor.classDescriptor
        is SyntheticMemberDescriptor<*> -> candidateDescriptor.baseDescriptorForSynthetic
        is PropertyDescriptor, is FunctionDescriptor -> candidateDescriptor
        else -> error(
            "Unexpected candidate descriptor of resolved call with " +
                    "ResolvedUsingDeprecatedVisibility-diagnostic: $candidateDescriptor\n" +
                    "Call context: ${call.callElement.parent?.text}"
        )
    }

    // If this descriptor was resolved from HierarchicalScope, then there can be another, non-deprecated path
    // in parents of base scope
    val sourceScope = diagnostic.baseSourceScope
    val canBeResolvedWithoutDeprecation = if (sourceScope is HierarchicalScope) {
        descriptorToLookup.canBeResolvedWithoutDeprecation(
            sourceScope,
            diagnostic.lookupLocation
        )
    } else {
        // Normally, that should be unreachable, but instead of asserting that, we will report diagnostic
        false
    }

    if (!canBeResolvedWithoutDeprecation) {
        trace.report(
            Errors.DEPRECATED_ACCESS_BY_SHORT_NAME.on(call.callElement, resultingDescriptor)
        )
    }
}
