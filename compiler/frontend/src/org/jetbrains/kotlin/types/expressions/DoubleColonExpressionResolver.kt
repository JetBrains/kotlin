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
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.context.*
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.calls.util.createValueParametersForInvokeInFunctionType
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.createTypeInfo
import java.lang.UnsupportedOperationException
import javax.inject.Inject

sealed class DoubleColonLHS(val type: KotlinType) {
    class Expression(typeInfo: KotlinTypeInfo, val isObject: Boolean) : DoubleColonLHS(typeInfo.type!!) {
        val dataFlowInfo: DataFlowInfo = typeInfo.dataFlowInfo
    }

    class Type(type: KotlinType, val possiblyBareType: PossiblyBareType) : DoubleColonLHS(type)
}

// Returns true if this expression has the form "A<B>" which means it's a type on the LHS of a double colon expression
internal val KtCallExpression.isWithoutValueArguments: Boolean
    get() = valueArgumentList == null && lambdaArguments.isEmpty()

class DoubleColonExpressionResolver(
        val callResolver: CallResolver,
        val qualifiedExpressionResolver: QualifiedExpressionResolver,
        val dataFlowAnalyzer: DataFlowAnalyzer,
        val reflectionTypes: ReflectionTypes,
        val typeResolver: TypeResolver,
        val languageVersionSettings: LanguageVersionSettings
) {
    private lateinit var expressionTypingServices: ExpressionTypingServices

    // component dependency cycle
    @Inject
    fun setExpressionTypingServices(expressionTypingServices: ExpressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices
    }

    fun visitClassLiteralExpression(expression: KtClassLiteralExpression, c: ExpressionTypingContext): KotlinTypeInfo {
        if (expression.isEmptyLHS) {
            // "::class" will maybe mean "this::class", a class of "this" instance
            c.trace.report(UNSUPPORTED.on(expression, "Class literals with empty left hand side are not yet supported"))
        }
        else {
            val result = resolveDoubleColonLHS(expression, c)
            val type = result?.type
            if (type != null && !type.isError) {
                checkClassLiteral(c, expression, result)
                val kClassType = reflectionTypes.getKClassType(Annotations.EMPTY, type)
                val dataFlowInfo = (result as? DoubleColonLHS.Expression)?.dataFlowInfo ?: c.dataFlowInfo
                return dataFlowAnalyzer.checkType(createTypeInfo(kClassType, dataFlowInfo), expression, c)
            }
        }

        return createTypeInfo(ErrorUtils.createErrorType("Unresolved class"), c)
    }

    private fun checkClassLiteral(c: ExpressionTypingContext, expression: KtClassLiteralExpression, result: DoubleColonLHS) {
        if (result is DoubleColonLHS.Expression) {
            if (!result.isObject) reportUnsupportedIfNeeded(expression, c)
            return
        }

        val type = (result as DoubleColonLHS.Type).type
        val reportError: Boolean
        if (result.possiblyBareType.isBare) {
            val descriptor = type.constructor.declarationDescriptor
            if (descriptor is ClassDescriptor && KotlinBuiltIns.isNonPrimitiveArray(descriptor)) {
                c.trace.report(ARRAY_CLASS_LITERAL_REQUIRES_ARGUMENT.on(expression))
            }
            reportError = false
        }
        else {
            reportError = !isAllowedInClassLiteral(type)
        }


        val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(type)
        if (type is SimpleType && !type.isMarkedNullable && typeParameterDescriptor != null && !typeParameterDescriptor.isReified) {
            c.trace.report(TYPE_PARAMETER_AS_REIFIED.on(expression, typeParameterDescriptor))
        }
        else if (type.isMarkedNullable || reportError) {
            c.trace.report(CLASS_LITERAL_LHS_NOT_A_CLASS.on(expression))
        }
    }

    // Returns true if the expression is not a call expression without value arguments (such as "A<B>") or a qualified expression
    // which contains such call expression as one of its parts.
    // In this case it's pointless to attempt to type check an expression on the LHS in "A<B>::class", since "A<B>" certainly means a type.
    private fun KtExpression.canBeConsideredProperExpression(): Boolean {
        return when (this) {
            is KtCallExpression ->
                !isWithoutValueArguments
            is KtDotQualifiedExpression ->
                receiverExpression.canBeConsideredProperExpression() &&
                selectorExpression?.canBeConsideredProperExpression() ?: false
            else -> true
        }
    }

    private fun KtExpression.canBeConsideredProperType(): Boolean {
        return when (this) {
            is KtSimpleNameExpression ->
                true
            is KtCallExpression ->
                isWithoutValueArguments
            is KtDotQualifiedExpression ->
                receiverExpression.canBeConsideredProperType() && selectorExpression.let { it != null && it.canBeConsideredProperType() }
            else -> false
        }
    }

    private fun shouldTryResolveLHSAsExpression(expression: KtDoubleColonExpression): Boolean {
        val lhs = expression.receiverExpression ?: return false
        return lhs.canBeConsideredProperExpression() && !expression.hasQuestionMarks /* TODO: test this */
    }

    private fun shouldTryResolveLHSAsType(expression: KtDoubleColonExpression): Boolean {
        val lhs = expression.receiverExpression
        return lhs != null && lhs.canBeConsideredProperType()
    }

    private fun reportUnsupportedIfNeeded(expression: KtDoubleColonExpression, c: ExpressionTypingContext) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.BoundCallableReferences)) {
            c.trace.report(UNSUPPORTED_FEATURE.on(expression.receiverExpression!!, LanguageFeature.BoundCallableReferences))
        }
    }

    private fun resolveDoubleColonLHS(doubleColonExpression: KtDoubleColonExpression, c: ExpressionTypingContext): DoubleColonLHS? {
        val resultForExpr = tryResolveLHS(doubleColonExpression, c, this::shouldTryResolveLHSAsExpression, this::resolveExpressionOnLHS)
        if (resultForExpr != null) {
            val lhs = resultForExpr.lhs as DoubleColonLHS.Expression?
            // If expression result is an object, we remember this and skip it here, because there are valid situations where
            // another type (representing another classifier) should win
            if (lhs != null && !lhs.isObject) {
                return resultForExpr.commit()
            }
        }

        val resultForType = tryResolveLHS(doubleColonExpression, c, this::shouldTryResolveLHSAsType) { expression, context ->
            resolveTypeOnLHS(expression, doubleColonExpression, context)
        }
        if (resultForType != null) {
            val lhs = resultForType.lhs
            if (resultForExpr != null && lhs != null && lhs.type == resultForExpr.lhs?.type) {
                // If we skipped an object expression result before and the type result is the same, this means that
                // there were no other classifier except that object that could win. We prefer to treat the LHS as an expression here,
                // to have a bound callable reference / class literal
                return resultForExpr.commit()
            }
            if (lhs != null) {
                return resultForType.commit()
            }
        }

        // If the LHS could be resolved neither as an expression nor as a type, we should still type-check it to allow all diagnostics
        // to be reported and references to be resolved. For that, we commit one of the applicable traces here, preferring the expression
        if (resultForExpr != null) return resultForExpr.commit()
        if (resultForType != null) return resultForType.commit()

        return null
    }

    private class LHSResolutionResult(
            val lhs: DoubleColonLHS?,
            val expression: KtExpression,
            val traceAndCache: TemporaryTraceAndCache
    ) {
        fun commit(): DoubleColonLHS? {
            if (lhs != null) {
                traceAndCache.trace.record(BindingContext.DOUBLE_COLON_LHS, expression, lhs)
            }
            traceAndCache.commit()
            return lhs
        }
    }

    /**
     * Returns null if the LHS is definitely not an expression. Returns a non-null result if a resolution was attempted and led to
     * either a successful result or not.
     */
    private fun tryResolveLHS(
            doubleColonExpression: KtDoubleColonExpression,
            context: ExpressionTypingContext,
            criterion: (KtDoubleColonExpression) -> Boolean,
            resolve: (KtExpression, ExpressionTypingContext) -> DoubleColonLHS?
    ): LHSResolutionResult? {
        val expression = doubleColonExpression.receiverExpression ?: return null

        if (!criterion(doubleColonExpression)) return null

        val traceAndCache = TemporaryTraceAndCache.create(context, "resolve '::' LHS", doubleColonExpression)
        val c = context
                .replaceTraceAndCache(traceAndCache)
                .replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(ContextDependency.INDEPENDENT)

        val lhs = resolve(expression, c)
        return LHSResolutionResult(lhs, expression, traceAndCache)
    }

    private fun resolveExpressionOnLHS(expression: KtExpression, c: ExpressionTypingContext): DoubleColonLHS.Expression? {
        val typeInfo = expressionTypingServices.getTypeInfo(expression, c)

        // TODO: do not lose data flow info maybe
        if (typeInfo.type == null) return null

        // Be careful not to call a utility function to get a resolved call by an expression which may accidentally
        // deparenthesize that expression, as this is undesirable here
        val call = c.trace.bindingContext[BindingContext.CALL, expression.getQualifiedElementSelector()]
        val resolvedCall = call.getResolvedCall(c.trace.bindingContext)

        if (resolvedCall != null) {
            val resultingDescriptor = resolvedCall.resultingDescriptor
            if (resultingDescriptor is FakeCallableDescriptorForObject) {
                val classDescriptor = resultingDescriptor.classDescriptor
                if (classDescriptor.companionObjectDescriptor != null) return null

                if (DescriptorUtils.isObject(classDescriptor)) {
                    return DoubleColonLHS.Expression(typeInfo, isObject = true)
                }
            }

            // Check if this is resolved to a function (with the error "arguments expected"), such as in "Runnable::class"
            if (expression.canBeConsideredProperType() && resultingDescriptor !is VariableDescriptor) return null
        }

        return DoubleColonLHS.Expression(typeInfo, isObject = false)
    }

    private fun resolveTypeOnLHS(
            expression: KtExpression, doubleColonExpression: KtDoubleColonExpression, c: ExpressionTypingContext
    ): DoubleColonLHS.Type? {
        val qualifierResolutionResult =
                qualifiedExpressionResolver.resolveDescriptorForDoubleColonLHS(expression, c.scope, c.trace, c.isDebuggerContext)

        val typeResolutionContext = TypeResolutionContext(
                c.scope, c.trace, /* checkBounds = */ true, /* allowBareTypes = */ true,
                /* isDebuggerContext = */ expression.suppressDiagnosticsInDebugMode() /* TODO: test this */
        )

        val classifier = qualifierResolutionResult.classifierDescriptor
        if (classifier == null) {
            typeResolver.resolveTypeProjections(
                    typeResolutionContext, ErrorUtils.createErrorType("No type").constructor, qualifierResolutionResult.allProjections
            )
            return null
        }

        val possiblyBareType = typeResolver.resolveTypeForClassifier(
                typeResolutionContext, classifier, qualifierResolutionResult, expression, Annotations.EMPTY
        )

        val type = if (possiblyBareType.isBare) {
            val descriptor = possiblyBareType.bareTypeConstructor.declarationDescriptor as? ClassDescriptor
                             ?: error("Only classes can produce bare types: $possiblyBareType")

            if (doubleColonExpression is KtCallableReferenceExpression) {
                c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(expression, descriptor.typeConstructor.parameters.size, descriptor))
            }

            val arguments = descriptor.typeConstructor.parameters.map(TypeUtils::makeStarProjection)
            KotlinTypeFactory.simpleType(
                    Annotations.EMPTY, descriptor.typeConstructor, arguments,
                    possiblyBareType.isNullable || doubleColonExpression.hasQuestionMarks
            )
        }
        else {
            TypeUtils.makeNullableAsSpecified(possiblyBareType.actualType, doubleColonExpression.hasQuestionMarks)
        }

        return DoubleColonLHS.Type(type, possiblyBareType)
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
        val callableReference = expression.callableReference
        if (callableReference.getReferencedName().isEmpty()) {
            if (!expression.isEmptyLHS) resolveDoubleColonLHS(expression, c)
            c.trace.report(UNRESOLVED_REFERENCE.on(callableReference, callableReference))
            val errorType = ErrorUtils.createErrorType("Empty callable reference")
            return dataFlowAnalyzer.createCheckedTypeInfo(errorType, c, expression)
        }

        val (lhs, resolutionResults) = resolveCallableReference(expression, c, ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS)
        val result = getCallableReferenceType(expression, lhs, resolutionResults, c)
        val dataFlowInfo = (lhs as? DoubleColonLHS.Expression)?.dataFlowInfo ?: c.dataFlowInfo
        return dataFlowAnalyzer.checkType(createTypeInfo(result, dataFlowInfo), expression, c)
    }

    private fun getCallableReferenceType(
            expression: KtCallableReferenceExpression,
            lhs: DoubleColonLHS?,
            resolutionResults: OverloadResolutionResults<*>?,
            context: ExpressionTypingContext
    ): KotlinType? {
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

        val type = createKCallableTypeForReference(descriptor, lhs, reflectionTypes, context.scope.ownerDescriptor) ?: return null

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
        if (descriptor is ConstructorDescriptor && DescriptorUtils.isAnnotationClass(descriptor.containingDeclaration)) {
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
                expression.toSourceElement(),
                /* isCoroutine = */ false
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
                /* isDelegated = */ false, expression.toSourceElement()
        )

        context.trace.record(BindingContext.VARIABLE, expression, localVariable)
    }

    fun resolveCallableReference(
            expression: KtCallableReferenceExpression,
            context: ExpressionTypingContext,
            resolveArgumentsMode: ResolveArgumentsMode
    ): Pair<DoubleColonLHS?, OverloadResolutionResults<*>?> {
        val lhsResult =
                if (expression.isEmptyLHS) null
                else resolveDoubleColonLHS(expression, context)

        if (lhsResult is DoubleColonLHS.Expression) {
            reportUnsupportedIfNeeded(expression, context)
        }

        val resolutionResults =
                resolveCallableReferenceRHS(expression, lhsResult, context, resolveArgumentsMode)

        return lhsResult to resolutionResults
    }

    private fun tryResolveRHSWithReceiver(
            traceTitle: String,
            receiver: Receiver?,
            reference: KtSimpleNameExpression,
            outerContext: ResolutionContext<*>,
            resolutionMode: ResolveArgumentsMode
    ): OverloadResolutionResults<CallableDescriptor>? {
        val call = CallMaker.makeCall(reference, receiver, null, reference, emptyList())
        val temporaryTrace = TemporaryTraceAndCache.create(outerContext, traceTitle, reference)
        val newContext =
                if (resolutionMode == ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS)
                    outerContext.replaceTraceAndCache(temporaryTrace).replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
                else
                    outerContext.replaceTraceAndCache(temporaryTrace)

        val resolutionResults = callResolver.resolveCallForMember(
                reference, BasicCallResolutionContext.create(newContext, call, CheckArgumentTypesMode.CHECK_CALLABLE_TYPE)
        )

        val shouldCommitTrace =
                if (resolutionMode == ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS) resolutionResults.isSingleResult
                else !resolutionResults.isNothing
        if (shouldCommitTrace) temporaryTrace.commit()

        return if (resolutionResults.isNothing) null else resolutionResults
    }

    private fun resolveCallableReferenceRHS(
            expression: KtCallableReferenceExpression,
            lhs: DoubleColonLHS?,
            c: ResolutionContext<*>,
            mode: ResolveArgumentsMode
    ): OverloadResolutionResults<CallableDescriptor>? {
        val reference = expression.callableReference

        val lhsType = lhs?.type ?:
                      return tryResolveRHSWithReceiver("resolve callable reference with empty LHS", null, reference, c, mode)

        when (lhs) {
            is DoubleColonLHS.Type -> {
                val classifier = lhsType.constructor.declarationDescriptor
                if (classifier !is ClassDescriptor) {
                    c.trace.report(CALLABLE_REFERENCE_LHS_NOT_A_CLASS.on(expression))
                    return null
                }

                val qualifier = c.trace.get(BindingContext.QUALIFIER, expression.receiverExpression!!)
                if (qualifier is ClassQualifier) {
                    val possibleStatic = tryResolveRHSWithReceiver(
                            "resolve unbound callable reference in static scope", qualifier, reference, c, mode
                    )
                    if (possibleStatic != null) return possibleStatic
                }

                val possibleWithReceiver = tryResolveRHSWithReceiver(
                        "resolve unbound callable reference with receiver", TransientReceiver(lhsType), reference, c, mode
                )
                if (possibleWithReceiver != null) return possibleWithReceiver
            }
            is DoubleColonLHS.Expression -> {
                val expressionReceiver = ExpressionReceiver.create(expression.receiverExpression!!, lhsType, c.trace.bindingContext)
                val result = tryResolveRHSWithReceiver(
                        "resolve bound callable reference", expressionReceiver, reference, c, mode
                )
                if (result != null) return result
            }
        }

        return null
    }

    companion object {
        fun createKCallableTypeForReference(
                descriptor: CallableDescriptor,
                lhs: DoubleColonLHS?,
                reflectionTypes: ReflectionTypes,
                scopeOwnerDescriptor: DeclarationDescriptor
        ): KotlinType? {
            val receiverType =
                    if (descriptor.extensionReceiverParameter != null || descriptor.dispatchReceiverParameter != null)
                        (lhs as? DoubleColonLHS.Type)?.type
                    else null

            return when (descriptor) {
                is FunctionDescriptor -> {
                    val returnType = descriptor.returnType ?: return null
                    val parametersTypes = descriptor.valueParameters.map { it.type }
                    val parametersNames = descriptor.valueParameters.map { it.name }
                    return reflectionTypes.getKFunctionType(Annotations.EMPTY, receiverType,
                                                            parametersTypes, parametersNames, returnType, descriptor.builtIns)
                }
                is PropertyDescriptor -> {
                    val mutable = descriptor.isVar && run {
                        val setter = descriptor.setter
                        setter == null || Visibilities.isVisible(receiverType?.let(::TransientReceiver), setter, scopeOwnerDescriptor)
                    }
                    reflectionTypes.getKPropertyType(Annotations.EMPTY, receiverType, descriptor.type, mutable)
                }
                is VariableDescriptor -> null
                else -> throw UnsupportedOperationException("Callable reference resolved to an unsupported descriptor: $descriptor")
            }
        }
    }
}
