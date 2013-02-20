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

import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;

public class TracingStrategyImpl implements TracingStrategy {
    private final JetReferenceExpression reference;
    private final Call call;

    private TracingStrategyImpl(@NotNull JetReferenceExpression reference, @NotNull Call call) {
        this.reference = reference;
        this.call = call;
    }

    @NotNull
    public static TracingStrategy create(@NotNull JetReferenceExpression reference, @NotNull Call call) {
        return new TracingStrategyImpl(reference, call);
    }

    @Override
    public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall) {
        CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();
        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            descriptor = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall().getResultingDescriptor();
        }
        DeclarationDescriptor storedReference = trace.get(REFERENCE_TARGET, reference);
        if (storedReference == null || !ErrorUtils.isError(descriptor)) {
            trace.record(REFERENCE_TARGET, reference, descriptor);
        }
    }

    @Override
    public <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall) {
        trace.record(RESOLVED_CALL, call.getCalleeExpression(), resolvedCall);
        trace.record(CALL, call.getCalleeExpression(), call);
    }

    @Override
    public <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCallWithTrace<D>> candidates) {
        Collection<D> descriptors = Sets.newHashSet();
        for (ResolvedCallWithTrace<D> candidate : candidates) {
            descriptors.add(candidate.getCandidateDescriptor());
        }
        trace.record(AMBIGUOUS_REFERENCE_TARGET, reference, descriptors);
    }

    @Override
    public void unresolvedReference(@NotNull BindingTrace trace) {
        trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
    }

    @Override
    public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {
        JetElement reportOn;
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
    public void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor expectedReceiver) {
        trace.report(MISSING_RECEIVER.on(reference, expectedReceiver.getType()));
    }

    @Override
    public void wrongReceiverType(@NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor receiverParameter, @NotNull ReceiverValue receiverArgument) {
        if (receiverArgument instanceof ExpressionReceiver) {
            ExpressionReceiver expressionReceiver = (ExpressionReceiver)receiverArgument;
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
    public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors) {
        trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(call.getCallElement(), descriptors));
    }

    @Override
    public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors) {
        trace.report(NONE_APPLICABLE.on(reference, descriptors));
    }

    @Override
    public <D extends CallableDescriptor> void cannotCompleteResolve(
            @NotNull BindingTrace trace,
            @NotNull Collection<ResolvedCallWithTrace<D>> descriptors
    ) {
        trace.report(CANNOT_COMPLETE_RESOLVE.on(reference, descriptors));
    }

    @Override
    public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {
        trace.report(CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS.on(call.getCallElement()));
    }

    @Override
    public void unsafeCall(@NotNull BindingTrace trace, @NotNull JetType type, boolean isCallForImplicitInvoke) {
        ASTNode callOperationNode = call.getCallOperationNode();
        if (callOperationNode != null && !isCallForImplicitInvoke) {
            trace.report(UNSAFE_CALL.on(callOperationNode.getPsi(), type));
        }
        else {
            PsiElement callElement = call.getCallElement();
            if (callElement instanceof JetBinaryExpression) {
                JetBinaryExpression binaryExpression = (JetBinaryExpression)callElement;
                JetSimpleNameExpression operationReference = binaryExpression.getOperationReference();

                Name operationString = operationReference.getReferencedNameElementType() == JetTokens.IDENTIFIER ?
                        Name.identifier(operationReference.getText()) :
                        OperatorConventions.getNameForOperationSymbol((JetToken) operationReference.getReferencedNameElementType());

                JetExpression right = binaryExpression.getRight();
                if (right != null) {
                    trace.report(UNSAFE_INFIX_CALL.on(reference, binaryExpression.getLeft().getText(), operationString.getName(), right.getText()));
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

    @Override
    public void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor) {
        trace.report(INVISIBLE_MEMBER.on(call.getCallElement(), descriptor, descriptor.getVisibility(), descriptor.getContainingDeclaration()));
    }

    @Override
    public void typeInferenceFailed(@NotNull BindingTrace trace, @NotNull InferenceErrorData.ExtendedInferenceErrorData data, @NotNull ConstraintSystem systemWithoutExpectedTypeConstraint) {
        ConstraintSystem constraintSystem = data.constraintSystem;
        assert !constraintSystem.isSuccessful();
        if (constraintSystem.hasErrorInConstrainingTypes()) {
            return;
        }
        boolean successfulWithoutExpectedTypeConstraint = systemWithoutExpectedTypeConstraint.isSuccessful();
        if (constraintSystem.hasExpectedTypeMismatch() || successfulWithoutExpectedTypeConstraint) {
            JetType returnType = data.descriptor.getReturnType();
            assert returnType != null;
            if (successfulWithoutExpectedTypeConstraint) {
                returnType = systemWithoutExpectedTypeConstraint.getResultingSubstitutor().substitute(returnType, Variance.INVARIANT);
                assert returnType != null;
            }
            assert data.expectedType != null;
            trace.report(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.on(reference, returnType, data.expectedType));
        }
        else if (constraintSystem.hasTypeConstructorMismatch()) {
            trace.report(TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH.on(reference, data));
        }
        else if (constraintSystem.hasConflictingConstraints()) {
            trace.report(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS.on(reference, data));
        }
        else {
            assert constraintSystem.hasUnknownParameters();
            trace.report(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(reference, data));
        }
    }

    @Override
    public void upperBoundViolated(@NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData) {
        trace.report(Errors.TYPE_INFERENCE_UPPER_BOUND_VIOLATED.on(reference, inferenceErrorData));
    }
}
