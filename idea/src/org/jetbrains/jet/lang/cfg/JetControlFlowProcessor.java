package org.jetbrains.jet.lang.cfg;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.JetTypeInferrer;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
* @author abreslav
*/
public class JetControlFlowProcessor {

    private final Map<String, Stack<JetElement>> labeledElements = new HashMap<String, Stack<JetElement>>();

    private final JetSemanticServices semanticServices;
    private final JetControlFlowBuilder builder;
    private final BindingTrace trace;

    public JetControlFlowProcessor(JetSemanticServices semanticServices, BindingTrace trace, JetControlFlowBuilder builder) {
        this.semanticServices = semanticServices;
        this.builder = builder;
        this.trace = trace;
    }

    public void generate(@NotNull JetElement subroutineElement, @NotNull JetExpression body) {
        generateSubroutineControlFlow(subroutineElement, Collections.singletonList(body), false);
    }

    public void generateSubroutineControlFlow(@NotNull JetElement subroutineElement, @NotNull List<? extends JetElement> body, boolean preferBlocks) {
        if (subroutineElement instanceof JetNamedDeclaration) {
            JetNamedDeclaration namedDeclaration = (JetNamedDeclaration) subroutineElement;
            enterLabeledElement(JetPsiUtil.safeName(namedDeclaration.getName()), namedDeclaration);
        }
        boolean functionLiteral = subroutineElement instanceof JetFunctionLiteralExpression;
        builder.enterSubroutine(subroutineElement, functionLiteral);
        for (JetElement statement : body) {
            statement.accept(new CFPVisitor(preferBlocks, false));
        }
        builder.exitSubroutine(subroutineElement, functionLiteral);
    }

    private void enterLabeledElement(@NotNull String labelName, @NotNull JetElement labeledElement) {
        Stack<JetElement> stack = labeledElements.get(labelName);
        if (stack == null) {
            stack = new Stack<JetElement>();
            labeledElements.put(labelName, stack);
        }
        stack.push(labeledElement);
    }

    private void exitElement(JetElement element) {
        // TODO : really suboptimal
        for (Iterator<Map.Entry<String, Stack<JetElement>>> mapIter = labeledElements.entrySet().iterator(); mapIter.hasNext(); ) {
            Map.Entry<String, Stack<JetElement>> entry = mapIter.next();
            Stack<JetElement> stack = entry.getValue();
            for (Iterator<JetElement> stackIter = stack.iterator(); stackIter.hasNext(); ) {
                JetElement recorded = stackIter.next();
                if (recorded == element) {
                    stackIter.remove();
                }
            }
            if (stack.isEmpty()) {
                mapIter.remove();
            }
        }
    }

    @Nullable
    private JetElement resolveLabel(@NotNull String labelName, @NotNull JetSimpleNameExpression labelExpression) {
        Stack<JetElement> stack = labeledElements.get(labelName);
        if (stack == null || stack.isEmpty()) {
            trace.getErrorHandler().unresolvedReference(labelExpression);
            return null;
        }
        else if (stack.size() > 1) {
            trace.getErrorHandler().genericWarning(labelExpression.getNode(), "There is more than one label with such a name in this scope");
        }

        JetElement result = stack.peek();
        trace.recordLabelResolution(labelExpression, result);
        return result;
    }

    private class CFPVisitor extends JetVisitor {
        private final boolean preferBlock;
        private final boolean inCondition;

        private CFPVisitor(boolean preferBlock, boolean inCondition) {
            this.preferBlock = preferBlock;
            this.inCondition = inCondition;
        }

        private void value(@Nullable JetElement element, boolean preferBlock, boolean inCondition) {
            if (element == null) return;
            CFPVisitor visitor;
            if (this.preferBlock == preferBlock && this.inCondition == inCondition) {
                visitor = this;
            }
            else {
                visitor = new CFPVisitor(preferBlock, inCondition);
            }
            element.accept(visitor);
            exitElement(element);
        }

        @Override
        public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
            JetExpression innerExpression = expression.getExpression();
            if (innerExpression != null) {
                value(innerExpression, false, inCondition);
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
                enterLabeledElement(labelName, deparenthesized);
                value(labeledExpression, false, inCondition);
            }
        }

        @Override
        public void visitBinaryExpression(JetBinaryExpression expression) {
            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            JetExpression right = expression.getRight();
            if (operationType == JetTokens.ANDAND) {
                value(expression.getLeft(), false, true);
                Label resultLabel = builder.createUnboundLabel();
                builder.jumpOnFalse(resultLabel);
                if (right != null) {
                    value(right, false, true);
                }
                builder.bindLabel(resultLabel);
                if (!inCondition) {
                    builder.read(expression);
                }
            }
            else if (operationType == JetTokens.OROR) {
                value(expression.getLeft(), false, true);
                Label resultLabel = builder.createUnboundLabel();
                builder.jumpOnTrue(resultLabel);
                if (right != null) {
                    value(right, false, true);
                }
                builder.bindLabel(resultLabel);
                if (!inCondition) {
                    builder.read(expression);
                }
            }
            else if (operationType == JetTokens.EQ) {
                JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
                if (right != null) {
                    value(right, false, false);
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
                    value(qualifiedExpression.getReceiverExpression(), false, false);
                    value(expression.getOperationReference(), false, false);
                    builder.write(expression, left);
                } else {
                    builder.unsupported(expression); // TODO
                }
            }
            else if (JetTypeInferrer.assignmentOperationNames.containsKey(operationType)) {
                JetExpression left = JetPsiUtil.deparenthesize(expression.getLeft());
                if (left != null) {
                    value(left, false, false);
                }
                if (right != null) {
                    value(right, false, false);
                }
                if (left instanceof JetSimpleNameExpression || left instanceof JetArrayAccessExpression) {
                    value(expression.getOperationReference(), false, false);
                    builder.write(expression, left);
                }
                else if (left != null) {
                    builder.unsupported(expression); // TODO
                }
            }
            else {
                value(expression.getLeft(), false, false);
                if (right != null) {
                    value(right, false, false);
                }
                value(expression.getOperationReference(), false, false);
                builder.read(expression);
            }
        }

        private void visitAssignToArrayAccess(JetBinaryExpression expression, JetArrayAccessExpression arrayAccessExpression) {
            for (JetExpression index : arrayAccessExpression.getIndexExpressions()) {
                value(index, false, false);
            }
            value(arrayAccessExpression.getArrayExpression(), false, false);
            value(expression.getOperationReference(), false, false);
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
                value(baseExpression, false, false);
                value(operationSign, false, false);

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
                value(condition, false, true);
            }
            Label elseLabel = builder.createUnboundLabel();
            builder.jumpOnFalse(elseLabel);
            JetExpression thenBranch = expression.getThen();
            if (thenBranch != null) {
                value(thenBranch, true, inCondition);
            }
            else {
                builder.readUnit(expression);
            }
            Label resultLabel = builder.createUnboundLabel();
            builder.jump(resultLabel);
            builder.bindLabel(elseLabel);
            JetExpression elseBranch = expression.getElse();
            if (elseBranch != null) {
                value(elseBranch, true, inCondition);
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
                        value(finallyBlock.getFinalExpression(), true, inCondition);
                        working = false;
                    }
                });
            }

            Label onException = builder.createUnboundLabel();
            builder.nondeterministicJump(onException);
            value(expression.getTryBlock(), true, inCondition);

            List<JetCatchClause> catchClauses = expression.getCatchClauses();
            if (!catchClauses.isEmpty()) {
                Label afterCatches = builder.createUnboundLabel();
                builder.jump(afterCatches);

                builder.bindLabel(onException);
                for (Iterator<JetCatchClause> iterator = catchClauses.iterator(); iterator.hasNext(); ) {
                    JetCatchClause catchClause = iterator.next();
                    JetExpression catchBody = catchClause.getCatchBody();
                    if (catchBody != null) {
                        value(catchBody, true, false);
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
                value(finallyBlock.getFinalExpression(), true, inCondition);
            }
        }

        @Override
        public void visitWhileExpression(JetWhileExpression expression) {
            Label loopExitPoint = builder.createUnboundLabel();
            Label loopEntryPoint = builder.enterLoop(expression, loopExitPoint);
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                value(condition, false, true);
            }
            builder.jumpOnFalse(loopExitPoint);
            JetExpression body = expression.getBody();
            if (body != null) {
                value(body, true, false);
            }
            builder.jump(loopEntryPoint);
            builder.exitLoop(expression);
            builder.readUnit(expression);
        }

        @Override
        public void visitDoWhileExpression(JetDoWhileExpression expression) {
            Label loopExitPoint = builder.createUnboundLabel();
            Label loopEntryPoint = builder.enterLoop(expression, loopExitPoint);
            JetExpression body = expression.getBody();
            if (body != null) {
                value(body, true, false);
            }
            JetExpression condition = expression.getCondition();
            if (condition != null) {
                value(condition, false, true);
            }
            builder.jumpOnTrue(loopEntryPoint);
            builder.exitLoop(expression);
            builder.readUnit(expression);
        }

        @Override
        public void visitForExpression(JetForExpression expression) {
            JetExpression loopRange = expression.getLoopRange();
            if (loopRange != null) {
                value(loopRange, false, false);
            }
            // TODO : primitive cases
            Label loopExitPoint = builder.createUnboundLabel();
            builder.nondeterministicJump(loopExitPoint);
            Label loopEntryPoint = builder.enterLoop(expression, loopExitPoint);
            JetExpression body = expression.getBody();
            if (body != null) {
                value(body, true, false);
            }
            builder.nondeterministicJump(loopEntryPoint);
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
                loop = resolveLabel(labelName, targetLabel);
                if (!isLoop(loop)) {
                    trace.getErrorHandler().genericError(expression.getNode(), "The label '" + targetLabel.getText() + "' does not denote a loop");
                    loop = null;
                }
            }
            else {
                loop = builder.getCurrentLoop();
                if (loop == null) {
                    trace.getErrorHandler().genericError(expression.getNode(), "'break' and 'continue' are only allowed inside a loop");
                }
            }
            return loop;
        }

        private boolean isLoop(JetElement loop) {
            return loop instanceof JetWhileExpression ||
                   loop instanceof JetDoWhileExpression ||
                   loop instanceof JetForExpression;
        }

        @Override
        public void visitReturnExpression(JetReturnExpression expression) {
            JetExpression returnedExpression = expression.getReturnedExpression();
            if (returnedExpression != null) {
                value(returnedExpression, false, false);
            }
            JetSimpleNameExpression labelElement = expression.getTargetLabel();
            JetElement subroutine;
            if (labelElement != null) {
                String labelName = expression.getLabelName();
                assert labelName != null;
                subroutine = resolveLabel(labelName, labelElement);
            }
            else {
                subroutine = builder.getCurrentSubroutine();
                // TODO : a context check
            }
            if (subroutine != null) {
                if (returnedExpression == null) {
                    builder.returnNoValue(expression, subroutine);
                }
                else {
                    builder.returnValue(expression, subroutine);
                }
            }
        }

        @Override
        public void visitBlockExpression(JetBlockExpression expression) {
            for (JetElement statement : expression.getStatements()) {
                value(statement, true, false);
            }
        }

        @Override
        public void visitFunction(JetFunction function) {
            JetExpression bodyExpression = function.getBodyExpression();
            if (bodyExpression != null) {
                generate(function, bodyExpression);
            }
        }

        @Override
        public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
            if (preferBlock && !expression.hasParameterSpecification()) {
                for (JetElement statement : expression.getBody()) {
                    value(statement, true, false);
                }
            }
            else {
                generateSubroutineControlFlow(expression, expression.getBody(), true);
            }
        }

        @Override
        public void visitQualifiedExpression(JetQualifiedExpression expression) {
            value(expression.getReceiverExpression(), false, false);
            JetExpression selectorExpression = expression.getSelectorExpression();
            if (selectorExpression != null) {
                value(selectorExpression, false, false);
            }
            builder.read(expression);
        }

        private void visitCall(JetCall call) {
            for (JetArgument argument : call.getValueArguments()) {
                JetExpression argumentExpression = argument.getArgumentExpression();
                if (argumentExpression != null) {
                    value(argumentExpression, false, false);
                }
            }

            for (JetExpression functionLiteral : call.getFunctionLiteralArguments()) {
                value(functionLiteral, false, false);
            }
        }

        @Override
        public void visitCallExpression(JetCallExpression expression) {
            for (JetTypeProjection typeArgument : expression.getTypeArguments()) {
                value(typeArgument, false, false);
            }

            visitCall(expression);

            value(expression.getCalleeExpression(), false, false);
            builder.read(expression);
        }

        @Override
        public void visitNewExpression(JetNewExpression expression) {
            // TODO : Instantiated class is loaded
            // TODO : type arguments?
            visitCall(expression);
            builder.read(expression);
        }

        @Override
        public void visitProperty(JetProperty property) {
            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                value(initializer, false, false);
                builder.write(property, property);
            }
        }

        @Override
        public void visitTupleExpression(JetTupleExpression expression) {
            for (JetExpression entry : expression.getEntries()) {
                value(entry, false, false);
            }
            builder.read(expression);
        }

        @Override
        public void visitBinaryWithTypeRHSExpression(JetBinaryExpressionWithTypeRHS expression) {
            IElementType operationType = expression.getOperationSign().getReferencedNameElementType();
            if (operationType == JetTokens.COLON || operationType == JetTokens.AS_KEYWORD || operationType == JetTokens.AS_SAFE) {
                value(expression.getLeft(), false, false);
            }
            else {
                visitJetElement(expression);
            }
        }

        @Override
        public void visitThrowExpression(JetThrowExpression expression) {
            JetExpression thrownExpression = expression.getThrownExpression();
            if (thrownExpression != null) {
                value(thrownExpression, false, false);
            }
            builder.jumpToError(expression);
        }

        @Override
        public void visitArrayAccessExpression(JetArrayAccessExpression expression) {
            for (JetExpression index : expression.getIndexExpressions()) {
                value(index, false, false);
            }
            value(expression.getArrayExpression(), false, false);
            // TODO : read 'get' or 'set' function
            builder.read(expression);
        }

        @Override
        public void visitTypeofExpression(JetTypeofExpression expression) {
            value(expression.getBaseExpression(), false, false);
            builder.read(expression);
        }

        @Override
        public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
            List<JetDelegationSpecifier> delegationSpecifiers = expression.getDelegationSpecifiers();
            for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
                if (delegationSpecifier instanceof JetDelegatorByExpressionSpecifier) {
                    JetDelegatorByExpressionSpecifier specifier = (JetDelegatorByExpressionSpecifier) delegationSpecifier;
                    JetExpression delegateExpression = specifier.getDelegateExpression();
                    if (delegateExpression != null) {
                        value(delegateExpression, false, false);
                    }
                }
            }
            builder.read(expression);
        }


        @Override
        public void visitIsExpression(JetIsExpression expression) {
            value(expression.getLeftHandSide(), false, inCondition);
            // TODO : builder.read(expression.getPattern());
            builder.read(expression);
        }

        @Override
        public void visitWhenExpression(JetWhenExpression expression) {
            // TODO : no more than one else
            // TODO : else must be the last
            JetExpression subjectExpression = expression.getSubjectExpression();
            if (subjectExpression != null) {
                value(subjectExpression, false, inCondition);
            }

            Label doneLabel = builder.createUnboundLabel();

            Label nextLabel = builder.createUnboundLabel();
            for (JetWhenEntry whenEntry : expression.getEntries()) {
                if (whenEntry.getSubWhen() != null) throw new UnsupportedOperationException(); // TODO

                if (whenEntry.isElseContinue()) throw new UnsupportedOperationException(); // TODO

                JetWhenCondition condition = whenEntry.getCondition();
                if (condition != null) {
                    condition.accept(new JetVisitor() {
                        @Override
                        public void visitWhenConditionWithExpression(JetWhenConditionWithExpression condition) {
                            value(condition.getExpression(), false, inCondition); // TODO : inCondition?
                        }

                        @Override
                        public void visitWhenConditionCall(JetWhenConditionCall condition) {
                            value(condition.getCallSuffixExpression(), false, inCondition); // TODO : inCondition?
                        }

                        @Override
                        public void visitWhenConditionInRange(JetWhenConditionInRange condition) {
                            value(condition.getRangeExpression(), false, inCondition); // TODO : inCondition?
                            value(condition.getOperationReference(), false, inCondition); // TODO : inCondition?
                            // TODO : read the call to contains()...
                        }

                        @Override
                        public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
                            JetPattern pattern = condition.getPattern();
                            if (pattern != null) {
                                pattern.accept(new JetVisitor() {
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
                                        value(pattern.getExpression(), false, inCondition);
                                    }

                                    @Override
                                    public void visitTuplePattern(JetTuplePattern pattern) {
                                        // TODO
                                    }

                                    @Override
                                    public void visitDecomposerPattern(JetDecomposerPattern pattern) {
                                        value(pattern.getDecomposerExpression(), false, inCondition);
                                        pattern.getArgumentList().accept(this);
                                    }

                                    @Override
                                    public void visitJetElement(JetElement elem) {
                                        throw new UnsupportedOperationException("[JetControlFlowProcessor] " + elem.toString());
                                    }
                                });
                            }
                        }

                        @Override
                        public void visitJetElement(JetElement elem) {
                            throw new UnsupportedOperationException("[JetControlFlowProcessor] " + elem.toString());
                        }
                    });
                }

                builder.nondeterministicJump(nextLabel);

                value(whenEntry.getExpression(), true, inCondition);
                builder.jump(doneLabel);
                builder.bindLabel(nextLabel);
                nextLabel = builder.createUnboundLabel();
            }
            // TODO : if there's else, no error can happen
            builder.jumpToError(null);
            builder.bindLabel(doneLabel);
        }

        @Override
        public void visitTypeProjection(JetTypeProjection typeProjection) {
            // TODO : Support Type Arguments. Class object may be initialized at this point");
        }

        @Override
        public void visitJetElement(JetElement elem) {
            builder.unsupported(elem);
        }
    }
}