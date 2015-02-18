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

package org.jetbrains.kotlin.resolve.calls.tasks

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import com.intellij.openapi.progress.ProgressIndicatorProvider
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext

import org.jetbrains.kotlin.resolve.calls.CallResolverUtil.isOrOverridesSynthesized
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.util.*
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.*

public class TaskPrioritizer(private val storageManager: StorageManager) {

    public fun <D : CallableDescriptor> splitLexicallyLocalDescriptors(
            allDescriptors: Collection<ResolutionCandidate<D>>,
            containerOfTheCurrentLocality: DeclarationDescriptor,
            local: MutableCollection<ResolutionCandidate<D>>,
            nonlocal: MutableCollection<ResolutionCandidate<D>>
    ) {
        for (resolvedCall in allDescriptors) {
            if (ExpressionTypingUtils.isLocal(containerOfTheCurrentLocality, resolvedCall.getDescriptor())) {
                local.add(resolvedCall)
            }
            else {
                nonlocal.add(resolvedCall)
            }
        }
    }

    public fun <D : CallableDescriptor, F : D> computePrioritizedTasks(
            context: BasicCallResolutionContext,
            name: Name,
            tracing: TracingStrategy,
            callableDescriptorCollectors: CallableDescriptorCollectors<D>
    ): List<ResolutionTask<D, F>> {
        val explicitReceiver = context.call.getExplicitReceiver()
        val result = ResolutionTaskHolder<D, F>(storageManager, context, PriorityProviderImpl<D>(context), tracing)
        val taskPrioritizerContext = TaskPrioritizerContext(name, result, context, context.scope, callableDescriptorCollectors)

        if (explicitReceiver is QualifierReceiver) {
            val qualifierReceiver = explicitReceiver : QualifierReceiver
            doComputeTasks(NO_RECEIVER, taskPrioritizerContext.replaceScope(qualifierReceiver.getNestedClassesAndPackageMembersScope()))
            computeTasksForClassObjectReceiver(qualifierReceiver, taskPrioritizerContext)
        }
        else {
            doComputeTasks(explicitReceiver, taskPrioritizerContext)
        }

        return result.getTasks()
    }

    private fun <D : CallableDescriptor, F : D> computeTasksForClassObjectReceiver(
            qualifierReceiver: QualifierReceiver,
            taskPrioritizerContext: TaskPrioritizerContext<D, F>
    ) {
        val classObjectReceiver = qualifierReceiver.getClassObjectReceiver()
        if (!classObjectReceiver.exists()) {
            return
        }
        val classifierDescriptor = qualifierReceiver.classifier
        doComputeTasks(classObjectReceiver, taskPrioritizerContext.filterCollectors {
            when {
                classifierDescriptor is ClassDescriptor && classifierDescriptor.getDefaultObjectDescriptor() != null -> {
                    // nested classes and objects should not be accessible via short reference to default object
                    it !is ConstructorDescriptor && it !is FakeCallableDescriptorForObject
                }
                classifierDescriptor != null && DescriptorUtils.isEnumEntry(classifierDescriptor) -> {
                    // objects nested in enum should not be accessible via enum entries reference
                    it !is FakeCallableDescriptorForObject
                }
                else -> true
            }
        })
    }

    private fun <D : CallableDescriptor, F : D> doComputeTasks(receiver: ReceiverValue, c: TaskPrioritizerContext<D, F>) {
        ProgressIndicatorProvider.checkCanceled()

        val resolveInvoke = c.context.call.getDispatchReceiver().exists()
        if (resolveInvoke) {
            addCandidatesForInvoke(receiver, c)
            return
        }
        val implicitReceivers = Sets.newLinkedHashSet<ReceiverValue>(JetScopeUtils.getImplicitReceiversHierarchyValues(c.scope))
        if (receiver.exists()) {
            addCandidatesForExplicitReceiver(receiver, implicitReceivers, c, isExplicit = true)
            addMembers(receiver, c, staticMembers = true, isExplicit = true)
            return
        }
        addCandidatesForNoReceiver(implicitReceivers, c)
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesForExplicitReceiver(
            explicitReceiver: ReceiverValue,
            implicitReceivers: Collection<ReceiverValue>,
            c: TaskPrioritizerContext<D, F>,
            isExplicit: Boolean
    ) {
        addMembers(explicitReceiver, c, staticMembers = false, isExplicit = isExplicit)

        if (explicitReceiver.getType().isDynamic()) {
            addCandidatesForDynamicReceiver(explicitReceiver, implicitReceivers, c, isExplicit)
        }
        else {
            addExtensionCandidates(explicitReceiver, implicitReceivers, c, isExplicit)
        }
    }

    private fun <D : CallableDescriptor, F : D> addExtensionCandidates(
            explicitReceiver: ReceiverValue,
            implicitReceivers: Collection<ReceiverValue>,
            c: TaskPrioritizerContext<D, F>,
            isExplicit: Boolean
    ) {
        for (callableDescriptorCollector in c.callableDescriptorCollectors) {
            //member extensions
            for (implicitReceiver in implicitReceivers) {
                addMemberExtensionCandidates(
                        implicitReceiver,
                        explicitReceiver,
                        callableDescriptorCollector,
                        c,
                        createKind(EXTENSION_RECEIVER, isExplicit)
                )
            }
            //extensions
            c.result.addCandidates {
                convertWithImpliedThis(
                        c.scope,
                        explicitReceiver,
                        callableDescriptorCollector.getExtensionsByName(c.scope, c.name, c.context.trace),
                        createKind(EXTENSION_RECEIVER, isExplicit),
                        c.context.call
                )
            }
        }
    }

    private fun <D : CallableDescriptor, F : D> addMembers(
            explicitReceiver: ReceiverValue,
            c: TaskPrioritizerContext<D, F>,
            staticMembers: Boolean,
            isExplicit: Boolean
    ) {
        for (callableDescriptorCollector in c.callableDescriptorCollectors) {
            c.result.addCandidates {
                val variantsForExplicitReceiver = SmartCastUtils.getSmartCastVariants(explicitReceiver, c.context as ResolutionContext<*>) //workaround KT-1969
                val members = Lists.newArrayList<ResolutionCandidate<D>>()
                for (type in variantsForExplicitReceiver) {
                    val membersForThisVariant = if (staticMembers) {
                        callableDescriptorCollector.getStaticMembersByName(type, c.name, c.context.trace)
                    }
                    else {
                        callableDescriptorCollector.getMembersByName(type, c.name, c.context.trace)
                    }
                    convertWithReceivers(
                            membersForThisVariant,
                            explicitReceiver,
                            NO_RECEIVER,
                            members,
                            createKind(DISPATCH_RECEIVER, isExplicit),
                            c.context.call
                    )
                }
                members
            }
        }
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesForDynamicReceiver(
            explicitReceiver: ReceiverValue,
            implicitReceivers: Collection<ReceiverValue>,
            c: TaskPrioritizerContext<D, F>,
            isExplicit: Boolean
    ) {
        val onlyDynamicReceivers = c.replaceCollectors(c.callableDescriptorCollectors.onlyDynamicReceivers<D>())
        addExtensionCandidates(explicitReceiver, implicitReceivers, onlyDynamicReceivers, isExplicit)

        c.result.addCandidates {
            val dynamicScope = DynamicCallableDescriptors.createDynamicDescriptorScope(c.context.call, c.scope.getContainingDeclaration())

            val dynamicDescriptors = c.callableDescriptorCollectors.flatMap {
                it.getNonExtensionsByName(dynamicScope, c.name, c.context.trace)
            }

            convertWithReceivers(dynamicDescriptors, explicitReceiver, NO_RECEIVER, createKind(DISPATCH_RECEIVER, isExplicit), c.context.call)
        }
    }

    private fun createKind(kind: ExplicitReceiverKind, isExplicit: Boolean): ExplicitReceiverKind {
        if (isExplicit) return kind
        return ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
    }

    private fun <D : CallableDescriptor, F : D> addMemberExtensionCandidates(
            dispatchReceiver: ReceiverValue,
            receiverParameter: ReceiverValue,
            callableDescriptorCollector: CallableDescriptorCollector<D>,
            c: TaskPrioritizerContext<D, F>,
            receiverKind: ExplicitReceiverKind
    ) {
        c.result.addCandidates {
            val memberExtensions =
                    callableDescriptorCollector.getExtensionsByName(dispatchReceiver.getType().getMemberScope(), c.name, c.context.trace)
            convertWithReceivers(memberExtensions, dispatchReceiver, receiverParameter, receiverKind, c.context.call)
        }
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesForNoReceiver(
            implicitReceivers: Collection<ReceiverValue>,
            c: TaskPrioritizerContext<D, F>
    ) {
        val localsList = Lists.newArrayList<Collection<ResolutionCandidate<D>>>()
        val nonlocalsList = Lists.newArrayList<Collection<ResolutionCandidate<D>>>()
        for (callableDescriptorCollector in c.callableDescriptorCollectors) {

            val members = convertWithImpliedThisAndNoReceiver(
                    c.scope,
                    callableDescriptorCollector.getNonExtensionsByName(c.scope, c.name, c.context.trace),
                    c.context.call
            )

            val nonlocals = Lists.newArrayList<ResolutionCandidate<D>>()
            val locals = Lists.newArrayList<ResolutionCandidate<D>>()
            //noinspection unchecked,RedundantTypeArguments
            splitLexicallyLocalDescriptors(members, c.scope.getContainingDeclaration(), locals, nonlocals)

            localsList.add(locals)
            nonlocalsList.add(nonlocals)
        }

        //locals
        c.result.addCandidates(localsList)

        //try all implicit receivers as explicit
        for (implicitReceiver in implicitReceivers) {
            addCandidatesForExplicitReceiver(implicitReceiver, implicitReceivers, c, isExplicit = false)
        }

        //nonlocals
        c.result.addCandidates(nonlocalsList)

        //static (only for better error reporting)
        for (implicitReceiver in implicitReceivers) {
            addMembers(implicitReceiver, c, staticMembers = true, isExplicit = false)
        }
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesForInvoke(explicitReceiver: ReceiverValue, c: TaskPrioritizerContext<D, F>) {
        val implicitReceivers = JetScopeUtils.getImplicitReceiversHierarchyValues(c.scope)

        // For 'a.foo()' where foo has function type,
        // a is explicitReceiver, foo is variableReceiver.
        val variableReceiver = c.context.call.getDispatchReceiver()
        assert(variableReceiver.exists(), "'Invoke' call hasn't got variable receiver")

        // For invocation a.foo() explicit receiver 'a'
        // can be a receiver for 'foo' variable
        // or for 'invoke' function.

        // (1) a.foo + foo.invoke()
        if (!explicitReceiver.exists()) {
            addCandidatesForExplicitReceiver(variableReceiver, implicitReceivers, c, isExplicit = true)
        }

        // (2) foo + a.invoke()

        // 'invoke' is member extension to explicit receiver while variable receiver is 'this object'
        //trait A
        //trait Foo { fun A.invoke() }

        if (explicitReceiver.exists()) {
            //a.foo()
            addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(variableReceiver, explicitReceiver, c, BOTH_RECEIVERS)
            return
        }
        // with (a) { foo() }
        for (implicitReceiver in implicitReceivers) {
            addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(variableReceiver, implicitReceiver, c, DISPATCH_RECEIVER)
        }
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(
            dispatchReceiver: ReceiverValue,
            receiverParameter: ReceiverValue,
            c: TaskPrioritizerContext<D, F>,
            receiverKind: ExplicitReceiverKind
    ) {
        for (callableDescriptorCollector in c.callableDescriptorCollectors) {
            addMemberExtensionCandidates(dispatchReceiver, receiverParameter, callableDescriptorCollector, c, receiverKind)
        }
    }

    private fun <D : CallableDescriptor> convertWithReceivers(
            descriptors: Collection<D>,
            dispatchReceiver: ReceiverValue,
            extensionReceiver: ReceiverValue,
            explicitReceiverKind: ExplicitReceiverKind,
            call: Call
    ): Collection<ResolutionCandidate<D>> {
        val result = Lists.newArrayList<ResolutionCandidate<D>>()
        convertWithReceivers(descriptors, dispatchReceiver, extensionReceiver, result, explicitReceiverKind, call)
        return result
    }

    private fun <D : CallableDescriptor> convertWithReceivers(
            descriptors: Collection<D>,
            dispatchReceiver: ReceiverValue,
            extensionReceiver: ReceiverValue,
            result: MutableCollection<ResolutionCandidate<D>>,
            explicitReceiverKind: ExplicitReceiverKind,
            call: Call
    ) {
        for (descriptor in descriptors) {
            val candidate = ResolutionCandidate.create<D>(call, descriptor)
            candidate.setDispatchReceiver(dispatchReceiver)
            candidate.setExtensionReceiver(extensionReceiver)
            candidate.setExplicitReceiverKind(explicitReceiverKind)
            result.add(candidate)
        }
    }

    public fun <D : CallableDescriptor> convertWithImpliedThisAndNoReceiver(
            scope: JetScope,
            descriptors: Collection<D>,
            call: Call
    ): Collection<ResolutionCandidate<D>> {
        return convertWithImpliedThis(scope, NO_RECEIVER, descriptors, NO_EXPLICIT_RECEIVER, call)
    }

    public fun <D : CallableDescriptor> convertWithImpliedThis(
            scope: JetScope,
            receiverParameter: ReceiverValue,
            descriptors: Collection<D>,
            receiverKind: ExplicitReceiverKind,
            call: Call
    ): Collection<ResolutionCandidate<D>> {
        val result = Lists.newArrayList<ResolutionCandidate<D>>()
        for (descriptor in descriptors) {
            val candidate = ResolutionCandidate.create<D>(call, descriptor)
            candidate.setExtensionReceiver(receiverParameter)
            candidate.setExplicitReceiverKind(receiverKind)
            if (setImpliedThis(scope, candidate)) {
                result.add(candidate)
            }
        }
        return result
    }

    private fun <D : CallableDescriptor> setImpliedThis(
            scope: JetScope,
            candidate: ResolutionCandidate<D>
    ): Boolean {
        val dispatchReceiver = candidate.getDescriptor().getDispatchReceiverParameter()
        if (dispatchReceiver == null) return true
        val receivers = scope.getImplicitReceiversHierarchy()
        for (receiver in receivers) {
            if (JetTypeChecker.DEFAULT.isSubtypeOf(receiver.getType(), dispatchReceiver.getType())) {
                // TODO : Smartcasts & nullability
                candidate.setDispatchReceiver(dispatchReceiver.getValue())
                return true
            }
        }
        return false
    }

    public fun <D : CallableDescriptor, F : D> computePrioritizedTasksFromCandidates(
            context: BasicCallResolutionContext,
            candidates: Collection<ResolutionCandidate<D>>,
            tracing: TracingStrategy
    ): List<ResolutionTask<D, F>> {
        val result = ResolutionTaskHolder<D, F>(storageManager, context, PriorityProviderImpl<D>(context), tracing)
        result.addCandidates {
            candidates
        }
        return result.getTasks()
    }

    private class PriorityProviderImpl<D : CallableDescriptor>(private val context: BasicCallResolutionContext) :
            ResolutionTaskHolder.PriorityProvider<ResolutionCandidate<D>> {

        override fun getPriority(candidate: ResolutionCandidate<D>)
                = if (hasImplicitDynamicReceiver(candidate)) 0
                  else (if (isVisible(candidate)) 2 else 0) + (if (isSynthesized(candidate)) 0 else 1)

        override fun getMaxPriority() = 3

        private fun isVisible(candidate: ResolutionCandidate<D>?): Boolean {
            if (candidate == null) return false
            val candidateDescriptor = candidate.getDescriptor()
            if (ErrorUtils.isError(candidateDescriptor)) return true
            val receiverValue = ExpressionTypingUtils.normalizeReceiverValueForVisibility(candidate.getDispatchReceiver(), context.trace.getBindingContext())
            return Visibilities.isVisible(receiverValue, candidateDescriptor, context.scope.getContainingDeclaration())
        }

        private fun isSynthesized(candidate: ResolutionCandidate<D>): Boolean {
            val descriptor = candidate.getDescriptor()
            return descriptor is CallableMemberDescriptor && isOrOverridesSynthesized(descriptor : CallableMemberDescriptor)
        }

        fun hasImplicitDynamicReceiver(candidate: ResolutionCandidate<D>): Boolean {
            return (!candidate.getExplicitReceiverKind().isDispatchReceiver() || !candidate.getCall().getExplicitReceiver().exists())
                   && candidate.getDescriptor().isDynamic()
        }
    }

    private class TaskPrioritizerContext<D : CallableDescriptor, F : D>(
            val name: Name,
            val result: ResolutionTaskHolder<D, F>,
            val context: BasicCallResolutionContext,
            val scope: JetScope,
            val callableDescriptorCollectors: CallableDescriptorCollectors<D>
    ) {
        fun replaceScope(newScope: JetScope): TaskPrioritizerContext<D, F> {
            return TaskPrioritizerContext(name, result, context, newScope, callableDescriptorCollectors)
        }

        fun replaceCollectors(newCollectors: CallableDescriptorCollectors<D>): TaskPrioritizerContext<D, F> {
            return TaskPrioritizerContext(name, result, context, scope, newCollectors)
        }

        fun filterCollectors(filter: (D) -> Boolean): TaskPrioritizerContext<D, F> {
            return TaskPrioritizerContext(name, result, context, scope, callableDescriptorCollectors.filtered(filter))
        }
    }
}
