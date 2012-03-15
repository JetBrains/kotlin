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

import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.inference.SolutionStatus;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.diagnostics.Errors.DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED;
import static org.jetbrains.jet.lang.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLVED_CALL;

/**
 * Stores candidates for call resolution.
 *
 * @author abreslav
 */
public class ResolutionTask<D extends CallableDescriptor> extends ResolutionContext {
    private final Collection<ResolvedCallImpl<D>> candidates;
    /*package*/ final JetReferenceExpression reference;
    private DescriptorCheckStrategy checkingStrategy;

    public ResolutionTask(@NotNull Collection<ResolvedCallImpl<D>> candidates, @NotNull JetReferenceExpression reference,
                          BindingTrace trace, JetScope scope, Call call, JetType expectedType, DataFlowInfo dataFlowInfo) {
        super(trace, scope, call, expectedType, dataFlowInfo);
        this.candidates = candidates;
        this.reference = reference;
    }

    public ResolutionTask(@NotNull Collection<ResolvedCallImpl<D>> candidates, @NotNull JetReferenceExpression reference, @NotNull BasicResolutionContext context) {
        this(candidates, reference, context.trace, context.scope, context.call, context.expectedType, context.dataFlowInfo);
    }

    @NotNull
    public Collection<ResolvedCallImpl<D>> getCandidates() {
        return candidates;
    }

    public void setCheckingStrategy(DescriptorCheckStrategy strategy) {
        checkingStrategy = strategy;
    }

    public boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing) {
        if (checkingStrategy != null && !checkingStrategy.performAdvancedChecks(descriptor, trace, tracing)) {
            return false;
        }
        return true;
    }

    public ResolutionTask<D> withTrace(BindingTrace newTrace) {
        ResolutionTask<D> newTask = new ResolutionTask<D>(candidates, reference, newTrace, scope, call, expectedType, dataFlowInfo);
        newTask.setCheckingStrategy(checkingStrategy);
        return newTask;
    }

    public interface DescriptorCheckStrategy {
        <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing);
    }
    
    public final TracingStrategy tracing = new TracingStrategy() {
                @Override
                public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCallImpl<D> resolvedCall) {
                    D descriptor = resolvedCall.getCandidateDescriptor();
    //                if (descriptor instanceof VariableAsFunctionDescriptor) {
    //                    VariableAsFunctionDescriptor variableAsFunctionDescriptor = (VariableAsFunctionDescriptor) descriptor;
    //                    trace.record(REFERENCE_TARGET, reference, variableAsFunctionDescriptor.getVariableDescriptor());
    //                }
    //                else {
    //                }
                    trace.record(RESOLVED_CALL, call.getCalleeExpression(), resolvedCall);
                    trace.record(REFERENCE_TARGET, reference, descriptor);
                }
    
                @Override
                public <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCallImpl<D>> candidates) {
                    Collection<D> descriptors = Sets.newHashSet();
                    for (ResolvedCallImpl<D> candidate : candidates) {
                        descriptors.add(candidate.getCandidateDescriptor());
                    }
                    trace.record(AMBIGUOUS_REFERENCE_TARGET, reference, descriptors);
                }
    
                @Override
                public void unresolvedReference(@NotNull BindingTrace trace) {
                    trace.report(UNRESOLVED_REFERENCE.on(reference));
                }
    
                @Override
                public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {
                    PsiElement reportOn;
                    JetValueArgumentList valueArgumentList = call.getValueArgumentList();
                    if (valueArgumentList != null) {
                        reportOn = valueArgumentList;
                    }
                    else {
                        reportOn = reference;
                    }
                    trace.report(NO_VALUE_FOR_PARAMETER.on(reportOn, valueParameter));
                }
    
                @Override
                public void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor expectedReceiver) {
                    trace.report(MISSING_RECEIVER.on(reference, expectedReceiver.getType()));
                }
    
                @Override
                public void wrongReceiverType(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor receiverParameter, @NotNull ReceiverDescriptor receiverArgument) {
                    if (receiverArgument instanceof ExpressionReceiver) {
                        ExpressionReceiver expressionReceiver = (ExpressionReceiver) receiverArgument;
                        trace.report(TYPE_MISMATCH.on(expressionReceiver.getExpression(), receiverParameter.getType(), receiverArgument.getType()));
                    }
                    else {
                        trace.report(TYPE_MISMATCH.on(reference, receiverParameter.getType(), receiverArgument.getType()));
                    }
                }
    
                @Override
                public void noReceiverAllowed(@NotNull BindingTrace trace) {
                    trace.report(NO_RECEIVER_ADMITTED.on(reference));
                }
    
                @Override
                public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount) {
                    JetTypeArgumentList typeArgumentList = call.getTypeArgumentList();
                    if (typeArgumentList != null) {
                        trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(typeArgumentList, expectedTypeArgumentCount));
                    }
                    else {
                        trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(reference, expectedTypeArgumentCount));
                    }
                }
    
                @Override
                public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallImpl<D>> descriptors) {
                    trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(call.getCallElement(), descriptors));
                }
    
                @Override
                public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallImpl<D>> descriptors) {
                    trace.report(NONE_APPLICABLE.on(reference, descriptors));
                }
    
                @Override
                public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {
                    trace.report(CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS.on(call.getCallElement()));
                }
    
                @Override
                public void typeInferenceFailed(@NotNull BindingTrace trace, SolutionStatus status) {
                    assert !status.isSuccessful();
                    trace.report(TYPE_INFERENCE_FAILED.on(call.getCallElement(), status));
                }
    
                @Override
                public void unsafeCall(@NotNull BindingTrace trace, @NotNull JetType type) {
                    ASTNode callOperationNode = call.getCallOperationNode();
                    if (callOperationNode != null) {
                        trace.report(UNSAFE_CALL.on(callOperationNode.getPsi(), type));
                    }
                    else {
                        PsiElement callElement = call.getCallElement();
                        if (callElement instanceof JetBinaryExpression) {
                            JetBinaryExpression binaryExpression = (JetBinaryExpression) callElement;
                            JetSimpleNameExpression operationReference = binaryExpression.getOperationReference();
    
                            String operationString = operationReference.getReferencedNameElementType() == JetTokens.IDENTIFIER ?
                                    operationReference.getText() :
                                    OperatorConventions.getNameForOperationSymbol((JetToken) operationReference.getReferencedNameElementType());
    
                            JetExpression right = binaryExpression.getRight();
                            if (right != null) {
                                trace.report(UNSAFE_INFIX_CALL.on(reference, binaryExpression.getLeft().getText(), operationString, right.getText()));
                            }
                        }
                        else {
                            trace.report(UNSAFE_CALL.on(reference, type));
                        }
                    }
                }
    
                @Override
                public void unnecessarySafeCall(@NotNull BindingTrace trace, @NotNull JetType type) {
                    ASTNode callOperationNode = call.getCallOperationNode();
                    assert callOperationNode != null;
                    trace.report(UNNECESSARY_SAFE_CALL.on(callOperationNode.getPsi(), type));
                }
    
                @Override
                public void danglingFunctionLiteralArgumentSuspected(@NotNull BindingTrace trace, @NotNull List<JetExpression> functionLiteralArguments) {
                    for (JetExpression functionLiteralArgument : functionLiteralArguments) {
                        trace.report(DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED.on(functionLiteralArgument));
                    }
                }
            };
}
