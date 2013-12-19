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
import com.google.common.collect.Maps;
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
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemStatus;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintsUtil;
import org.jetbrains.jet.lang.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.jet.lang.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.tasks.ResolutionCandidate;
import org.jetbrains.jet.lang.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.CALL;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLVED_CALL;

public class ControlStructureTypingUtils {
    private ControlStructureTypingUtils() {
    }

    /*package*/ static ResolvedCall<FunctionDescriptor> resolveSpecialConstructionAsCall(
            @NotNull Call call,
            @NotNull String constructionName,
            @NotNull List<String> argumentNames,
            @NotNull List<Boolean> isArgumentNullable,
            @NotNull ExpressionTypingContext context,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        SimpleFunctionDescriptorImpl function = createFunctionDescriptorForSpecialConstruction(
                constructionName.toUpperCase(), argumentNames, isArgumentNullable);
        JetReferenceExpression reference = JetPsiFactory.createSimpleName(
                context.expressionTypingServices.getProject(), "fake" + constructionName + "Call");
        TracingStrategy tracing = createTracingForSpecialConstruction(call, constructionName);
        ResolutionCandidate<CallableDescriptor> resolutionCandidate = ResolutionCandidate.<CallableDescriptor>create(function, null);
        CallResolver callResolver = context.expressionTypingServices.getCallResolver();
        OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveCallWithKnownCandidate(
                call, tracing, reference, context, resolutionCandidate, dataFlowInfoForArguments);
        assert results.isSingleResult() : "Not single result after resolving one known candidate";
        return results.getResultingCall();
    }

    private static SimpleFunctionDescriptorImpl createFunctionDescriptorForSpecialConstruction(
            @NotNull String constructionName,
            @NotNull List<String> argumentNames,
            @NotNull List<Boolean> isArgumentNullable
    ) {
        assert argumentNames.size() == isArgumentNullable.size();

        List<AnnotationDescriptor> noAnnotations = Collections.emptyList();
        Name specialFunctionName = Name.identifierNoValidate("<SPECIAL-FUNCTION-FOR-" + constructionName + "-RESOLVE>");

        SimpleFunctionDescriptorImpl function = new SimpleFunctionDescriptorImpl(
                ErrorUtils.getErrorModule(),//todo hack to avoid returning true in 'isError(DeclarationDescriptor)'
                noAnnotations, specialFunctionName, CallableMemberDescriptor.Kind.DECLARATION);

        TypeParameterDescriptor typeParameter = TypeParameterDescriptorImpl.createWithDefaultBound(
                function, noAnnotations, false, Variance.INVARIANT,
                Name.identifierNoValidate("<TYPE-PARAMETER-FOR-" + constructionName + "-RESOLVE>"), 0);

        JetType type = new JetTypeImpl(typeParameter.getTypeConstructor(), JetScope.EMPTY);
        JetType nullableType = new JetTypeImpl(
                noAnnotations, typeParameter.getTypeConstructor(), true, Collections.<TypeProjection>emptyList(), JetScope.EMPTY);

        List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();
        for (int i = 0; i < argumentNames.size(); i++) {
            JetType argumentType = isArgumentNullable.get(i) ? nullableType : type;
            ValueParameterDescriptorImpl valueParameter = new ValueParameterDescriptorImpl(
                    function, i, noAnnotations, Name.identifier(argumentNames.get(i)), argumentType, false, null);
            valueParameters.add(valueParameter);
        }
        function.initialize(
                null,
                ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER,
                Lists.newArrayList(typeParameter),
                valueParameters,
                type,
                Modality.FINAL,
                Visibilities.PUBLIC
        );
        return function;
    }

    /*package*/ static MutableDataFlowInfoForArguments createIndependentDataFlowInfoForArgumentsForCall(
            final Map<ValueArgument, DataFlowInfo> dataFlowInfoForArgumentsMap
    ) {
        return new MutableDataFlowInfoForArguments() {
            private DataFlowInfo initialDataFlowInfo;

            @Override
            public void setInitialDataFlowInfo(@NotNull DataFlowInfo dataFlowInfo) {
                this.initialDataFlowInfo = dataFlowInfo;
            }

            @Override
            public void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo) {
                //todo
            }

            @NotNull
            @Override
            public DataFlowInfo getInfo(@NotNull ValueArgument valueArgument) {
                return dataFlowInfoForArgumentsMap.get(valueArgument);
            }

            @NotNull
            @Override
            public DataFlowInfo getResultInfo() {
                //todo merge and use
                return initialDataFlowInfo;
            }
        };
    }

    public static MutableDataFlowInfoForArguments createDataFlowInfoForArgumentsForIfCall(
            @NotNull Call callForIf,
            @NotNull DataFlowInfo thenInfo,
            @NotNull DataFlowInfo elseInfo
    ) {
        Map<ValueArgument, DataFlowInfo> dataFlowInfoForArgumentsMap = Maps.newHashMap();
        dataFlowInfoForArgumentsMap.put(callForIf.getValueArguments().get(0), thenInfo);
        dataFlowInfoForArgumentsMap.put(callForIf.getValueArguments().get(1), elseInfo);
        return createIndependentDataFlowInfoForArgumentsForCall(dataFlowInfoForArgumentsMap);
    }

    /*package*/ static Call createCallForSpecialConstruction(
            @NotNull final JetExpression expression,
            @NotNull List<? extends JetExpression> arguments
    ) {
        final List<ValueArgument> valueArguments = Lists.newArrayList();
        for (JetExpression argument : arguments) {
            valueArguments.add(CallMaker.makeValueArgument(argument, argument));
        }
        return new Call() {
            @Nullable
            @Override
            public ASTNode getCallOperationNode() {
                return expression.getNode();
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
                return expression;
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
                return expression;
            }

            @NotNull
            @Override
            public CallType getCallType() {
                return CallType.DEFAULT;
            }
        };
    }

    /*package*/ static TracingStrategy createTracingForSpecialConstruction(
            final @NotNull Call call,
            final @NotNull String constructionName
    ) {
        class CheckTypeContext {
            public BindingTrace trace;
            public JetType expectedType;

            CheckTypeContext(@NotNull BindingTrace trace, @NotNull JetType expectedType) {
                this.trace = trace;
                this.expectedType = expectedType;
            }

            CheckTypeContext makeTypeNullable() {
                if (TypeUtils.noExpectedType(expectedType)) return this;
                return new CheckTypeContext(trace, TypeUtils.makeNullable(expectedType));
            }
        }

        final JetVisitor<Void, CheckTypeContext> checkTypeVisitor = new JetVisitor<Void, CheckTypeContext>() {
            private void checkExpressionType(@Nullable JetExpression expression, CheckTypeContext c) {
                if (expression == null) return;
                expression.accept(this, c);
            }

            @Override
            public Void visitIfExpression(@NotNull JetIfExpression ifExpression, CheckTypeContext c) {
                JetExpression thenBranch = ifExpression.getThen();
                JetExpression elseBranch = ifExpression.getElse();
                if (thenBranch == null || elseBranch == null) {
                    visitExpression(ifExpression, c);
                    return null;
                }
                checkExpressionType(thenBranch, c);
                checkExpressionType(elseBranch, c);
                return null;
            }

            @Override
            public Void visitBlockExpression(@NotNull JetBlockExpression expression, CheckTypeContext c) {
                if (expression.getStatements().isEmpty()) {
                    visitExpression(expression, c);
                    return null;
                }
                JetElement lastStatement = JetPsiUtil.getLastStatementInABlock(expression);
                if (lastStatement instanceof JetExpression) {
                    checkExpressionType((JetExpression) lastStatement, c);
                }
                return null;
            }

            @Override
            public Void visitPostfixExpression(@NotNull JetPostfixExpression expression, CheckTypeContext c) {
                if (expression.getOperationReference().getReferencedNameElementType() == JetTokens.EXCLEXCL) {
                    checkExpressionType(expression.getBaseExpression(), c.makeTypeNullable());
                    return null;
                }
                return super.visitPostfixExpression(expression, c);
            }

            @Override
            public Void visitBinaryExpression(@NotNull JetBinaryExpression expression, CheckTypeContext c) {
                if (expression.getOperationReference().getReferencedNameElementType() == JetTokens.ELVIS) {
                    checkExpressionType(expression.getLeft(), c.makeTypeNullable());
                    checkExpressionType(expression.getRight(), c);
                    return null;
                }
                return super.visitBinaryExpression(expression, c);
            }

            @Override
            public Void visitExpression(@NotNull JetExpression expression, CheckTypeContext c) {
                JetTypeInfo typeInfo = BindingContextUtils.getRecordedTypeInfo(expression, c.trace.getBindingContext());
                if (typeInfo != null) {
                    DataFlowUtils.checkType(typeInfo.getType(), expression, c.expectedType, typeInfo.getDataFlowInfo(), c.trace);
                }
                return null;
            }
        };

        return new ThrowingOnErrorTracingStrategy("resolve " + constructionName + " as a call") {
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
                trace.record(RESOLVED_CALL, call.getCalleeExpression(), resolvedCall);
                trace.record(CALL, call.getCalleeExpression(), call);

            }

            @Override
            public void typeInferenceFailed(
                    @NotNull BindingTrace trace, @NotNull InferenceErrorData data
            ) {
                ConstraintSystem constraintSystem = data.constraintSystem;
                ConstraintSystemStatus status = constraintSystem.getStatus();
                assert !status.isSuccessful() : "Report error only for not successful constraint system";

                if (status.hasErrorInConstrainingTypes()) {
                    return;
                }
                JetExpression expression = call.getCalleeExpression();
                if (expression == null) return;
                if (status.hasOnlyErrorsFromPosition(ConstraintPosition.EXPECTED_TYPE_POSITION) || status.hasConflictingConstraints()) {
                    expression.accept(checkTypeVisitor, new CheckTypeContext(trace, data.expectedType));
                    return;
                }
                throwError("Expression: " + expression.getText() + ".\nConstraint system status: \n" + ConstraintsUtil.getDebugMessageForStatus(status));
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
            throwError(null);
        }

        protected void throwError(@Nullable String additionalInformation) {
            String errorMessage = "Resolution error of this type shouldn't occur for " + debugName;
            if (additionalInformation != null) {
                errorMessage += ".\n" + additionalInformation;
            }
            throw new IllegalStateException(errorMessage);
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
                @NotNull BindingTrace trace, @NotNull InferenceErrorData inferenceErrorData
        ) {
            throwError();
        }
    }
}
