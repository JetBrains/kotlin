/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cfg;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartFMap;
import com.intellij.util.containers.ContainerUtil;
import kotlin.ArraysKt;
import kotlin.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cfg.pseudocode.JetControlFlowInstructionsGenerator;
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue;
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.AccessTarget;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.InstructionWithValue;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicKind;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.JetPsiUtilKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.*;
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import java.util.*;

import static org.jetbrains.kotlin.cfg.JetControlFlowBuilder.PredefinedOperation.*;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.KtTokens.*;

public class JetControlFlowProcessor {

    private final JetControlFlowBuilder builder;
    private final BindingTrace trace;

    public JetControlFlowProcessor(BindingTrace trace) {
        this.builder = new JetControlFlowInstructionsGenerator();
        this.trace = trace;
    }

    @NotNull
    public Pseudocode generatePseudocode(@NotNull KtElement subroutine) {
        Pseudocode pseudocode = generate(subroutine);
        ((PseudocodeImpl) pseudocode).postProcess();
        return pseudocode;
    }

    @NotNull
    private Pseudocode generate(@NotNull KtElement subroutine) {
        builder.enterSubroutine(subroutine);
        CFPVisitor cfpVisitor = new CFPVisitor(builder);
        if (subroutine instanceof KtDeclarationWithBody && !(subroutine instanceof KtSecondaryConstructor)) {
            KtDeclarationWithBody declarationWithBody = (KtDeclarationWithBody) subroutine;
            List<KtParameter> valueParameters = declarationWithBody.getValueParameters();
            for (KtParameter valueParameter : valueParameters) {
                cfpVisitor.generateInstructions(valueParameter);
            }
            KtExpression bodyExpression = declarationWithBody.getBodyExpression();
            if (bodyExpression != null) {
                cfpVisitor.generateInstructions(bodyExpression);
                if (!declarationWithBody.hasBlockBody()) {
                    generateImplicitReturnValue(bodyExpression, subroutine);
                }
            }
        } else {
            cfpVisitor.generateInstructions(subroutine);
        }
        return builder.exitSubroutine(subroutine);
    }

    private void generateImplicitReturnValue(@NotNull KtExpression bodyExpression, @NotNull KtElement subroutine) {
        CallableDescriptor subroutineDescriptor = (CallableDescriptor) trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, subroutine);
        if (subroutineDescriptor == null) return;

        KotlinType returnType = subroutineDescriptor.getReturnType();
        if (returnType != null && KotlinBuiltIns.isUnit(returnType) && subroutineDescriptor instanceof AnonymousFunctionDescriptor) return;

        PseudoValue returnValue = builder.getBoundValue(bodyExpression);
        if (returnValue == null) return;

        builder.returnValue(bodyExpression, returnValue, subroutine);
    }

    private void processLocalDeclaration(@NotNull KtDeclaration subroutine) {
        Label afterDeclaration = builder.createUnboundLabel("after local declaration");

        builder.nondeterministicJump(afterDeclaration, subroutine, null);
        generate(subroutine);
        builder.bindLabel(afterDeclaration);
    }

    private class CFPVisitor extends KtVisitorVoid {
        private final JetControlFlowBuilder builder;

        private final KtVisitorVoid conditionVisitor = new KtVisitorVoid() {

            private KtExpression getSubjectExpression(KtWhenCondition condition) {
                KtWhenExpression whenExpression = PsiTreeUtil.getParentOfType(condition, KtWhenExpression.class);
                return whenExpression != null ? whenExpression.getSubjectExpression() : null;
            }

            @Override
            public void visitWhenConditionInRange(@NotNull KtWhenConditionInRange condition) {
                if (!generateCall(condition.getOperationReference())) {
                    KtExpression rangeExpression = condition.getRangeExpression();
                    generateInstructions(rangeExpression);
                    createNonSyntheticValue(condition, MagicKind.UNRESOLVED_CALL, rangeExpression);
                }
            }

            @Override
            public void visitWhenConditionIsPattern(@NotNull KtWhenConditionIsPattern condition) {
                mark(condition);
                createNonSyntheticValue(condition, MagicKind.IS, getSubjectExpression(condition));
            }

            @Override
            public void visitWhenConditionWithExpression(@NotNull KtWhenConditionWithExpression condition) {
                mark(condition);

                KtExpression expression = condition.getExpression();
                generateInstructions(expression);

                KtExpression subjectExpression = getSubjectExpression(condition);
                if (subjectExpression != null) {
                    // todo: this can be replaced by equals() invocation (when corresponding resolved call is recorded)
                    createNonSyntheticValue(condition, MagicKind.EQUALS_IN_WHEN_CONDITION, subjectExpression, expression);
                }
                else {
                    copyValue(expression, condition);
                }
            }

            @Override
            public void visitJetElement(@NotNull KtElement element) {
                throw new UnsupportedOperationException("[JetControlFlowProcessor] " + element.toString());
            }
        };

        private CFPVisitor(@NotNull JetControlFlowBuilder builder) {
            this.builder = builder;
        }

        private void mark(KtElement element) {
            builder.mark(element);
        }

        public void generateInstructions(@Nullable KtElement element) {
            if (element == null) return;
            element.accept(this);
            checkNothingType(element);
        }

        private void checkNothingType(KtElement element) {
            if (!(element instanceof KtExpression)) return;

            KtExpression expression = KtPsiUtil.deparenthesize((KtExpression) element);
            if (expression == null) return;

            if (expression instanceof KtStatementExpression || expression instanceof KtTryExpression
                || expression instanceof KtIfExpression || expression instanceof KtWhenExpression) {
                return;
            }

            KotlinType type = trace.getBindingContext().getType(expression);
            if (type != null && KotlinBuiltIns.isNothing(type)) {
                builder.jumpToError(expression);
            }
        }

        @NotNull
        private PseudoValue createSyntheticValue(@NotNull KtElement instructionElement, @NotNull MagicKind kind, KtElement... from) {
            List<PseudoValue> values = elementsToValues(from.length > 0 ? Arrays.asList(from) : Collections.<KtElement>emptyList());
            return builder.magic(instructionElement, null, values, kind).getOutputValue();
        }

        @NotNull
        private PseudoValue createNonSyntheticValue(
                @NotNull KtElement to, @NotNull List<? extends KtElement> from, @NotNull MagicKind kind
        ) {
            List<PseudoValue> values = elementsToValues(from);
            return builder.magic(to, to, values, kind).getOutputValue();
        }

        @NotNull
        private PseudoValue createNonSyntheticValue(@NotNull KtElement to, @NotNull MagicKind kind, KtElement... from) {
            return createNonSyntheticValue(to, Arrays.asList(from), kind);
        }

        private void mergeValues(@NotNull List<KtExpression> from, @NotNull KtExpression to) {
            builder.merge(to, elementsToValues(from));
        }

        private void copyValue(@Nullable KtElement from, @NotNull KtElement to) {
            PseudoValue value = getBoundOrUnreachableValue(from);
            if (value != null) {
                builder.bindValue(value, to);
            }
        }

        @Nullable
        private PseudoValue getBoundOrUnreachableValue(@Nullable KtElement element) {
            if (element == null) return null;

            PseudoValue value = builder.getBoundValue(element);
            return value != null || element instanceof KtDeclaration ? value : builder.newValue(element);
        }

        private List<PseudoValue> elementsToValues(List<? extends KtElement> from) {
            if (from.isEmpty()) return Collections.emptyList();
            return CollectionsKt.filterNotNull(
                    CollectionsKt.map(
                            from,
                            new Function1<KtElement, PseudoValue>() {
                                @Override
                                public PseudoValue invoke(KtElement element) {
                                    return getBoundOrUnreachableValue(element);
                                }
                            }
                    )
            );
        }

        private void generateInitializer(@NotNull KtDeclaration declaration, @NotNull PseudoValue initValue) {
            builder.write(
                    declaration,
                    declaration,
                    initValue,
                    getDeclarationAccessTarget(declaration),
                    Collections.<PseudoValue, ReceiverValue>emptyMap()
            );
        }

        @NotNull
        private AccessTarget getResolvedCallAccessTarget(KtElement element) {
            ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(element, trace.getBindingContext());
            return resolvedCall != null ? new AccessTarget.Call(resolvedCall) : AccessTarget.BlackBox.INSTANCE$;
        }

        @NotNull
        private AccessTarget getDeclarationAccessTarget(KtElement element) {
            DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
            return descriptor instanceof VariableDescriptor
                   ? new AccessTarget.Declaration((VariableDescriptor) descriptor)
                   : AccessTarget.BlackBox.INSTANCE$;
        }

        @Override
        public void visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression) {
            mark(expression);
            KtExpression innerExpression = expression.getExpression();
            if (innerExpression != null) {
                generateInstructions(innerExpression);
                copyValue(innerExpression, expression);
            }
        }

        @Override
        public void visitAnnotatedExpression(@NotNull KtAnnotatedExpression expression) {
            KtExpression baseExpression = expression.getBaseExpression();
            if (baseExpression != null) {
                generateInstructions(baseExpression);
                copyValue(baseExpression, expression);
            }
        }

        @Override
        public void visitThisExpression(@NotNull KtThisExpression expression) {
            ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(expression, trace.getBindingContext());
            if (resolvedCall == null) {
                createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL);
                return;
            }

            CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
            if (resultingDescriptor instanceof ReceiverParameterDescriptor) {
                builder.readVariable(expression, resolvedCall, getReceiverValues(resolvedCall));
            }

            copyValue(expression, expression.getInstanceReference());
        }

        @Override
        public void visitConstantExpression(@NotNull KtConstantExpression expression) {
            CompileTimeConstant<?> constant = ConstantExpressionEvaluator.getConstant(expression, trace.getBindingContext());
            builder.loadConstant(expression, constant);
        }

        @Override
        public void visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression) {
            ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(expression, trace.getBindingContext());
            if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
                VariableAsFunctionResolvedCall variableAsFunctionResolvedCall = (VariableAsFunctionResolvedCall) resolvedCall;
                generateCall(variableAsFunctionResolvedCall.getVariableCall());
            }
            else if (!generateCall(expression) && !(expression.getParent() instanceof KtCallExpression)) {
                createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL, generateAndGetReceiverIfAny(expression));
            }
        }

        @Override
        public void visitLabeledExpression(@NotNull KtLabeledExpression expression) {
            mark(expression);
            KtExpression baseExpression = expression.getBaseExpression();
            if (baseExpression != null) {
                generateInstructions(baseExpression);
                copyValue(baseExpression, expression);
            }
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        @Override
        public void visitBinaryExpression(@NotNull KtBinaryExpression expression) {
            KtSimpleNameExpression operationReference = expression.getOperationReference();
            IElementType operationType = operationReference.getReferencedNameElementType();

            KtExpression left = expression.getLeft();
            KtExpression right = expression.getRight();
            if (operationType == ANDAND || operationType == OROR) {
                generateBooleanOperation(expression);
            }
            else if (operationType == EQ) {
                visitAssignment(left, getDeferredValue(right), expression);
            }
            else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
                ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(expression, trace.getBindingContext());
                if (resolvedCall != null) {
                    PseudoValue rhsValue = generateCall(resolvedCall).getOutputValue();
                    Name assignMethodName = OperatorConventions.getNameForOperationSymbol((KtToken) expression.getOperationToken());
                    if (!resolvedCall.getResultingDescriptor().getName().equals(assignMethodName)) {
                        /* At this point assignment of the form a += b actually means a = a + b
                         * So we first generate call of "+" operation and then use its output pseudo-value
                         * as a right-hand side when generating assignment call
                         */
                        visitAssignment(left, getValueAsFunction(rhsValue), expression);
                    }
                }
                else {
                    generateBothArgumentsAndMark(expression);
                }
            }
            else if (operationType == ELVIS) {
                generateInstructions(left);
                mark(expression);
                Label afterElvis = builder.createUnboundLabel("after elvis operator");
                builder.jumpOnTrue(afterElvis, expression, builder.getBoundValue(left));
                if (right != null) {
                    generateInstructions(right);
                }
                builder.bindLabel(afterElvis);
                mergeValues(Arrays.asList(left, right), expression);
            }
            else {
                if (!generateCall(expression)) {
                    generateBothArgumentsAndMark(expression);
                }
            }
        }

        private void generateBooleanOperation(KtBinaryExpression expression) {
            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            KtExpression left = expression.getLeft();
            KtExpression right = expression.getRight();

            Label resultLabel = builder.createUnboundLabel("result of boolean operation");
            generateInstructions(left);
            if (operationType == ANDAND) {
                builder.jumpOnFalse(resultLabel, expression, builder.getBoundValue(left));
            }
            else {
                builder.jumpOnTrue(resultLabel, expression, builder.getBoundValue(left));
            }
            if (right != null) {
                generateInstructions(right);
            }
            builder.bindLabel(resultLabel);
            JetControlFlowBuilder.PredefinedOperation operation = operationType == ANDAND ? AND : OR;
            builder.predefinedOperation(expression, operation, elementsToValues(Arrays.asList(left, right)));
        }

        private Function0<PseudoValue> getValueAsFunction(final PseudoValue value) {
            return new Function0<PseudoValue>() {
                @Override
                public PseudoValue invoke() {
                    return value;
                }
            };
        }

        private Function0<PseudoValue> getDeferredValue(final KtExpression expression) {
            return new Function0<PseudoValue>() {
                @Override
                public PseudoValue invoke() {
                    generateInstructions(expression);
                    return getBoundOrUnreachableValue(expression);
                }
            };
        }

        private void generateBothArgumentsAndMark(KtBinaryExpression expression) {
            KtExpression left = KtPsiUtil.deparenthesize(expression.getLeft());
            if (left != null) {
                generateInstructions(left);
            }
            KtExpression right = expression.getRight();
            if (right != null) {
                generateInstructions(right);
            }
            mark(expression);
            createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL, left, right);
        }

        private void visitAssignment(
                KtExpression lhs,
                @NotNull Function0<PseudoValue> rhsDeferredValue,
                KtExpression parentExpression
        ) {
            KtExpression left = KtPsiUtil.deparenthesize(lhs);
            if (left == null) {
                List<PseudoValue> arguments = Collections.singletonList(rhsDeferredValue.invoke());
                builder.magic(parentExpression, parentExpression, arguments, MagicKind.UNSUPPORTED_ELEMENT);
                return;
            }

            if (left instanceof KtArrayAccessExpression) {
                generateArrayAssignment((KtArrayAccessExpression) left, rhsDeferredValue, parentExpression);
                return;
            }

            Map<PseudoValue, ReceiverValue> receiverValues = SmartFMap.emptyMap();
            AccessTarget accessTarget = AccessTarget.BlackBox.INSTANCE$;
            if (left instanceof KtSimpleNameExpression || left instanceof KtQualifiedExpression) {
                accessTarget = getResolvedCallAccessTarget(JetPsiUtilKt.getQualifiedElementSelector(left));
                if (accessTarget instanceof AccessTarget.Call) {
                    receiverValues = getReceiverValues(((AccessTarget.Call) accessTarget).getResolvedCall());
                }
            }
            else if (left instanceof KtProperty) {
                accessTarget = getDeclarationAccessTarget(left);
            }

            if (accessTarget == AccessTarget.BlackBox.INSTANCE$ && !(left instanceof KtProperty)) {
                generateInstructions(left);
                createSyntheticValue(left, MagicKind.VALUE_CONSUMER, left);
            }

            PseudoValue rightValue = rhsDeferredValue.invoke();
            PseudoValue rValue =
                    rightValue != null ? rightValue : createSyntheticValue(parentExpression, MagicKind.UNRECOGNIZED_WRITE_RHS);
            builder.write(parentExpression, left, rValue, accessTarget, receiverValues);
        }

        private void generateArrayAssignment(
                KtArrayAccessExpression lhs,
                @NotNull Function0<PseudoValue> rhsDeferredValue,
                @NotNull KtExpression parentExpression
        ) {
            ResolvedCall<FunctionDescriptor> setResolvedCall = trace.get(BindingContext.INDEXED_LVALUE_SET, lhs);

            if (setResolvedCall == null) {
                generateArrayAccess(lhs, null);

                List<PseudoValue> arguments = CollectionsKt.filterNotNull(
                        Arrays.asList(getBoundOrUnreachableValue(lhs), rhsDeferredValue.invoke())
                );
                builder.magic(parentExpression, parentExpression, arguments, MagicKind.UNRESOLVED_CALL);

                return;
            }

            // In case of simple ('=') array assignment mark instruction is not generated yet, so we put it before generating "set" call
            if (((KtOperationExpression) parentExpression).getOperationReference().getReferencedNameElementType() == EQ) {
                mark(lhs);
            }

            generateInstructions(lhs.getArrayExpression());

            Map<PseudoValue, ReceiverValue> receiverValues = getReceiverValues(setResolvedCall);
            SmartFMap<PseudoValue, ValueParameterDescriptor> argumentValues = getArraySetterArguments(rhsDeferredValue, setResolvedCall);

            builder.call(parentExpression, setResolvedCall, receiverValues, argumentValues);
        }

        /* We assume that assignment right-hand side corresponds to the last argument of the call
        *  So receiver instructions/pseudo-values are generated for all arguments except the last one which is replaced
        *  by pre-generated pseudo-value
        *  For example, assignment a[1, 2] += 3 means a.set(1, 2, a.get(1) + 3), so in order to generate "set" call
        *  we first generate instructions for 1 and 2 whereas 3 is replaced by pseudo-value corresponding to "a.get(1) + 3"
        */
        private SmartFMap<PseudoValue, ValueParameterDescriptor> getArraySetterArguments(
                Function0<PseudoValue> rhsDeferredValue,
                final ResolvedCall<FunctionDescriptor> setResolvedCall
        ) {
            List<ValueArgument> valueArguments = CollectionsKt.flatMapTo(
                    setResolvedCall.getResultingDescriptor().getValueParameters(),
                    new ArrayList<ValueArgument>(),
                    new Function1<ValueParameterDescriptor, Iterable<? extends ValueArgument>>() {
                        @Override
                        public Iterable<? extends ValueArgument> invoke(ValueParameterDescriptor descriptor) {
                            ResolvedValueArgument resolvedValueArgument = setResolvedCall.getValueArguments().get(descriptor);
                            return resolvedValueArgument != null
                                   ? resolvedValueArgument.getArguments()
                                   : Collections.<ValueArgument>emptyList();
                        }
                    }
            );

            ValueArgument rhsArgument = CollectionsKt.lastOrNull(valueArguments);
            SmartFMap<PseudoValue, ValueParameterDescriptor> argumentValues = SmartFMap.emptyMap();
            for (ValueArgument valueArgument : valueArguments) {
                ArgumentMapping argumentMapping = setResolvedCall.getArgumentMapping(valueArgument);
                if (argumentMapping.isError() || (!(argumentMapping instanceof ArgumentMatch))) continue;

                ValueParameterDescriptor parameterDescriptor = ((ArgumentMatch) argumentMapping).getValueParameter();
                if (valueArgument != rhsArgument) {
                    argumentValues = generateValueArgument(valueArgument, parameterDescriptor, argumentValues);
                }
                else {
                    PseudoValue rhsValue = rhsDeferredValue.invoke();
                    if (rhsValue != null) {
                        argumentValues = argumentValues.plus(rhsValue, parameterDescriptor);
                    }
                }
            }
            return argumentValues;
        }

        private void generateArrayAccess(KtArrayAccessExpression arrayAccessExpression, @Nullable ResolvedCall<?> resolvedCall) {
            if (builder.getBoundValue(arrayAccessExpression) != null) return;
            mark(arrayAccessExpression);
            if (!checkAndGenerateCall(resolvedCall)) {
                generateArrayAccessWithoutCall(arrayAccessExpression);
            }
        }

        private void generateArrayAccessWithoutCall(KtArrayAccessExpression arrayAccessExpression) {
            createNonSyntheticValue(arrayAccessExpression, generateArrayAccessArguments(arrayAccessExpression), MagicKind.UNRESOLVED_CALL);
        }

        private List<KtExpression> generateArrayAccessArguments(KtArrayAccessExpression arrayAccessExpression) {
            List<KtExpression> inputExpressions = new ArrayList<KtExpression>();

            KtExpression arrayExpression = arrayAccessExpression.getArrayExpression();
            inputExpressions.add(arrayExpression);
            generateInstructions(arrayExpression);

            for (KtExpression index : arrayAccessExpression.getIndexExpressions()) {
                generateInstructions(index);
                inputExpressions.add(index);
            }

            return inputExpressions;
        }

        @Override
        public void visitUnaryExpression(@NotNull KtUnaryExpression expression) {
            KtSimpleNameExpression operationSign = expression.getOperationReference();
            IElementType operationType = operationSign.getReferencedNameElementType();
            KtExpression baseExpression = expression.getBaseExpression();
            if (baseExpression == null) return;
            if (KtTokens.EXCLEXCL == operationType) {
                generateInstructions(baseExpression);
                builder.predefinedOperation(expression, NOT_NULL_ASSERTION, elementsToValues(Collections.singletonList(baseExpression)));
                return;
            }

            boolean incrementOrDecrement = isIncrementOrDecrement(operationType);
            ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(expression, trace.getBindingContext());

            PseudoValue rhsValue;
            if (resolvedCall != null) {
                rhsValue = generateCall(resolvedCall).getOutputValue();
            }
            else {
                generateInstructions(baseExpression);
                rhsValue = createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL, baseExpression);
            }

            if (incrementOrDecrement) {
                visitAssignment(baseExpression, getValueAsFunction(rhsValue), expression);
                if (expression instanceof KtPostfixExpression) {
                    copyValue(baseExpression, expression);
                }
            }
        }

        private boolean isIncrementOrDecrement(IElementType operationType) {
            return operationType == KtTokens.PLUSPLUS || operationType == KtTokens.MINUSMINUS;
        }

        @Override
        public void visitIfExpression(@NotNull KtIfExpression expression) {
            mark(expression);
            List<KtExpression> branches = new ArrayList<KtExpression>(2);
            KtExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition);
            }
            Label elseLabel = builder.createUnboundLabel("else branch");
            builder.jumpOnFalse(elseLabel, expression, builder.getBoundValue(condition));
            KtExpression thenBranch = expression.getThen();
            if (thenBranch != null) {
                branches.add(thenBranch);
                generateInstructions(thenBranch);
            }
            else {
                builder.loadUnit(expression);
            }
            Label resultLabel = builder.createUnboundLabel("'if' expression result");
            builder.jump(resultLabel, expression);
            builder.bindLabel(elseLabel);
            KtExpression elseBranch = expression.getElse();
            if (elseBranch != null) {
                branches.add(elseBranch);
                generateInstructions(elseBranch);
            }
            else {
                builder.loadUnit(expression);
            }
            builder.bindLabel(resultLabel);
            mergeValues(branches, expression);
        }

        private class FinallyBlockGenerator {
            private final KtFinallySection finallyBlock;
            private Label startFinally = null;
            private Label finishFinally = null;

            private FinallyBlockGenerator(KtFinallySection block) {
                finallyBlock = block;
            }

            public void generate() {
                KtBlockExpression finalExpression = finallyBlock.getFinalExpression();
                if (finalExpression == null) return;
                if (startFinally != null) {
                    assert finishFinally != null;
                    builder.repeatPseudocode(startFinally, finishFinally);
                    return;
                }
                startFinally = builder.createUnboundLabel("start finally");
                builder.bindLabel(startFinally);
                generateInstructions(finalExpression);
                finishFinally = builder.createUnboundLabel("finish finally");
                builder.bindLabel(finishFinally);
            }
        }

        @Override
        public void visitTryExpression(@NotNull KtTryExpression expression) {
            mark(expression);

            KtFinallySection finallyBlock = expression.getFinallyBlock();
            final FinallyBlockGenerator finallyBlockGenerator = new FinallyBlockGenerator(finallyBlock);
            boolean hasFinally = finallyBlock != null;
            if (hasFinally) {
                builder.enterTryFinally(new GenerationTrigger() {
                    private boolean working = false;

                    @Override
                    public void generate() {
                        // This checks are needed for the case of having e.g. return inside finally: 'try {return} finally{return}'
                        if (working) return;
                        working = true;
                        finallyBlockGenerator.generate();
                        working = false;
                    }
                });
            }

            Label onExceptionToFinallyBlock = generateTryAndCatches(expression);

            if (hasFinally) {
                assert onExceptionToFinallyBlock != null : "No finally lable generated: " + expression.getText();

                builder.exitTryFinally();

                Label skipFinallyToErrorBlock = builder.createUnboundLabel("skipFinallyToErrorBlock");
                builder.jump(skipFinallyToErrorBlock, expression);
                builder.bindLabel(onExceptionToFinallyBlock);
                finallyBlockGenerator.generate();
                builder.jumpToError(expression);
                builder.bindLabel(skipFinallyToErrorBlock);

                finallyBlockGenerator.generate();
            }

            List<KtExpression> branches = new ArrayList<KtExpression>();
            branches.add(expression.getTryBlock());
            for (KtCatchClause catchClause : expression.getCatchClauses()) {
                branches.add(catchClause.getCatchBody());
            }
            mergeValues(branches, expression);
        }

        // Returns label for 'finally' block
        @Nullable
        private Label generateTryAndCatches(@NotNull KtTryExpression expression) {
            List<KtCatchClause> catchClauses = expression.getCatchClauses();
            boolean hasCatches = !catchClauses.isEmpty();

            Label onException = null;
            if (hasCatches) {
                onException = builder.createUnboundLabel("onException");
                builder.nondeterministicJump(onException, expression, null);
            }

            Label onExceptionToFinallyBlock = null;
            if (expression.getFinallyBlock() != null) {
                onExceptionToFinallyBlock = builder.createUnboundLabel("onExceptionToFinallyBlock");
                builder.nondeterministicJump(onExceptionToFinallyBlock, expression, null);
            }

            KtBlockExpression tryBlock = expression.getTryBlock();
            generateInstructions(tryBlock);

            if (hasCatches) {
                Label afterCatches = builder.createUnboundLabel("afterCatches");
                builder.jump(afterCatches, expression);

                builder.bindLabel(onException);
                LinkedList<Label> catchLabels = Lists.newLinkedList();
                int catchClausesSize = catchClauses.size();
                for (int i = 0; i < catchClausesSize - 1; i++) {
                    catchLabels.add(builder.createUnboundLabel("catch " + i));
                }
                if (!catchLabels.isEmpty()) {
                    builder.nondeterministicJump(catchLabels, expression);
                }
                boolean isFirst = true;
                for (KtCatchClause catchClause : catchClauses) {
                    builder.enterLexicalScope(catchClause);
                    if (!isFirst) {
                        builder.bindLabel(catchLabels.remove());
                    }
                    else {
                        isFirst = false;
                    }
                    KtParameter catchParameter = catchClause.getCatchParameter();
                    if (catchParameter != null) {
                        builder.declareParameter(catchParameter);
                        generateInitializer(catchParameter, createSyntheticValue(catchParameter, MagicKind.FAKE_INITIALIZER));
                    }
                    KtExpression catchBody = catchClause.getCatchBody();
                    if (catchBody != null) {
                        generateInstructions(catchBody);
                    }
                    builder.jump(afterCatches, expression);
                    builder.exitLexicalScope(catchClause);
                }

                builder.bindLabel(afterCatches);
            }

            return onExceptionToFinallyBlock;
        }

        @Override
        public void visitWhileExpression(@NotNull KtWhileExpression expression) {
            LoopInfo loopInfo = builder.enterLoop(expression);

            builder.bindLabel(loopInfo.getConditionEntryPoint());
            KtExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition);
            }
            mark(expression);
            boolean conditionIsTrueConstant = CompileTimeConstantUtils.canBeReducedToBooleanConstant(condition, trace, true);
            if (!conditionIsTrueConstant) {
                builder.jumpOnFalse(loopInfo.getExitPoint(), expression, builder.getBoundValue(condition));
            }
            else {
                assert condition != null : "Invalid while condition: " + expression.getText();
                createSyntheticValue(condition, MagicKind.VALUE_CONSUMER, condition);
            }

            builder.enterLoopBody(expression);
            KtExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body);
            }
            builder.jump(loopInfo.getEntryPoint(), expression);
            builder.exitLoopBody(expression);
            builder.bindLabel(loopInfo.getExitPoint());
            builder.loadUnit(expression);
        }

        @Override
        public void visitDoWhileExpression(@NotNull KtDoWhileExpression expression) {
            builder.enterLexicalScope(expression);
            mark(expression);
            LoopInfo loopInfo = builder.enterLoop(expression);

            builder.enterLoopBody(expression);
            KtExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body);
            }
            builder.exitLoopBody(expression);
            builder.bindLabel(loopInfo.getConditionEntryPoint());
            KtExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition);
            }
            builder.jumpOnTrue(loopInfo.getEntryPoint(), expression, builder.getBoundValue(condition));
            builder.bindLabel(loopInfo.getExitPoint());
            builder.loadUnit(expression);
            builder.exitLexicalScope(expression);
        }

        @Override
        public void visitForExpression(@NotNull KtForExpression expression) {
            builder.enterLexicalScope(expression);

            KtExpression loopRange = expression.getLoopRange();
            if (loopRange != null) {
                generateInstructions(loopRange);
            }
            declareLoopParameter(expression);

            // TODO : primitive cases
            LoopInfo loopInfo = builder.enterLoop(expression);

            builder.bindLabel(loopInfo.getConditionEntryPoint());
            builder.nondeterministicJump(loopInfo.getExitPoint(), expression, null);


            writeLoopParameterAssignment(expression);

            mark(expression);
            builder.enterLoopBody(expression);
            KtExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body);
            }
            builder.jump(loopInfo.getEntryPoint(), expression);

            builder.exitLoopBody(expression);
            builder.bindLabel(loopInfo.getExitPoint());
            builder.loadUnit(expression);
            builder.exitLexicalScope(expression);
        }

        private void declareLoopParameter(KtForExpression expression) {
            KtParameter loopParameter = expression.getLoopParameter();
            KtMultiDeclaration multiDeclaration = expression.getMultiParameter();
            if (loopParameter != null) {
                builder.declareParameter(loopParameter);
            }
            else if (multiDeclaration != null) {
                visitMultiDeclaration(multiDeclaration, false);
            }
        }

        private void writeLoopParameterAssignment(KtForExpression expression) {
            KtParameter loopParameter = expression.getLoopParameter();
            KtMultiDeclaration multiDeclaration = expression.getMultiParameter();
            KtExpression loopRange = expression.getLoopRange();

            PseudoValue value = builder.magic(
                    loopRange != null ? loopRange : expression,
                    null,
                    ContainerUtil.createMaybeSingletonList(builder.getBoundValue(loopRange)),
                    MagicKind.LOOP_RANGE_ITERATION
            ).getOutputValue();

            if (loopParameter != null) {
                generateInitializer(loopParameter, value);
            }
            else if (multiDeclaration != null) {
                for (KtMultiDeclarationEntry entry : multiDeclaration.getEntries()) {
                    generateInitializer(entry, value);
                }
            }
        }

        @Override
        public void visitBreakExpression(@NotNull KtBreakExpression expression) {
            KtElement loop = getCorrespondingLoop(expression);
            if (loop != null) {
                checkJumpDoesNotCrossFunctionBoundary(expression, loop);
                builder.jump(builder.getExitPoint(loop), expression);
            }
        }

        @Override
        public void visitContinueExpression(@NotNull KtContinueExpression expression) {
            KtElement loop = getCorrespondingLoop(expression);
            if (loop != null) {
                checkJumpDoesNotCrossFunctionBoundary(expression, loop);
                builder.jump(builder.getConditionEntryPoint(loop), expression);
            }
        }

        @Nullable
        private KtElement getCorrespondingLoop(KtExpressionWithLabel expression) {
            String labelName = expression.getLabelName();
            KtLoopExpression loop;
            if (labelName != null) {
                KtSimpleNameExpression targetLabel = expression.getTargetLabel();
                assert targetLabel != null;
                PsiElement labeledElement = trace.get(BindingContext.LABEL_TARGET, targetLabel);
                if (labeledElement instanceof KtLoopExpression) {
                    loop = (KtLoopExpression) labeledElement;
                }
                else {
                    trace.report(NOT_A_LOOP_LABEL.on(expression, targetLabel.getText()));
                    loop = null;
                }
            }
            else {
                loop = builder.getCurrentLoop();
                if (loop == null) {
                    trace.report(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP.on(expression));
                } else {
                    KtWhenExpression whenExpression = PsiTreeUtil.getParentOfType(expression, KtWhenExpression.class, true,
                                                                                  KtLoopExpression.class);
                    if (whenExpression != null) {
                        trace.report(BREAK_OR_CONTINUE_IN_WHEN.on(expression));
                    }
                }
            }
            if (loop != null && loop.getBody() != null
                    // the faster version of 'isAncestor' check:
                    && !loop.getBody().getTextRange().contains(expression.getTextRange())) {
                trace.report(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP.on(expression));
                return null;
            }
            return loop;
        }

        private void checkJumpDoesNotCrossFunctionBoundary(@NotNull KtExpressionWithLabel jumpExpression, @NotNull KtElement jumpTarget) {
            BindingContext bindingContext = trace.getBindingContext();

            FunctionDescriptor labelExprEnclosingFunc = BindingContextUtils.getEnclosingFunctionDescriptor(bindingContext, jumpExpression);
            FunctionDescriptor labelTargetEnclosingFunc = BindingContextUtils.getEnclosingFunctionDescriptor(bindingContext, jumpTarget);
            if (labelExprEnclosingFunc != labelTargetEnclosingFunc) {
                trace.report(BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY.on(jumpExpression));
            }
        }

        @Override
        public void visitReturnExpression(@NotNull KtReturnExpression expression) {
            KtExpression returnedExpression = expression.getReturnedExpression();
            if (returnedExpression != null) {
                generateInstructions(returnedExpression);
            }
            KtSimpleNameExpression labelElement = expression.getTargetLabel();
            KtElement subroutine;
            String labelName = expression.getLabelName();
            if (labelElement != null && labelName != null) {
                PsiElement labeledElement = trace.get(BindingContext.LABEL_TARGET, labelElement);
                if (labeledElement != null) {
                    assert labeledElement instanceof KtElement;
                    subroutine = (KtElement) labeledElement;
                }
                else {
                    subroutine = null;
                }
            }
            else {
                subroutine = builder.getReturnSubroutine();
                // TODO : a context check
            }

            if (subroutine instanceof KtFunction || subroutine instanceof KtPropertyAccessor) {
                PseudoValue returnValue = returnedExpression != null ? builder.getBoundValue(returnedExpression) : null;
                if (returnValue == null) {
                    builder.returnNoValue(expression, subroutine);
                }
                else {
                    builder.returnValue(expression, returnValue, subroutine);
                }
            }
            else {
                createNonSyntheticValue(expression, MagicKind.UNSUPPORTED_ELEMENT, returnedExpression);
            }
        }

        @Override
        public void visitParameter(@NotNull KtParameter parameter) {
            builder.declareParameter(parameter);
            KtExpression defaultValue = parameter.getDefaultValue();
            if (defaultValue != null) {
                Label skipDefaultValue = builder.createUnboundLabel("after default value for parameter " + parameter.getName());
                builder.nondeterministicJump(skipDefaultValue, defaultValue, null);
                generateInstructions(defaultValue);
                builder.bindLabel(skipDefaultValue);
            }
            generateInitializer(parameter, computePseudoValueForParameter(parameter));
        }

        @NotNull
        private PseudoValue computePseudoValueForParameter(@NotNull KtParameter parameter) {
            PseudoValue syntheticValue = createSyntheticValue(parameter, MagicKind.FAKE_INITIALIZER);
            PseudoValue defaultValue = builder.getBoundValue(parameter.getDefaultValue());
            if (defaultValue == null) {
                return syntheticValue;
            }
            return builder.merge(parameter, Lists.newArrayList(defaultValue, syntheticValue)).getOutputValue();
        }

        @Override
        public void visitBlockExpression(@NotNull KtBlockExpression expression) {
            boolean declareLexicalScope = !isBlockInDoWhile(expression);
            if (declareLexicalScope) {
                builder.enterLexicalScope(expression);
            }
            mark(expression);
            List<KtExpression> statements = expression.getStatements();
            for (KtExpression statement : statements) {
                generateInstructions(statement);
            }
            if (statements.isEmpty()) {
                builder.loadUnit(expression);
            }
            else {
                copyValue(CollectionsKt.lastOrNull(statements), expression);
            }
            if (declareLexicalScope) {
                builder.exitLexicalScope(expression);
            }
        }

        private boolean isBlockInDoWhile(@NotNull KtBlockExpression expression) {
            PsiElement parent = expression.getParent();
            if (parent == null) return false;
            return parent.getParent() instanceof KtDoWhileExpression;
        }

        private void visitFunction(@NotNull KtFunction function) {
            processLocalDeclaration(function);
            boolean isAnonymousFunction = function instanceof KtFunctionLiteral || function.getName() == null;
            if (isAnonymousFunction || (function.isLocal() && !(function.getParent() instanceof KtBlockExpression))) {
                builder.createLambda(function);
            }
        }

        @Override
        public void visitNamedFunction(@NotNull KtNamedFunction function) {
            visitFunction(function);
        }

        @Override
        public void visitFunctionLiteralExpression(@NotNull KtFunctionLiteralExpression expression) {
            mark(expression);
            KtFunctionLiteral functionLiteral = expression.getFunctionLiteral();
            visitFunction(functionLiteral);
            copyValue(functionLiteral, expression);
        }

        @Override
        public void visitQualifiedExpression(@NotNull KtQualifiedExpression expression) {
            mark(expression);
            KtExpression selectorExpression = expression.getSelectorExpression();
            KtExpression receiverExpression = expression.getReceiverExpression();

            // todo: replace with selectorExpresion != null after parser is fixed
            if (selectorExpression instanceof KtCallExpression || selectorExpression instanceof KtSimpleNameExpression) {
                generateInstructions(selectorExpression);
                copyValue(selectorExpression, expression);
            }
            else {
                generateInstructions(receiverExpression);
                createNonSyntheticValue(expression, MagicKind.UNSUPPORTED_ELEMENT, receiverExpression);
            }
        }

        @Override
        public void visitCallExpression(@NotNull KtCallExpression expression) {
            if (!generateCall(expression)) {
                List<KtExpression> inputExpressions = new ArrayList<KtExpression>();
                for (ValueArgument argument : expression.getValueArguments()) {
                    KtExpression argumentExpression = argument.getArgumentExpression();
                    if (argumentExpression != null) {
                        generateInstructions(argumentExpression);
                        inputExpressions.add(argumentExpression);
                    }
                }
                KtExpression calleeExpression = expression.getCalleeExpression();
                generateInstructions(calleeExpression);
                inputExpressions.add(calleeExpression);
                inputExpressions.add(generateAndGetReceiverIfAny(expression));

                mark(expression);
                createNonSyntheticValue(expression, inputExpressions, MagicKind.UNRESOLVED_CALL);
            }
        }

        @Nullable
        private KtExpression generateAndGetReceiverIfAny(KtExpression expression) {
            PsiElement parent = expression.getParent();
            if (!(parent instanceof KtQualifiedExpression)) return null;

            KtQualifiedExpression qualifiedExpression = (KtQualifiedExpression) parent;
            if (qualifiedExpression.getSelectorExpression() != expression) return null;

            KtExpression receiverExpression = qualifiedExpression.getReceiverExpression();
            generateInstructions(receiverExpression);

            return receiverExpression;
        }

        @Override
        public void visitProperty(@NotNull KtProperty property) {
            builder.declareVariable(property);
            KtExpression initializer = property.getInitializer();
            if (initializer != null) {
                visitAssignment(property, getDeferredValue(initializer), property);
            }
            KtExpression delegate = property.getDelegateExpression();
            if (delegate != null) {
                generateInstructions(delegate);
                if (builder.getBoundValue(delegate) != null) {
                    createSyntheticValue(property, MagicKind.VALUE_CONSUMER, delegate);
                }
            }

            if (KtPsiUtil.isLocal(property)) {
                for (KtPropertyAccessor accessor : property.getAccessors()) {
                    generateInstructions(accessor);
                }
            }
        }

        @Override
        public void visitMultiDeclaration(@NotNull KtMultiDeclaration declaration) {
            visitMultiDeclaration(declaration, true);
        }

        private void visitMultiDeclaration(@NotNull KtMultiDeclaration declaration, boolean generateWriteForEntries) {
            KtExpression initializer = declaration.getInitializer();
            generateInstructions(initializer);
            for (KtMultiDeclarationEntry entry : declaration.getEntries()) {
                builder.declareVariable(entry);

                ResolvedCall<FunctionDescriptor> resolvedCall = trace.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);

                PseudoValue writtenValue;
                if (resolvedCall != null) {
                    writtenValue = builder.call(
                            entry,
                            resolvedCall,
                            getReceiverValues(resolvedCall),
                            Collections.<PseudoValue, ValueParameterDescriptor>emptyMap()
                    ).getOutputValue();
                }
                else {
                    writtenValue = createSyntheticValue(entry, MagicKind.UNRESOLVED_CALL, initializer);
                }

                if (generateWriteForEntries) {
                    generateInitializer(entry, writtenValue != null ? writtenValue : createSyntheticValue(entry, MagicKind.FAKE_INITIALIZER));
                }
            }
        }

        @Override
        public void visitPropertyAccessor(@NotNull KtPropertyAccessor accessor) {
            processLocalDeclaration(accessor);
        }

        @Override
        public void visitBinaryWithTypeRHSExpression(@NotNull KtBinaryExpressionWithTypeRHS expression) {
            mark(expression);

            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            KtExpression left = expression.getLeft();
            if (operationType == KtTokens.AS_KEYWORD || operationType == KtTokens.AS_SAFE) {
                generateInstructions(left);
                if (getBoundOrUnreachableValue(left) != null) {
                    createNonSyntheticValue(expression, MagicKind.CAST, left);
                }
            }
            else {
                visitJetElement(expression);
                createNonSyntheticValue(expression, MagicKind.UNSUPPORTED_ELEMENT, left);
            }
        }

        @Override
        public void visitThrowExpression(@NotNull KtThrowExpression expression) {
            mark(expression);

            KtExpression thrownExpression = expression.getThrownExpression();
            if (thrownExpression == null) return;

            generateInstructions(thrownExpression);

            PseudoValue thrownValue = builder.getBoundValue(thrownExpression);
            if (thrownValue == null) return;

            builder.throwException(expression, thrownValue);
        }

        @Override
        public void visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression) {
            generateArrayAccess(expression, trace.get(BindingContext.INDEXED_LVALUE_GET, expression));
        }

        @Override
        public void visitIsExpression(@NotNull KtIsExpression expression) {
            mark(expression);
            KtExpression left = expression.getLeftHandSide();
            generateInstructions(left);
            createNonSyntheticValue(expression, MagicKind.IS, left);
        }

        @Override
        public void visitWhenExpression(@NotNull KtWhenExpression expression) {
            mark(expression);

            KtExpression subjectExpression = expression.getSubjectExpression();
            if (subjectExpression != null) {
                generateInstructions(subjectExpression);
            }

            List<KtExpression> branches = new ArrayList<KtExpression>();

            Label doneLabel = builder.createUnboundLabel("after 'when' expression");

            Label nextLabel = null;
            for (Iterator<KtWhenEntry> iterator = expression.getEntries().iterator(); iterator.hasNext(); ) {
                KtWhenEntry whenEntry = iterator.next();
                mark(whenEntry);

                boolean isElse = whenEntry.isElse();
                if (isElse) {
                    if (iterator.hasNext()) {
                        trace.report(ELSE_MISPLACED_IN_WHEN.on(whenEntry));
                    }
                }
                Label bodyLabel = builder.createUnboundLabel("'when' entry body");

                KtWhenCondition[] conditions = whenEntry.getConditions();
                for (int i = 0; i < conditions.length; i++) {
                    KtWhenCondition condition = conditions[i];
                    condition.accept(conditionVisitor);
                    if (i + 1 < conditions.length) {
                        builder.nondeterministicJump(bodyLabel, expression, builder.getBoundValue(condition));
                    }
                }

                if (!isElse) {
                    nextLabel = builder.createUnboundLabel("next 'when' entry");
                    KtWhenCondition lastCondition = ArraysKt.lastOrNull(conditions);
                    builder.nondeterministicJump(nextLabel, expression, builder.getBoundValue(lastCondition));
                }

                builder.bindLabel(bodyLabel);
                KtExpression whenEntryExpression = whenEntry.getExpression();
                if (whenEntryExpression != null) {
                    generateInstructions(whenEntryExpression);
                    branches.add(whenEntryExpression);
                }
                builder.jump(doneLabel, expression);

                if (!isElse) {
                    builder.bindLabel(nextLabel);
                    // For the last entry of exhaustive when,
                    // attempt to jump further should lead to error, not to "done"
                    if (!iterator.hasNext() && WhenChecker.isWhenExhaustive(expression, trace)) {
                        builder.jumpToError(expression);
                    }
                }
            }
            builder.bindLabel(doneLabel);

            mergeValues(branches, expression);
        }

        @Override
        public void visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expression) {
            mark(expression);
            KtObjectDeclaration declaration = expression.getObjectDeclaration();
            generateInstructions(declaration);

            builder.createAnonymousObject(expression);
        }

        @Override
        public void visitObjectDeclaration(@NotNull KtObjectDeclaration objectDeclaration) {
            generateHeaderDelegationSpecifiers(objectDeclaration);
            generateClassOrObjectInitializers(objectDeclaration);
            generateDeclarationForLocalClassOrObjectIfNeeded(objectDeclaration);
        }

        @Override
        public void visitStringTemplateExpression(@NotNull KtStringTemplateExpression expression) {
            mark(expression);

            List<KtExpression> inputExpressions = new ArrayList<KtExpression>();
            for (KtStringTemplateEntry entry : expression.getEntries()) {
                if (entry instanceof KtStringTemplateEntryWithExpression) {
                    KtExpression entryExpression = entry.getExpression();
                    generateInstructions(entryExpression);
                    inputExpressions.add(entryExpression);
                }
            }
            builder.loadStringTemplate(expression, elementsToValues(inputExpressions));
        }

        @Override
        public void visitTypeProjection(@NotNull KtTypeProjection typeProjection) {
            // TODO : Support Type Arguments. Companion object may be initialized at this point");
        }

        @Override
        public void visitAnonymousInitializer(@NotNull KtClassInitializer classInitializer) {
            generateInstructions(classInitializer.getBody());
        }

        private void generateHeaderDelegationSpecifiers(@NotNull KtClassOrObject classOrObject) {
            for (KtDelegationSpecifier specifier : classOrObject.getDelegationSpecifiers()) {
                generateInstructions(specifier);
            }
        }

        private void generateClassOrObjectInitializers(@NotNull KtClassOrObject classOrObject) {
            for (KtDeclaration declaration : classOrObject.getDeclarations()) {
                if (declaration instanceof KtProperty || declaration instanceof KtClassInitializer) {
                    generateInstructions(declaration);
                }
            }
        }

        @Override
        public void visitClass(@NotNull KtClass klass) {
            if (klass.hasPrimaryConstructor()) {
                processParameters(klass.getPrimaryConstructorParameters());

                // delegation specifiers of primary constructor, anonymous class and property initializers
                generateHeaderDelegationSpecifiers(klass);
                generateClassOrObjectInitializers(klass);
            }

            generateDeclarationForLocalClassOrObjectIfNeeded(klass);
        }

        private void generateDeclarationForLocalClassOrObjectIfNeeded(@NotNull KtClassOrObject classOrObject) {
            if (classOrObject.isLocal()) {
                for (KtDeclaration declaration : classOrObject.getDeclarations()) {
                    if (declaration instanceof KtSecondaryConstructor ||
                        declaration instanceof KtProperty ||
                        declaration instanceof KtClassInitializer) {
                        continue;
                    }
                    generateInstructions(declaration);
                }
            }
        }

        private void processParameters(@NotNull List<KtParameter> parameters) {
            for (KtParameter parameter : parameters) {
                generateInstructions(parameter);
            }
        }

        @Override
        public void visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor) {
            KtClassOrObject classOrObject = PsiTreeUtil.getParentOfType(constructor, KtClassOrObject.class);
            assert classOrObject != null : "Guaranteed by parsing contract";

            processParameters(constructor.getValueParameters());
            generateCallOrMarkUnresolved(constructor.getDelegationCall());

            if (!constructor.getDelegationCall().isCallToThis()) {
                generateClassOrObjectInitializers(classOrObject);
            }

            generateInstructions(constructor.getBodyExpression());
        }

        @Override
        public void visitDelegationToSuperCallSpecifier(@NotNull KtDelegatorToSuperCall call) {
            generateCallOrMarkUnresolved(call);
        }

        private void generateCallOrMarkUnresolved(@Nullable KtCallElement call) {
            if (call == null) return;
            if (!generateCall(call)) {
                List<KtExpression> arguments = CollectionsKt.map(
                        call.getValueArguments(),
                        new Function1<ValueArgument, KtExpression>() {
                            @Override
                            public KtExpression invoke(ValueArgument valueArgument) {
                                return valueArgument.getArgumentExpression();
                            }
                        }
                );

                for (KtExpression argument : arguments) {
                    generateInstructions(argument);
                }
                createNonSyntheticValue(call, arguments, MagicKind.UNRESOLVED_CALL);
            }
        }

        @Override
        public void visitDelegationByExpressionSpecifier(@NotNull KtDelegatorByExpressionSpecifier specifier) {
            KtExpression delegateExpression = specifier.getDelegateExpression();
            generateInstructions(delegateExpression);
            createSyntheticValue(specifier, MagicKind.VALUE_CONSUMER, delegateExpression);
        }

        @Override
        public void visitDelegationToSuperClassSpecifier(@NotNull KtDelegatorToSuperClass specifier) {
            // Do not generate UNSUPPORTED_ELEMENT here
        }

        @Override
        public void visitDelegationSpecifierList(@NotNull KtDelegationSpecifierList list) {
            list.acceptChildren(this);
        }

        @Override
        public void visitJetFile(@NotNull KtFile file) {
            for (KtDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof KtProperty) {
                    generateInstructions(declaration);
                }
            }
        }

        @Override
        public void visitDoubleColonExpression(@NotNull KtDoubleColonExpression expression) {
            mark(expression);
            createNonSyntheticValue(expression, MagicKind.CALLABLE_REFERENCE);
        }

        @Override
        public void visitJetElement(@NotNull KtElement element) {
            createNonSyntheticValue(element, MagicKind.UNSUPPORTED_ELEMENT);
        }

        private boolean generateCall(@Nullable KtElement callElement) {
            if (callElement == null) return false;
            return checkAndGenerateCall(CallUtilKt.getResolvedCall(callElement, trace.getBindingContext()));
        }

        private boolean checkAndGenerateCall(@Nullable ResolvedCall<?> resolvedCall) {
            if (resolvedCall == null) return false;
            generateCall(resolvedCall);
            return true;
        }

        @NotNull
        private InstructionWithValue generateCall(@NotNull ResolvedCall<?> resolvedCall) {
            KtElement callElement = resolvedCall.getCall().getCallElement();

            Map<PseudoValue, ReceiverValue> receivers = getReceiverValues(resolvedCall);

            SmartFMap<PseudoValue, ValueParameterDescriptor> parameterValues = SmartFMap.emptyMap();
            for (ValueArgument argument : resolvedCall.getCall().getValueArguments()) {
                ArgumentMapping argumentMapping = resolvedCall.getArgumentMapping(argument);
                KtExpression argumentExpression = argument.getArgumentExpression();
                if (argumentMapping instanceof ArgumentMatch) {
                    parameterValues = generateValueArgument(argument, ((ArgumentMatch) argumentMapping).getValueParameter(), parameterValues);
                }
                else if (argumentExpression != null) {
                    generateInstructions(argumentExpression);
                    createSyntheticValue(argumentExpression, MagicKind.VALUE_CONSUMER, argumentExpression);
                }
            }

            if (resolvedCall.getResultingDescriptor() instanceof VariableDescriptor) {
                // If a callee of the call is just a variable (without 'invoke'), 'read variable' is generated.
                // todo : process arguments for such a case (KT-5387)
                KtExpression callExpression = callElement instanceof KtExpression ? (KtExpression) callElement : null;
                assert callExpression != null
                        : "Variable-based call without callee expression: " + callElement.getText();
                assert parameterValues.isEmpty()
                        : "Variable-based call with non-empty argument list: " + callElement.getText();
                return builder.readVariable(callExpression, resolvedCall, receivers);
            }

            mark(resolvedCall.getCall().getCallElement());
            return builder.call(callElement, resolvedCall, receivers, parameterValues);
        }

        @NotNull
        private Map<PseudoValue, ReceiverValue> getReceiverValues(ResolvedCall<?> resolvedCall) {
            PseudoValue varCallResult = null;
            ReceiverValue explicitReceiver = ReceiverValue.NO_RECEIVER;
            if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
                varCallResult = generateCall(((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall()).getOutputValue();

                ExplicitReceiverKind kind = resolvedCall.getExplicitReceiverKind();
                //noinspection EnumSwitchStatementWhichMissesCases
                switch (kind) {
                    case DISPATCH_RECEIVER:
                        explicitReceiver = resolvedCall.getDispatchReceiver();
                        break;
                    case EXTENSION_RECEIVER:
                    case BOTH_RECEIVERS:
                        explicitReceiver = resolvedCall.getExtensionReceiver();
                        break;
                }
            }

            SmartFMap<PseudoValue, ReceiverValue> receiverValues = SmartFMap.emptyMap();
            if (explicitReceiver.exists() && varCallResult != null) {
                receiverValues = receiverValues.plus(varCallResult, explicitReceiver);
            }
            KtElement callElement = resolvedCall.getCall().getCallElement();
            receiverValues = getReceiverValues(callElement, resolvedCall.getDispatchReceiver(), receiverValues);
            receiverValues = getReceiverValues(callElement, resolvedCall.getExtensionReceiver(), receiverValues);
            return receiverValues;
        }

        @NotNull
        private SmartFMap<PseudoValue, ReceiverValue> getReceiverValues(
                KtElement callElement,
                ReceiverValue receiver,
                SmartFMap<PseudoValue, ReceiverValue> receiverValues
        ) {
            if (!receiver.exists() || receiverValues.containsValue(receiver)) return receiverValues;

            if (receiver instanceof ThisReceiver) {
                receiverValues = receiverValues.plus(createSyntheticValue(callElement, MagicKind.IMPLICIT_RECEIVER), receiver);
            }
            else if (receiver instanceof ExpressionReceiver) {
                KtExpression expression = ((ExpressionReceiver) receiver).getExpression();
                if (builder.getBoundValue(expression) == null) {
                    generateInstructions(expression);
                }

                PseudoValue receiverPseudoValue = getBoundOrUnreachableValue(expression);
                if (receiverPseudoValue != null) {
                    receiverValues = receiverValues.plus(receiverPseudoValue, receiver);
                }
            }
            else if (receiver instanceof TransientReceiver) {
                // Do nothing
            }
            else {
                throw new IllegalArgumentException("Unknown receiver kind: " + receiver);
            }

            return receiverValues;
        }

        @NotNull
        private SmartFMap<PseudoValue, ValueParameterDescriptor> generateValueArgument(
                ValueArgument valueArgument,
                ValueParameterDescriptor parameterDescriptor,
                SmartFMap<PseudoValue, ValueParameterDescriptor> parameterValues) {
            KtExpression expression = valueArgument.getArgumentExpression();
            if (expression != null) {
                if (!valueArgument.isExternal()) {
                    generateInstructions(expression);
                }

                PseudoValue argValue = getBoundOrUnreachableValue(expression);
                if (argValue != null) {
                    parameterValues = parameterValues.plus(argValue, parameterDescriptor);
                }
            }
            return parameterValues;
        }
    }
}
