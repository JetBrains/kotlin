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
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSuperExpression;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastUtils;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.PackageType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.calls.CallResolverUtil.isOrOverridesSynthesized;
import static org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind.*;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;

public class TaskPrioritizer {

    public static <D extends CallableDescriptor> void splitLexicallyLocalDescriptors(
            @NotNull Collection<ResolutionCandidate<D>> allDescriptors,
            @NotNull DeclarationDescriptor containerOfTheCurrentLocality,
            @NotNull Collection<ResolutionCandidate<D>> local,
            @NotNull Collection<ResolutionCandidate<D>> nonlocal
    ) {
        for (ResolutionCandidate<D> resolvedCall : allDescriptors) {
            if (ExpressionTypingUtils.isLocal(containerOfTheCurrentLocality, resolvedCall.getDescriptor())) {
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
            @NotNull TracingStrategy tracing,
            @NotNull CallableDescriptorCollectors<D> callableDescriptorCollectors
    ) {
        List<Pair<JetScope, ReceiverValue>> variants = new ArrayList<Pair<JetScope, ReceiverValue>>(2);

        ReceiverValue explicitReceiver = context.call.getExplicitReceiver();
        if (explicitReceiver.exists() && explicitReceiver.getType() instanceof PackageType) {
            JetType receiverType = explicitReceiver.getType();
            variants.add(Pair.create(receiverType.getMemberScope(), NO_RECEIVER));
            ReceiverValue value = ((PackageType) receiverType).getReceiverValue();
            if (value.exists()) {
                variants.add(Pair.create(context.scope, value));
            }
        }
        else {
            variants.add(Pair.create(context.scope, explicitReceiver));
        }

        ResolutionTaskHolder<D, F> result =
                new ResolutionTaskHolder<D, F>(context, new MyPriorityProvider<D>(context), tracing);
        for (Pair<JetScope, ReceiverValue> pair : variants) {
            doComputeTasks(pair.second, new TaskPrioritizerContext<D, F>(name, result, context, pair.first, callableDescriptorCollectors));
        }

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
        Collection<ReceiverValue> implicitReceivers = Sets.newLinkedHashSet(JetScopeUtils.getImplicitReceiversHierarchyValues(c.scope));
        if (receiver.exists()) {
            addCandidatesForExplicitReceiver(receiver, implicitReceivers, c, /*isExplicit=*/true);
            return;
        }
        addCandidatesForNoReceiver(implicitReceivers, c);
    }

    private static <D extends CallableDescriptor, F extends D> void addCandidatesForExplicitReceiver(
            @NotNull ReceiverValue explicitReceiver,
            @NotNull Collection<ReceiverValue> implicitReceivers,
            @NotNull TaskPrioritizerContext<D, F> c,
            boolean isExplicit
    ) {

        List<JetType> variantsForExplicitReceiver = AutoCastUtils.getAutoCastVariants(explicitReceiver, c.context);

        //members
        for (CallableDescriptorCollector<D> callableDescriptorCollector : c.callableDescriptorCollectors) {
            Collection<ResolutionCandidate<D>> members = Lists.newArrayList();
            for (JetType type : variantsForExplicitReceiver) {
                Collection<D> membersForThisVariant =
                        callableDescriptorCollector.getMembersByName(type, c.name, c.context.trace);
                convertWithReceivers(membersForThisVariant, explicitReceiver,
                                     NO_RECEIVER, members, createKind(THIS_OBJECT, isExplicit), c.context.call);
            }
            c.result.addCandidates(members);
        }

        for (CallableDescriptorCollector<D> callableDescriptorCollector : c.callableDescriptorCollectors) {
            //member extensions
            for (ReceiverValue implicitReceiver : implicitReceivers) {
                addMemberExtensionCandidates(implicitReceiver, explicitReceiver,
                                             callableDescriptorCollector, c, createKind(RECEIVER_ARGUMENT, isExplicit));
            }
            //extensions
            Collection<ResolutionCandidate<D>> extensions = convertWithImpliedThis(
                    c.scope, explicitReceiver, callableDescriptorCollector.getNonMembersByName(c.scope, c.name, c.context.trace),
                    createKind(RECEIVER_ARGUMENT, isExplicit), c.context.call);
            c.result.addCandidates(extensions);
        }
    }

    private static ExplicitReceiverKind createKind(ExplicitReceiverKind kind, boolean isExplicit) {
        if (isExplicit) return kind;
        return ExplicitReceiverKind.NO_EXPLICIT_RECEIVER;
    }

    private static <D extends CallableDescriptor, F extends D> void addMemberExtensionCandidates(
            @NotNull ReceiverValue thisObject,
            @NotNull ReceiverValue receiverParameter,
            @NotNull CallableDescriptorCollector<D> callableDescriptorCollector, TaskPrioritizerContext<D, F> c,
            @NotNull ExplicitReceiverKind receiverKind
    ) {
        Collection<D> memberExtensions = callableDescriptorCollector.getNonMembersByName(
                thisObject.getType().getMemberScope(), c.name, c.context.trace);
        c.result.addCandidates(convertWithReceivers(
                memberExtensions, thisObject, receiverParameter, receiverKind, c.context.call));
    }

    private static <D extends CallableDescriptor, F extends D> void addCandidatesForNoReceiver(
            @NotNull Collection<ReceiverValue> implicitReceivers,
            @NotNull TaskPrioritizerContext<D, F> c
    ) {
        List<Collection<ResolutionCandidate<D>>> localsList = Lists.newArrayList();
        List<Collection<ResolutionCandidate<D>>> nonlocalsList = Lists.newArrayList();
        for (CallableDescriptorCollector<D> callableDescriptorCollector : c.callableDescriptorCollectors) {

            Collection<ResolutionCandidate<D>> members = convertWithImpliedThisAndNoReceiver(
                    c.scope, callableDescriptorCollector.getNonExtensionsByName(c.scope, c.name, c.context.trace), c.context.call);

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
            addCandidatesForExplicitReceiver(implicitReceiver, implicitReceivers, c, /*isExplicit=*/false);
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
            addCandidatesForExplicitReceiver(variableReceiver, implicitReceivers, c, /*isExplicit=*/true);
        }

        // (2) foo + a.invoke()

        // 'invoke' is member extension to explicit receiver while variable receiver is 'this object'
        //trait A
        //trait Foo { fun A.invoke() }

        if (explicitReceiver.exists()) {
            //a.foo()
            addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(variableReceiver, explicitReceiver, c, BOTH_RECEIVERS);
            return;
        }
        // with (a) { foo() }
        for (ReceiverValue implicitReceiver : implicitReceivers) {
            addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(variableReceiver, implicitReceiver, c, THIS_OBJECT);
        }
    }

    private static <D extends CallableDescriptor, F extends D> void addCandidatesWhenInvokeIsMemberAndExtensionToExplicitReceiver(
            @NotNull ReceiverValue thisObject,
            @NotNull ReceiverValue receiverParameter,
            @NotNull TaskPrioritizerContext<D, F> c,
            @NotNull ExplicitReceiverKind receiverKind
    ) {
        for (CallableDescriptorCollector<D> callableDescriptorCollector : c.callableDescriptorCollectors) {
            addMemberExtensionCandidates(thisObject, receiverParameter, callableDescriptorCollector, c, receiverKind);
        }
    }

    private static <D extends CallableDescriptor> Collection<ResolutionCandidate<D>> convertWithReceivers(
            @NotNull Collection<D> descriptors,
            @NotNull ReceiverValue thisObject,
            @NotNull ReceiverValue receiverParameter,
            @NotNull ExplicitReceiverKind explicitReceiverKind,
            @NotNull Call call
    ) {
        Collection<ResolutionCandidate<D>> result = Lists.newArrayList();
        convertWithReceivers(descriptors, thisObject, receiverParameter, result, explicitReceiverKind, call);
        return result;
    }

    private static <D extends CallableDescriptor> void convertWithReceivers(
            @NotNull Collection<D> descriptors,
            @NotNull ReceiverValue thisObject,
            @NotNull ReceiverValue receiverParameter,
            @NotNull Collection<ResolutionCandidate<D>> result,
            @NotNull ExplicitReceiverKind explicitReceiverKind,
            @NotNull Call call
    ) {
        for (D extension : descriptors) {
            if (extension instanceof ConstructorDescriptor && DescriptorUtils.isStaticNestedClass(extension.getContainingDeclaration())) {
                // We don't want static nested classes' constructors to be resolved with expectedThisObject
                continue;
            }
            ResolutionCandidate<D> candidate = ResolutionCandidate.create(call, extension);
            candidate.setThisObject(thisObject);
            candidate.setReceiverArgument(receiverParameter);
            candidate.setExplicitReceiverKind(explicitReceiverKind);
            result.add(candidate);
        }
    }

    public static <D extends CallableDescriptor> Collection<ResolutionCandidate<D>> convertWithImpliedThisAndNoReceiver(
            @NotNull JetScope scope,
            @NotNull Collection<? extends D> descriptors,
            @NotNull Call call
    ) {
        return convertWithImpliedThis(scope, NO_RECEIVER, descriptors, NO_EXPLICIT_RECEIVER, call);
    }

    public static <D extends CallableDescriptor> Collection<ResolutionCandidate<D>> convertWithImpliedThis(
            @NotNull JetScope scope,
            @NotNull ReceiverValue receiverParameter,
            @NotNull Collection<? extends D> descriptors,
            @NotNull ExplicitReceiverKind receiverKind,
            @NotNull Call call
    ) {
        Collection<ResolutionCandidate<D>> result = Lists.newArrayList();
        for (D descriptor : descriptors) {
            ResolutionCandidate<D> candidate = ResolutionCandidate.create(call, descriptor);
            candidate.setReceiverArgument(receiverParameter);
            candidate.setExplicitReceiverKind(receiverKind);
            if (setImpliedThis(scope, candidate)) {
                result.add(candidate);
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
            if (JetTypeChecker.DEFAULT.isSubtypeOf(receiver.getType(), expectedThisObject.getType())) {
                // TODO : Autocasts & nullability
                candidate.setThisObject(expectedThisObject.getValue());
                return true;
            }
        }
        return false;
    }

    public static <D extends CallableDescriptor, F extends D> List<ResolutionTask<D, F>> computePrioritizedTasksFromCandidates(
            @NotNull BasicCallResolutionContext context,
            @NotNull Collection<ResolutionCandidate<D>> candidates,
            @NotNull TracingStrategy tracing
    ) {
        ResolutionTaskHolder<D, F> result = new ResolutionTaskHolder<D, F>(
                context, new MyPriorityProvider<D>(context), tracing);
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
        @NotNull public final CallableDescriptorCollectors<D> callableDescriptorCollectors;

        private TaskPrioritizerContext(
                @NotNull Name name,
                @NotNull ResolutionTaskHolder<D, F> result,
                @NotNull BasicCallResolutionContext context,
                @NotNull JetScope scope,
                @NotNull CallableDescriptorCollectors<D> callableDescriptorCollectors
        ) {
            this.name = name;
            this.result = result;
            this.context = context;
            this.scope = scope;
            this.callableDescriptorCollectors = callableDescriptorCollectors;
        }
    }
}
