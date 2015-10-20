/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.expressions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.AnnotationChecker;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.types.DeferredType;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import org.jetbrains.kotlin.util.PerformanceCounter;
import org.jetbrains.kotlin.util.ReenteringLazyValueComputationException;
import org.jetbrains.kotlin.utils.KotlinFrontEndException;

import static org.jetbrains.kotlin.diagnostics.Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM;

public abstract class ExpressionTypingVisitorDispatcher extends KtVisitor<JetTypeInfo, ExpressionTypingContext>
        implements ExpressionTypingInternals {

    public static final PerformanceCounter typeInfoPerfCounter = PerformanceCounter.Companion.create("Type info", true);

    private static final Logger LOG = Logger.getInstance(ExpressionTypingVisitor.class);

    public static class ForDeclarations extends ExpressionTypingVisitorDispatcher {
        public ForDeclarations(@NotNull ExpressionTypingComponents components, @NotNull AnnotationChecker annotationChecker) {
            super(components, annotationChecker);
        }

        @Override
        protected ExpressionTypingVisitorForStatements getStatementVisitor(@NotNull ExpressionTypingContext context) {
            return createStatementVisitor(context);
        }
    }

    protected abstract ExpressionTypingVisitorForStatements getStatementVisitor(@NotNull ExpressionTypingContext context);

    public static class ForBlock extends ExpressionTypingVisitorDispatcher {

        private final ExpressionTypingVisitorForStatements visitorForBlock;

        public ForBlock(
                @NotNull ExpressionTypingComponents components,
                @NotNull AnnotationChecker annotationChecker,
                @NotNull LexicalWritableScope writableScope
        ) {
            super(components, annotationChecker);
            this.visitorForBlock = new ExpressionTypingVisitorForStatements(
                    this, writableScope, basic, controlStructures, patterns, functions
            );
        }

        @Override
        protected ExpressionTypingVisitorForStatements getStatementVisitor(@NotNull ExpressionTypingContext context) {
            return visitorForBlock;
        }
    }

    private final ExpressionTypingComponents components;
    @NotNull private final AnnotationChecker annotationChecker;
    protected final BasicExpressionTypingVisitor basic;
    protected final FunctionsTypingVisitor functions;
    protected final ControlStructureTypingVisitor controlStructures;
    protected final PatternMatchingTypingVisitor patterns;

    private ExpressionTypingVisitorDispatcher(
            @NotNull ExpressionTypingComponents components,
            @NotNull AnnotationChecker annotationChecker
    ) {
        this.components = components;
        this.annotationChecker = annotationChecker;
        this.basic = new BasicExpressionTypingVisitor(this);
        this.controlStructures = new ControlStructureTypingVisitor(this);
        this.patterns = new PatternMatchingTypingVisitor(this);
        this.functions = new FunctionsTypingVisitor(this);
    }

    @Override
    @NotNull
    public ExpressionTypingComponents getComponents() {
        return components;
    }

    @NotNull
    @Override
    public JetTypeInfo checkInExpression(
            @NotNull KtElement callElement,
            @NotNull KtSimpleNameExpression operationSign,
            @NotNull ValueArgument leftArgument,
            @Nullable KtExpression right,
            @NotNull ExpressionTypingContext context
    ) {
        return basic.checkInExpression(callElement, operationSign, leftArgument, right, context);
    }

    @Override
    @NotNull
    public final JetTypeInfo safeGetTypeInfo(@NotNull KtExpression expression, ExpressionTypingContext context) {
        JetTypeInfo typeInfo = getTypeInfo(expression, context);
        if (typeInfo.getType() != null) {
            return typeInfo;
        }
        return typeInfo
                .replaceType(ErrorUtils.createErrorType("Type for " + expression.getText()))
                .replaceDataFlowInfo(context.dataFlowInfo);
    }

    @Override
    @NotNull
    public final JetTypeInfo getTypeInfo(@NotNull KtExpression expression, ExpressionTypingContext context) {
        JetTypeInfo result = getTypeInfo(expression, context, this);
        annotationChecker.checkExpression(expression, context.trace);
        return result;
    }

    @Override
    @NotNull
    public final JetTypeInfo getTypeInfo(@NotNull KtExpression expression, ExpressionTypingContext context, boolean isStatement) {
        if (!isStatement) return getTypeInfo(expression, context);
        return getTypeInfo(expression, context, getStatementVisitor(context));
    }
    
    protected ExpressionTypingVisitorForStatements createStatementVisitor(ExpressionTypingContext context) {
        return new ExpressionTypingVisitorForStatements(this,
                                                        ExpressionTypingUtils.newWritableScopeImpl(context, "statement scope"),
                                                        basic, controlStructures, patterns, functions);
    }

    @Override
    public void checkStatementType(@NotNull KtExpression expression, ExpressionTypingContext context) {
        expression.accept(createStatementVisitor(context), context);
    }

    @NotNull
    private static JetTypeInfo getTypeInfo(@NotNull final KtExpression expression, final ExpressionTypingContext context, final KtVisitor<JetTypeInfo, ExpressionTypingContext> visitor) {
        return typeInfoPerfCounter.time(new Function0<JetTypeInfo>() {
            @Override
            public JetTypeInfo invoke() {
                try {
                    JetTypeInfo recordedTypeInfo = BindingContextUtils.getRecordedTypeInfo(expression, context.trace.getBindingContext());
                    if (recordedTypeInfo != null) {
                        return recordedTypeInfo;
                    }
                    JetTypeInfo result;
                    try {
                        result = expression.accept(visitor, context);
                        // Some recursive definitions (object expressions) must put their types in the cache manually:
                        //noinspection ConstantConditions
                        if (context.trace.get(BindingContext.PROCESSED, expression)) {
                            KotlinType type = context.trace.getBindingContext().getType(expression);
                            return result.replaceType(type);
                        }

                        if (result.getType() instanceof DeferredType) {
                            result = result.replaceType(((DeferredType) result.getType()).getDelegate());
                        }
                        context.trace.record(BindingContext.EXPRESSION_TYPE_INFO, expression, result);
                    }
                    catch (ReenteringLazyValueComputationException e) {
                        context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(expression));
                        result = TypeInfoFactoryKt.noTypeInfo(context);
                    }

                    context.trace.record(BindingContext.PROCESSED, expression);

                    // todo save scope before analyze and fix debugger: see CodeFragmentAnalyzer.correctContextForExpression
                    BindingContextUtilsKt.recordScope(context.trace, context.scope, expression);
                    BindingContextUtilsKt.recordDataFlowInfo(context.replaceDataFlowInfo(result.getDataFlowInfo()), expression);
                    return result;
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (KotlinFrontEndException e) {
                    throw e;
                }
                catch (Throwable e) {
                    context.trace.report(Errors.EXCEPTION_FROM_ANALYZER.on(expression, e));
                    logOrThrowException(expression, e);
                    return TypeInfoFactoryKt.createTypeInfo(
                            ErrorUtils.createErrorType(e.getClass().getSimpleName() + " from analyzer"),
                            context
                    );
                }
            }
        });
    }

    private static void logOrThrowException(@NotNull KtExpression expression, Throwable e) {
        try {
            // This trows AssertionError in CLI and reports the error in the IDE
            LOG.error(
                    "Exception while analyzing expression at " + DiagnosticUtils.atLocation(expression) + ":\n" + expression.getText() + "\n",
                    e
            );
        }
        catch (AssertionError errorFromLogger) {
            // If we ended up here, we are in CLI, and the initial exception needs to be rethrown,
            // simply throwing AssertionError causes its being wrapped over and over again
            throw new KotlinFrontEndException(errorFromLogger.getMessage(), e);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitFunctionLiteralExpression(@NotNull KtFunctionLiteralExpression expression, ExpressionTypingContext data) {
        return functions.visitFunctionLiteralExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitNamedFunction(@NotNull KtNamedFunction function, ExpressionTypingContext data) {
        return functions.visitNamedFunction(function, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitThrowExpression(@NotNull KtThrowExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitThrowExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitReturnExpression(@NotNull KtReturnExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitReturnExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitContinueExpression(@NotNull KtContinueExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitContinueExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitIfExpression(@NotNull KtIfExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitIfExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitTryExpression(@NotNull KtTryExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitTryExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitForExpression(@NotNull KtForExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitForExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitWhileExpression(@NotNull KtWhileExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitWhileExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(@NotNull KtDoWhileExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitDoWhileExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitBreakExpression(@NotNull KtBreakExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitBreakExpression(expression, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitIsExpression(@NotNull KtIsExpression expression, ExpressionTypingContext data) {
        return patterns.visitIsExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitWhenExpression(@NotNull KtWhenExpression expression, ExpressionTypingContext data) {
        return patterns.visitWhenExpression(expression, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression, ExpressionTypingContext data) {
        return basic.visitSimpleNameExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression, ExpressionTypingContext data) {
        return basic.visitParenthesizedExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitConstantExpression(@NotNull KtConstantExpression expression, ExpressionTypingContext data) {
        return basic.visitConstantExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitBinaryWithTypeRHSExpression(@NotNull KtBinaryExpressionWithTypeRHS expression, ExpressionTypingContext data) {
        return basic.visitBinaryWithTypeRHSExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitThisExpression(@NotNull KtThisExpression expression, ExpressionTypingContext data) {
        return basic.visitThisExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitSuperExpression(@NotNull KtSuperExpression expression, ExpressionTypingContext data) {
        return basic.visitSuperExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitBlockExpression(@NotNull KtBlockExpression expression, ExpressionTypingContext data) {
        return basic.visitBlockExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitClassLiteralExpression(@NotNull KtClassLiteralExpression expression, ExpressionTypingContext data) {
        return basic.visitClassLiteralExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitCallableReferenceExpression(@NotNull KtCallableReferenceExpression expression, ExpressionTypingContext data) {
        return basic.visitCallableReferenceExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expression, ExpressionTypingContext data) {
        return basic.visitObjectLiteralExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitQualifiedExpression(@NotNull KtQualifiedExpression expression, ExpressionTypingContext data) {
        return basic.visitQualifiedExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitCallExpression(@NotNull KtCallExpression expression, ExpressionTypingContext data) {
        return basic.visitCallExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitUnaryExpression(@NotNull KtUnaryExpression expression, ExpressionTypingContext data) {
        return basic.visitUnaryExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitLabeledExpression(@NotNull KtLabeledExpression expression, ExpressionTypingContext data) {
        return basic.visitLabeledExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitBinaryExpression(@NotNull KtBinaryExpression expression, ExpressionTypingContext data) {
        return basic.visitBinaryExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression, ExpressionTypingContext data) {
        return basic.visitArrayAccessExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitDeclaration(@NotNull KtDeclaration dcl, ExpressionTypingContext data) {
        return basic.visitDeclaration(dcl, data);
    }

    @Override
    public JetTypeInfo visitRootPackageExpression(@NotNull KtRootPackageExpression expression, ExpressionTypingContext data) {
        return basic.visitRootPackageExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitStringTemplateExpression(@NotNull KtStringTemplateExpression expression, ExpressionTypingContext data) {
        return basic.visitStringTemplateExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitAnnotatedExpression(@NotNull KtAnnotatedExpression expression, ExpressionTypingContext data) {
        return basic.visitAnnotatedExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitJetElement(@NotNull KtElement element, ExpressionTypingContext data) {
        return element.accept(basic, data);
    }
}
