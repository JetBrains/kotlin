/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.util.CallUtilKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import java.util.Collection;
import java.util.HashSet;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;

public abstract class AbstractTracingStrategy implements TracingStrategy {
    protected final KtExpression reference;
    protected final Call call;

    protected AbstractTracingStrategy(@NotNull KtExpression reference, @NotNull Call call) {
        this.reference = reference;
        this.call = call;
    }

    @Override
    public <D extends CallableDescriptor> void recordAmbiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates) {
        Collection<D> descriptors = new HashSet<>();
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
    public void wrongNumberOfTypeArguments(
            @NotNull BindingTrace trace, int expectedTypeArgumentCount, @NotNull CallableDescriptor descriptor
    ) {
        KtTypeArgumentList typeArgumentList = call.getTypeArgumentList();
        trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(
                typeArgumentList != null ? typeArgumentList : reference, expectedTypeArgumentCount, descriptor
        ));
    }

    @Override
    public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> resolvedCalls) {
        trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(reference, resolvedCalls));
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
    public void recursiveType(@NotNull BindingTrace trace, boolean shouldReportErrorsOnRecursiveTypeInsidePlusAssignment) {
        KtExpression expression = call.getCalleeExpression();
        if (expression == null) return;
        DiagnosticFactory0<KtExpression> factory;
        if (shouldReportErrorsOnRecursiveTypeInsidePlusAssignment) {
            factory = TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.getErrorFactory();
        } else {
            factory = TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.getWarningFactory();
        }
        trace.report(factory.on(expression));
    }

    @Override
    public void abstractSuperCall(@NotNull BindingTrace trace) {
        trace.report(ABSTRACT_SUPER_CALL.on(reference));
    }

    @Override
    public void abstractSuperCallWarning(@NotNull BindingTrace trace) {
        trace.report(ABSTRACT_SUPER_CALL_WARNING.on(reference));
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
                reportUnsafeCallOnBinaryExpression(trace, (KtBinaryExpression) callElement);
            }
            else if (isCallForImplicitInvoke) {
                trace.report(UNSAFE_IMPLICIT_INVOKE_CALL.on(reference, type));
            }
            else {
                trace.report(UNSAFE_CALL.on(reference, type));
            }
        }
    }

    private void reportUnsafeCallOnBinaryExpression(@NotNull BindingTrace trace, @NotNull KtBinaryExpression binaryExpression) {
        KtSimpleNameExpression operationReference = binaryExpression.getOperationReference();
        boolean isInfixCall = operationReference.getReferencedNameElementType() == KtTokens.IDENTIFIER;
        Name operationString = isInfixCall ?
                               Name.identifier(operationReference.getText()) :
                               OperatorConventions.getNameForOperationSymbol((KtToken) operationReference.getReferencedNameElementType());

        if (operationString == null) return;

        KtExpression left = binaryExpression.getLeft();
        KtExpression right = binaryExpression.getRight();
        if (left == null || right == null) return;

        if (isInfixCall) {
            trace.report(UNSAFE_INFIX_CALL.on(reference, left, operationString.asString(), right));
        }
        else {
            boolean inOperation = KtPsiUtil.isInOrNotInOperation(binaryExpression);
            KtExpression receiver = inOperation ? right : left;
            KtExpression argument = inOperation ? left : right;
            trace.report(UNSAFE_OPERATOR_CALL.on(reference, receiver, operationString.asString(), argument));
        }
    }

    @Override
    public void invisibleMember(@NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor) {
        trace.report(INVISIBLE_MEMBER.on(call.getCallElement(), descriptor, descriptor.getVisibility(), descriptor));
    }
}
