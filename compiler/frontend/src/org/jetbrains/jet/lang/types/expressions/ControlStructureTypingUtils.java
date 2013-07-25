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

package org.jetbrains.jet.lang.types.expressions;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.jet.lang.resolve.calls.tasks.TracingStrategyImpl;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_MISMATCH;
import static org.jetbrains.jet.lang.resolve.BindingContext.CALL;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLVED_CALL;

public class ControlStructureTypingUtils {
    private ControlStructureTypingUtils() {
    }

    /*package*/ static ResolvedCall<FunctionDescriptor> resolveIfAsCall(
            @NotNull Call callForIf,
            @NotNull ExpressionTypingContext context
    ) {
        List<AnnotationDescriptor> noAnnotations = Collections.emptyList();
        Name specialFunctionName = Name.identifierNoValidate("<SPECIAL-FUNCTION-FOR-IF-RESOLVE>");

        SimpleFunctionDescriptorImpl ifFunction = new SimpleFunctionDescriptorImpl(
                ErrorUtils.getErrorModule(),//todo hack to avoid returning true in 'isError(DeclarationDescriptor)'
                noAnnotations, specialFunctionName, CallableMemberDescriptor.Kind.DECLARATION);

        TypeParameterDescriptor typeParameter = TypeParameterDescriptorImpl.createWithDefaultBound(
                ifFunction, noAnnotations, false, Variance.INVARIANT, Name.identifier("T"), 0);

        JetType type = new JetTypeImpl(typeParameter.getTypeConstructor(), JetScope.EMPTY);

        ValueParameterDescriptorImpl thenValueParameter = new ValueParameterDescriptorImpl(
                ifFunction, 0, noAnnotations, Name.identifier("thenBranch"), type, false, null);
        ValueParameterDescriptorImpl elseValueParameter = new ValueParameterDescriptorImpl(
                ifFunction, 1, noAnnotations, Name.identifier("elseBranch"), type, false, null);
        ifFunction.initialize(
                null,
                ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER,
                Lists.newArrayList(typeParameter),
                Lists.<ValueParameterDescriptor>newArrayList(thenValueParameter, elseValueParameter),
                type,
                Modality.FINAL,
                Visibilities.PUBLIC,
                /*isInline = */ false
        );

        JetReferenceExpression ifReference = JetPsiFactory.createSimpleName(context.expressionTypingServices.getProject(), "fakeIfCall");
        TracingStrategy tracingForIf = createTracingForIf(callForIf);
        OverloadResolutionResults<FunctionDescriptor>
                results = context.expressionTypingServices.getCallResolver().resolveCallWithKnownCandidate(
                callForIf, tracingForIf, ifReference, context, ifFunction);
        assert results.isSingleResult() : "Not single result after resolving one known candidate";
        return results.getResultingCall();
    }



    /*package*/ static Call createCallForIf(
            final JetIfExpression ifExpression,
            @NotNull JetExpression thenBranch,
            @NotNull JetExpression elseBranch
    ) {
        final List<ValueArgument> valueArguments = Lists.newArrayList(
                CallMaker.makeValueArgument(thenBranch, thenBranch),
                CallMaker.makeValueArgument(elseBranch, elseBranch));
        return new Call() {
            @Nullable
            @Override
            public ASTNode getCallOperationNode() {
                return ifExpression.getNode();
            }

            @NotNull
            @Override
            public ReceiverValue getExplicitReceiver() {
                return ReceiverValue.NO_RECEIVER;
            }

            @NotNull
            @Override
            public ReceiverValue getThisObject() {
                return ReceiverValue.NO_RECEIVER;
            }

            @Nullable
            @Override
            public JetExpression getCalleeExpression() {
                return ifExpression;
            }

            @Nullable
            @Override
            public JetValueArgumentList getValueArgumentList() {
                return null;
            }

            @NotNull
            @Override
            public List<? extends ValueArgument> getValueArguments() {
                return valueArguments;
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

            @Nullable
            @Override
            public JetTypeArgumentList getTypeArgumentList() {
                return null;
            }

            @NotNull
            @Override
            public PsiElement getCallElement() {
                return ifExpression;
            }

            @NotNull
            @Override
            public CallType getCallType() {
                return CallType.DEFAULT;
            }
        };
    }

    /*package*/ static TracingStrategy createTracingForIf(final @NotNull Call callForIf) {

        return new ThrowingOnErrorTracingStrategy("resolve 'if' as a call") {
            @Override
            public <D extends CallableDescriptor> void bindReference(
                    @NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall
            ) {
                //do nothing
            }

            @Override
            public <D extends CallableDescriptor> void bindResolvedCall(
                    @NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall
            ) {
                trace.record(RESOLVED_CALL, callForIf.getCalleeExpression(), resolvedCall);
                trace.record(CALL, callForIf.getCalleeExpression(), callForIf);

            }

            @Override
            public void typeInferenceFailed(
                    @NotNull BindingTrace trace, @NotNull InferenceErrorData.ExtendedInferenceErrorData data
            ) {
                ConstraintSystem constraintSystem = data.constraintSystem;
                assert !constraintSystem.isSuccessful() : "Report error only for not successful constraint system";

                if (constraintSystem.hasErrorInConstrainingTypes()) {
                    return;
                }
                if (constraintSystem.hasOnlyExpectedTypeMismatch()) {
                    JetExpression ifExpression = callForIf.getCalleeExpression();
                    assert ifExpression != null;
                    TracingStrategyImpl.reportTypeInferenceExpectedTypeMismatch(TYPE_MISMATCH, ifExpression, data, trace);
                    return;
                }
                super.typeInferenceFailed(trace, data);
            }
        };
    }
    
    private abstract static class ThrowingOnErrorTracingStrategy implements TracingStrategy {
        private final String debugName;

        protected ThrowingOnErrorTracingStrategy(String debugName) {
            this.debugName = debugName;
        }

        private void throwError() {
            throw new IllegalStateException("Resolution error of this type shouldn't occur for " + debugName);
        }

        @Override
        public void unresolvedReference(@NotNull BindingTrace trace) {
            throwError();
        }

        @Override
        public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(
                @NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> candidates
        ) {
            throwError();
        }

        @Override
        public <D extends CallableDescriptor> void recordAmbiguity(
                @NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> candidates
        ) {
            throwError();
        }

        @Override
        public void missingReceiver(
                @NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor expectedReceiver
        ) {
            throwError();
        }

        @Override
        public void wrongReceiverType(
                @NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor receiverParameter, @NotNull ReceiverValue receiverArgument
        ) {
            throwError();
        }

        @Override
        public void noReceiverAllowed(@NotNull BindingTrace trace) {
            throwError();
        }

        @Override
        public void noValueForParameter(
                @NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter
        ) {
            throwError();
        }

        @Override
        public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount) {
            throwError();
        }

        @Override
        public <D extends CallableDescriptor> void ambiguity(
                @NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors
        ) {
            throwError();
        }

        @Override
        public <D extends CallableDescriptor> void noneApplicable(
                @NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors
        ) {
            throwError();
        }

        @Override
        public <D extends CallableDescriptor> void cannotCompleteResolve(
                @NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> descriptors
        ) {
            throwError();
        }

        @Override
        public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {
            throwError();
        }

        @Override
        public void unsafeCall(
                @NotNull BindingTrace trace, @NotNull JetType type, boolean isCallForImplicitInvoke
        ) {
            throwError();
        }

        @Override
        public void unnecessarySafeCall(
                @NotNull BindingTrace trace, @NotNull JetType type
        ) {
            throwError();
        }

        @Override
        public void danglingFunctionLiteralArgumentSuspected(
                @NotNull BindingTrace trace, @NotNull List<JetExpression> functionLiteralArguments
        ) {
            throwError();
        }

        @Override
        public void invisibleMember(
                @NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor
        ) {
            throwError();
        }

        @Override
        public void typeInferenceFailed(
                @NotNull BindingTrace trace, @NotNull InferenceErrorData.ExtendedInferenceErrorData inferenceErrorData
        ) {
            throwError();
        }

        @Override
        public void upperBoundViolated(
                @NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData
        ) {
            throwError();
        }
    }
}
