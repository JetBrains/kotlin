/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.tasks;

import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.DiagnosticUtilsKt;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemStatus;
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.Variance;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.getFqNameFromTopLevelClass;
import static org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemUtilsKt.filterConstraintsOut;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION;
import static org.jetbrains.kotlin.types.TypeUtils.noExpectedType;

public abstract class AbstractTracingStrategy implements TracingStrategy {
    protected final KtExpression reference;
    protected final Call call;

    protected AbstractTracingStrategy(@NotNull KtExpression reference, @NotNull Call call) {
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
        KtElement reportOn = CallUtilKt.getValueArgumentListOrElement(call);
        trace.report(NO_VALUE_FOR_PARAMETER.on(reportOn, valueParameter));
    }

    @Override
    public void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor expectedReceiver) {
        trace.report(MISSING_RECEIVER.on(reference, expectedReceiver.getType()));
    }

    @Override
    public void wrongReceiverType(
            @NotNull BindingTrace trace,
            @NotNull ReceiverParameterDescriptor receiverParameter,
            @NotNull ReceiverValue receiverArgument,
            @NotNull ResolutionContext<?> c
    ) {
        KtExpression reportOn = receiverArgument instanceof ExpressionReceiver
                                ? ((ExpressionReceiver) receiverArgument).getExpression()
                                : reference;

        if (!DiagnosticUtilsKt.reportTypeMismatchDueToTypeProjection(
                c, reportOn, receiverParameter.getType(), receiverArgument.getType())) {
            trace.report(TYPE_MISMATCH.on(reportOn, receiverParameter.getType(), receiverArgument.getType()));
        }
    }

    @Override
    public void noReceiverAllowed(@NotNull BindingTrace trace) {
        trace.report(NO_RECEIVER_ALLOWED.on(reference));
    }

    @Override
    public void wrongNumberOfTypeArguments(
            @NotNull BindingTrace trace, int expectedTypeArgumentCount, @NotNull CallableDescriptor descriptor
    ) {
        KtTypeArgumentList typeArgumentList = call.getTypeArgumentList();
        trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(
                typeArgumentList != null ? typeArgumentList : reference, expectedTypeArgumentCount, descriptor
        ));
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
    public void abstractSuperCall(@NotNull BindingTrace trace) {
        trace.report(ABSTRACT_SUPER_CALL.on(reference));
    }

    @Override
    public void nestedClassAccessViaInstanceReference(
            @NotNull BindingTrace trace,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull ExplicitReceiverKind explicitReceiverKind
    ) {
        if (explicitReceiverKind == ExplicitReceiverKind.NO_EXPLICIT_RECEIVER) {
            DeclarationDescriptor importableDescriptor = DescriptorUtilsKt.getImportableDescriptor(classDescriptor);
            if (DescriptorUtils.getFqName(importableDescriptor).isSafe()) {
                FqName fqName = getFqNameFromTopLevelClass(importableDescriptor);
                String qualifiedName;
                if (reference.getParent() instanceof KtCallableReferenceExpression) {
                    qualifiedName = fqName.parent() + "::" + classDescriptor.getName();
                }
                else {
                    qualifiedName = fqName.asString();
                }
                trace.report(NESTED_CLASS_SHOULD_BE_QUALIFIED.on(reference, classDescriptor, qualifiedName));
                return;
            }
        }
        trace.report(NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE.on(reference, classDescriptor));
    }

    @Override
    public void unsafeCall(@NotNull BindingTrace trace, @NotNull KotlinType type, boolean isCallForImplicitInvoke) {
        ASTNode callOperationNode = call.getCallOperationNode();
        if (callOperationNode != null && !isCallForImplicitInvoke) {
            trace.report(UNSAFE_CALL.on(callOperationNode.getPsi(), type));
        }
        else {
            PsiElement callElement = call.getCallElement();
            if (callElement instanceof KtBinaryExpression) {
                KtBinaryExpression binaryExpression = (KtBinaryExpression)callElement;
                KtSimpleNameExpression operationReference = binaryExpression.getOperationReference();

                Name operationString = operationReference.getReferencedNameElementType() == KtTokens.IDENTIFIER ?
                                       Name.identifier(operationReference.getText()) :
                                       OperatorConventions.getNameForOperationSymbol((KtToken) operationReference.getReferencedNameElementType());

                KtExpression left = binaryExpression.getLeft();
                KtExpression right = binaryExpression.getRight();
                if (left != null && right != null) {
                    trace.report(UNSAFE_INFIX_CALL.on(reference, left.getText(), operationString.asString(), right.getText()));
                }
            }
            else if (isCallForImplicitInvoke) {
                trace.report(UNSAFE_IMPLICIT_INVOKE_CALL.on(reference, type));
            }
            else {
                trace.report(UNSAFE_CALL.on(reference, type));
            }
        }
    }

    @Override
    public void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor) {
        trace.report(INVISIBLE_MEMBER.on(call.getCallElement(), descriptor, descriptor.getVisibility(), descriptor));
    }

    @Override
    public void typeInferenceFailed(@NotNull ResolutionContext<?> context, @NotNull InferenceErrorData data) {
        ConstraintSystem constraintSystem = data.constraintSystem;
        ConstraintSystemStatus status = constraintSystem.getStatus();
        assert !status.isSuccessful() : "Report error only for not successful constraint system";

        if (status.hasErrorInConstrainingTypes()) {
            // Do not report type inference errors if there is one in the arguments
            // (it's useful, when the arguments, e.g. lambdas or calls are incomplete)
            return;
        }
        BindingTrace trace = context.trace;
        if (status.hasOnlyErrorsDerivedFrom(EXPECTED_TYPE_POSITION)) {
            KotlinType declaredReturnType = data.descriptor.getReturnType();
            if (declaredReturnType == null) return;

            ConstraintSystem systemWithoutExpectedTypeConstraint = filterConstraintsOut(constraintSystem, EXPECTED_TYPE_POSITION);
            KotlinType substitutedReturnType = systemWithoutExpectedTypeConstraint.getResultingSubstitutor().substitute(
                    declaredReturnType, Variance.OUT_VARIANCE);
            assert substitutedReturnType != null; //todo

            assert !noExpectedType(data.expectedType) : "Expected type doesn't exist, but there is an expected type mismatch error";
            if (!DiagnosticUtilsKt.reportTypeMismatchDueToTypeProjection(
                    context, call.getCallElement(), data.expectedType, substitutedReturnType)) {
                trace.report(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH.on(call.getCallElement(), data.expectedType, substitutedReturnType));
            }
        }
        else if (status.hasCannotCaptureTypesError()) {
            trace.report(TYPE_INFERENCE_CANNOT_CAPTURE_TYPES.on(reference, data));
        }
        else if (status.hasViolatedUpperBound()) {
            trace.report(TYPE_INFERENCE_UPPER_BOUND_VIOLATED.on(reference, data));
        }
        else if (status.hasParameterConstraintError()) {
            trace.report(TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR.on(reference, data));
        }
        else if (status.hasConflictingConstraints()) {
            trace.report(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS.on(reference, data));
        }
        else if (status.hasTypeInferenceIncorporationError()) {
            trace.report(TYPE_INFERENCE_INCORPORATION_ERROR.on(reference));
        }
        else if (status.hasTypeParameterWithUnsatisfiedOnlyInputTypesError()) {
            //todo
            trace.report(TYPE_INFERENCE_ONLY_INPUT_TYPES.on(reference, data.descriptor.getTypeParameters().get(0)));
        }
        else {
            assert status.hasUnknownParameters();
            trace.report(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(reference, data));
        }
    }
}
