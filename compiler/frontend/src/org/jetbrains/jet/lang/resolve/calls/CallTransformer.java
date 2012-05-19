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
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
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
 *
 * @author svtk
 */
public class CallTransformer<D extends CallableDescriptor, F extends D> {
    private CallTransformer() {}

    /**
     * Returns two contexts for 'variable as function' case (in FUNCTION_CALL_TRANSFORMER), one context otherwise
     */
    @NotNull
    public Collection<CallResolutionContext<D, F>> createCallContexts(@NotNull ResolutionCandidate<D> candidate,
            @NotNull ResolutionTask<D, F> task,
            @NotNull TemporaryBindingTrace temporaryTrace) {

        ResolvedCallImpl<D> candidateCall = ResolvedCallImpl.create(candidate, temporaryTrace);
        return Collections.singleton(CallResolutionContext.create(candidateCall, task, temporaryTrace, task.tracing));
    }

    /**
     * Returns collection of resolved calls for 'invoke' for 'variable as function' case (in FUNCTION_CALL_TRANSFORMER),
     * the resolved call from callResolutionContext otherwise
     */
    @NotNull
    public Collection<ResolvedCallWithTrace<F>> transformCall(@NotNull CallResolutionContext<D, F> callResolutionContext,
            @NotNull CallResolver callResolver,
            @NotNull ResolutionTask<D, F> task) {

        return Collections.singleton((ResolvedCallWithTrace<F>)callResolutionContext.candidateCall);
    }


    public static CallTransformer<VariableDescriptor, VariableDescriptor> PROPERTY_CALL_TRANSFORMER = new CallTransformer<VariableDescriptor, VariableDescriptor>();

    public static CallTransformer<CallableDescriptor, FunctionDescriptor> FUNCTION_CALL_TRANSFORMER = new CallTransformer<CallableDescriptor, FunctionDescriptor>() {
        @NotNull
        @Override
        public Collection<CallResolutionContext<CallableDescriptor, FunctionDescriptor>> createCallContexts(@NotNull ResolutionCandidate<CallableDescriptor> candidate,
                @NotNull ResolutionTask<CallableDescriptor, FunctionDescriptor> task, @NotNull TemporaryBindingTrace temporaryTrace) {

            if (candidate.getDescriptor() instanceof FunctionDescriptor) {
                return super.createCallContexts(candidate, task, temporaryTrace);
            }

            assert candidate.getDescriptor() instanceof VariableDescriptor;

            boolean hasReceiver = candidate.getReceiverArgument().exists();
            Call variableCall = stripCallArguments(task);
            if (!hasReceiver) {
                CallResolutionContext<CallableDescriptor, FunctionDescriptor> context = CallResolutionContext.create(
                        ResolvedCallImpl.create(candidate, temporaryTrace), task, temporaryTrace, task.tracing, variableCall);
                return Collections.singleton(context);
            }
            Call variableCallWithoutReceiver = stripReceiver(variableCall);
            CallResolutionContext<CallableDescriptor, FunctionDescriptor> contextWithReceiver = createContextWithChainedTrace(
                    candidate, variableCall, temporaryTrace, task);

            ResolutionCandidate<CallableDescriptor> candidateWithoutReceiver = ResolutionCandidate.create(
                    candidate.getDescriptor(), candidate.getThisObject(), ReceiverDescriptor.NO_RECEIVER, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER);

            CallResolutionContext<CallableDescriptor, FunctionDescriptor> contextWithoutReceiver = createContextWithChainedTrace(
                    candidateWithoutReceiver, variableCallWithoutReceiver, temporaryTrace, task);

            contextWithoutReceiver.receiverForVariableAsFunctionSecondCall = variableCall.getExplicitReceiver();

            return Lists.newArrayList(contextWithReceiver, contextWithoutReceiver);
        }

        private CallResolutionContext<CallableDescriptor, FunctionDescriptor> createContextWithChainedTrace(ResolutionCandidate<CallableDescriptor> candidate,
                Call call, TemporaryBindingTrace temporaryTrace, ResolutionTask<CallableDescriptor, FunctionDescriptor> task) {

            ChainedTemporaryBindingTrace chainedTrace = ChainedTemporaryBindingTrace.create(temporaryTrace);
            ResolvedCallImpl<CallableDescriptor> resolvedCall = ResolvedCallImpl.create(candidate, chainedTrace);
            return CallResolutionContext.create(resolvedCall, task, chainedTrace, task.tracing, call);
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
                public ReceiverDescriptor getExplicitReceiver() {
                    return ReceiverDescriptor.NO_RECEIVER;
                }
            };
        }

        @NotNull
        @Override
        public Collection<ResolvedCallWithTrace<FunctionDescriptor>> transformCall(@NotNull final CallResolutionContext<CallableDescriptor, FunctionDescriptor> context,
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

            final TemporaryBindingTrace variableCallTrace = context.candidateCall.getTrace();
            BasicResolutionContext basicResolutionContext = BasicResolutionContext.create(variableCallTrace, context.scope, functionCall, context.expectedType, context.dataFlowInfo);

            // 'invoke' call resolve
            OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveCallWithGivenName(basicResolutionContext, task.reference, "invoke");
            Collection<ResolvedCallWithTrace<FunctionDescriptor>> calls = ((OverloadResolutionResultsImpl<FunctionDescriptor>)results).getResultingCalls();

            return Collections2.transform(calls, new Function<ResolvedCallWithTrace<FunctionDescriptor>, ResolvedCallWithTrace<FunctionDescriptor>>() {
                @Override
                public ResolvedCallWithTrace<FunctionDescriptor> apply(ResolvedCallWithTrace<FunctionDescriptor> functionResolvedCall) {
                    return new VariableAsFunctionResolvedCall(functionResolvedCall, variableResolvedCall);
                }
            });
        }

        private Call createFunctionCall(final CallResolutionContext<CallableDescriptor, FunctionDescriptor> context,
                final ResolutionTask<CallableDescriptor, FunctionDescriptor> task, JetType returnType) {

            final ExpressionReceiver receiverFromVariable = new ExpressionReceiver(task.reference, returnType);
            final JetSimpleNameExpression invokeExpression = (JetSimpleNameExpression) JetPsiFactory.createExpression(
                    task.call.getCallElement().getProject(), "invoke");

            return new CallForImplicitInvoke(task.call) {
                @NotNull
                @Override
                public ReceiverDescriptor getExplicitReceiver() {
                    return context.receiverForVariableAsFunctionSecondCall;
                }

                @NotNull
                @Override
                public ReceiverDescriptor getThisObject() {
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
