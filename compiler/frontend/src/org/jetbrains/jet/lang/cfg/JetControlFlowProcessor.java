package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Lists;
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

/**
* @author abreslav
* @author svtk
*/
public class JetControlFlowProcessor {

    private final JetControlFlowBuilder builder;
    private final BindingTrace trace;

    public JetControlFlowProcessor(BindingTrace trace, JetControlFlowBuilder builder) {
        this.builder = builder;
        this.trace = trace;
    }

    public void generate(@NotNull JetDeclaration subroutine) {
        builder.enterSubroutine(subroutine);
        if (subroutine instanceof JetDeclarationWithBody) {
            JetDeclarationWithBody declarationWithBody = (JetDeclarationWithBody) subroutine;
            CFPVisitor cfpVisitor = new CFPVisitor(false);
            List<JetParameter> valueParameters = declarationWithBody.getValueParameters();
            for (JetParameter valueParameter : valueParameters) {
                valueParameter.accept(cfpVisitor);
            }
            JetExpression bodyExpression = declarationWithBody.getBodyExpression();
            if (bodyExpression != null) {
                bodyExpression.accept(cfpVisitor);
            }
        }
        else {
            subroutine.accept(new CFPVisitor(false));
        }
        builder.exitSubroutine(subroutine);
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
                value(condition.getRangeExpression(), CFPVisitor.this.inCondition); // TODO : inCondition?
                value(condition.getOperationReference(), CFPVisitor.this.inCondition); // TODO : inCondition?
                // TODO : read the call to contains()...
            }

            @Override
            public void visitWhenConditionIsPattern(JetWhenConditionIsPattern condition) {
                JetPattern pattern = condition.getPattern();
                if (pattern != null) {
                    pattern.accept(patternVisitor);
                }
            }

            @Override
            public void visitWhenConditionExpression(JetWhenConditionWithExpression condition) {
                JetExpressionPattern pattern = condition.getPattern();
                if (pattern != null) {
                    pattern.accept(patternVisitor);
                }
            }

            @Override
            public void visitJetElement(JetElement element) {
                throw new UnsupportedOperationException("[JetControlFlowProcessor] " + element.toString());
            }
        };
        private final JetVisitorVoid patternVisitor = new JetVisitorVoid() {
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
                List<JetTuplePatternEntry> entries = pattern.getEntries();
                for (JetTuplePatternEntry entry : entries) {
                    JetPattern entryPattern = entry.getPattern();
                    if (entryPattern != null) {
                        entryPattern.accept(this);
                    }
                }
            }

            @Override
            public void visitDecomposerPattern(JetDecomposerPattern pattern) {
                value(pattern.getDecomposerExpression(), inCondition);
                pattern.getArgumentList().accept(this);
            }

            @Override
            public void visitBindingPattern(JetBindingPattern pattern) {
                JetProperty variableDeclaration = pattern.getVariableDeclaration();
                builder.write(pattern, variableDeclaration);
                JetWhenCondition condition = pattern.getCondition();
                if (condition != null) {
                    condition.accept(conditionVisitor);
                }
            }

            @Override
            public void visitJetElement(JetElement element) {
                throw new UnsupportedOperationException("[JetControlFlowProcessor] " + element.toString());
            }
        };

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
        }

        @Override
        public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
            builder.read(expression);

            JetExpression innerExpression = expression.getExpression();
            if (innerExpression != null) {
                value(innerExpression, inCondition);
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
            JetSimpleNameExpression operationSign = expression.getOperationReference();
            IElementType operationType = operationSign.getReferencedNameElementType();
            JetExpression baseExpression = expression.getBaseExpression();
            if (baseExpression == null) return;
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
            builder.read(expression);
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

            List<JetCatchClause> catchClauses = expression.getCatchClauses();
            final boolean hasCatches = !catchClauses.isEmpty();
            Label onException = null;
            if (hasCatches) {
                onException = builder.createUnboundLabel();
                builder.nondeterministicJump(onException);
            }
            value(expression.getTryBlock(), inCondition);

            if (hasCatches) {
                builder.allowDead();
                Label afterCatches = builder.createUnboundLabel();
                builder.jump(afterCatches);

                builder.bindLabel(onException);
                LinkedList<Label> catchLabels = Lists.newLinkedList();
                int catchClausesSize = catchClauses.size();
                for (int i = 0; i < catchClausesSize - 1; i++) {
                    catchLabels.add(builder.createUnboundLabel());
                }
                builder.nondeterministicJump(catchLabels);
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
                        value(catchBody, false);
                    }
                    builder.allowDead();
                    builder.jump(afterCatches);
                }

                builder.bindLabel(afterCatches);
            }
            else {
                builder.allowDead();
            }

            if (finallyBlock != null) {
                builder.exitTryFinally();
                value(finallyBlock.getFinalExpression(), inCondition);
            }
            builder.stopAllowDead();
        }

        @Override
        public void visitWhileExpression(JetWhileExpression expression) {
            builder.read(expression);
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
            builder.read(expression);
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
            builder.read(expression);
            JetExpression loopRange = expression.getLoopRange();
            if (loopRange != null) {
                value(loopRange, false);
            }
            JetParameter loopParameter = expression.getLoopParameter();
            if (loopParameter != null) {
                builder.declare(loopParameter);
                builder.write(loopParameter, loopParameter);
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
                value(returnedExpression, false);
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
            //todo cache JetFunctionLiteral instead
            if (subroutine instanceof JetFunctionLiteralExpression) {
                subroutine = ((JetFunctionLiteralExpression) subroutine).getFunctionLiteral();
            }
            boolean error = false;
            if (builder.getCurrentSubroutine() != subroutine) {
                trace.report(RETURN_NOT_ALLOWED.on(expression));
                error = true;
            }
            if (subroutine instanceof JetFunction || subroutine instanceof JetPropertyAccessor || subroutine instanceof JetSecondaryConstructor) {
                if (returnedExpression == null) {
                    builder.returnNoValue(expression, subroutine);
                }
                else {
                    builder.returnValue(expression, subroutine);
                }
            }
            else if (!error) {
                if (labelElement != null) {
                    trace.report(NOT_A_RETURN_LABEL.on(expression, labelName));
                }
                else {
                    trace.report(RETURN_NOT_ALLOWED.on(expression));
                }
            }
        }

        @Override
        public void visitParameter(JetParameter parameter) {
            JetExpression defaultValue = parameter.getDefaultValue();
            builder.declare(parameter);
            if (defaultValue != null) {
                value(defaultValue, inCondition);
            }
            builder.write(parameter, parameter);
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
            builder.declare(property);
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
        public void visitIsExpression(final JetIsExpression expression) {
            value(expression.getLeftHandSide(), inCondition);
            JetPattern pattern = expression.getPattern();
            if (pattern != null) {
                pattern.accept(patternVisitor);
            }
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
            boolean hasElseOrIrrefutableBranch = false;

            Label doneLabel = builder.createUnboundLabel();

            Label nextLabel = null;
            for (Iterator<JetWhenEntry> iterator = expression.getEntries().iterator(); iterator.hasNext(); ) {
                JetWhenEntry whenEntry = iterator.next();

                builder.read(whenEntry);

                if (whenEntry.isElse()) {
                    hasElseOrIrrefutableBranch = true;
                    if (iterator.hasNext()) {
                        trace.report(ELSE_MISPLACED_IN_WHEN.on(whenEntry));
                    }
                }
                boolean isIrrefutable = JetPsiUtil.isIrrefutable(whenEntry);
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
                value(whenEntry.getExpression(), inCondition);
                builder.allowDead();
                builder.jump(doneLabel);

                if (!isIrrefutable) {
                    builder.bindLabel(nextLabel);
                }
            }
            builder.bindLabel(doneLabel);
            if (!hasElseOrIrrefutableBranch) {
                trace.report(NO_ELSE_IN_WHEN.on(expression));
            }
            builder.stopAllowDead();
        }

        @Override
        public void visitObjectLiteralExpression(JetObjectLiteralExpression expression) {
            JetObjectDeclaration declaration = expression.getObjectDeclaration();
            value(declaration, inCondition);

            List<JetDeclaration> declarations = declaration.getDeclarations();
            List<JetDeclaration> functions = Lists.newArrayList();
            for (JetDeclaration localDeclaration : declarations) {
                if (!(localDeclaration instanceof JetProperty) && !(localDeclaration instanceof JetClassInitializer)) {
                    functions.add(localDeclaration);
                }
            }
            for (JetDeclaration function : functions) {
                value(function, inCondition);
            }
            builder.read(expression);
        }

        @Override
        public void visitObjectDeclaration(JetObjectDeclaration objectDeclaration) {
//            for (JetDelegationSpecifier delegationSpecifier : objectDeclaration.getDelegationSpecifiers()) {
//                value(delegationSpecifier, inCondition);
//            }
            visitClassOrObject(objectDeclaration);
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
        public void visitAnonymousInitializer(JetClassInitializer classInitializer) {
            value(classInitializer.getBody(), inCondition);
        }
        
        private void visitClassOrObject(JetClassOrObject classOrObject) {
            for (JetDelegationSpecifier specifier : classOrObject.getDelegationSpecifiers()) {
                value(specifier, inCondition);
            }
            List<JetDeclaration> declarations = classOrObject.getDeclarations();
            List<JetProperty> properties = Lists.newArrayList();
            for (JetDeclaration declaration : declarations) {
                if (declaration instanceof JetProperty) {
                    value(declaration, inCondition);
                    properties.add((JetProperty) declaration);
                }
                else if (declaration instanceof JetClassInitializer) {
                    value(declaration, inCondition);
                }
            }
        }

        @Override
        public void visitClass(JetClass klass) {
            List<JetParameter> parameters = klass.getPrimaryConstructorParameters();
            for (JetParameter parameter : parameters) {
                value(parameter, inCondition);
            }
            visitClassOrObject(klass);
        }

        @Override
        public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
            List<? extends ValueArgument> valueArguments = call.getValueArguments();
            for (ValueArgument valueArgument : valueArguments) {
                value(valueArgument.getArgumentExpression(), inCondition);
            }
        }

        @Override
        public void visitJetElement(JetElement element) {
            builder.unsupported(element);
        }
    }
}
