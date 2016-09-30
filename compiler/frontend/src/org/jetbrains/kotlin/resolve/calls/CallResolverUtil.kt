/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.callResolverUtil

import com.google.common.collect.Lists
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.getNestedTypeVariables
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.contains

enum class ResolveArgumentsMode {
    RESOLVE_FUNCTION_ARGUMENTS,
    SHAPE_FUNCTION_ARGUMENTS
}


fun hasUnknownFunctionParameter(type: KotlinType): Boolean {
    assert(ReflectionTypes.isCallableType(type)) { "type $type is not a function or property" }
    return getParameterArgumentsOfCallableType(type).any {
        it.type.contains { TypeUtils.isDontCarePlaceholder(it) } || ErrorUtils.containsUninferredParameter(it.type)
    }
}

fun hasUnknownReturnType(type: KotlinType): Boolean {
    assert(ReflectionTypes.isCallableType(type)) { "type $type is not a function or property" }
    return ErrorUtils.containsErrorType(getReturnTypeForCallable(type))
}

fun replaceReturnTypeForCallable(type: KotlinType, given: KotlinType): KotlinType {
    assert(ReflectionTypes.isCallableType(type)) { "type $type is not a function or property" }
    val newArguments = Lists.newArrayList<TypeProjection>()
    newArguments.addAll(getParameterArgumentsOfCallableType(type))
    newArguments.add(TypeProjectionImpl(Variance.INVARIANT, given))
    return replaceTypeArguments(type, newArguments)
}

fun replaceReturnTypeByUnknown(type: KotlinType) = replaceReturnTypeForCallable(type, DONT_CARE)

private fun replaceTypeArguments(type: KotlinType, newArguments: List<TypeProjection>) =
        KotlinTypeImpl.create(type.annotations, type.constructor, type.isMarkedNullable, newArguments, type.memberScope)

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

fun getErasedReceiverType(receiverParameterDescriptor: ReceiverParameterDescriptor, descriptor: CallableDescriptor): KotlinType {
    var receiverType = receiverParameterDescriptor.type
    for (typeParameter in descriptor.typeParameters) {
        if (typeParameter.typeConstructor == receiverType.constructor) {
            receiverType = TypeIntersector.getUpperBoundsAsType(typeParameter)
        }
    }
    val fakeTypeArguments = ContainerUtil.newSmartList<TypeProjection>()
    for (typeProjection in receiverType.arguments) {
        fakeTypeArguments.add(TypeProjectionImpl(typeProjection.projectionKind, DONT_CARE))
    }
    return KotlinTypeImpl.create(receiverType.annotations, receiverType.constructor, receiverType.isMarkedNullable, fakeTypeArguments,
                                 ErrorUtils.createErrorScope("Error scope for erased receiver type", /*throwExceptions=*/true))
}

fun isOrOverridesSynthesized(descriptor: CallableMemberDescriptor): Boolean {
    if (descriptor.kind == CallableMemberDescriptor.Kind.SYNTHESIZED) {
        return true
    }
    if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return descriptor.overriddenDescriptors.all {
            isOrOverridesSynthesized(it)
        }
    }
    return false
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

fun getEffectiveExpectedType(parameterDescriptor: ValueParameterDescriptor, argument: ValueArgument): KotlinType {
    if (argument.getSpreadElement() != null) {
        if (parameterDescriptor.varargElementType == null) {
            // Spread argument passed to a non-vararg parameter, an error is already reported by ValueArgumentsToParametersMapper
            return DONT_CARE
        }
        return parameterDescriptor.type
    }
    val varargElementType = parameterDescriptor.varargElementType
    if (varargElementType != null) {
        return varargElementType
    }

    return parameterDescriptor.type
}

fun createResolutionCandidatesForConstructors(
        lexicalScope: LexicalScope,
        call: Call,
        classWithConstructors: ClassDescriptor,
        knownSubstitutor: TypeSubstitutor? = null
): Collection<ResolutionCandidate<ConstructorDescriptor>> {
    val constructors = classWithConstructors.constructors

    if (constructors.isEmpty()) return emptyList()

    val receiverKind: ExplicitReceiverKind
    val dispatchReceiver: ReceiverValue?

    if (classWithConstructors.isInner) {
        val outerClassType = (classWithConstructors.containingDeclaration as? ClassDescriptor)?.defaultType ?: return emptyList()
        val substitutedOuterClassType = knownSubstitutor?.let { it.substitute(outerClassType, Variance.INVARIANT) } ?: outerClassType

        val receiver = lexicalScope.getImplicitReceiversHierarchy().firstOrNull {
            KotlinTypeChecker.DEFAULT.isSubtypeOf(it.type, substitutedOuterClassType)
        } ?: return emptyList()

        receiverKind = ExplicitReceiverKind.DISPATCH_RECEIVER
        dispatchReceiver = receiver.value
    }
    else {
        receiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        dispatchReceiver = null
    }

    return constructors.map { ResolutionCandidate.create(call, it, dispatchReceiver, receiverKind, knownSubstitutor) }
}
