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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.callableReferences.createReflectionTypeForCallableDescriptor
import org.jetbrains.kotlin.resolve.callableReferences.resolvePossiblyAmbiguousCallableReference
import org.jetbrains.kotlin.resolve.calls.CallExpressionResolver
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.source.toSourceElement
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

        var type: KotlinType? = null
        if (possiblyBareType.isBare) {
            if (!possiblyBareType.isNullable) {
                val descriptor = possiblyBareType.bareTypeConstructor.declarationDescriptor
                if (descriptor is ClassDescriptor) {
                    if (KotlinBuiltIns.isNonPrimitiveArray(descriptor)) {
                        context.trace.report(ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT.on(expression))
                        return null
                    }
                    type = KotlinTypeImpl.create(
                            Annotations.EMPTY, descriptor, false, descriptor.typeConstructor.parameters.map(TypeUtils::makeStarProjection)
                    )
                }
            }
        }
        else {
            val actualType = possiblyBareType.actualType
            if (actualType.isError) return null
            if (isAllowedInClassLiteral(actualType)) {
                type = actualType
            }
            else {
                val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(actualType)
                if (!actualType.isMarkedNullable && typeParameterDescriptor != null && !typeParameterDescriptor.isReified) {
                    c.trace.report(TYPE_PARAMETER_AS_REIFIED.on(expression, typeParameterDescriptor))
                    return null
                }
            }
        }

        if (type != null) {
            checkNoExpressionOnLHS(expression, c) { "" }
            return type
        }

        context.trace.report(CLASS_LITERAL_LHS_NOT_A_CLASS.on(expression))
        return null
    }

    private fun isAllowedInClassLiteral(type: KotlinType): Boolean =
            isClassifierAvailableAtRuntime(type, false)

    private fun isClassifierAvailableAtRuntime(type: KotlinType, canBeNullable: Boolean): Boolean {
        if (type.isMarkedNullable && !canBeNullable) return false

        val typeConstructor = type.constructor
        val typeDeclarationDescriptor = typeConstructor.declarationDescriptor
        val typeIsArray = KotlinBuiltIns.isArray(type)

        when (typeDeclarationDescriptor) {
            is ClassDescriptor -> {
                val parameters = typeConstructor.parameters
                if (parameters.size != type.arguments.size) return false

                val typeArgumentsIterator = type.arguments.iterator()
                for (parameter in parameters) {
                    if (!typeIsArray && !parameter.isReified) return false

                    val typeArgument = typeArgumentsIterator.next() ?: return false

                    if (typeArgument.isStarProjection) return false
                    if (!isClassifierAvailableAtRuntime(typeArgument.type, true)) return false
                }

                return true
            }
            is TypeParameterDescriptor -> return typeDeclarationDescriptor.isReified
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
            checkNoExpressionOnLHS(expression, c) {
                "Original result: ${expression.callableReference.getResolvedCall(c.trace.bindingContext)?.resultingDescriptor}"
            }
        }
        return dataFlowAnalyzer.createCheckedTypeInfo(result, c, expression)
    }

    private fun hasErrors(trace: TemporaryBindingTrace): Boolean =
            trace.bindingContext.diagnostics.all().any { diagnostic -> diagnostic.severity == Severity.ERROR }

    private fun checkNoExpressionOnLHS(expression: KtDoubleColonExpression, c: ExpressionTypingContext, extraInfo: () -> String) {
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
                "Expressions on left-hand side of ${expression.javaClass.simpleName} are not supported yet.\n" +
                "Resolution result: %s\n%s",
                resultingCalls.map { call -> call.resultingDescriptor },
                extraInfo()
        ))
    }

    private fun getCallableReferenceType(
            expression: KtCallableReferenceExpression,
            lhsType: KotlinType?,
            context: ExpressionTypingContext
    ): KotlinType? {
        val resolutionResults = resolvePossiblyAmbiguousCallableReference(
                expression, lhsType, context, ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS, callResolver
        )
        val descriptor =
                if (resolutionResults != null && !resolutionResults.isNothing) {
                    val resolvedCall = OverloadResolutionResultsUtil.getResultingCall(resolutionResults, context.contextDependency)
                    resolvedCall?.resultingDescriptor ?: return null
                }
                else {
                    context.trace.report(UNRESOLVED_REFERENCE.on(expression.callableReference, expression.callableReference))
                    return null
                }

        checkReferenceIsToAllowedMember(descriptor, context.trace, expression)

        val type = createReflectionTypeForCallableDescriptor(
                descriptor, lhsType, reflectionTypes, expression.isEmptyLHS, context.scope.ownerDescriptor
        ) ?: return null

        when (descriptor) {
            is FunctionDescriptor -> bindFunctionReference(expression, type, context)
            is PropertyDescriptor -> bindPropertyReference(expression, type, context)
        }

        return type
    }

    private fun checkReferenceIsToAllowedMember(
            descriptor: CallableDescriptor, trace: BindingTrace, expression: KtCallableReferenceExpression
    ) {
        val simpleName = expression.callableReference
        if (expression.isEmptyLHS &&
            (descriptor.dispatchReceiverParameter != null || descriptor.extensionReceiverParameter != null)) {
            trace.report(CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS.on(simpleName))
        }

        val containingDeclaration = descriptor.containingDeclaration
        if (DescriptorUtils.isObject(containingDeclaration)) {
            trace.report(CALLABLE_REFERENCE_TO_OBJECT_MEMBER.on(simpleName))
        }
        if (descriptor is ConstructorDescriptor && DescriptorUtils.isAnnotationClass(containingDeclaration)) {
            trace.report(CALLABLE_REFERENCE_TO_ANNOTATION_CONSTRUCTOR.on(simpleName))
        }
        if (descriptor is CallableMemberDescriptor && isMemberExtension(descriptor)) {
            trace.report(EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED.on(simpleName, descriptor))
        }
        if (descriptor is VariableDescriptor && descriptor !is PropertyDescriptor) {
            trace.report(UNSUPPORTED.on(simpleName, "References to variables aren't supported yet"))
        }
    }

    private fun isMemberExtension(descriptor: CallableMemberDescriptor): Boolean {
        val original = (descriptor as? ImportedFromObjectCallableDescriptor<*>)?.callableFromObject ?: descriptor
        return original.extensionReceiverParameter != null && original.dispatchReceiverParameter != null
    }

    private fun bindFunctionReference(expression: KtCallableReferenceExpression, type: KotlinType, context: ResolutionContext<*>) {
        val functionDescriptor = AnonymousFunctionDescriptor(
                context.scope.ownerDescriptor,
                Annotations.EMPTY,
                CallableMemberDescriptor.Kind.DECLARATION,
                expression.toSourceElement()
        )

        functionDescriptor.initialize(
                null, null, emptyList(),
                createValueParametersForInvokeInFunctionType(functionDescriptor, type.arguments.dropLast(1)),
                type.arguments.last().type,
                Modality.FINAL,
                Visibilities.PUBLIC
        )

        context.trace.record(BindingContext.FUNCTION, expression, functionDescriptor)
    }

    private fun bindPropertyReference(expression: KtCallableReferenceExpression, referenceType: KotlinType, context: ResolutionContext<*>) {
        val localVariable = LocalVariableDescriptor(
                context.scope.ownerDescriptor, Annotations.EMPTY, Name.special("<anonymous>"), referenceType, /* mutable = */ false,
                expression.toSourceElement()
        )

        context.trace.record(BindingContext.VARIABLE, expression, localVariable)
    }
}
