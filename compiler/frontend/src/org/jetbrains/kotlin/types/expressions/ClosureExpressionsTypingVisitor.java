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
import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.*;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.source.SourcePackage.toSourceElement;
import static org.jetbrains.kotlin.types.TypeUtils.*;
import static org.jetbrains.kotlin.types.expressions.CoercionStrategy.COERCION_TO_UNIT;

public class ClosureExpressionsTypingVisitor extends ExpressionTypingVisitor {

    protected ClosureExpressionsTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public JetTypeInfo visitFunctionLiteralExpression(@NotNull JetFunctionLiteralExpression expression, ExpressionTypingContext context) {
        if (!expression.getFunctionLiteral().hasBody()) return null;

        JetType expectedType = context.expectedType;
        boolean functionTypeExpected = !noExpectedType(expectedType) && KotlinBuiltIns.isFunctionOrExtensionFunctionType(
                expectedType);

        AnonymousFunctionDescriptor functionDescriptor = createFunctionDescriptor(expression, context, functionTypeExpected);
        JetType safeReturnType = computeReturnType(expression, context, functionDescriptor, functionTypeExpected);
        functionDescriptor.setReturnType(safeReturnType);

        JetType receiver = DescriptorUtils.getReceiverParameterType(functionDescriptor.getExtensionReceiverParameter());
        List<JetType> valueParametersTypes = ExpressionTypingUtils.getValueParametersTypes(functionDescriptor.getValueParameters());
        JetType resultType = components.builtIns.getFunctionType(
                Annotations.EMPTY, receiver, valueParametersTypes, safeReturnType);
        if (!noExpectedType(expectedType) && KotlinBuiltIns.isFunctionOrExtensionFunctionType(expectedType)) {
            // all checks were done before
            return JetTypeInfo.create(resultType, context.dataFlowInfo);
        }

        return DataFlowUtils.checkType(resultType, expression, context, context.dataFlowInfo);
    }

    @NotNull
    private AnonymousFunctionDescriptor createFunctionDescriptor(
            @NotNull JetFunctionLiteralExpression expression,
            @NotNull ExpressionTypingContext context,
            boolean functionTypeExpected
    ) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        JetTypeReference receiverTypeRef = functionLiteral.getReceiverTypeReference();
        AnonymousFunctionDescriptor functionDescriptor = new AnonymousFunctionDescriptor(
                context.scope.getContainingDeclaration(), Annotations.EMPTY, CallableMemberDescriptor.Kind.DECLARATION,
                toSourceElement(functionLiteral)
        );

        List<ValueParameterDescriptor> valueParameterDescriptors = createValueParameterDescriptors(context, functionLiteral,
                                                                                                   functionDescriptor, functionTypeExpected);

        JetType effectiveReceiverType;
        if (receiverTypeRef == null) {
            if (functionTypeExpected) {
                effectiveReceiverType = KotlinBuiltIns.getReceiverType(context.expectedType);
            }
            else {
                effectiveReceiverType = null;
            }
        }
        else {
            effectiveReceiverType = components.expressionTypingServices.getTypeResolver().resolveType(context.scope, receiverTypeRef, context.trace, true);
        }
        functionDescriptor.initialize(effectiveReceiverType,
                                      ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER,
                                      Collections.<TypeParameterDescriptorImpl>emptyList(),
                                      valueParameterDescriptors,
                                      /*unsubstitutedReturnType = */ null,
                                      Modality.FINAL,
                                      Visibilities.LOCAL
        );
        BindingContextUtils.recordFunctionDeclarationToDescriptor(context.trace, functionLiteral, functionDescriptor);
        return functionDescriptor;
    }

    @NotNull
    private List<ValueParameterDescriptor> createValueParameterDescriptors(
            @NotNull ExpressionTypingContext context,
            @NotNull JetFunctionLiteral functionLiteral,
            @NotNull FunctionDescriptorImpl functionDescriptor,
            boolean functionTypeExpected
    ) {
        List<ValueParameterDescriptor> valueParameterDescriptors = Lists.newArrayList();
        List<JetParameter> declaredValueParameters = functionLiteral.getValueParameters();

        List<ValueParameterDescriptor> expectedValueParameters =  (functionTypeExpected)
                                                          ? KotlinBuiltIns.getValueParameters(functionDescriptor, context.expectedType)
                                                          : null;

        JetParameterList valueParameterList = functionLiteral.getValueParameterList();
        boolean hasDeclaredValueParameters = valueParameterList != null;
        if (functionTypeExpected && !hasDeclaredValueParameters && expectedValueParameters.size() == 1) {
            ValueParameterDescriptor valueParameterDescriptor = expectedValueParameters.get(0);
            ValueParameterDescriptor it = new ValueParameterDescriptorImpl(
                    functionDescriptor, null, 0, Annotations.EMPTY, Name.identifier("it"),
                    valueParameterDescriptor.getType(), valueParameterDescriptor.hasDefaultValue(), valueParameterDescriptor.getVarargElementType(),
                    SourceElement.NO_SOURCE
            );
            valueParameterDescriptors.add(it);
            context.trace.record(AUTO_CREATED_IT, it);
        }
        else {
            if (expectedValueParameters != null && declaredValueParameters.size() != expectedValueParameters.size()) {
                List<JetType> expectedParameterTypes = ExpressionTypingUtils.getValueParametersTypes(expectedValueParameters);
                context.trace.report(EXPECTED_PARAMETERS_NUMBER_MISMATCH.on(functionLiteral, expectedParameterTypes.size(), expectedParameterTypes));
            }
            for (int i = 0; i < declaredValueParameters.size(); i++) {
                ValueParameterDescriptor valueParameterDescriptor = createValueParameterDescriptor(
                        context, functionDescriptor, declaredValueParameters, expectedValueParameters, i);
                valueParameterDescriptors.add(valueParameterDescriptor);
            }
        }
        return valueParameterDescriptors;
    }

    @NotNull
    private ValueParameterDescriptor createValueParameterDescriptor(
            @NotNull ExpressionTypingContext context,
            @NotNull FunctionDescriptorImpl functionDescriptor,
            @NotNull List<JetParameter> declaredValueParameters,
            @Nullable List<ValueParameterDescriptor> expectedValueParameters,
            int index
    ) {
        JetParameter declaredParameter = declaredValueParameters.get(index);
        JetTypeReference typeReference = declaredParameter.getTypeReference();

        JetType expectedType;
        if (expectedValueParameters != null && index < expectedValueParameters.size()) {
            expectedType = expectedValueParameters.get(index).getType();
        }
        else {
            expectedType = null;
        }
        JetType type;
        if (typeReference != null) {
            type = components.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, true);
            if (expectedType != null) {
                if (!JetTypeChecker.DEFAULT.isSubtypeOf(expectedType, type)) {
                    context.trace.report(EXPECTED_PARAMETER_TYPE_MISMATCH.on(declaredParameter, expectedType));
                }
            }
        }
        else {
            boolean containsUninferredParameter = TypeUtils.containsSpecialType(expectedType, new Function1<JetType, Boolean>() {
                @Override
                public Boolean invoke(JetType type) {
                    return TypeUtils.isDontCarePlaceholder(type) || ErrorUtils.isUninferredParameter(type);
                }
            });
            if (expectedType == null || containsUninferredParameter) {
                context.trace.report(CANNOT_INFER_PARAMETER_TYPE.on(declaredParameter));
            }
            if (expectedType != null) {
                type = expectedType;
            }
            else {
                type = CANT_INFER_LAMBDA_PARAM_TYPE;
            }
        }
        return components.expressionTypingServices.getDescriptorResolver().resolveValueParameterDescriptorWithAnnotationArguments(
                context.scope, functionDescriptor, declaredParameter, index, type, context.trace);
    }

    @NotNull
    private JetType computeReturnType(
            @NotNull JetFunctionLiteralExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull SimpleFunctionDescriptorImpl functionDescriptor,
            boolean functionTypeExpected
    ) {
        JetType expectedReturnType = functionTypeExpected ? KotlinBuiltIns.getReturnTypeFromFunctionType(context.expectedType) : null;
        JetType returnType = computeUnsafeReturnType(expression, context, functionDescriptor, expectedReturnType);

        if (!expression.getFunctionLiteral().hasDeclaredReturnType() && functionTypeExpected) {
            if (KotlinBuiltIns.isUnit(expectedReturnType)) {
                return components.builtIns.getUnitType();
            }
        }
        return returnType == null ? CANT_INFER_LAMBDA_PARAM_TYPE : returnType;
    }

    @Nullable
    private JetType computeUnsafeReturnType(
            @NotNull JetFunctionLiteralExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull SimpleFunctionDescriptorImpl functionDescriptor,
            @Nullable JetType expectedReturnType
    ) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        JetBlockExpression bodyExpression = functionLiteral.getBodyExpression();
        assert bodyExpression != null;

        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
        JetTypeReference returnTypeRef = functionLiteral.getTypeReference();
        JetType declaredReturnType = null;
        if (returnTypeRef != null) {
            declaredReturnType = components.expressionTypingServices.getTypeResolver().resolveType(context.scope, returnTypeRef, context.trace, true);
            // This is needed for ControlStructureTypingVisitor#visitReturnExpression() to properly type-check returned expressions
            functionDescriptor.setReturnType(declaredReturnType);
            if (expectedReturnType != null) {
                if (!JetTypeChecker.DEFAULT.isSubtypeOf(declaredReturnType, expectedReturnType)) {
                    context.trace.report(EXPECTED_RETURN_TYPE_MISMATCH.on(returnTypeRef, expectedReturnType));
                }
            }
        }

        // Type-check the body
        JetType expectedType = declaredReturnType != null
                       ? declaredReturnType
                       : (expectedReturnType != null ? expectedReturnType : NO_EXPECTED_TYPE);
        ExpressionTypingContext newContext = context.replaceScope(functionInnerScope).replaceExpectedType(expectedType);
        context.trace.record(EXPECTED_RETURN_TYPE, functionLiteral, expectedType);
        JetType typeOfBodyExpression = // needed for error reporting
                components.expressionTypingServices.getBlockReturnedType(bodyExpression, COERCION_TO_UNIT, newContext).getType();

        if (declaredReturnType != null) {
            return declaredReturnType;
        }
        else {
            return computeReturnTypeBasedOnReturnExpressions(functionLiteral, context, typeOfBodyExpression);
        }
    }

    @Nullable
    private JetType computeReturnTypeBasedOnReturnExpressions(
            @NotNull JetFunctionLiteral functionLiteral,
            @NotNull ExpressionTypingContext context,
            @Nullable JetType typeOfBodyExpression
    ) {
        List<JetType> returnedExpressionTypes = Lists.newArrayList();

        boolean hasEmptyReturn = false;
        Collection<JetReturnExpression> returnExpressions = collectReturns(functionLiteral, context.trace);
        for (JetReturnExpression returnExpression : returnExpressions) {
            JetExpression returnedExpression = returnExpression.getReturnedExpression();
            if (returnedExpression == null) {
                hasEmptyReturn = true;
            }
            else {
                // the type should have been computed by getBlockReturnedType() above, but can be null, if returnExpression contains some error
                ContainerUtil.addIfNotNull(returnedExpressionTypes, context.trace.get(EXPRESSION_TYPE, returnedExpression));
            }
        }

        if (hasEmptyReturn) {
            for (JetReturnExpression returnExpression : returnExpressions) {
                JetExpression returnedExpression = returnExpression.getReturnedExpression();
                if (returnedExpression != null) {
                    JetType type = context.trace.get(EXPRESSION_TYPE, returnedExpression);
                    if (type == null || !KotlinBuiltIns.isUnit(type)) {
                        context.trace.report(RETURN_TYPE_MISMATCH.on(returnedExpression, components.builtIns.getUnitType()));
                    }
                }
            }
            return components.builtIns.getUnitType();
        }

        ContainerUtil.addIfNotNull(returnedExpressionTypes, typeOfBodyExpression);

        if (returnedExpressionTypes.isEmpty()) return null;
        return CommonSupertypes.commonSupertype(returnedExpressionTypes);
    }

    private static Collection<JetReturnExpression> collectReturns(
            @NotNull final JetFunctionLiteral functionLiteral,
            @NotNull final BindingTrace trace
    ) {
        Collection<JetReturnExpression> result = Lists.newArrayList();
        JetBlockExpression bodyExpression = functionLiteral.getBodyExpression();
        assert bodyExpression != null;
        bodyExpression.accept(
                new JetTreeVisitor<Collection<JetReturnExpression>>() {
                    @Override
                    public Void visitReturnExpression(
                            @NotNull JetReturnExpression expression, Collection<JetReturnExpression> data
                    ) {
                        data.add(expression);
                        return null;
                    }
                },
                result
        );
        return ContainerUtil.mapNotNull(result, new Function<JetReturnExpression, JetReturnExpression>() {
            @Override
            public JetReturnExpression fun(@NotNull JetReturnExpression returnExpression) {
                JetSimpleNameExpression label = returnExpression.getTargetLabel();
                if (label == null) {
                    // No label => non-local return
                    return null;
                }

                PsiElement labelTarget = trace.get(BindingContext.LABEL_TARGET, label);
                if (labelTarget != functionLiteral) {
                    // Either a local return of inner lambda/function or a non-local return
                    return null;
                }
                return returnExpression;
            }
        });
    }
}
