package org.jetbrains.jet.lang.cfg;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

/**
* @author abreslav
*/
public class JetControlFlowProcessor {

//    private final Map<String, Stack<JetElement>> labeledElements = new HashMap<String, Stack<JetElement>>();

    private final JetControlFlowBuilder builder;
    private final BindingTrace trace;

    public JetControlFlowProcessor(BindingTrace trace, JetControlFlowBuilder builder) {
        this.builder = builder;
        this.trace = trace;
    }

    public void generate(@NotNull JetElement subroutineElement, @NotNull JetExpression body) {
        generateSubroutineControlFlow(subroutineElement, Collections.singletonList(body));
    }

    public void generateSubroutineControlFlow(@NotNull JetElement subroutineElement, @NotNull List<? extends JetElement> body) {
//        if (subroutineElement instanceof JetNamedDeclaration) {
//            JetNamedDeclaration namedDeclaration = (JetNamedDeclaration) subroutineElement;
//            enterLabeledElement(JetPsiUtil.safeName(namedDeclaration.getName()), namedDeclaration);
//        }
        boolean functionLiteral = subroutineElement instanceof JetFunctionLiteralExpression;
        builder.enterSubroutine(subroutineElement, functionLiteral);
        for (JetElement statement : body) {
            statement.accept(new CFPVisitor(false));
        }
        builder.exitSubroutine(subroutineElement, functionLiteral);
    }

//    private void enterLabeledElement(@NotNull String labelName, @NotNull JetElement labeledElement) {
//        Stack<JetElement> stack = labeledElements.get(labelName);
//        if (stack == null) {
//            stack = new Stack<JetElement>();
//            labeledElements.put(labelName, stack);
//        }
//        stack.push(labeledElement);
//    }
//
//    private void exitElement(JetElement element) {
//        // TODO : really suboptimal
//        for (Iterator<Map.Entry<String, Stack<JetElement>>> mapIter = labeledElements.entrySet().iterator(); mapIter.hasNext(); ) {
//            Map.Entry<String, Stack<JetElement>> entry = mapIter.next();
//            Stack<JetElement> stack = entry.getValue();
//            for (Iterator<JetElement> stackIter = stack.iterator(); stackIter.hasNext(); ) {
//                JetElement recorded = stackIter.next();
//                if (recorded == element) {
//                    stackIter.remove();
//                }
//            }
//            if (stack.isEmpty()) {
//                mapIter.remove();
//            }
//        }
//    }

//    @Nullable
//    private JetElement resolveLabel(@NotNull String labelName, @NotNull JetSimpleNameExpression labelExpression, boolean reportUnresolved) {
//        Stack<JetElement> stack = labeledElements.get(labelName);
//        if (stack == null || stack.isEmpty()) {
//            if (reportUnresolved) {
////                trace.report(UNRESOLVED_REFERENCE.on(labelExpression));
//            }
//            return null;
//        }
//        else if (stack.size() > 1) {
////            trace.getErrorHandler().genericWarning(labelExpression.getNode(), "There is more than one label with such a name in this scope");
////            trace.report(LABEL_NAME_CLASH.on(labelExpression));
//        }
//
//        JetElement result = stack.peek();
////        trace.record(BindingContext.LABEL_TARGET, labelExpression, result);
//        return result;
//    }

    private class CFPVisitor extends JetVisitorVoid {
        private final boolean inCondition;

        private CFPVisitor(boolean inCondition) {
            this.inCondition = inCondition;
        }

        private void value(@Nullable JetElement element, boolean inCondition) {
            if (element == null) return;
            CFPVisitor visitor;
            if (this.inCondition == inCondition) {
                visitor = this;
            }
            else {
                visitor = new CFPVisitor(inCondition);
            }
            element.accept(visitor);
//            exitElement(element);
        }

        @Override
        public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
            JetExpression innerExpression = expression.getExpression();
            if (innerExpression != null) {
                value(innerExpression, inCondition);
            }
        }

        @Override
        public void visitThisExpression(JetThisExpression expression) {
//            JetSimpleNameExpression targetLabel = expression.getTargetLabel();
//            if (targetLabel != null) {
//                String labelName = expression.getLabelName();
//                assert labelName != null;
//                resolveLabel(labelName, targetLabel, false);
//            }
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
                if (type != null && JetStandardClasses.isNothing(type)) {
                    builder.jumpToError(expression);
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
            JetExpression deparenthesized = JetPsiUtil.deparenthesize(labeledExpression);
            if (deparenthesized != null) {
//                enterLabeledElement(labelName, deparenthesized);
                value(labeledExpression, inCondition);
            }
        }

        @Override
        public void visitBinaryExpression(JetBinaryExpression expression) {
            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            JetExpression right = expression.getRight();
            if (operationType == JetTokens.ANDAND) {
                value(expression.getLeft(), true);
                Label resultLabel = builder.createUnboundLabel();
                builder.jumpOnFalse(resultLabel);
                if (right != null) {
                    value(right, true);
                }
                builder.bindLabel(resultLabel);
                if (!inCondition) {
                    builder.read(expression);
                }
            }
            else if (operationType == JetTokens.OROR) {
                value(expression.getLeft(), true);
                Label resultLabel = builder.createUnboundLabel();
                builder.jumpOnTrue(resultLabel);
                if (right != null) {
                    value(right, true);
                }
                builder.bindLabel(resultLabel);
                if (!inCondition) {
                    builder.read(expression);
                }
            }
            else if (operationType == JetTokens.EQ) {
                JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
                if (right != null) {
                    value(right, false);
                }
                if (left instanceof JetSimpleNameExpression) {
                    builder.write(expression, left);
                }
                else if (left instanceof JetArrayAccessExpression) {
                    JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
                    visitAssignToArrayAccess(expression, arrayAccessExpression);
                } else if (left instanceof JetQualifiedExpression) {
                    assert !(left instanceof JetPredicateExpression) : left; // TODO
                    assert !(left instanceof JetHashQualifiedExpression) : left; // TODO
                    JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) left;
                    value(qualifiedExpression.getReceiverExpression(), false);
                    value(expression.getOperationReference(), false);
                    builder.write(expression, left);
                } else {
                    builder.unsupported(expression); // TODO
                }
            }
            else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
                JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
                if (left != null) {
                    value(left, false);
                }
                if (right != null) {
                    value(right, false);
                }
                if (left instanceof JetSimpleNameExpression || left instanceof JetArrayAccessExpression) {
                    value(expression.getOperationReference(), false);
                    builder.write(expression, left);
                }
                else if (left != null) {
                    builder.unsupported(expression); // TODO
                }
            }
            else {
                value(expression.getLeft(), false);
                if (right != null) {
                    value(right, false);
                }
                value(expression.getOperationReference(), false);
                builder.read(expression);
            }
        }

        private void visitAssignToArrayAccess(JetBinaryExpression expression, JetArrayAccessExpression arrayAccessExpression) {
            for (JetExpression index : arrayAccessExpression.getIndexExpressions()) {
                value(index, false);
            }
            value(arrayAccessExpression.getArrayExpression(), false);
            value(expression.getOperationReference(), false);
            builder.write(expression, arrayAccessExpression); // TODO : ???
        }

        @Override
        public void visitUnaryExpression(JetUnaryExpression expression) {
            JetSimpleNameExpression operationSign = expression.getOperationSign();
            IElementType operationType = operationSign.getReferencedNameElementType();
            JetExpression baseExpression = expression.getBaseExpression();
            if (JetTokens.LABELS.contains(operationType)) {
                String referencedName = operationSign.getReferencedName();
                referencedName = referencedName == null ? " <?>" : referencedName;
                visitLabeledExpression(referencedName.substring(1), baseExpression);
            }
            else {
                value(baseExpression, false);
                value(operationSign, false);

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
                value(condition, true);
            }
            Label elseLabel = builder.createUnboundLabel();
            builder.jumpOnFalse(elseLabel);
            JetExpression thenBranch = expression.getThen();
            if (thenBranch != null) {
                value(thenBranch, inCondition);
            }
            else {
                builder.readUnit(expression);
            }
            Label resultLabel = builder.createUnboundLabel();
            builder.jump(resultLabel);
            builder.bindLabel(elseLabel);
            JetExpression elseBranch = expression.getElse();
            if (elseBranch != null) {
                value(elseBranch, inCondition);
            }
            else {
                builder.readUnit(expression);
            }
            builder.bindLabel(resultLabel);
        }

        @Override
        public void visitTryExpression(JetTryExpression expression) {
            final JetFinallySection finallyBlock = expression.getFinallyBlock();
            if (finallyBlock != null) {
                builder.enterTryFinally(new GenerationTrigger() {
                    private boolean working = false;

                    @Override
                    public void generate() {
                        // This checks are needed for the case of having e.g. return inside finally: 'try {return} finally{return}'
                        if (working) return;
                        working = true;
                        value(finallyBlock.getFinalExpression(), inCondition);
                        working = false;
                    }
                });
            }

            Label onException = builder.createUnboundLabel();
            builder.nondeterministicJump(onException);
            value(expression.getTryBlock(), inCondition);

            List<JetCatchClause> catchClauses = expression.getCatchClauses();
            if (!catchClauses.isEmpty()) {
                Label afterCatches = builder.createUnboundLabel();
                builder.jump(afterCatches);

                builder.bindLabel(onException);
                for (Iterator<JetCatchClause> iterator = catchClauses.iterator(); iterator.hasNext(); ) {
                    JetCatchClause catchClause = iterator.next();
                    JetExpression catchBody = catchClause.getCatchBody();
                    if (catchBody != null) {
                        value(catchBody, false);
                    }
                    if (iterator.hasNext()) {
                        builder.nondeterministicJump(afterCatches);
                    }
                }

                builder.bindLabel(afterCatches);
            } else {
                builder.bindLabel(onException);
            }

            if (finallyBlock != null) {
                builder.exitTryFinally();
                value(finallyBlock.getFinalExpression(), inCondition);
            }
        }

        @Override
        public void visitWhileExpression(JetWhileExpression expression) {
            LoopInfo loopInfo = builder.enterLoop(expression, null, null);

            builder.bindLabel(loopInfo.getConditionEntryPoint());
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                value(condition, true);
            }
            builder.jumpOnFalse(loopInfo.getExitPoint());

            builder.bindLabel(loopInfo.getBodyEntryPoint());
            JetExpression body = expression.getBody();
            if (body != null) {
                value(body, false);
            }
            builder.jump(loopInfo.getEntryPoint());
            builder.exitLoop(expression);
            builder.readUnit(expression);
        }

        @Override
        public void visitDoWhileExpression(JetDoWhileExpression expression) {
            LoopInfo loopInfo = builder.enterLoop(expression, null, null);

            builder.bindLabel(loopInfo.getBodyEntryPoint());
            JetExpression body = expression.getBody();
            if (body != null) {
                value(body, false);
            }
            builder.bindLabel(loopInfo.getConditionEntryPoint());
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                value(condition, true);
            }
            builder.jumpOnTrue(loopInfo.getEntryPoint());
            builder.exitLoop(expression);
            builder.readUnit(expression);
        }

        @Override
        public void visitForExpression(JetForExpression expression) {
            JetExpression loopRange = expression.getLoopRange();
            if (loopRange != null) {
                value(loopRange, false);
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
                value(body, false);
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
//                loop = resolveLabel(labelName, targetLabel, true);
//                if (!isLoop(loop)) {
////                    trace.getErrorHandler().genericError(expression.getNode(), "The label '" + targetLabel.getText() + "' does not denote a loop");
//                    trace.report(NOT_A_LOOP_LABEL.on(expression, targetLabel.getText()));
//                    loop = null;
//                }
            }
            else {
                loop = builder.getCurrentLoop();
                if (loop == null) {
//                    trace.getErrorHandler().genericError(expression.getNode(), "'break' and 'continue' are only allowed inside a loop");
                    trace.report(BREAK_OR_CONTINUE_OUTSIDE_A_LOOP.on(expression));
                }
            }
            return loop;
        }

//        private boolean isLoop(JetElement loop) {
//            return loop instanceof JetWhileExpression ||
//                   loop instanceof JetDoWhileExpression ||
//                   loop instanceof JetForExpression;
//        }

        @Override
        public void visitReturnExpression(JetReturnExpression expression) {
            JetExpression returnedExpression = expression.getReturnedExpression();
            if (returnedExpression != null) {
                value(returnedExpression, false);
            }
            JetSimpleNameExpression labelElement = expression.getTargetLabel();
            JetElement subroutine;
            if (labelElement != null) {
                String labelName = expression.getLabelName();
                assert labelName != null;
                PsiElement labeledElement = BindingContextUtils.resolveToDeclarationPsiElement(trace.getBindingContext(), labelElement);
                if (labeledElement != null) {
                    assert labeledElement instanceof JetElement;
                    subroutine = (JetElement) labeledElement;
                }
                else {
                    subroutine = null;
                }
                //subroutine = resolveLabel(labelName, labelElement, true);
            }
            else {
                subroutine = builder.getCurrentSubroutine();
                // TODO : a context check
            }
            if (subroutine instanceof JetFunction || subroutine instanceof JetFunctionLiteralExpression) {
                if (returnedExpression == null) {
                    builder.returnNoValue(expression, subroutine);
                }
                else {
                    builder.returnValue(expression, subroutine);
                }
            }
            else {
                if (labelElement != null) {
                    trace.report(NOT_A_RETURN_LABEL.on(expression, expression.getLabelName()));
                }
            }
        }

        @Override
        public void visitBlockExpression(JetBlockExpression expression) {
            List<JetElement> statements = expression.getStatements();
            for (JetElement statement : statements) {
                value(statement, false);
            }
            if (statements.isEmpty()) {
                builder.readUnit(expression);
            }
        }

        @Override
        public void visitNamedFunction(JetNamedFunction function) {
            JetExpression bodyExpression = function.getBodyExpression();
            if (bodyExpression != null) {
                generate(function, bodyExpression);
            }
        }

        @Override
        public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
            JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
            JetBlockExpression bodyExpression = expression.getFunctionLiteral().getBodyExpression();
            if (bodyExpression != null) {
                List<JetElement> statements = bodyExpression.getStatements();
                generateSubroutineControlFlow(expression, statements);
            }
        }

        @Override
        public void visitQualifiedExpression(JetQualifiedExpression expression) {
            value(expression.getReceiverExpression(), false);
            JetExpression selectorExpression = expression.getSelectorExpression();
            if (selectorExpression != null) {
                value(selectorExpression, false);
            }
            builder.read(expression);
            if (trace.get(BindingContext.PROCESSED, expression)) {
                JetType type = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
                if (type != null && JetStandardClasses.isNothing(type)) {
                    builder.jumpToError(expression);
                }
            }
        }

        private void visitCall(JetCallElement call) {
            for (ValueArgument argument : call.getValueArguments()) {
                JetExpression argumentExpression = argument.getArgumentExpression();
                if (argumentExpression != null) {
                    value(argumentExpression, false);
                }
            }

            for (JetExpression functionLiteral : call.getFunctionLiteralArguments()) {
                value(functionLiteral, false);
            }
        }

        @Override
        public void visitCallExpression(JetCallExpression expression) {
            for (JetTypeProjection typeArgument : expression.getTypeArguments()) {
                value(typeArgument, false);
            }

            visitCall(expression);

            value(expression.getCalleeExpression(), false);
            builder.read(expression);
            if (trace.get(BindingContext.PROCESSED, expression)) {
                JetType type = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
                if (type != null && JetStandardClasses.isNothing(type)) {
                    builder.jumpToError(expression);
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
            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                value(initializer, false);
                builder.write(property, property);
            }
        }

        @Override
        public void visitTupleExpression(JetTupleExpression expression) {
            for (JetExpression entry : expression.getEntries()) {
                value(entry, false);
            }
            builder.read(expression);
        }

        @Override
        public void visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression) {
            IElementType operationType = expression.getOperationSign().getReferencedNameElementType();
            if (operationType == JetTokens.COLON || operationType == JetTokens.AS_KEYWORD || operationType == JetTokens.AS_SAFE) {
                value(expression.getLeft(), false);
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
                value(thrownExpression, false);
            }
            builder.jumpToError(expression);
        }

        @Override
        public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
            for (JetExpression index : expression.getIndexExpressions()) {
                value(index, false);
            }
            value(expression.getArrayExpression(), false);
            // TODO : read 'get' or 'set' function
            builder.read(expression);
        }

        @Override
        public void visitIsExpression(JetIsExpression expression) {
            value(expression.getLeftHandSide(), inCondition);
            // TODO : builder.read(expression.getPattern());
            builder.read(expression);
        }

        @Override
        public void visitWhenExpression(JetWhenExpression expression) {
            // TODO : no more than one else
            // TODO : else must be the last
            JetExpression subjectExpression = expression.getSubjectExpression();
            if (subjectExpression != null) {
                value(subjectExpression, inCondition);
            }

            Label doneLabel = builder.createUnboundLabel();

            Label nextLabel = builder.createUnboundLabel();
            for (Iterator<JetWhenEntry> iterator = expression.getEntries().iterator(); iterator.hasNext(); ) {
                JetWhenEntry whenEntry = iterator.next();

                if (whenEntry.isElse()) {
                    if (iterator.hasNext()) {
//                        trace.getErrorHandler().genericError(whenEntry.getNode(), "'else' entry must be the last one in a when-expression");
                        trace.report(ELSE_MISPLACED_IN_WHEN.on(whenEntry));
                    }
                }

                Label bodyLabel = builder.createUnboundLabel();

                JetWhenCondition[] conditions = whenEntry.getConditions();
                for (int i = 0; i < conditions.length; i++) {
                    JetWhenCondition condition = conditions[i];
                    condition.accept(new JetVisitorVoid() {
                        private final JetVisitorVoid conditionVisitor = this;

                        @Override
                        public void visitWhenConditionCall(JetWhenConditionCall condition) {
                            value(condition.getCallSuffixExpression(), inCondition); // TODO : inCondition?
                        }

                        @Override
                        public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
                            value(condition.getRangeExpression(), inCondition); // TODO : inCondition?
                            value(condition.getOperationReference(), inCondition); // TODO : inCondition?
                            // TODO : read the call to contains()...
                        }

                        @Override
                        public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
                            JetPattern pattern = condition.getPattern();
                            if (pattern != null) {
                                pattern.accept(new JetVisitorVoid() {
                                    @Override
                                    public void visitTypePattern(JetTypePattern typePattern) {
                                        // TODO
                                    }

                                    @Override
                                    public void visitWildcardPattern(JetWildcardPattern pattern) {
                                        // TODO
                                    }

                                    @Override
                                    public void visitExpressionPattern(JetExpressionPattern pattern) {
                                        value(pattern.getExpression(), inCondition);
                                    }

                                    @Override
                                    public void visitTuplePattern(JetTuplePattern pattern) {
                                        // TODO
                                    }

                                    @Override
                                    public void visitDecomposerPattern(JetDecomposerPattern pattern) {
                                        value(pattern.getDecomposerExpression(), inCondition);
                                        pattern.getArgumentList().accept(this);
                                    }

                                    @Override
                                    public void visitBindingPattern(JetBindingPattern pattern) {
                                        JetWhenCondition condition = pattern.getCondition();
                                        if (condition != null) {
                                            condition.accept(conditionVisitor);
                                        }
                                    }

                                    @Override
                                    public void visitJetElement(JetElement element) {
                                        throw new UnsupportedOperationException("[JetControlFlowProcessor] " + element.toString());
                                    }
                                });
                            }
                        }

                        @Override
                        public void visitJetElement(JetElement element) {
                            throw new UnsupportedOperationException("[JetControlFlowProcessor] " + element.toString());
                        }
                    });
                    if (i + 1 < conditions.length) {
                        builder.nondeterministicJump(bodyLabel);
                    }
                }

                builder.nondeterministicJump(nextLabel);

                builder.bindLabel(bodyLabel);
                value(whenEntry.getExpression(), inCondition);
                builder.jump(doneLabel);
                builder.bindLabel(nextLabel);
                nextLabel = builder.createUnboundLabel();
            }
            // TODO : if there's else, no error can happen
            builder.jumpToError(null);
            builder.bindLabel(doneLabel);
        }

        @Override
        public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
//            List<JetDelegationSpecifier> delegationSpecifiers = expression.getObjectDeclaration().getDelegationSpecifiers();
//            for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
//                if (delegationSpecifier instanceof JetDelegatorByExpressionSpecifier) {
//                    JetDelegatorByExpressionSpecifier specifier = (JetDelegatorByExpressionSpecifier) delegationSpecifier;
//                    JetExpression delegateExpression = specifier.getDelegateExpression();
//                    if (delegateExpression != null) {
//                        value(delegateExpression, false, false);
//                    }
//                }
//            }
            value(expression.getObjectDeclaration(), inCondition);
            builder.read(expression);
        }

        @Override
        public void visitObjectDeclaration(JetObjectDeclaration declaration) {
            for (JetDelegationSpecifier delegationSpecifier : declaration.getDelegationSpecifiers()) {
                value(delegationSpecifier, inCondition);
            }
            for (JetDeclaration jetDeclaration : declaration.getDeclarations()) {
                FOR_LOCAL_CLASSES.value(jetDeclaration, false);
            }
        }

        @Override
        public void visitStringTemplateExpression(JetStringTemplateExpression expression) {
            for (JetStringTemplateEntry entry : expression.getEntries()) {
                if (entry instanceof JetStringTemplateEntryWithExpression) {
                    JetStringTemplateEntryWithExpression entryWithExpression = (JetStringTemplateEntryWithExpression) entry;
                    value(entryWithExpression.getExpression(), false);
                }
            }
            builder.read(expression);
        }

        @Override
        public void visitTypeProjection(JetTypeProjection typeProjection) {
            // TODO : Support Type Arguments. Class object may be initialized at this point");
        }

        @Override
        public void visitJetElement(JetElement element) {
            builder.unsupported(element);
        }
    }

    private final CFPVisitor FOR_LOCAL_CLASSES = new CFPVisitor(false) {
        @Override
        public void visitNamedFunction(JetNamedFunction function) {
            // Nothing
        }
    };
}
