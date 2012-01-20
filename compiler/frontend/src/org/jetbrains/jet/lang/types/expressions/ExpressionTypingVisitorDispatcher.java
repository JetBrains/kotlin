package org.jetbrains.jet.lang.types.expressions;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.util.lazy.ReenteringLazyValueComputationException;

import static org.jetbrains.jet.lang.diagnostics.Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM;

/**
 * @author abreslav
 */
public class ExpressionTypingVisitorDispatcher extends JetVisitor<JetType, ExpressionTypingContext> implements ExpressionTypingInternals {

    @NotNull
    public static ExpressionTypingFacade create() {
        return new ExpressionTypingVisitorDispatcher(null);
    }

    @NotNull
    public static ExpressionTypingInternals createForBlock(final WritableScope writableScope) {
        return new ExpressionTypingVisitorDispatcher(writableScope);
    }

    private final BasicExpressionTypingVisitor basic;
    private final ExpressionTypingVisitorForStatements statements;
    private final ClosureExpressionsTypingVisitor closures = new ClosureExpressionsTypingVisitor(this);
    private final ControlStructureTypingVisitor controlStructures = new ControlStructureTypingVisitor(this);
    private final PatternMatchingTypingVisitor patterns = new PatternMatchingTypingVisitor(this);
    protected DataFlowInfo resultDataFlowInfo;

    private ExpressionTypingVisitorDispatcher(WritableScope writableScope) {
        this.basic = new BasicExpressionTypingVisitor(this);
        if (writableScope != null) {
            this.statements = new ExpressionTypingVisitorForStatements(this, writableScope, basic, controlStructures, patterns);
        }
        else {
            this.statements = null;
        }
    }

    @Override
    @Nullable
    public DataFlowInfo getResultingDataFlowInfo() {
        return resultDataFlowInfo;
    }

    @Override
    public JetType getSelectorReturnType(@NotNull ReceiverDescriptor receiver, @Nullable ASTNode callOperationNode, @NotNull JetExpression selectorExpression, @NotNull ExpressionTypingContext context) {
        return basic.getSelectorReturnType(receiver, callOperationNode, selectorExpression, context);
    }

    @Override
    public boolean checkInExpression(JetElement callElement, @NotNull JetSimpleNameExpression operationSign, @Nullable JetExpression left, @NotNull JetExpression right, ExpressionTypingContext context) {
        return basic.checkInExpression(callElement, operationSign, left, right, context);
    }

    @Override
    public void setResultingDataFlowInfo(@NotNull DataFlowInfo dataFlowInfo) {
        this.resultDataFlowInfo = dataFlowInfo;
    }

    @Override
    @NotNull
    public final JetType safeGetType(@NotNull JetExpression expression, ExpressionTypingContext context) {
        JetType type = getType(expression, context);
        if (type != null) {
            return type;
        }
        return ErrorUtils.createErrorType("Type for " + expression.getText());
    }

    @Override
    @Nullable
    public final JetType getType(@NotNull JetExpression expression, ExpressionTypingContext context) {
        return getType(expression, context, this);
    }

    @Nullable
    public final JetType getType(@NotNull JetExpression expression, ExpressionTypingContext context, boolean isStatement) {
        if (!isStatement) return getType(expression, context);
        if (statements != null) {
            return getType(expression, context, statements);
        }
        return getType(expression, context, createStatementVisitor(context));
    }
    
    private ExpressionTypingVisitorForStatements createStatementVisitor(ExpressionTypingContext context) {
        return new ExpressionTypingVisitorForStatements(this, ExpressionTypingUtils.newWritableScopeImpl(context).setDebugName("statement scope"), basic, controlStructures, patterns);
    }

    @Override
    public void checkStatementType(@NotNull JetExpression expression, ExpressionTypingContext context) {
        expression.accept(createStatementVisitor(context), context);
    }

    @Nullable
    private JetType getType(@NotNull JetExpression expression, ExpressionTypingContext context, JetVisitor<JetType, ExpressionTypingContext> visitor) {
        if (context.trace.get(BindingContext.PROCESSED, expression)) {
            return context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
        }
        JetType result;
        try {
            result = expression.accept(visitor, context);
            // Some recursive definitions (object expressions) must put their types in the cache manually:
            if (context.trace.get(BindingContext.PROCESSED, expression)) {
                return context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
            }

            if (result instanceof DeferredType) {
                result = ((DeferredType) result).getActualType();
            }
            if (result != null) {
                context.trace.record(BindingContext.EXPRESSION_TYPE, expression, result);
            }

        }
        catch (ReenteringLazyValueComputationException e) {
            context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(expression));
            result = null;
        }

        if (!context.trace.get(BindingContext.PROCESSED, expression)) {
            context.trace.record(BindingContext.RESOLUTION_SCOPE, expression, context.scope);
        }
        context.trace.record(BindingContext.PROCESSED, expression);
        return result;        
    }  

    //////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetType visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, ExpressionTypingContext data) {
        return expression.accept(closures, data);
    }

    @Override
    public JetType visitObjectLiteralExpression(JetObjectLiteralExpression expression, ExpressionTypingContext data) {
        return expression.accept(closures, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetType visitThrowExpression(JetThrowExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetType visitReturnExpression(JetReturnExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetType visitContinueExpression(JetContinueExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetType visitIfExpression(JetIfExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetType visitTryExpression(JetTryExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetType visitForExpression(JetForExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetType visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetType visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetType visitBreakExpression(JetBreakExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetType visitIsExpression(JetIsExpression expression, ExpressionTypingContext data) {
        return expression.accept(patterns, data);
    }

    @Override
    public JetType visitWhenExpression(JetWhenExpression expression, ExpressionTypingContext data) {
        return expression.accept(patterns, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetType visitJetElement(JetElement element, ExpressionTypingContext data) {
        return element.accept(basic, data);
    }
}
