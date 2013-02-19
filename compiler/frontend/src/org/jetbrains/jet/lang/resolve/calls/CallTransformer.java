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

package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.CallCandidateResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionTask;
import org.jetbrains.jet.lang.resolve.calls.util.DelegatingCall;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * CallTransformer treats specially 'variable as function' call case, other cases keeps unchanged (base realization).
 *
 * For the call 'b.foo(1)' where foo is a variable that has method 'invoke' (for example of function type)
 * CallTransformer creates two contexts, two calls in each, and performs second ('invoke') call resolution:
 *
 *   context#1. calls: 'b.foo' 'invoke(1)'
 *   context#2. calls: 'foo'   'b.invoke(1)'
 *
 * If success VariableAsFunctionResolvedCall is created.
 */
public class CallTransformer<D extends CallableDescriptor, F extends D> {
    private CallTransformer() {}

    /**
     * Returns two contexts for 'variable as function' case (in FUNCTION_CALL_TRANSFORMER), one context otherwise
     */
    @NotNull
    public Collection<CallCandidateResolutionContext<D>> createCallContexts(@NotNull ResolutionCandidate<D> candidate,
            @NotNull ResolutionTask<D, F> task,
            @NotNull TemporaryBindingTrace candidateTrace) {

        ResolvedCallImpl<D> candidateCall = ResolvedCallImpl.create(candidate, candidateTrace, task.tracing);
        return Collections.singleton(CallCandidateResolutionContext.create(candidateCall, task, candidateTrace, task.tracing));
    }

    /**
     * Returns collection of resolved calls for 'invoke' for 'variable as function' case (in FUNCTION_CALL_TRANSFORMER),
     * the resolved call from callCandidateResolutionContext otherwise
     */
    @NotNull
    public Collection<ResolvedCallWithTrace<F>> transformCall(@NotNull CallCandidateResolutionContext<D> callCandidateResolutionContext,
            @NotNull CallResolver callResolver,
            @NotNull ResolutionTask<D, F> task) {

        return Collections.singleton((ResolvedCallWithTrace<F>) callCandidateResolutionContext.candidateCall);
    }


    public static CallTransformer<VariableDescriptor, VariableDescriptor> PROPERTY_CALL_TRANSFORMER = new CallTransformer<VariableDescriptor, VariableDescriptor>();

    public static CallTransformer<CallableDescriptor, FunctionDescriptor> FUNCTION_CALL_TRANSFORMER = new CallTransformer<CallableDescriptor, FunctionDescriptor>() {
        @NotNull
        @Override
        public Collection<CallCandidateResolutionContext<CallableDescriptor>> createCallContexts(@NotNull ResolutionCandidate<CallableDescriptor> candidate,
                @NotNull ResolutionTask<CallableDescriptor, FunctionDescriptor> task, @NotNull TemporaryBindingTrace candidateTrace) {

            if (candidate.getDescriptor() instanceof FunctionDescriptor) {
                return super.createCallContexts(candidate, task, candidateTrace);
            }

            assert candidate.getDescriptor() instanceof VariableDescriptor;

            boolean hasReceiver = candidate.getReceiverArgument().exists();
            Call variableCall = stripCallArguments(task);
            if (!hasReceiver) {
                CallCandidateResolutionContext<CallableDescriptor> context = CallCandidateResolutionContext.create(
                        ResolvedCallImpl.create(candidate, candidateTrace, task.tracing), task, candidateTrace, task.tracing, variableCall);
                return Collections.singleton(context);
            }
            Call variableCallWithoutReceiver = stripReceiver(variableCall);
            CallCandidateResolutionContext<CallableDescriptor> contextWithReceiver = createContextWithChainedTrace(
                    candidate, variableCall, candidateTrace, task);

            ResolutionCandidate<CallableDescriptor> candidateWithoutReceiver = ResolutionCandidate.create(
                    candidate.getDescriptor(), candidate.getThisObject(), ReceiverValue.NO_RECEIVER, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, false);

            CallCandidateResolutionContext<CallableDescriptor> contextWithoutReceiver = createContextWithChainedTrace(
                    candidateWithoutReceiver, variableCallWithoutReceiver, candidateTrace, task);

            contextWithoutReceiver.receiverForVariableAsFunctionSecondCall = variableCall.getExplicitReceiver();

            return Lists.newArrayList(contextWithReceiver, contextWithoutReceiver);
        }

        private CallCandidateResolutionContext<CallableDescriptor> createContextWithChainedTrace(ResolutionCandidate<CallableDescriptor> candidate,
                Call call, TemporaryBindingTrace temporaryTrace, ResolutionTask<CallableDescriptor, FunctionDescriptor> task) {

            ChainedTemporaryBindingTrace chainedTrace = ChainedTemporaryBindingTrace.create(temporaryTrace, "chained trace to resolve candidate", candidate);
            ResolvedCallImpl<CallableDescriptor> resolvedCall = ResolvedCallImpl.create(candidate, chainedTrace, task.tracing);
            return CallCandidateResolutionContext.create(resolvedCall, task, chainedTrace, task.tracing, call);
        }

        private Call stripCallArguments(@NotNull ResolutionTask<CallableDescriptor, FunctionDescriptor> task) {
            return new DelegatingCall(task.call) {
                @Override
                public JetValueArgumentList getValueArgumentList() {
                    return null;
                }

                @NotNull
                @Override
                public List<? extends ValueArgument> getValueArguments() {
                    return Collections.emptyList();
                }

                @NotNull
                @Override
                public List<JetExpression> getFunctionLiteralArguments() {
                    return Collections.emptyList();
                }

                @NotNull
                @Override
                public List<JetTypeProjection> getTypeArguments() {
                    return Collections.emptyList();
                }

                @Override
                public JetTypeArgumentList getTypeArgumentList() {
                    return null;
                }
            };
        }

        private Call stripReceiver(@NotNull Call variableCall) {
            return new DelegatingCall(variableCall) {
                @NotNull
                @Override
                public ReceiverValue getExplicitReceiver() {
                    return ReceiverValue.NO_RECEIVER;
                }
            };
        }

        @NotNull
        @Override
        public Collection<ResolvedCallWithTrace<FunctionDescriptor>> transformCall(@NotNull final CallCandidateResolutionContext<CallableDescriptor> context,
                @NotNull CallResolver callResolver, @NotNull final ResolutionTask<CallableDescriptor, FunctionDescriptor> task) {

            final CallableDescriptor descriptor = context.candidateCall.getCandidateDescriptor();
            if (descriptor instanceof FunctionDescriptor) {
                return super.transformCall(context, callResolver, task);
            }

            assert descriptor instanceof VariableDescriptor;
            JetType returnType = descriptor.getReturnType();
            if (returnType == null) {
                return Collections.emptyList();
            }

            final ResolvedCallWithTrace<VariableDescriptor> variableResolvedCall = (ResolvedCallWithTrace)context.candidateCall;

            Call functionCall = createFunctionCall(context, task, returnType);

            final DelegatingBindingTrace variableCallTrace = context.candidateCall.getTrace();
            BasicCallResolutionContext basicCallResolutionContext = BasicCallResolutionContext.create(
                    variableCallTrace, context.scope, functionCall, context.expectedType, context.dataFlowInfo, context.resolveMode, context.expressionPosition);

            // 'invoke' call resolve
            OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveCallWithGivenName(basicCallResolutionContext, task.reference, Name.identifier("invoke"));
            Collection<ResolvedCallWithTrace<FunctionDescriptor>> calls = ((OverloadResolutionResultsImpl<FunctionDescriptor>)results).getResultingCalls();

            return Collections2.transform(calls, new Function<ResolvedCallWithTrace<FunctionDescriptor>, ResolvedCallWithTrace<FunctionDescriptor>>() {
                @Override
                public ResolvedCallWithTrace<FunctionDescriptor> apply(ResolvedCallWithTrace<FunctionDescriptor> functionResolvedCall) {
                    return new VariableAsFunctionResolvedCall(functionResolvedCall, variableResolvedCall);
                }
            });
        }

        private Call createFunctionCall(final CallCandidateResolutionContext<CallableDescriptor> context,
                final ResolutionTask<CallableDescriptor, FunctionDescriptor> task, JetType returnType) {

            final ExpressionReceiver receiverFromVariable = new ExpressionReceiver(task.reference, returnType);
            final JetSimpleNameExpression invokeExpression = (JetSimpleNameExpression) JetPsiFactory.createExpression(
                    task.call.getCallElement().getProject(), "invoke");

            return new CallForImplicitInvoke(task.call) {
                @NotNull
                @Override
                public ReceiverValue getExplicitReceiver() {
                    return context.receiverForVariableAsFunctionSecondCall;
                }

                @NotNull
                @Override
                public ReceiverValue getThisObject() {
                    return receiverFromVariable;
                }

                @Override
                public JetExpression getCalleeExpression() {
                    return invokeExpression;
                }

                @NotNull
                @Override
                public PsiElement getCallElement() {
                    if (task.call.getCallElement() instanceof JetCallElement) {
                        //to report errors properly
                        JetValueArgumentList list = ((JetCallElement)task.call.getCallElement()).getValueArgumentList();
                        if (list != null) {
                            return list;
                        }
                    }
                    return invokeExpression;
                }
            };
        }
    };

    public static class CallForImplicitInvoke extends DelegatingCall {
        public CallForImplicitInvoke(@NotNull Call delegate) {
            super(delegate);
        }
    }
}
