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

import com.intellij.psi.tree.IElementType;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ScriptDescriptor;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.resolve.scopes.TraceBasedLocalRedeclarationChecker;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import java.util.Iterator;
import java.util.List;

import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.kotlin.types.TypeUtils.UNIT_EXPECTED_TYPE;
import static org.jetbrains.kotlin.types.expressions.CoercionStrategy.COERCION_TO_UNIT;

public class ExpressionTypingServices {

    private final ExpressionTypingFacade expressionTypingFacade;
    private final ExpressionTypingComponents expressionTypingComponents;

    @NotNull private final AnnotationChecker annotationChecker;
    @NotNull private final StatementFilter statementFilter;

    public ExpressionTypingServices(
            @NotNull ExpressionTypingComponents components,
            @NotNull AnnotationChecker annotationChecker,
            @NotNull StatementFilter statementFilter,
            @NotNull ExpressionTypingVisitorDispatcher.ForDeclarations facade
    ) {
        this.expressionTypingComponents = components;
        this.annotationChecker = annotationChecker;
        this.statementFilter = statementFilter;
        this.expressionTypingFacade = facade;
    }

    @NotNull public StatementFilter getStatementFilter() {
        return statementFilter;
    }

    @NotNull
    public KotlinType safeGetType(
            @NotNull LexicalScope scope,
            @NotNull KtExpression expression,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        KotlinType type = getType(scope, expression, expectedType, dataFlowInfo, trace);

        return type != null ? type : ErrorUtils.createErrorType("Type for " + expression.getText());
    }

    @NotNull
    public KotlinTypeInfo getTypeInfo(
            @NotNull LexicalScope scope,
            @NotNull KtExpression expression,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace,
            boolean isStatement
    ) {
        return getTypeInfo(scope, expression, expectedType, dataFlowInfo, trace, isStatement, expression, ContextDependency.INDEPENDENT);
    }

    @NotNull
    public KotlinTypeInfo getTypeInfo(
            @NotNull LexicalScope scope,
            @NotNull final KtExpression expression,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace,
            boolean isStatement,
            @NotNull final KtExpression contextExpression,
            @NotNull ContextDependency contextDependency
    ) {
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                trace, scope, dataFlowInfo, expectedType, contextDependency
        );
        if (contextExpression != expression) {
            context = context.replaceExpressionContextProvider(new Function1<KtExpression, KtExpression>() {
                @Override
                public KtExpression invoke(KtExpression arg) {
                    return arg == expression ? contextExpression : null;
                }
            });
        }
        return expressionTypingFacade.getTypeInfo(expression, context, isStatement);
    }

    @NotNull
    public KotlinTypeInfo getTypeInfo(@NotNull KtExpression expression, @NotNull ResolutionContext resolutionContext) {
        return expressionTypingFacade.getTypeInfo(expression, ExpressionTypingContext.newContext(resolutionContext));
    }

    @Nullable
    public KotlinType getType(
            @NotNull LexicalScope scope,
            @NotNull KtExpression expression,
            @NotNull KotlinType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        return getTypeInfo(scope, expression, expectedType, dataFlowInfo, trace, false).getType();
    }

    /////////////////////////////////////////////////////////

    public void checkFunctionReturnType(
            @NotNull LexicalScope functionInnerScope,
            @NotNull KtDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull DataFlowInfo dataFlowInfo,
            @Nullable KotlinType expectedReturnType,
            BindingTrace trace
    ) {
        if (expectedReturnType == null) {
            expectedReturnType = functionDescriptor.getReturnType();
            if (!function.hasBlockBody() && !function.hasDeclaredReturnType()) {
                expectedReturnType = NO_EXPECTED_TYPE;
            }
        }
        checkFunctionReturnType(function, ExpressionTypingContext.newContext(
                trace,
                functionInnerScope, dataFlowInfo, expectedReturnType != null ? expectedReturnType : NO_EXPECTED_TYPE
        ));
    }

    /*package*/ void checkFunctionReturnType(KtDeclarationWithBody function, ExpressionTypingContext context) {
        KtExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) return;

        boolean blockBody = function.hasBlockBody();
        ExpressionTypingContext newContext =
                blockBody
                ? context.replaceExpectedType(NO_EXPECTED_TYPE)
                : context;

        expressionTypingFacade.getTypeInfo(bodyExpression, newContext, blockBody);
    }

    @NotNull
    public KotlinTypeInfo getBlockReturnedType(KtBlockExpression expression, ExpressionTypingContext context, boolean isStatement) {
        return getBlockReturnedType(expression, isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION, context);
    }

    @NotNull
    public KotlinTypeInfo getBlockReturnedType(
            @NotNull KtBlockExpression expression,
            @NotNull CoercionStrategy coercionStrategyForLastExpression,
            @NotNull ExpressionTypingContext context
    ) {
        List<KtExpression> block = StatementFilterKt.filterStatements(statementFilter, expression);

        DeclarationDescriptor containingDescriptor = context.scope.getOwnerDescriptor();
        TraceBasedLocalRedeclarationChecker redeclarationChecker
                = new TraceBasedLocalRedeclarationChecker(context.trace, expressionTypingComponents.overloadChecker);
        LexicalWritableScope scope = new LexicalWritableScope(context.scope, containingDescriptor, false, null, redeclarationChecker,
                                                              LexicalScopeKind.CODE_BLOCK);

        KotlinTypeInfo r;
        if (block.isEmpty()) {
            r = expressionTypingComponents.dataFlowAnalyzer
                    .createCheckedTypeInfo(expressionTypingComponents.builtIns.getUnitType(), context, expression);
        }
        else {
            r = getBlockReturnedTypeWithWritableScope(scope, block, coercionStrategyForLastExpression,
                                                      context.replaceStatementFilter(statementFilter));
        }
        scope.freeze();

        if (containingDescriptor instanceof ScriptDescriptor) {
            context.trace.record(BindingContext.SCRIPT_SCOPE, (ScriptDescriptor) containingDescriptor, scope);
        }

        return r;
    }

    @NotNull
    public KotlinType getBodyExpressionType(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope outerScope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull KtDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor
    ) {
        KtExpression bodyExpression = function.getBodyExpression();
        assert bodyExpression != null;
        LexicalScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(outerScope, functionDescriptor, trace,
                                                                                       expressionTypingComponents.overloadChecker);

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                trace, functionInnerScope, dataFlowInfo, NO_EXPECTED_TYPE
        );
        KotlinTypeInfo typeInfo = expressionTypingFacade.getTypeInfo(bodyExpression, context, function.hasBlockBody());

        KotlinType type = typeInfo.getType();
        if (type != null) {
            return type;
        }
        else {
            return ErrorUtils.createErrorType("Error function type");
        }
    }

    /**
     * Visits block statements propagating data flow information from the first to the last.
     * Determines block returned type and data flow information at the end of the block AND
     * at the nearest jump point from the block beginning.
     */
    /*package*/ KotlinTypeInfo getBlockReturnedTypeWithWritableScope(
            @NotNull LexicalWritableScope scope,
            @NotNull List<? extends KtElement> block,
            @NotNull CoercionStrategy coercionStrategyForLastExpression,
            @NotNull ExpressionTypingContext context
    ) {
        if (block.isEmpty()) {
            return TypeInfoFactoryKt.createTypeInfo(expressionTypingComponents.builtIns.getUnitType(), context);
        }

        ExpressionTypingInternals blockLevelVisitor = new ExpressionTypingVisitorDispatcher.ForBlock(
                expressionTypingComponents, annotationChecker, scope);
        ExpressionTypingContext newContext = context.replaceScope(scope).replaceExpectedType(NO_EXPECTED_TYPE);

        KotlinTypeInfo result = TypeInfoFactoryKt.noTypeInfo(context);
        // Jump point data flow info
        DataFlowInfo beforeJumpInfo = newContext.dataFlowInfo;
        boolean jumpOutPossible = false;
        for (Iterator<? extends KtElement> iterator = block.iterator(); iterator.hasNext(); ) {
            KtElement statement = iterator.next();
            if (!(statement instanceof KtExpression)) {
                continue;
            }
            KtExpression statementExpression = (KtExpression) statement;
            if (!iterator.hasNext()) {
                result = getTypeOfLastExpressionInBlock(
                        statementExpression, newContext.replaceExpectedType(context.expectedType), coercionStrategyForLastExpression,
                        blockLevelVisitor);
            }
            else {
                result = blockLevelVisitor
                        .getTypeInfo(statementExpression, newContext.replaceContextDependency(ContextDependency.INDEPENDENT), true);
            }

            DataFlowInfo newDataFlowInfo = result.getDataFlowInfo();
            // If jump is not possible, we take new data flow info before jump
            if (!jumpOutPossible) {
                beforeJumpInfo = result.getJumpFlowInfo();
                jumpOutPossible = result.getJumpOutPossible();
            }
            if (newDataFlowInfo != context.dataFlowInfo) {
                newContext = newContext.replaceDataFlowInfo(newDataFlowInfo);
                // We take current data flow info if jump there is not possible
            }
            blockLevelVisitor = new ExpressionTypingVisitorDispatcher.ForBlock(expressionTypingComponents, annotationChecker, scope);
        }
        return result.replaceJumpOutPossible(jumpOutPossible).replaceJumpFlowInfo(beforeJumpInfo);
    }

    private KotlinTypeInfo getTypeOfLastExpressionInBlock(
            @NotNull KtExpression statementExpression,
            @NotNull ExpressionTypingContext context,
            @NotNull CoercionStrategy coercionStrategyForLastExpression,
            @NotNull ExpressionTypingInternals blockLevelVisitor
    ) {
        if (context.expectedType != NO_EXPECTED_TYPE) {
            KotlinType expectedType;
            if (context.expectedType == UNIT_EXPECTED_TYPE ||//the first check is necessary to avoid invocation 'isUnit(UNIT_EXPECTED_TYPE)'
                (coercionStrategyForLastExpression == COERCION_TO_UNIT && KotlinBuiltIns.isUnit(context.expectedType))) {
                expectedType = UNIT_EXPECTED_TYPE;
            }
            else {
                expectedType = context.expectedType;
            }

            return blockLevelVisitor.getTypeInfo(statementExpression, context.replaceExpectedType(expectedType), true);
        }
        KotlinTypeInfo result = blockLevelVisitor.getTypeInfo(statementExpression, context, true);
        if (coercionStrategyForLastExpression == COERCION_TO_UNIT) {
            boolean mightBeUnit = false;
            if (statementExpression instanceof KtDeclaration) {
                if (!(statementExpression instanceof KtNamedFunction) || statementExpression.getName() != null) {
                    mightBeUnit = true;
                }
            }
            if (statementExpression instanceof KtBinaryExpression) {
                KtBinaryExpression binaryExpression = (KtBinaryExpression) statementExpression;
                IElementType operationType = binaryExpression.getOperationToken();
                //noinspection SuspiciousMethodCalls
                if (operationType == KtTokens.EQ || OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
                    mightBeUnit = true;
                }
            }
            if (mightBeUnit) {
                // ExpressionTypingVisitorForStatements should return only null or Unit for declarations and assignments,
                // but (for correct assignment / initialization analysis) data flow info must be preserved
                assert result.getType() == null || KotlinBuiltIns.isUnit(result.getType());
                result = result.replaceType(expressionTypingComponents.builtIns.getUnitType());
            }
        }
        return result;
    }

}
