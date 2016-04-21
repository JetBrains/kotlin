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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.TypeResolutionContext
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.callableReferences.createReflectionTypeForResolvedCallableReference
import org.jetbrains.kotlin.resolve.callableReferences.resolveCallableReferenceTarget
import org.jetbrains.kotlin.resolve.calls.CallExpressionResolver
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeImpl
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo

class DoubleColonExpressionResolver(
        val callResolver: CallResolver,
        val callExpressionResolver: CallExpressionResolver,
        val dataFlowAnalyzer: DataFlowAnalyzer,
        val reflectionTypes: ReflectionTypes,
        val typeResolver: TypeResolver
) {
    fun visitClassLiteralExpression(expression: KtClassLiteralExpression, c: ExpressionTypingContext): KotlinTypeInfo {
        val type = resolveClassLiteral(expression, c)
        if (type != null && !type.isError) {
            return dataFlowAnalyzer.createCheckedTypeInfo(reflectionTypes.getKClassType(Annotations.EMPTY, type), c, expression)
        }

        return createTypeInfo(ErrorUtils.createErrorType("Unresolved class"), c)
    }

    private fun resolveClassLiteral(expression: KtClassLiteralExpression, c: ExpressionTypingContext): KotlinType? {
        if (expression.isEmptyLHS) {
            // "::class" will maybe mean "this::class", a class of "this" instance
            c.trace.report(UNSUPPORTED.on(expression, "Class literals with empty left hand side are not yet supported"))
            return null
        }

        val context = TypeResolutionContext(
                c.scope, c.trace, /* checkBounds = */ false, /* allowBareTypes = */ true, /* isDebuggerContext = */ false
        )
        val possiblyBareType = typeResolver.resolvePossiblyBareType(context, expression.typeReference!!)

        if (!possiblyBareType.isBare && possiblyBareType.actualType.isError) {
            return null
        }

        var reportError = false
        val type: KotlinType
        if (possiblyBareType.isBare) {
            val descriptor = possiblyBareType.bareTypeConstructor.declarationDescriptor as? ClassDescriptor
                             ?: error("Only classes can produce bare types: $possiblyBareType")
            if (KotlinBuiltIns.isNonPrimitiveArray(descriptor)) {
                context.trace.report(ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT.on(expression))
            }

            type = KotlinTypeImpl.create(
                    Annotations.EMPTY, descriptor, possiblyBareType.isNullable,
                    descriptor.typeConstructor.parameters.map(TypeUtils::makeStarProjection)
            )
        }
        else {
            type = possiblyBareType.actualType
            reportError = !isAllowedInClassLiteral(type)
        }

        if (type.isMarkedNullable || reportError) {
            context.trace.report(CLASS_LITERAL_LHS_NOT_A_CLASS.on(expression))
        }

        return type
    }

    private fun isAllowedInClassLiteral(type: KotlinType): Boolean {
        val typeConstructor = type.constructor
        val descriptor = typeConstructor.declarationDescriptor

        when (descriptor) {
            is ClassDescriptor -> {
                if (KotlinBuiltIns.isNonPrimitiveArray(descriptor)) {
                    return type.arguments.none { typeArgument ->
                        typeArgument.isStarProjection || !isAllowedInClassLiteral(typeArgument.type)
                    }
                }

                return type.arguments.isEmpty()
            }
            is TypeParameterDescriptor -> return descriptor.isReified
            else -> return false
        }
    }

    fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, c: ExpressionTypingContext): KotlinTypeInfo {
        val typeReference = expression.typeReference

        val receiverType = typeReference?.let { typeReference ->
            typeResolver.resolveType(c.scope, typeReference, c.trace, false)
        }

        val callableReference = expression.callableReference
        if (callableReference.getReferencedName().isEmpty()) {
            c.trace.report(UNRESOLVED_REFERENCE.on(callableReference, callableReference))
            val errorType = ErrorUtils.createErrorType("Empty callable reference")
            return dataFlowAnalyzer.createCheckedTypeInfo(errorType, c, expression)
        }

        val trace = TemporaryBindingTrace.create(c.trace, "Callable reference type")
        val result = getCallableReferenceType(expression, receiverType, c.replaceBindingTrace(trace))
        val hasErrors = hasErrors(trace) // Do not inline this local variable (execution order is important)
        trace.commit()
        if (!hasErrors && result != null) {
            checkNoExpressionOnLHS(expression, c)
        }
        return dataFlowAnalyzer.createCheckedTypeInfo(result, c, expression)
    }

    private fun hasErrors(trace: TemporaryBindingTrace): Boolean =
            trace.bindingContext.diagnostics.all().any { diagnostic -> diagnostic.severity == Severity.ERROR }

    private fun checkNoExpressionOnLHS(expression: KtCallableReferenceExpression, c: ExpressionTypingContext) {
        val typeReference = expression.typeReference ?: return
        var typeElement = typeReference.typeElement as? KtUserType ?: return

        while (true) {
            if (typeElement.typeArgumentList != null) return
            typeElement = typeElement.qualifier ?: break
        }

        val simpleNameExpression = typeElement.referenceExpression ?: return

        val traceAndCache = TemporaryTraceAndCache.create(c, "Resolve expression on LHS of callable reference", simpleNameExpression)
        val resolutionResult = callExpressionResolver.resolveSimpleName(c.replaceTraceAndCache(traceAndCache), simpleNameExpression)

        val resultingCalls = resolutionResult.resultingCalls.filter { call ->
            call.status.possibleTransformToSuccess() && !ErrorUtils.isError(call.resultingDescriptor)
        }
        if (resultingCalls.isEmpty()) return

        if (resultingCalls.singleOrNull()?.resultingDescriptor is FakeCallableDescriptorForObject) return

        throw AssertionError(String.format(
                "Expressions on left-hand side of callable reference are not supported yet.\n" +
                "Resolution result: %s\n" +
                "Original result: %s",
                resultingCalls.map { call -> call.resultingDescriptor },
                expression.callableReference.getResolvedCall(c.trace.bindingContext)?.resultingDescriptor
        ))
    }

    private fun getCallableReferenceType(
            expression: KtCallableReferenceExpression,
            lhsType: KotlinType?,
            context: ExpressionTypingContext
    ): KotlinType? {
        val reference = expression.callableReference

        val resolved = BooleanArray(1)
        val descriptor = resolveCallableReferenceTarget(expression, lhsType, context, resolved, callResolver)
        if (!resolved[0]) {
            context.trace.report(UNRESOLVED_REFERENCE.on(reference, reference))
        }
        if (descriptor == null) return null

        if (expression.isEmptyLHS &&
            (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null)) {
            context.trace.report(CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS.on(reference))
        }

        val containingDeclaration = descriptor.containingDeclaration
        if (DescriptorUtils.isObject(containingDeclaration)) {
            context.trace.report(CALLABLE_REFERENCE_TO_OBJECT_MEMBER.on(reference))
        }
        if (descriptor is ConstructorDescriptor && DescriptorUtils.isAnnotationClass(containingDeclaration)) {
            context.trace.report(CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR.on(reference))
        }

        return createReflectionTypeForResolvedCallableReference(expression, lhsType, descriptor, context, reflectionTypes)
    }
}
