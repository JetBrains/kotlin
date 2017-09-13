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

package org.jetbrains.kotlin.resolve.calls.callResolverUtil

import com.google.common.collect.Lists
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.CallTransformer
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getValueArgumentForExpression
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION
import org.jetbrains.kotlin.resolve.calls.inference.getNestedTypeVariables
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
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
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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
        KotlinTypeFactory.simpleType(type.annotations, type.constructor, newArguments, type.isMarkedNullable, type.memberScope)

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

    val receiverTypeConstructor = if (receiverType.constructor is IntersectionTypeConstructor) {
        val superTypesWithFakeArguments = receiverType.constructor.supertypes.map { supertype ->
            val fakeArguments = supertype.arguments.map { TypeProjectionImpl(it.projectionKind, DONT_CARE) }
            supertype.replace(fakeArguments)
        }

        IntersectionTypeConstructor(superTypesWithFakeArguments)
    } else {
        receiverType.constructor
    }

    return KotlinTypeFactory.simpleType(receiverType.annotations, receiverTypeConstructor, fakeTypeArguments,
                                        receiverType.isMarkedNullable, ErrorUtils.createErrorScope("Error scope for erased receiver type", /*throwExceptions=*/true))
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
        call.calleeExpression.let { it is KtConstructorCalleeExpression  || it is KtConstructorDelegationReferenceExpression }

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
        argument: ValueArgument,
        context: ResolutionContext<*>
): KotlinType {
    if (argument.getSpreadElement() != null || shouldCheckAsArray(parameterDescriptor, argument, context)) {
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

private fun shouldCheckAsArray(
        parameterDescriptor: ValueParameterDescriptor,
        argument: ValueArgument,
        context: ResolutionContext<*>
): Boolean {
    if (!context.languageVersionSettings.supportsFeature(LanguageFeature.AssigningArraysToVarargsInNamedFormInAnnotations)) return false

    if (!isParameterOfAnnotation(parameterDescriptor)) return false

    return argument.isNamed() && parameterDescriptor.isVararg && isArrayOrArrayLiteral(argument, context)
}

fun isParameterOfAnnotation(parameterDescriptor: ValueParameterDescriptor): Boolean {
    val constructedClass = parameterDescriptor.containingDeclaration.safeAs<ConstructorDescriptor>()?.constructedClass
    return DescriptorUtils.isAnnotationClass(constructedClass)
}

fun isArrayOrArrayLiteral(argument: ValueArgument, context: ResolutionContext<*>): Boolean {
    val argumentExpression = argument.getArgumentExpression() ?: return false
    if (argumentExpression is KtCollectionLiteralExpression) return true

    val type = context.trace.getType(argumentExpression) ?: return false
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

    val constructors = typeAliasDescriptor?.constructors?.mapNotNull(TypeAliasConstructorDescriptor::withDispatchReceiver) ?: classWithConstructors.constructors

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
    }
    else {
        receiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        dispatchReceiver = null
    }

    val syntheticConstructors = constructors.flatMap { syntheticScopes.collectSyntheticConstructors(it) }

    return (constructors + syntheticConstructors).map {
        ResolutionCandidate.create(call, it, dispatchReceiver, receiverKind, knownSubstitutor)
    }
}

fun KtLambdaExpression.getCorrespondingParameterForFunctionArgument(
        bindingContext: BindingContext
): ValueParameterDescriptor? {
    val resolvedCall = KtPsiUtil.getParentCallIfPresent(this)?.getResolvedCall(bindingContext) ?: return null
    val valueArgument =
            resolvedCall.call.getValueArgumentForExpression(this)
            ?: return null
    val mapping = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return null

    return mapping.valueParameter
}
