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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.ObservableBindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.util.lazy.LazyValueWithDefault;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.CANNOT_INFER_PARAMETER_TYPE;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 * @author svtk
 */
public class ClosureExpressionsTypingVisitor extends ExpressionTypingVisitor {
    protected ClosureExpressionsTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public JetType visitObjectLiteralExpression(final JetObjectLiteralExpression expression, final ExpressionTypingContext context) {
        final JetType[] result = new JetType[1];
        ObservableBindingTrace.RecordHandler<PsiElement, ClassDescriptor> handler = new ObservableBindingTrace.RecordHandler<PsiElement, ClassDescriptor>() {

            @Override
            public void handleRecord(WritableSlice<PsiElement, ClassDescriptor> slice, PsiElement declaration, final ClassDescriptor descriptor) {
                if (slice == CLASS && declaration == expression.getObjectDeclaration()) {
                    JetType defaultType = DeferredType.create(context.trace, new LazyValueWithDefault<JetType>(ErrorUtils.createErrorType("Recursive dependency")) {
                        @Override
                        protected JetType compute() {
                            return descriptor.getDefaultType();
                        }
                    });
                    result[0] = defaultType;
                    if (!context.trace.get(PROCESSED, expression)) {
                        context.trace.record(EXPRESSION_TYPE, expression, defaultType);
                        context.trace.record(PROCESSED, expression);
                    }
                }
            }
        };
        ObservableBindingTrace traceAdapter = new ObservableBindingTrace(context.trace);
        traceAdapter.addHandler(CLASS, handler);
        TopDownAnalyzer.processObject(context.semanticServices, traceAdapter, context.scope, context.scope.getContainingDeclaration(), expression.getObjectDeclaration());
        return DataFlowUtils.checkType(result[0], expression, context);
    }

    @Override
    public JetType visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, ExpressionTypingContext context) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        JetBlockExpression bodyExpression = functionLiteral.getBodyExpression();
        if (bodyExpression == null) return null;

        JetType expectedType = context.expectedType;
        boolean functionTypeExpected = expectedType != TypeUtils.NO_EXPECTED_TYPE && JetStandardClasses.isFunctionType(expectedType);

        SimpleFunctionDescriptorImpl functionDescriptor = createFunctionDescriptor(expression, context, functionTypeExpected);

        List<JetType> parameterTypes = Lists.newArrayList();
        List<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
        for (ValueParameterDescriptor valueParameter : valueParameters) {
            parameterTypes.add(valueParameter.getOutType());
        }
        ReceiverDescriptor receiverParameter = functionDescriptor.getReceiverParameter();
        JetType receiver = receiverParameter != NO_RECEIVER ? receiverParameter.getType() : null;

        JetType returnType = TypeUtils.NO_EXPECTED_TYPE;
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
        JetTypeReference returnTypeRef = functionLiteral.getReturnTypeRef();
        if (returnTypeRef != null) {
            returnType = context.getTypeResolver().resolveType(context.scope, returnTypeRef);
            context.getServices().checkFunctionReturnType(expression, context.replaceScope(functionInnerScope).
                    replaceExpectedType(returnType).replaceExpectedReturnType(returnType).replaceDataFlowInfo(context.dataFlowInfo));
        }
        else {
            if (functionTypeExpected) {
                returnType = JetStandardClasses.getReturnTypeFromFunctionType(expectedType);
            }
            returnType = context.getServices().getBlockReturnedType(functionInnerScope, bodyExpression, CoercionStrategy.COERCION_TO_UNIT,
                                                                    context.replaceExpectedType(returnType).replaceExpectedReturnType(returnType));
        }
        JetType safeReturnType = returnType == null ? ErrorUtils.createErrorType("<return type>") : returnType;
        functionDescriptor.setReturnType(safeReturnType);

        boolean hasDeclaredValueParameters = functionLiteral.getValueParameterList() != null;
        if (!hasDeclaredValueParameters && functionTypeExpected) {
            JetType expectedReturnType = JetStandardClasses.getReturnTypeFromFunctionType(expectedType);
            if (JetStandardClasses.isUnit(expectedReturnType)) {
                functionDescriptor.setReturnType(JetStandardClasses.getUnitType());
                return DataFlowUtils.checkType(JetStandardClasses.getFunctionType(Collections.<AnnotationDescriptor>emptyList(), receiver, parameterTypes, JetStandardClasses.getUnitType()), expression, context);
            }

        }
        return DataFlowUtils.checkType(JetStandardClasses.getFunctionType(Collections.<AnnotationDescriptor>emptyList(), receiver, parameterTypes, safeReturnType), expression, context);
    }

    private SimpleFunctionDescriptorImpl createFunctionDescriptor(JetFunctionLiteralExpression expression, ExpressionTypingContext context, boolean functionTypeExpected) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        JetTypeReference receiverTypeRef = functionLiteral.getReceiverTypeRef();
        SimpleFunctionDescriptorImpl functionDescriptor = new SimpleFunctionDescriptorImpl(
                context.scope.getContainingDeclaration(), Collections.<AnnotationDescriptor>emptyList(), "<anonymous>", CallableMemberDescriptor.Kind.DECLARATION);

        List<ValueParameterDescriptor> valueParameterDescriptors = createValueParameterDescriptors(context, functionLiteral, functionDescriptor, functionTypeExpected);

        JetType effectiveReceiverType;
        if (receiverTypeRef == null) {
            if (functionTypeExpected) {
                effectiveReceiverType = JetStandardClasses.getReceiverType(context.expectedType);
            }
            else {
                effectiveReceiverType = null;
            }
        }
        else {
            effectiveReceiverType = context.getTypeResolver().resolveType(context.scope, receiverTypeRef);
        }
        functionDescriptor.initialize(effectiveReceiverType, NO_RECEIVER, Collections.<TypeParameterDescriptor>emptyList(), valueParameterDescriptors, null, Modality.FINAL, Visibility.LOCAL);
        context.trace.record(BindingContext.FUNCTION, expression, functionDescriptor);
        return functionDescriptor;
    }

    private List<ValueParameterDescriptor> createValueParameterDescriptors(ExpressionTypingContext context, JetFunctionLiteral functionLiteral, FunctionDescriptorImpl functionDescriptor, boolean functionTypeExpected) {
        List<ValueParameterDescriptor> valueParameterDescriptors = Lists.newArrayList();
        List<JetParameter> declaredValueParameters = functionLiteral.getValueParameters();

        List<ValueParameterDescriptor> expectedValueParameters =  (functionTypeExpected)
                                                          ? JetStandardClasses.getValueParameters(functionDescriptor, context.expectedType)
                                                          : null;

        boolean hasDeclaredValueParameters = functionLiteral.getValueParameterList() != null;
        if (functionTypeExpected && !hasDeclaredValueParameters && expectedValueParameters.size() == 1) {
            ValueParameterDescriptor valueParameterDescriptor = expectedValueParameters.get(0);
            ValueParameterDescriptor it = new ValueParameterDescriptorImpl(
                    functionDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), "it", false, valueParameterDescriptor.getOutType(), valueParameterDescriptor.hasDefaultValue(), valueParameterDescriptor.getVarargElementType()
            );
            valueParameterDescriptors.add(it);
            context.trace.record(AUTO_CREATED_IT, it);
        }
        else {
            for (int i = 0; i < declaredValueParameters.size(); i++) {
                JetParameter declaredParameter = declaredValueParameters.get(i);
                JetTypeReference typeReference = declaredParameter.getTypeReference();

                JetType type;
                if (typeReference != null) {
                    type = context.getTypeResolver().resolveType(context.scope, typeReference);
                }
                else {
                    if (expectedValueParameters != null && i < expectedValueParameters.size()) {
                        type = expectedValueParameters.get(i).getOutType();
                    }
                    else {
                        context.trace.report(CANNOT_INFER_PARAMETER_TYPE.on(declaredParameter));
                        type = ErrorUtils.createErrorType("Cannot be inferred");
                    }
                }
                ValueParameterDescriptor valueParameterDescriptor = context.getDescriptorResolver().resolveValueParameterDescriptor(functionDescriptor, declaredParameter, i, type);
                valueParameterDescriptors.add(valueParameterDescriptor);
            }
        }
        return valueParameterDescriptors;
    }
}
