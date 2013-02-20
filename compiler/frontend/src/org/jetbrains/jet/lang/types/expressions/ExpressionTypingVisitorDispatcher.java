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

package org.jetbrains.jet.lang.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetTypeInfo;
import org.jetbrains.jet.util.lazy.ReenteringLazyValueComputationException;

import static org.jetbrains.jet.lang.diagnostics.Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM;

public class ExpressionTypingVisitorDispatcher extends JetVisitor<JetTypeInfo, ExpressionTypingContext> implements ExpressionTypingInternals {

    @Override
    public JetTypeInfo visitIdeTemplate(JetIdeTemplate expression, ExpressionTypingContext data) {
        return basic.visitIdeTemplate(expression, data);
    }

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

    private ExpressionTypingVisitorDispatcher(WritableScope writableScope) {
        this.basic = new BasicExpressionTypingVisitor(this);
        if (writableScope != null) {
            this.statements = new ExpressionTypingVisitorForStatements(this, writableScope, basic, controlStructures, patterns);
        }
        else {
            this.statements = null;
        }
    }

    @NotNull
    @Override
    public JetTypeInfo checkInExpression(JetElement callElement, @NotNull JetSimpleNameExpression operationSign, @Nullable JetExpression left, @NotNull JetExpression right, ExpressionTypingContext context) {
        return basic.checkInExpression(callElement, operationSign, left, right, context);
    }

    @Override
    @NotNull
    public final JetTypeInfo safeGetTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context) {
        JetTypeInfo typeInfo = getTypeInfo(expression, context);
        if (typeInfo.getType() != null) {
            return typeInfo;
        }
        return JetTypeInfo.create(ErrorUtils.createErrorType("Type for " + expression.getText()), context.dataFlowInfo);
    }

    @Override
    @NotNull
    public final JetTypeInfo getTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context) {
        return getTypeInfo(expression, context, this);
    }

    @NotNull
    public final JetTypeInfo getTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context, boolean isStatement) {
        if (!isStatement) return getTypeInfo(expression, context);
        if (statements != null) {
            return getTypeInfo(expression, context, statements);
        }
        return getTypeInfo(expression, context, createStatementVisitor(context));
    }
    
    private ExpressionTypingVisitorForStatements createStatementVisitor(ExpressionTypingContext context) {
        return new ExpressionTypingVisitorForStatements(this, ExpressionTypingUtils.newWritableScopeImpl(context, "statement scope"), basic, controlStructures, patterns);
    }

    @Override
    public void checkStatementType(@NotNull JetExpression expression, ExpressionTypingContext context) {
        expression.accept(createStatementVisitor(context), context);
    }

    @NotNull
    private JetTypeInfo getTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context, JetVisitor<JetTypeInfo, ExpressionTypingContext> visitor) {
        JetTypeInfo recordedTypeInfo = BindingContextUtils.getRecordedTypeInfo(expression, context.trace.getBindingContext());
        if (recordedTypeInfo != null) {
            return recordedTypeInfo;
        }
        JetTypeInfo result;
        try {
            result = expression.accept(visitor, context);
            // Some recursive definitions (object expressions) must put their types in the cache manually:
            if (context.trace.get(BindingContext.PROCESSED, expression)) {
                return JetTypeInfo.create(context.trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, expression),
                                          result.getDataFlowInfo());
            }

            if (result.getType() instanceof DeferredType) {
                result = JetTypeInfo.create(((DeferredType) result.getType()).getActualType(), result.getDataFlowInfo());
            }
            if (result.getType() != null) {
                context.trace.record(BindingContext.EXPRESSION_TYPE, expression, result.getType());
            }

        }
        catch (ReenteringLazyValueComputationException e) {
            context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(expression));
            result = JetTypeInfo.create(null, context.dataFlowInfo);
        }

        if (!context.trace.get(BindingContext.PROCESSED, expression) && !(expression instanceof JetReferenceExpression)) {
            context.trace.record(BindingContext.RESOLUTION_SCOPE, expression, context.scope);
        }
        context.trace.record(BindingContext.PROCESSED, expression);
        if (result.getDataFlowInfo() != DataFlowInfo.EMPTY) {
            context.trace.record(BindingContext.EXPRESSION_DATA_FLOW_INFO, expression, result.getDataFlowInfo());
        }
        return result;
    }  

    //////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, ExpressionTypingContext data) {
        return expression.accept(closures, data);
    }

    @Override
    public JetTypeInfo visitObjectLiteralExpression(JetObjectLiteralExpression expression, ExpressionTypingContext data) {
        return expression.accept(closures, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitThrowExpression(JetThrowExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitReturnExpression(JetReturnExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitContinueExpression(JetContinueExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitIfExpression(JetIfExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitTryExpression(JetTryExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitForExpression(JetForExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitBreakExpression(JetBreakExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitIsExpression(JetIsExpression expression, ExpressionTypingContext data) {
        return expression.accept(patterns, data);
    }

    @Override
    public JetTypeInfo visitWhenExpression(JetWhenExpression expression, ExpressionTypingContext data) {
        return expression.accept(patterns, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitJetElement(JetElement element, ExpressionTypingContext data) {
        return element.accept(basic, data);
    }
}
