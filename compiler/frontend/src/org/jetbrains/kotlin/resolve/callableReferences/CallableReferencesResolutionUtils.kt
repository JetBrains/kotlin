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

package org.jetbrains.kotlin.resolve.callableReferences

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.CheckArgumentTypesMode
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.scopes.BaseLexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.ScopeUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.types.FunctionPlaceholders
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.ThrowingList

fun resolveCallableReferenceReceiverType(
        callableReferenceExpression: KtCallableReferenceExpression,
        context: ResolutionContext<*>,
        typeResolver: TypeResolver
): KotlinType? =
        callableReferenceExpression.typeReference?.let {
            typeResolver.resolveType(context.scope, it, context.trace, false)
        }

private fun <D : CallableDescriptor> ResolveArgumentsMode.acceptResolution(results: OverloadResolutionResults<D>, trace: TemporaryTraceAndCache) {
    when (this) {
        ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS ->
            if (results.isSingleResult) trace.commit()
        ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS ->
            if (results.isSomething()) trace.commit()
    }
}

private fun resolvePossiblyAmbiguousCallableReference(
        reference: KtSimpleNameExpression,
        receiver: ReceiverValue?,
        context: ResolutionContext<*>,
        resolutionMode: ResolveArgumentsMode,
        callResolver: CallResolver
): OverloadResolutionResults<CallableDescriptor> {
    val call = CallMaker.makeCall(reference, receiver, null, reference, ThrowingList.instance<ValueArgument>())
    val temporaryTrace = TemporaryTraceAndCache.create(context, "trace to resolve ::${reference.getReferencedName()} as function", reference)
    val newContext = if (resolutionMode == ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS)
        context.replaceTraceAndCache(temporaryTrace).replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
    else
        context.replaceTraceAndCache(temporaryTrace)
    val callResolutionContext = BasicCallResolutionContext.create(
            newContext, call, CheckArgumentTypesMode.CHECK_CALLABLE_TYPE)
    val resolutionResults = callResolver.resolveCallForMember(reference, callResolutionContext)
    resolutionMode.acceptResolution(resolutionResults, temporaryTrace)
    return resolutionResults
}

private fun OverloadResolutionResults<*>.isSomething(): Boolean = !isNothing

fun resolvePossiblyAmbiguousCallableReference(
        callableReferenceExpression: KtCallableReferenceExpression,
        lhsType: KotlinType?,
        context: ResolutionContext<*>,
        resolutionMode: ResolveArgumentsMode,
        callResolver: CallResolver
): OverloadResolutionResults<CallableDescriptor>? {
    val reference = callableReferenceExpression.getCallableReference()

    fun resolveInScope(traceTitle: String, classifier: ClassifierDescriptor, staticScope: MemberScope): OverloadResolutionResults<CallableDescriptor> {

        // todo: drop this class when new resolve will be finished
        class StaticScopeAsLexicalScope(val staticScope: MemberScope) : BaseLexicalScope(staticScope.memberScopeAsImportingScope(), classifier) {
            override val kind: LexicalScopeKind
                get() = LexicalScopeKind.CALLABLE_REFERENCE

            override fun printStructure(p: Printer) {
                p.println(toString())
            }

            override fun toString(): String = "${javaClass.canonicalName} for $staticScope"

            // this method is needed for correct rewrite LEXICAL_SCOPE in trace
            override fun equals(other: Any?) = other is StaticScopeAsLexicalScope && other.staticScope == staticScope
            override fun hashCode() = staticScope.hashCode()
        }

        val temporaryTraceAndCache = TemporaryTraceAndCache.create(context, traceTitle, reference)
        val newContext = context.replaceTraceAndCache(temporaryTraceAndCache).replaceScope(StaticScopeAsLexicalScope(staticScope))
        val results = resolvePossiblyAmbiguousCallableReference(reference, null, newContext, resolutionMode, callResolver)
        resolutionMode.acceptResolution(results, temporaryTraceAndCache)
        return results
    }

    fun resolveWithReceiver(traceTitle: String, receiver: ReceiverValue): OverloadResolutionResults<CallableDescriptor> {
        val temporaryTraceAndCache = TemporaryTraceAndCache.create(context, traceTitle, reference)
        val newContext = context.replaceTraceAndCache(temporaryTraceAndCache)
        val results = resolvePossiblyAmbiguousCallableReference(reference, receiver, newContext, resolutionMode, callResolver)
        resolutionMode.acceptResolution(results, temporaryTraceAndCache)
        return results
    }

    if (lhsType == null) {
        return resolvePossiblyAmbiguousCallableReference(reference, null, context, resolutionMode, callResolver)
    }

    val classifier = lhsType.constructor.declarationDescriptor
    if (classifier !is ClassDescriptor) {
        context.trace.report(CALLABLE_REFERENCE_LHS_NOT_A_CLASS.on(callableReferenceExpression))
        return null
    }

    val possibleStatic = resolveInScope("trace to resolve ::${reference.getReferencedName()} in static scope", classifier, classifier.staticScope)
    if (possibleStatic.isSomething()) return possibleStatic

    val possibleNested = resolveInScope("trace to resolve ::${reference.getReferencedName()} in static nested classes scope",
                                        classifier, ScopeUtils.getStaticNestedClassesScope(classifier))
    if (possibleNested.isSomething()) return possibleNested

    val possibleWithReceiver = resolveWithReceiver("trace to resolve ::${reference.getReferencedName()} with receiver",
                                                   TransientReceiver(lhsType))
    if (possibleWithReceiver.isSomething()) return possibleWithReceiver

    return null
}

fun resolveCallableReferenceTarget(
        callableReferenceExpression: KtCallableReferenceExpression,
        lhsType: KotlinType?,
        context: ResolutionContext<*>,
        resolvedToSomething: BooleanArray,
        callResolver: CallResolver
): CallableDescriptor? {
    val resolutionResults = resolvePossiblyAmbiguousCallableReference(
            callableReferenceExpression, lhsType, context, ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS, callResolver)
    return resolutionResults?.let { results ->
        if (results.isSomething()) {
            resolvedToSomething[0] = true
            OverloadResolutionResultsUtil.getResultingCall(results, context.contextDependency)?.let { call ->
                call.resultingDescriptor
            }
        }
        else {
            null
        }
    }
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
    val localVariable = LocalVariableDescriptor(context.scope.ownerDescriptor, Annotations.EMPTY, Name.special("<anonymous>"),
                                                referenceType, /* mutable = */ false, false, expression.toSourceElement())

    context.trace.record(BindingContext.VARIABLE, expression, localVariable)
}

private fun createReflectionTypeForCallableDescriptor(
        descriptor: CallableDescriptor,
        lhsType: KotlinType?,
        reflectionTypes: ReflectionTypes,
        trace: BindingTrace?,
        reportOn: KtExpression?,
        ignoreReceiver: Boolean
): KotlinType? {
    val extensionReceiver = descriptor.extensionReceiverParameter
    val dispatchReceiver = descriptor.dispatchReceiverParameter?.let { dispatchReceiver ->
        // See CallableDescriptor#getOwnerForEffectiveDispatchReceiverParameter
        if ((descriptor as? CallableMemberDescriptor)?.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
            DescriptorUtils.getDispatchReceiverParameterIfNeeded(descriptor.containingDeclaration)
        else dispatchReceiver
    }

    if (extensionReceiver != null && dispatchReceiver != null && descriptor is CallableMemberDescriptor) {
        if (reportOn != null) {
            trace?.report(EXTENSION_IN_CLASS_REFERENCE_NOT_ALLOWED.on(reportOn, descriptor))
        }
        return null
    }

    val receiverType =
            if ((extensionReceiver != null || dispatchReceiver != null) && !ignoreReceiver)
                lhsType ?: extensionReceiver?.type ?: dispatchReceiver?.type
            else null

    return when (descriptor) {
        is FunctionDescriptor -> {
            val returnType = descriptor.returnType ?: return null
            val valueParametersTypes = ExpressionTypingUtils.getValueParametersTypes(descriptor.valueParameters)
            return reflectionTypes.getKFunctionType(Annotations.EMPTY, receiverType, valueParametersTypes, returnType)
        }
        is PropertyDescriptor -> {
            reflectionTypes.getKPropertyType(Annotations.EMPTY, receiverType, descriptor.type, descriptor.isVar)
        }
        is VariableDescriptor -> {
            if (reportOn != null) {
                trace?.report(UNSUPPORTED.on(reportOn, "References to variables aren't supported yet"))
            }
            null
        }
        else ->
            throw UnsupportedOperationException("Callable reference resolved to an unsupported descriptor: $descriptor")
    }
}

fun getReflectionTypeForCandidateDescriptor(
        descriptor: CallableDescriptor,
        reflectionTypes: ReflectionTypes,
        ignoreReceiver: Boolean
): KotlinType? =
        createReflectionTypeForCallableDescriptor(descriptor, null, reflectionTypes, null, null, ignoreReceiver)

fun createReflectionTypeForResolvedCallableReference(
        reference: KtCallableReferenceExpression,
        lhsType: KotlinType?,
        descriptor: CallableDescriptor,
        context: ResolutionContext<*>,
        reflectionTypes: ReflectionTypes
): KotlinType? {
    val type = createReflectionTypeForCallableDescriptor(
            descriptor, lhsType, reflectionTypes, context.trace, reference.callableReference, reference.isEmptyLHS
    ) ?: return null
    when (descriptor) {
        is FunctionDescriptor -> {
            bindFunctionReference(reference, type, context)
        }
        is PropertyDescriptor -> {
            bindPropertyReference(reference, type, context)
        }
    }
    return type
}

fun getResolvedCallableReferenceShapeType(
        reference: KtCallableReferenceExpression,
        lhsType: KotlinType?,
        overloadResolutionResults: OverloadResolutionResults<CallableDescriptor>?,
        context: ResolutionContext<*>,
        expectedTypeUnknown: Boolean,
        reflectionTypes: ReflectionTypes,
        builtIns: KotlinBuiltIns,
        functionPlaceholders: FunctionPlaceholders
): KotlinType? =
        when {
            overloadResolutionResults == null ->
                null
            overloadResolutionResults.isSingleResult ->
                OverloadResolutionResultsUtil.getResultingCall(overloadResolutionResults, context.contextDependency)?.let { call ->
                    createReflectionTypeForCallableDescriptor(
                            call.resultingDescriptor, lhsType, reflectionTypes, context.trace, reference, reference.isEmptyLHS
                    )
                }
            expectedTypeUnknown /* && overload resolution was ambiguous */ ->
                functionPlaceholders.createFunctionPlaceholderType(emptyList(), false)
            else ->
                createFunctionType(builtIns, Annotations.EMPTY, null, emptyList(), TypeUtils.DONT_CARE)
        }
