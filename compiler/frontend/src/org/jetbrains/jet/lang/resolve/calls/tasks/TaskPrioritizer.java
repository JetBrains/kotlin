/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.tasks;

import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSuperExpression;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.LibrarySourceHacks;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastServiceImpl;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.NamespaceType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isClassObject;
import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.isOrOverridesSynthesized;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;

public class TaskPrioritizer {

    public static <D extends CallableDescriptor> void splitLexicallyLocalDescriptors(
            @NotNull Collection<ResolutionCandidate<D>> allDescriptors,
            @NotNull DeclarationDescriptor containerOfTheCurrentLocality,
            @NotNull Collection<ResolutionCandidate<D>> local,
            @NotNull Collection<ResolutionCandidate<D>> nonlocal
    ) {
        for (ResolutionCandidate<D> resolvedCall : allDescriptors) {
            if (DescriptorUtils.isLocal(containerOfTheCurrentLocality, resolvedCall.getDescriptor())) {
                local.add(resolvedCall);
            }
            else {
                nonlocal.add(resolvedCall);
            }
        }
    }

    @Nullable
    public static JetSuperExpression getReceiverSuper(@NotNull ReceiverValue receiver) {
        if (receiver instanceof ExpressionReceiver) {
            ExpressionReceiver expressionReceiver = (ExpressionReceiver) receiver;
            JetExpression expression = expressionReceiver.getExpression();
            if (expression instanceof JetSuperExpression) {
                return (JetSuperExpression) expression;
            }
        }
        return null;
    }

    @NotNull
    public static <D extends CallableDescriptor, F extends D> List<ResolutionTask<D, F>> computePrioritizedTasks(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull JetReferenceExpression functionReference,
            @NotNull List<CallableDescriptorCollector<? extends D>> callableDescriptorCollectors
    ) {
        ReceiverValue explicitReceiver = context.call.getExplicitReceiver();
        JetScope scope;
        if (explicitReceiver.exists() && explicitReceiver.getType() instanceof NamespaceType) {
            // Receiver is a namespace
            scope = explicitReceiver.getType().getMemberScope();
            explicitReceiver = NO_RECEIVER;
        }
        else {
            scope = context.scope;
        }

        ResolutionTaskHolder<D, F> result = new ResolutionTaskHolder<D, F>(functionReference, context, new MyPriorityProvider<D>(context), null);
        TaskPrioritizerContext<D, F> c = new TaskPrioritizerContext<D, F>(name, result, context, scope, callableDescriptorCollectors);
        doComputeTasks(explicitReceiver, c);
        return result.getTasks();
    }

    private static <D extends CallableDescriptor, F extends D> void doComputeTasks(
            @NotNull ReceiverValue receiver,
            @NotNull TaskPrioritizerContext<D, F> c
    ) {
        ProgressIndicatorProvider.checkCanceled();

        boolean resolveInvoke = c.context.call.getThisObject().exists();
        if (resolveInvoke) {
            addCandidatesForInvoke(receiver, c);
            return;
        }
        List<ReceiverValue> implicitReceivers = JetScopeUtils.getImplicitReceiversHierarchyValues(c.scope);
        if (receiver.exists()) {
            addCandidatesForExplicitReceiver(receiver, implicitReceivers, c, /*resolveInvoke=*/false);
            return;
        }
        addCandidatesForNoReceiver(implicitReceivers, c);
    }

    private static <D extends CallableDescriptor, F extends D> void addCandidatesForExplicitReceiver(
            @NotNull ReceiverValue receiver,
            @NotNull List<ReceiverValue> implicitReceivers,
            @NotNull TaskPrioritizerContext<D, F> c,
            boolean resolveInvoke
    ) {

        List<ReceiverValue> variantsForExplicitReceiver = c.autoCastService.getVariantsForReceiver(receiver);

        //members
        for (CallableDescriptorCollector<? extends D> callableDescriptorCollector : c.callableDescriptorCollectors) {
            Collection<ResolutionCandidate<D>> members = Lists.newArrayList();
            for (ReceiverValue variant : variantsForExplicitReceiver) {
                Collection<? extends D> membersForThisVariant =
                        callableDescriptorCollector.getMembersByName(variant.getType(), c.name, c.context.trace);
                convertWithReceivers(membersForThisVariant, Collections.singletonList(variant),
                                     Collections.singletonList(NO_RECEIVER), members, resolveInvoke);
            }
            c.result.addCandidates(members);
        }

        for (CallableDescriptorCollector<? extends D> callableDescriptorCollector : c.callableDescriptorCollectors) {
            //member extensions
            for (ReceiverValue implicitReceiver : implicitReceivers) {
                addMemberExtensionCandidates(implicitReceiver, variantsForExplicitReceiver,
                                             callableDescriptorCollector, c, resolveInvoke);
            }
            //extensions
            Collection<ResolutionCandidate<D>> extensions = convertWithImpliedThis(
                    c.scope, variantsForExplicitReceiver, callableDescriptorCollector.getNonMembersByName(c.scope, c.name, c.context.trace));
            c.result.addCandidates(extensions);
        }
    }

    private static <D extends CallableDescriptor, F extends D> void addMemberExtensionCandidates(
            @NotNull ReceiverValue implicitReceiver,
            @NotNull List<ReceiverValue> variantsForExplicitReceiver,
            @NotNull CallableDescriptorCollector<? extends D> callableDescriptorCollector, TaskPrioritizerContext<D, F> c,
            boolean resolveInvoke
    ) {
        Collection<? extends D> memberExtensions = callableDescriptorCollector.getNonMembersByName(
                implicitReceiver.getType().getMemberScope(), c.name, c.context.trace);
        List<ReceiverValue> variantsForImplicitReceiver = c.autoCastService.getVariantsForReceiver(implicitReceiver);
        c.result.addCandidates(convertWithReceivers(memberExtensions, variantsForImplicitReceiver,
                                                  variantsForExplicitReceiver, resolveInvoke));
    }

    private static <D extends CallableDescriptor, F extends D> void addCandidatesForNoReceiver(
            @NotNull List<ReceiverValue> implicitReceivers,
            @NotNull TaskPrioritizerContext<D, F> c
    ) {
        List<Collection<ResolutionCandidate<D>>> localsList = Lists.newArrayList();
        List<Collection<ResolutionCandidate<D>>> nonlocalsList = Lists.newArrayList();
        for (CallableDescriptorCollector<? extends D> callableDescriptorCollector : c.callableDescriptorCollectors) {

            Collection<ResolutionCandidate<D>> members =
                    convertWithImpliedThis(c.scope, Collections.singletonList(NO_RECEIVER), callableDescriptorCollector
                            .getNonExtensionsByName(c.scope, c.name, c.context.trace));

            List<ResolutionCandidate<D>> nonlocals = Lists.newArrayList();
            List<ResolutionCandidate<D>> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(members, c.scope.getContainingDeclaration(), locals, nonlocals);

            localsList.add(locals);
            nonlocalsList.add(nonlocals);
        }

        //locals
        c.result.addCandidates(localsList);

        //try all implicit receivers as explicit
        for (ReceiverValue implicitReceiver : implicitReceivers) {
            addCandidatesForExplicitReceiver(implicitReceiver, implicitReceivers, c, /*resolveInvoke=*/false);
        }
        
        //nonlocals
        c.result.addCandidates(nonlocalsList);
    }

    private static <D extends CallableDescriptor, F extends D> void addCandidatesForInvoke(
            @NotNull ReceiverValue explicitReceiver,
            @NotNull TaskPrioritizerContext<D, F> c
    ) {
        List<ReceiverValue> implicitReceivers = JetScopeUtils.getImplicitReceiversHierarchyValues(c.scope);

        // For 'a.foo()' where foo has function type,
        // a is explicitReceiver, foo is variableReceiver.
        ReceiverValue variableReceiver = c.context.call.getThisObject();
        assert variableReceiver.exists() : "'Invoke' call hasn't got variable receiver";

        // For invocation a.foo() explicit receiver 'a'
        // can be a receiver for 'foo' variable
        // or for 'invoke' function.

        // (1) a.foo + foo.invoke()
        if (!explicitReceiver.exists()) {
            addCandidatesForExplicitReceiver(variableReceiver, implicitReceivers, c, /*resolveInvoke=*/true);
        }

        // (2) foo + a.invoke()

        // 'invoke' is member extension to explicit receiver while variable receiver is 'this object'
        //trait A
        //trait Foo { fun A.invoke() }

        if (explicitReceiver.exists()) {
            //a.foo()
            addCandidatesWhenInvokeIsMemberExtensionToExplicitReceiver(variableReceiver, explicitReceiver, c);
            return;
        }
        // with (a) { foo() }
        for (ReceiverValue implicitReceiver : implicitReceivers) {
            addCandidatesWhenInvokeIsMemberExtensionToExplicitReceiver(variableReceiver, implicitReceiver, c);
        }
    }

    private static <D extends CallableDescriptor, F extends D> void addCandidatesWhenInvokeIsMemberExtensionToExplicitReceiver(
            @NotNull ReceiverValue variableReceiver,
            @NotNull ReceiverValue explicitReceiver,
            @NotNull TaskPrioritizerContext<D, F> c
    ) {
        List<ReceiverValue> variantsForExplicitReceiver = c.autoCastService.getVariantsForReceiver(explicitReceiver);

        for (CallableDescriptorCollector<? extends D> callableDescriptorCollector : c.callableDescriptorCollectors) {
            addMemberExtensionCandidates(variableReceiver, variantsForExplicitReceiver, callableDescriptorCollector, c, /*resolveInvoke=*/true);
        }
    }

    private static <D extends CallableDescriptor> Collection<ResolutionCandidate<D>> convertWithReceivers(
            @NotNull Collection<? extends D> descriptors,
            @NotNull Iterable<ReceiverValue> thisObjects,
            @NotNull Iterable<ReceiverValue> receiverParameters,
            boolean hasExplicitThisObject
    ) {
        Collection<ResolutionCandidate<D>> result = Lists.newArrayList();
        convertWithReceivers(descriptors, thisObjects, receiverParameters, result, hasExplicitThisObject);
        return result;
    }

    private static <D extends CallableDescriptor> void convertWithReceivers(
            @NotNull Collection<? extends D> descriptors,
            @NotNull Iterable<ReceiverValue> thisObjects,
            @NotNull Iterable<ReceiverValue> receiverParameters,
            @NotNull Collection<ResolutionCandidate<D>> result,
            boolean hasExplicitThisObject
    ) {
        for (ReceiverValue thisObject : thisObjects) {
            for (ReceiverValue receiverParameter : receiverParameters) {
                for (D extension : descriptors) {
                    if (DescriptorUtils.isConstructorOfStaticNestedClass(extension)) {
                        // We don't want static nested classes' constructors to be resolved with expectedThisObject
                        continue;
                    }
                    ResolutionCandidate<D> candidate = ResolutionCandidate.create(extension);
                    candidate.setThisObject(thisObject);
                    candidate.setReceiverArgument(receiverParameter);
                    candidate.setExplicitReceiverKind(
                            hasExplicitThisObject ? ExplicitReceiverKind.BOTH_RECEIVERS : ExplicitReceiverKind.THIS_OBJECT);
                    result.add(candidate);
                }
            }
        }
    }

    public static <D extends CallableDescriptor> Collection<ResolutionCandidate<D>> convertWithImpliedThis(
            @NotNull JetScope scope,
            @NotNull Collection<ReceiverValue> receiverParameters,
            @NotNull Collection<? extends D> descriptors
    ) {
        Collection<ResolutionCandidate<D>> result = Lists.newArrayList();
        for (ReceiverValue receiverParameter : receiverParameters) {
            for (D descriptor : descriptors) {
                ResolutionCandidate<D> candidate = ResolutionCandidate.create(descriptor);
                candidate.setReceiverArgument(receiverParameter);
                candidate.setExplicitReceiverKind(
                        receiverParameter.exists() ? ExplicitReceiverKind.RECEIVER_ARGUMENT : ExplicitReceiverKind.NO_EXPLICIT_RECEIVER);
                if (setImpliedThis(scope, candidate)) {
                    result.add(candidate);
                }
            }
        }
        if (receiverParameters.size() == 1 && !receiverParameters.iterator().next().exists()) {
            for (D descriptor : descriptors) {
                if (descriptor.getExpectedThisObject() != null && descriptor.getReceiverParameter() == null) {
                    DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
                    if (descriptor instanceof ConstructorDescriptor) {
                        containingDeclaration = containingDeclaration.getContainingDeclaration();
                    }
                    if (containingDeclaration != null && isClassObject(containingDeclaration)) {
                        ResolutionCandidate<D> candidate = ResolutionCandidate.create(descriptor);
                        candidate.setThisObject(((ClassDescriptor) containingDeclaration).getThisAsReceiverParameter().getValue());
                        candidate.setExplicitReceiverKind(ExplicitReceiverKind.NO_EXPLICIT_RECEIVER);
                        result.add(candidate);
                    }
                }
            }
        }
        return result;
    }

    private static <D extends CallableDescriptor> boolean setImpliedThis(
            @NotNull JetScope scope,
            @NotNull ResolutionCandidate<D> candidate
    ) {
        ReceiverParameterDescriptor expectedThisObject = candidate.getDescriptor().getExpectedThisObject();
        if (expectedThisObject == null) return true;
        List<ReceiverParameterDescriptor> receivers = scope.getImplicitReceiversHierarchy();
        for (ReceiverParameterDescriptor receiver : receivers) {
            if (JetTypeChecker.INSTANCE.isSubtypeOf(receiver.getType(), expectedThisObject.getType())) {
                // TODO : Autocasts & nullability
                candidate.setThisObject(expectedThisObject.getValue());
                return true;
            }
        }
        return false;
    }

    public static <D extends CallableDescriptor, F extends D> List<ResolutionTask<D, F>> computePrioritizedTasksFromCandidates(
            @NotNull BasicCallResolutionContext context,
            @NotNull JetReferenceExpression functionReference,
            @NotNull Collection<ResolutionCandidate<D>> candidates,
            @Nullable TracingStrategy tracing
    ) {
        ResolutionTaskHolder<D, F> result = new ResolutionTaskHolder<D, F>(
                functionReference, context, new MyPriorityProvider<D>(context), tracing);
        result.addCandidates(candidates);
        return result.getTasks();
    }

    private static class MyPriorityProvider<D extends CallableDescriptor>
            implements ResolutionTaskHolder.PriorityProvider<ResolutionCandidate<D>> {
        private final BasicCallResolutionContext context;

        public MyPriorityProvider(BasicCallResolutionContext context) {
            this.context = context;
        }

        @Override
        public int getPriority(ResolutionCandidate<D> call) {
            return (isVisible(call) ? 2 : 0) + (isSynthesized(call) ? 0 : 1);
        }

        @Override
        public int getMaxPriority() {
            return 3;
        }

        private boolean isVisible(ResolutionCandidate<D> call) {
            if (call == null) return false;
            D candidateDescriptor = call.getDescriptor();
            if (ErrorUtils.isError(candidateDescriptor)) return true;
            return Visibilities.isVisible(candidateDescriptor, context.scope.getContainingDeclaration());
        }

        private boolean isSynthesized(ResolutionCandidate<D> call) {
            D descriptor = call.getDescriptor();
            return descriptor instanceof CallableMemberDescriptor &&
                   isOrOverridesSynthesized((CallableMemberDescriptor) descriptor);
        }
    }

    private static class TaskPrioritizerContext<D extends CallableDescriptor, F extends D> {
        @NotNull public final Name name;
        @NotNull public final ResolutionTaskHolder<D, F> result;
        @NotNull public final BasicCallResolutionContext context;
        @NotNull public final JetScope scope;
        @NotNull public final List<CallableDescriptorCollector<? extends D>> callableDescriptorCollectors;
        @NotNull AutoCastServiceImpl autoCastService;

        private TaskPrioritizerContext(
                @NotNull Name name,
                @NotNull ResolutionTaskHolder<D, F> result,
                @NotNull BasicCallResolutionContext context,
                @NotNull JetScope scope,
                @NotNull List<CallableDescriptorCollector<? extends D>> callableDescriptorCollectors
        ) {
            this.name = name;
            this.result = result;
            this.context = context;
            this.scope = scope;
            this.callableDescriptorCollectors = callableDescriptorCollectors;
            autoCastService = new AutoCastServiceImpl(context.dataFlowInfo, context.trace.getBindingContext());
        }
    }
}
