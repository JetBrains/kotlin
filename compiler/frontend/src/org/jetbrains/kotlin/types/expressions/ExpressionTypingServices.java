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

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalyzerPackage;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ScriptDescriptor;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryPackage;

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
    public JetType safeGetType(
            @NotNull JetScope scope,
            @NotNull JetExpression expression,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        JetType type = getType(scope, expression, expectedType, dataFlowInfo, trace);
        return AnalyzerPackage.safeType(type, expression);
    }

    @NotNull
    public JetTypeInfo getTypeInfo(
            @NotNull JetScope scope,
            @NotNull JetExpression expression,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                trace, scope, dataFlowInfo, expectedType
        );
        return expressionTypingFacade.getTypeInfo(expression, context);
    }

    @NotNull
    public JetTypeInfo getTypeInfo(@NotNull JetExpression expression, @NotNull ResolutionContext resolutionContext) {
        return expressionTypingFacade.getTypeInfo(expression, ExpressionTypingContext.newContext(resolutionContext));
    }

    @Nullable
    public JetType getType(
            @NotNull JetScope scope,
            @NotNull JetExpression expression,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        return getTypeInfo(scope, expression, expectedType, dataFlowInfo, trace).getType();
    }

    /////////////////////////////////////////////////////////

    public void checkFunctionReturnType(
            @NotNull JetScope functionInnerScope,
            @NotNull JetDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull DataFlowInfo dataFlowInfo,
            @Nullable JetType expectedReturnType,
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

    /*package*/ void checkFunctionReturnType(JetDeclarationWithBody function, ExpressionTypingContext context) {
        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) return;

        boolean blockBody = function.hasBlockBody();
        ExpressionTypingContext newContext =
                blockBody
                ? context.replaceExpectedType(NO_EXPECTED_TYPE)
                : context;

        expressionTypingFacade.getTypeInfo(bodyExpression, newContext, blockBody);
    }

    @NotNull
    public JetTypeInfo getBlockReturnedType(JetBlockExpression expression, ExpressionTypingContext context, boolean isStatement) {
        return getBlockReturnedType(expression, isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION, context);
    }

    @NotNull
    public JetTypeInfo getBlockReturnedType(
            @NotNull JetBlockExpression expression,
            @NotNull CoercionStrategy coercionStrategyForLastExpression,
            @NotNull ExpressionTypingContext context
    ) {
        List<JetExpression> block = ResolvePackage.filterStatements(statementFilter, expression);

        // SCRIPT: get code descriptor for script declaration
        DeclarationDescriptor containingDescriptor = context.scope.getContainingDeclaration();
        if (containingDescriptor instanceof ScriptDescriptor) {
            if (!(expression.getParent() instanceof JetScript)) {
                // top level script declarations should have ScriptDescriptor parent
                // and lower level script declarations should be ScriptCodeDescriptor parent
                containingDescriptor = ((ScriptDescriptor) containingDescriptor).getScriptCodeDescriptor();
            }
        }
        WritableScope scope = new WritableScopeImpl(
                context.scope, containingDescriptor, new TraceBasedRedeclarationHandler(context.trace), "getBlockReturnedType");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);

        JetTypeInfo r;
        if (block.isEmpty()) {
            r = expressionTypingComponents.dataFlowAnalyzer
                    .createCheckedTypeInfo(expressionTypingComponents.builtIns.getUnitType(), context, expression);
        }
        else {
            r = getBlockReturnedTypeWithWritableScope(scope, block, coercionStrategyForLastExpression,
                                                      context.replaceStatementFilter(statementFilter));
        }
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        if (containingDescriptor instanceof ScriptDescriptor) {
            context.trace.record(BindingContext.SCRIPT_SCOPE, (ScriptDescriptor) containingDescriptor, scope);
        }

        return r;
    }

    @NotNull
    public JetType getBodyExpressionType(
            @NotNull BindingTrace trace,
            @NotNull JetScope outerScope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor
    ) {
        JetExpression bodyExpression = function.getBodyExpression();
        assert bodyExpression != null;
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(outerScope, functionDescriptor, trace);

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                trace, functionInnerScope, dataFlowInfo, NO_EXPECTED_TYPE
        );
        JetTypeInfo typeInfo = expressionTypingFacade.getTypeInfo(bodyExpression, context, function.hasBlockBody());

        JetType type = typeInfo.getType();
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
    /*package*/ JetTypeInfo getBlockReturnedTypeWithWritableScope(
            @NotNull WritableScope scope,
            @NotNull List<? extends JetElement> block,
            @NotNull CoercionStrategy coercionStrategyForLastExpression,
            @NotNull ExpressionTypingContext context
    ) {
        if (block.isEmpty()) {
            return TypeInfoFactoryPackage.createTypeInfo(expressionTypingComponents.builtIns.getUnitType(), context);
        }

        ExpressionTypingInternals blockLevelVisitor = new ExpressionTypingVisitorDispatcher.ForBlock(
                expressionTypingComponents, annotationChecker, scope);
        ExpressionTypingContext newContext = context.replaceScope(scope).replaceExpectedType(NO_EXPECTED_TYPE);

        JetTypeInfo result = TypeInfoFactoryPackage.noTypeInfo(context);
        // Jump point data flow info
        DataFlowInfo beforeJumpInfo = newContext.dataFlowInfo;
        boolean jumpOutPossible = false;
        for (Iterator<? extends JetElement> iterator = block.iterator(); iterator.hasNext(); ) {
            JetElement statement = iterator.next();
            if (!(statement instanceof JetExpression)) {
                continue;
            }
            JetExpression statementExpression = (JetExpression) statement;
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

    private JetTypeInfo getTypeOfLastExpressionInBlock(
            @NotNull JetExpression statementExpression,
            @NotNull ExpressionTypingContext context,
            @NotNull CoercionStrategy coercionStrategyForLastExpression,
            @NotNull ExpressionTypingInternals blockLevelVisitor
    ) {
        if (context.expectedType != NO_EXPECTED_TYPE) {
            JetType expectedType;
            if (context.expectedType == UNIT_EXPECTED_TYPE ||//the first check is necessary to avoid invocation 'isUnit(UNIT_EXPECTED_TYPE)'
                (coercionStrategyForLastExpression == COERCION_TO_UNIT && KotlinBuiltIns.isUnit(context.expectedType))) {
                expectedType = UNIT_EXPECTED_TYPE;
            }
            else {
                expectedType = context.expectedType;
            }

            return blockLevelVisitor.getTypeInfo(statementExpression, context.replaceExpectedType(expectedType), true);
        }
        JetTypeInfo result = blockLevelVisitor.getTypeInfo(statementExpression, context, true);
        if (coercionStrategyForLastExpression == COERCION_TO_UNIT) {
            boolean mightBeUnit = false;
            if (statementExpression instanceof JetDeclaration) {
                mightBeUnit = true;
            }
            if (statementExpression instanceof JetBinaryExpression) {
                JetBinaryExpression binaryExpression = (JetBinaryExpression) statementExpression;
                IElementType operationType = binaryExpression.getOperationToken();
                //noinspection SuspiciousMethodCalls
                if (operationType == JetTokens.EQ || OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
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
