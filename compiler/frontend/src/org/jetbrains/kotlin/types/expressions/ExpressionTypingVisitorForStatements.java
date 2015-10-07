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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.AnnotationResolver;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace;
import org.jetbrains.kotlin.resolve.calls.context.TemporaryTraceAndCache;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResultsUtil;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory;
import org.jetbrains.kotlin.resolve.scopes.LexicalWritableScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.utils.UtilsPackage;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryPackage;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.psi.JetPsiUtil.deparenthesize;
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
    private JetType checkAssignmentType(
            @Nullable JetType assignmentType,
            @NotNull JetBinaryExpression expression,
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
    public JetTypeInfo visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, ExpressionTypingContext context) {
        components.localClassifierAnalyzer.processClassOrObject(
                scope, context.replaceScope(scope).replaceContextDependency(INDEPENDENT),
                scope.getOwnerDescriptor(),
                declaration);
        return TypeInfoFactoryPackage.createTypeInfo(components.dataFlowAnalyzer.checkStatementType(declaration, context), context);
    }

    @Override
    public JetTypeInfo visitProperty(@NotNull JetProperty property, ExpressionTypingContext typingContext) {
        ExpressionTypingContext context = typingContext.replaceContextDependency(INDEPENDENT).replaceScope(scope);
        JetTypeReference receiverTypeRef = property.getReceiverTypeReference();
        if (receiverTypeRef != null) {
            context.trace.report(LOCAL_EXTENSION_PROPERTY.on(receiverTypeRef));
        }

        JetPropertyAccessor getter = property.getGetter();
        if (getter != null) {
            context.trace.report(LOCAL_VARIABLE_WITH_GETTER.on(getter));
        }

        JetPropertyAccessor setter = property.getSetter();
        if (setter != null) {
            context.trace.report(LOCAL_VARIABLE_WITH_SETTER.on(setter));
        }

        JetExpression delegateExpression = property.getDelegateExpression();
        if (delegateExpression != null) {
            components.expressionTypingServices.getTypeInfo(delegateExpression, context);
            context.trace.report(LOCAL_VARIABLE_WITH_DELEGATE.on(property.getDelegate()));
        }

        for (JetTypeParameter typeParameter : property.getTypeParameters()) {
            AnnotationResolver.reportUnsupportedAnnotationForTypeParameter(typeParameter, context.trace);
        }

        VariableDescriptor propertyDescriptor = components.descriptorResolver.
                resolveLocalVariableDescriptor(scope, property, context.dataFlowInfo, context.trace);
        JetExpression initializer = property.getInitializer();
        JetTypeInfo typeInfo;
        if (initializer != null) {
            JetType outType = propertyDescriptor.getType();
            typeInfo = facade.getTypeInfo(initializer, context.replaceExpectedType(outType));
            DataFlowInfo dataFlowInfo = typeInfo.getDataFlowInfo();
            JetType type = typeInfo.getType();
            // At this moment we do not take initializer value into account if type is given for a property
            // We can comment first part of this condition to take them into account, like here: var s: String? = "xyz"
            // In this case s will be not-nullable until it is changed
            if (property.getTypeReference() == null && type != null) {
                DataFlowValue variableDataFlowValue = DataFlowValueFactory.createDataFlowValue(
                        propertyDescriptor, context.trace.getBindingContext(),
                        DescriptorUtils.getContainingModuleOrNull(scope.getOwnerDescriptor()));
                DataFlowValue initializerDataFlowValue = DataFlowValueFactory.createDataFlowValue(initializer, type, context);
                // We cannot say here anything new about initializerDataFlowValue
                // except it has the same value as variableDataFlowValue
                typeInfo = typeInfo.replaceDataFlowInfo(dataFlowInfo.assign(variableDataFlowValue, initializerDataFlowValue));
            }
        }
        else {
            typeInfo = TypeInfoFactoryPackage.noTypeInfo(context);
        }

        {
            VariableDescriptor olderVariable = UtilsPackage.getLocalVariable(scope, propertyDescriptor.getName());
            ExpressionTypingUtils.checkVariableShadowing(context, propertyDescriptor, olderVariable);
        }

        scope.addVariableDescriptor(propertyDescriptor);
        components.modifiersChecker.withTrace(context.trace).checkModifiersForLocalDeclaration(property, propertyDescriptor);
        return typeInfo.replaceType(components.dataFlowAnalyzer.checkStatementType(property, context));
    }

    @Override
    public JetTypeInfo visitMultiDeclaration(@NotNull JetMultiDeclaration multiDeclaration, ExpressionTypingContext context) {
        components.annotationResolver.resolveAnnotationsWithArguments(scope, multiDeclaration.getModifierList(), context.trace);

        JetExpression initializer = multiDeclaration.getInitializer();
        if (initializer == null) {
            context.trace.report(INITIALIZER_REQUIRED_FOR_MULTIDECLARATION.on(multiDeclaration));
            return TypeInfoFactoryPackage.noTypeInfo(context);
        }
        ExpressionReceiver expressionReceiver = ExpressionTypingUtils.getExpressionReceiver(
                facade, initializer, context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(INDEPENDENT));
        JetTypeInfo typeInfo = facade.getTypeInfo(initializer, context);
        if (expressionReceiver == null) {
            return TypeInfoFactoryPackage.noTypeInfo(context);
        }
        components.multiDeclarationResolver.defineLocalVariablesFromMultiDeclaration(scope, multiDeclaration, expressionReceiver, initializer, context);
        components.modifiersChecker.withTrace(context.trace).checkModifiersForMultiDeclaration(multiDeclaration);
        return typeInfo.replaceType(components.dataFlowAnalyzer.checkStatementType(multiDeclaration, context));
    }

    @Override
    public JetTypeInfo visitNamedFunction(@NotNull JetNamedFunction function, ExpressionTypingContext context) {
        return functions.visitNamedFunction(function, context, true, scope);
    }

    @Override
    public JetTypeInfo visitClass(@NotNull JetClass klass, ExpressionTypingContext context) {
        components.localClassifierAnalyzer.processClassOrObject(
                scope, context.replaceScope(scope).replaceContextDependency(INDEPENDENT),
                scope.getOwnerDescriptor(),
                klass);
        return TypeInfoFactoryPackage.createTypeInfo(components.dataFlowAnalyzer.checkStatementType(klass, context), context);
    }

    @Override
    public JetTypeInfo visitTypedef(@NotNull JetTypedef typedef, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(typedef, "Typedefs are not supported"));
        return super.visitTypedef(typedef, context);
    }

    @Override
    public JetTypeInfo visitDeclaration(@NotNull JetDeclaration dcl, ExpressionTypingContext context) {
        return TypeInfoFactoryPackage.createTypeInfo(components.dataFlowAnalyzer.checkStatementType(dcl, context), context);
    }

    @Override
    public JetTypeInfo visitBinaryExpression(@NotNull JetBinaryExpression expression, ExpressionTypingContext context) {
        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        JetTypeInfo result;
        if (operationType == JetTokens.EQ) {
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
    protected JetTypeInfo visitAssignmentOperation(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        //There is a temporary binding trace for an opportunity to resolve set method for array if needed (the initial trace should be used there)
        TemporaryTraceAndCache temporary = TemporaryTraceAndCache.create(
                contextWithExpectedType, "trace to resolve array set method for binary expression", expression);
        ExpressionTypingContext context = contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE)
                .replaceTraceAndCache(temporary).replaceContextDependency(INDEPENDENT);

        JetSimpleNameExpression operationSign = expression.getOperationReference();
        IElementType operationType = operationSign.getReferencedNameElementType();
        JetExpression leftOperand = expression.getLeft();
        JetTypeInfo leftInfo = ExpressionTypingUtils.getTypeInfoOrNullType(leftOperand, context, facade);
        JetType leftType = leftInfo.getType();

        JetExpression right = expression.getRight();
        JetExpression left = leftOperand == null ? null : deparenthesize(leftOperand);
        if (right == null || left == null) {
            temporary.commit();
            return leftInfo.clearType();
        }

        if (leftType == null) {
            JetTypeInfo rightInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(leftInfo.getDataFlowInfo()));
            context.trace.report(UNRESOLVED_REFERENCE.on(operationSign, operationSign));
            temporary.commit();
            return rightInfo.clearType();
        }
        ExpressionReceiver receiver = new ExpressionReceiver(left, leftType);

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
        JetType assignmentOperationType = OverloadResolutionResultsUtil.getResultingType(assignmentOperationDescriptors,
                                                                                         context.contextDependency);

        OverloadResolutionResults<FunctionDescriptor> binaryOperationDescriptors;
        JetType binaryOperationType;
        TemporaryTraceAndCache temporaryForBinaryOperation = TemporaryTraceAndCache.create(
                context, "trace to check binary operation like '+' for", expression);
        TemporaryBindingTrace ignoreReportsTrace = TemporaryBindingTrace.create(context.trace, "Trace for checking assignability");
        boolean lhsAssignable = basic.checkLValue(ignoreReportsTrace, context, left, right);
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

        JetType type = assignmentOperationType != null ? assignmentOperationType : binaryOperationType;
        JetTypeInfo rightInfo = leftInfo;
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
            if (!JetTypeChecker.DEFAULT.equalTypes(components.builtIns.getUnitType(), assignmentOperationType)) {
                context.trace.report(ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT.on(operationSign, assignmentOperationDescriptors.getResultingDescriptor(), operationSign));
            }
        }
        else {
            // There's only 'plus()', so we try 'a = a + b'
            temporaryForBinaryOperation.commit();
            context.trace.record(VARIABLE_REASSIGNMENT, expression);
            if (left instanceof JetArrayAccessExpression) {
                ExpressionTypingContext contextForResolve = context.replaceScope(scope).replaceBindingTrace(TemporaryBindingTrace.create(
                        context.trace, "trace to resolve array set method for assignment", expression));
                basic.resolveArrayAccessSetMethod((JetArrayAccessExpression) left, right, contextForResolve, context.trace);
            }
            rightInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(leftInfo.getDataFlowInfo()));
            components.dataFlowAnalyzer.checkType(binaryOperationType, expression, context.replaceExpectedType(leftType).replaceDataFlowInfo(rightInfo.getDataFlowInfo()));
            basic.checkLValue(context.trace, context, leftOperand, right);
        }
        temporary.commit();
        return rightInfo.replaceType(checkAssignmentType(type, expression, contextWithExpectedType));
    }

    @NotNull
    protected JetTypeInfo visitAssignment(JetBinaryExpression expression, ExpressionTypingContext contextWithExpectedType) {
        final ExpressionTypingContext context =
                contextWithExpectedType.replaceExpectedType(NO_EXPECTED_TYPE).replaceScope(scope).replaceContextDependency(INDEPENDENT);
        JetExpression leftOperand = expression.getLeft();
        if (leftOperand instanceof JetAnnotatedExpression) {
            // We will lose all annotations during deparenthesizing, so we have to resolve them right now
            components.annotationResolver.resolveAnnotationsWithArguments(
                    scope, ((JetAnnotatedExpression) leftOperand).getAnnotationEntries(), context.trace
            );
        }
        JetExpression left = deparenthesize(leftOperand);
        JetExpression right = expression.getRight();
        if (left instanceof JetArrayAccessExpression) {
            JetArrayAccessExpression arrayAccessExpression = (JetArrayAccessExpression) left;
            if (right == null) return TypeInfoFactoryPackage.noTypeInfo(context);
            JetTypeInfo typeInfo = basic.resolveArrayAccessSetMethod(arrayAccessExpression, right, context, context.trace);
            basic.checkLValue(context.trace, context, arrayAccessExpression, right);
            return typeInfo.replaceType(checkAssignmentType(typeInfo.getType(), expression, contextWithExpectedType));
        }
        JetTypeInfo leftInfo = ExpressionTypingUtils.getTypeInfoOrNullType(left, context, facade);
        JetType leftType = leftInfo.getType();
        DataFlowInfo dataFlowInfo = leftInfo.getDataFlowInfo();
        JetTypeInfo resultInfo;
        if (right != null) {
            resultInfo = facade.getTypeInfo(right, context.replaceDataFlowInfo(dataFlowInfo).replaceExpectedType(leftType));
            dataFlowInfo = resultInfo.getDataFlowInfo();
            JetType rightType = resultInfo.getType();
            if (left != null && leftType != null && rightType != null) {
                DataFlowValue leftValue = DataFlowValueFactory.createDataFlowValue(left, leftType, context);
                DataFlowValue rightValue = DataFlowValueFactory.createDataFlowValue(right, rightType, context);
                // We cannot say here anything new about rightValue except it has the same value as leftValue
                resultInfo = resultInfo.replaceDataFlowInfo(dataFlowInfo.assign(leftValue, rightValue));
            }
        }
        else {
            resultInfo = leftInfo;
        }
        if (leftType != null && leftOperand != null) { //if leftType == null, some other error has been generated
            basic.checkLValue(context.trace, context, leftOperand, right);
        }
        return resultInfo.replaceType(components.dataFlowAnalyzer.checkStatementType(expression, contextWithExpectedType));
    }


    @Override
    public JetTypeInfo visitExpression(@NotNull JetExpression expression, ExpressionTypingContext context) {
        return facade.getTypeInfo(expression, context);
    }

    @Override
    public JetTypeInfo visitJetElement(@NotNull JetElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, "in a block"));
        return TypeInfoFactoryPackage.noTypeInfo(context);
    }

    @Override
    public JetTypeInfo visitWhileExpression(@NotNull JetWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitWhileExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitDoWhileExpression(@NotNull JetDoWhileExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitDoWhileExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitForExpression(@NotNull JetForExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitForExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitAnnotatedExpression(
            @NotNull JetAnnotatedExpression expression, ExpressionTypingContext data
    ) {
        return basic.visitAnnotatedExpression(expression, data, true);
    }

    @Override
    public JetTypeInfo visitIfExpression(@NotNull JetIfExpression expression, ExpressionTypingContext context) {
        return controlStructures.visitIfExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitWhenExpression(@NotNull JetWhenExpression expression, ExpressionTypingContext context) {
        return patterns.visitWhenExpression(expression, context, true);
    }

    @Override
    public JetTypeInfo visitBlockExpression(@NotNull JetBlockExpression expression, ExpressionTypingContext context) {
        return components.expressionTypingServices.getBlockReturnedType(expression, context, true);
    }

    @Override
    public JetTypeInfo visitLabeledExpression(@NotNull JetLabeledExpression expression, ExpressionTypingContext context) {
        return basic.visitLabeledExpression(expression, context, true);
    }
}
