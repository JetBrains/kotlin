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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.model.MutableDataFlowInfoForArguments;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.types.CommonSupertypes;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getBuiltIns;
import static org.jetbrains.kotlin.types.TypeUtils.*;
import static org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.createCallForSpecialConstruction;
import static org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.createDataFlowInfoForArgumentsForIfCall;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.*;

public class ControlStructureTypingVisitor extends ExpressionTypingVisitor {

    public static final String RETURN_NOT_ALLOWED_MESSAGE = "Return not allowed";

    protected ControlStructureTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @NotNull
    private DataFlowInfo checkCondition(@NotNull JetScope scope, @Nullable JetExpression condition, ExpressionTypingContext context) {
        if (condition != null) {
            JetTypeInfo typeInfo = facade.getTypeInfo(condition, context.replaceScope(scope)
                    .replaceExpectedType(components.builtIns.getBooleanType()).replaceContextDependency(INDEPENDENT));
            JetType conditionType = typeInfo.getType();

            if (conditionType != null && !components.builtIns.isBooleanOrSubtype(conditionType)) {
                context.trace.report(TYPE_MISMATCH_IN_CONDITION.on(condition, conditionType));
            }

            return typeInfo.getDataFlowInfo();
        }
        return context.dataFlowInfo;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public JetTypeInfo visitIfExpression(@NotNull JetIfExpression expression, ExpressionTypingContext context) {
        return visitIfExpression(expression, context, false);
    }

    public JetTypeInfo visitIfExpression(JetIfExpression ifExpression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE);
        JetExpression condition = ifExpression.getCondition();
        DataFlowInfo conditionDataFlowInfo = checkCondition(context.scope, condition, context);

        JetExpression elseBranch = ifExpression.getElse();
        JetExpression thenBranch = ifExpression.getThen();

        WritableScopeImpl thenScope = newWritableScopeImpl(context, "Then scope");
        WritableScopeImpl elseScope = newWritableScopeImpl(context, "Else scope");
        DataFlowInfo thenInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, true, context).and(conditionDataFlowInfo);
        DataFlowInfo elseInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context).and(conditionDataFlowInfo);

        if (elseBranch == null) {
            if (thenBranch != null) {
                JetTypeInfo result = getTypeInfoWhenOnlyOneBranchIsPresent(
                        thenBranch, thenScope, thenInfo, elseInfo, contextWithExpectedType, ifExpression, isStatement);
                // If jump was possible, take condition check info as the jump info
                return result.getJumpOutPossible()
                       ? result.replaceJumpOutPossible(true).replaceJumpFlowInfo(conditionDataFlowInfo)
                       : result;
            }
            return TypeInfoFactoryPackage.createTypeInfo(components.dataFlowAnalyzer.checkImplicitCast(
                                                                 components.builtIns.getUnitType(), ifExpression,
                                                                 contextWithExpectedType, isStatement
                                                         ),
                                                         thenInfo.or(elseInfo)
            );
        }
        if (thenBranch == null) {
            return getTypeInfoWhenOnlyOneBranchIsPresent(
                    elseBranch, elseScope, elseInfo, thenInfo, contextWithExpectedType, ifExpression, isStatement);
        }
        JetPsiFactory psiFactory = JetPsiFactory(ifExpression);
        JetBlockExpression thenBlock = psiFactory.wrapInABlockWrapper(thenBranch);
        JetBlockExpression elseBlock = psiFactory.wrapInABlockWrapper(elseBranch);
        Call callForIf = createCallForSpecialConstruction(ifExpression, ifExpression, Lists.newArrayList(thenBlock, elseBlock));
        MutableDataFlowInfoForArguments dataFlowInfoForArguments =
                    createDataFlowInfoForArgumentsForIfCall(callForIf, thenInfo, elseInfo);
        ResolvedCall<FunctionDescriptor> resolvedCall = components.controlStructureTypingUtils.resolveSpecialConstructionAsCall(
                callForIf, "If", Lists.newArrayList("thenBranch", "elseBranch"),
                Lists.newArrayList(false, false),
                contextWithExpectedType, dataFlowInfoForArguments);

        BindingContext bindingContext = context.trace.getBindingContext();
        JetTypeInfo thenTypeInfo = BindingContextUtils.getRecordedTypeInfo(thenBranch, bindingContext);
        JetTypeInfo elseTypeInfo = BindingContextUtils.getRecordedTypeInfo(elseBranch, bindingContext);
        assert thenTypeInfo != null : "'Then' branch of if expression  was not processed: " + ifExpression;
        assert elseTypeInfo != null : "'Else' branch of if expression  was not processed: " + ifExpression;
        boolean loopBreakContinuePossible = thenTypeInfo.getJumpOutPossible() || elseTypeInfo.getJumpOutPossible();

        JetType thenType = thenTypeInfo.getType();
        JetType elseType = elseTypeInfo.getType();
        DataFlowInfo thenDataFlowInfo = thenTypeInfo.getDataFlowInfo();
        DataFlowInfo elseDataFlowInfo = elseTypeInfo.getDataFlowInfo();

        boolean jumpInThen = thenType != null && KotlinBuiltIns.isNothing(thenType);
        boolean jumpInElse = elseType != null && KotlinBuiltIns.isNothing(elseType);

        DataFlowInfo resultDataFlowInfo;
        if (thenType == null && elseType == null) {
            resultDataFlowInfo = thenDataFlowInfo.or(elseDataFlowInfo);
        }
        else if (thenType == null || (jumpInThen && !jumpInElse)) {
            resultDataFlowInfo = elseDataFlowInfo;
        }
        else if (elseType == null || (jumpInElse && !jumpInThen)) {
            resultDataFlowInfo = thenDataFlowInfo;
        }
        else {
            resultDataFlowInfo = thenDataFlowInfo.or(elseDataFlowInfo);
        }

        JetType resultType = resolvedCall.getResultingDescriptor().getReturnType();
        // If break or continue was possible, take condition check info as the jump info
        return TypeInfoFactoryPackage
                .createTypeInfo(components.dataFlowAnalyzer.checkImplicitCast(resultType, ifExpression, contextWithExpectedType, isStatement),
                                resultDataFlowInfo, loopBreakContinuePossible, conditionDataFlowInfo);
    }

    @NotNull
    private JetTypeInfo getTypeInfoWhenOnlyOneBranchIsPresent(
            @NotNull JetExpression presentBranch,
            @NotNull WritableScopeImpl presentScope,
            @NotNull DataFlowInfo presentInfo,
            @NotNull DataFlowInfo otherInfo,
            @NotNull ExpressionTypingContext context,
            @NotNull JetIfExpression ifExpression,
            boolean isStatement
    ) {
        ExpressionTypingContext newContext = context.replaceDataFlowInfo(presentInfo).replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceContextDependency(INDEPENDENT);
        JetTypeInfo typeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                presentScope, Collections.singletonList(presentBranch), CoercionStrategy.NO_COERCION, newContext);
        JetType type = typeInfo.getType();
        DataFlowInfo dataFlowInfo;
        if (type != null && KotlinBuiltIns.isNothing(type)) {
            dataFlowInfo = otherInfo;
        } else {
            dataFlowInfo = typeInfo.getDataFlowInfo().or(otherInfo);
        }
        return components.dataFlowAnalyzer.checkImplicitCast(
                components.dataFlowAnalyzer.checkType(
                        typeInfo.replaceType(components.builtIns.getUnitType()),
                        ifExpression,
                        context
                ),
                ifExpression,
                context,
                isStatement
        ).replaceDataFlowInfo(dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitWhileExpression(@NotNull JetWhileExpression expression, ExpressionTypingContext context) {
        return visitWhileExpression(expression, context, false);
    }

    public JetTypeInfo visitWhileExpression(JetWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return components.dataFlowAnalyzer.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(
                INDEPENDENT);
        // Preliminary analysis
        PreliminaryLoopVisitor loopVisitor = PreliminaryLoopVisitor.visitLoop(expression);
        context = context.replaceDataFlowInfo(
                loopVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo)
        );

        JetExpression condition = expression.getCondition();
        // Extract data flow info from condition itself without taking value into account
        DataFlowInfo dataFlowInfo = checkCondition(context.scope, condition, context);

        JetExpression body = expression.getBody();
        JetTypeInfo bodyTypeInfo;
        DataFlowInfo conditionInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, true, context).and(dataFlowInfo);
        if (body != null) {
            WritableScopeImpl scopeToExtend = newWritableScopeImpl(context, "Scope extended in while's condition");
            bodyTypeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                    scopeToExtend, Collections.singletonList(body),
                    CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(conditionInfo));
        }
        else {
            bodyTypeInfo = TypeInfoFactoryPackage.noTypeInfo(conditionInfo);
        }

        // Condition is false at this point only if there is no jumps outside
        if (!containsJumpOutOfLoop(expression, context)) {
            dataFlowInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context).and(dataFlowInfo);
        }

        // Special case: while (true)
        // In this case we must record data flow information at the nearest break / continue and
        // .and it with entrance data flow information, because while body until break is executed at least once in this case
        // See KT-6284
        if (body != null && JetPsiUtil.isTrueConstant(condition)) {
            // We should take data flow info from the first jump point,
            // but without affecting changing variables
            dataFlowInfo = dataFlowInfo.and(loopVisitor.clearDataFlowInfoForAssignedLocalVariables(bodyTypeInfo.getJumpFlowInfo()));
        }
        return components.dataFlowAnalyzer
                .checkType(bodyTypeInfo.replaceType(components.builtIns.getUnitType()), expression, contextWithExpectedType)
                .replaceDataFlowInfo(dataFlowInfo);
    }

    private boolean containsJumpOutOfLoop(final JetLoopExpression loopExpression, final ExpressionTypingContext context) {
        final boolean[] result = new boolean[1];
        result[0] = false;
        //todo breaks in inline function literals
        loopExpression.accept(new JetTreeVisitor<List<JetLoopExpression>>() {
            @Override
            public Void visitBreakExpression(@NotNull JetBreakExpression breakExpression, List<JetLoopExpression> outerLoops) {
                JetSimpleNameExpression targetLabel = breakExpression.getTargetLabel();
                PsiElement element = targetLabel != null ? context.trace.get(LABEL_TARGET, targetLabel) : null;
                if (element == loopExpression || (targetLabel == null && outerLoops.get(outerLoops.size() - 1) == loopExpression)) {
                    result[0] = true;
                }
                return null;
            }

            @Override
            public Void visitContinueExpression(@NotNull JetContinueExpression expression, List<JetLoopExpression> outerLoops) {
                // continue@someOuterLoop is also considered as break
                JetSimpleNameExpression targetLabel = expression.getTargetLabel();
                if (targetLabel != null) {
                    PsiElement element = context.trace.get(LABEL_TARGET, targetLabel);
                    if (element instanceof JetLoopExpression && !outerLoops.contains(element)) {
                        result[0] = true;
                    }
                }
                return null;
            }

            @Override
            public Void visitLoopExpression(@NotNull JetLoopExpression loopExpression, List<JetLoopExpression> outerLoops) {
                List<JetLoopExpression> newOuterLoops = Lists.newArrayList(outerLoops);
                newOuterLoops.add(loopExpression);
                return super.visitLoopExpression(loopExpression, newOuterLoops);
            }
        }, Lists.newArrayList(loopExpression));

        return result[0];
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(@NotNull JetDoWhileExpression expression, ExpressionTypingContext context) {
        return visitDoWhileExpression(expression, context, false);
    }

    public JetTypeInfo visitDoWhileExpression(JetDoWhileExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return components.dataFlowAnalyzer.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context =
                contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        JetExpression body = expression.getBody();
        JetScope conditionScope = context.scope;
        // Preliminary analysis
        PreliminaryLoopVisitor loopVisitor = PreliminaryLoopVisitor.visitLoop(expression);
        context = context.replaceDataFlowInfo(
                loopVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo)
        );
        // Here we must record data flow information at the end of the body (or at the first jump, to be precise) and
        // .and it with entrance data flow information, because do-while body is executed at least once
        // See KT-6283
        JetTypeInfo bodyTypeInfo;
        if (body instanceof JetFunctionLiteralExpression) {
            // As a matter of fact, function literal is always unused at this point
            bodyTypeInfo = facade.getTypeInfo(body, context.replaceScope(context.scope));
        }
        else if (body != null) {
            WritableScope writableScope = newWritableScopeImpl(context, "do..while body scope");
            conditionScope = writableScope;
            List<JetExpression> block;
            if (body instanceof JetBlockExpression) {
                block = ((JetBlockExpression)body).getStatements();
            }
            else {
                block = Collections.singletonList(body);
            }
            bodyTypeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(
                    writableScope, block, CoercionStrategy.NO_COERCION, context);
        }
        else {
            bodyTypeInfo = TypeInfoFactoryPackage.noTypeInfo(context);
        }
        JetExpression condition = expression.getCondition();
        DataFlowInfo conditionDataFlowInfo = checkCondition(conditionScope, condition, context);
        DataFlowInfo dataFlowInfo;
        // Without jumps out, condition is entered and false, with jumps out, we know nothing about it
        if (!containsJumpOutOfLoop(expression, context)) {
            dataFlowInfo = components.dataFlowAnalyzer.extractDataFlowInfoFromCondition(condition, false, context).and(conditionDataFlowInfo);
        }
        else {
            dataFlowInfo = context.dataFlowInfo;
        }
        // Here we must record data flow information at the end of the body (or at the first jump, to be precise) and
        // .and it with entrance data flow information, because do-while body is executed at least once
        // See KT-6283
        // NB: it's really important to do it for non-empty body which is not a function literal
        // If it's a function literal, it appears always unused so it's no matter what we do at this point
        if (body != null) {
            // We should take data flow info from the first jump point,
            // but without affecting changing variables
            dataFlowInfo = dataFlowInfo.and(loopVisitor.clearDataFlowInfoForAssignedLocalVariables(bodyTypeInfo.getJumpFlowInfo()));
        }
        return components.dataFlowAnalyzer
                .checkType(bodyTypeInfo.replaceType(components.builtIns.getUnitType()), expression, contextWithExpectedType)
                .replaceDataFlowInfo(dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitForExpression(@NotNull JetForExpression expression, ExpressionTypingContext context) {
        return visitForExpression(expression, context, false);
    }

    public JetTypeInfo visitForExpression(JetForExpression expression, ExpressionTypingContext contextWithExpectedType, boolean isStatement) {
        if (!isStatement) return components.dataFlowAnalyzer.illegalStatementType(expression, contextWithExpectedType, facade);

        ExpressionTypingContext context =
                contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT);
        // Preliminary analysis
        PreliminaryLoopVisitor loopVisitor = PreliminaryLoopVisitor.visitLoop(expression);
        context = context.replaceDataFlowInfo(loopVisitor.clearDataFlowInfoForAssignedLocalVariables(context.dataFlowInfo));

        JetExpression loopRange = expression.getLoopRange();
        JetType expectedParameterType = null;
        JetTypeInfo loopRangeInfo;
        if (loopRange != null) {
            ExpressionReceiver loopRangeReceiver = getExpressionReceiver(facade, loopRange, context.replaceScope(context.scope));
            loopRangeInfo = facade.getTypeInfo(loopRange, context);
            if (loopRangeReceiver != null) {
                expectedParameterType = components.forLoopConventionsChecker.checkIterableConvention(loopRangeReceiver, context);
            }
        }
        else {
            loopRangeInfo = TypeInfoFactoryPackage.noTypeInfo(context);
        }

        WritableScope loopScope = newWritableScopeImpl(context, "Scope with for-loop index");

        JetParameter loopParameter = expression.getLoopParameter();
        if (loopParameter != null) {
            VariableDescriptor variableDescriptor = createLoopParameterDescriptor(loopParameter, expectedParameterType, context);

            loopScope.addVariableDescriptor(variableDescriptor);
        }
        else {
            JetMultiDeclaration multiParameter = expression.getMultiParameter();
            if (multiParameter != null && loopRange != null) {
                JetType elementType = expectedParameterType == null ? ErrorUtils.createErrorType("Loop range has no type") : expectedParameterType;
                TransientReceiver iteratorNextAsReceiver = new TransientReceiver(elementType);
                components.annotationResolver.resolveAnnotationsWithArguments(loopScope, multiParameter.getModifierList(), context.trace);
                components.multiDeclarationResolver.defineLocalVariablesFromMultiDeclaration(
                        loopScope, multiParameter, iteratorNextAsReceiver, loopRange, context
                );
            }
        }

        JetExpression body = expression.getBody();
        JetTypeInfo bodyTypeInfo;
        if (body != null) {
            bodyTypeInfo = components.expressionTypingServices.getBlockReturnedTypeWithWritableScope(loopScope, Collections.singletonList(body),
                    CoercionStrategy.NO_COERCION, context.replaceDataFlowInfo(loopRangeInfo.getDataFlowInfo()));
        }
        else {
            bodyTypeInfo = loopRangeInfo;
        }

        return components.dataFlowAnalyzer
                .checkType(bodyTypeInfo.replaceType(components.builtIns.getUnitType()), expression, contextWithExpectedType)
                .replaceDataFlowInfo(loopRangeInfo.getDataFlowInfo());
    }

    private VariableDescriptor createLoopParameterDescriptor(
            JetParameter loopParameter,
            JetType expectedParameterType,
            ExpressionTypingContext context
    ) {
        components.modifiersChecker.withTrace(context.trace).checkParameterHasNoValOrVar(loopParameter, VAL_OR_VAR_ON_LOOP_PARAMETER);

        JetTypeReference typeReference = loopParameter.getTypeReference();
        VariableDescriptor variableDescriptor;
        if (typeReference != null) {
            variableDescriptor = components.descriptorResolver.resolveLocalVariableDescriptor(context.scope, loopParameter, context.trace);
            JetType actualParameterType = variableDescriptor.getType();
            if (expectedParameterType != null &&
                    !JetTypeChecker.DEFAULT.isSubtypeOf(expectedParameterType, actualParameterType)) {
                context.trace.report(TYPE_MISMATCH_IN_FOR_LOOP.on(typeReference, expectedParameterType, actualParameterType));
            }
        }
        else {
            if (expectedParameterType == null) {
                expectedParameterType = ErrorUtils.createErrorType("Error");
            }
            variableDescriptor = components.descriptorResolver.resolveLocalVariableDescriptor(loopParameter, expectedParameterType, context.trace, context.scope);
        }

        {
            // http://youtrack.jetbrains.net/issue/KT-527

            VariableDescriptor olderVariable = context.scope.getLocalVariable(variableDescriptor.getName());
            if (olderVariable != null && isLocal(context.scope.getContainingDeclaration(), olderVariable)) {
                PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor);
                context.trace.report(Errors.NAME_SHADOWING.on(declaration, variableDescriptor.getName().asString()));
            }
        }
        return variableDescriptor;
    }

    @Override
    public JetTypeInfo visitTryExpression(@NotNull JetTryExpression expression, ExpressionTypingContext typingContext) {
        ExpressionTypingContext context = typingContext.replaceContextDependency(INDEPENDENT);
        JetExpression tryBlock = expression.getTryBlock();
        List<JetCatchClause> catchClauses = expression.getCatchClauses();
        JetFinallySection finallyBlock = expression.getFinallyBlock();
        List<JetType> types = new ArrayList<JetType>();
        for (JetCatchClause catchClause : catchClauses) {
            JetParameter catchParameter = catchClause.getCatchParameter();
            JetExpression catchBody = catchClause.getCatchBody();
            if (catchParameter != null) {
                ModifiersChecker.ModifiersCheckingProcedure modifiersChecking = components.modifiersChecker.withTrace(context.trace);
                modifiersChecking.checkParameterHasNoValOrVar(catchParameter, VAL_OR_VAR_ON_CATCH_PARAMETER);
                ModifierCheckerCore.INSTANCE$.check(catchParameter, context.trace, null);

                VariableDescriptor variableDescriptor = components.descriptorResolver.resolveLocalVariableDescriptor(
                        context.scope, catchParameter, context.trace);
                JetType throwableType = components.builtIns.getThrowable().getDefaultType();
                components.dataFlowAnalyzer.checkType(variableDescriptor.getType(), catchParameter, context.replaceExpectedType(throwableType));
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

        JetTypeInfo result = TypeInfoFactoryPackage.noTypeInfo(context);
        if (finallyBlock != null) {
            result = facade.getTypeInfo(finallyBlock.getFinalExpression(),
                                        context.replaceExpectedType(NO_EXPECTED_TYPE));
        }

        JetType type = facade.getTypeInfo(tryBlock, context).getType();
        if (type != null) {
            types.add(type);
        }
        if (types.isEmpty()) {
            return result.clearType();
        }
        else {
            return result.replaceType(CommonSupertypes.commonSupertype(types));
        }
    }

    @Override
    public JetTypeInfo visitThrowExpression(@NotNull JetThrowExpression expression, ExpressionTypingContext context) {
        JetExpression thrownExpression = expression.getThrownExpression();
        if (thrownExpression != null) {
            JetType throwableType = components.builtIns.getThrowable().getDefaultType();
            facade.getTypeInfo(thrownExpression, context
                    .replaceExpectedType(throwableType).replaceScope(context.scope).replaceContextDependency(INDEPENDENT));
        }
        return components.dataFlowAnalyzer.createCheckedTypeInfo(components.builtIns.getNothingType(), context, expression);
    }

    @Override
    public JetTypeInfo visitReturnExpression(@NotNull JetReturnExpression expression, ExpressionTypingContext context) {
        JetElement labelTargetElement = LabelResolver.INSTANCE.resolveControlLabel(expression, context);

        JetExpression returnedExpression = expression.getReturnedExpression();

        JetType expectedType = NO_EXPECTED_TYPE;
        JetType resultType = components.builtIns.getNothingType();
        JetDeclaration parentDeclaration = PsiTreeUtil.getParentOfType(expression, JetDeclaration.class);

        if (parentDeclaration instanceof JetParameter) {
            // In a default value for parameter
            context.trace.report(RETURN_NOT_ALLOWED.on(expression));
        }

        if (expression.getTargetLabel() == null) {
            while (parentDeclaration instanceof JetMultiDeclaration) {
                //TODO: It's hacking fix for KT-5100: Strange "Return is not allowed here" for multi-declaration initializer with elvis expression
                parentDeclaration = PsiTreeUtil.getParentOfType(parentDeclaration, JetDeclaration.class);
            }

            assert parentDeclaration != null : "Can't find parent declaration for " + expression.getText();
            DeclarationDescriptor declarationDescriptor = context.trace.get(DECLARATION_TO_DESCRIPTOR, parentDeclaration);
            Pair<FunctionDescriptor, PsiElement> containingFunInfo =
                    BindingContextUtils.getContainingFunctionSkipFunctionLiterals(declarationDescriptor, false);
            FunctionDescriptor containingFunctionDescriptor = containingFunInfo.getFirst();

            if (containingFunctionDescriptor != null) {
                if (!InlineUtil.checkNonLocalReturnUsage(containingFunctionDescriptor, expression, context.trace) ||
                    isClassInitializer(containingFunInfo)) {
                    // Unqualified, in a function literal
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                    resultType = ErrorUtils.createErrorType(RETURN_NOT_ALLOWED_MESSAGE);
                }

                expectedType = getFunctionExpectedReturnType(containingFunctionDescriptor, (JetElement) containingFunInfo.getSecond(), context);
            }
            else {
                // Outside a function
                context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                resultType = ErrorUtils.createErrorType(RETURN_NOT_ALLOWED_MESSAGE);
            }
        }
        else if (labelTargetElement != null) {
            SimpleFunctionDescriptor functionDescriptor = context.trace.get(FUNCTION, labelTargetElement);
            if (functionDescriptor != null) {
                expectedType = getFunctionExpectedReturnType(functionDescriptor, labelTargetElement, context);
                if (!InlineUtil.checkNonLocalReturnUsage(functionDescriptor, expression, context.trace)) {
                    // Qualified, non-local
                    context.trace.report(RETURN_NOT_ALLOWED.on(expression));
                    resultType = ErrorUtils.createErrorType(RETURN_NOT_ALLOWED_MESSAGE);
                }
            }
            else {
                context.trace.report(NOT_A_RETURN_LABEL.on(expression, expression.getLabelName()));
            }
        }
        if (returnedExpression != null) {
            facade.getTypeInfo(returnedExpression, context.replaceExpectedType(expectedType).replaceScope(context.scope)
                    .replaceContextDependency(INDEPENDENT));
        }
        else {
            if (expectedType != null &&
                !noExpectedType(expectedType) &&
                !KotlinBuiltIns.isUnit(expectedType) &&
                !isDontCarePlaceholder(expectedType)) // for lambda with implicit return type Unit
            {
                context.trace.report(RETURN_TYPE_MISMATCH.on(expression, expectedType));
            }
        }
        return components.dataFlowAnalyzer.createCheckedTypeInfo(resultType, context, expression);
    }

    private static boolean isClassInitializer(@NotNull Pair<FunctionDescriptor, PsiElement> containingFunInfo) {
        return containingFunInfo.getFirst() instanceof ConstructorDescriptor &&
               !(containingFunInfo.getSecond() instanceof JetSecondaryConstructor);
    }

    @Override
    public JetTypeInfo visitBreakExpression(@NotNull JetBreakExpression expression, ExpressionTypingContext context) {
        LabelResolver.INSTANCE.resolveControlLabel(expression, context);
        return components.dataFlowAnalyzer.createCheckedTypeInfo(components.builtIns.getNothingType(), context, expression).
                replaceJumpOutPossible(true);
    }

    @Override
    public JetTypeInfo visitContinueExpression(@NotNull JetContinueExpression expression, ExpressionTypingContext context) {
        LabelResolver.INSTANCE.resolveControlLabel(expression, context);
        return components.dataFlowAnalyzer.createCheckedTypeInfo(components.builtIns.getNothingType(), context, expression).
                replaceJumpOutPossible(true);
    }

    @NotNull
    private static JetType getFunctionExpectedReturnType(
            @NotNull FunctionDescriptor descriptor,
            @NotNull JetElement function,
            @NotNull ExpressionTypingContext context
    ) {
        JetType expectedType;
        if (function instanceof JetSecondaryConstructor) {
            expectedType = getBuiltIns(descriptor).getUnitType();
        }
        else if (function instanceof JetFunction) {
            JetFunction jetFunction = (JetFunction) function;
            expectedType = context.trace.get(EXPECTED_RETURN_TYPE, jetFunction);

            if ((expectedType == null) && (jetFunction.getTypeReference() != null || jetFunction.hasBlockBody())) {
                expectedType = descriptor.getReturnType();
            }
        }
        else {
            expectedType = descriptor.getReturnType();
        }
        return expectedType != null ? expectedType : TypeUtils.NO_EXPECTED_TYPE;
    }
}
