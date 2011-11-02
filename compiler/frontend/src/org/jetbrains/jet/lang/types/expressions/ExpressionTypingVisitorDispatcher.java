package org.jetbrains.jet.lang.types.expressions;

import com.intellij.lang.ASTNode;
import com.intellij.util.Function;
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

    private static Function<ExpressionTypingInternals, BasicExpressionTypingVisitor> BASIC_FACTORY = new Function<ExpressionTypingInternals, BasicExpressionTypingVisitor>() {
        @Override
        public BasicExpressionTypingVisitor fun(ExpressionTypingInternals facade) {
            return  new BasicExpressionTypingVisitor(facade);
        }
    };

    @NotNull
    public static ExpressionTypingFacade create() {
        return new ExpressionTypingVisitorDispatcher(BASIC_FACTORY);
    }

    @NotNull
    public static ExpressionTypingInternals createForBlock(final WritableScope writableScope) {
        return new ExpressionTypingVisitorDispatcher(new Function<ExpressionTypingInternals, BasicExpressionTypingVisitor>() {
                @Override
                public BasicExpressionTypingVisitor fun(ExpressionTypingInternals facade) {
                    return  new ExpressionTypingVisitorForStatements(facade, writableScope);
                }
            });
    }

    private final BasicExpressionTypingVisitor basic;
    private final ClosureExpressionsTypingVisitor closures = new ClosureExpressionsTypingVisitor(this);
    private final ControlStructureTypingVisitor controlStructures = new ControlStructureTypingVisitor(this);
    private final PatternMatchingTypingVisitor patterns = new PatternMatchingTypingVisitor(this);
    protected DataFlowInfo resultDataFlowInfo;

    private ExpressionTypingVisitorDispatcher(Function<ExpressionTypingInternals, BasicExpressionTypingVisitor> factoryForBasic) {
        this.basic = factoryForBasic.fun(this);
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
    public void checkInExpression(JetElement callElement, @NotNull JetSimpleNameExpression operationSign, @NotNull JetExpression left, @NotNull JetExpression right, ExpressionTypingContext context) {
        basic.checkInExpression(callElement, operationSign, left, right, context);
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
        if (context.trace.get(BindingContext.PROCESSED, expression)) {
            return context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression);
        }
        JetType result;
        try {
            result = expression.accept(this, context);
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
