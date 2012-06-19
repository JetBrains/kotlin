/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptorUtil;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.CommonSupertypes;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeInfo;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lexer.JetTokens;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.TYPE_MISMATCH;
import static org.jetbrains.jet.lang.resolve.BindingContext.LABEL_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.STATEMENT;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
* @author abreslav
*/
public class ExpressionTypingServices {

    private final ExpressionTypingFacade expressionTypingFacade = ExpressionTypingVisitorDispatcher.create();

    @NotNull
    private Project project;
    @NotNull
    private CallResolver callResolver;
    @NotNull
    private DescriptorResolver descriptorResolver;
    @NotNull
    private TypeResolver typeResolver;



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

    public JetType safeGetType(@NotNull JetScope scope, @NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace) {
        JetType type = getType(scope, expression, expectedType, dataFlowInfo, trace);
        if (type != null) {
            return type;
        }
        return ErrorUtils.createErrorType("Type for " + expression.getText());
    }

    @NotNull
    public JetTypeInfo getTypeInfo(@NotNull final JetScope scope, @NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace) {
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                this, trace, scope, dataFlowInfo, expectedType, false
        );
        return expressionTypingFacade.getTypeInfo(expression, context);
    }

    @Nullable
    public JetType getType(@NotNull final JetScope scope, @NotNull JetExpression expression, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo, @NotNull BindingTrace trace) {
        return getTypeInfo(scope, expression, expectedType, dataFlowInfo, trace).getType();
    }

    public JetType getTypeWithNamespaces(@NotNull final JetScope scope, @NotNull JetExpression expression, @NotNull BindingTrace trace) {
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                this, trace, scope, DataFlowInfo.EMPTY, NO_EXPECTED_TYPE, true);
        return expressionTypingFacade.getTypeInfo(expression, context).getType();
//        return ((ExpressionTypingContext) ExpressionTyperVisitorWithNamespaces).INSTANCE.getType(expression, ExpressionTypingContext.newRootContext(semanticServices, trace, scope, DataFlowInfo.getEmpty(), TypeUtils.NO_EXPECTED_TYPE, TypeUtils.NO_EXPECTED_TYPE));
    }

    @NotNull
    public JetType inferFunctionReturnType(@NotNull JetScope outerScope, @NotNull JetDeclarationWithBody function, @NotNull FunctionDescriptor functionDescriptor, @NotNull BindingTrace trace) {
        Map<JetExpression, JetType> typeMap = collectReturnedExpressionsWithTypes(trace, outerScope, function, functionDescriptor);
        Collection<JetType> types = typeMap.values();
        return types.isEmpty()
               ? JetStandardClasses.getNothingType()
               : CommonSupertypes.commonSupertype(types);
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
                this, trace, functionInnerScope, dataFlowInfo, expectedReturnType != null ? expectedReturnType : NO_EXPECTED_TYPE, false
        ), trace);
    }

    /*package*/ void checkFunctionReturnType(JetDeclarationWithBody function, ExpressionTypingContext context, BindingTrace trace) {
        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) return;

        final boolean blockBody = function.hasBlockBody();
        final ExpressionTypingContext newContext =
                blockBody
                ? context.replaceExpectedType(NO_EXPECTED_TYPE)
                : context;

        if (function instanceof JetFunctionLiteralExpression) {
            JetFunctionLiteralExpression functionLiteralExpression = (JetFunctionLiteralExpression) function;
            JetBlockExpression blockExpression = functionLiteralExpression.getBodyExpression();
            assert blockExpression != null;
            getBlockReturnedType(newContext.scope, blockExpression, CoercionStrategy.COERCION_TO_UNIT, context, trace);
        }
        else {
            expressionTypingFacade.getTypeInfo(bodyExpression, newContext, !blockBody);
        }
    }

    @NotNull
    public JetTypeInfo getBlockReturnedType(@NotNull JetScope outerScope, @NotNull JetBlockExpression expression, @NotNull CoercionStrategy coercionStrategyForLastExpression, ExpressionTypingContext context, BindingTrace trace) {
        List<JetElement> block = expression.getStatements();

        DeclarationDescriptor containingDescriptor = outerScope.getContainingDeclaration();
        if (containingDescriptor instanceof ScriptDescriptor) {
            if (!(expression.getParent() instanceof JetScript)) {
                // top level script declarations should have ScriptDescriptor parent
                // and lower level script declarations should be ScriptCodeDescriptor parent
                containingDescriptor = ((ScriptDescriptor) containingDescriptor).getScriptCodeDescriptor();
            }
        }
        WritableScope scope = new WritableScopeImpl(
                outerScope, containingDescriptor, new TraceBasedRedeclarationHandler(context.trace), "getBlockReturnedType");
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);

        JetTypeInfo r;
        if (block.isEmpty()) {
            r = DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, context, context.dataFlowInfo);
        }
        else {
            r = getBlockReturnedTypeWithWritableScope(scope, block, coercionStrategyForLastExpression, context, trace);
        }
        scope.changeLockLevel(WritableScope.LockLevel.READING);

        if (containingDescriptor instanceof ScriptDescriptor) {
            trace.record(BindingContext.SCRIPT_SCOPE, (ScriptDescriptor) containingDescriptor, scope);
        }

        return r;
    }

    private Map<JetExpression, JetType> collectReturnedExpressionsWithTypes(
            final @NotNull BindingTrace trace,
            JetScope outerScope,
            final JetDeclarationWithBody function,
            FunctionDescriptor functionDescriptor) {
        JetExpression bodyExpression = function.getBodyExpression();
        assert bodyExpression != null;
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(outerScope, functionDescriptor, trace);
        expressionTypingFacade.getTypeInfo(bodyExpression, ExpressionTypingContext.newContext(
                this,
                trace, functionInnerScope, DataFlowInfo.EMPTY, NO_EXPECTED_TYPE, false), !function.hasBlockBody());
        //todo function literals
        final Collection<JetExpression> returnedExpressions = Lists.newArrayList();
        if (function.hasBlockBody()) {
            //now this code is never invoked!, it should be invoked for inference of return type of function literal with local returns
            bodyExpression.accept(new JetTreeVisitor<JetDeclarationWithBody>() {
                @Override
                public Void visitReturnExpression(JetReturnExpression expression, JetDeclarationWithBody outerFunction) {
                    JetSimpleNameExpression targetLabel = expression.getTargetLabel();
                    PsiElement element = targetLabel != null ? trace.get(LABEL_TARGET, targetLabel) : null;
                    if (element == function || (targetLabel == null && outerFunction == function)) {
                        returnedExpressions.add(expression);
                    }
                    return null;
                }

                @Override
                public Void visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, JetDeclarationWithBody outerFunction) {
                    return super.visitFunctionLiteralExpression(expression, expression.getFunctionLiteral());
                }

                @Override
                public Void visitNamedFunction(JetNamedFunction function, JetDeclarationWithBody outerFunction) {
                    return super.visitNamedFunction(function, function);
                }
            }, function);
        }
        else {
            returnedExpressions.add(bodyExpression);
        }
        Map<JetExpression, JetType> typeMap = new HashMap<JetExpression, JetType>();
        for (JetExpression returnedExpression : returnedExpressions) {
            JetType cachedType = trace.getBindingContext().get(BindingContext.EXPRESSION_TYPE, returnedExpression);
            trace.record(STATEMENT, returnedExpression, false);
            if (cachedType != null) {
                typeMap.put(returnedExpression, cachedType);
            } 
            else {
                typeMap.put(returnedExpression, ErrorUtils.createErrorType("Error function type"));
            }
        }
        return typeMap;
    }

    /*package*/
    @SuppressWarnings("SuspiciousMethodCalls")
    JetTypeInfo getBlockReturnedTypeWithWritableScope(@NotNull WritableScope scope, @NotNull List<? extends JetElement> block, @NotNull CoercionStrategy coercionStrategyForLastExpression, ExpressionTypingContext context, BindingTrace trace) {
        if (block.isEmpty()) {
            return JetTypeInfo.create(JetStandardClasses.getUnitType(), context.dataFlowInfo);
        }

        ExpressionTypingInternals blockLevelVisitor = ExpressionTypingVisitorDispatcher.createForBlock(scope);
        ExpressionTypingContext newContext = createContext(context, trace, scope, context.dataFlowInfo, NO_EXPECTED_TYPE);

        JetTypeInfo result = JetTypeInfo.create(null, context.dataFlowInfo);
        for (Iterator<? extends JetElement> iterator = block.iterator(); iterator.hasNext(); ) {
            final JetElement statement = iterator.next();
            trace.record(STATEMENT, statement);
            final JetExpression statementExpression = (JetExpression) statement;
            //TODO constructor assert context.expectedType != FORBIDDEN : ""
            if (!iterator.hasNext()) {
                if (context.expectedType != NO_EXPECTED_TYPE) {
                    if (coercionStrategyForLastExpression == CoercionStrategy.COERCION_TO_UNIT && JetStandardClasses.isUnit(context.expectedType)) {
                        // This implements coercion to Unit
                        TemporaryBindingTrace temporaryTraceExpectingUnit = TemporaryBindingTrace.create(trace);
                        final boolean[] mismatch = new boolean[1];
                        ObservableBindingTrace errorInterceptingTrace = makeTraceInterceptingTypeMismatch(temporaryTraceExpectingUnit, statementExpression, mismatch);
                        newContext = createContext(newContext, errorInterceptingTrace, scope, newContext.dataFlowInfo, context.expectedType);
                        result = blockLevelVisitor.getTypeInfo(statementExpression, newContext, true);
                        if (mismatch[0]) {
                            TemporaryBindingTrace temporaryTraceNoExpectedType = TemporaryBindingTrace.create(trace);
                            mismatch[0] = false;
                            ObservableBindingTrace interceptingTrace = makeTraceInterceptingTypeMismatch(temporaryTraceNoExpectedType, statementExpression, mismatch);
                            newContext = createContext(newContext, interceptingTrace, scope, newContext.dataFlowInfo, NO_EXPECTED_TYPE);
                            result = blockLevelVisitor.getTypeInfo(statementExpression, newContext, true);
                            if (mismatch[0]) {
                                temporaryTraceExpectingUnit.commit();
                            }
                            else {
                                temporaryTraceNoExpectedType.commit();
                            }
                        }
                        else {
                            temporaryTraceExpectingUnit.commit();
                        }
                    }
                    else {
                        newContext = createContext(newContext, trace, scope, newContext.dataFlowInfo, context.expectedType);
                        result = blockLevelVisitor.getTypeInfo(statementExpression, newContext, true);
                    }
                }
                else {
                    result = blockLevelVisitor.getTypeInfo(statementExpression, newContext, true);
                    if (coercionStrategyForLastExpression == CoercionStrategy.COERCION_TO_UNIT) {
                        boolean mightBeUnit = false;
                        if (statementExpression instanceof JetDeclaration) {
                            mightBeUnit = true;
                        }
                        if (statementExpression instanceof JetBinaryExpression) {
                            JetBinaryExpression binaryExpression = (JetBinaryExpression) statementExpression;
                            IElementType operationType = binaryExpression.getOperationToken();
                            if (operationType == JetTokens.EQ || OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
                                mightBeUnit = true;
                            }
                        }
                        if (mightBeUnit) {
                            // ExpressionTypingVisitorForStatements should return only null or Unit for declarations and assignments
                            assert result.getType() == null || JetStandardClasses.isUnit(result.getType());
                            result = JetTypeInfo.create(JetStandardClasses.getUnitType(), newContext.dataFlowInfo);
                        }
                    }
                }
            }
            else {
                result = blockLevelVisitor.getTypeInfo(statementExpression, newContext, true);
            }

            DataFlowInfo newDataFlowInfo = result.getDataFlowInfo();
            if (newDataFlowInfo != context.dataFlowInfo) {
                newContext = createContext(newContext, trace, scope, newDataFlowInfo, NO_EXPECTED_TYPE);
            }
            blockLevelVisitor = ExpressionTypingVisitorDispatcher.createForBlock(scope);
        }
        return result;
    }

    private ExpressionTypingContext createContext(ExpressionTypingContext oldContext, BindingTrace trace, WritableScope scope, DataFlowInfo dataFlowInfo, JetType expectedType) {
        return ExpressionTypingContext.newContext(
                this, oldContext.patternsToDataFlowInfo, oldContext.patternsToBoundVariableLists, oldContext.labelResolver,
                trace, scope, dataFlowInfo, expectedType, oldContext.namespacesAllowed);
    }

    private ObservableBindingTrace makeTraceInterceptingTypeMismatch(final BindingTrace trace, final JetExpression expressionToWatch, final boolean[] mismatchFound) {
        return new ObservableBindingTrace(trace) {

            @Override
            public void report(@NotNull Diagnostic diagnostic) {
                if (diagnostic.getFactory() == TYPE_MISMATCH && diagnostic.getPsiElement() == expressionToWatch) {
                    mismatchFound[0] = true;
                }
                super.report(diagnostic);
            }
        };
    }
}
