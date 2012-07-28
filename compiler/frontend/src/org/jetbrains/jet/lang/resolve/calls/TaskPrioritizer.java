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
import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSuperExpression;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastServiceImpl;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.NamespaceType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 */
/*package*/ abstract class TaskPrioritizer {

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
    public static <D extends CallableDescriptor, F extends D> List<ResolutionTask<D, F>> computePrioritizedTasks(@NotNull BasicResolutionContext context, @NotNull Name name,
                                                           @NotNull JetReferenceExpression functionReference, @NotNull List<CallableDescriptorCollector<? extends D>> callableDescriptorCollectors) {
        ReceiverDescriptor explicitReceiver = context.call.getExplicitReceiver();
        final JetScope scope;
        if (explicitReceiver.exists() && explicitReceiver.getType() instanceof NamespaceType) {
            // Receiver is a namespace
            scope = explicitReceiver.getType().getMemberScope();
            explicitReceiver = NO_RECEIVER;
        }
        else {
            scope = context.scope;
        }
        final Predicate<ResolutionCandidate<D>> visibleStrategy = new Predicate<ResolutionCandidate<D>>() {
            @Override
            public boolean apply(@Nullable ResolutionCandidate<D> call) {
                if (call == null) return false;
                D candidateDescriptor = call.getDescriptor();
                if (ErrorUtils.isError(candidateDescriptor)) return true;
                return Visibilities.isVisible(candidateDescriptor, scope.getContainingDeclaration());
            }
        };

        ResolutionTaskHolder<D, F> result = new ResolutionTaskHolder<D, F>(functionReference, context, visibleStrategy);
        for (CallableDescriptorCollector<? extends D> callableDescriptorCollector : callableDescriptorCollectors) {
            doComputeTasks(scope, explicitReceiver, name, result, context, callableDescriptorCollector);
        }
        return result.getTasks();
    }

    private static <D extends CallableDescriptor, F extends D> void doComputeTasks(@NotNull JetScope scope, @NotNull ReceiverDescriptor receiver,
            @NotNull Name name, @NotNull ResolutionTaskHolder<D, F> result,
            @NotNull BasicResolutionContext context, @NotNull CallableDescriptorCollector<? extends D> callableDescriptorCollector) {

        ProgressIndicatorProvider.checkCanceled();

        AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(context.dataFlowInfo, context.trace.getBindingContext());
        List<ReceiverDescriptor> implicitReceivers = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(implicitReceivers);
        boolean hasExplicitThisObject = context.call.getThisObject().exists();
        if (hasExplicitThisObject) {
            implicitReceivers.add(context.call.getThisObject());
        }
        if (receiver.exists()) {
            List<ReceiverDescriptor> variantsForExplicitReceiver = autoCastService.getVariantsForReceiver(receiver);

            Collection<ResolutionCandidate<D>> extensionFunctions = convertWithImpliedThis(scope, variantsForExplicitReceiver, callableDescriptorCollector.getNonMembersByName(scope, name));
            List<ResolutionCandidate<D>> nonlocals = Lists.newArrayList();
            List<ResolutionCandidate<D>> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(extensionFunctions, scope.getContainingDeclaration(), locals, nonlocals);

            Collection<ResolutionCandidate<D>> members = Lists.newArrayList();
            for (ReceiverDescriptor variant : variantsForExplicitReceiver) {
                Collection<? extends D> membersForThisVariant = callableDescriptorCollector.getMembersByName(variant.getType(), name);
                convertWithReceivers(membersForThisVariant, Collections.singletonList(variant), Collections.singletonList(NO_RECEIVER), members, hasExplicitThisObject);
            }

            result.addLocalExtensions(locals);
            result.addMembers(members);

            for (ReceiverDescriptor implicitReceiver : implicitReceivers) {
                Collection<? extends D> memberExtensions = callableDescriptorCollector.getNonMembersByName(
                        implicitReceiver.getType().getMemberScope(), name);
                List<ReceiverDescriptor> variantsForImplicitReceiver = autoCastService.getVariantsForReceiver(implicitReceiver);
                result.addNonLocalExtensions(convertWithReceivers(memberExtensions, variantsForImplicitReceiver, variantsForExplicitReceiver, hasExplicitThisObject));
            }

            result.addNonLocalExtensions(nonlocals);
        }
        else {
            Collection<ResolutionCandidate<D>> functions = convertWithImpliedThis(scope, Collections.singletonList(receiver), callableDescriptorCollector
                    .getNonExtensionsByName(scope, name));

            List<ResolutionCandidate<D>> nonlocals = Lists.newArrayList();
            List<ResolutionCandidate<D>> locals = Lists.newArrayList();
            //noinspection unchecked,RedundantTypeArguments
            TaskPrioritizer.<D>splitLexicallyLocalDescriptors(functions, scope.getContainingDeclaration(), locals, nonlocals);

            result.addLocalExtensions(locals);
            result.addNonLocalExtensions(nonlocals);

            for (ReceiverDescriptor implicitReceiver : implicitReceivers) {
                doComputeTasks(scope, implicitReceiver, name, result, context, callableDescriptorCollector);
            }
        }
    }

    private static <D extends CallableDescriptor> Collection<ResolutionCandidate<D>> convertWithReceivers(Collection<? extends D> descriptors, Iterable<ReceiverDescriptor> thisObjects,
            Iterable<ReceiverDescriptor> receiverParameters, boolean hasExplicitThisObject) {

        Collection<ResolutionCandidate<D>> result = Lists.newArrayList();
        convertWithReceivers(descriptors, thisObjects, receiverParameters, result, hasExplicitThisObject);
        return result;
    }

    private static <D extends CallableDescriptor> void convertWithReceivers(Collection<? extends D> descriptors, Iterable<ReceiverDescriptor> thisObjects, Iterable<ReceiverDescriptor> receiverParameters,
            Collection<ResolutionCandidate<D>> result, boolean hasExplicitThisObject) {

        for (ReceiverDescriptor thisObject : thisObjects) {
            for (ReceiverDescriptor receiverParameter : receiverParameters) {
                for (D extension : descriptors) {
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

    public static <D extends CallableDescriptor> Collection<ResolutionCandidate<D>> convertWithImpliedThis(JetScope scope, Collection<ReceiverDescriptor> receiverParameters, Collection<? extends D> descriptors) {
        Collection<ResolutionCandidate<D>> result = Lists.newArrayList();
        for (ReceiverDescriptor receiverParameter : receiverParameters) {
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
                if (descriptor.getExpectedThisObject().exists() && !descriptor.getReceiverParameter().exists()) {
                    DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
                    if (descriptor instanceof ConstructorDescriptor) {
                        assert containingDeclaration != null;
                        containingDeclaration = containingDeclaration.getContainingDeclaration();
                    }
                    if (containingDeclaration instanceof ClassDescriptor && DescriptorUtils.isClassObject(containingDeclaration)) {
                        ResolutionCandidate<D> candidate = ResolutionCandidate.create(descriptor);
                        candidate.setThisObject(new ClassReceiver((ClassDescriptor) containingDeclaration));
                        candidate.setExplicitReceiverKind(ExplicitReceiverKind.NO_EXPLICIT_RECEIVER);
                        result.add(candidate);
                    }
                }
            }
        }
        return result;
    }

    private static <D extends CallableDescriptor> boolean setImpliedThis(@NotNull JetScope scope, ResolutionCandidate<D> candidate) {
        ReceiverDescriptor expectedThisObject = candidate.getDescriptor().getExpectedThisObject();
        if (!expectedThisObject.exists()) return true;
        List<ReceiverDescriptor> receivers = Lists.newArrayList();
        scope.getImplicitReceiversHierarchy(receivers);
        for (ReceiverDescriptor receiver : receivers) {
            if (JetTypeChecker.INSTANCE.isSubtypeOf(receiver.getType(), expectedThisObject.getType())) {
                // TODO : Autocasts & nullability
                candidate.setThisObject(expectedThisObject);
                return true;
            }
        }
        return false;
    }
}
