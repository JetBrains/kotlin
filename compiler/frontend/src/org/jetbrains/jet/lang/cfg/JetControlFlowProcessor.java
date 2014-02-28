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

package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowInstructionsGenerator;
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode;
import org.jetbrains.jet.lang.cfg.pseudocode.PseudocodeImpl;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.util.ExpressionAsFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.constants.BooleanValue;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.jet.lang.cfg.JetControlFlowBuilder.PredefinedOperation.*;
import static org.jetbrains.jet.lang.cfg.JetControlFlowProcessor.CFPContext.IN_CONDITION;
import static org.jetbrains.jet.lang.cfg.JetControlFlowProcessor.CFPContext.NOT_IN_CONDITION;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

public class JetControlFlowProcessor {

    private final JetControlFlowBuilder builder;
    private final BindingTrace trace;

    public JetControlFlowProcessor(BindingTrace trace) {
        this.builder = new JetControlFlowInstructionsGenerator();
        this.trace = trace;
    }

    public Pseudocode generatePseudocode(@NotNull JetElement subroutine) {
        Pseudocode pseudocode = generate(subroutine);
        ((PseudocodeImpl) pseudocode).postProcess();
        return pseudocode;
    }

    private Pseudocode generate(@NotNull JetElement subroutine) {
        builder.enterSubroutine(subroutine);
        CFPVisitor cfpVisitor = new CFPVisitor(builder);
        if (subroutine instanceof JetDeclarationWithBody) {
            JetDeclarationWithBody declarationWithBody = (JetDeclarationWithBody) subroutine;
            List<JetParameter> valueParameters = declarationWithBody.getValueParameters();
            for (JetParameter valueParameter : valueParameters) {
                cfpVisitor.generateInstructions(valueParameter, NOT_IN_CONDITION);
            }
            JetExpression bodyExpression = declarationWithBody.getBodyExpression();
            if (bodyExpression != null) {
                cfpVisitor.generateInstructions(bodyExpression, NOT_IN_CONDITION);
            }
        } else {
            cfpVisitor.generateInstructions(subroutine, NOT_IN_CONDITION);
        }
        return builder.exitSubroutine(subroutine);
    }

    private void processLocalDeclaration(@NotNull JetDeclaration subroutine) {
        Label afterDeclaration = builder.createUnboundLabel();
        builder.nondeterministicJump(afterDeclaration);
        generate(subroutine);
        builder.bindLabel(afterDeclaration);
    }

    /*package*/ enum CFPContext {
        IN_CONDITION(true),
        NOT_IN_CONDITION(false);

        private final boolean inCondition;

        private CFPContext(boolean inCondition) {
            this.inCondition = inCondition;
        }

        public boolean inCondition() {
            return inCondition;
        }
    }
    
    private class CFPVisitor extends JetVisitorVoidWithParameter<CFPContext> {
        private final JetControlFlowBuilder builder;

        private final JetVisitorVoidWithParameter<CFPContext> conditionVisitor = new JetVisitorVoidWithParameter<CFPContext>() {

            @Override
            public void visitWhenConditionInRangeVoid(@NotNull JetWhenConditionInRange condition, CFPContext context) {
                generateInstructions(condition.getRangeExpression(), context);
                generateInstructions(condition.getOperationReference(), context);
                // TODO : read the call to contains()...
            }

            @Override
            public void visitWhenConditionIsPatternVoid(@NotNull JetWhenConditionIsPattern condition, CFPContext context) {
                // TODO: types in CF?
            }

            @Override
            public void visitWhenConditionWithExpressionVoid(@NotNull JetWhenConditionWithExpression condition, CFPContext context) {
                generateInstructions(condition.getExpression(), context);
            }

            @Override
            public void visitJetElementVoid(@NotNull JetElement element, CFPContext context) {
                throw new UnsupportedOperationException("[JetControlFlowProcessor] " + element.toString());
            }
        };

        private CFPVisitor(@NotNull JetControlFlowBuilder builder) {
            this.builder = builder;
        }

        private void mark(JetElement element) {
            builder.mark(element);
        }

        public void generateInstructions(@Nullable JetElement element, CFPContext context) {
            if (element == null) return;
            element.accept(this, context);
            checkNothingType(element);
        }

        private void checkNothingType(JetElement element) {
            if (!(element instanceof JetExpression)) return;
            JetExpression expression = JetPsiUtil.deparenthesize((JetExpression) element);
            if (expression instanceof JetStatementExpression || expression instanceof JetTryExpression
                    || expression instanceof JetIfExpression || expression instanceof JetWhenExpression) {
                return;
            }

            JetType type = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
            if (type != null && KotlinBuiltIns.getInstance().isNothing(type)) {
                builder.jumpToError();
            }
        }

        @Override
        public void visitParenthesizedExpressionVoid(@NotNull JetParenthesizedExpression expression, CFPContext context) {
            mark(expression);
            JetExpression innerExpression = expression.getExpression();
            if (innerExpression != null) {
                generateInstructions(innerExpression, context);
            }
        }

        @Override
        public void visitAnnotatedExpressionVoid(@NotNull JetAnnotatedExpression expression, CFPContext context) {
            JetExpression baseExpression = expression.getBaseExpression();
            if (baseExpression != null) {
                generateInstructions(baseExpression, context);
            }
        }

        @Override
        public void visitThisExpressionVoid(@NotNull JetThisExpression expression, CFPContext context) {
            ResolvedCall<?> resolvedCall = getResolvedCall(expression);
            if (resolvedCall == null) {
                builder.readThis(expression, null);
                return;
            }

            CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
            if (resultingDescriptor instanceof ReceiverParameterDescriptor) {
                builder.readThis(expression, (ReceiverParameterDescriptor) resultingDescriptor);
            }
            else if (resultingDescriptor instanceof ExpressionAsFunctionDescriptor) {
                // TODO: no information about actual target
                builder.readThis(expression, null);
            }
        }

        @Override
        public void visitConstantExpressionVoid(@NotNull JetConstantExpression expression, CFPContext context) {
            CompileTimeConstant<?> constant = trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
            builder.loadConstant(expression, constant);
        }

        @Override
        public void visitSimpleNameExpressionVoid(@NotNull JetSimpleNameExpression expression, CFPContext context) {
            ResolvedCall<?> resolvedCall = getResolvedCall(expression);
            if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
                VariableAsFunctionResolvedCall variableAsFunctionResolvedCall = (VariableAsFunctionResolvedCall) resolvedCall;
                generateCall(expression, variableAsFunctionResolvedCall.getVariableCall());
            }
            else {
                generateCall(expression);
            }
        }

        @Override
        public void visitLabelQualifiedExpressionVoid(@NotNull JetLabelQualifiedExpression expression, CFPContext context) {
            String labelName = expression.getLabelName();
            JetExpression labeledExpression = expression.getLabeledExpression();
            if (labelName != null && labeledExpression != null) {
                visitLabeledExpression(labelName, labeledExpression, context);
            }
        }

        private void visitLabeledExpression(@NotNull String labelName, @NotNull JetExpression labeledExpression, CFPContext context) {
            JetExpression deparenthesized = JetPsiUtil.deparenthesize(labeledExpression);
            if (deparenthesized != null) {
                generateInstructions(labeledExpression, context);
            }
        }

        @SuppressWarnings("SuspiciousMethodCalls") @Override
        public void visitBinaryExpressionVoid(@NotNull JetBinaryExpression expression, CFPContext context) {
            JetSimpleNameExpression operationReference = expression.getOperationReference();
            IElementType operationType = operationReference.getReferencedNameElementType();
            if (!ImmutableSet.of(ANDAND, OROR, EQ, ELVIS).contains(operationType)) {
                mark(expression);
            }
            JetExpression right = expression.getRight();
            if (operationType == ANDAND) {
                generateInstructions(expression.getLeft(), IN_CONDITION);
                Label resultLabel = builder.createUnboundLabel();
                builder.jumpOnFalse(resultLabel);
                if (right != null) {
                    generateInstructions(right, IN_CONDITION);
                }
                builder.bindLabel(resultLabel);
                if (!context.inCondition()) {
                    builder.predefinedOperation(expression, AND);
                }
            }
            else if (operationType == OROR) {
                generateInstructions(expression.getLeft(), IN_CONDITION);
                Label resultLabel = builder.createUnboundLabel();
                builder.jumpOnTrue(resultLabel);
                if (right != null) {
                    generateInstructions(right, IN_CONDITION);
                }
                builder.bindLabel(resultLabel);
                if (!context.inCondition()) {
                    builder.predefinedOperation(expression, OR);
                }
            }
            else if (operationType == EQ) {
                visitAssignment(expression.getLeft(), right, expression);
            }
            else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
                if (generateCall(operationReference)) {
                    ResolvedCall<?> resolvedCall = getResolvedCall(operationReference);
                    assert resolvedCall != null : "Generation succeeded, but no call is found: " + expression.getText();
                    CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();
                    Name assignMethodName = OperatorConventions.getNameForOperationSymbol((JetToken) expression.getOperationToken());
                    if (!descriptor.getName().equals(assignMethodName)) {
                        // plus() called, assignment needed
                        visitAssignment(expression.getLeft(), null, expression);
                    }
                }
                else {
                    generateBothArguments(expression);
                }
            }
            else if (operationType == ELVIS) {
                generateInstructions(expression.getLeft(), NOT_IN_CONDITION);
                Label afterElvis = builder.createUnboundLabel();
                builder.jumpOnTrue(afterElvis);
                if (right != null) {
                    generateInstructions(right, NOT_IN_CONDITION);
                }
                builder.bindLabel(afterElvis);
            }
            else {
                if (!generateCall(operationReference)) {
                    generateBothArguments(expression);
                }
            }
        }

        private void generateBothArguments(JetBinaryExpression expression) {
            JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
            if (left != null) {
                generateInstructions(left, NOT_IN_CONDITION);
            }
            JetExpression right = expression.getRight();
            if (right != null) {
                generateInstructions(right, NOT_IN_CONDITION);
            }
        }

        private void visitAssignment(JetExpression lhs, @Nullable JetExpression rhs, JetExpression parentExpression) {
            JetExpression left = JetPsiUtil.deparenthesize(lhs);
            if (left == null) {
                builder.compilationError(lhs, "No lValue in assignment");
                return;
            }

            if (left instanceof JetArrayAccessExpression) {
                ResolvedCall<FunctionDescriptor> setResolvedCall = trace.get(BindingContext.INDEXED_LVALUE_SET, left);
                generateArrayAccess((JetArrayAccessExpression) left, setResolvedCall);
                recordWrite(left, parentExpression);
                return;
            }

            generateInstructions(rhs, NOT_IN_CONDITION);
            if (left instanceof JetSimpleNameExpression || left instanceof JetProperty) {
                // Do nothing, just record write below
            }
            else if (left instanceof JetQualifiedExpression) {
                generateInstructions(((JetQualifiedExpression) left).getReceiverExpression(), NOT_IN_CONDITION);
            }
            else {
                builder.unsupported(parentExpression); // TODO
            }

            recordWrite(left, parentExpression);
        }

        private void recordWrite(JetExpression left, JetExpression parentExpression) {
            VariableDescriptor descriptor = BindingContextUtils.extractVariableDescriptorIfAny(trace.getBindingContext(), left, false);
            if (descriptor != null) {
                builder.write(parentExpression, left);
            }
        }

        private void generateArrayAccess(JetArrayAccessExpression arrayAccessExpression, @Nullable ResolvedCall<?> resolvedCall) {
            mark(arrayAccessExpression);
            if (!checkAndGenerateCall(arrayAccessExpression, resolvedCall)) {
                for (JetExpression index : arrayAccessExpression.getIndexExpressions()) {
                    generateInstructions(index, NOT_IN_CONDITION);
                }

                generateInstructions(arrayAccessExpression.getArrayExpression(), NOT_IN_CONDITION);
            }
        }

        @Override
        public void visitUnaryExpressionVoid(@NotNull JetUnaryExpression expression, CFPContext context) {
            mark(expression);
            JetSimpleNameExpression operationSign = expression.getOperationReference();
            IElementType operationType = operationSign.getReferencedNameElementType();
            JetExpression baseExpression = expression.getBaseExpression();
            if (baseExpression == null) return;
            if (JetTokens.LABELS.contains(operationType)) {
                String referencedName = operationSign.getReferencedName();
                visitLabeledExpression(referencedName.substring(1), baseExpression, context);
            }
            else if (JetTokens.EXCLEXCL == operationType) {
                generateInstructions(baseExpression, NOT_IN_CONDITION);
                builder.predefinedOperation(expression, NOT_NULL_ASSERTION);
            }
            else {
                if (!generateCall(expression.getOperationReference())) {
                    generateInstructions(baseExpression, NOT_IN_CONDITION);
                }

                boolean incrementOrDecrement = isIncrementOrDecrement(operationType);
                if (incrementOrDecrement) {
                    // We skip dup's and other subtleties here
                    visitAssignment(baseExpression, null, expression);
                }
            }
        }

        private boolean isIncrementOrDecrement(IElementType operationType) {
            return operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS;
        }


        @Override
        public void visitIfExpressionVoid(@NotNull JetIfExpression expression, CFPContext context) {
            mark(expression);
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition, IN_CONDITION);
            }
            Label elseLabel = builder.createUnboundLabel();
            builder.jumpOnFalse(elseLabel);
            JetExpression thenBranch = expression.getThen();
            if (thenBranch != null) {
                generateInstructions(thenBranch, context);
            }
            else {
                builder.loadUnit(expression);
            }
            Label resultLabel = builder.createUnboundLabel();
            builder.jump(resultLabel);
            builder.bindLabel(elseLabel);
            JetExpression elseBranch = expression.getElse();
            if (elseBranch != null) {
                generateInstructions(elseBranch, context);
            }
            else {
                builder.loadUnit(expression);
            }
            builder.bindLabel(resultLabel);
        }

        private class FinallyBlockGenerator {
            private final JetFinallySection finallyBlock;
            private final CFPContext context;
            private Label startFinally = null;
            private Label finishFinally = null;

            private FinallyBlockGenerator(JetFinallySection block, CFPContext context) {
                finallyBlock = block;
                this.context = context;
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
                generateInstructions(finalExpression, context);
                finishFinally = builder.createUnboundLabel("finish finally");
                builder.bindLabel(finishFinally);
            }
        }
       

        @Override
        public void visitTryExpressionVoid(@NotNull JetTryExpression expression, CFPContext context) {
            mark(expression);
            JetFinallySection finallyBlock = expression.getFinallyBlock();
            final FinallyBlockGenerator finallyBlockGenerator = new FinallyBlockGenerator(finallyBlock, context);
            if (finallyBlock != null) {
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

            List<JetCatchClause> catchClauses = expression.getCatchClauses();
            boolean hasCatches = !catchClauses.isEmpty();
            Label onException = null;
            if (hasCatches) {
                onException = builder.createUnboundLabel("onException");
                builder.nondeterministicJump(onException);
            }
            Label onExceptionToFinallyBlock = null;
            if (finallyBlock != null) {
                onExceptionToFinallyBlock = builder.createUnboundLabel("onExceptionToFinallyBlock");
                builder.nondeterministicJump(onExceptionToFinallyBlock);
            }
            generateInstructions(expression.getTryBlock(), context);

            if (hasCatches) {
                Label afterCatches = builder.createUnboundLabel("afterCatches");
                builder.jump(afterCatches);

                builder.bindLabel(onException);
                LinkedList<Label> catchLabels = Lists.newLinkedList();
                int catchClausesSize = catchClauses.size();
                for (int i = 0; i < catchClausesSize - 1; i++) {
                    catchLabels.add(builder.createUnboundLabel("catch " + i));
                }
                if (!catchLabels.isEmpty()) {
                    builder.nondeterministicJump(catchLabels);
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
                        builder.write(catchParameter, catchParameter);
                    }
                    JetExpression catchBody = catchClause.getCatchBody();
                    if (catchBody != null) {
                        generateInstructions(catchBody, NOT_IN_CONDITION);
                    }
                    builder.jump(afterCatches);
                    builder.exitLexicalScope(catchClause);
                }

                builder.bindLabel(afterCatches);
            }

            if (finallyBlock != null) {
                builder.exitTryFinally();

                Label skipFinallyToErrorBlock = builder.createUnboundLabel("skipFinallyToErrorBlock");
                builder.jump(skipFinallyToErrorBlock);
                builder.bindLabel(onExceptionToFinallyBlock);
                finallyBlockGenerator.generate();
                builder.jumpToError();
                builder.bindLabel(skipFinallyToErrorBlock);

                finallyBlockGenerator.generate();
            }
        }

        @Override
        public void visitWhileExpressionVoid(@NotNull JetWhileExpression expression, CFPContext context) {
            mark(expression);
            LoopInfo loopInfo = builder.enterLoop(expression, null, null);

            builder.bindLabel(loopInfo.getConditionEntryPoint());
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition, IN_CONDITION);
            }
            boolean conditionIsTrueConstant = false;
            if (condition instanceof JetConstantExpression && condition.getNode().getElementType() == JetNodeTypes.BOOLEAN_CONSTANT) {
                CompileTimeConstant<?> compileTimeConstant = ConstantExpressionEvaluator.object$.evaluate(condition, trace, KotlinBuiltIns.getInstance().getBooleanType());
                if (compileTimeConstant instanceof BooleanValue) {
                    Boolean value = ((BooleanValue) compileTimeConstant).getValue();
                    if (Boolean.TRUE.equals(value)) {
                        conditionIsTrueConstant = true;
                    }
                }
            }
            if (!conditionIsTrueConstant) {
                builder.jumpOnFalse(loopInfo.getExitPoint());
            }

            builder.bindLabel(loopInfo.getBodyEntryPoint());
            JetExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body, NOT_IN_CONDITION);
            }
            builder.jump(loopInfo.getEntryPoint());
            builder.exitLoop(expression);
            builder.loadUnit(expression);
        }

        @Override
        public void visitDoWhileExpressionVoid(@NotNull JetDoWhileExpression expression, CFPContext context) {
            builder.enterLexicalScope(expression);
            mark(expression);
            LoopInfo loopInfo = builder.enterLoop(expression, null, null);

            builder.bindLabel(loopInfo.getBodyEntryPoint());
            JetExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body, NOT_IN_CONDITION);
            }
            builder.bindLabel(loopInfo.getConditionEntryPoint());
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition, IN_CONDITION);
            }
            builder.jumpOnTrue(loopInfo.getEntryPoint());
            builder.exitLoop(expression);
            builder.loadUnit(expression);
            builder.exitLexicalScope(expression);
        }

        @Override
        public void visitForExpressionVoid(@NotNull JetForExpression expression, CFPContext context) {
            builder.enterLexicalScope(expression);
            mark(expression);
            JetExpression loopRange = expression.getLoopRange();
            if (loopRange != null) {
                generateInstructions(loopRange, NOT_IN_CONDITION);
            }
            declareLoopParameter(expression);

            // TODO : primitive cases
            Label loopExitPoint = builder.createUnboundLabel();
            Label conditionEntryPoint = builder.createUnboundLabel();

            builder.bindLabel(conditionEntryPoint);
            builder.nondeterministicJump(loopExitPoint);

            LoopInfo loopInfo = builder.enterLoop(expression, loopExitPoint, conditionEntryPoint);

            builder.bindLabel(loopInfo.getBodyEntryPoint());
            writeLoopParameterAssignment(expression);

            JetExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body, NOT_IN_CONDITION);
            }

            builder.nondeterministicJump(loopInfo.getEntryPoint());
            builder.exitLoop(expression);
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
            if (loopParameter != null) {
                builder.write(loopParameter, loopParameter);
            }
            else if (multiDeclaration != null) {
                for (JetMultiDeclarationEntry entry : multiDeclaration.getEntries()) {
                    builder.write(entry, entry);
                }
            }
        }

        @Override
        public void visitBreakExpressionVoid(@NotNull JetBreakExpression expression, CFPContext context) {
            JetElement loop = getCorrespondingLoop(expression);
            if (loop != null) {
                checkJumpDoesNotCrossFunctionBoundary(expression, loop);
                builder.jump(builder.getExitPoint(loop));
            }
        }

        @Override
        public void visitContinueExpressionVoid(@NotNull JetContinueExpression expression, CFPContext context) {
            JetElement loop = getCorrespondingLoop(expression);
            if (loop != null) {
                checkJumpDoesNotCrossFunctionBoundary(expression, loop);
                builder.jump(builder.getEntryPoint(loop));
            }
        }

        private JetElement getCorrespondingLoop(JetLabelQualifiedExpression expression) {
            String labelName = expression.getLabelName();
            JetElement loop;
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
            return loop;
        }

        private void checkJumpDoesNotCrossFunctionBoundary(@NotNull JetLabelQualifiedExpression jumpExpression, @NotNull JetElement jumpTarget) {
            BindingContext bindingContext = trace.getBindingContext();

            FunctionDescriptor labelExprEnclosingFunc = BindingContextUtils.getEnclosingFunctionDescriptor(bindingContext, jumpExpression);
            FunctionDescriptor labelTargetEnclosingFunc = BindingContextUtils.getEnclosingFunctionDescriptor(bindingContext, jumpTarget);
            if (labelExprEnclosingFunc != labelTargetEnclosingFunc) {
                trace.report(BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY.on(jumpExpression));
            }
        }

        @Override
        public void visitReturnExpressionVoid(@NotNull JetReturnExpression expression, CFPContext context) {
            JetExpression returnedExpression = expression.getReturnedExpression();
            if (returnedExpression != null) {
                generateInstructions(returnedExpression, NOT_IN_CONDITION);
            }
            JetSimpleNameExpression labelElement = expression.getTargetLabel();
            JetElement subroutine;
            String labelName = expression.getLabelName();
            if (labelElement != null) {
                assert labelName != null;
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
                if (returnedExpression == null) {
                    builder.returnNoValue(expression, subroutine);
                }
                else {
                    builder.returnValue(expression, subroutine);
                }
            }
        }

        @Override
        public void visitParameterVoid(@NotNull JetParameter parameter, CFPContext context) {
            builder.declareParameter(parameter);
            JetExpression defaultValue = parameter.getDefaultValue();
            if (defaultValue != null) {
                generateInstructions(defaultValue, context);
            }
            builder.write(parameter, parameter);
        }

        @Override
        public void visitBlockExpressionVoid(@NotNull JetBlockExpression expression, CFPContext context) {
            boolean declareLexicalScope = !isBlockInDoWhile(expression);
            if (declareLexicalScope) {
                builder.enterLexicalScope(expression);
            }
            mark(expression);
            List<JetElement> statements = expression.getStatements();
            for (JetElement statement : statements) {
                generateInstructions(statement, NOT_IN_CONDITION);
            }
            if (statements.isEmpty()) {
                builder.loadUnit(expression);
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
        public void visitNamedFunctionVoid(@NotNull JetNamedFunction function, CFPContext context) {
            processLocalDeclaration(function);
        }

        @Override
        public void visitFunctionLiteralExpressionVoid(@NotNull JetFunctionLiteralExpression expression, CFPContext context) {
            mark(expression);
            JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
            processLocalDeclaration(functionLiteral);
            builder.createFunctionLiteral(expression);
        }

        @Override
        public void visitQualifiedExpressionVoid(@NotNull JetQualifiedExpression expression, CFPContext context) {
            mark(expression);
            JetExpression selectorExpression = expression.getSelectorExpression();

            if (selectorExpression == null) {
                generateInstructions(expression.getReceiverExpression(), NOT_IN_CONDITION);
                return;
            }

            generateInstructions(selectorExpression, NOT_IN_CONDITION);

            // receiver was generated for resolvedCall
            JetExpression calleeExpression = JetPsiUtil.getCalleeExpressionIfAny(selectorExpression);
            if (calleeExpression == null || getResolvedCall(calleeExpression) == null) {
                generateInstructions(expression.getReceiverExpression(), NOT_IN_CONDITION);
            }
        }

        @Override
        public void visitCallExpressionVoid(@NotNull JetCallExpression expression, CFPContext context) {
            mark(expression);
            if (!generateCall(expression.getCalleeExpression())) {
                for (ValueArgument argument : expression.getValueArguments()) {
                    JetExpression argumentExpression = argument.getArgumentExpression();
                    if (argumentExpression != null) {
                        generateInstructions(argumentExpression, NOT_IN_CONDITION);
                    }
                }

                for (JetExpression functionLiteral : expression.getFunctionLiteralArguments()) {
                    generateInstructions(functionLiteral, NOT_IN_CONDITION);
                }

                generateInstructions(expression.getCalleeExpression(), NOT_IN_CONDITION);
            }
        }

        @Override
        public void visitPropertyVoid(@NotNull JetProperty property, CFPContext context) {
            builder.declareVariable(property);
            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                generateInstructions(initializer, NOT_IN_CONDITION);
                visitAssignment(property, null, property);
            }
            JetExpression delegate = property.getDelegateExpression();
            if (delegate != null) {
                generateInstructions(delegate, NOT_IN_CONDITION);
            }
            if (JetPsiUtil.isLocal(property)) {
                for (JetPropertyAccessor accessor : property.getAccessors()) {
                    generateInstructions(accessor, NOT_IN_CONDITION);
                }
            }
        }

        @Override
        public void visitMultiDeclarationVoid(@NotNull JetMultiDeclaration declaration, CFPContext context) {
            visitMultiDeclaration(declaration, true);
        }

        private void visitMultiDeclaration(@NotNull JetMultiDeclaration declaration, boolean generateWriteForEntries) {
            generateInstructions(declaration.getInitializer(), NOT_IN_CONDITION);

            for (JetMultiDeclarationEntry entry : declaration.getEntries()) {
                builder.declareVariable(entry);
                ResolvedCall<FunctionDescriptor> resolvedCall = trace.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                if (resolvedCall != null) {
                    builder.call(entry, resolvedCall);
                }
                if (generateWriteForEntries) {
                    builder.write(entry, entry);
                }
            }
        }

        @Override
        public void visitPropertyAccessorVoid(@NotNull JetPropertyAccessor accessor, CFPContext context) {
            processLocalDeclaration(accessor);
        }

        @Override
        public void visitBinaryWithTypeRHSExpressionVoid(@NotNull JetBinaryExpressionWithTypeRHS expression, CFPContext context) {
            mark(expression);
            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            if (operationType == JetTokens.COLON || operationType == JetTokens.AS_KEYWORD || operationType == JetTokens.AS_SAFE) {
                generateInstructions(expression.getLeft(), NOT_IN_CONDITION);
            }
            else {
                visitJetElementVoid(expression, context);
            }
        }

        @Override
        public void visitThrowExpressionVoid(@NotNull JetThrowExpression expression, CFPContext context) {
            mark(expression);
            JetExpression thrownExpression = expression.getThrownExpression();
            if (thrownExpression != null) {
                generateInstructions(thrownExpression, NOT_IN_CONDITION);
            }
            builder.throwException(expression);
        }

        @Override
        public void visitArrayAccessExpressionVoid(@NotNull JetArrayAccessExpression expression, CFPContext context) {
            mark(expression);
            ResolvedCall<FunctionDescriptor> getMethodResolvedCall = trace.get(BindingContext.INDEXED_LVALUE_GET, expression);
            if (!checkAndGenerateCall(expression, getMethodResolvedCall)) {
                generateArrayAccess(expression, getMethodResolvedCall);
            }
        }

        @Override
        public void visitIsExpressionVoid(@NotNull JetIsExpression expression, CFPContext context) {
            mark(expression);
            generateInstructions(expression.getLeftHandSide(), context);
        }

        @Override
        public void visitWhenExpressionVoid(@NotNull JetWhenExpression expression, CFPContext context) {
            mark(expression);
            JetExpression subjectExpression = expression.getSubjectExpression();
            if (subjectExpression != null) {
                generateInstructions(subjectExpression, context);
            }
            boolean hasElse = false;

            Label doneLabel = builder.createUnboundLabel();

            Label nextLabel = null;
            for (Iterator<JetWhenEntry> iterator = expression.getEntries().iterator(); iterator.hasNext(); ) {
                JetWhenEntry whenEntry = iterator.next();
                mark(whenEntry);

                boolean isElse = whenEntry.isElse();
                if (isElse) {
                    hasElse = true;
                    if (iterator.hasNext()) {
                        trace.report(ELSE_MISPLACED_IN_WHEN.on(whenEntry));
                    }
                }
                Label bodyLabel = builder.createUnboundLabel();

                JetWhenCondition[] conditions = whenEntry.getConditions();
                for (int i = 0; i < conditions.length; i++) {
                    JetWhenCondition condition = conditions[i];
                    condition.accept(conditionVisitor, context);
                    if (i + 1 < conditions.length) {
                        builder.nondeterministicJump(bodyLabel);
                    }
                }

                if (!isElse) {
                    nextLabel = builder.createUnboundLabel();
                    builder.nondeterministicJump(nextLabel);
                }

                builder.bindLabel(bodyLabel);
                generateInstructions(whenEntry.getExpression(), context);
                builder.jump(doneLabel);

                if (!isElse) {
                    builder.bindLabel(nextLabel);
                }
            }
            builder.bindLabel(doneLabel);
            if (!hasElse && WhenChecker.mustHaveElse(expression, trace)) {
                trace.report(NO_ELSE_IN_WHEN.on(expression));
            }
        }

        @Override
        public void visitObjectLiteralExpressionVoid(@NotNull JetObjectLiteralExpression expression, CFPContext context) {
            mark(expression);
            JetObjectDeclaration declaration = expression.getObjectDeclaration();
            generateInstructions(declaration, context);

            builder.createAnonymousObject(expression);
        }

        @Override
        public void visitObjectDeclarationVoid(@NotNull JetObjectDeclaration objectDeclaration, CFPContext context) {
            visitClassOrObject(objectDeclaration, context);
        }

        @Override
        public void visitStringTemplateExpressionVoid(@NotNull JetStringTemplateExpression expression, CFPContext context) {
            mark(expression);
            for (JetStringTemplateEntry entry : expression.getEntries()) {
                if (entry instanceof JetStringTemplateEntryWithExpression) {
                    JetStringTemplateEntryWithExpression entryWithExpression = (JetStringTemplateEntryWithExpression) entry;
                    generateInstructions(entryWithExpression.getExpression(), NOT_IN_CONDITION);
                }
            }
            builder.loadStringTemplate(expression);
        }

        @Override
        public void visitTypeProjectionVoid(@NotNull JetTypeProjection typeProjection, CFPContext context) {
            // TODO : Support Type Arguments. Class object may be initialized at this point");
        }

        @Override
        public void visitAnonymousInitializerVoid(@NotNull JetClassInitializer classInitializer, CFPContext context) {
            generateInstructions(classInitializer.getBody(), context);
        }

        private void visitClassOrObject(JetClassOrObject classOrObject, CFPContext context) {
            for (JetDelegationSpecifier specifier : classOrObject.getDelegationSpecifiers()) {
                generateInstructions(specifier, context);
            }
            List<JetDeclaration> declarations = classOrObject.getDeclarations();
            if (JetPsiUtil.isLocal(classOrObject)) {
                for (JetDeclaration declaration : declarations) {
                    generateInstructions(declaration, context);
                }
                return;
            }
            //For top-level and inner classes and objects functions are collected and checked separately.
            for (JetDeclaration declaration : declarations) {
                if (declaration instanceof JetProperty || declaration instanceof JetClassInitializer) {
                    generateInstructions(declaration, context);
                }
            }
        }

        @Override
        public void visitClassVoid(@NotNull JetClass klass, CFPContext context) {
            List<JetParameter> parameters = klass.getPrimaryConstructorParameters();
            for (JetParameter parameter : parameters) {
                generateInstructions(parameter, context);
            }
            visitClassOrObject(klass, context);
        }

        @Override
        public void visitDelegationToSuperCallSpecifierVoid(@NotNull JetDelegatorToSuperCall call, CFPContext context) {
            List<? extends ValueArgument> valueArguments = call.getValueArguments();
            for (ValueArgument valueArgument : valueArguments) {
                generateInstructions(valueArgument.getArgumentExpression(), context);
            }
        }

        @Override
        public void visitDelegationByExpressionSpecifierVoid(@NotNull JetDelegatorByExpressionSpecifier specifier, CFPContext context) {
            generateInstructions(specifier.getDelegateExpression(), context);
        }

        @Override
        public void visitJetFileVoid(@NotNull JetFile file, CFPContext context) {
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetProperty) {
                    generateInstructions(declaration, context);
                }
            }
        }

        @Override
        public void visitJetElementVoid(@NotNull JetElement element, CFPContext context) {
            builder.unsupported(element);
        }

        @Nullable
        private ResolvedCall<?> getResolvedCall(@NotNull JetElement expression) {
            return trace.get(BindingContext.RESOLVED_CALL, expression);
        }

        private boolean generateCall(@Nullable JetExpression calleeExpression) {
            if (calleeExpression == null) return false;
            return checkAndGenerateCall(calleeExpression, getResolvedCall(calleeExpression));
        }

        private boolean checkAndGenerateCall(JetExpression calleeExpression, @Nullable ResolvedCall<?> resolvedCall) {
            if (resolvedCall == null) {
                builder.compilationError(calleeExpression, "No resolved call");
                return false;
            }
            generateCall(calleeExpression, resolvedCall);
            return true;
        }

        private void generateCall(JetExpression calleeExpression, ResolvedCall<?> resolvedCall) {
            if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
                VariableAsFunctionResolvedCall variableAsFunctionResolvedCall = (VariableAsFunctionResolvedCall) resolvedCall;
                generateCall(calleeExpression, variableAsFunctionResolvedCall.getFunctionCall());
                return;
            }

            CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
            if (resultingDescriptor instanceof ExpressionAsFunctionDescriptor) {
                generateInstructions(((ExpressionAsFunctionDescriptor) resultingDescriptor).getExpression(), NOT_IN_CONDITION);
            }

            generateReceiver(resolvedCall.getThisObject());
            generateReceiver(resolvedCall.getReceiverArgument());

            for (ValueParameterDescriptor parameterDescriptor : resultingDescriptor.getValueParameters()) {
                ResolvedValueArgument argument = resolvedCall.getValueArguments().get(parameterDescriptor);
                if (argument == null) continue;

                generateValueArgument(argument);
            }

            if (resultingDescriptor instanceof VariableDescriptor) {
                builder.readVariable(calleeExpression, (VariableDescriptor) resultingDescriptor);
            }
            else {
                builder.call(calleeExpression, resolvedCall);
            }
        }

        private void generateReceiver(ReceiverValue receiver) {
            if (!receiver.exists()) return;
            if (receiver instanceof ThisReceiver) {
                // TODO: Receiver is passed implicitly: no expression to tie the read to
            }
            else if (receiver instanceof ExpressionReceiver) {
                generateInstructions(((ExpressionReceiver) receiver).getExpression(), NOT_IN_CONDITION);
            }
            else if (receiver instanceof TransientReceiver) {
                // Do nothing
            }
            else {
                throw new IllegalArgumentException("Unknown receiver kind: " + receiver);
            }
        }

        private void generateValueArgument(ResolvedValueArgument argument) {
            for (ValueArgument valueArgument : argument.getArguments()) {
                JetExpression expression = valueArgument.getArgumentExpression();
                if (expression != null) {
                    generateInstructions(expression, NOT_IN_CONDITION);
                }
            }
        }
    }
}
