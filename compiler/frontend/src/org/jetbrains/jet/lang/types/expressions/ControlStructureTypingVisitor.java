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

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.*;

/**
 * @author abreslav
 */
public class ControlStructureTypingVisitor extends ExpressionTypingVisitor {

    protected ControlStructureTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    private void checkCondition(@NotNull JetScope scope, @Nullable JetExpression condition, ExpressionTypingContext context) {
        if (condition != null) {
            JetType conditionType = facade.getTypeInfo(condition, context.replaceScope(scope)).getType();

            if (conditionType != null && !isBoolean(conditionType)) {
                context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(condition, conditionType));
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public JetTypeInfo visitIfExpression(JetIfExpression expression, ExpressionTypingContext context) {
        return visitIfExpression(expression, context, false);
    }

    public JetTypeInfo visitIfExpression(JetIfExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression condition = expression.getCondition();
        checkCondition(context.scope, condition, context);

        JetExpression elseBranch = expression.getElse();
        JetExpression thenBranch = expression.getThen();

        WritableScopeImpl thenScope = newWritableScopeImpl(context, "Then scope");
        WritableScopeImpl elseScope = newWritableScopeImpl(context, "Else scope");
        DataFlowInfo thenInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, true, thenScope, context);
        DataFlowInfo elseInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, null, context);

        if (elseBranch == null) {
            if (thenBranch != null) {
                JetTypeInfo typeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(thenScope, Collections.singletonList(thenBranch), CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(thenInfo), context.trace);
                JetType type = typeInfo.getType();
                DataFlowInfo dataFlowInfo;
                if (type != null && JetStandardClasses.isNothing(type)) {
                    dataFlowInfo = elseInfo;
                } else {
                    dataFlowInfo = typeInfo.getDataFlowInfo().or(elseInfo);
                }
                return DataFlowUtils.checkImplicitCast(DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType), expression, contextWithExpectedType, isStatement, dataFlowInfo);
            }
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }
        if (thenBranch == null) {
            JetTypeInfo typeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(elseScope, Collections.singletonList(elseBranch), CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(elseInfo), context.trace);
            JetType type = typeInfo.getType();
            DataFlowInfo dataFlowInfo;
            if (type != null && JetStandardClasses.isNothing(type)) {
                dataFlowInfo = thenInfo;
            } else {
                dataFlowInfo = typeInfo.getDataFlowInfo().or(thenInfo);
            }
            return DataFlowUtils.checkImplicitCast(DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType), expression, contextWithExpectedType, isStatement, dataFlowInfo);
        }
        CoercionStrategy coercionStrategy = isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION;
        JetTypeInfo thenTypeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(thenScope, Collections.singletonList(thenBranch), coercionStrategy, contextWithExpectedType.replaceDataFlowInfo(thenInfo), context.trace);
        JetTypeInfo elseTypeInfo = context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(elseScope, Collections.singletonList(elseBranch), coercionStrategy, contextWithExpectedType.replaceDataFlowInfo(elseInfo), context.trace);
        JetType thenType = thenTypeInfo.getType();
        JetType elseType = elseTypeInfo.getType();
        DataFlowInfo thenDataFlowInfo = thenTypeInfo.getDataFlowInfo();
        DataFlowInfo elseDataFlowInfo = elseTypeInfo.getDataFlowInfo();

        boolean jumpInThen = thenType != null && JetStandardClasses.isNothing(thenType);
        boolean jumpInElse = elseType != null && JetStandardClasses.isNothing(elseType);

        JetTypeInfo result;
        if (thenType == null && elseType == null) {
            result = JetTypeInfo.create(null, thenDataFlowInfo.or(elseDataFlowInfo));
        }
        else if (thenType == null || (jumpInThen && !jumpInElse)) {
            result = elseTypeInfo;
        }
        else if (elseType == null || (jumpInElse && !jumpInThen)) {
            result = thenTypeInfo;
        }
        else {
            result = JetTypeInfo.create(CommonSupertypes.commonSupertype(Arrays.asList(thenType, elseType)), thenDataFlowInfo.or(elseDataFlowInfo));
        }

        return DataFlowUtils.checkImplicitCast(result.getType(), expression, contextWithExpectedType, isStatement, result.getDataFlowInfo());
     }

    @Override
    public JetTypeInfo visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext context) {
        return visitWhileExpression(expression, context, false);
    }

    public JetTypeInfo visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression condition = expression.getCondition();
        checkCondition(context.scope, condition, context);
        JetExpression body = expression.getBody();
        if (body != null) {
            WritableScopeImpl scopeToExtend = newWritableScopeImpl(context, "Scope extended in while's condition");
            DataFlowInfo conditionInfo = condition == null ? context.dataFlowInfo : DataFlowUtils.extractDataFlowInfoFromCondition(condition, true, scopeToExtend, context);
            context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(scopeToExtend, Collections.singletonList(body), CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(conditionInfo), context.trace);
        }
        DataFlowInfo dataFlowInfo;
        if (!containsBreak(expression, context)) {
            dataFlowInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, null, context);
        }
        else {
            dataFlowInfo = context.dataFlowInfo;
        }
        return DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType, dataFlowInfo);
    }

    private boolean containsBreak(final JetLoopExpression loopExpression, final ExpressionTypingContext context) {
        final boolean[] result = new boolean[1];
        result[0] = false;
        //todo breaks in inline function literals
        loopExpression.accept(new JetTreeVisitor<JetLoopExpression>() {
            @Override
            public Void visitBreakExpression(JetBreakExpression breakExpression, JetLoopExpression outerLoop) {
                JetSimpleNameExpression targetLabel = breakExpression.getTargetLabel();
                PsiElement element = targetLabel != null ? context.trace.get(LABEL_TARGET, targetLabel) : null;
                if (element == loopExpression || (targetLabel == null && outerLoop == loopExpression)) {
                    result[0] = true;
                }
                return null;
            }

            @Override
            public Void visitLoopExpression(JetLoopExpression loopExpression, JetLoopExpression outerLoop) {
                return super.visitLoopExpression(loopExpression, loopExpression);
            }
        }, loopExpression);

        return result[0];
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext context) {
        return visitDoWhileExpression(expression, context, false);
    }
    public JetTypeInfo visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression body = expression.getBody();
        JetScope conditionScope = context.scope;
        if (body instanceof JetFunctionLiteralExpression) {
            JetFunctionLiteralExpression function = (JetFunctionLiteralExpression) body;
            if (!function.getFunctionLiteral().hasParameterSpecification()) {
                WritableScope writableScope = newWritableScopeImpl(context, "do..while body scope");
                conditionScope = writableScope;
                context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(writableScope, function.getFunctionLiteral().getBodyExpression().getStatements(), CoercionStrategy.NO_COERCION, context, context.trace);
                context.trace.record(BindingContext.BLOCK, function);
            }
            else {
                facade.getTypeInfo(body, context.replaceScope(context.scope));
            }
        }
        else if (body != null) {
            WritableScope writableScope = newWritableScopeImpl(context, "do..while body scope");
            conditionScope = writableScope;
            List<JetElement> block;
            if (body instanceof JetBlockExpression) {
                block = ((JetBlockExpression)body).getStatements();
            }
            else {
                block = Collections.<JetElement>singletonList(body);
            }
            context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(writableScope, block, CoercionStrategy.NO_COERCION, context, context.trace);
        }
        JetExpression condition = expression.getCondition();
        checkCondition(conditionScope, condition, context);
        DataFlowInfo dataFlowInfo;
        if (!containsBreak(expression, context)) {
            dataFlowInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, null, context);
        }
        else {
            dataFlowInfo = context.dataFlowInfo;
        }
        return DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType, dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitForExpression(JetForExpression expression, ExpressionTypingContext context) {
        return visitForExpression(expression, context, false);
    }

    public JetTypeInfo visitForExpression(JetForExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetParameter loopParameter = expression.getLoopParameter();
        JetExpression loopRange = expression.getLoopRange();
        JetType expectedParameterType = null;
        if (loopRange != null) {
            ExpressionReceiver loopRangeReceiver = getExpressionReceiver(facade, loopRange, context.replaceScope(context.scope));
            if (loopRangeReceiver != null) {
                expectedParameterType = checkIterableConvention(loopRangeReceiver, context);
            }
        }

        WritableScope loopScope = newWritableScopeImpl(context, "Scope with for-loop index");

        if (loopParameter != null) {
            JetTypeReference typeReference = loopParameter.getTypeReference();
            VariableDescriptor variableDescriptor;
            if (typeReference != null) {
                variableDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), context.scope, loopParameter, context.trace);
                JetType actualParameterType = variableDescriptor.getType();
                if (expectedParameterType != null &&
                        actualParameterType != null &&
                        !JetTypeChecker.INSTANCE.isSubtypeOf(expectedParameterType, actualParameterType)) {
                    context.trace.report(TYPE_MISMATCH_IN_FOR_LOOP.on(typeReference, expectedParameterType, actualParameterType));
                }
            }
            else {
                if (expectedParameterType == null) {
                    expectedParameterType = ErrorUtils.createErrorType("Error");
                }
                variableDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), loopParameter, expectedParameterType, context.trace);
            }

            {
                // http://youtrack.jetbrains.net/issue/KT-527

                VariableDescriptor olderVariable = context.scope.getLocalVariable(variableDescriptor.getName());
                if (olderVariable != null && DescriptorUtils.isLocal(context.scope.getContainingDeclaration(), olderVariable)) {
                    PsiElement declaration = BindingContextUtils.descriptorToDeclaration(context.trace.getBindingContext(), variableDescriptor);
                    context.trace.report(Errors.NAME_SHADOWING.on(declaration, variableDescriptor.getName().getName()));
                }
            }

            loopScope.addVariableDescriptor(variableDescriptor);
        }

        JetExpression body = expression.getBody();
        if (body != null) {
            context.expressionTypingServices.getBlockReturnedTypeWithWritableScope(loopScope, Collections.singletonList(body), CoercionStrategy.NO_COERCION, context, context.trace);
        }

        return DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType, context.dataFlowInfo);
    }

    @Nullable
    /*package*/ static JetType checkIterableConvention(@NotNull ExpressionReceiver loopRange, ExpressionTypingContext context) {
        JetExpression loopRangeExpression = loopRange.getExpression();

        // Make a fake call loopRange.iterator(), and try to resolve it
        Name iterator = Name.identifier("iterator");
        OverloadResolutionResults<FunctionDescriptor> iteratorResolutionResults = resolveFakeCall(loopRange, context, iterator);

        // We allow the loop range to be null (nothing happens), so we make the receiver type non-null
        if (!iteratorResolutionResults.isSuccess()) {
            ExpressionReceiver nonNullReceiver = new ExpressionReceiver(loopRange.getExpression(), TypeUtils.makeNotNullable(loopRange.getType()));
            OverloadResolutionResults<FunctionDescriptor> iteratorResolutionResultsWithNonNullReceiver = resolveFakeCall(nonNullReceiver, context, iterator);
            if (iteratorResolutionResultsWithNonNullReceiver.isSuccess()) {
                iteratorResolutionResults = iteratorResolutionResultsWithNonNullReceiver;
            }
        }
        
        if (iteratorResolutionResults.isSuccess()) {
            FunctionDescriptor iteratorFunction = iteratorResolutionResults.getResultingCall().getResultingDescriptor();

            context.trace.record(LOOP_RANGE_ITERATOR, loopRangeExpression, iteratorFunction);

            JetType iteratorType = iteratorFunction.getReturnType();
            FunctionDescriptor hasNextFunction = checkHasNextFunctionSupport(loopRangeExpression, iteratorType, context);
            boolean hasNextFunctionSupported = hasNextFunction != null;
            VariableDescriptor hasNextProperty = checkHasNextPropertySupport(loopRangeExpression, iteratorType, context);
            boolean hasNextPropertySupported = hasNextProperty != null;
            if (hasNextFunctionSupported && hasNextPropertySupported && !ErrorUtils.isErrorType(iteratorType)) {
                // TODO : overload resolution rules impose priorities here???
                context.trace.report(HAS_NEXT_PROPERTY_AND_FUNCTION_AMBIGUITY.on(loopRangeExpression));
            }
            else if (!hasNextFunctionSupported && !hasNextPropertySupported) {
                context.trace.report(HAS_NEXT_MISSING.on(loopRangeExpression));
            }
            else {
                context.trace.record(LOOP_RANGE_HAS_NEXT, loopRange.getExpression(), hasNextFunctionSupported ? hasNextFunction : hasNextProperty);
            }

            OverloadResolutionResults<FunctionDescriptor> nextResolutionResults = context.resolveExactSignature(new TransientReceiver(iteratorType), Name.identifier("next"), Collections.<JetType>emptyList());
            if (nextResolutionResults.isAmbiguity()) {
                context.trace.report(NEXT_AMBIGUITY.on(loopRangeExpression));
            }
            else if (nextResolutionResults.isNothing()) {
                context.trace.report(NEXT_MISSING.on(loopRangeExpression));
            }
            else {
                FunctionDescriptor nextFunction = nextResolutionResults.getResultingCall().getResultingDescriptor();
                context.trace.record(LOOP_RANGE_NEXT, loopRange.getExpression(), nextFunction);
                return nextFunction.getReturnType();
            }
        }
        else {
            if (iteratorResolutionResults.isAmbiguity()) {
//                    StringBuffer stringBuffer = new StringBuffer("Method 'iterator()' is ambiguous for this expression: ");
//                    for (FunctionDescriptor functionDescriptor : iteratorResolutionResults.getResultingCalls()) {
//                        stringBuffer.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
//                    }
//                    errorMessage = stringBuffer.toString();
                context.trace.report(ITERATOR_AMBIGUITY.on(loopRangeExpression, iteratorResolutionResults.getResultingCalls()));
            }
            else {
                context.trace.report(ITERATOR_MISSING.on(loopRangeExpression));
            }
        }
        return null;
    }

    public static OverloadResolutionResults<FunctionDescriptor> resolveFakeCall(ExpressionReceiver receiver,
                                                                                ExpressionTypingContext context, Name name) {
        JetReferenceExpression fake = JetPsiFactory.createSimpleName(context.expressionTypingServices.getProject(), "fake");
        BindingTrace fakeTrace = new BindingTraceContext();
        Call call = CallMaker.makeCall(fake, receiver, null, fake, Collections.<ValueArgument>emptyList());
        return context.replaceBindingTrace(fakeTrace).resolveCallWithGivenName(call, fake, name);
    }

    @Nullable
    private static FunctionDescriptor checkHasNextFunctionSupport(@NotNull JetExpression loopRange, @NotNull JetType iteratorType, ExpressionTypingContext context) {
        OverloadResolutionResults<FunctionDescriptor> hasNextResolutionResults = context.resolveExactSignature(new TransientReceiver(iteratorType), Name.identifier("hasNext"), Collections.<JetType>emptyList());
        if (hasNextResolutionResults.isAmbiguity()) {
            context.trace.report(HAS_NEXT_FUNCTION_AMBIGUITY.on(loopRange));
        }
        else if (hasNextResolutionResults.isNothing()) {
            return null;
        }
        else {
            assert hasNextResolutionResults.isSuccess();
            JetType hasNextReturnType = hasNextResolutionResults.getResultingDescriptor().getReturnType();
            if (!isBoolean(hasNextReturnType)) {
                context.trace.report(HAS_NEXT_FUNCTION_TYPE_MISMATCH.on(loopRange, hasNextReturnType));
            }
        }
        return hasNextResolutionResults.getResultingCall().getResultingDescriptor();
    }

    @Nullable
    private static VariableDescriptor checkHasNextPropertySupport(@NotNull JetExpression loopRange, @NotNull JetType iteratorType, ExpressionTypingContext context) {
        VariableDescriptor hasNextProperty = DescriptorUtils.filterNonExtensionProperty(iteratorType.getMemberScope().getProperties(Name.identifier("hasNext")));
        if (hasNextProperty == null) {
            return null;
        }
        else {
            JetType hasNextReturnType = hasNextProperty.getType();
            if (hasNextReturnType == null) {
                // TODO : accessibility
                context.trace.report(HAS_NEXT_MUST_BE_READABLE.on(loopRange));
            }
            else if (!isBoolean(hasNextReturnType)) {
                context.trace.report(HAS_NEXT_PROPERTY_TYPE_MISMATCH.on(loopRange, hasNextReturnType));
            }
        }
        return hasNextProperty;
    }

    @Override
    public JetTypeInfo visitTryExpression(JetTryExpression expression, ExpressionTypingContext context) {
        JetExpression tryBlock = expression.getTryBlock();
        List<JetCatchClause> catchClauses = expression.getCatchClauses();
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        List<JetType> types = new ArrayList<JetType>();
        for (JetCatchClause catchClause : catchClauses) {
            JetParameter catchParameter = catchClause.getCatchParameter();
            JetExpression catchBody = catchClause.getCatchBody();
            if (catchParameter != null) {
                VariableDescriptor variableDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveLocalVariableDescriptor(
                        context.scope.getContainingDeclaration(), context.scope, catchParameter, context.trace);
                JetType throwableType = JetStandardLibrary.getInstance().getThrowable().getDefaultType();
                DataFlowUtils.checkType(variableDescriptor.getType(), catchParameter, context.replaceExpectedType(throwableType));
                if (catchBody != null) {
                    WritableScope catchScope = newWritableScopeImpl(context, "Catch scope");
                    catchScope.addVariableDescriptor(variableDescriptor);
                    JetType type = facade.getTypeInfo(catchBody, context.replaceScope(catchScope)).getType();
                    if (type != null) {
                        types.add(type);
                    }
                }
            }
        }
        if (finallyBlock != null) {
            facade.getTypeInfo(finallyBlock.getFinalExpression(), context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE));
        }
        JetType type = facade.getTypeInfo(tryBlock, context).getType();
        if (type != null) {
            types.add(type);
        }
        if (types.isEmpty()) {
            return JetTypeInfo.create(null, context.dataFlowInfo);
        }
        else {
            return JetTypeInfo.create(CommonSupertypes.commonSupertype(types), context.dataFlowInfo);
        }
    }

    @Override
    public JetTypeInfo visitThrowExpression(JetThrowExpression expression, ExpressionTypingContext context) {
        JetExpression thrownExpression = expression.getThrownExpression();
        if (thrownExpression != null) {
            JetType throwableType = JetStandardLibrary.getInstance().getThrowable().getDefaultType();
            facade.getTypeInfo(thrownExpression, context.replaceExpectedType(throwableType).replaceScope(context.scope));
        }
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitReturnExpression(JetReturnExpression expression, ExpressionTypingContext context) {
        JetElement element = context.labelResolver.resolveLabel(expression, context);

        JetExpression returnedExpression = expression.getReturnedExpression();

        JetType expectedType = TypeUtils.NO_EXPECTED_TYPE;
        JetExpression parentDeclaration = PsiTreeUtil.getParentOfType(expression, JetDeclaration.class);
        if (parentDeclaration instanceof JetFunctionLiteral) {
            parentDeclaration = (JetFunctionLiteralExpression) parentDeclaration.getParent();
        }
        if (parentDeclaration instanceof JetParameter) {
            context.trace.report(RETURN_NOT_ALLOWED.on(expression));
        }
        assert parentDeclaration != null;
        DeclarationDescriptor declarationDescriptor = context.trace.get(DECLARATION_TO_DESCRIPTOR, parentDeclaration);
        FunctionDescriptor containingFunctionDescriptor = DescriptorUtils.getParentOfType(declarationDescriptor, FunctionDescriptor.class, false);

        if (expression.getTargetLabel() == null) {
            if (containingFunctionDescriptor != null) {
                PsiElement containingFunction = BindingContextUtils.callableDescriptorToDeclaration(context.trace.getBindingContext(), containingFunctionDescriptor);
                assert containingFunction != null;
                if (containingFunction instanceof JetFunctionLiteralExpression) {
                    do {
                        containingFunctionDescriptor = DescriptorUtils.getParentOfType(containingFunctionDescriptor, FunctionDescriptor.class);
                        containingFunction = containingFunctionDescriptor != null ? BindingContextUtils.callableDescriptorToDeclaration(context.trace.getBindingContext(), containingFunctionDescriptor) : null;
                    } while (containingFunction instanceof JetFunctionLiteralExpression);
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                }
                if (containingFunctionDescriptor != null) {
                    expectedType = DescriptorUtils.getFunctionExpectedReturnType(containingFunctionDescriptor, (JetElement) containingFunction);
                }
            }
            else {
                context.trace.report(RETURN_NOT_ALLOWED.on(expression));
            }
        }
        else if (element != null) {
            SimpleFunctionDescriptor functionDescriptor = context.trace.get(FUNCTION, element);
            if (functionDescriptor != null) {
                expectedType = DescriptorUtils.getFunctionExpectedReturnType(functionDescriptor, element);
                if (functionDescriptor != containingFunctionDescriptor) {
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                }
            }
            else {
                context.trace.report(NOT_A_RETURN_LABEL.on(expression, expression.getLabelName()));
            }
        }
        if (returnedExpression != null) {
            facade.getTypeInfo(returnedExpression, context.replaceExpectedType(expectedType).replaceScope(context.scope));
        }
        else {
            if (expectedType != TypeUtils.NO_EXPECTED_TYPE && expectedType != null && !JetStandardClasses.isUnit(expectedType)) {
                context.trace.report(RETURN_TYPE_MISMATCH.on(expression, expectedType));
            }
        }
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitBreakExpression(JetBreakExpression expression, ExpressionTypingContext context) {
        context.labelResolver.resolveLabel(expression, context);
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitContinueExpression(JetContinueExpression expression, ExpressionTypingContext context) {
        context.labelResolver.resolveLabel(expression, context);
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context, context.dataFlowInfo);
    }
}
