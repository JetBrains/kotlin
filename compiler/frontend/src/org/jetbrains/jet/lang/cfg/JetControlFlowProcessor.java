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

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowInstructionsGenerator;
import org.jetbrains.jet.lang.cfg.pseudocode.LocalDeclarationInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode;
import org.jetbrains.jet.lang.cfg.pseudocode.PseudocodeImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.constants.BooleanValue;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantResolver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

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
        for (LocalDeclarationInstruction localDeclarationInstruction : pseudocode.getLocalDeclarations()) {
            ((PseudocodeImpl)localDeclarationInstruction.getBody()).postProcess();
        }
        return pseudocode;
    }

    private Pseudocode generate(@NotNull JetElement subroutine) {
        builder.enterSubroutine(subroutine);
        CFPVisitor cfpVisitor = new CFPVisitor(false);
        if (subroutine instanceof JetDeclarationWithBody) {
            JetDeclarationWithBody declarationWithBody = (JetDeclarationWithBody) subroutine;
            List<JetParameter> valueParameters = declarationWithBody.getValueParameters();
            for (JetParameter valueParameter : valueParameters) {
                valueParameter.accept(cfpVisitor);
            }
            JetExpression bodyExpression = declarationWithBody.getBodyExpression();
            if (bodyExpression != null) {
                bodyExpression.accept(cfpVisitor);
            }
        } else {
            subroutine.accept(cfpVisitor);
        }
        return builder.exitSubroutine(subroutine);
    }

    private void processLocalDeclaration(@NotNull JetDeclaration subroutine) {
        Label afterDeclaration = builder.createUnboundLabel();
        builder.nondeterministicJump(afterDeclaration);
        generate(subroutine);
        builder.bindLabel(afterDeclaration);
    }

    
    private class CFPVisitor extends JetVisitorVoid {
        private final boolean inCondition;
        private final JetVisitorVoid conditionVisitor = new JetVisitorVoid() {

            @Override
            public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
                generateInstructions(condition.getRangeExpression(), CFPVisitor.this.inCondition); // TODO : inCondition?
                generateInstructions(condition.getOperationReference(), CFPVisitor.this.inCondition); // TODO : inCondition?
                // TODO : read the call to contains()...
            }

            @Override
            public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
                // TODO: types in CF?
            }

            @Override
            public void visitWhenConditionWithExpression(JetWhenConditionWithExpression condition) {
                generateInstructions(condition.getExpression(), inCondition);
            }

            @Override
            public void visitJetElement(JetElement element) {
                throw new UnsupportedOperationException("[JetControlFlowProcessor] " + element.toString());
            }
        };
        private final JetVisitorVoid patternVisitor = new JetVisitorVoid() {

            @Override
            public void visitJetElement(JetElement element) {
                throw new UnsupportedOperationException("[JetControlFlowProcessor] " + element.toString());
            }
        };

        private CFPVisitor(boolean inCondition) {
            this.inCondition = inCondition;
        }

        private void generateInstructions(@Nullable JetElement element, boolean inCondition) {
            if (element == null) return;
            CFPVisitor visitor;
            if (this.inCondition == inCondition) {
                visitor = this;
            }
            else {
                visitor = new CFPVisitor(inCondition);
            }
            element.accept(visitor);
        }

        @Override
        public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
            builder.read(expression);

            JetExpression innerExpression = expression.getExpression();
            if (innerExpression != null) {
                generateInstructions(innerExpression, inCondition);
            }
        }

        @Override
        public void visitThisExpression(JetThisExpression expression) {
            builder.read(expression);
        }

        @Override
        public void visitConstantExpression(JetConstantExpression expression) {
            builder.read(expression);
        }

        @Override
        public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
            builder.read(expression);
            if (trace.get(BindingContext.PROCESSED, expression)) {
                JetType type = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
                if (type != null && KotlinBuiltIns.getInstance().isNothing(type)) {
                    builder.jumpToError();
                }
            }
        }

        @Override
        public void visitLabelQualifiedExpression(JetLabelQualifiedExpression expression) {
            String labelName = expression.getLabelName();
            JetExpression labeledExpression = expression.getLabeledExpression();
            if (labelName != null && labeledExpression != null) {
                visitLabeledExpression(labelName, labeledExpression);
            }
        }

        private void visitLabeledExpression(@NotNull String labelName, @NotNull JetExpression labeledExpression) {
            JetExpression deparenthesized = JetPsiUtil.deparenthesizeWithNoTypeResolution(labeledExpression);
            if (deparenthesized != null) {
                generateInstructions(labeledExpression, inCondition);
            }
        }

        @SuppressWarnings("SuspiciousMethodCalls") @Override
        public void visitBinaryExpression(JetBinaryExpression expression) {
            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            JetExpression right = expression.getRight();
            if (operationType == JetTokens.ANDAND) {
                generateInstructions(expression.getLeft(), true);
                Label resultLabel = builder.createUnboundLabel();
                builder.jumpOnFalse(resultLabel);
                if (right != null) {
                    generateInstructions(right, true);
                }
                builder.bindLabel(resultLabel);
                if (!inCondition) {
                    builder.read(expression);
                }
            }
            else if (operationType == JetTokens.OROR) {
                generateInstructions(expression.getLeft(), true);
                Label resultLabel = builder.createUnboundLabel();
                builder.jumpOnTrue(resultLabel);
                if (right != null) {
                    generateInstructions(right, true);
                }
                builder.bindLabel(resultLabel);
                if (!inCondition) {
                    builder.read(expression);
                }
            }
            else if (operationType == JetTokens.EQ) {
                JetExpression left = JetPsiUtil.deparenthesizeWithNoTypeResolution(expression.getLeft());
                if (right != null) {
                    generateInstructions(right, false);
                }
                if (left instanceof JetSimpleNameExpression) {
                    builder.write(expression, left);
                }
                else if (left instanceof JetArrayAccessExpression) {
                    JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
                    visitAssignToArrayAccess(expression, arrayAccessExpression);
                }
                else if (left instanceof JetQualifiedExpression) {
                    JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) left;
                    generateInstructions(qualifiedExpression.getReceiverExpression(), false);
                    generateInstructions(expression.getOperationReference(), false);
                    builder.write(expression, left);
                }
                else {
                    builder.unsupported(expression); // TODO
                }
            }
            else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
                JetExpression left = JetPsiUtil.deparenthesizeWithNoTypeResolution(expression.getLeft());
                if (left != null) {
                    generateInstructions(left, false);
                }
                if (right != null) {
                    generateInstructions(right, false);
                }
                if (left instanceof JetSimpleNameExpression || left instanceof JetArrayAccessExpression) {
                    generateInstructions(expression.getOperationReference(), false);
                    builder.write(expression, left);
                }
                else if (left != null) {
                    builder.unsupported(expression); // TODO
                }
            }
            else if (operationType == JetTokens.ELVIS) {
                builder.read(expression);
                generateInstructions(expression.getLeft(), false);
                generateInstructions(expression.getOperationReference(), false);
                Label afterElvis = builder.createUnboundLabel();
                builder.jumpOnTrue(afterElvis);
                if (right != null) {
                    generateInstructions(right, false);
                }
                builder.bindLabel(afterElvis);
            }
            else {
                generateInstructions(expression.getLeft(), false);
                if (right != null) {
                    generateInstructions(right, false);
                }
                generateInstructions(expression.getOperationReference(), false);
                builder.read(expression);
            }
        }

        private void visitAssignToArrayAccess(JetBinaryExpression expression, JetArrayAccessExpression arrayAccessExpression) {
            for (JetExpression index : arrayAccessExpression.getIndexExpressions()) {
                generateInstructions(index, false);
            }
            generateInstructions(arrayAccessExpression.getArrayExpression(), false);
            generateInstructions(expression.getOperationReference(), false);
            builder.write(expression, arrayAccessExpression); // TODO : ???
        }

        @Override
        public void visitUnaryExpression(JetUnaryExpression expression) {
            JetSimpleNameExpression operationSign = expression.getOperationReference();
            IElementType operationType = operationSign.getReferencedNameElementType();
            JetExpression baseExpression = expression.getBaseExpression();
            if (baseExpression == null) return;
            if (JetTokens.LABELS.contains(operationType)) {
                String referencedName = operationSign.getReferencedName();
                visitLabeledExpression(referencedName.substring(1), baseExpression);
            }
            else {
                generateInstructions(baseExpression, false);
                generateInstructions(operationSign, false);

                boolean incrementOrDecrement = isIncrementOrDecrement(operationType);
                if (incrementOrDecrement) {
                    builder.write(expression, baseExpression);
                }

                builder.read(expression);
            }
        }

        private boolean isIncrementOrDecrement(IElementType operationType) {
            return operationType == JetTokens.PLUSPLUS || operationType == JetTokens.MINUSMINUS;
        }


        @Override
        public void visitIfExpression(JetIfExpression expression) {
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition, true);
            }
            Label elseLabel = builder.createUnboundLabel();
            builder.jumpOnFalse(elseLabel);
            JetExpression thenBranch = expression.getThen();
            if (thenBranch != null) {
                generateInstructions(thenBranch, inCondition);
            }
            else {
                builder.readUnit(expression);
            }
            Label resultLabel = builder.createUnboundLabel();
            builder.jump(resultLabel);
            builder.bindLabel(elseLabel);
            JetExpression elseBranch = expression.getElse();
            if (elseBranch != null) {
                generateInstructions(elseBranch, inCondition);
            }
            else {
                builder.readUnit(expression);
            }
            builder.bindLabel(resultLabel);
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
                generateInstructions(finalExpression, inCondition);
                finishFinally = builder.createUnboundLabel("finish finally");
                builder.bindLabel(finishFinally);
            }
        }
       

        @Override
        public void visitTryExpression(JetTryExpression expression) {
            builder.read(expression);
            JetFinallySection finallyBlock = expression.getFinallyBlock();
            final FinallyBlockGenerator finallyBlockGenerator = new FinallyBlockGenerator(finallyBlock);
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
            generateInstructions(expression.getTryBlock(), inCondition);

            Collection<Label> allowDeadLabels = Lists.newArrayList();
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
                    if (!isFirst) {
                        builder.bindLabel(catchLabels.remove());
                    }
                    else {
                        isFirst = false;
                    }
                    JetParameter catchParameter = catchClause.getCatchParameter();
                    if (catchParameter != null) {
                        builder.declare(catchParameter);
                        builder.write(catchParameter, catchParameter);
                    }
                    JetExpression catchBody = catchClause.getCatchBody();
                    if (catchBody != null) {
                        generateInstructions(catchBody, false);
                    }
                    builder.jump(afterCatches);
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
        public void visitWhileExpression(JetWhileExpression expression) {
            builder.read(expression);
            LoopInfo loopInfo = builder.enterLoop(expression, null, null);

            builder.bindLabel(loopInfo.getConditionEntryPoint());
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition, true);
            }
            boolean conditionIsTrueConstant = false;
            if (condition instanceof JetConstantExpression && condition.getNode().getElementType() == JetNodeTypes.BOOLEAN_CONSTANT) {
                if (BooleanValue.TRUE == new CompileTimeConstantResolver().getBooleanValue(condition.getText(), KotlinBuiltIns.getInstance().getBooleanType())) {
                    conditionIsTrueConstant = true;
                }
            }
            if (!conditionIsTrueConstant) {
                builder.jumpOnFalse(loopInfo.getExitPoint());
            }

            builder.bindLabel(loopInfo.getBodyEntryPoint());
            JetExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body, false);
            }
            builder.jump(loopInfo.getEntryPoint());
            builder.exitLoop(expression);
            builder.readUnit(expression);
        }

        @Override
        public void visitDoWhileExpression(JetDoWhileExpression expression) {
            builder.read(expression);
            LoopInfo loopInfo = builder.enterLoop(expression, null, null);

            builder.bindLabel(loopInfo.getBodyEntryPoint());
            JetExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body, false);
            }
            builder.bindLabel(loopInfo.getConditionEntryPoint());
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                generateInstructions(condition, true);
            }
            builder.jumpOnTrue(loopInfo.getEntryPoint());
            builder.exitLoop(expression);
            builder.readUnit(expression);
        }

        @Override
        public void visitForExpression(JetForExpression expression) {
            builder.read(expression);
            JetExpression loopRange = expression.getLoopRange();
            if (loopRange != null) {
                generateInstructions(loopRange, false);
            }
            JetParameter loopParameter = expression.getLoopParameter();
            if (loopParameter != null) {
                generateInstructions(loopParameter, inCondition);
            }
            else {
                JetMultiDeclaration multiParameter = expression.getMultiParameter();
                generateInstructions(multiParameter, inCondition);
            }

            // TODO : primitive cases
            Label loopExitPoint = builder.createUnboundLabel();
            Label conditionEntryPoint = builder.createUnboundLabel();

            builder.bindLabel(conditionEntryPoint);
            builder.nondeterministicJump(loopExitPoint);

            LoopInfo loopInfo = builder.enterLoop(expression, loopExitPoint, conditionEntryPoint);

            builder.bindLabel(loopInfo.getBodyEntryPoint());
            JetExpression body = expression.getBody();
            if (body != null) {
                generateInstructions(body, false);
            }

            builder.nondeterministicJump(loopInfo.getEntryPoint());
            builder.exitLoop(expression);
            builder.readUnit(expression);
        }

        @Override
        public void visitBreakExpression(JetBreakExpression expression) {
            JetElement loop = getCorrespondingLoop(expression);
            if (loop != null) {
                builder.jump(builder.getExitPoint(loop));
            }
        }

        @Override
        public void visitContinueExpression(JetContinueExpression expression) {
            JetElement loop = getCorrespondingLoop(expression);
            if (loop != null) {
                builder.jump(builder.getEntryPoint(loop));
            }
        }

        private JetElement getCorrespondingLoop(JetLabelQualifiedExpression expression) {
            String labelName = expression.getLabelName();
            JetElement loop;
            if (labelName != null) {
                JetSimpleNameExpression targetLabel = expression.getTargetLabel();
                assert targetLabel != null;
                PsiElement labeledElement = BindingContextUtils.resolveToDeclarationPsiElement(trace.getBindingContext(), targetLabel);
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

        @Override
        public void visitReturnExpression(JetReturnExpression expression) {
            JetExpression returnedExpression = expression.getReturnedExpression();
            if (returnedExpression != null) {
                generateInstructions(returnedExpression, false);
            }
            JetSimpleNameExpression labelElement = expression.getTargetLabel();
            JetElement subroutine;
            String labelName = expression.getLabelName();
            if (labelElement != null) {
                assert labelName != null;
                PsiElement labeledElement = BindingContextUtils.resolveToDeclarationPsiElement(trace.getBindingContext(), labelElement);
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
        public void visitParameter(JetParameter parameter) {
            builder.declare(parameter);
            JetExpression defaultValue = parameter.getDefaultValue();
            if (defaultValue != null) {
                generateInstructions(defaultValue, inCondition);
            }
            builder.write(parameter, parameter);
        }

        @Override
        public void visitBlockExpression(JetBlockExpression expression) {
            List<JetElement> statements = expression.getStatements();
            for (JetElement statement : statements) {
                generateInstructions(statement, false);
            }
            if (statements.isEmpty()) {
                builder.readUnit(expression);
            }
        }

        @Override
        public void visitNamedFunction(JetNamedFunction function) {
            processLocalDeclaration(function);
        }

        @Override
        public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
            JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
            processLocalDeclaration(functionLiteral);
            builder.read(expression);
        }

        @Override
        public void visitQualifiedExpression(JetQualifiedExpression expression) {
            generateInstructions(expression.getReceiverExpression(), false);
            JetExpression selectorExpression = expression.getSelectorExpression();
            if (selectorExpression != null) {
                generateInstructions(selectorExpression, false);
            }
            builder.read(expression);
            if (trace.get(BindingContext.PROCESSED, expression)) {
                JetType type = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
                if (type != null && KotlinBuiltIns.getInstance().isNothing(type)) {
                    builder.jumpToError();
                }
            }
        }

        private void visitCall(JetCallElement call) {
            for (ValueArgument argument : call.getValueArguments()) {
                JetExpression argumentExpression = argument.getArgumentExpression();
                if (argumentExpression != null) {
                    generateInstructions(argumentExpression, false);
                }
            }

            for (JetExpression functionLiteral : call.getFunctionLiteralArguments()) {
                generateInstructions(functionLiteral, false);
            }
        }

        @Override
        public void visitCallExpression(JetCallExpression expression) {
            //inline functions after M1
//            ResolvedCall<? extends CallableDescriptor> resolvedCall = trace.get(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());
//            assert resolvedCall != null;
//            CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
//            PsiElement element = trace.get(BindingContext.DESCRIPTOR_TO_DECLARATION, resultingDescriptor);
//            if (element instanceof JetNamedFunction) {
//                JetNamedFunction namedFunction = (JetNamedFunction) element;
//                if (namedFunction.hasModifier(JetTokens.INLINE_KEYWORD)) {
//                }
//            }

            for (JetTypeProjection typeArgument : expression.getTypeArguments()) {
                generateInstructions(typeArgument, false);
            }

            visitCall(expression);

            generateInstructions(expression.getCalleeExpression(), false);
            builder.read(expression);
            if (trace.get(BindingContext.PROCESSED, expression)) {
                JetType type = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
                if (type != null && KotlinBuiltIns.getInstance().isNothing(type)) {
                    builder.jumpToError();
                }
            }
        }

//        @Override
//        public void visitNewExpression(JetNewExpression expression) {
//            // TODO : Instantiated class is loaded
//            // TODO : type arguments?
//            visitCall(expression);
//            builder.read(expression);
//        }

        @Override
        public void visitProperty(JetProperty property) {
            builder.declare(property);
            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                generateInstructions(initializer, false);
                builder.write(property, property);
            }
            for (JetPropertyAccessor accessor : property.getAccessors()) {
                generateInstructions(accessor, false);
            }
        }

        @Override
        public void visitMultiDeclaration(JetMultiDeclaration declaration) {
            JetExpression initializer = declaration.getInitializer();
            if (initializer != null) {
                generateInstructions(initializer, false);
            }
            List<JetMultiDeclarationEntry> entries = declaration.getEntries();
            for (JetMultiDeclarationEntry entry : entries) {
                builder.declare(entry);
                builder.write(entry, entry);
            }
        }

        @Override
        public void visitPropertyAccessor(JetPropertyAccessor accessor) {
            processLocalDeclaration(accessor);
        }

        @Override
        public void visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression) {
            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            if (operationType == JetTokens.COLON || operationType == JetTokens.AS_KEYWORD || operationType == JetTokens.AS_SAFE) {
                generateInstructions(expression.getLeft(), false);
                builder.read(expression);
            }
            else {
                visitJetElement(expression);
            }
        }

        @Override
        public void visitThrowExpression(JetThrowExpression expression) {
            JetExpression thrownExpression = expression.getThrownExpression();
            if (thrownExpression != null) {
                generateInstructions(thrownExpression, false);
            }
            builder.throwException(expression);
        }

        @Override
        public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
            for (JetExpression index : expression.getIndexExpressions()) {
                generateInstructions(index, false);
            }
            generateInstructions(expression.getArrayExpression(), false);
            // TODO : read 'get' or 'set' function
            builder.read(expression);
        }

        @Override
        public void visitIsExpression(JetIsExpression expression) {
            generateInstructions(expression.getLeftHandSide(), inCondition);
            // no CF for types
            // TODO : builder.read(expression.getPattern());
            builder.read(expression);
        }

        @Override
        public void visitWhenExpression(JetWhenExpression expression) {
            JetExpression subjectExpression = expression.getSubjectExpression();
            if (subjectExpression != null) {
                generateInstructions(subjectExpression, inCondition);
            }
            boolean hasElseOrIrrefutableBranch = false;

            Label doneLabel = builder.createUnboundLabel();

            Label nextLabel = null;
            Collection<Label> allowDeadLabels = Lists.newArrayList();
            for (Iterator<JetWhenEntry> iterator = expression.getEntries().iterator(); iterator.hasNext(); ) {
                JetWhenEntry whenEntry = iterator.next();

                builder.read(whenEntry);

                if (whenEntry.isElse()) {
                    hasElseOrIrrefutableBranch = true;
                    if (iterator.hasNext()) {
                        trace.report(ELSE_MISPLACED_IN_WHEN.on(whenEntry));
                    }
                }
                boolean isIrrefutable = whenEntry.isElse();
                if (isIrrefutable) {
                    hasElseOrIrrefutableBranch = true;
                }

                Label bodyLabel = builder.createUnboundLabel();

                JetWhenCondition[] conditions = whenEntry.getConditions();
                for (int i = 0; i < conditions.length; i++) {
                    JetWhenCondition condition = conditions[i];
                    condition.accept(conditionVisitor);
                    if (i + 1 < conditions.length) {
                        builder.nondeterministicJump(bodyLabel);
                    }
                }

                if (!isIrrefutable) {
                    nextLabel = builder.createUnboundLabel();
                    builder.nondeterministicJump(nextLabel);
                }

                builder.bindLabel(bodyLabel);
                generateInstructions(whenEntry.getExpression(), inCondition);
                builder.jump(doneLabel);

                if (!isIrrefutable) {
                    builder.bindLabel(nextLabel);
                }
            }
            builder.bindLabel(doneLabel);
            boolean isWhenExhaust = WhenChecker.isWhenExhaustive(expression, trace);
            if (!hasElseOrIrrefutableBranch && !isWhenExhaust) {
                trace.report(NO_ELSE_IN_WHEN.on(expression));
            }
        }

        @Override
        public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
            JetObjectDeclaration declaration = expression.getObjectDeclaration();
            generateInstructions(declaration, inCondition);

            List<JetDeclaration> declarations = declaration.getDeclarations();
            List<JetDeclaration> functions = Lists.newArrayList();
            for (JetDeclaration localDeclaration : declarations) {
                if (!(localDeclaration instanceof JetProperty) && !(localDeclaration instanceof JetClassInitializer)) {
                    functions.add(localDeclaration);
                }
            }
            for (JetDeclaration function : functions) {
                generateInstructions(function, inCondition);
            }
            builder.read(expression);
        }

        @Override
        public void visitObjectDeclaration(JetObjectDeclaration objectDeclaration) {
            visitClassOrObject(objectDeclaration);
        }

        @Override
        public void visitStringTemplateExpression(JetStringTemplateExpression expression) {
            for (JetStringTemplateEntry entry : expression.getEntries()) {
                if (entry instanceof JetStringTemplateEntryWithExpression) {
                    JetStringTemplateEntryWithExpression entryWithExpression = (JetStringTemplateEntryWithExpression) entry;
                    generateInstructions(entryWithExpression.getExpression(), false);
                }
            }
            builder.read(expression);
        }

        @Override
        public void visitTypeProjection(JetTypeProjection typeProjection) {
            // TODO : Support Type Arguments. Class object may be initialized at this point");
        }

        @Override
        public void visitAnonymousInitializer(JetClassInitializer classInitializer) {
            generateInstructions(classInitializer.getBody(), inCondition);
        }

        private void visitClassOrObject(JetClassOrObject classOrObject) {
            for (JetDelegationSpecifier specifier : classOrObject.getDelegationSpecifiers()) {
                generateInstructions(specifier, inCondition);
            }
            List<JetDeclaration> declarations = classOrObject.getDeclarations();
            for (JetDeclaration declaration : declarations) {
                if (declaration instanceof JetProperty || declaration instanceof JetClassInitializer) {
                    generateInstructions(declaration, inCondition);
                }
            }
        }

        @Override
        public void visitClass(JetClass klass) {
            List<JetParameter> parameters = klass.getPrimaryConstructorParameters();
            for (JetParameter parameter : parameters) {
                generateInstructions(parameter, inCondition);
            }
            visitClassOrObject(klass);
        }

        @Override
        public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
            List<? extends ValueArgument> valueArguments = call.getValueArguments();
            for (ValueArgument valueArgument : valueArguments) {
                generateInstructions(valueArgument.getArgumentExpression(), inCondition);
            }
        }

        @Override
        public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
            generateInstructions(specifier.getDelegateExpression(), inCondition);
        }

        @Override
        public void visitJetFile(JetFile file) {
            for (JetDeclaration declaration : file.getDeclarations()) {
                if (declaration instanceof JetProperty) {
                    generateInstructions(declaration, inCondition);
                }
            }
        }

        @Override
        public void visitJetElement(JetElement element) {
            builder.unsupported(element);
        }
    }
}
