/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.incremental.components.LookupTracker;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.codeFragmentUtil.CodeFragmentUtilKt;
import org.jetbrains.kotlin.resolve.AnnotationChecker;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.DeclarationsCheckerBuilder;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.resolve.calls.context.CallPosition;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.types.DeferredType;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import org.jetbrains.kotlin.util.LookupTrackerUtilKt;
import org.jetbrains.kotlin.util.PerformanceCounter;
import org.jetbrains.kotlin.util.ReenteringLazyValueComputationException;
import org.jetbrains.kotlin.utils.KotlinFrontEndException;

import static org.jetbrains.kotlin.diagnostics.Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM;

public abstract class ExpressionTypingVisitorDispatcher extends KtVisitor<KotlinTypeInfo, ExpressionTypingContext>
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
    protected final DeclarationsCheckerBuilder declarationsCheckerBuilder;

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
        this.declarationsCheckerBuilder = components.declarationsCheckerBuilder;
    }

    @Override
    @NotNull
    public ExpressionTypingComponents getComponents() {
        return components;
    }

    @NotNull
    @Override
    public KotlinTypeInfo checkInExpression(
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
    public final KotlinTypeInfo safeGetTypeInfo(@NotNull KtExpression expression, ExpressionTypingContext context) {
        KotlinTypeInfo typeInfo = getTypeInfo(expression, context);
        if (typeInfo.getType() != null) {
            return typeInfo;
        }
        return typeInfo
                .replaceType(ErrorUtils.createErrorType("Type for " + expression.getText()))
                .replaceDataFlowInfo(context.dataFlowInfo);
    }

    @Override
    @NotNull
    public final KotlinTypeInfo getTypeInfo(@NotNull KtExpression expression, ExpressionTypingContext context) {
        KotlinTypeInfo result = getTypeInfo(expression, context, this);
        annotationChecker.checkExpression(expression, context.trace);
        return result;
    }

    @Override
    @NotNull
    public final KotlinTypeInfo getTypeInfo(@NotNull KtExpression expression, ExpressionTypingContext context, boolean isStatement) {
        ExpressionTypingContext newContext = context;
        if (CodeFragmentUtilKt.suppressDiagnosticsInDebugMode(expression)) {
            newContext = ExpressionTypingContext.newContext(context, true);
        }
        if (!isStatement) return getTypeInfo(expression, newContext);
        return getTypeInfo(expression, newContext, getStatementVisitor(newContext));
    }
    
    protected ExpressionTypingVisitorForStatements createStatementVisitor(ExpressionTypingContext context) {
        return new ExpressionTypingVisitorForStatements(this,
                                                        ExpressionTypingUtils.newWritableScopeImpl(context, LexicalScopeKind.CODE_BLOCK, components.overloadChecker),
                                                        basic, controlStructures, patterns, functions);
    }

    @Override
    public void checkStatementType(@NotNull KtExpression expression, ExpressionTypingContext context) {
        expression.accept(createStatementVisitor(context), context);
    }

    @NotNull
    private KotlinTypeInfo getTypeInfo(@NotNull final KtExpression expression, final ExpressionTypingContext context, final KtVisitor<KotlinTypeInfo, ExpressionTypingContext> visitor) {
        return typeInfoPerfCounter.time(new Function0<KotlinTypeInfo>() {
            @Override
            public KotlinTypeInfo invoke() {
                try {
                    KotlinTypeInfo recordedTypeInfo = BindingContextUtils.getRecordedTypeInfo(expression, context.trace.getBindingContext());
                    if (recordedTypeInfo != null) {
                        return recordedTypeInfo;
                    }
                    KotlinTypeInfo result;
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
                    try {
                        // Here we have to resolve some types, so the following exception is possible
                        // Example: val a = ::a, fun foo() = ::foo
                        recordTypeInfo(expression, result);
                    }
                    catch (ReenteringLazyValueComputationException e) {
                        context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(expression));
                        return TypeInfoFactoryKt.noTypeInfo(context);
                    }
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

    private void recordTypeInfo(@NotNull KtExpression expression, @NotNull KotlinTypeInfo typeInfo) {
        LookupTracker lookupTracker = getComponents().lookupTracker;
        KotlinType resultType = typeInfo.getType();

        if (resultType != null) {
            LookupTrackerUtilKt.record(lookupTracker, expression, resultType);
        }
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
    public KotlinTypeInfo visitLambdaExpression(@NotNull KtLambdaExpression expression, ExpressionTypingContext data) {
        // Erasing call position to unknown is necessary to prevent wrong call positions when type checking lambda's body
        return functions.visitLambdaExpression(expression, data.replaceCallPosition(CallPosition.Unknown.INSTANCE));
    }

    @Override
    public KotlinTypeInfo visitNamedFunction(@NotNull KtNamedFunction function, ExpressionTypingContext data) {
        return functions.visitNamedFunction(function, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public KotlinTypeInfo visitThrowExpression(@NotNull KtThrowExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitThrowExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitReturnExpression(@NotNull KtReturnExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitReturnExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitContinueExpression(@NotNull KtContinueExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitContinueExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitIfExpression(@NotNull KtIfExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitIfExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitTryExpression(@NotNull KtTryExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitTryExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitForExpression(@NotNull KtForExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitForExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitWhileExpression(@NotNull KtWhileExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitWhileExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitDoWhileExpression(@NotNull KtDoWhileExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitDoWhileExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitBreakExpression(@NotNull KtBreakExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitBreakExpression(expression, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public KotlinTypeInfo visitIsExpression(@NotNull KtIsExpression expression, ExpressionTypingContext data) {
        return patterns.visitIsExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitWhenExpression(@NotNull KtWhenExpression expression, ExpressionTypingContext data) {
        return patterns.visitWhenExpression(expression, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public KotlinTypeInfo visitSimpleNameExpression(@NotNull KtSimpleNameExpression expression, ExpressionTypingContext data) {
        return basic.visitSimpleNameExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitParenthesizedExpression(@NotNull KtParenthesizedExpression expression, ExpressionTypingContext data) {
        return basic.visitParenthesizedExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitConstantExpression(@NotNull KtConstantExpression expression, ExpressionTypingContext data) {
        return basic.visitConstantExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitBinaryWithTypeRHSExpression(@NotNull KtBinaryExpressionWithTypeRHS expression, ExpressionTypingContext data) {
        return basic.visitBinaryWithTypeRHSExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitThisExpression(@NotNull KtThisExpression expression, ExpressionTypingContext data) {
        return basic.visitThisExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitSuperExpression(@NotNull KtSuperExpression expression, ExpressionTypingContext data) {
        return basic.visitSuperExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitBlockExpression(@NotNull KtBlockExpression expression, ExpressionTypingContext data) {
        return basic.visitBlockExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitClassLiteralExpression(@NotNull KtClassLiteralExpression expression, ExpressionTypingContext data) {
        return basic.visitClassLiteralExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitCallableReferenceExpression(@NotNull KtCallableReferenceExpression expression, ExpressionTypingContext data) {
        return basic.visitCallableReferenceExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitObjectLiteralExpression(@NotNull KtObjectLiteralExpression expression, ExpressionTypingContext data) {
        return basic.visitObjectLiteralExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitQualifiedExpression(@NotNull KtQualifiedExpression expression, ExpressionTypingContext data) {
        return basic.visitQualifiedExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitCallExpression(@NotNull KtCallExpression expression, ExpressionTypingContext data) {
        return basic.visitCallExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitUnaryExpression(@NotNull KtUnaryExpression expression, ExpressionTypingContext data) {
        return basic.visitUnaryExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitLabeledExpression(@NotNull KtLabeledExpression expression, ExpressionTypingContext data) {
        return basic.visitLabeledExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitBinaryExpression(@NotNull KtBinaryExpression expression, ExpressionTypingContext data) {
        return basic.visitBinaryExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitArrayAccessExpression(@NotNull KtArrayAccessExpression expression, ExpressionTypingContext data) {
        return basic.visitArrayAccessExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitDeclaration(@NotNull KtDeclaration dcl, ExpressionTypingContext data) {
        return basic.visitDeclaration(dcl, data);
    }

    @Override
    public KotlinTypeInfo visitClass(@NotNull KtClass klass, ExpressionTypingContext data) {
        return basic.visitClass(klass, data);
    }

    @Override
    public KotlinTypeInfo visitProperty(@NotNull KtProperty property, ExpressionTypingContext data) {
        return basic.visitProperty(property, data);
    }

    @Override
    public KotlinTypeInfo visitStringTemplateExpression(@NotNull KtStringTemplateExpression expression, ExpressionTypingContext data) {
        return basic.visitStringTemplateExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitAnnotatedExpression(@NotNull KtAnnotatedExpression expression, ExpressionTypingContext data) {
        return basic.visitAnnotatedExpression(expression, data);
    }

    @Override
    public KotlinTypeInfo visitKtElement(@NotNull KtElement element, ExpressionTypingContext data) {
        return element.accept(basic, data);
    }
}
