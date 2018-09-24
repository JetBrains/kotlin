/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.callResolverUtil

import com.google.common.collect.Lists
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.getNestedTypeVariables
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.descriptorUtil.isParameterOfAnnotation
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.collectSyntheticConstructors
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.types.typeUtil.contains

enum class ResolveArgumentsMode {
    RESOLVE_FUNCTION_ARGUMENTS,
    SHAPE_FUNCTION_ARGUMENTS
}


fun hasUnknownFunctionParameter(type: KotlinType): Boolean {
    assert(ReflectionTypes.isCallableType(type) || type.isSuspendFunctionType) { "type $type is not a function or property" }
    return getParameterArgumentsOfCallableType(type).any {
        it.type.contains { TypeUtils.isDontCarePlaceholder(it) } || ErrorUtils.containsUninferredParameter(it.type)
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
    KotlinTypeFactory.simpleType(type.annotations, type.constructor, newArguments, type.isMarkedNullable)

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
    val fakeTypeArguments = ContainerUtil.newSmartList<TypeProjection>()
    for (typeProjection in receiverType.arguments) {
        fakeTypeArguments.add(TypeProjectionImpl(typeProjection.projectionKind, DONT_CARE))
    }

    val receiverTypeConstructor = if (receiverType.constructor is IntersectionTypeConstructor) {
        val superTypesWithFakeArguments = receiverType.constructor.supertypes.map { supertype ->
            val fakeArguments = supertype.arguments.map { TypeProjectionImpl(it.projectionKind, DONT_CARE) }
            supertype.replace(fakeArguments)
        }

        IntersectionTypeConstructor(superTypesWithFakeArguments)
    } else {
        receiverType.constructor
    }

    return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
        receiverType.annotations, receiverTypeConstructor, fakeTypeArguments,
        receiverType.isMarkedNullable, ErrorUtils.createErrorScope("Error scope for erased receiver type", /*throwExceptions=*/true)
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

    val name = OperatorConventions.getNameForOperationSymbol(operator, true, true)
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

    if (arrayAssignmentToVarargInNamedFormInAnnotation(parameterDescriptor, argument, languageVersionSettings, trace)) {
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

    if (!isParameterOfAnnotation(parameterDescriptor)) return false

    return argument.isNamed() && parameterDescriptor.isVararg && isArrayOrArrayLiteral(argument, trace)
}

fun isArrayOrArrayLiteral(argument: ValueArgument, trace: BindingTrace): Boolean {
    val argumentExpression = argument.getArgumentExpression() ?: return false
    if (argumentExpression is KtCollectionLiteralExpression) return true

    val type = trace.getType(argumentExpression) ?: return false
    return KotlinBuiltIns.isArrayOrPrimitiveArray(type)
}

fun createResolutionCandidatesForConstructors(
    lexicalScope: LexicalScope,
    call: Call,
    typeWithConstructors: KotlinType,
    useKnownTypeSubstitutor: Boolean,
    syntheticScopes: SyntheticScopes
): List<ResolutionCandidate<ConstructorDescriptor>> {
    val classWithConstructors = typeWithConstructors.constructor.declarationDescriptor as ClassDescriptor

    val unwrappedType = typeWithConstructors.unwrap()
    val knownSubstitutor =
        if (useKnownTypeSubstitutor)
            TypeSubstitutor.create(
                (unwrappedType as? AbbreviatedType)?.abbreviation ?: unwrappedType
            )
        else null

    val typeAliasDescriptor =
        if (unwrappedType is AbbreviatedType)
            unwrappedType.abbreviation.constructor.declarationDescriptor as? TypeAliasDescriptor
        else
            null

    val constructors = typeAliasDescriptor?.constructors?.mapNotNull(TypeAliasConstructorDescriptor::withDispatchReceiver)
            ?: classWithConstructors.constructors

    if (constructors.isEmpty()) return emptyList()

    val receiverKind: ExplicitReceiverKind
    val dispatchReceiver: ReceiverValue?

    if (classWithConstructors.isInner) {
        val outerClassType = (classWithConstructors.containingDeclaration as? ClassDescriptor)?.defaultType ?: return emptyList()
        val substitutedOuterClassType = knownSubstitutor?.substitute(outerClassType, Variance.INVARIANT) ?: outerClassType

        val receiver = lexicalScope.getImplicitReceiversHierarchy().firstOrNull {
            KotlinTypeChecker.DEFAULT.isSubtypeOf(it.type, substitutedOuterClassType)
        } ?: return emptyList()

        receiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER
        dispatchReceiver = receiver.value
    } else {
        receiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        dispatchReceiver = null
    }

    val syntheticConstructors = constructors.flatMap { syntheticScopes.collectSyntheticConstructors(it) }

    return (constructors + syntheticConstructors).map {
        ResolutionCandidate.create(call, it, dispatchReceiver, receiverKind, knownSubstitutor)
    }
}
