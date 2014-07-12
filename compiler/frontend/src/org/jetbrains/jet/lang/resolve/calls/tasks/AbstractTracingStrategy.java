/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorWithVisibility;
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.inference.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.jetbrains.jet.lang.types.TypeUtils.noExpectedType;

public abstract class AbstractTracingStrategy implements TracingStrategy {
    protected final JetExpression reference;
    protected final Call call;

    protected AbstractTracingStrategy(@NotNull JetExpression reference, @NotNull Call call) {
        this.reference = reference;
        this.call = call;
    }

    @Override
    public <D extends CallableDescriptor> void recordAmbiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates) {
        Collection<D> descriptors = Sets.newHashSet();
        for (ResolvedCall<D> candidate : candidates) {
            descriptors.add(candidate.getCandidateDescriptor());
        }
        trace.record(AMBIGUOUS_REFERENCE_TARGET, reference, descriptors);
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
    public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors) {
        trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(reference, descriptors));
    }

    @Override
    public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors) {
        trace.report(NONE_APPLICABLE.on(reference, descriptors));
    }

    @Override
    public <D extends CallableDescriptor> void cannotCompleteResolve(
            @NotNull BindingTrace trace,
            @NotNull Collection<? extends ResolvedCall<D>> descriptors
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

                JetExpression left = binaryExpression.getLeft();
                JetExpression right = binaryExpression.getRight();
                if (left != null && right != null) {
                    trace.report(UNSAFE_INFIX_CALL.on(reference, left.getText(), operationString.asString(), right.getText()));
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
    public void danglingFunctionLiteralArgumentSuspected(@NotNull BindingTrace trace, @NotNull List<JetFunctionLiteralArgument> functionLiteralArguments) {
        for (JetFunctionLiteralArgument functionLiteralArgument : functionLiteralArguments) {
            trace.report(DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED.on(functionLiteralArgument.getArgumentExpression()));
        }
    }

    @Override
    public void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor) {
        trace.report(INVISIBLE_MEMBER.on(call.getCallElement(), descriptor, descriptor.getVisibility(), descriptor.getContainingDeclaration()));
    }

    @Override
    public void typeInferenceFailed(@NotNull BindingTrace trace, @NotNull InferenceErrorData data) {
        ConstraintSystem constraintSystem = data.constraintSystem;
        ConstraintSystemStatus status = constraintSystem.getStatus();
        assert !status.isSuccessful() : "Report error only for not successful constraint system";

        if (status.hasErrorInConstrainingTypes()) {
            // Do not report type inference errors if there is one in the arguments
            // (it's useful, when the arguments, e.g. lambdas or calls are incomplete)
            return;
        }
        if (status.hasOnlyErrorsFromPosition(ConstraintPosition.EXPECTED_TYPE_POSITION)) {
            JetType declaredReturnType = data.descriptor.getReturnType();
            if (declaredReturnType == null) return;

            ConstraintSystem systemWithoutExpectedTypeConstraint =
                    ((ConstraintSystemImpl) constraintSystem).filterConstraintsOut(ConstraintPosition.EXPECTED_TYPE_POSITION);
            JetType substitutedReturnType = systemWithoutExpectedTypeConstraint.getResultingSubstitutor().substitute(declaredReturnType, Variance.INVARIANT);
            assert substitutedReturnType != null; //todo

            assert !noExpectedType(data.expectedType) : "Expected type doesn't exist, but there is an expected type mismatch error";
            trace.report(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.on(reference, data.expectedType, substitutedReturnType));
        }
        else if (status.hasViolatedUpperBound()) {
            trace.report(TYPE_INFERENCE_UPPER_BOUND_VIOLATED.on(reference, data));
        }
        else if (status.hasTypeConstructorMismatch()) {
            trace.report(TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH.on(reference, data));
        }
        else if (status.hasConflictingConstraints()) {
            trace.report(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS.on(reference, data));
        }
        else {
            assert status.hasUnknownParameters();
            trace.report(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(reference, data));
        }
    }

}
