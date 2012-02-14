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
import org.jetbrains.jet.lang.descriptors.NamedFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.*;

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
            JetType conditionType = facade.getType(condition, context.replaceScope(scope));

            if (conditionType != null && !isBoolean(context.semanticServices, conditionType)) {
                context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(condition, conditionType));
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public JetType visitIfExpression(JetIfExpression expression, ExpressionTypingContext context) {
        return visitIfExpression(expression, context, false);
    }

    public JetType visitIfExpression(JetIfExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression condition = expression.getCondition();
        checkCondition(context.scope, condition, context);

        JetExpression elseBranch = expression.getElse();
        JetExpression thenBranch = expression.getThen();

        WritableScopeImpl thenScope = newWritableScopeImpl(context).setDebugName("Then scope");
        WritableScopeImpl elseScope = newWritableScopeImpl(context).setDebugName("Else scope");
        DataFlowInfo thenInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, true, thenScope, context);
        DataFlowInfo elseInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, null, context);

        if (elseBranch == null) {
            if (thenBranch != null) {
                JetType type = context.getServices().getBlockReturnedTypeWithWritableScope(thenScope, Collections.singletonList(thenBranch), CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(thenInfo));
                if (type != null && JetStandardClasses.isNothing(type)) {
                    facade.setResultingDataFlowInfo(elseInfo);
                }
                return DataFlowUtils.checkImplicitCast(DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType), expression, contextWithExpectedType, isStatement);
            }
            return null;
        }
        if (thenBranch == null) {
            JetType type = context.getServices().getBlockReturnedTypeWithWritableScope(elseScope, Collections.singletonList(elseBranch), CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(elseInfo));
            if (type != null && JetStandardClasses.isNothing(type)) {
                facade.setResultingDataFlowInfo(thenInfo);
            }
            return DataFlowUtils.checkImplicitCast(DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType), expression, contextWithExpectedType, isStatement);
        }
        CoercionStrategy coercionStrategy = isStatement ? CoercionStrategy.COERCION_TO_UNIT : CoercionStrategy.NO_COERCION;
        JetType thenType = context.getServices().getBlockReturnedTypeWithWritableScope(thenScope, Collections.singletonList(thenBranch), coercionStrategy, contextWithExpectedType.replaceDataFlowInfo(thenInfo));
        JetType elseType = context.getServices().getBlockReturnedTypeWithWritableScope(elseScope, Collections.singletonList(elseBranch), coercionStrategy, contextWithExpectedType.replaceDataFlowInfo(elseInfo));

        JetType result;
        if (thenType == null) {
            result = elseType;
        }
        else if (elseType == null) {
            result = thenType;
        }
        else {
            result = CommonSupertypes.commonSupertype(Arrays.asList(thenType, elseType));
        }

        boolean jumpInThen = thenType != null && JetStandardClasses.isNothing(thenType);
        boolean jumpInElse = elseType != null && JetStandardClasses.isNothing(elseType);

        if (jumpInThen && !jumpInElse) {
            facade.setResultingDataFlowInfo(elseInfo);
        }
        else if (jumpInElse && !jumpInThen) {
            facade.setResultingDataFlowInfo(thenInfo);
        }
        if (result == null) return null;
        return DataFlowUtils.checkImplicitCast(result, expression, contextWithExpectedType, isStatement);
    }

    @Override
    public JetType visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext context) {
        return visitWhileExpression(expression, context, false);
    }

    public JetType visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression condition = expression.getCondition();
        checkCondition(context.scope, condition, context);
        JetExpression body = expression.getBody();
        if (body != null) {
            WritableScopeImpl scopeToExtend = newWritableScopeImpl(context).setDebugName("Scope extended in while's condition");
            DataFlowInfo conditionInfo = condition == null ? context.dataFlowInfo : DataFlowUtils.extractDataFlowInfoFromCondition(condition, true, scopeToExtend, context);
            context.getServices().getBlockReturnedTypeWithWritableScope(scopeToExtend, Collections.singletonList(body), CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(conditionInfo));
        }
        if (!containsBreak(expression, context)) {
            facade.setResultingDataFlowInfo(DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, null, context));
        }
        return DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
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
    public JetType visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext context) {
        return visitDoWhileExpression(expression, context, false);
    }
    public JetType visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return DataFlowUtils.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression body = expression.getBody();
        JetScope conditionScope = context.scope;
        if (body instanceof JetFunctionLiteralExpression) {
            JetFunctionLiteralExpression function = (JetFunctionLiteralExpression) body;
            if (!function.getFunctionLiteral().hasParameterSpecification()) {
                WritableScope writableScope = newWritableScopeImpl(context).setDebugName("do..while body scope");
                conditionScope = writableScope;
                context.getServices().getBlockReturnedTypeWithWritableScope(writableScope, function.getFunctionLiteral().getBodyExpression().getStatements(), CoercionStrategy.NO_COERCION, context);
                context.trace.record(BindingContext.BLOCK, function);
            } else {
                facade.getType(body, context.replaceScope(context.scope));
            }
        }
        else if (body != null) {
            WritableScope writableScope = newWritableScopeImpl(context).setDebugName("do..while body scope");
            conditionScope = writableScope;
            List<JetElement> block;
            if (body instanceof JetBlockExpression) {
                block = ((JetBlockExpression)body).getStatements();
            }
            else {
                block = Collections.<JetElement>singletonList(body);
            }
            context.getServices().getBlockReturnedTypeWithWritableScope(writableScope, block, CoercionStrategy.NO_COERCION, context);
        }
        JetExpression condition = expression.getCondition();
        checkCondition(conditionScope, condition, context);
        if (!containsBreak(expression, context)) {
            facade.setResultingDataFlowInfo(DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, null, context));
        }
        return DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
    }

    @Override
    public JetType visitForExpression(JetForExpression expression, ExpressionTypingContext context) {
        return visitForExpression(expression, context, false);
    }

    public JetType visitForExpression(JetForExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
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

        WritableScope loopScope = newWritableScopeImpl(context).setDebugName("Scope with for-loop index");

        if (loopParameter != null) {
            JetTypeReference typeReference = loopParameter.getTypeReference();
            VariableDescriptor variableDescriptor;
            if (typeReference != null) {
                variableDescriptor = context.getDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), context.scope, loopParameter);
                JetType actualParameterType = variableDescriptor.getOutType();
                if (expectedParameterType != null &&
                        actualParameterType != null &&
                        !context.semanticServices.getTypeChecker().isSubtypeOf(expectedParameterType, actualParameterType)) {
                    context.trace.report(TYPE_MISMATCH_IN_FOR_LOOP.on(typeReference, expectedParameterType, actualParameterType));
                }
            }
            else {
                if (expectedParameterType == null) {
                    expectedParameterType = ErrorUtils.createErrorType("Error");
                }
                variableDescriptor = context.getDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), loopParameter, expectedParameterType);
            }

            {
                // http://youtrack.jetbrains.net/issue/KT-527

                VariableDescriptor olderVariable = context.scope.getLocalVariable(variableDescriptor.getName());
                if (olderVariable != null && DescriptorUtils.isLocal(context.scope.getContainingDeclaration(), olderVariable)) {
                    context.trace.report(Errors.NAME_SHADOWING.on(variableDescriptor, context.trace.getBindingContext()));
                }
            }

            loopScope.addVariableDescriptor(variableDescriptor);
        }

        JetExpression body = expression.getBody();
        if (body != null) {
            context.getServices().getBlockReturnedTypeWithWritableScope(loopScope, Collections.singletonList(body), CoercionStrategy.NO_COERCION, context);
        }

        return DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
    }

    @Nullable
    /*package*/ static JetType checkIterableConvention(@NotNull ExpressionReceiver loopRange, ExpressionTypingContext context) {
        JetExpression loopRangeExpression = loopRange.getExpression();

        // Make a fake call loopRange.iterator(), and try to resolve it
        String iterator = "iterator";
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

            OverloadResolutionResults<FunctionDescriptor> nextResolutionResults = context.resolveExactSignature(new TransientReceiver(iteratorType), "next", Collections.<JetType>emptyList());
            if (nextResolutionResults.isAmbiguity()) {
                context.trace.report(NEXT_AMBIGUITY.on(loopRangeExpression));
            } else if (nextResolutionResults.isNothing()) {
                context.trace.report(NEXT_MISSING.on(loopRangeExpression));
            } else {
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

    private static OverloadResolutionResults<FunctionDescriptor> resolveFakeCall(ExpressionReceiver receiver, ExpressionTypingContext context, String name) {
        JetReferenceExpression fake = JetPsiFactory.createSimpleName(context.getProject(), "fake");
        BindingTrace fakeTrace = new BindingTraceContext();
        Call call = CallMaker.makeCall(fake, receiver, null, fake, Collections.<ValueArgument>emptyList());
        return context.replaceBindingTrace(fakeTrace).resolveCallWithGivenName(call, fake, name);
    }

    @Nullable
    private static FunctionDescriptor checkHasNextFunctionSupport(@NotNull JetExpression loopRange, @NotNull JetType iteratorType, ExpressionTypingContext context) {
        OverloadResolutionResults<FunctionDescriptor> hasNextResolutionResults = context.resolveExactSignature(new TransientReceiver(iteratorType), "hasNext", Collections.<JetType>emptyList());
        if (hasNextResolutionResults.isAmbiguity()) {
            context.trace.report(HAS_NEXT_FUNCTION_AMBIGUITY.on(loopRange));
        } else if (hasNextResolutionResults.isNothing()) {
            return null;
        } else {
            assert hasNextResolutionResults.isSuccess();
            JetType hasNextReturnType = hasNextResolutionResults.getResultingDescriptor().getReturnType();
            if (!isBoolean(context.semanticServices, hasNextReturnType)) {
                context.trace.report(HAS_NEXT_FUNCTION_TYPE_MISMATCH.on(loopRange, hasNextReturnType));
            }
        }
        return hasNextResolutionResults.getResultingCall().getResultingDescriptor();
    }

    @Nullable
    private static VariableDescriptor checkHasNextPropertySupport(@NotNull JetExpression loopRange, @NotNull JetType iteratorType, ExpressionTypingContext context) {
        VariableDescriptor hasNextProperty = DescriptorUtils.filterNonExtensionProperty(iteratorType.getMemberScope().getProperties("hasNext"));
        if (hasNextProperty == null) {
            return null;
        } else {
            JetType hasNextReturnType = hasNextProperty.getOutType();
            if (hasNextReturnType == null) {
                // TODO : accessibility
                context.trace.report(HAS_NEXT_MUST_BE_READABLE.on(loopRange));
            }
            else if (!isBoolean(context.semanticServices, hasNextReturnType)) {
                context.trace.report(HAS_NEXT_PROPERTY_TYPE_MISMATCH.on(loopRange, hasNextReturnType));
            }
        }
        return hasNextProperty;
    }

    @Override
    public JetType visitTryExpression(JetTryExpression expression, ExpressionTypingContext context) {
        JetExpression tryBlock = expression.getTryBlock();
        List<JetCatchClause> catchClauses = expression.getCatchClauses();
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        List<JetType> types = new ArrayList<JetType>();
        for (JetCatchClause catchClause : catchClauses) {
            JetParameter catchParameter = catchClause.getCatchParameter();
            JetExpression catchBody = catchClause.getCatchBody();
            if (catchParameter != null) {
                VariableDescriptor variableDescriptor = context.getDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), context.scope, catchParameter);
                if (catchBody != null) {
                    WritableScope catchScope = newWritableScopeImpl(context).setDebugName("Catch scope");
                    catchScope.addVariableDescriptor(variableDescriptor);
                    JetType type = facade.getType(catchBody, context.replaceScope(catchScope));
                    if (type != null) {
                        types.add(type);
                    }
                }
            }
        }
        if (finallyBlock != null) {
            types.clear(); // Do not need the list for the check, but need the code above to typecheck catch bodies
            JetType type = facade.getType(finallyBlock.getFinalExpression(), context.replaceScope(context.scope));
            if (type != null) {
                types.add(type);
            }
        }
        JetType type = facade.getType(tryBlock, context.replaceScope(context.scope));
        if (type != null) {
            types.add(type);
        }
        if (types.isEmpty()) {
            return null;
        }
        else {
            return CommonSupertypes.commonSupertype(types);
        }
    }

    @Override
    public JetType visitThrowExpression(JetThrowExpression expression, ExpressionTypingContext context) {
        JetExpression thrownExpression = expression.getThrownExpression();
        if (thrownExpression != null) {
            JetType type = facade.getType(thrownExpression, context.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceScope(context.scope));
            // TODO : check that it inherits Throwable
        }
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context);
    }

    @Override
    public JetType visitReturnExpression(JetReturnExpression expression, ExpressionTypingContext context) {
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
                PsiElement containingFunction = context.trace.get(DESCRIPTOR_TO_DECLARATION, containingFunctionDescriptor);
                assert containingFunction != null;
                if (containingFunction instanceof JetFunctionLiteralExpression) {
                    do {
                        containingFunctionDescriptor = DescriptorUtils.getParentOfType(containingFunctionDescriptor, FunctionDescriptor.class);
                        containingFunction = containingFunctionDescriptor != null ? context.trace.get(DESCRIPTOR_TO_DECLARATION, containingFunctionDescriptor) : null;
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
            NamedFunctionDescriptor functionDescriptor = context.trace.get(FUNCTION, element);
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
            facade.getType(returnedExpression, context.replaceExpectedType(expectedType).replaceScope(context.scope));
        }
        else {
            if (expectedType != TypeUtils.NO_EXPECTED_TYPE && expectedType != null && !JetStandardClasses.isUnit(expectedType)) {
                context.trace.report(RETURN_TYPE_MISMATCH.on(expression, expectedType));
            }
        }
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context);
    }

    @Override
    public JetType visitBreakExpression(JetBreakExpression expression, ExpressionTypingContext context) {
        context.labelResolver.resolveLabel(expression, context);
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context);
    }

    @Override
    public JetType visitContinueExpression(JetContinueExpression expression, ExpressionTypingContext context) {
        context.labelResolver.resolveLabel(expression, context);
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context);
    }
}
