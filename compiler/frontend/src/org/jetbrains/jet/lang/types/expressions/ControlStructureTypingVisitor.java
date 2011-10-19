package org.jetbrains.jet.lang.types.expressions;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

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

    private JetType getTypeWithNewScopeAndDataFlowInfo(@NotNull JetScope scope, @NotNull JetExpression expression, @NotNull DataFlowInfo newDataFlowInfo, @NotNull ExpressionTypingContext context) {
        return facade.getType(expression, context.replaceScope(scope).replaceDataFlowInfo(newDataFlowInfo));
    }

    private void checkCondition(@NotNull JetScope scope, @Nullable JetExpression condition, ExpressionTypingContext context) {
        if (condition != null) {
            JetType conditionType = facade.getType(condition, context.replaceScope(scope));

            if (conditionType != null && !isBoolean(context.semanticServices, conditionType)) {
//                    context.trace.getErrorHandler().genericError(condition.getNode(), "Condition must be of type Boolean, but was of type " + conditionType);
                context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(condition, conditionType));
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public JetType visitIfExpression(JetIfExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression condition = expression.getCondition();
        checkCondition(context.scope, condition, context);

        JetExpression elseBranch = expression.getElse();
        JetExpression thenBranch = expression.getThen();

        WritableScopeImpl thenScope = newWritableScopeImpl(context).setDebugName("Then scope");
        DataFlowInfo thenInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, true, thenScope, context);
        DataFlowInfo elseInfo = DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, null, context);

        if (elseBranch == null) {
            if (thenBranch != null) {
                JetType type = getTypeWithNewScopeAndDataFlowInfo(thenScope, thenBranch, thenInfo, context);
                if (type != null && JetStandardClasses.isNothing(type)) {
                    facade.setResultingDataFlowInfo(elseInfo);
//                        resultScope = elseScope;
                }
                return DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
            }
            return null;
        }
        if (thenBranch == null) {
            JetType type = getTypeWithNewScopeAndDataFlowInfo(context.scope, elseBranch, elseInfo, context);
            if (type != null && JetStandardClasses.isNothing(type)) {
                facade.setResultingDataFlowInfo(thenInfo);
//                    resultScope = thenScope;
            }
            return DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
        }
        JetType thenType = getTypeWithNewScopeAndDataFlowInfo(thenScope, thenBranch, thenInfo, contextWithExpectedType);
        JetType elseType = getTypeWithNewScopeAndDataFlowInfo(context.scope, elseBranch, elseInfo, contextWithExpectedType);

        JetType result;
        if (thenType == null) {
            result = elseType;
        }
        else if (elseType == null) {
            result = thenType;
        }
        else {
            result = context.semanticServices.getTypeChecker().commonSupertype(Arrays.asList(thenType, elseType));
        }

        boolean jumpInThen = thenType != null && JetStandardClasses.isNothing(thenType);
        boolean jumpInElse = elseType != null && JetStandardClasses.isNothing(elseType);

        if (jumpInThen && !jumpInElse) {
            facade.setResultingDataFlowInfo(elseInfo);
        }
        else if (jumpInElse && !jumpInThen) {
            facade.setResultingDataFlowInfo(thenInfo);
        }
        return result;
    }

    @Override
    public JetType visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        JetExpression condition = expression.getCondition();
        checkCondition(context.scope, condition, context);
        JetExpression body = expression.getBody();
        if (body != null) {
            WritableScopeImpl scopeToExtend = newWritableScopeImpl(context).setDebugName("Scope extended in while's condition");
            DataFlowInfo conditionInfo = condition == null ? context.dataFlowInfo : DataFlowUtils.extractDataFlowInfoFromCondition(condition, true, scopeToExtend, context);
            getTypeWithNewScopeAndDataFlowInfo(scopeToExtend, body, conditionInfo, context);
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
    public JetType visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext contextWithExpectedType) {
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
            context.getServices().getBlockReturnedTypeWithWritableScope(writableScope, Collections.singletonList(body), CoercionStrategy.NO_COERCION, context);
        }
        JetExpression condition = expression.getCondition();
        checkCondition(conditionScope, condition, context);
        if (!containsBreak(expression, context)) {
            facade.setResultingDataFlowInfo(DataFlowUtils.extractDataFlowInfoFromCondition(condition, false, null, context));
        }
        return DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
    }

    @Override
    public JetType visitForExpression(JetForExpression expression, ExpressionTypingContext contextWithExpectedType) {
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
                variableDescriptor = context.getClassDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), context.scope, loopParameter);
                JetType actualParameterType = variableDescriptor.getOutType();
                if (expectedParameterType != null &&
                        actualParameterType != null &&
                        !context.semanticServices.getTypeChecker().isSubtypeOf(expectedParameterType, actualParameterType)) {
//                        context.trace.getErrorHandler().genericError(typeReference.getNode(), "The loop iterates over values of type " + expectedParameterType + " but the parameter is declared to be " + actualParameterType);
                    context.trace.report(TYPE_MISMATCH_IN_FOR_LOOP.on(typeReference, expectedParameterType, actualParameterType));
                }
            }
            else {
                if (expectedParameterType == null) {
                    expectedParameterType = ErrorUtils.createErrorType("Error");
                }
                variableDescriptor = context.getClassDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), loopParameter, expectedParameterType);
            }
            loopScope.addVariableDescriptor(variableDescriptor);
        }

        JetExpression body = expression.getBody();
        if (body != null) {
            facade.getType(body, context.replaceScope(loopScope));
        }

        return DataFlowUtils.checkType(JetStandardClasses.getUnitType(), expression, contextWithExpectedType);
    }

    @Nullable
    private JetType checkIterableConvention(@NotNull ExpressionReceiver loopRange, ExpressionTypingContext context) {
        JetExpression loopRangeExpression = loopRange.getExpression();
        OverloadResolutionResults<FunctionDescriptor> iteratorResolutionResults = context.resolveExactSignature(loopRange, "iterator", Collections.<JetType>emptyList());
        if (iteratorResolutionResults.isSuccess()) {
            FunctionDescriptor iteratorFunction = iteratorResolutionResults.getResult().getResultingDescriptor();

            context.trace.record(LOOP_RANGE_ITERATOR, loopRangeExpression, iteratorFunction);

            JetType iteratorType = iteratorFunction.getReturnType();
            FunctionDescriptor hasNextFunction = checkHasNextFunctionSupport(loopRangeExpression, iteratorType, context);
            boolean hasNextFunctionSupported = hasNextFunction != null;
            VariableDescriptor hasNextProperty = checkHasNextPropertySupport(loopRangeExpression, iteratorType, context);
            boolean hasNextPropertySupported = hasNextProperty != null;
            if (hasNextFunctionSupported && hasNextPropertySupported && !ErrorUtils.isErrorType(iteratorType)) {
                // TODO : overload resolution rules impose priorities here???
//                    context.trace.getErrorHandler().genericError(reportErrorsOn, "An ambiguity between 'iterator().hasNext()' function and 'iterator().hasNext' property");
                context.trace.report(HAS_NEXT_PROPERTY_AND_FUNCTION_AMBIGUITY.on(loopRangeExpression));
            }
            else if (!hasNextFunctionSupported && !hasNextPropertySupported) {
//                    context.trace.getErrorHandler().genericError(reportErrorsOn, "Loop range must have an 'iterator().hasNext()' function or an 'iterator().hasNext' property");
                context.trace.report(HAS_NEXT_MISSING.on(loopRangeExpression));
            }
            else {
                context.trace.record(LOOP_RANGE_HAS_NEXT, loopRange.getExpression(), hasNextFunctionSupported ? hasNextFunction : hasNextProperty);
            }

            OverloadResolutionResults<FunctionDescriptor> nextResolutionResults = context.resolveExactSignature(new TransientReceiver(iteratorType), "next", Collections.<JetType>emptyList());
            if (nextResolutionResults.isAmbiguity()) {
//                    context.trace.getErrorHandler().genericError(reportErrorsOn, "Method 'iterator().next()' is ambiguous for this expression");
                context.trace.report(NEXT_AMBIGUITY.on(loopRangeExpression));
            } else if (nextResolutionResults.isNothing()) {
//                    context.trace.getErrorHandler().genericError(reportErrorsOn, "Loop range must have an 'iterator().next()' method");
                context.trace.report(NEXT_MISSING.on(loopRangeExpression));
            } else {
                FunctionDescriptor nextFunction = nextResolutionResults.getResult().getResultingDescriptor();
                context.trace.record(LOOP_RANGE_NEXT, loopRange.getExpression(), nextFunction);
                return nextFunction.getReturnType();
            }
        }
        else {
            if (iteratorResolutionResults.isAmbiguity()) {
//                    StringBuffer stringBuffer = new StringBuffer("Method 'iterator()' is ambiguous for this expression: ");
//                    for (FunctionDescriptor functionDescriptor : iteratorResolutionResults.getResults()) {
//                        stringBuffer.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
//                    }
//                    errorMessage = stringBuffer.toString();
                context.trace.report(ITERATOR_AMBIGUITY.on(loopRangeExpression, iteratorResolutionResults.getResults()));
            }
            else {
//                    context.trace.getErrorHandler().genericError(reportErrorsOn, errorMessage);
                context.trace.report(ITERATOR_MISSING.on(loopRangeExpression));
            }
        }
        return null;
    }

    @Nullable
    private FunctionDescriptor checkHasNextFunctionSupport(@NotNull JetExpression loopRange, @NotNull JetType iteratorType, ExpressionTypingContext context) {
        OverloadResolutionResults<FunctionDescriptor> hasNextResolutionResults = context.resolveExactSignature(new TransientReceiver(iteratorType), "hasNext", Collections.<JetType>emptyList());
        if (hasNextResolutionResults.isAmbiguity()) {
//                context.trace.getErrorHandler().genericError(loopRange.getNode(), "Method 'iterator().hasNext()' is ambiguous for this expression");
            context.trace.report(HAS_NEXT_FUNCTION_AMBIGUITY.on(loopRange));
        } else if (hasNextResolutionResults.isNothing()) {
            return null;
        } else {
            assert hasNextResolutionResults.isSuccess();
            JetType hasNextReturnType = hasNextResolutionResults.getResult().getResultingDescriptor().getReturnType();
            if (!isBoolean(context.semanticServices, hasNextReturnType)) {
//                    context.trace.getErrorHandler().genericError(loopRange.getNode(), "The 'iterator().hasNext()' method of the loop range must return Boolean, but returns " + hasNextReturnType);
                context.trace.report(HAS_NEXT_FUNCTION_TYPE_MISMATCH.on(loopRange, hasNextReturnType));
            }
        }
        return hasNextResolutionResults.getResult().getResultingDescriptor();
    }

    @Nullable
    private VariableDescriptor checkHasNextPropertySupport(@NotNull JetExpression loopRange, @NotNull JetType iteratorType, ExpressionTypingContext context) {
        VariableDescriptor hasNextProperty = iteratorType.getMemberScope().getVariable("hasNext");
        // TODO :extension properties
        if (hasNextProperty == null) {
            return null;
        } else {
            JetType hasNextReturnType = hasNextProperty.getOutType();
            if (hasNextReturnType == null) {
                // TODO : accessibility
//                    context.trace.getErrorHandler().genericError(loopRange.getNode(), "The 'iterator().hasNext' property of the loop range must be readable");
                context.trace.report(HAS_NEXT_MUST_BE_READABLE.on(loopRange));
            }
            else if (!isBoolean(context.semanticServices, hasNextReturnType)) {
//                    context.trace.getErrorHandler().genericError(loopRange.getNode(), "The 'iterator().hasNext' property of the loop range must return Boolean, but returns " + hasNextReturnType);
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
                VariableDescriptor variableDescriptor = context.getClassDescriptorResolver().resolveLocalVariableDescriptor(context.scope.getContainingDeclaration(), context.scope, catchParameter);
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
            return context.semanticServices.getTypeChecker().commonSupertype(types);
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
        context.labelResolver.recordLabel(expression, context);
        if (context.expectedReturnType == TypeUtils.FORBIDDEN) {
//                context.trace.getErrorHandler().genericError(expression.getNode(), "'return' is not allowed here");
            context.trace.report(RETURN_NOT_ALLOWED.on(expression));
            return null;
        }
        JetExpression returnedExpression = expression.getReturnedExpression();

        JetType returnedType = JetStandardClasses.getUnitType();
        if (returnedExpression != null) {
            facade.getType(returnedExpression, context.replaceExpectedType(context.expectedReturnType).replaceScope(context.scope));
        }
        else {
            if (context.expectedReturnType != TypeUtils.NO_EXPECTED_TYPE && !JetStandardClasses.isUnit(context.expectedReturnType)) {
//                    context.trace.getErrorHandler().genericError(expression.getNode(), "This function must return a value of type " + context.expectedReturnType);
                context.trace.report(RETURN_TYPE_MISMATCH.on(expression, context.expectedReturnType));
            }
        }
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context);
    }

    @Override
    public JetType visitBreakExpression(JetBreakExpression expression, ExpressionTypingContext context) {
        context.labelResolver.recordLabel(expression, context);
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context);
    }

    @Override
    public JetType visitContinueExpression(JetContinueExpression expression, ExpressionTypingContext context) {
        context.labelResolver.recordLabel(expression, context);
        return DataFlowUtils.checkType(JetStandardClasses.getNothingType(), expression, context);
    }


}
