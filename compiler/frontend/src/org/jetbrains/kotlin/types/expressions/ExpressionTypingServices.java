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

import com.google.common.base.Function;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalyzerPackage;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ScriptDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.CallExpressionResolver;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker;
import org.jetbrains.kotlin.resolve.calls.checkers.CompositeChecker;
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency;
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext;
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.JetTypeInfo;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.List;

import static org.jetbrains.kotlin.types.TypeUtils.*;
import static org.jetbrains.kotlin.types.expressions.CoercionStrategy.COERCION_TO_UNIT;

public class ExpressionTypingServices {

    @NotNull
    private final ExpressionTypingFacade expressionTypingFacade;
    @NotNull
    private final ExpressionTypingComponents expressionTypingComponents;

    private Project project;
    private CallResolver callResolver;
    private CallExpressionResolver callExpressionResolver;
    private DescriptorResolver descriptorResolver;
    private TypeResolver typeResolver;
    private AnnotationResolver annotationResolver;
    private StatementFilter statementFilter;
    private KotlinBuiltIns builtIns;

    @NotNull
    public Project getProject() {
        return project;
    }

    @Inject
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public CallResolver getCallResolver() {
        return callResolver;
    }

    @Inject
    public void setCallResolver(@NotNull CallResolver callResolver) {
        this.callResolver = callResolver;
    }

    @NotNull
    public CallExpressionResolver getCallExpressionResolver() {
        return callExpressionResolver;
    }

    @Inject
    public void setCallExpressionResolver(@NotNull CallExpressionResolver callExpressionResolver) {
        this.callExpressionResolver = callExpressionResolver;
    }

    @NotNull
    public DescriptorResolver getDescriptorResolver() {
        return descriptorResolver;
    }

    @Inject
    public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @NotNull
    public TypeResolver getTypeResolver() {
        return typeResolver;
    }

    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @NotNull
    public AnnotationResolver getAnnotationResolver() {
        return annotationResolver;
    }

    @Inject
    public void setAnnotationResolver(@NotNull AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @NotNull
    private StatementFilter getStatementFilter() {
        return statementFilter;
    }

    @Inject
    public void setStatementFilter(@NotNull StatementFilter statementFilter) {
        this.statementFilter = statementFilter;
    }

    @Inject
    public void setBuiltIns(@NotNull KotlinBuiltIns builtIns) {
        this.builtIns = builtIns;
    }

    public ExpressionTypingServices(@NotNull ExpressionTypingComponents components) {
        this.expressionTypingComponents = components;
        this.expressionTypingFacade = ExpressionTypingVisitorDispatcher.create(components);
    }

    @NotNull
    public JetType safeGetType(@NotNull JetScope scope, @NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace) {
        JetType type = getType(scope, expression, expectedType, dataFlowInfo, trace);
        return AnalyzerPackage.safeType(type, expression);
    }

    @NotNull
    public JetTypeInfo getTypeInfo(@NotNull JetScope scope, @NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace) {
        ExpressionTypingContext context = ExpressionTypingContext.newContext(this, trace, scope, dataFlowInfo, expectedType);
        return expressionTypingFacade.getTypeInfo(expression, context);
    }

    @NotNull
    public JetTypeInfo getTypeInfo(@NotNull JetExpression expression, @NotNull ResolutionContext resolutionContext) {
        return expressionTypingFacade.getTypeInfo(expression, ExpressionTypingContext.newContext(resolutionContext));
    }

    @Nullable
    public JetType getType(@NotNull JetScope scope, @NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace) {
        return getTypeInfo(scope, expression, expectedType, dataFlowInfo, trace).getType();
    }

    /////////////////////////////////////////////////////////

    public void checkFunctionReturnType(@NotNull JetScope functionInnerScope, @NotNull JetDeclarationWithBody function, @NotNull FunctionDescriptor functionDescriptor, @NotNull DataFlowInfo dataFlowInfo, @Nullable JetType expectedReturnType, BindingTrace trace) {
        if (expectedReturnType == null) {
            expectedReturnType = functionDescriptor.getReturnType();
            if (!function.hasBlockBody() && !function.hasDeclaredReturnType()) {
                expectedReturnType = NO_EXPECTED_TYPE;
            }
        }
        checkFunctionReturnType(function, ExpressionTypingContext.newContext(
                this, trace, functionInnerScope, dataFlowInfo, expectedReturnType != null ? expectedReturnType : NO_EXPECTED_TYPE
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
        List<JetElement> block = ResolvePackage.filterStatements(getStatementFilter(), expression);

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
            r = DataFlowUtils.checkType(builtIns.getUnitType(), expression, context, context.dataFlowInfo);
        }
        else {
            r = getBlockReturnedTypeWithWritableScope(scope, block, coercionStrategyForLastExpression,
                                                      context.replacestatementFilter(getStatementFilter()));
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
                this, trace, functionInnerScope, dataFlowInfo, NO_EXPECTED_TYPE
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

    /*package*/ JetTypeInfo getBlockReturnedTypeWithWritableScope(
            @NotNull WritableScope scope,
            @NotNull List<? extends JetElement> block,
            @NotNull CoercionStrategy coercionStrategyForLastExpression,
            @NotNull ExpressionTypingContext context
    ) {
        if (block.isEmpty()) {
            return JetTypeInfo.create(builtIns.getUnitType(), context.dataFlowInfo);
        }

        ExpressionTypingInternals blockLevelVisitor = ExpressionTypingVisitorDispatcher.createForBlock(expressionTypingComponents, scope);
        ExpressionTypingContext newContext = context.replaceScope(scope).replaceExpectedType(NO_EXPECTED_TYPE);

        JetTypeInfo result = JetTypeInfo.create(null, context.dataFlowInfo);
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
                result = blockLevelVisitor.getTypeInfo(statementExpression, newContext.replaceContextDependency(ContextDependency.INDEPENDENT), true);
            }

            DataFlowInfo newDataFlowInfo = result.getDataFlowInfo();
            if (newDataFlowInfo != context.dataFlowInfo) {
                newContext = newContext.replaceDataFlowInfo(newDataFlowInfo);
            }
            blockLevelVisitor = ExpressionTypingVisitorDispatcher.createForBlock(expressionTypingComponents, scope);
        }
        return result;
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
                // ExpressionTypingVisitorForStatements should return only null or Unit for declarations and assignments
                assert result.getType() == null || KotlinBuiltIns.isUnit(result.getType());
                result = JetTypeInfo.create(builtIns.getUnitType(), context.dataFlowInfo);
            }
        }
        return result;
    }

    @Nullable
    public JetExpression deparenthesizeWithTypeResolution(
            @Nullable JetExpression expression,
            @NotNull final ExpressionTypingContext context
    ) {
        return JetPsiUtil.deparenthesizeWithResolutionStrategy(expression, true, new Function<JetTypeReference, Void>() {
            @Override
            public Void apply(JetTypeReference reference) {
                getTypeResolver().resolveType(context.scope, reference, context.trace, true);
                return null;
            }
        });
    }

    public void resolveValueParameters(
            @NotNull List<JetParameter> valueParameters,
            @NotNull List<ValueParameterDescriptor> valueParameterDescriptors,
            @NotNull JetScope declaringScope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        for (int i = 0; i < valueParameters.size(); i++) {
            ValueParameterDescriptor valueParameterDescriptor = valueParameterDescriptors.get(i);
            JetParameter jetParameter = valueParameters.get(i);

            AnnotationResolver.resolveAnnotationsArguments(jetParameter.getModifierList(), trace);

            resolveDefaultValue(declaringScope, valueParameterDescriptor, jetParameter, dataFlowInfo, trace);
        }
    }

    private void resolveDefaultValue(
            @NotNull JetScope declaringScope,
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull JetParameter jetParameter,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull BindingTrace trace
    ) {
        if (valueParameterDescriptor.hasDefaultValue()) {
            JetExpression defaultValue = jetParameter.getDefaultValue();
            if (defaultValue != null) {
                getType(declaringScope, defaultValue, valueParameterDescriptor.getType(), dataFlowInfo, trace);
                if (DescriptorUtils.isAnnotationClass(DescriptorResolver.getContainingClass(declaringScope))) {
                    ConstantExpressionEvaluator.evaluate(defaultValue, trace, valueParameterDescriptor.getType());
                }
            }
        }
    }

    @NotNull
    public CallChecker getCallChecker() {
        List<CallChecker> checkers = expressionTypingComponents.additionalCheckerProvider.getCallCheckers();
        return new CompositeChecker(checkers);
    }

    @NotNull
    public AdditionalTypeChecker getAdditionalTypeChecker() {
        List<AdditionalTypeChecker> checkers = expressionTypingComponents.additionalCheckerProvider.getAdditionalTypeCheckers();
        return new AdditionalTypeChecker.Composite(checkers);
    }
}
