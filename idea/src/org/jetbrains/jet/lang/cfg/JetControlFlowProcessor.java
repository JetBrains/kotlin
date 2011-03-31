package org.jetbrains.jet.lang.cfg;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
* @author abreslav
*/
public class JetControlFlowProcessor {

    private JetControlFlowBuilder builder;

    public JetControlFlowProcessor(JetControlFlowBuilder builder) {
        this.builder = builder;
    }

    public void generate(@NotNull JetElement subroutineElement, @NotNull JetExpression body) {
        generateSubroutineControlFlow(subroutineElement, Collections.singletonList(body), false);
    }

    public void generateSubroutineControlFlow(@NotNull JetElement subroutineElement, @NotNull List<? extends JetElement> body, boolean preferBlocks) {
        builder.enterSubroutine(subroutineElement);
        for (JetElement statement : body) {
            statement.accept(new CFPVisitor(preferBlocks));
        }
        builder.exitSubroutine(subroutineElement);
    }

    private void registerLabeledElement(@NotNull JetSimpleNameExpression label, @NotNull JetExpression labeledExpression) {
        throw new UnsupportedOperationException(); // TODO
    }

    private JetElement resolveLabel(JetSimpleNameExpression labelElement) {
        throw new UnsupportedOperationException(); // TODO
    }

    private class CFPVisitor extends JetVisitor {
        private final boolean preferBlock;

        private CFPVisitor(boolean preferBlock) {
            this.preferBlock = preferBlock;
        }

        private void value(@NotNull JetElement element, boolean preferBlock) {
            CFPVisitor visitor;
            if (this.preferBlock == preferBlock) {
                visitor = this;
            }
            else {
                visitor = new CFPVisitor(preferBlock);
            }
            element.accept(visitor);
        }

        @Override
        public void visitConstantExpression(JetConstantExpression expression) {
            builder.readNode(expression);
        }

        @Override
        public void visitSimpleNameExpression(JetSimpleNameExpression expression) {
            builder.readNode(expression);
        }

        @Override
        public void visitLabelQualifiedExpression(JetLabelQualifiedExpression expression) {
            registerLabeledElement(expression.getTargetLabel(), expression.getLabeledExpression());
            value(expression.getLabeledExpression(), false);
        }

        @Override
        public void visitFunction(JetFunction function) {
            generate(function, function.getBodyExpression());
        }

        @Override
        public void visitBinaryExpression(JetBinaryExpression expression) {
            IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
            if (operationType == JetTokens.ANDAND) {
                value(expression.getLeft(), false);
                Label resultLabel = builder.createUnboundLabel();
                builder.jumpOnFalse(resultLabel);
                value(expression.getRight(), false);
                builder.bindLabel(resultLabel);
            }
            else if (operationType == JetTokens.OROR) {
                value(expression.getLeft(), false);
                Label resultLabel = builder.createUnboundLabel();
                builder.jumpOnTrue(resultLabel);
                value(expression.getRight(), false);
                builder.bindLabel(resultLabel);
            }
            else {
                value(expression.getLeft(), false);
                value(expression.getRight(), false);
                builder.readNode(expression);
            }
        }

        @Override
        public void visitIfExpression(JetIfExpression expression) {
            value(expression.getCondition(), false);
            Label elseLabel = builder.createUnboundLabel();
            builder.jumpOnFalse(elseLabel);
            value(expression.getThen(), true);
            Label resultLabel = builder.createUnboundLabel();
            builder.jump(resultLabel);
            builder.bindLabel(elseLabel);
            JetExpression elseBranch = expression.getElse();
            if (elseBranch != null) {
                value(elseBranch, true);
            }
            builder.bindLabel(resultLabel);
    //            builder.readNode(expression);
        }

        @Override
        public void visitTryExpression(JetTryExpression expression) {
            JetFinallySection finallyBlock = expression.getFinallyBlock();
            if (finallyBlock != null) {
                builder.pushFinally(finallyBlock.getFinalExpression());
            }

            List<JetCatchClause> catchClauses = expression.getCatchClauses();
            if (catchClauses.isEmpty()) {
                value(expression.getTryBlock(), true);
            }
            else {
                Label catchBlock = builder.createUnboundLabel();
                builder.nondeterministicJump(catchBlock);

                value(expression.getTryBlock(), true);

                Label afterCatches = builder.createUnboundLabel();
                builder.jump(afterCatches);

                builder.bindLabel(catchBlock);
                for (Iterator<JetCatchClause> iterator = catchClauses.iterator(); iterator.hasNext(); ) {
                    JetCatchClause catchClause = iterator.next();
                    value(catchClause.getCatchBody(), true);
                    if (iterator.hasNext()) {
                        builder.nondeterministicJump(afterCatches);
                    }
                }

                builder.bindLabel(afterCatches);
            }

            if (finallyBlock != null) {
                builder.popFinally();
            }
        }

        @Override
        public void visitWhileExpression(JetWhileExpression expression) {
            Label loopExitPoint = builder.createUnboundLabel();
            Label loopEntryPoint = builder.enterLoop(expression, loopExitPoint);
            value(expression.getCondition(), false);
            builder.jumpOnFalse(loopExitPoint);
            value(expression.getBody(), true);
            builder.jump(loopEntryPoint);
            builder.exitLoop(expression);
        }

        @Override
        public void visitDoWhileExpression(JetDoWhileExpression expression) {
            Label loopExitPoint = builder.createUnboundLabel();
            Label loopEntryPoint = builder.enterLoop(expression, loopExitPoint);
            value(expression.getBody(), true);
            value(expression.getCondition(), false);
            builder.jumpOnTrue(loopEntryPoint);
            builder.exitLoop(expression);
        }

        @Override
        public void visitForExpression(JetForExpression expression) {
            value(expression.getLoopRange(), false);
            Label loopExitPoint = builder.createUnboundLabel();
            Label loopEntryPoint = builder.enterLoop(expression, loopExitPoint);
            value(expression.getBody(), true);
            builder.nondeterministicJump(loopEntryPoint);
            builder.exitLoop(expression);
        }

        @Override
        public void visitBreakExpression(JetBreakExpression expression) {
            JetSimpleNameExpression labelElement = expression.getTargetLabel();
            Label exitPoint = (labelElement != null)
                    ? builder.getExitPoint(labelElement)
                    : builder.getExitPoint(builder.getCurrentLoop());
            builder.jump(exitPoint);
        }

        @Override
        public void visitContinueExpression(JetContinueExpression expression) {
            JetSimpleNameExpression labelElement = expression.getTargetLabel();
            if (labelElement != null) {
                builder.jump(builder.getEntryPoint(labelElement));
            }
            else {
                builder.jump(builder.getEntryPoint(builder.getCurrentLoop()));
            }
        }

        @Override
        public void visitReturnExpression(JetReturnExpression expression) {
            JetExpression returnedExpression = expression.getReturnedExpression();
            if (returnedExpression != null) {
                value(returnedExpression, false);
            }
            JetSimpleNameExpression labelElement = expression.getTargetLabel();
            JetElement subroutine = (labelElement != null)
                    ? resolveLabel(labelElement)
                    : builder.getCurrentSubroutine();
            if (returnedExpression == null) {
                builder.returnNoValue(subroutine);
            }
            else {
                builder.returnValue(subroutine);
            }
        }

        @Override
        public void visitBlockExpression(JetBlockExpression expression) {
            for (JetElement statement : expression.getStatements()) {
                value(statement, true);
            }
        }

        @Override
        public void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression) {
            if (preferBlock && !expression.hasParameterSpecification()) {
                for (JetElement statement : expression.getBody()) {
                    value(statement, true);
                }
            }
            else {
                generateSubroutineControlFlow(expression, expression.getBody(), true);
            }
        }

        @Override
        public void visitJetElement(JetElement elem) {
            builder.unsupported(elem);
        }
    }

}