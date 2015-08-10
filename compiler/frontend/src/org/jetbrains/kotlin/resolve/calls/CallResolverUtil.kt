/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetSuperExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.DONT_CARE
import org.jetbrains.kotlin.types.typeUtil.getNestedArguments
import java.util.*

public enum class ResolveArgumentsMode {
    RESOLVE_FUNCTION_ARGUMENTS,
    SHAPE_FUNCTION_ARGUMENTS
}


public fun hasUnknownFunctionParameter(type: JetType): Boolean {
    assert(ReflectionTypes.isCallableType(type)) { "type $type is not a function or property" }
    return getParameterArgumentsOfCallableType(type).any {
        TypeUtils.containsSpecialType(it.getType(), DONT_CARE) || ErrorUtils.containsUninferredParameter(it.getType())
    }
}

public fun hasUnknownReturnType(type: JetType): Boolean {
    assert(ReflectionTypes.isCallableType(type)) { "type $type is not a function or property" }
    return ErrorUtils.containsErrorType(getReturnTypeForCallable(type))
}

public fun replaceReturnTypeByUnknown(type: JetType): JetType {
    assert(ReflectionTypes.isCallableType(type)) { "type $type is not a function or property" }
    val newArguments = Lists.newArrayList<TypeProjection>()
    newArguments.addAll(getParameterArgumentsOfCallableType(type))
    newArguments.add(TypeProjectionImpl(Variance.INVARIANT, DONT_CARE))
    return replaceTypeArguments(type, newArguments)
}

private fun replaceTypeArguments(type: JetType, newArguments: List<TypeProjection>) =
        JetTypeImpl.create(type.getAnnotations(), type.getConstructor(), type.isMarkedNullable(), newArguments, type.getMemberScope())

private fun getParameterArgumentsOfCallableType(type: JetType) =
        type.getArguments().dropLast(1)

private fun getReturnTypeForCallable(type: JetType) =
        type.getArguments().last().getType()

private fun CallableDescriptor.hasReturnTypeDependentOnUninferredParams(constraintSystem: ConstraintSystem): Boolean {
    val returnType = getReturnType() ?: return false

    val nestedTypeVariables = with (constraintSystem as ConstraintSystemImpl) {
        returnType.getNestedTypeVariables()
    }
    return nestedTypeVariables.any { constraintSystem.getTypeBounds(it).value == null }
}

public fun CallableDescriptor.hasInferredReturnType(constraintSystem: ConstraintSystem): Boolean {
    if (hasReturnTypeDependentOnUninferredParams(constraintSystem)) return false

    // Expected type mismatch was reported before as 'TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH'
    if (constraintSystem.getStatus().hasOnlyErrorsDerivedFrom(EXPECTED_TYPE_POSITION)) return false
    return true
}

public fun getErasedReceiverType(receiverParameterDescriptor: ReceiverParameterDescriptor, descriptor: CallableDescriptor): JetType {
    var receiverType = receiverParameterDescriptor.getType()
    for (typeParameter in descriptor.getTypeParameters()) {
        if (typeParameter.getTypeConstructor() == receiverType.getConstructor()) {
            receiverType = typeParameter.getUpperBoundsAsType()
        }
    }
    val fakeTypeArguments = ContainerUtil.newSmartList<TypeProjection>()
    for (typeProjection in receiverType.getArguments()) {
        fakeTypeArguments.add(TypeProjectionImpl(typeProjection.getProjectionKind(), DONT_CARE))
    }
    return JetTypeImpl.create(receiverType.getAnnotations(), receiverType.getConstructor(), receiverType.isMarkedNullable(), fakeTypeArguments,
                       ErrorUtils.createErrorScope("Error scope for erased receiver type", /*throwExceptions=*/true))
}

public fun isOrOverridesSynthesized(descriptor: CallableMemberDescriptor): Boolean {
    if (descriptor.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED) {
        return true
    }
    if (descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
        return descriptor.getOverriddenDescriptors().all {
            isOrOverridesSynthesized(it)
        }
    }
    return false
}

public fun isInvokeCallOnVariable(call: Call): Boolean {
    if (call.getCallType() !== Call.CallType.INVOKE) return false
    val dispatchReceiver = call.getDispatchReceiver()
    //calleeExpressionAsDispatchReceiver for invoke is always ExpressionReceiver, see CallForImplicitInvoke
    val expression = (dispatchReceiver as ExpressionReceiver).getExpression()
    return expression is JetSimpleNameExpression
}

public fun isInvokeCallOnExpressionWithBothReceivers(call: Call): Boolean {
    if (call.getCallType() !== Call.CallType.INVOKE || isInvokeCallOnVariable(call)) return false
    return call.getExplicitReceiver().exists() && call.getDispatchReceiver().exists()
}

public fun getSuperCallExpression(call: Call): JetSuperExpression? {
    return (call.getExplicitReceiver() as? ExpressionReceiver)?.getExpression() as? JetSuperExpression
}

public fun getEffectiveExpectedType(parameterDescriptor: ValueParameterDescriptor, argument: ValueArgument): JetType {
    if (argument.getSpreadElement() != null) {
        if (parameterDescriptor.getVarargElementType() == null) {
            // Spread argument passed to a non-vararg parameter, an error is already reported by ValueArgumentsToParametersMapper
            return DONT_CARE
        }
        return parameterDescriptor.getType()
    }
    val varargElementType = parameterDescriptor.getVarargElementType()
    if (varargElementType != null) {
        return varargElementType
    }

    return parameterDescriptor.getType()
}
