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
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.types.DeferredType;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryPackage;
import org.jetbrains.kotlin.util.PerformanceCounter;
import org.jetbrains.kotlin.util.ReenteringLazyValueComputationException;
import org.jetbrains.kotlin.utils.KotlinFrontEndException;

import static org.jetbrains.kotlin.diagnostics.Errors.TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM;
import static org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage.recordScopeAndDataFlowInfo;

public abstract class ExpressionTypingVisitorDispatcher extends JetVisitor<JetTypeInfo, ExpressionTypingContext> implements ExpressionTypingInternals {

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
                @NotNull WritableScope writableScope
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
        return typeInfo
                .replaceType(ErrorUtils.createErrorType("Type for " + expression.getText()))
                .replaceDataFlowInfo(context.dataFlowInfo);
    }

    @Override
    @NotNull
    public final JetTypeInfo getTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context) {
        JetTypeInfo result = getTypeInfo(expression, context, this);
        annotationChecker.checkExpression(expression, context.trace);
        return result;
    }

    @Override
    @NotNull
    public final JetTypeInfo getTypeInfo(@NotNull JetExpression expression, ExpressionTypingContext context, boolean isStatement) {
        if (!isStatement) return getTypeInfo(expression, context);
        return getTypeInfo(expression, context, getStatementVisitor(context));
    }
    
    protected ExpressionTypingVisitorForStatements createStatementVisitor(ExpressionTypingContext context) {
        return new ExpressionTypingVisitorForStatements(this,
                                                        ExpressionTypingUtils.newWritableScopeImpl(context, "statement scope"),
                                                        basic, controlStructures, patterns, functions);
    }

    @Override
    public void checkStatementType(@NotNull JetExpression expression, ExpressionTypingContext context) {
        expression.accept(createStatementVisitor(context), context);
    }

    @NotNull
    private static JetTypeInfo getTypeInfo(@NotNull final JetExpression expression, final ExpressionTypingContext context, final JetVisitor<JetTypeInfo, ExpressionTypingContext> visitor) {
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
                            JetType type = context.trace.getBindingContext().getType(expression);
                            return result.replaceType(type);
                        }

                        if (result.getType() instanceof DeferredType) {
                            result = result.replaceType(((DeferredType) result.getType()).getDelegate());
                        }
                        context.trace.record(BindingContext.EXPRESSION_TYPE_INFO, expression, result);
                    }
                    catch (ReenteringLazyValueComputationException e) {
                        context.trace.report(TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM.on(expression));
                        result = TypeInfoFactoryPackage.noTypeInfo(context);
                    }

                    context.trace.record(BindingContext.PROCESSED, expression);
                    recordScopeAndDataFlowInfo(context.replaceDataFlowInfo(result.getDataFlowInfo()), expression);
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
                    return TypeInfoFactoryPackage.createTypeInfo(
                            ErrorUtils.createErrorType(e.getClass().getSimpleName() + " from analyzer"),
                            context
                    );
                }
            }
        });
    }

    private static void logOrThrowException(@NotNull JetExpression expression, Throwable e) {
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
    public JetTypeInfo visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression, ExpressionTypingContext data) {
        return functions.visitFunctionLiteralExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitNamedFunction(@NotNull JetNamedFunction function, ExpressionTypingContext data) {
        return functions.visitNamedFunction(function, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitThrowExpression(@NotNull JetThrowExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitThrowExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitReturnExpression(@NotNull JetReturnExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitReturnExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitContinueExpression(@NotNull JetContinueExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitContinueExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitIfExpression(@NotNull JetIfExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitIfExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitTryExpression(@NotNull JetTryExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitTryExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitForExpression(@NotNull JetForExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitForExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitWhileExpression(@NotNull JetWhileExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitWhileExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(@NotNull JetDoWhileExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitDoWhileExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitBreakExpression(@NotNull JetBreakExpression expression, ExpressionTypingContext data) {
        return controlStructures.visitBreakExpression(expression, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitIsExpression(@NotNull JetIsExpression expression, ExpressionTypingContext data) {
        return patterns.visitIsExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitWhenExpression(@NotNull JetWhenExpression expression, ExpressionTypingContext data) {
        return patterns.visitWhenExpression(expression, data);
    }

//////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public JetTypeInfo visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, ExpressionTypingContext data) {
        return basic.visitSimpleNameExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, ExpressionTypingContext data) {
        return basic.visitParenthesizedExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitConstantExpression(@NotNull JetConstantExpression expression, ExpressionTypingContext data) {
        return basic.visitConstantExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitBinaryWithTypeRHSExpression(@NotNull JetBinaryExpressionWithTypeRHS expression, ExpressionTypingContext data) {
        return basic.visitBinaryWithTypeRHSExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitThisExpression(@NotNull JetThisExpression expression, ExpressionTypingContext data) {
        return basic.visitThisExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitSuperExpression(@NotNull JetSuperExpression expression, ExpressionTypingContext data) {
        return basic.visitSuperExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitBlockExpression(@NotNull JetBlockExpression expression, ExpressionTypingContext data) {
        return basic.visitBlockExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitClassLiteralExpression(@NotNull JetClassLiteralExpression expression, ExpressionTypingContext data) {
        return basic.visitClassLiteralExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitCallableReferenceExpression(@NotNull JetCallableReferenceExpression expression, ExpressionTypingContext data) {
        return basic.visitCallableReferenceExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitObjectLiteralExpression(@NotNull JetObjectLiteralExpression expression, ExpressionTypingContext data) {
        return basic.visitObjectLiteralExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitQualifiedExpression(@NotNull JetQualifiedExpression expression, ExpressionTypingContext data) {
        return basic.visitQualifiedExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitCallExpression(@NotNull JetCallExpression expression, ExpressionTypingContext data) {
        return basic.visitCallExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitUnaryExpression(@NotNull JetUnaryExpression expression, ExpressionTypingContext data) {
        return basic.visitUnaryExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitLabeledExpression(@NotNull JetLabeledExpression expression, ExpressionTypingContext data) {
        return basic.visitLabeledExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitBinaryExpression(@NotNull JetBinaryExpression expression, ExpressionTypingContext data) {
        return basic.visitBinaryExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitArrayAccessExpression(@NotNull JetArrayAccessExpression expression, ExpressionTypingContext data) {
        return basic.visitArrayAccessExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitDeclaration(@NotNull JetDeclaration dcl, ExpressionTypingContext data) {
        return basic.visitDeclaration(dcl, data);
    }

    @Override
    public JetTypeInfo visitRootPackageExpression(@NotNull JetRootPackageExpression expression, ExpressionTypingContext data) {
        return basic.visitRootPackageExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression, ExpressionTypingContext data) {
        return basic.visitStringTemplateExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitAnnotatedExpression(@NotNull JetAnnotatedExpression expression, ExpressionTypingContext data) {
        return basic.visitAnnotatedExpression(expression, data);
    }

    @Override
    public JetTypeInfo visitJetElement(@NotNull JetElement element, ExpressionTypingContext data) {
        return element.accept(basic, data);
    }
}
