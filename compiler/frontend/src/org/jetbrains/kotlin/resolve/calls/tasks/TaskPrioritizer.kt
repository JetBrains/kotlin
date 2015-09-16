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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.doNotAnalyze
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isOrOverridesSynthesized
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.BOTH_RECEIVERS
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.DISPATCH_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.EXTENSION_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.CallableDescriptorCollector
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.CallableDescriptorCollectors
import org.jetbrains.kotlin.resolve.calls.tasks.collectors.filtered
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER
import org.jetbrains.kotlin.resolve.scopes.utils.asJetScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.isDynamic

public class TaskPrioritizer(
        private val storageManager: StorageManager,
        private val smartCastManager: SmartCastManager
) {

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
        val taskPrioritizerContext = TaskPrioritizerContext(name, result, context, context.scope.asJetScope(), callableDescriptorCollectors)

        if (explicitReceiver is QualifierReceiver) {
            val qualifierReceiver: QualifierReceiver = explicitReceiver
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
                classifierDescriptor is ClassDescriptor && classifierDescriptor.getCompanionObjectDescriptor() != null -> {
                    // nested classes and objects should not be accessible via short reference to companion object
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
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val receiverWithTypes = ReceiverWithTypes(receiver, c.context)

        val resolveInvoke = c.context.call.getDispatchReceiver().exists()
        if (resolveInvoke) {
            addCandidatesForInvoke(receiverWithTypes, c)
            return
        }
        val implicitReceivers = Sets.newLinkedHashSet<ReceiverValue>(JetScopeUtils.getImplicitReceiversHierarchyValues(c.scope))
        if (receiver.exists()) {
            addCandidatesForExplicitReceiver(receiverWithTypes, implicitReceivers, c, isExplicit = true)
            addMembers(receiverWithTypes, c, staticMembers = true, isExplicit = true)
            return
        }
        addCandidatesForNoReceiver(implicitReceivers, c)
    }

    private inner class ReceiverWithTypes(
            val value: ReceiverValue,
            private val context: ResolutionContext<*>
    ) {
        val types: Collection<JetType> by lazy { smartCastManager.getSmartCastVariants(value, context) }
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesForExplicitReceiver(
            explicitReceiver: ReceiverWithTypes,
            implicitReceivers: Collection<ReceiverValue>,
            c: TaskPrioritizerContext<D, F>,
            isExplicit: Boolean
    ) {
        addMembers(explicitReceiver, c, staticMembers = false, isExplicit = isExplicit)

        if (explicitReceiver.value.type.isDynamic()) {
            addCandidatesForDynamicReceiver(explicitReceiver, implicitReceivers, c, isExplicit)
        }
        else {
            addExtensionCandidates(explicitReceiver, implicitReceivers, c, isExplicit)
        }
    }

    private fun <D : CallableDescriptor, F : D> addExtensionCandidates(
            explicitReceiver: ReceiverWithTypes,
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
                        explicitReceiver.value,
                        callableDescriptorCollector.getExtensionsByName(c.scope, c.name, explicitReceiver.types, createLookupLocation(c), c.context.trace),
                        createKind(EXTENSION_RECEIVER, isExplicit),
                        c.context.call
                )
            }
        }
    }

    private fun <D : CallableDescriptor, F : D> addMembers(
            explicitReceiver: ReceiverWithTypes,
            c: TaskPrioritizerContext<D, F>,
            staticMembers: Boolean,
            isExplicit: Boolean
    ) {
        for (callableDescriptorCollector in c.callableDescriptorCollectors) {
            c.result.addCandidates {
                val members = Lists.newArrayList<ResolutionCandidate<D>>()
                for (type in explicitReceiver.types) {
                    val membersForThisVariant = if (staticMembers) {
                        callableDescriptorCollector.getStaticMembersByName(type, c.name, createLookupLocation(c), c.context.trace)
                    }
                    else {
                        callableDescriptorCollector.getMembersByName(type, c.name, createLookupLocation(c), c.context.trace)
                    }
                    convertWithReceivers(
                            membersForThisVariant,
                            explicitReceiver.value,
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
            explicitReceiver: ReceiverWithTypes,
            implicitReceivers: Collection<ReceiverValue>,
            c: TaskPrioritizerContext<D, F>,
            isExplicit: Boolean
    ) {
        val onlyDynamicReceivers = c.replaceCollectors(c.callableDescriptorCollectors.onlyDynamicReceivers<D>())
        addExtensionCandidates(explicitReceiver, implicitReceivers, onlyDynamicReceivers, isExplicit)

        c.result.addCandidates {
            val dynamicScope = DynamicCallableDescriptors.createDynamicDescriptorScope(c.context.call, c.scope.getContainingDeclaration())

            val dynamicDescriptors = c.callableDescriptorCollectors.flatMap {
                it.getNonExtensionsByName(dynamicScope, c.name, createLookupLocation(c), c.context.trace)
            }

            convertWithReceivers(dynamicDescriptors, explicitReceiver.value, NO_RECEIVER, createKind(DISPATCH_RECEIVER, isExplicit), c.context.call)
        }
    }

    private fun createKind(kind: ExplicitReceiverKind, isExplicit: Boolean): ExplicitReceiverKind {
        if (isExplicit) return kind
        return ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
    }

    private fun <D : CallableDescriptor, F : D> addMemberExtensionCandidates(
            dispatchReceiver: ReceiverValue,
            receiverParameter: ReceiverWithTypes,
            callableDescriptorCollector: CallableDescriptorCollector<D>,
            c: TaskPrioritizerContext<D, F>,
            receiverKind: ExplicitReceiverKind
    ) {
        c.result.addCandidates {
            val memberExtensions =
                    callableDescriptorCollector.getExtensionsByName(dispatchReceiver.type.memberScope, c.name, receiverParameter.types, createLookupLocation(c), c.context.trace)
            convertWithReceivers(memberExtensions, dispatchReceiver, receiverParameter.value, receiverKind, c.context.call)
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
                    callableDescriptorCollector.getNonExtensionsByName(c.scope, c.name, createLookupLocation(c), c.context.trace),
                    c.context.call
            )

            if (members.isNotEmpty()) {
                val nonlocals = Lists.newArrayList<ResolutionCandidate<D>>()
                val locals = Lists.newArrayList<ResolutionCandidate<D>>()
                splitLexicallyLocalDescriptors(members, c.scope.getContainingDeclaration(), locals, nonlocals)

                localsList.add(locals)
                nonlocalsList.add(nonlocals)
            }
        }

        //locals
        c.result.addCandidates(localsList)

        val implicitReceiversWithTypes = implicitReceivers.map { ReceiverWithTypes(it, c.context) }

        //try all implicit receivers as explicit
        for (implicitReceiver in implicitReceiversWithTypes) {
            addCandidatesForExplicitReceiver(implicitReceiver, implicitReceivers, c, isExplicit = false)
        }

        //nonlocals
        c.result.addCandidates(nonlocalsList)

        //static (only for better error reporting)
        for (implicitReceiver in implicitReceiversWithTypes) {
            addMembers(implicitReceiver, c, staticMembers = true, isExplicit = false)
        }
    }

    private fun createLookupLocation(c: TaskPrioritizerContext<*, *>) = KotlinLookupLocation(c.context.call.run {
        calleeExpression?.let {
            // Can't use getContainingJetFile() because we can get from IDE an element with JavaDummyHolder as containing file
            if ((it.containingFile as? JetFile)?.doNotAnalyze == null) it else null
        }
        ?: callElement
    })

    private fun <D : CallableDescriptor, F : D> addCandidatesForInvoke(explicitReceiver: ReceiverWithTypes, c: TaskPrioritizerContext<D, F>) {
        val implicitReceivers = JetScopeUtils.getImplicitReceiversHierarchyValues(c.scope)

        // For 'a.foo()' where foo has function type,
        // a is explicitReceiver, foo is variableReceiver.
        val variableReceiver = c.context.call.getDispatchReceiver()
        assert(variableReceiver.exists(), "'Invoke' call hasn't got variable receiver")

        // For invocation a.foo() explicit receiver 'a'
        // can be a receiver for 'foo' variable
        // or for 'invoke' function.

        // (1) a.foo + foo.invoke()
        if (!explicitReceiver.value.exists()) {
            addCandidatesForExplicitReceiver(ReceiverWithTypes(variableReceiver, c.context), implicitReceivers, c, isExplicit = true)
        }

        // (2) foo + a.invoke()

        // 'invoke' is member extension to explicit receiver while variable receiver is 'this object'
        //trait A
        //trait Foo { fun A.invoke() }

        if (explicitReceiver.value.exists()) {
            //a.foo()
            addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(variableReceiver, explicitReceiver, c, BOTH_RECEIVERS)
            return
        }
        // with (a) { foo() }
        for (implicitReceiver in implicitReceivers) {
            addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(variableReceiver, ReceiverWithTypes(implicitReceiver, c.context), c, DISPATCH_RECEIVER)
        }
    }

    private fun <D : CallableDescriptor, F : D> addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(
            dispatchReceiver: ReceiverValue,
            receiverParameter: ReceiverWithTypes,
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
            return Visibilities.isVisible(receiverValue, candidateDescriptor, context.scope.ownerDescriptor)
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
