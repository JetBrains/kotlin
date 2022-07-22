/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.util

import com.google.common.collect.Lists
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
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
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.EmptySubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.getNestedTypeVariables
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isParameterOfAnnotation
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.collectSyntheticConstructors
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.error.ErrorScopeKind
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.util.buildNotFixedVariablesToPossibleResultType
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.runIf

enum class ResolveArgumentsMode {
    RESOLVE_FUNCTION_ARGUMENTS,
    SHAPE_FUNCTION_ARGUMENTS
}


fun hasUnknownFunctionParameter(type: KotlinType): Boolean {
    assert(ReflectionTypes.isCallableType(type) || type.isSuspendFunctionType) { "type $type is not a function or property" }
    return getParameterArgumentsOfCallableType(type).any { typeProjection ->
        typeProjection.type.contains { TypeUtils.isDontCarePlaceholder(it) }
                || ErrorUtils.containsUninferredTypeVariable(typeProjection.type)
    }
}

fun hasUnknownReturnType(type: KotlinType): Boolean {
    assert(ReflectionTypes.isCallableType(type) || type.isSuspendFunctionType) { "type $type is not a function or property" }
    return ErrorUtils.containsErrorType(getReturnTypeForCallable(type))
}

fun replaceReturnTypeForCallable(type: KotlinType, given: KotlinType): KotlinType {
    assert(ReflectionTypes.isCallableType(type) || type.isSuspendFunctionType) { "type $type is not a function or property" }
    val newArguments = Lists.newArrayList<TypeProjection>()
    newArguments.addAll(getParameterArgumentsOfCallableType(type))
    newArguments.add(TypeProjectionImpl(Variance.INVARIANT, given))
    return replaceTypeArguments(type, newArguments)
}

fun replaceReturnTypeByUnknown(type: KotlinType) = replaceReturnTypeForCallable(type, DONT_CARE)

private fun replaceTypeArguments(type: KotlinType, newArguments: List<TypeProjection>) =
    KotlinTypeFactory.simpleType(type.attributes, type.constructor, newArguments, type.isMarkedNullable)

private fun getParameterArgumentsOfCallableType(type: KotlinType) =
    type.arguments.dropLast(1)

fun getReturnTypeForCallable(type: KotlinType) =
    type.arguments.last().type

private fun CallableDescriptor.hasReturnTypeDependentOnUninferredParams(constraintSystem: ConstraintSystem): Boolean {
    val returnType = returnType ?: return false
    val nestedTypeVariables = constraintSystem.getNestedTypeVariables(returnType)
    return nestedTypeVariables.any { constraintSystem.getTypeBounds(it).value == null }
}

fun CallableDescriptor.hasInferredReturnType(constraintSystem: ConstraintSystem): Boolean {
    if (hasReturnTypeDependentOnUninferredParams(constraintSystem)) return false

    // Expected type mismatch was reported before as 'TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH'
    if (constraintSystem.status.hasOnlyErrorsDerivedFrom(EXPECTED_TYPE_POSITION)) return false
    return true
}

private fun filterOutTypeParameters(upperBounds: List<KotlinType>, candidateDescriptor: CallableDescriptor): List<KotlinType> {
    if (upperBounds.size < 2) return upperBounds
    val result = upperBounds.filterNot {
        val declarationDescriptor = it.constructor.declarationDescriptor
        declarationDescriptor is TypeParameterDescriptor && declarationDescriptor.containingDeclaration == candidateDescriptor
    }
    if (result.isEmpty()) return upperBounds
    return result
}

fun getErasedReceiverType(receiverParameterDescriptor: ReceiverParameterDescriptor, descriptor: CallableDescriptor): KotlinType {
    var receiverType = receiverParameterDescriptor.type
    for (typeParameter in descriptor.typeParameters) {
        if (typeParameter.typeConstructor == receiverType.constructor) {
            val properUpperBounds = filterOutTypeParameters(typeParameter.upperBounds, descriptor)
            receiverType = TypeIntersector.intersectUpperBounds(typeParameter, properUpperBounds)
        }
    }
    val fakeTypeArguments = SmartList<TypeProjection>()
    for (typeProjection in receiverType.arguments) {
        fakeTypeArguments.add(TypeProjectionImpl(typeProjection.projectionKind, DONT_CARE))
    }

    val oldReceiverTypeConstructor = receiverType.constructor
    val receiverTypeConstructor = if (oldReceiverTypeConstructor is IntersectionTypeConstructor) {
        oldReceiverTypeConstructor.transformComponents { supertype ->
            val fakeArguments = supertype.arguments.map { TypeProjectionImpl(it.projectionKind, DONT_CARE) }
            supertype.replace(fakeArguments)
        } ?: oldReceiverTypeConstructor
    } else {
        oldReceiverTypeConstructor
    }

    return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
        receiverType.attributes, receiverTypeConstructor, fakeTypeArguments,
        receiverType.isMarkedNullable, ErrorUtils.createErrorScope(ErrorScopeKind.ERASED_RECEIVER_TYPE_SCOPE, throwExceptions = true)
    )
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

fun isInvokeCallOnExpressionWithBothReceivers(call: Call): Boolean {
    if (call.callType !== Call.CallType.INVOKE || isInvokeCallOnVariable(call)) return false
    return call.explicitReceiver != null && call.dispatchReceiver != null
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
