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

package org.jetbrains.kotlin.types.expressions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemStatus;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintsUtil;
import org.jetbrains.kotlin.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.kotlin.resolve.calls.tasks.OldResolutionCandidate;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.descriptorUtil.AnnotationsForResolveUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.typeUtil.TypeUtilsKt;

import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.TYPE_INFERENCE_FAILED_ON_SPECIAL_CONSTRUCT;
import static org.jetbrains.kotlin.resolve.BindingContext.CALL;
import static org.jetbrains.kotlin.resolve.BindingContext.RESOLVED_CALL;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.EXPECTED_TYPE_POSITION;

public class ControlStructureTypingUtils {
    private static final Logger LOG = Logger.getInstance(ControlStructureTypingUtils.class);

    public enum ResolveConstruct {
        IF("if"), ELVIS("elvis"), EXCL_EXCL("ExclExcl"), WHEN("when"), TRY("try");

        private final String name;
        private final Name specialFunctionName;
        private final Name specialTypeParameterName;

        ResolveConstruct(String name) {
            this.name = name;
            this.specialFunctionName = Name.identifier("<SPECIAL-FUNCTION-FOR-" + name.toUpperCase() + "-RESOLVE>");
            this.specialTypeParameterName = Name.identifier("<TYPE-PARAMETER-FOR-" + name.toUpperCase() + "-RESOLVE>");
        }

        public String getName() {
            return name;
        }

        public Name getSpecialFunctionName() {
            return specialFunctionName;
        }

        public Name getSpecialTypeParameterName() {
            return specialTypeParameterName;
        }
    }

    private final CallResolver callResolver;
    private final DataFlowAnalyzer dataFlowAnalyzer;
    private final ModuleDescriptor moduleDescriptor;
    private final StorageManager storageManager;

    public ControlStructureTypingUtils(
            @NotNull CallResolver callResolver,
            @NotNull DataFlowAnalyzer dataFlowAnalyzer,
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull StorageManager storageManager
    ) {
        this.callResolver = callResolver;
        this.dataFlowAnalyzer = dataFlowAnalyzer;
        this.moduleDescriptor = moduleDescriptor;
        this.storageManager = storageManager;
    }

    /*package*/ ResolvedCall<FunctionDescriptor> resolveSpecialConstructionAsCall(
            @NotNull Call call,
            @NotNull ResolveConstruct construct,
            @NotNull List<String> argumentNames,
            @NotNull List<Boolean> isArgumentNullable,
            @NotNull ExpressionTypingContext context,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        SimpleFunctionDescriptorImpl function = createFunctionDescriptorForSpecialConstruction(
                construct, argumentNames, isArgumentNullable);
        return resolveSpecialConstructionAsCall(call, function, construct, context, dataFlowInfoForArguments);
    }

    /*package*/ ResolvedCall<FunctionDescriptor> resolveTryAsCall(
            @NotNull Call call,
            @NotNull List<Pair<KtExpression, VariableDescriptor>> catchedExceptions,
            @NotNull ExpressionTypingContext context,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        List<String> argumentNames = Lists.newArrayList("tryBlock");
        List<Boolean> argumentsNullability = Lists.newArrayList(false);

        int counter = 0;
        for (Pair<KtExpression, VariableDescriptor> descriptorPair : catchedExceptions) {
            // catchedExceptions are corresponding to PSI arguments that were used to create a call
            // therefore, it's important to use only them to have consistent parameters
            argumentNames.add("catchBlock" + counter);
            argumentsNullability.add(false);

            KtExpression catchBlock = descriptorPair.getFirst();
            VariableDescriptor catchedExceptionDescriptor = descriptorPair.getSecond();
            context.trace.record(BindingContext.NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER, catchBlock, Ref.create(catchedExceptionDescriptor));

            counter++;
        }

        SimpleFunctionDescriptorImpl function =
                createFunctionDescriptorForSpecialConstruction(ResolveConstruct.TRY, argumentNames, argumentsNullability);

        return resolveSpecialConstructionAsCall(call, function, ResolveConstruct.TRY, context, dataFlowInfoForArguments);
    }

    private ResolvedCall<FunctionDescriptor> resolveSpecialConstructionAsCall(
            @NotNull Call call,
            @NotNull SimpleFunctionDescriptorImpl function,
            @NotNull ResolveConstruct construct,
            @NotNull ExpressionTypingContext context,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        TracingStrategy tracing = createTracingForSpecialConstruction(call, construct.getName(), context);
        TypeSubstitutor knownTypeParameterSubstitutor = createKnownTypeParameterSubstitutorForSpecialCall(construct, function, context.expectedType, context.languageVersionSettings);
        OldResolutionCandidate<FunctionDescriptor> resolutionCandidate =
                OldResolutionCandidate.create(call, function, knownTypeParameterSubstitutor);
        OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveCallWithKnownCandidate(
                call, tracing, context, resolutionCandidate, dataFlowInfoForArguments);
        assert results.isSingleResult() : "Not single result after resolving one known candidate";
        return results.getResultingCall();
    }

    private static @Nullable TypeSubstitutor createKnownTypeParameterSubstitutorForSpecialCall(
            @NotNull ResolveConstruct construct,
            @NotNull SimpleFunctionDescriptorImpl function,
            @NotNull KotlinType expectedType,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        if (construct == ResolveConstruct.ELVIS
            || TypeUtils.noExpectedType(expectedType)
            || TypeUtils.isDontCarePlaceholder(expectedType)
            || KotlinBuiltIns.isUnitOrNullableUnit(expectedType)
            || KotlinBuiltIns.isAnyOrNullableAny(expectedType)
                ) {
            return null;
        }

        TypeConstructor typeParameterConstructor = function.getTypeParameters().get(0).getTypeConstructor();
        TypeProjection typeProjection = new TypeProjectionImpl(expectedType);
        return TypeSubstitutor.create(ImmutableMap.of(typeParameterConstructor, typeProjection));
    }

    private SimpleFunctionDescriptorImpl createFunctionDescriptorForSpecialConstruction(
            @NotNull ResolveConstruct construct,
            @NotNull List<String> argumentNames,
            @NotNull List<Boolean> isArgumentNullable
    ) {
        assert argumentNames.size() == isArgumentNullable.size();

        SimpleFunctionDescriptorImpl function = SimpleFunctionDescriptorImpl.create(
                moduleDescriptor, Annotations.Companion.getEMPTY(), construct.getSpecialFunctionName(),
                CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
        );

        TypeParameterDescriptor typeParameter = TypeParameterDescriptorImpl.createWithDefaultBound(
                function, Annotations.Companion.getEMPTY(), false, Variance.INVARIANT,
                construct.getSpecialTypeParameterName(), 0, storageManager);

        KotlinType type = typeParameter.getDefaultType();
        KotlinType nullableType = TypeUtils.makeNullable(type);

        List<ValueParameterDescriptor> valueParameters = new ArrayList<>(argumentNames.size());
        for (int i = 0; i < argumentNames.size(); i++) {
            KotlinType argumentType = isArgumentNullable.get(i) ? nullableType : type;
            ValueParameterDescriptorImpl valueParameter = new ValueParameterDescriptorImpl(
                    function, null, i, Annotations.Companion.getEMPTY(), Name.identifier(argumentNames.get(i)),
                    argumentType,
                    /* declaresDefaultValue = */ false,
                    /* isCrossinline = */ false,
                    /* isNoinline = */ false,
                    null, SourceElement.NO_SOURCE
            );
            valueParameters.add(valueParameter);
        }
        KotlinType returnType = construct != ResolveConstruct.ELVIS ? type : TypeUtilsKt.replaceAnnotations(type, AnnotationsForResolveUtilsKt.getExactInAnnotations());
        function.initialize(
                null,
                null,
                Collections.emptyList(),
                Lists.newArrayList(typeParameter),
                valueParameters,
                returnType,
                Modality.FINAL,
                DescriptorVisibilities.PUBLIC
        );
        return function;
    }

    public static class ControlStructureDataFlowInfo extends MutableDataFlowInfoForArguments {
        public final Map<ValueArgument, DataFlowInfo> dataFlowInfoForArgumentsMap;

        ControlStructureDataFlowInfo(
                @NotNull DataFlowInfo initialDataFlowInfo,
                @NotNull Map<ValueArgument, DataFlowInfo> map
        ) {
            super(initialDataFlowInfo);
            dataFlowInfoForArgumentsMap = map;
        }


        @Override
        public void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo) {
            dataFlowInfoForArgumentsMap.put(valueArgument, dataFlowInfo);
        }

        @Override
        public void updateResultInfo(@NotNull DataFlowInfo dataFlowInfo) { }

        @NotNull
        @Override
        public DataFlowInfo getInfo(@NotNull ValueArgument valueArgument) {
            return dataFlowInfoForArgumentsMap.get(valueArgument);
        }
    }

    private static MutableDataFlowInfoForArguments createIndependentDataFlowInfoForArgumentsForCall(
            @NotNull DataFlowInfo initialDataFlowInfo,
            @NotNull Map<ValueArgument, DataFlowInfo> dataFlowInfoForArgumentsMap
    ) {
        return new ControlStructureDataFlowInfo(initialDataFlowInfo, dataFlowInfoForArgumentsMap);
    }

    public static MutableDataFlowInfoForArguments createDataFlowInfoForArgumentsForIfCall(
            @NotNull Call callForIf,
            @NotNull DataFlowInfo conditionInfo,
            @NotNull DataFlowInfo thenInfo,
            @NotNull DataFlowInfo elseInfo
    ) {
        Map<ValueArgument, DataFlowInfo> dataFlowInfoForArgumentsMap = new HashMap<>();
        dataFlowInfoForArgumentsMap.put(callForIf.getValueArguments().get(0), thenInfo);
        dataFlowInfoForArgumentsMap.put(callForIf.getValueArguments().get(1), elseInfo);
        return createIndependentDataFlowInfoForArgumentsForCall(conditionInfo, dataFlowInfoForArgumentsMap);
    }

    public static MutableDataFlowInfoForArguments createDataFlowInfoForArgumentsOfWhenCall(
            @NotNull Call callForWhen,
            @NotNull DataFlowInfo subjectDataFlowInfo,
            @NotNull List<DataFlowInfo> entryDataFlowInfos
    ) {
        Map<ValueArgument, DataFlowInfo> dataFlowInfoForArgumentsMap = new HashMap<>();
        int i = 0;
        for (ValueArgument argument : callForWhen.getValueArguments()) {
            DataFlowInfo entryDataFlowInfo = entryDataFlowInfos.get(i++);
            dataFlowInfoForArgumentsMap.put(argument, entryDataFlowInfo);
        }
        return createIndependentDataFlowInfoForArgumentsForCall(subjectDataFlowInfo, dataFlowInfoForArgumentsMap);
    }

    public static MutableDataFlowInfoForArguments createDataFlowInfoForArgumentsOfTryCall(
            @NotNull Call callForTry,
            @NotNull DataFlowInfo dataFlowInfoBeforeTry,
            @NotNull DataFlowInfo dataFlowInfoAfterTry
    ) {
        Map<ValueArgument, DataFlowInfo> dataFlowInfoForArgumentsMap = new HashMap<>();
        List<? extends ValueArgument> valueArguments = callForTry.getValueArguments();
        dataFlowInfoForArgumentsMap.put(valueArguments.get(0), dataFlowInfoBeforeTry);
        for (int i = 1; i < valueArguments.size(); i++) {
            dataFlowInfoForArgumentsMap.put(valueArguments.get(i), dataFlowInfoAfterTry);
        }
        return createIndependentDataFlowInfoForArgumentsForCall(dataFlowInfoBeforeTry, dataFlowInfoForArgumentsMap);
    }

    /*package*/ static Call createCallForSpecialConstruction(
            @NotNull KtExpression expression,
            @NotNull KtExpression calleeExpression,
            @NotNull List<? extends KtExpression> arguments
    ) {
        List<ValueArgument> valueArguments = Lists.newArrayList();
        for (KtExpression argument : arguments) {
            valueArguments.add(CallMaker.makeValueArgument(argument));
        }
        return new Call() {
            @Nullable
            @Override
            public ASTNode getCallOperationNode() {
                return expression.getNode();
            }

            @Nullable
            @Override
            public ReceiverValue getExplicitReceiver() {
                return null;
            }

            @Nullable
            @Override
            public ReceiverValue getDispatchReceiver() {
                return null;
            }

            @Nullable
            @Override
            public KtExpression getCalleeExpression() {
                return calleeExpression;
            }

            @Nullable
            @Override
            public KtValueArgumentList getValueArgumentList() {
                return null;
            }

            @NotNull
            @Override
            public List<? extends ValueArgument> getValueArguments() {
                return valueArguments;
            }

            @NotNull
            @Override
            public List<LambdaArgument> getFunctionLiteralArguments() {
                return Collections.emptyList();
            }

            @NotNull
            @Override
            public List<KtTypeProjection> getTypeArguments() {
                return Collections.emptyList();
            }

            @Nullable
            @Override
            public KtTypeArgumentList getTypeArgumentList() {
                return null;
            }

            @NotNull
            @Override
            public KtElement getCallElement() {
                return expression;
            }

            @NotNull
            @Override
            public CallType getCallType() {
                return CallType.DEFAULT;
            }
        };
    }

    @NotNull
    private TracingStrategy createTracingForSpecialConstruction(
            @NotNull Call call,
            @NotNull String constructionName,
            @NotNull ExpressionTypingContext context
    ) {
        class CheckTypeContext {
            public BindingTrace trace;
            public KotlinType expectedType;

            CheckTypeContext(@NotNull BindingTrace trace, @NotNull KotlinType expectedType) {
                this.trace = trace;
                this.expectedType = expectedType;
            }

            CheckTypeContext makeTypeNullable() {
                if (TypeUtils.noExpectedType(expectedType)) return this;
                return new CheckTypeContext(trace, TypeUtils.makeNullable(expectedType));
            }
        }

        KtVisitor<Boolean, CheckTypeContext> checkTypeVisitor = new KtVisitor<Boolean, CheckTypeContext>() {
            private boolean checkExpressionType(@NotNull KtExpression expression, CheckTypeContext c) {
                KotlinTypeInfo typeInfo = BindingContextUtils.getRecordedTypeInfo(expression, c.trace.getBindingContext());
                if (typeInfo == null) return false;

                Ref<Boolean> hasError = Ref.create();
                dataFlowAnalyzer.checkType(
                        typeInfo.getType(),
                        expression,
                        context
                                .replaceExpectedType(c.expectedType)
                                .replaceDataFlowInfo(typeInfo.getDataFlowInfo())
                                .replaceBindingTrace(c.trace),
                        hasError,
                        true
                );
                return hasError.get();
            }

            private boolean checkExpressionTypeRecursively(@Nullable KtExpression expression, CheckTypeContext c) {
                if (expression == null) return false;
                return expression.accept(this, c);
            }

            private boolean checkSubExpressions(
                    KtExpression firstSub, KtExpression secondSub, KtExpression expression,
                    CheckTypeContext firstContext, CheckTypeContext secondContext, CheckTypeContext context
            ) {
                boolean errorWasReported = checkExpressionTypeRecursively(firstSub, firstContext);
                errorWasReported |= checkExpressionTypeRecursively(secondSub, secondContext);
                return errorWasReported || checkExpressionType(expression, context);
            }

            @Override
            public Boolean visitWhenExpression(@NotNull KtWhenExpression whenExpression, CheckTypeContext c) {
                boolean errorWasReported = false;
                for (KtWhenEntry whenEntry : whenExpression.getEntries()) {
                    KtExpression entryExpression = whenEntry.getExpression();
                    if (entryExpression != null) {
                        errorWasReported |= checkExpressionTypeRecursively(entryExpression, c);
                    }
                }
                errorWasReported |= checkExpressionType(whenExpression, c);
                return errorWasReported;
            }

            @Override
            public Boolean visitIfExpression(@NotNull KtIfExpression ifExpression, CheckTypeContext c) {
                KtExpression thenBranch = ifExpression.getThen();
                KtExpression elseBranch = ifExpression.getElse();
                if (thenBranch == null || elseBranch == null) {
                    return checkExpressionType(ifExpression, c);
                }
                return checkSubExpressions(thenBranch, elseBranch, ifExpression, c, c, c);
            }

            @Override
            public Boolean visitBlockExpression(@NotNull KtBlockExpression expression, CheckTypeContext c) {
                if (expression.getStatements().isEmpty()) {
                    return checkExpressionType(expression, c);
                }
                KtExpression lastStatement = KtPsiUtil.getLastStatementInABlock(expression);
                if (lastStatement != null) {
                    return checkExpressionTypeRecursively(lastStatement, c);
                }
                return false;
            }

            @Override
            public Boolean visitPostfixExpression(@NotNull KtPostfixExpression expression, CheckTypeContext c) {
                if (expression.getOperationReference().getReferencedNameElementType() == KtTokens.EXCLEXCL) {
                    return checkExpressionTypeRecursively(expression.getBaseExpression(), c.makeTypeNullable());
                }
                return super.visitPostfixExpression(expression, c);
            }

            @Override
            public Boolean visitBinaryExpression(@NotNull KtBinaryExpression expression, CheckTypeContext c) {
                if (expression.getOperationReference().getReferencedNameElementType() == KtTokens.ELVIS) {

                    return checkSubExpressions(expression.getLeft(), expression.getRight(), expression, c.makeTypeNullable(), c, c);
                }
                return super.visitBinaryExpression(expression, c);
            }

            @Override
            public Boolean visitExpression(@NotNull KtExpression expression, CheckTypeContext c) {
                return checkExpressionType(expression, c);
            }
        };

        return new ThrowingOnErrorTracingStrategy("resolve " + constructionName + " as a call") {
            @Override
            public <D extends CallableDescriptor> void bindReference(
                    @NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall
            ) {
                //do nothing
            }

            @Override
            public void bindCall(@NotNull BindingTrace trace, @NotNull Call call) {
                trace.record(CALL, call.getCalleeExpression(), call);
            }

            @Override
            public <D extends CallableDescriptor> void bindResolvedCall(
                    @NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall
            ) {
                trace.record(RESOLVED_CALL, call, resolvedCall);
            }

            @Override
            public void typeInferenceFailed(
                    @NotNull ResolutionContext<?> context, @NotNull InferenceErrorData data
            ) {
                ConstraintSystem constraintSystem = data.constraintSystem;
                ConstraintSystemStatus status = constraintSystem.getStatus();
                assert !status.isSuccessful() : "Report error only for not successful constraint system";

                if (status.hasErrorInConstrainingTypes() || status.hasUnknownParameters()) {
                    return;
                }
                KtExpression expression = (KtExpression) call.getCallElement();
                if (status.hasOnlyErrorsDerivedFrom(EXPECTED_TYPE_POSITION) || status.hasConflictingConstraints()
                        || status.hasTypeInferenceIncorporationError()) { // todo after KT-... remove this line
                    if (noTypeCheckingErrorsInExpression(expression, context.trace, data.expectedType)) {
                        KtExpression calleeExpression = call.getCalleeExpression();
                        if (calleeExpression instanceof KtWhenExpression || calleeExpression instanceof KtIfExpression) {
                            if (status.hasConflictingConstraints() || status.hasTypeInferenceIncorporationError()) {
                                // TODO provide comprehensible error report for hasConflictingConstraints() case (if possible)
                                context.trace.report(TYPE_INFERENCE_FAILED_ON_SPECIAL_CONSTRUCT.on(expression));
                            }
                        }
                    }
                    return;
                }
                KtDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(expression, KtNamedDeclaration.class);
                logError("Expression: " + (parentDeclaration != null ? parentDeclaration.getText() : expression.getText()) +
                         "\nConstraint system status: \n" + ConstraintsUtil.getDebugMessageForStatus(status));
            }

            private boolean noTypeCheckingErrorsInExpression(
                    KtExpression expression,
                    @NotNull BindingTrace trace,
                    @NotNull KotlinType expectedType
            ) {
                return Boolean.TRUE != expression.accept(checkTypeVisitor, new CheckTypeContext(trace, expectedType));
            }
        };
    }

    private abstract static class ThrowingOnErrorTracingStrategy implements TracingStrategy {
        private final String debugName;

        protected ThrowingOnErrorTracingStrategy(String debugName) {
            this.debugName = debugName;
        }

        private void logError() {
            logError(null);
        }

        protected void logError(@Nullable String additionalInformation) {
            String errorMessage = "Resolution error of this type shouldn't occur for " + debugName;
            if (additionalInformation != null) {
                errorMessage += ".\n" + additionalInformation;
            }
            LOG.error(errorMessage);
        }

        @Override
        public void unresolvedReference(@NotNull BindingTrace trace) {
            logError();
        }

        @Override
        public void recursiveType(@NotNull BindingTrace trace) {
            logError();
        }

        @Override
        public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(
                @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates
        ) {
            logError();
        }

        @Override
        public <D extends CallableDescriptor> void recordAmbiguity(
                @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates
        ) {
            logError();
        }

        @Override
        public void missingReceiver(
                @NotNull BindingTrace trace, @NotNull ReceiverParameterDescriptor expectedReceiver
        ) {
            logError();
        }

        @Override
        public void wrongReceiverType(
                @NotNull BindingTrace trace,
                @NotNull ReceiverParameterDescriptor receiverParameter,
                @NotNull ReceiverValue receiverArgument,
                @NotNull ResolutionContext<?> c
        ) {
            logError();
        }

        @Override
        public void noReceiverAllowed(@NotNull BindingTrace trace) {
            logError();
        }

        @Override
        public void noValueForParameter(
                @NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter
        ) {
            logError();
        }

        @Override
        public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount, @NotNull CallableDescriptor descriptor) {
            logError();
        }

        @Override
        public <D extends CallableDescriptor> void ambiguity(
                @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> resolvedCalls
        ) {
            logError();
        }

        @Override
        public <D extends CallableDescriptor> void noneApplicable(
                @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors
        ) {
            logError();
        }

        @Override
        public <D extends CallableDescriptor> void cannotCompleteResolve(
                @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> descriptors
        ) {
            logError();
        }

        @Override
        public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {
            logError();
        }

        @Override
        public void abstractSuperCall(@NotNull BindingTrace trace) {
            logError();
        }

        @Override
        public void nestedClassAccessViaInstanceReference(
                @NotNull BindingTrace trace, @NotNull ClassDescriptor classDescriptor,
                @NotNull ExplicitReceiverKind explicitReceiverKind
        ) {
            logError();
        }

        @Override
        public void unsafeCall(
                @NotNull BindingTrace trace, @NotNull KotlinType type, boolean isCallForImplicitInvoke
        ) {
            logError();
        }

        @Override
        public void invisibleMember(
                @NotNull BindingTrace trace, @NotNull DeclarationDescriptorWithVisibility descriptor
        ) {
            logError();
        }

        @Override
        public void typeInferenceFailed(
                @NotNull ResolutionContext<?> context, @NotNull InferenceErrorData inferenceErrorData
        ) {
            logError();
        }
    }
}
