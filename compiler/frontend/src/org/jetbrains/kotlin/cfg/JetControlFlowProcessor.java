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
import kotlin.Function0;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cfg.pseudocode.*;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.AccessTarget;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.InstructionWithValue;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MagicKind;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.lexer.JetToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.model.*;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.OperatorConventions;

import java.util.*;

import static org.jetbrains.kotlin.cfg.JetControlFlowBuilder.PredefinedOperation.*;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.lexer.JetTokens.*;
import static org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage.getResolvedCall;

public class JetControlFlowProcessor {

    private final JetControlFlowBuilder builder;
    private final BindingTrace trace;

    public JetControlFlowProcessor(BindingTrace trace) {
        this.builder = new JetControlFlowInstructionsGenerator();
        this.trace = trace;
    }

    @NotNull
    public Pseudocode generatePseudocode(@NotNull JetElement subroutine) {
        Pseudocode pseudocode = generate(subroutine);
        ((PseudocodeImpl) pseudocode).postProcess();
        return pseudocode;
    }

    @NotNull
    private Pseudocode generate(@NotNull JetElement subroutine) {
        builder.enterSubroutine(subroutine);
        CFPVisitor cfpVisitor = new CFPVisitor(builder);
        if (subroutine instanceof JetDeclarationWithBody && !(subroutine instanceof JetSecondaryConstructor)) {
            JetDeclarationWithBody declarationWithBody = (JetDeclarationWithBody) subroutine;
            List<JetParameter> valueParameters = declarationWithBody.getValueParameters();
            for (JetParameter valueParameter : valueParameters) {
                cfpVisitor.generateInstructions(valueParameter);
            }
            JetExpression bodyExpression = declarationWithBody.getBodyExpression();
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

    private void generateImplicitReturnValue(@NotNull JetExpression bodyExpression, @NotNull JetElement subroutine) {
        CallableDescriptor subroutineDescriptor = (CallableDescriptor) trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, subroutine);
        if (subroutineDescriptor == null) return;

        JetType returnType = subroutineDescriptor.getReturnType();
        if (returnType != null && KotlinBuiltIns.isUnit(returnType) && subroutineDescriptor instanceof AnonymousFunctionDescriptor) return;

        PseudoValue returnValue = builder.getBoundValue(bodyExpression);
        if (returnValue == null) return;

        builder.returnValue(bodyExpression, returnValue, subroutine);
    }

    private void processLocalDeclaration(@NotNull JetDeclaration subroutine) {
        Label afterDeclaration = builder.createUnboundLabel("after local declaration");

        builder.nondeterministicJump(afterDeclaration, subroutine, null);
        generate(subroutine);
        builder.bindLabel(afterDeclaration);
    }

    private class CFPVisitor extends JetVisitorVoid {
        private final JetControlFlowBuilder builder;

        private final JetVisitorVoid conditionVisitor = new JetVisitorVoid() {

            private JetExpression getSubjectExpression(JetWhenCondition condition) {
                JetWhenExpression whenExpression = PsiTreeUtil.getParentOfType(condition, JetWhenExpression.class);
                return whenExpression != null ? whenExpression.getSubjectExpression() : null;
            }

            @Override
            public void visitWhenConditionInRange(@NotNull JetWhenConditionInRange condition) {
                if (!generateCall(condition.getOperationReference())) {
                    JetExpression rangeExpression = condition.getRangeExpression();
                    generateInstructions(rangeExpression);
                    createNonSyntheticValue(condition, MagicKind.UNRESOLVED_CALL, rangeExpression);
                }
            }

            @Override
            public void visitWhenConditionIsPattern(@NotNull JetWhenConditionIsPattern condition) {
                mark(condition);
                createNonSyntheticValue(condition, MagicKind.IS, getSubjectExpression(condition));
            }

            @Override
            public void visitWhenConditionWithExpression(@NotNull JetWhenConditionWithExpression condition) {
                mark(condition);

                JetExpression expression = condition.getExpression();
                generateInstructions(expression);

                JetExpression subjectExpression = getSubjectExpression(condition);
                if (subjectExpression != null) {
                    // todo: this can be replaced by equals() invocation (when corresponding resolved call is recorded)
                    createNonSyntheticValue(condition, MagicKind.EQUALS_IN_WHEN_CONDITION, subjectExpression, expression);
                }
                else {
                    copyValue(expression, condition);
                }
            }

            @Override
            public void visitJetElement(@NotNull JetElement element) {
                throw new UnsupportedOperationException("[JetControlFlowProcessor] " + element.toString());
            }
        };

        private CFPVisitor(@NotNull JetControlFlowBuilder builder) {
            this.builder = builder;
        }

        private void mark(JetElement element) {
            builder.mark(element);
        }

        public void generateInstructions(@Nullable JetElement element) {
            if (element == null) return;
            element.accept(this);
            checkNothingType(element);
        }

        private void checkNothingType(JetElement element) {
            if (!(element instanceof JetExpression)) return;

            JetExpression expression = JetPsiUtil.deparenthesize((JetExpression) element);
            if (expression == null) return;

            if (expression instanceof JetStatementExpression || expression instanceof JetTryExpression
                    || expression instanceof JetIfExpression || expression instanceof JetWhenExpression) {
                return;
            }

            JetType type = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
            if (type != null && KotlinBuiltIns.isNothing(type)) {
                builder.jumpToError(expression);
            }
        }

        @NotNull
        private PseudoValue createSyntheticValue(@NotNull JetElement instructionElement, @NotNull MagicKind kind, JetElement... from) {
            List<PseudoValue> values = elementsToValues(from.length > 0 ? Arrays.asList(from) : Collections.<JetElement>emptyList());
            return builder.magic(instructionElement, null, values, defaultTypeMap(values), kind).getOutputValue();
        }

        @NotNull
        private PseudoValue createNonSyntheticValue(
                @NotNull JetElement to, @NotNull List<? extends JetElement> from, @NotNull MagicKind kind
        ) {
            List<PseudoValue> values = elementsToValues(from);
            return builder.magic(to, to, values, defaultTypeMap(values), kind).getOutputValue();
        }

        @NotNull
        private PseudoValue createNonSyntheticValue(@NotNull JetElement to, @NotNull MagicKind kind, JetElement... from) {
            return createNonSyntheticValue(to, Arrays.asList(from), kind);
        }

        @NotNull
        private Map<PseudoValue, TypePredicate> defaultTypeMap(List<PseudoValue> values) {
            return PseudocodePackage.expectedTypeFor(AllTypes.INSTANCE$, values);
        }

        private void mergeValues(@NotNull List<JetExpression> from, @NotNull JetExpression to) {
            builder.merge(to, elementsToValues(from));
        }

        private void copyValue(@Nullable JetElement from, @NotNull JetElement to) {
            PseudoValue value = getBoundOrUnreachableValue(from);
            if (value != null) {
                builder.bindValue(value, to);
            }
        }

        @Nullable
        private PseudoValue getBoundOrUnreachableValue(@Nullable JetElement element) {
            if (element == null) return null;

            PseudoValue value = builder.getBoundValue(element);
            return value != null || element instanceof JetDeclaration ? value : builder.newValue(element);
        }

        private List<PseudoValue> elementsToValues(List<? extends JetElement> from) {
            if (from.isEmpty()) return Collections.emptyList();
            return KotlinPackage.filterNotNull(
                    KotlinPackage.map(
                            from,
                            new Function1<JetElement, PseudoValue>() {
                                @Override
                                public PseudoValue invoke(JetElement element) {
                                    return getBoundOrUnreachableValue(element);
                                }
                            }
                    )
            );
        }

        private void generateInitializer(@NotNull JetDeclaration declaration, @NotNull PseudoValue initValue) {
            builder.write(
                    declaration,
                    declaration,
                    initValue,
                    getDeclarationAccessTarget(declaration),
                    Collections.<PseudoValue, ReceiverValue>emptyMap()
            );
        }

        @NotNull
        private AccessTarget getResolvedCallAccessTarget(JetElement element) {
            ResolvedCall<?> resolvedCall = getResolvedCall(element, trace.getBindingContext());
            return resolvedCall != null ? new AccessTarget.Call(resolvedCall) : AccessTarget.BlackBox.INSTANCE$;
        }

        @NotNull
        private AccessTarget getDeclarationAccessTarget(JetElement element) {
            DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
            return descriptor instanceof VariableDescriptor
                   ? new AccessTarget.Declaration((VariableDescriptor) descriptor)
                   : AccessTarget.BlackBox.INSTANCE$;
        }

        @Override
        public void visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression) {
            mark(expression);
            JetExpression innerExpression = expression.getExpression();
            if (innerExpression != null) {
                generateInstructions(innerExpression);
                copyValue(innerExpression, expression);
            }
        }

        @Override
        public void visitAnnotatedExpression(@NotNull JetAnnotatedExpression expression) {
            JetExpression baseExpression = expression.getBaseExpression();
            if (baseExpression != null) {
                generateInstructions(baseExpression);
                copyValue(baseExpression, expression);
            }
        }

        @Override
        public void visitThisExpression(@NotNull JetThisExpression expression) {
            ResolvedCall<?> resolvedCall = getResolvedCall(expression, trace.getBindingContext());
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
        public void visitConstantExpression(@NotNull JetConstantExpression expression) {
            CompileTimeConstant<?> constant = trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
            builder.loadConstant(expression, constant);
        }

        @Override
        public void visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression) {
            ResolvedCall<?> resolvedCall = getResolvedCall(expression, trace.getBindingContext());
            if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
                VariableAsFunctionResolvedCall variableAsFunctionResolvedCall = (VariableAsFunctionResolvedCall) resolvedCall;
                generateCall(variableAsFunctionResolvedCall.getVariableCall());
            }
            else if (!generateCall(expression) && !(expression.getParent() instanceof JetCallExpression)) {
                createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL, generateAndGetReceiverIfAny(expression));
            }
        }

        @Override
        public void visitLabeledExpression(@NotNull JetLabeledExpression expression) {
            mark(expression);
            JetExpression baseExpression = expression.getBaseExpression();
            if (baseExpression != null) {
                generateInstructions(baseExpression);
                copyValue(baseExpression, expression);
            }
        }

        @SuppressWarnings("SuspiciousMethodCalls")
        @Override
        public void visitBinaryExpression(@NotNull JetBinaryExpression expression) {
            JetSimpleNameExpression operationReference = expression.getOperationReference();
            IElementType operationType = operationReference.getReferencedNameElementType();

            JetExpression left = expression.getLeft();
            JetExpression right = expression.getRight();
            if (operationType == ANDAND || operationType == OROR) {
                generateBooleanOperation(expression);
            }
            else if (operationType == EQ) {
                visitAssignment(left, getDeferredValue(right), expression);
            }
            else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
                ResolvedCall<?> resolvedCall = getResolvedCall(expression, trace.getBindingContext());
                if (resolvedCall != null) {
                    PseudoValue rhsValue = generateCall(resolvedCall).getOutputValue();
                    Name assignMethodName = OperatorConventions.getNameForOperationSymbol((JetToken) expression.getOperationToken());
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

        private void generateBooleanOperation(JetBinaryExpression expression) {
            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            JetExpression left = expression.getLeft();
            JetExpression right = expression.getRight();

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

        private Function0<PseudoValue> getDeferredValue(final JetExpression expression) {
            return new Function0<PseudoValue>() {
                @Override
                public PseudoValue invoke() {
                    generateInstructions(expression);
                    return getBoundOrUnreachableValue(expression);
                }
            };
        }

        private void generateBothArgumentsAndMark(JetBinaryExpression expression) {
            JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
            if (left != null) {
                generateInstructions(left);
            }
            JetExpression right = expression.getRight();
            if (right != null) {
                generateInstructions(right);
            }
            createNonSyntheticValue(expression, MagicKind.UNRESOLVED_CALL, left, right);
            mark(expression);
        }

        private void visitAssignment(
                JetExpression lhs,
                @NotNull Function0<PseudoValue> rhsDeferredValue,
                JetExpression parentExpression
        ) {
            JetExpression left = JetPsiUtil.deparenthesize(lhs);
            if (left == null) {
                List<PseudoValue> arguments = Collections.singletonList(rhsDeferredValue.invoke());
                builder.magic(parentExpression, parentExpression, arguments, defaultTypeMap(arguments), MagicKind.UNSUPPORTED_ELEMENT);
                return;
            }

            if (left instanceof JetArrayAccessExpression) {
                generateArrayAssignment((JetArrayAccessExpression) left, rhsDeferredValue, parentExpression);
                return;
            }

            Map<PseudoValue, ReceiverValue> receiverValues = SmartFMap.emptyMap();
            AccessTarget accessTarget = AccessTarget.BlackBox.INSTANCE$;
            if (left instanceof JetSimpleNameExpression || left instanceof JetQualifiedExpression) {
                accessTarget = getResolvedCallAccessTarget(PsiUtilPackage.getQualifiedElementSelector(left));
                if (accessTarget instanceof AccessTarget.Call) {
                    receiverValues = getReceiverValues(((AccessTarget.Call) accessTarget).getResolvedCall());
                }
            }
            else if (left instanceof JetProperty) {
                accessTarget = getDeclarationAccessTarget(left);
            }

            recordWrite(left, accessTarget, rhsDeferredValue.invoke(), receiverValues, parentExpression);
        }

        private void generateArrayAssignment(
                JetArrayAccessExpression lhs,
                @NotNull Function0<PseudoValue> rhsDeferredValue,
                @NotNull JetExpression parentExpression
        ) {
            ResolvedCall<FunctionDescriptor> setResolvedCall = trace.get(BindingContext.INDEXED_LVALUE_SET, lhs);

            if (setResolvedCall == null) {
                generateArrayAccess(lhs, null);

                List<PseudoValue> arguments = KotlinPackage.filterNotNull(
                        Arrays.asList(getBoundOrUnreachableValue(lhs), rhsDeferredValue.invoke())
                );
                builder.magic(parentExpression, parentExpression, arguments, defaultTypeMap(arguments), MagicKind.UNRESOLVED_CALL);

                return;
            }

            // In case of simple ('=') array assignment mark instruction is not generated yet, so we put it before generating "set" call
            if (((JetOperationExpression) parentExpression).getOperationReference().getReferencedNameElementType() == EQ) {
                mark(lhs);
            }

            generateInstructions(lhs.getArrayExpression());

            Map<PseudoValue, ReceiverValue> receiverValues = getReceiverValues(setResolvedCall);
            SmartFMap<PseudoValue, ValueParameterDescriptor> argumentValues =
                    getArraySetterArguments(rhsDeferredValue, setResolvedCall);

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
            List<ValueArgument> valueArguments = KotlinPackage.flatMapTo(
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

            ValueArgument rhsArgument = KotlinPackage.lastOrNull(valueArguments);
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

        private void recordWrite(
                @NotNull JetExpression left,
                @NotNull AccessTarget target,
                @Nullable PseudoValue rightValue,
                @NotNull Map<PseudoValue, ReceiverValue> receiverValues,
                @NotNull JetExpression parentExpression
        ) {
            if (target == AccessTarget.BlackBox.INSTANCE$) {
                List<PseudoValue> values = ContainerUtil.createMaybeSingletonList(rightValue);
                builder.magic(parentExpression, parentExpression, values, defaultTypeMap(values), MagicKind.UNSUPPORTED_ELEMENT);
            }
            else {
                PseudoValue rValue =
                        rightValue != null ? rightValue : createSyntheticValue(parentExpression, MagicKind.UNRECOGNIZED_WRITE_RHS);
                builder.write(parentExpression, left, rValue, target, receiverValues);
            }
        }

        private void generateArrayAccess(JetArrayAccessExpression arrayAccessExpression, @Nullable ResolvedCall<?> resolvedCall) {
            if (builder.getBoundValue(arrayAccessExpression) != null) return;
            mark(arrayAccessExpression);
            if (!checkAndGenerateCall(resolvedCall)) {
                generateArrayAccessWithoutCall(arrayAccessExpression);
            }
        }

        private void generateArrayAccessWithoutCall(JetArrayAccessExpression arrayAccessExpression) {
            createNonSyntheticValue(arrayAccessExpression, generateArrayAccessArguments(arrayAccessExpression), MagicKind.UNRESOLVED_CALL);
        }

        private List<JetExpression> generateArrayAccessArguments(JetArrayAccessExpression arrayAccessExpression) {
            List<JetExpression> inputExpressions = new ArrayList<JetExpression>();

            JetExpression arrayExpression = arrayAccessExpression.getArrayExpression();
            inputExpressions.add(arrayExpression);
            generateInstructions(arrayExpression);

            for (JetExpression index : arrayAccessExpression.getIndexExpressions()) {
                generateInstructions(index);
                inputExpressions.add(index);
            }

            return inputExpressions;
        }

        @Override
        public void visitUnaryExpression(@NotNull JetUnaryExpression expression) {
            JetSimpleNameExpression operationSign = expression.getOperationReference();
            IElementType operationType = operationSign.getReferencedNameElementType();
            JetExpression baseExpression = expression.getBaseExpression();
            if (baseExpression == null) return;
            if (JetTokens.EXCLEXCL == operationType) {
                generateInstructions(baseExpression);
                builder.predefinedOperation(expression, NOT_NULL_ASSERTION, elementsToValues(Collections.singletonList(baseExpression)));
                return;
            }

            boolean incrementOrDecrement = isIncrementOrDecrement(operationType);
            ResolvedCall<?> resolvedCall = getResolvedCall(expression, trace.getBindingContext());

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
                if (expression instanceof JetPostfixExpression) {
                    copyValue(baseExpression, expression);
                }
            }
        }

        private boolean isIncrementOrDecrement(IElementType operationType) {
            return operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS;
        }

        @Override
        public void visitIfExpression(@NotNull JetIfExpression expression) {
            mark(expression);
            List<JetExpression> branches = new ArrayList<JetExpression>(2);
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition);
            }
            Label elseLabel = builder.createUnboundLabel("else branch");
            builder.jumpOnFalse(elseLabel, expression, builder.getBoundValue(condition));
            JetExpression thenBranch = expression.getThen();
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
            JetExpression elseBranch = expression.getElse();
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
            private final JetFinallySection finallyBlock;
            private Label startFinally = null;
            private Label finishFinally = null;

            private FinallyBlockGenerator(JetFinallySection block) {
                finallyBlock = block;
            }

            public void generate() {
                JetBlockExpression finalExpression = finallyBlock.getFinalExpression();
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
        public void visitTryExpression(@NotNull JetTryExpression expression) {
            mark(expression);

            JetFinallySection finallyBlock = expression.getFinallyBlock();
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

            List<JetExpression> branches = new ArrayList<JetExpression>();
            branches.add(expression.getTryBlock());
            for (JetCatchClause catchClause : expression.getCatchClauses()) {
                branches.add(catchClause.getCatchBody());
            }
            mergeValues(branches, expression);
        }

        // Returns label for 'finally' block
        @Nullable
        private Label generateTryAndCatches(@NotNull JetTryExpression expression) {
            List<JetCatchClause> catchClauses = expression.getCatchClauses();
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

            JetBlockExpression tryBlock = expression.getTryBlock();
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
                for (JetCatchClause catchClause : catchClauses) {
                    builder.enterLexicalScope(catchClause);
                    if (!isFirst) {
                        builder.bindLabel(catchLabels.remove());
                    }
                    else {
                        isFirst = false;
                    }
                    JetParameter catchParameter = catchClause.getCatchParameter();
                    if (catchParameter != null) {
                        builder.declareParameter(catchParameter);
                        generateInitializer(catchParameter, createSyntheticValue(catchParameter, MagicKind.FAKE_INITIALIZER));
                    }
                    JetExpression catchBody = catchClause.getCatchBody();
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
        public void visitWhileExpression(@NotNull JetWhileExpression expression) {
            LoopInfo loopInfo = builder.enterLoop(expression);

            builder.bindLabel(loopInfo.getConditionEntryPoint());
            JetExpression condition = expression.getCondition();
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
                List<PseudoValue> values = ContainerUtil.createMaybeSingletonList(builder.getBoundValue(condition));
                Map<PseudoValue, TypePredicate> typePredicates =
                        PseudocodePackage.expectedTypeFor(new SingleType(KotlinBuiltIns.getInstance().getBooleanType()), values);
                builder.magic(condition, null, values, typePredicates, MagicKind.VALUE_CONSUMER);
            }

            builder.enterLoopBody(expression);
            JetExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body);
            }
            builder.jump(loopInfo.getEntryPoint(), expression);
            builder.exitLoopBody(expression);
            builder.bindLabel(loopInfo.getExitPoint());
            builder.loadUnit(expression);
        }

        @Override
        public void visitDoWhileExpression(@NotNull JetDoWhileExpression expression) {
            builder.enterLexicalScope(expression);
            mark(expression);
            LoopInfo loopInfo = builder.enterLoop(expression);

            builder.enterLoopBody(expression);
            JetExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body);
            }
            builder.exitLoopBody(expression);
            builder.bindLabel(loopInfo.getConditionEntryPoint());
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition);
            }
            builder.jumpOnTrue(loopInfo.getEntryPoint(), expression, builder.getBoundValue(condition));
            builder.bindLabel(loopInfo.getExitPoint());
            builder.loadUnit(expression);
            builder.exitLexicalScope(expression);
        }

        @Override
        public void visitForExpression(@NotNull JetForExpression expression) {
            builder.enterLexicalScope(expression);

            JetExpression loopRange = expression.getLoopRange();
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
            JetExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body);
            }
            builder.jump(loopInfo.getEntryPoint(), expression);

            builder.exitLoopBody(expression);
            builder.bindLabel(loopInfo.getExitPoint());
            builder.loadUnit(expression);
            builder.exitLexicalScope(expression);
        }

        private void declareLoopParameter(JetForExpression expression) {
            JetParameter loopParameter = expression.getLoopParameter();
            JetMultiDeclaration multiDeclaration = expression.getMultiParameter();
            if (loopParameter != null) {
                builder.declareParameter(loopParameter);
            }
            else if (multiDeclaration != null) {
                visitMultiDeclaration(multiDeclaration, false);
            }
        }

        private void writeLoopParameterAssignment(JetForExpression expression) {
            JetParameter loopParameter = expression.getLoopParameter();
            JetMultiDeclaration multiDeclaration = expression.getMultiParameter();
            JetExpression loopRange = expression.getLoopRange();

            TypePredicate loopRangeTypePredicate =
                    getTypePredicateByReceiverValue(trace.get(BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL, loopRange));

            PseudoValue loopRangeValue = builder.getBoundValue(loopRange);
            PseudoValue value = builder.magic(
                    loopRange != null ? loopRange : expression,
                    null,
                    ContainerUtil.createMaybeSingletonList(loopRangeValue),
                    loopRangeValue != null
                        ? Collections.singletonMap(loopRangeValue, loopRangeTypePredicate)
                        : Collections.<PseudoValue, TypePredicate>emptyMap(),
                    MagicKind.LOOP_RANGE_ITERATION
            ).getOutputValue();

            if (loopParameter != null) {
                generateInitializer(loopParameter, value);
            }
            else if (multiDeclaration != null) {
                for (JetMultiDeclarationEntry entry : multiDeclaration.getEntries()) {
                    generateInitializer(entry, value);
                }
            }
        }

        private ReceiverValue getExplicitReceiverValue(ResolvedCall<?> resolvedCall) {
            switch(resolvedCall.getExplicitReceiverKind()) {
                case DISPATCH_RECEIVER:
                    return resolvedCall.getDispatchReceiver();
                case EXTENSION_RECEIVER:
                    return resolvedCall.getExtensionReceiver();
                default:
                    return ReceiverValue.NO_RECEIVER;
            }
        }

        @Override
        public void visitBreakExpression(@NotNull JetBreakExpression expression) {
            JetElement loop = getCorrespondingLoop(expression);
            if (loop != null) {
                checkJumpDoesNotCrossFunctionBoundary(expression, loop);
                builder.jump(builder.getExitPoint(loop), expression);
            }
        }

        @Override
        public void visitContinueExpression(@NotNull JetContinueExpression expression) {
            JetElement loop = getCorrespondingLoop(expression);
            if (loop != null) {
                checkJumpDoesNotCrossFunctionBoundary(expression, loop);
                builder.jump(builder.getConditionEntryPoint(loop), expression);
            }
        }

        @Nullable
        private JetElement getCorrespondingLoop(JetExpressionWithLabel expression) {
            String labelName = expression.getLabelName();
            JetLoopExpression loop;
            if (labelName != null) {
                JetSimpleNameExpression targetLabel = expression.getTargetLabel();
                assert targetLabel != null;
                PsiElement labeledElement = trace.get(BindingContext.LABEL_TARGET, targetLabel);
                if (labeledElement instanceof JetLoopExpression) {
                    loop = (JetLoopExpression) labeledElement;
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

        private void checkJumpDoesNotCrossFunctionBoundary(@NotNull JetExpressionWithLabel jumpExpression, @NotNull JetElement jumpTarget) {
            BindingContext bindingContext = trace.getBindingContext();

            FunctionDescriptor labelExprEnclosingFunc = BindingContextUtils.getEnclosingFunctionDescriptor(bindingContext, jumpExpression);
            FunctionDescriptor labelTargetEnclosingFunc = BindingContextUtils.getEnclosingFunctionDescriptor(bindingContext, jumpTarget);
            if (labelExprEnclosingFunc != labelTargetEnclosingFunc) {
                trace.report(BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY.on(jumpExpression));
            }
        }

        @Override
        public void visitReturnExpression(@NotNull JetReturnExpression expression) {
            JetExpression returnedExpression = expression.getReturnedExpression();
            if (returnedExpression != null) {
                generateInstructions(returnedExpression);
            }
            JetSimpleNameExpression labelElement = expression.getTargetLabel();
            JetElement subroutine;
            String labelName = expression.getLabelName();
            if (labelElement != null && labelName != null) {
                PsiElement labeledElement = trace.get(BindingContext.LABEL_TARGET, labelElement);
                if (labeledElement != null) {
                    assert labeledElement instanceof JetElement;
                    subroutine = (JetElement) labeledElement;
                }
                else {
                    subroutine = null;
                }
            }
            else {
                subroutine = builder.getReturnSubroutine();
                // TODO : a context check
            }

            if (subroutine instanceof JetFunction || subroutine instanceof JetPropertyAccessor) {
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
        public void visitParameter(@NotNull JetParameter parameter) {
            builder.declareParameter(parameter);
            JetExpression defaultValue = parameter.getDefaultValue();
            if (defaultValue != null) {
                Label skipDefaultValue = builder.createUnboundLabel("after default value for parameter " + parameter.getName());
                builder.nondeterministicJump(skipDefaultValue, defaultValue, null);
                generateInstructions(defaultValue);
                builder.bindLabel(skipDefaultValue);
            }
            generateInitializer(parameter, computePseudoValueForParameter(parameter));
        }

        @NotNull
        private PseudoValue computePseudoValueForParameter(@NotNull JetParameter parameter) {
            PseudoValue syntheticValue = createSyntheticValue(parameter, MagicKind.FAKE_INITIALIZER);
            PseudoValue defaultValue = builder.getBoundValue(parameter.getDefaultValue());
            if (defaultValue == null) {
                return syntheticValue;
            }
            return builder.merge(parameter, Lists.newArrayList(defaultValue, syntheticValue)).getOutputValue();
        }

        @Override
        public void visitBlockExpression(@NotNull JetBlockExpression expression) {
            boolean declareLexicalScope = !isBlockInDoWhile(expression);
            if (declareLexicalScope) {
                builder.enterLexicalScope(expression);
            }
            mark(expression);
            List<JetElement> statements = expression.getStatements();
            for (JetElement statement : statements) {
                generateInstructions(statement);
            }
            if (statements.isEmpty()) {
                builder.loadUnit(expression);
            }
            else {
                copyValue(KotlinPackage.lastOrNull(statements), expression);
            }
            if (declareLexicalScope) {
                builder.exitLexicalScope(expression);
            }
        }

        private boolean isBlockInDoWhile(@NotNull JetBlockExpression expression) {
            PsiElement parent = expression.getParent();
            if (parent == null) return false;
            return parent.getParent() instanceof JetDoWhileExpression;
        }

        @Override
        public void visitNamedFunction(@NotNull JetNamedFunction function) {
            processLocalDeclaration(function);
        }

        @Override
        public void visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression) {
            mark(expression);
            JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
            processLocalDeclaration(functionLiteral);
            builder.createFunctionLiteral(expression);
        }

        @Override
        public void visitQualifiedExpression(@NotNull JetQualifiedExpression expression) {
            mark(expression);
            JetExpression selectorExpression = expression.getSelectorExpression();
            JetExpression receiverExpression = expression.getReceiverExpression();

            // todo: replace with selectorExpresion != null after parser is fixed
            if (selectorExpression instanceof JetCallExpression || selectorExpression instanceof JetSimpleNameExpression) {
                generateInstructions(selectorExpression);
                copyValue(selectorExpression, expression);
            }
            else {
                generateInstructions(receiverExpression);
                createNonSyntheticValue(expression, MagicKind.UNSUPPORTED_ELEMENT, receiverExpression);
            }
        }

        @Override
        public void visitCallExpression(@NotNull JetCallExpression expression) {
            if (!generateCall(expression)) {
                List<JetExpression> inputExpressions = new ArrayList<JetExpression>();
                for (ValueArgument argument : expression.getValueArguments()) {
                    JetExpression argumentExpression = argument.getArgumentExpression();
                    if (argumentExpression != null) {
                        generateInstructions(argumentExpression);
                        inputExpressions.add(argumentExpression);
                    }
                }
                JetExpression calleeExpression = expression.getCalleeExpression();
                generateInstructions(calleeExpression);
                inputExpressions.add(calleeExpression);
                inputExpressions.add(generateAndGetReceiverIfAny(expression));

                mark(expression);
                createNonSyntheticValue(expression, inputExpressions, MagicKind.UNRESOLVED_CALL);
            }
        }

        @Nullable
        private JetExpression generateAndGetReceiverIfAny(JetExpression expression) {
            PsiElement parent = expression.getParent();
            if (!(parent instanceof JetQualifiedExpression)) return null;

            JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) parent;
            if (qualifiedExpression.getSelectorExpression() != expression) return null;

            JetExpression receiverExpression = qualifiedExpression.getReceiverExpression();
            generateInstructions(receiverExpression);

            return receiverExpression;
        }

        @Override
        public void visitProperty(@NotNull JetProperty property) {
            builder.declareVariable(property);
            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                visitAssignment(property, getDeferredValue(initializer), property);
            }
            JetExpression delegate = property.getDelegateExpression();
            if (delegate != null) {
                generateInstructions(delegate);
                generateDelegateConsumer(property, delegate);
            }

            if (JetPsiUtil.isLocal(property)) {
                for (JetPropertyAccessor accessor : property.getAccessors()) {
                    generateInstructions(accessor);
                }
            }
        }

        private void generateDelegateConsumer(@NotNull JetProperty property, @NotNull JetExpression delegate) {
            DeclarationDescriptor descriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
            if (!(descriptor instanceof PropertyDescriptor)) return;

            PseudoValue delegateValue = builder.getBoundValue(delegate);
            if (delegateValue == null) return;

            List<TypePredicate> typePredicates = KotlinPackage.map(
                    ((PropertyDescriptor) descriptor).getAccessors(),
                    new Function1<PropertyAccessorDescriptor, TypePredicate>() {
                        @Override
                        public TypePredicate invoke(PropertyAccessorDescriptor descriptor) {
                            return getTypePredicateByReceiverValue(trace.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, descriptor));
                        }
                    }
            );
            Map<PseudoValue, TypePredicate> valuesToTypePredicates = SmartFMap
                    .<PseudoValue, TypePredicate>emptyMap()
                    .plus(delegateValue, PseudocodePackage.and(KotlinPackage.filterNotNull(typePredicates)));
            builder.magic(property, null, Collections.singletonList(delegateValue), valuesToTypePredicates, MagicKind.VALUE_CONSUMER);
        }

        private TypePredicate getTypePredicateByReceiverValue(@Nullable ResolvedCall<?> resolvedCall) {
            if (resolvedCall == null) return AllTypes.INSTANCE$;

            ReceiverValue receiverValue = getExplicitReceiverValue(resolvedCall);
            if (receiverValue.exists()) {
                return PseudocodePackage.getReceiverTypePredicate(resolvedCall, receiverValue);
            }

            return AllTypes.INSTANCE$;
        }

        @Override
        public void visitMultiDeclaration(@NotNull JetMultiDeclaration declaration) {
            visitMultiDeclaration(declaration, true);
        }

        private void visitMultiDeclaration(@NotNull JetMultiDeclaration declaration, boolean generateWriteForEntries) {
            JetExpression initializer = declaration.getInitializer();
            generateInstructions(initializer);
            for (JetMultiDeclarationEntry entry : declaration.getEntries()) {
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
        public void visitPropertyAccessor(@NotNull JetPropertyAccessor accessor) {
            processLocalDeclaration(accessor);
        }

        @Override
        public void visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression) {
            mark(expression);

            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            JetExpression left = expression.getLeft();
            if (operationType == JetTokens.COLON || operationType == JetTokens.AS_KEYWORD || operationType == JetTokens.AS_SAFE) {
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
        public void visitThrowExpression(@NotNull JetThrowExpression expression) {
            mark(expression);

            JetExpression thrownExpression = expression.getThrownExpression();
            if (thrownExpression == null) return;

            generateInstructions(thrownExpression);

            PseudoValue thrownValue = builder.getBoundValue(thrownExpression);
            if (thrownValue == null) return;

            builder.throwException(expression, thrownValue);
        }

        @Override
        public void visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression) {
            generateArrayAccess(expression, trace.get(BindingContext.INDEXED_LVALUE_GET, expression));
        }

        @Override
        public void visitIsExpression(@NotNull JetIsExpression expression) {
            mark(expression);
            JetExpression left = expression.getLeftHandSide();
            generateInstructions(left);
            createNonSyntheticValue(expression, MagicKind.IS, left);
        }

        @Override
        public void visitWhenExpression(@NotNull JetWhenExpression expression) {
            mark(expression);

            JetExpression subjectExpression = expression.getSubjectExpression();
            if (subjectExpression != null) {
                generateInstructions(subjectExpression);
            }

            List<JetExpression> branches = new ArrayList<JetExpression>();

            Label doneLabel = builder.createUnboundLabel("after 'when' expression");

            Label nextLabel = null;
            for (Iterator<JetWhenEntry> iterator = expression.getEntries().iterator(); iterator.hasNext(); ) {
                JetWhenEntry whenEntry = iterator.next();
                mark(whenEntry);

                boolean isElse = whenEntry.isElse();
                if (isElse) {
                    if (iterator.hasNext()) {
                        trace.report(ELSE_MISPLACED_IN_WHEN.on(whenEntry));
                    }
                }
                Label bodyLabel = builder.createUnboundLabel("'when' entry body");

                JetWhenCondition[] conditions = whenEntry.getConditions();
                for (int i = 0; i < conditions.length; i++) {
                    JetWhenCondition condition = conditions[i];
                    condition.accept(conditionVisitor);
                    if (i + 1 < conditions.length) {
                        builder.nondeterministicJump(bodyLabel, expression, builder.getBoundValue(condition));
                    }
                }

                if (!isElse) {
                    nextLabel = builder.createUnboundLabel("next 'when' entry");
                    JetWhenCondition lastCondition = KotlinPackage.lastOrNull(conditions);
                    builder.nondeterministicJump(nextLabel, expression, builder.getBoundValue(lastCondition));
                }

                builder.bindLabel(bodyLabel);
                JetExpression whenEntryExpression = whenEntry.getExpression();
                if (whenEntryExpression != null) {
                    generateInstructions(whenEntryExpression);
                    branches.add(whenEntryExpression);
                }
                builder.jump(doneLabel, expression);

                if (!isElse) {
                    builder.bindLabel(nextLabel);
                }
            }
            builder.bindLabel(doneLabel);

            mergeValues(branches, expression);
        }

        @Override
        public void visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression) {
            mark(expression);
            JetObjectDeclaration declaration = expression.getObjectDeclaration();
            generateInstructions(declaration);

            builder.createAnonymousObject(expression);
        }

        @Override
        public void visitObjectDeclaration(@NotNull JetObjectDeclaration objectDeclaration) {
            generateHeaderDelegationSpecifiers(objectDeclaration);
            generateClassOrObjectInitializers(objectDeclaration);
            generateDeclarationForLocalClassOrObjectIfNeeded(objectDeclaration);
        }

        @Override
        public void visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression) {
            mark(expression);

            List<JetExpression> inputExpressions = new ArrayList<JetExpression>();
            for (JetStringTemplateEntry entry : expression.getEntries()) {
                if (entry instanceof JetStringTemplateEntryWithExpression) {
                    JetExpression entryExpression = entry.getExpression();
                    generateInstructions(entryExpression);
                    inputExpressions.add(entryExpression);
                }
            }
            builder.loadStringTemplate(expression, elementsToValues(inputExpressions));
        }

        @Override
        public void visitTypeProjection(@NotNull JetTypeProjection typeProjection) {
            // TODO : Support Type Arguments. Default object may be initialized at this point");
        }

        @Override
        public void visitAnonymousInitializer(@NotNull JetClassInitializer classInitializer) {
            generateInstructions(classInitializer.getBody());
        }

        private void generateHeaderDelegationSpecifiers(@NotNull JetClassOrObject classOrObject) {
            for (JetDelegationSpecifier specifier : classOrObject.getDelegationSpecifiers()) {
                generateInstructions(specifier);
            }
        }

        private void generateClassOrObjectInitializers(@NotNull JetClassOrObject classOrObject) {
            for (JetDeclaration declaration : classOrObject.getDeclarations()) {
                if (declaration instanceof JetProperty || declaration instanceof JetClassInitializer) {
                    generateInstructions(declaration);
                }
            }
        }

        @Override
        public void visitClass(@NotNull JetClass klass) {
            if (klass.hasPrimaryConstructor()) {
                processParameters(klass.getPrimaryConstructorParameters());

                // delegation specifiers of primary constructor, anonymous class and property initializers
                generateHeaderDelegationSpecifiers(klass);
                generateClassOrObjectInitializers(klass);
            }

            generateDeclarationForLocalClassOrObjectIfNeeded(klass);
        }

        private void generateDeclarationForLocalClassOrObjectIfNeeded(@NotNull JetClassOrObject classOrObject) {
            if (classOrObject.isLocal()) {
                for (JetDeclaration declaration : classOrObject.getDeclarations()) {
                    if (declaration instanceof JetSecondaryConstructor ||
                        declaration instanceof JetProperty ||
                        declaration instanceof JetClassInitializer) {
                        continue;
                    }
                    generateInstructions(declaration);
                }
            }
        }

        private void processParameters(@NotNull List<JetParameter> parameters) {
            for (JetParameter parameter : parameters) {
                generateInstructions(parameter);
            }
        }

        @Override
        public void visitSecondaryConstructor(@NotNull JetSecondaryConstructor constructor) {
            JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(constructor, JetClassOrObject.class);
            assert classOrObject != null : "Guaranteed by parsing contract";

            processParameters(constructor.getValueParameters());
            generateCallOrMarkUnresolved(constructor.getDelegationCall());

            if (constructor.getDelegationCall() != null &&
                constructor.getDelegationCall().getCalleeExpression() != null &&
                !constructor.getDelegationCall().getCalleeExpression().isThis()
            ) {
                generateClassOrObjectInitializers(classOrObject);
            }

            generateInstructions(constructor.getBodyExpression());
        }

        @Override
        public void visitDelegationToSuperCallSpecifier(@NotNull JetDelegatorToSuperCall call) {
            generateCallOrMarkUnresolved(call);
        }

        private void generateCallOrMarkUnresolved(@Nullable JetCallElement call) {
            if (call == null) return;
            if (!generateCall(call)) {
                List<JetExpression> arguments = KotlinPackage.map(
                        call.getValueArguments(),
                        new Function1<ValueArgument, JetExpression>() {
                            @Override
                            public JetExpression invoke(ValueArgument valueArgument) {
                                return valueArgument.getArgumentExpression();
                            }
                        }
                );

                for (JetExpression argument : arguments) {
                    generateInstructions(argument);
                }
                createNonSyntheticValue(call, arguments, MagicKind.UNRESOLVED_CALL);
            }
        }

        @Override
        public void visitDelegationByExpressionSpecifier(@NotNull JetDelegatorByExpressionSpecifier specifier) {
            generateInstructions(specifier.getDelegateExpression());

            List<PseudoValue> arguments = ContainerUtil.createMaybeSingletonList(builder.getBoundValue(specifier.getDelegateExpression()));
            JetType jetType = trace.get(BindingContext.TYPE, specifier.getTypeReference());
            TypePredicate expectedTypePredicate = jetType != null ? PseudocodePackage.getSubtypesPredicate(jetType) : AllTypes.INSTANCE$;
            builder.magic(specifier, null, arguments, PseudocodePackage.expectedTypeFor(expectedTypePredicate, arguments),
                          MagicKind.VALUE_CONSUMER);
        }

        @Override
        public void visitDelegationToSuperClassSpecifier(@NotNull JetDelegatorToSuperClass specifier) {
            // Do not generate UNSUPPORTED_ELEMENT here
        }

        @Override
        public void visitDelegationSpecifierList(@NotNull JetDelegationSpecifierList list) {
            list.acceptChildren(this);
        }

        @Override
        public void visitJetFile(@NotNull JetFile file) {
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetProperty) {
                    generateInstructions(declaration);
                }
            }
        }

        @Override
        public void visitDoubleColonExpression(@NotNull JetDoubleColonExpression expression) {
            mark(expression);
            createNonSyntheticValue(expression, MagicKind.CALLABLE_REFERENCE);
        }

        @Override
        public void visitJetElement(@NotNull JetElement element) {
            createNonSyntheticValue(element, MagicKind.UNSUPPORTED_ELEMENT);
        }

        private boolean generateCall(@Nullable JetElement callElement) {
            if (callElement == null) return false;
            return checkAndGenerateCall(getResolvedCall(callElement, trace.getBindingContext()));
        }

        private boolean checkAndGenerateCall(@Nullable ResolvedCall<?> resolvedCall) {
            if (resolvedCall == null) return false;
            generateCall(resolvedCall);
            return true;
        }

        @NotNull
        private InstructionWithValue generateCall(@NotNull ResolvedCall<?> resolvedCall) {
            JetElement callElement = resolvedCall.getCall().getCallElement();

            if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
                VariableAsFunctionResolvedCall variableAsFunctionResolvedCall = (VariableAsFunctionResolvedCall) resolvedCall;
                return generateCall(variableAsFunctionResolvedCall.getFunctionCall());
            }

            CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
            Map<PseudoValue, ReceiverValue> receivers = getReceiverValues(resolvedCall);
            SmartFMap<PseudoValue, ValueParameterDescriptor> parameterValues = SmartFMap.emptyMap();
            for (ValueArgument argument : resolvedCall.getCall().getValueArguments()) {
                ArgumentMapping argumentMapping = resolvedCall.getArgumentMapping(argument);
                JetExpression argumentExpression = argument.getArgumentExpression();
                if (argumentMapping instanceof ArgumentMatch) {
                    parameterValues = generateValueArgument(argument, ((ArgumentMatch) argumentMapping).getValueParameter(), parameterValues);
                }
                else if (argumentExpression != null) {
                    generateInstructions(argumentExpression);
                    createSyntheticValue(argumentExpression, MagicKind.VALUE_CONSUMER, argumentExpression);
                }
            }

            if (resultingDescriptor instanceof VariableDescriptor) {
                // If a callee of the call is just a variable (without 'invoke'), 'read variable' is generated.
                // todo : process arguments for such a case (KT-5387)
                JetExpression callExpression = callElement instanceof JetExpression ? (JetExpression) callElement : null;
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
            SmartFMap<PseudoValue, ReceiverValue> receiverValues = SmartFMap.emptyMap();
            JetElement callElement = resolvedCall.getCall().getCallElement();
            receiverValues = getReceiverValues(callElement, resolvedCall.getDispatchReceiver(), receiverValues);
            receiverValues = getReceiverValues(callElement, resolvedCall.getExtensionReceiver(), receiverValues);
            return receiverValues;
        }

        @NotNull
        private SmartFMap<PseudoValue, ReceiverValue> getReceiverValues(
                JetElement callElement,
                ReceiverValue receiver,
                SmartFMap<PseudoValue, ReceiverValue> receiverValues
        ) {
            if (!receiver.exists()) return receiverValues;

            if (receiver instanceof ThisReceiver) {
                receiverValues = receiverValues.plus(createSyntheticValue(callElement, MagicKind.IMPLICIT_RECEIVER), receiver);
            }
            else if (receiver instanceof ExpressionReceiver) {
                JetExpression expression = ((ExpressionReceiver) receiver).getExpression();
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
            JetExpression expression = valueArgument.getArgumentExpression();
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
