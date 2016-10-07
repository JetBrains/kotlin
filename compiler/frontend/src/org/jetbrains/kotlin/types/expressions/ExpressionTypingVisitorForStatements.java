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

import com.google.common.collect.Sets;
import com.intellij.psi.tree.IElementType;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.calls.context.CallPosition;
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize;
import static org.jetbrains.kotlin.resolve.BindingContext.AMBIGUOUS_REFERENCE_TARGET;
import static org.jetbrains.kotlin.resolve.BindingContext.VARIABLE_REASSIGNMENT;
import static org.jetbrains.kotlin.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.kotlin.types.TypeUtils.noExpectedType;

@SuppressWarnings("SuspiciousMethodCalls")
public class ExpressionTypingVisitorForStatements extends ExpressionTypingVisitor {
    private final LexicalWritableScope scope;
    private final BasicExpressionTypingVisitor basic;
    private final ControlStructureTypingVisitor controlStructures;
    private final PatternMatchingTypingVisitor patterns;
    private final FunctionsTypingVisitor functions;

    public ExpressionTypingVisitorForStatements(
            @NotNull ExpressionTypingInternals facade,
            @NotNull LexicalWritableScope scope,
            @NotNull BasicExpressionTypingVisitor basic,
            @NotNull ControlStructureTypingVisitor controlStructures,
            @NotNull PatternMatchingTypingVisitor patterns,
            @NotNull FunctionsTypingVisitor functions
    ) {
        super(facade);
        this.scope = scope;
        this.basic = basic;
        this.controlStructures = controlStructures;
        this.patterns = patterns;
        this.functions = functions;
    }

    @Nullable
    private KotlinType checkAssignmentType(
            @Nullable KotlinType assignmentType,
            @NotNull KtBinaryExpression expression,
            @NotNull ExpressionTypingContext context
    ) {
        if (assignmentType != null && !KotlinBuiltIns.isUnit(assignmentType) && !noExpectedType(context.expectedType) &&
            !context.expectedType.isError() && TypeUtils.equalTypes(context.expectedType, assignmentType)) {
            context.trace.report(Errors.ASSIGNMENT_TYPE_MISMATCH.on(expression, context.expectedType));
            return null;
        }
        return components.dataFlowAnalyzer.checkStatementType(expression, context);
    }

    @Override
    public KotlinTypeInfo visitObjectDeclaration(@NotNull KtObjectDeclaration declaration, ExpressionTypingContext context) {
        components.localClassifierAnalyzer.processClassOrObject(
                scope, context.replaceScope(scope).replaceContextDependency(INDEPENDENT),
                scope.getOwnerDescriptor(),
                declaration);
        return TypeInfoFactoryKt.createTypeInfo(components.dataFlowAnalyzer.checkStatementType(declaration, context), context);
    }

    @Override
    public KotlinTypeInfo visitProperty(@NotNull KtProperty property, ExpressionTypingContext typingContext) {
        Pair<KotlinTypeInfo, VariableDescriptor> typeInfoAndVariableDescriptor = components.localVariableResolver.process(property, typingContext, scope, facade);
        scope.addVariableDescriptor(typeInfoAndVariableDescriptor.getSecond());
        return typeInfoAndVariableDescriptor.getFirst();
    }

    @Override
    public KotlinTypeInfo visitTypeAlias(@NotNull KtTypeAlias typeAlias, ExpressionTypingContext context) {
        TypeAliasDescriptor typeAliasDescriptor = components.descriptorResolver.resolveTypeAliasDescriptor(
                context.scope.getOwnerDescriptor(), context.scope, typeAlias, context.trace);
        scope.addClassifierDescriptor(typeAliasDescriptor);
        ForceResolveUtil.forceResolveAllContents(typeAliasDescriptor);

        return TypeInfoFactoryKt.createTypeInfo(components.dataFlowAnalyzer.checkStatementType(typeAlias, context), context);
    }

    @Override
    public KotlinTypeInfo visitDestructuringDeclaration(@NotNull KtDestructuringDeclaration multiDeclaration, ExpressionTypingContext context) {
        components.annotationResolver.resolveAnnotationsWithArguments(scope, multiDeclaration.getModifierList(), context.trace);

        KtExpression initializer = multiDeclaration.getInitializer();
        if (initializer == null) {
            context.trace.report(INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION.on(multiDeclaration));
        }

        ExpressionReceiver expressionReceiver = initializer != null ? ExpressionTypingUtils.getExpressionReceiver(
                facade, initializer, context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT)) : null;

        components.destructuringDeclarationResolver
                .defineLocalVariablesFromDestructuringDeclaration(scope, multiDeclaration, expressionReceiver, initializer, context);
        components.modifiersChecker.withTrace(context.trace).checkModifiersForDestructuringDeclaration(multiDeclaration);
        components.identifierChecker.checkDeclaration(multiDeclaration, context.trace);

        if (expressionReceiver == null) {
            return TypeInfoFactoryKt.noTypeInfo(context);
        }
        else {
            return facade.getTypeInfo(initializer, context)
                    .replaceType(components.dataFlowAnalyzer.checkStatementType(multiDeclaration, context));
        }
    }

    @Override
    public KotlinTypeInfo visitNamedFunction(@NotNull KtNamedFunction function, ExpressionTypingContext context) {
        return functions.visitNamedFunction(function, context, /* isDeclaration = */ function.getName() != null, scope);
    }

    @Override
    public KotlinTypeInfo visitClass(@NotNull KtClass klass, ExpressionTypingContext context) {
        components.localClassifierAnalyzer.processClassOrObject(
                scope, context.replaceScope(scope).replaceContextDependency(INDEPENDENT),
                scope.getOwnerDescriptor(),
                klass);
        return TypeInfoFactoryKt.createTypeInfo(components.dataFlowAnalyzer.checkStatementType(klass, context), context);
    }

    @Override
    public KotlinTypeInfo visitDeclaration(@NotNull KtDeclaration dcl, ExpressionTypingContext context) {
        return TypeInfoFactoryKt.createTypeInfo(components.dataFlowAnalyzer.checkStatementType(dcl, context), context);
    }

    @Override
    public KotlinTypeInfo visitBinaryExpression(@NotNull KtBinaryExpression expression, ExpressionTypingContext context) {
        KtSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        KotlinTypeInfo result;
        if (operationType == KtTokens.EQ) {
            result = visitAssignment(expression, context);
        }
        else if (OperatorConventions.ASSIGNMENT_OPERATIONS.containsKey(operationType)) {
            result = visitAssignmentOperation(expression, context);
        }
        else {
            return facade.getTypeInfo(expression, context);
        }
        return components.dataFlowAnalyzer.checkType(result, expression, context);
    }

    @NotNull
    protected KotlinTypeInfo visitAssignmentOperation(KtBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        //There is a temporary binding trace for an opportunity to resolve set method for array if needed (the initial trace should be used there)
        TemporaryTraceAndCache temporary = TemporaryTraceAndCache.create(
                contextWithExpectedType, "trace to resolve array set method for binary expression", expression);
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceTraceAndCache(temporary).replaceContextDependency(INDEPENDENT);

        KtSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        KtExpression leftOperand = expression.getLeft();
        KotlinTypeInfo leftInfo = ExpressionTypingUtils.getTypeInfoOrNullType(leftOperand, context, facade);
        KotlinType leftType = leftInfo.getType();

        KtExpression right = expression.getRight();
        KtExpression left = leftOperand == null ? null : deparenthesize(leftOperand);
        if (right == null || left == null) {
            temporary.commit();
            return leftInfo.clearType();
        }

        if (leftType == null) {
            KotlinTypeInfo rightInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(leftInfo.getDataFlowInfo()));
            context.trace.report(UNRESOLVED_REFERENCE.on(operationSign, operationSign));
            temporary.commit();
            return rightInfo.clearType();
        }
        ExpressionReceiver receiver = ExpressionReceiver.Companion.create(left, leftType, context.trace.getBindingContext());

        // We check that defined only one of '+=' and '+' operations, and call it (in the case '+' we then also assign)
        // Check for '+='
        Name name = OperatorConventions.ASSIGNMENT_OPERATIONS.get(operationType);
        TemporaryTraceAndCache temporaryForAssignmentOperation = TemporaryTraceAndCache.create(
                context, "trace to check assignment operation like '+=' for", expression);
        OverloadResolutionResults<FunctionDescriptor> assignmentOperationDescriptors =
                components.callResolver.resolveBinaryCall(
                        context.replaceTraceAndCache(temporaryForAssignmentOperation).replaceScope(scope),
                        receiver, expression, name
                );
        KotlinType assignmentOperationType = OverloadResolutionResultsUtil.getResultingType(assignmentOperationDescriptors,
                                                                                            context.contextDependency);

        OverloadResolutionResults<FunctionDescriptor> binaryOperationDescriptors;
        KotlinType binaryOperationType;
        TemporaryTraceAndCache temporaryForBinaryOperation = TemporaryTraceAndCache.create(
                context, "trace to check binary operation like '+' for", expression);
        TemporaryBindingTrace ignoreReportsTrace = TemporaryBindingTrace.create(context.trace, "Trace for checking assignability");
        boolean lhsAssignable = basic.checkLValue(ignoreReportsTrace, context, left, right, expression);
        if (assignmentOperationType == null || lhsAssignable) {
            // Check for '+'
            Name counterpartName = OperatorConventions.BINARY_OPERATION_NAMES.get(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.get(operationType));
            binaryOperationDescriptors = components.callResolver.resolveBinaryCall(
                    context.replaceTraceAndCache(temporaryForBinaryOperation).replaceScope(scope),
                    receiver, expression, counterpartName
            );

            binaryOperationType = OverloadResolutionResultsUtil.getResultingType(binaryOperationDescriptors, context.contextDependency);
        }
        else {
            binaryOperationDescriptors = OverloadResolutionResultsImpl.nameNotFound();
            binaryOperationType = null;
        }

        KotlinType type = assignmentOperationType != null ? assignmentOperationType : binaryOperationType;
        KotlinTypeInfo rightInfo = leftInfo;
        if (assignmentOperationDescriptors.isSuccess() && binaryOperationDescriptors.isSuccess()) {
            // Both 'plus()' and 'plusAssign()' available => ambiguity
            OverloadResolutionResults<FunctionDescriptor> ambiguityResolutionResults = OverloadResolutionResultsUtil.ambiguity(assignmentOperationDescriptors, binaryOperationDescriptors);
            context.trace.report(ASSIGN_OPERATOR_AMBIGUITY.on(operationSign, ambiguityResolutionResults.getResultingCalls()));
            Collection<DeclarationDescriptor> descriptors = Sets.newHashSet();
            for (ResolvedCall<?> resolvedCall : ambiguityResolutionResults.getResultingCalls()) {
                descriptors.add(resolvedCall.getResultingDescriptor());
            }
            rightInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(leftInfo.getDataFlowInfo()));
            context.trace.record(AMBIGUOUS_REFERENCE_TARGET, operationSign, descriptors);
        }
        else if (assignmentOperationType != null && (assignmentOperationDescriptors.isSuccess() || !binaryOperationDescriptors.isSuccess())) {
            // There's 'plusAssign()', so we do a.plusAssign(b)
            temporaryForAssignmentOperation.commit();
            if (!KotlinTypeChecker.DEFAULT.equalTypes(components.builtIns.getUnitType(), assignmentOperationType)) {
                context.trace.report(ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT.on(operationSign, assignmentOperationDescriptors.getResultingDescriptor(), operationSign));
            }
        }
        else {
            // There's only 'plus()', so we try 'a = a + b'
            temporaryForBinaryOperation.commit();
            context.trace.record(VARIABLE_REASSIGNMENT, expression);
            if (left instanceof KtArrayAccessExpression) {
                ExpressionTypingContext contextForResolve = context.replaceScope(scope).replaceBindingTrace(TemporaryBindingTrace.create(
                        context.trace, "trace to resolve array set method for assignment", expression));
                basic.resolveImplicitArrayAccessSetMethod((KtArrayAccessExpression) left, right, contextForResolve, context.trace);
            }
            rightInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(leftInfo.getDataFlowInfo()));

            KotlinType expectedType = refineTypeFromPropertySetterIfPossible(context.trace.getBindingContext(), leftOperand, leftType);

            components.dataFlowAnalyzer.checkType(binaryOperationType, expression, context.replaceExpectedType(expectedType)
                    .replaceDataFlowInfo(rightInfo.getDataFlowInfo()).replaceCallPosition(new CallPosition.PropertyAssignment(left)));
            basic.checkLValue(context.trace, context, leftOperand, right, expression);
        }
        temporary.commit();
        return rightInfo.replaceType(checkAssignmentType(type, expression, contextWithExpectedType));
    }

    @Nullable
    private static KotlinType refineTypeFromPropertySetterIfPossible(
            @NotNull BindingContext bindingContext,
            @Nullable KtElement leftOperand,
            @Nullable KotlinType leftOperandType
    ) {
        VariableDescriptor descriptor = BindingContextUtils.extractVariableFromResolvedCall(bindingContext, leftOperand);

        if (descriptor instanceof PropertyDescriptor) {
            PropertySetterDescriptor setter = ((PropertyDescriptor) descriptor).getSetter();
            if (setter != null) return setter.getValueParameters().get(0).getType();
        }

        return leftOperandType;
    }

    @NotNull
    protected KotlinTypeInfo visitAssignment(KtBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        ExpressionTypingContext context =
                contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceScope(scope).replaceContextDependency(INDEPENDENT);
        KtExpression leftOperand = expression.getLeft();
        if (leftOperand instanceof KtAnnotatedExpression) {
            basic.resolveAnnotationsOnExpression((KtAnnotatedExpression) leftOperand, context);
        }
        KtExpression left = deparenthesize(leftOperand);
        KtExpression right = expression.getRight();
        if (left instanceof KtArrayAccessExpression) {
            KtArrayAccessExpression arrayAccessExpression = (KtArrayAccessExpression) left;
            if (right == null) return TypeInfoFactoryKt.noTypeInfo(context);
            KotlinTypeInfo typeInfo = basic.resolveArrayAccessSetMethod(arrayAccessExpression, right, context, context.trace);
            basic.checkLValue(context.trace, context, arrayAccessExpression, right, expression);
            return typeInfo.replaceType(checkAssignmentType(typeInfo.getType(), expression, contextWithExpectedType));
        }
        KotlinTypeInfo leftInfo = ExpressionTypingUtils.getTypeInfoOrNullType(left, context, facade);
        KotlinType expectedType = refineTypeFromPropertySetterIfPossible(context.trace.getBindingContext(), leftOperand, leftInfo.getType());
        DataFlowInfo dataFlowInfo = leftInfo.getDataFlowInfo();
        KotlinTypeInfo resultInfo;
        if (right != null) {
            resultInfo = facade.getTypeInfo(
                            right, context.replaceDataFlowInfo(dataFlowInfo).replaceExpectedType(expectedType).replaceCallPosition(
                                    new CallPosition.PropertyAssignment(leftOperand)));

            dataFlowInfo = resultInfo.getDataFlowInfo();
            KotlinType rightType = resultInfo.getType();
            if (left != null && expectedType != null && rightType != null) {
                DataFlowValue leftValue = DataFlowValueFactory.createDataFlowValue(left, expectedType, context);
                DataFlowValue rightValue = DataFlowValueFactory.createDataFlowValue(right, rightType, context);
                // We cannot say here anything new about rightValue except it has the same value as leftValue
                resultInfo = resultInfo.replaceDataFlowInfo(dataFlowInfo.assign(leftValue, rightValue));
            }
        }
        else {
            resultInfo = leftInfo;
        }
        if (expectedType != null && leftOperand != null) { //if expectedType == null, some other error has been generated
            basic.checkLValue(context.trace, context, leftOperand, right, expression);
        }
        return resultInfo.replaceType(components.dataFlowAnalyzer.checkStatementType(expression, contextWithExpectedType));
    }


    @Override
    public KotlinTypeInfo visitExpression(@NotNull KtExpression expression, ExpressionTypingContext context) {
        return facade.getTypeInfo(expression, context);
    }

    @Override
    public KotlinTypeInfo visitKtElement(@NotNull KtElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, "in a block"));
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @Override
    public KotlinTypeInfo visitWhileExpression(@NotNull KtWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitWhileExpression(expression, context, true);
    }

    @Override
    public KotlinTypeInfo visitDoWhileExpression(@NotNull KtDoWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitDoWhileExpression(expression, context, true);
    }

    @Override
    public KotlinTypeInfo visitForExpression(@NotNull KtForExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitForExpression(expression, context, true);
    }

    @Override
    public KotlinTypeInfo visitAnnotatedExpression(
            @NotNull KtAnnotatedExpression expression, ExpressionTypingContext data
    ) {
        return basic.visitAnnotatedExpression(expression, data, true);
    }

    @Override
    public KotlinTypeInfo visitIfExpression(@NotNull KtIfExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitIfExpression(expression, context, true);
    }

    @Override
    public KotlinTypeInfo visitWhenExpression(@NotNull KtWhenExpression expression, ExpressionTypingContext context) {
        return patterns.visitWhenExpression(expression, context, true);
    }

    @Override
    public KotlinTypeInfo visitBlockExpression(@NotNull KtBlockExpression expression, ExpressionTypingContext context) {
        return components.expressionTypingServices.getBlockReturnedType(expression, context, true);
    }

    @Override
    public KotlinTypeInfo visitLabeledExpression(@NotNull KtLabeledExpression expression, ExpressionTypingContext context) {
        return basic.visitLabeledExpression(expression, context, true);
    }
}
