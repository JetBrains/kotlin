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
import org.jetbrains.jet.util.ReenteringLazyValueComputationException;

import static org.jetbrains.jet.lang.diagnostics.Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM;

public class ExpressionTypingVisitorDispatcher extends JetVisitor<JetTypeInfo, ExpressionTypingContext> implements ExpressionTypingInternals {

    @NotNull
    public static ExpressionTypingFacade create(@NotNull ExpressionTypingComponents components) {
        return new ExpressionTypingVisitorDispatcher(components, null);
    }

    @NotNull
    public static ExpressionTypingInternals createForBlock(
            @NotNull ExpressionTypingComponents components,
            @NotNull WritableScope writableScope
    ) {
        return new ExpressionTypingVisitorDispatcher(components, writableScope);
    }

    private final ExpressionTypingComponents components;
    private final BasicExpressionTypingVisitor basic;
    private final ExpressionTypingVisitorForStatements statements;
    private final ClosureExpressionsTypingVisitor closures;
    private final ControlStructureTypingVisitor controlStructures;
    private final PatternMatchingTypingVisitor patterns;

    private ExpressionTypingVisitorDispatcher(@NotNull ExpressionTypingComponents components, WritableScope writableScope) {
        this.components = components;
        this.basic = new BasicExpressionTypingVisitor(this);
        controlStructures = new ControlStructureTypingVisitor(this);
        patterns = new PatternMatchingTypingVisitor(this);
        if (writableScope != null) {
            this.statements = new ExpressionTypingVisitorForStatements(this, writableScope, basic, controlStructures, patterns);
        }
        else {
            this.statements = null;
        }
        this.closures = new ClosureExpressionsTypingVisitor(this);
    }

    @Override
    @NotNull
    public ExpressionTypingComponents getComponents() {
        return components;
    }

    @NotNull
    @Override
    public JetTypeInfo checkInExpression(
            @NotNull JetElement callElement,
            @NotNull JetSimpleNameExpression operationSign,
            @NotNull ValueArgument leftArgument,
            @Nullable JetExpression right,
            @NotNull ExpressionTypingContext context
    ) {
        return basic.checkInExpression(callElement, operationSign, leftArgument, right, context);
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

    @Override
    @NotNull
    public final JetTypeInfo getTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context, boolean isStatement) {
        if (!isStatement) return getTypeInfo(expression, context);
        if (statements != null) {
            return getTypeInfo(expression, context, statements);
        }
        return getTypeInfo(expression, context, createStatementVisitor(context));
    }
    
    private ExpressionTypingVisitorForStatements createStatementVisitor(ExpressionTypingContext context) {
        return new ExpressionTypingVisitorForStatements(this,
                                                        ExpressionTypingUtils.newWritableScopeImpl(context, "statement scope"),
                                                        basic, controlStructures, patterns);
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
                result = JetTypeInfo.create(((DeferredType) result.getType()).getDelegate(), result.getDataFlowInfo());
            }
            if (result.getType() != null) {
                context.trace.record(BindingContext.EXPRESSION_TYPE, expression, result.getType());
            }

        }
        catch (ReenteringLazyValueComputationException e) {
            context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(expression));
            result = JetTypeInfo.create(null, context.dataFlowInfo);
        }

        if (!context.trace.get(BindingContext.PROCESSED, expression) && !BindingContextUtils.isExpressionWithValidReference(expression, context.trace.getBindingContext())) {
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
    public JetTypeInfo visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression, ExpressionTypingContext data) {
        return expression.accept(closures, data);
    }

    @Override
    public JetTypeInfo visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression, ExpressionTypingContext data) {
        return expression.accept(closures, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitThrowExpression(@NotNull JetThrowExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitReturnExpression(@NotNull JetReturnExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitContinueExpression(@NotNull JetContinueExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitIfExpression(@NotNull JetIfExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitTryExpression(@NotNull JetTryExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitForExpression(@NotNull JetForExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitWhileExpression(@NotNull JetWhileExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(@NotNull JetDoWhileExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

    @Override
    public JetTypeInfo visitBreakExpression(@NotNull JetBreakExpression expression, ExpressionTypingContext data) {
        return expression.accept(controlStructures, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitIsExpression(@NotNull JetIsExpression expression, ExpressionTypingContext data) {
        return expression.accept(patterns, data);
    }

    @Override
    public JetTypeInfo visitWhenExpression(@NotNull JetWhenExpression expression, ExpressionTypingContext data) {
        return expression.accept(patterns, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitJetElement(@NotNull JetElement element, ExpressionTypingContext data) {
        return element.accept(basic, data);
    }
}
