/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSuperExpression;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastServiceImpl;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.NamespaceType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 */
/*package*/ abstract class TaskPrioritizer<D extends CallableDescriptor> {

    public static <D extends CallableDescriptor> void splitLexicallyLocalDescriptors(
            @NotNull Collection<ResolvedCallImpl<D>> allDescriptors,
            @NotNull DeclarationDescriptor containerOfTheCurrentLocality,
            @NotNull Collection<ResolvedCallImpl<D>> local,
            @NotNull Collection<ResolvedCallImpl<D>> nonlocal
    ) {
        for (ResolvedCallImpl<D> resolvedCall : allDescriptors) {
            if (DescriptorUtils.isLocal(containerOfTheCurrentLocality, resolvedCall.getCandidateDescriptor())) {
                local.add(resolvedCall);
            }
            else {
                nonlocal.add(resolvedCall);
            }
        }
    }

    @Nullable
    /*package*/ static JetSuperExpression getReceiverSuper(@NotNull ReceiverDescriptor receiver) {
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
    public List<ResolutionTask<D>> computePrioritizedTasks(@NotNull BasicResolutionContext context, @NotNull String name,
                                                           @NotNull JetReferenceExpression functionReference) {
        List<ResolutionTask<D>> result = Lists.newArrayList();

        ReceiverDescriptor explicitReceiver = context.call.getExplicitReceiver();
        JetScope scope = context.scope;
        if (explicitReceiver.exists() && explicitReceiver.getType() instanceof NamespaceType) {
            // Receiver is a namespace
            scope = explicitReceiver.getType().getMemberScope();
            explicitReceiver = NO_RECEIVER;
        }
        doComputeTasks(scope, explicitReceiver, name, result, context, functionReference);

        return result;
    }

    private void doComputeTasks(JetScope scope, ReceiverDescriptor receiver, String name, List<ResolutionTask<D>> result, @NotNull BasicResolutionContext context, @NotNull JetReferenceExpression functionReference) {
        AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(context.dataFlowInfo, context.trace.getBindingContext());
        DataFlowInfo dataFlowInfo = autoCastService.getDataFlowInfo();
        List<ReceiverDescriptor> implicitReceivers = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(implicitReceivers);
        if (receiver.exists()) {
            List<ReceiverDescriptor> variantsForExplicitReceiver = autoCastService.getVariantsForReceiver(receiver);

            Collection<ResolvedCallImpl<D>> extensionFunctions = convertWithImpliedThis(scope, variantsForExplicitReceiver, getExtensionsByName(scope, name));
            List<ResolvedCallImpl<D>> nonlocals = Lists.newArrayList();
            List<ResolvedCallImpl<D>> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(extensionFunctions, scope.getContainingDeclaration(), locals, nonlocals);

            Collection<ResolvedCallImpl<D>> members = Lists.newArrayList();
            for (ReceiverDescriptor variant : variantsForExplicitReceiver) {
                Collection<D> membersForThisVariant = getMembersByName(variant.getType(), name);
                convertWithReceivers(membersForThisVariant, Collections.singletonList(variant), Collections.singletonList(NO_RECEIVER), members);
            }

            if (getReceiverSuper(receiver) != null) {
                // If the call is of the form super.foo(), it can actually be only a member
                // But  if there's no appropriate member, we would like to report that super cannot be a receiver for an extension
                // Thus, put members first
                addTask(result, members, context, functionReference);
                addTask(result, locals, context, functionReference);
            }
            else {
                addTask(result, locals, context, functionReference);
                addTask(result, members, context, functionReference);
            }

            for (ReceiverDescriptor implicitReceiver : implicitReceivers) {
                Collection<D> memberExtensions = getExtensionsByName(implicitReceiver.getType().getMemberScope(), name);
                List<ReceiverDescriptor> variantsForImplicitReceiver = autoCastService.getVariantsForReceiver(implicitReceiver);
                addTask(result, convertWithReceivers(memberExtensions, variantsForImplicitReceiver, variantsForExplicitReceiver), context, functionReference);
            }

            addTask(result, nonlocals, context, functionReference);
        }
        else {
            Collection<ResolvedCallImpl<D>> functions = convertWithImpliedThis(scope, Collections.singletonList(receiver), getNonExtensionsByName(scope, name));

            List<ResolvedCallImpl<D>> nonlocals = Lists.newArrayList();
            List<ResolvedCallImpl<D>> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(functions, scope.getContainingDeclaration(), locals, nonlocals);

            addTask(result, locals, context, functionReference);

            for (ReceiverDescriptor implicitReceiver : implicitReceivers) {
                doComputeTasks(scope, implicitReceiver, name, result, context, functionReference);
            }

            addTask(result, nonlocals, context, functionReference);
        }
    }

    private Collection<ResolvedCallImpl<D>> convertWithReceivers(Collection<D> descriptors, Iterable<ReceiverDescriptor> thisObjects, Iterable<ReceiverDescriptor> receiverParameters) {
        Collection<ResolvedCallImpl<D>> result = Lists.newArrayList();
        convertWithReceivers(descriptors, thisObjects, receiverParameters, result);
        return result;
    }

    private void convertWithReceivers(Collection<D> descriptors, Iterable<ReceiverDescriptor> thisObjects, Iterable<ReceiverDescriptor> receiverParameters, Collection<ResolvedCallImpl<D>> result) {
//        Collection<ResolvedCallImpl<D>> result = Lists.newArrayList();
        for (ReceiverDescriptor thisObject : thisObjects) {
            for (ReceiverDescriptor receiverParameter : receiverParameters) {
                for (D extension : descriptors) {
                    ResolvedCallImpl<D> resolvedCall = ResolvedCallImpl.create(extension);
                    resolvedCall.setThisObject(thisObject);
                    resolvedCall.setReceiverArgument(receiverParameter);
                    result.add(resolvedCall);
                }
            }
        }
//        return result;
    }

    public static <D extends CallableDescriptor> Collection<ResolvedCallImpl<D>> convertWithImpliedThis(JetScope scope, Iterable<ReceiverDescriptor> receiverParameters, Collection<? extends D> descriptors) {
        Collection<ResolvedCallImpl<D>> result = Lists.newArrayList();
        for (ReceiverDescriptor receiverParameter : receiverParameters) {
            for (D descriptor : descriptors) {
                ResolvedCallImpl<D> resolvedCall = ResolvedCallImpl.create(descriptor);
                resolvedCall.setReceiverArgument(receiverParameter);
                if (setImpliedThis(scope, resolvedCall)) {
                    result.add(resolvedCall);
                }
            }
        }
        for (D descriptor : descriptors) {
            if (descriptor.getExpectedThisObject().exists()) {
                DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
                if (descriptor instanceof ConstructorDescriptor) {
                    assert containingDeclaration != null;
                    containingDeclaration = containingDeclaration.getContainingDeclaration();
                }
                if (containingDeclaration instanceof ClassDescriptor && ((ClassDescriptor) containingDeclaration).getKind() == ClassKind.OBJECT) {
                    ResolvedCallImpl<D> resolvedCall = ResolvedCallImpl.create(descriptor);
                    resolvedCall.setThisObject(new ClassReceiver((ClassDescriptor) containingDeclaration));
                    result.add(resolvedCall);
                }
            }
        }
        return result;
    }

    private static <D extends CallableDescriptor> boolean setImpliedThis(@NotNull JetScope scope, ResolvedCallImpl<D> resolvedCall) {
        ReceiverDescriptor expectedThisObject = resolvedCall.getCandidateDescriptor().getExpectedThisObject();
        if (!expectedThisObject.exists()) return true;
        List<ReceiverDescriptor> receivers = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(receivers);
        for (ReceiverDescriptor receiver : receivers) {
            if (JetTypeChecker.INSTANCE.isSubtypeOf(receiver.getType(), expectedThisObject.getType())) {
                // TODO : Autocasts & nullability
                resolvedCall.setThisObject(expectedThisObject);
                return true;
            }
        }
        return false;
    }

    private void addTask(@NotNull List<ResolutionTask<D>> result, @NotNull Collection<ResolvedCallImpl<D>> candidates, @NotNull final BasicResolutionContext context, @NotNull JetReferenceExpression functionReference) {
        Collection<ResolvedCallImpl<D>> visibleCandidates = Collections2.filter(candidates, new Predicate<ResolvedCallImpl<D>>() {
            @Override
            public boolean apply(@Nullable ResolvedCallImpl<D> call) {
                if (call == null) return false;
                D candidateDescriptor = call.getCandidateDescriptor();
                if (ErrorUtils.isError(candidateDescriptor)) return true;
                return Visibilities.isVisible(candidateDescriptor, context.scope.getContainingDeclaration());
            }
        });
        if (visibleCandidates.isEmpty()) return;
        result.add(new ResolutionTask<D>(visibleCandidates, functionReference, context));
    }

    @NotNull
    protected abstract Collection<D> getNonExtensionsByName(JetScope scope, String name);

    @NotNull
    protected abstract Collection<D> getMembersByName(@NotNull JetType receiver, String name);

    @NotNull
    protected abstract Collection<D> getExtensionsByName(JetScope scope, String name);
}
