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
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.util.lazy.LazyValueWithDefault;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author abreslav
 * @author svtk
 */
public class ClosureExpressionsTypingVisitor extends ExpressionTypingVisitor {
    protected ClosureExpressionsTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        super(facade);
    }

    @Override
    public JetTypeInfo visitObjectLiteralExpression(final JetObjectLiteralExpression expression, final ExpressionTypingContext context) {
        DelegatingBindingTrace delegatingBindingTrace = context.trace.get(TRACE_DELTAS_CACHE, expression.getObjectDeclaration());
        if (delegatingBindingTrace != null) {
            delegatingBindingTrace.addAllMyDataTo(context.trace);
            JetType type = context.trace.get(EXPRESSION_TYPE, expression);
            return DataFlowUtils.checkType(type, expression, context, context.dataFlowInfo);
        }
        final JetType[] result = new JetType[1];
        final TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve object literal expression", expression);
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
                        temporaryTrace.record(EXPRESSION_TYPE, expression, defaultType);
                        temporaryTrace.record(PROCESSED, expression);
                    }
                }
            }
        };
        ObservableBindingTrace traceAdapter = new ObservableBindingTrace(temporaryTrace);
        traceAdapter.addHandler(CLASS, handler);
        TopDownAnalyzer.processClassOrObject(context.expressionTypingServices.getProject(), traceAdapter, context.scope,
                                             context.scope.getContainingDeclaration(), expression.getObjectDeclaration());

        DelegatingBindingTrace cloneDelta = new DelegatingBindingTrace(
                new BindingTraceContext().getBindingContext(), "cached delta trace for object literal expression resolve", expression);
        temporaryTrace.addAllMyDataTo(cloneDelta);
        context.trace.record(TRACE_DELTAS_CACHE, expression.getObjectDeclaration(), cloneDelta);
        temporaryTrace.commit();
        return DataFlowUtils.checkType(result[0], expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, ExpressionTypingContext context) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        JetBlockExpression bodyExpression = functionLiteral.getBodyExpression();
        if (bodyExpression == null) return null;

        JetType expectedType = context.expectedType;
        boolean functionTypeExpected = expectedType != NO_EXPECTED_TYPE && KotlinBuiltIns.getInstance().isFunctionType(expectedType);

        SimpleFunctionDescriptorImpl functionDescriptor = createFunctionDescriptor(expression, context, functionTypeExpected);

        List<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
        ReceiverParameterDescriptor receiverParameter = functionDescriptor.getReceiverParameter();
        JetType receiver = DescriptorUtils.getReceiverParameterType(receiverParameter);

        JetType returnType = NO_EXPECTED_TYPE;
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
        JetTypeReference returnTypeRef = functionLiteral.getReturnTypeRef();
        TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace, "trace to resolve function literal expression", expression);
        JetType expectedReturnType = functionTypeExpected ? KotlinBuiltIns.getInstance().getReturnTypeFromFunctionType(expectedType) : null;
        if (returnTypeRef != null) {
            returnType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, returnTypeRef, context.trace, true);
            context.expressionTypingServices.checkFunctionReturnType(expression, context.replaceScope(functionInnerScope).
                    replaceExpectedType(returnType).replaceBindingTrace(temporaryTrace), temporaryTrace);
            if (functionTypeExpected) {
                if (!JetTypeChecker.INSTANCE.isSubtypeOf(expectedReturnType, returnType)) {
                    temporaryTrace.report(EXPECTED_RETURN_TYPE_MISMATCH.on(returnTypeRef, expectedReturnType));
                }
            }
        }
        else {
            if (functionTypeExpected) {
                returnType = expectedReturnType;
            }
            returnType = context.expressionTypingServices.getBlockReturnedType(functionInnerScope, bodyExpression, CoercionStrategy.COERCION_TO_UNIT,
                    context.replaceExpectedType(returnType).replaceBindingTrace(temporaryTrace), temporaryTrace).getType();
        }
        temporaryTrace.commit(new TraceEntryFilter() {
            @Override
            public boolean accept(@NotNull WritableSlice<?, ?> slice, Object key) {
                return (slice != BindingContext.RESOLUTION_RESULTS_FOR_FUNCTION && slice != BindingContext.RESOLUTION_RESULTS_FOR_PROPERTY &&
                        slice != BindingContext.TRACE_DELTAS_CACHE);
            }
        }, true);
        JetType safeReturnType = returnType == null ? ErrorUtils.createErrorType("<return type>") : returnType;

        if (!functionLiteral.hasDeclaredReturnType() && functionTypeExpected) {
            if (KotlinBuiltIns.getInstance().isUnit(expectedReturnType)) {
                safeReturnType = KotlinBuiltIns.getInstance().getUnitType();
            }
        }
        functionDescriptor.setReturnType(safeReturnType);

        JetType resultType = KotlinBuiltIns.getInstance().getFunctionType(
                Collections.<AnnotationDescriptor>emptyList(), receiver, DescriptorUtils.getValueParametersTypes(valueParameters), safeReturnType);
        if (expectedType != NO_EXPECTED_TYPE && KotlinBuiltIns.getInstance().isFunctionType(expectedType)) {
            // all checks were done before
            return JetTypeInfo.create(resultType, context.dataFlowInfo);
        }
        return DataFlowUtils.checkType(resultType, expression, context, context.dataFlowInfo);
    }

    @NotNull
    private SimpleFunctionDescriptorImpl createFunctionDescriptor(
            @NotNull JetFunctionLiteralExpression expression,
            @NotNull ExpressionTypingContext context,
            boolean functionTypeExpected
    ) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        JetTypeReference receiverTypeRef = functionLiteral.getReceiverTypeRef();
        SimpleFunctionDescriptorImpl functionDescriptor = new SimpleFunctionDescriptorImpl(
                context.scope.getContainingDeclaration(), Collections.<AnnotationDescriptor>emptyList(), Name.special("<anonymous>"), CallableMemberDescriptor.Kind.DECLARATION);

        List<ValueParameterDescriptor> valueParameterDescriptors = createValueParameterDescriptors(context, functionLiteral, functionDescriptor, functionTypeExpected);

        JetType effectiveReceiverType;
        if (receiverTypeRef == null) {
            if (functionTypeExpected) {
                effectiveReceiverType = KotlinBuiltIns.getInstance().getReceiverType(context.expectedType);
            }
            else {
                effectiveReceiverType = null;
            }
        }
        else {
            effectiveReceiverType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, receiverTypeRef, context.trace, true);
        }
        functionDescriptor.initialize(effectiveReceiverType,
                                      ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER,
                                      Collections.<TypeParameterDescriptorImpl>emptyList(),
                                      valueParameterDescriptors,
                                      /*unsubstitutedReturnType = */ null,
                                      Modality.FINAL,
                                      Visibilities.LOCAL,
                                      /*isInline = */ false
        );
        context.trace.record(BindingContext.FUNCTION, expression, functionDescriptor);
        BindingContextUtils.recordFunctionDeclarationToDescriptor(context.trace, expression, functionDescriptor);
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
                                                          ? KotlinBuiltIns.getInstance().getValueParameters(functionDescriptor, context.expectedType)
                                                          : null;

        JetParameterList valueParameterList = functionLiteral.getValueParameterList();
        boolean hasDeclaredValueParameters = valueParameterList != null;
        if (functionTypeExpected && !hasDeclaredValueParameters && expectedValueParameters.size() == 1) {
            ValueParameterDescriptor valueParameterDescriptor = expectedValueParameters.get(0);
            ValueParameterDescriptor it = new ValueParameterDescriptorImpl(
                    functionDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("it"), false,
                    valueParameterDescriptor.getType(), valueParameterDescriptor.hasDefaultValue(), valueParameterDescriptor.getVarargElementType()
            );
            valueParameterDescriptors.add(it);
            context.trace.record(AUTO_CREATED_IT, it);
        }
        else {
            if (expectedValueParameters != null && declaredValueParameters.size() != expectedValueParameters.size()) {
                List<JetType> expectedParameterTypes = DescriptorUtils.getValueParametersTypes(expectedValueParameters);
                context.trace.report(EXPECTED_PARAMETERS_NUMBER_MISMATCH.on(functionLiteral, expectedParameterTypes.size(), expectedParameterTypes));
            }
            for (int i = 0; i < declaredValueParameters.size(); i++) {
                JetParameter declaredParameter = declaredValueParameters.get(i);
                JetTypeReference typeReference = declaredParameter.getTypeReference();

                JetType expectedType;
                if (expectedValueParameters != null && i < expectedValueParameters.size()) {
                    expectedType = expectedValueParameters.get(i).getType();
                }
                else {
                    expectedType = null;
                }
                JetType type;
                if (typeReference != null) {
                    type = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, true);
                    if (expectedType != null) {
                        if (!JetTypeChecker.INSTANCE.isSubtypeOf(type, expectedType)) {
                            context.trace.report(EXPECTED_PARAMETER_TYPE_MISMATCH.on(declaredParameter, expectedType));
                        }
                    }
                }
                else {
                    if (expectedType == null || expectedType == CallResolverUtil.DONT_CARE || expectedType == CallResolverUtil.CANT_INFER) {
                        context.trace.report(CANNOT_INFER_PARAMETER_TYPE.on(declaredParameter));
                    }
                    if (expectedType != null) {
                        type = expectedType;
                    }
                    else {
                        type = ErrorUtils.createErrorType("Cannot be inferred");
                    }
                }
                ValueParameterDescriptor valueParameterDescriptor = context.expressionTypingServices.getDescriptorResolver().resolveValueParameterDescriptor(
                        context.scope, functionDescriptor, declaredParameter, i, type, context.trace);
                valueParameterDescriptors.add(valueParameterDescriptor);
            }
        }
        return valueParameterDescriptors;
    }
}
