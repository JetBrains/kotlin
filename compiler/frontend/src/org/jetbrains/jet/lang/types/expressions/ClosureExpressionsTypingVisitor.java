/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.util.RecursionIntolerantLazyValueWithDefault;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.calls.context.ContextDependency.INDEPENDENT;
import static org.jetbrains.jet.lang.types.TypeUtils.*;
import static org.jetbrains.jet.lang.types.expressions.CoercionStrategy.COERCION_TO_UNIT;

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
                    JetType defaultType = DeferredType.create(context.trace, new RecursionIntolerantLazyValueWithDefault<JetType>(ErrorUtils.createErrorType("Recursive dependency")) {
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
        TopDownAnalyzer.processClassOrObject(context.replaceBindingTrace(traceAdapter).replaceContextDependency(INDEPENDENT),
                                             context.scope.getContainingDeclaration(),
                                             expression.getObjectDeclaration());

        DelegatingBindingTrace cloneDelta = new DelegatingBindingTrace(
                new BindingTraceContext().getBindingContext(), "cached delta trace for object literal expression resolve", expression);
        temporaryTrace.addAllMyDataTo(cloneDelta);
        context.trace.record(TRACE_DELTAS_CACHE, expression.getObjectDeclaration(), cloneDelta);
        temporaryTrace.commit();
        return DataFlowUtils.checkType(result[0], expression, context, context.dataFlowInfo);
    }

    @Override
    public JetTypeInfo visitFunctionLiteralExpression(JetFunctionLiteralExpression expression, ExpressionTypingContext context) {
        JetBlockExpression bodyExpression = expression.getFunctionLiteral().getBodyExpression();
        if (bodyExpression == null) return null;

        Name callerName = getCallerName(expression);
        if (callerName != null) {
            context.labelResolver.enterLabeledElement(new LabelName(callerName.asString()), expression);
        }

        JetType expectedType = context.expectedType;
        boolean functionTypeExpected = !noExpectedType(expectedType) && KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(
                expectedType);

        AnonymousFunctionDescriptor functionDescriptor = createFunctionDescriptor(expression, context, functionTypeExpected);
        JetType safeReturnType = computeReturnType(expression, context, functionDescriptor, functionTypeExpected);
        functionDescriptor.setReturnType(safeReturnType);

        JetType receiver = DescriptorUtils.getReceiverParameterType(functionDescriptor.getReceiverParameter());
        List<JetType> valueParametersTypes = DescriptorUtils.getValueParametersTypes(functionDescriptor.getValueParameters());
        JetType resultType = KotlinBuiltIns.getInstance().getFunctionType(
                Collections.<AnnotationDescriptor>emptyList(), receiver, valueParametersTypes, safeReturnType);
        if (!noExpectedType(expectedType) && KotlinBuiltIns.getInstance().isFunctionOrExtensionFunctionType(expectedType)) {
            // all checks were done before
            return JetTypeInfo.create(resultType, context.dataFlowInfo);
        }

        if (callerName != null) {
            context.labelResolver.exitLabeledElement(expression);
        }

        return DataFlowUtils.checkType(resultType, expression, context, context.dataFlowInfo);
    }

    @Nullable
    private static Name getCallerName(@NotNull JetFunctionLiteralExpression expression) {
        JetCallExpression callExpression = getContainingCallExpression(expression);
        if (callExpression == null) return null;

        JetExpression calleeExpression = callExpression.getCalleeExpression();
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) calleeExpression;
            return nameExpression.getReferencedNameAsName();
        }

        return null;
    }

    @Nullable
    private static JetCallExpression getContainingCallExpression(JetFunctionLiteralExpression expression) {
        PsiElement parent = expression.getParent();
        if (parent instanceof JetCallExpression) {
            // f {}
            return (JetCallExpression) parent;
        }

        if (parent instanceof JetValueArgument) {
            // f ({}) or f(p = {})
            JetValueArgument argument = (JetValueArgument) parent;
            PsiElement argList = argument.getParent();
            if (argList == null) return null;
            PsiElement call = argList.getParent();
            if (call instanceof JetCallExpression) {
                return (JetCallExpression) call;
            }
        }
        return null;
    }

    @NotNull
    private static AnonymousFunctionDescriptor createFunctionDescriptor(
            @NotNull JetFunctionLiteralExpression expression,
            @NotNull ExpressionTypingContext context,
            boolean functionTypeExpected
    ) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        JetTypeReference receiverTypeRef = functionLiteral.getReceiverTypeRef();
        AnonymousFunctionDescriptor functionDescriptor = new AnonymousFunctionDescriptor(
                context.scope.getContainingDeclaration(), Collections.<AnnotationDescriptor>emptyList(), CallableMemberDescriptor.Kind.DECLARATION);

        List<ValueParameterDescriptor> valueParameterDescriptors = createValueParameterDescriptors(context, functionLiteral,
                                                                                                   functionDescriptor, functionTypeExpected);

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
        BindingContextUtils.recordFunctionDeclarationToDescriptor(context.trace, functionLiteral, functionDescriptor);
        return functionDescriptor;
    }

    @NotNull
    private static List<ValueParameterDescriptor> createValueParameterDescriptors(
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
                    functionDescriptor, 0, Collections.<AnnotationDescriptor>emptyList(), Name.identifier("it"),
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
                ValueParameterDescriptor valueParameterDescriptor = createValueParameterDescriptor(
                        context, functionDescriptor, declaredValueParameters, expectedValueParameters, i);
                valueParameterDescriptors.add(valueParameterDescriptor);
            }
        }
        return valueParameterDescriptors;
    }

    @NotNull
    private static ValueParameterDescriptor createValueParameterDescriptor(
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
            type = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, typeReference, context.trace, true);
            if (expectedType != null) {
                if (!JetTypeChecker.INSTANCE.isSubtypeOf(expectedType, type)) {
                    context.trace.report(EXPECTED_PARAMETER_TYPE_MISMATCH.on(declaredParameter, expectedType));
                }
            }
        }
        else {
            if (expectedType == null || expectedType == DONT_CARE || expectedType == CANT_INFER_TYPE_PARAMETER) {
                context.trace.report(CANNOT_INFER_PARAMETER_TYPE.on(declaredParameter));
            }
            if (expectedType != null) {
                type = expectedType;
            }
            else {
                type = CANT_INFER_LAMBDA_PARAM_TYPE;
            }
        }
        return context.expressionTypingServices.getDescriptorResolver().resolveValueParameterDescriptorWithAnnotationArguments(
                context.scope, functionDescriptor, declaredParameter, index, type, context.trace);
    }

    @NotNull
    private static JetType computeReturnType(
            @NotNull JetFunctionLiteralExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull SimpleFunctionDescriptorImpl functionDescriptor,
            boolean functionTypeExpected
    ) {
        JetType expectedReturnType = functionTypeExpected ? KotlinBuiltIns.getInstance().getReturnTypeFromFunctionType(context.expectedType) : null;
        JetType returnType = computeUnsafeReturnType(expression, context, functionDescriptor, expectedReturnType);

        if (!expression.getFunctionLiteral().hasDeclaredReturnType() && functionTypeExpected) {
            if (KotlinBuiltIns.getInstance().isUnit(expectedReturnType)) {
                return KotlinBuiltIns.getInstance().getUnitType();
            }
        }
        return returnType == null ? CANT_INFER_LAMBDA_PARAM_TYPE : returnType;
    }

    @Nullable
    private static JetType computeUnsafeReturnType(
            @NotNull JetFunctionLiteralExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull SimpleFunctionDescriptorImpl functionDescriptor,
            @Nullable JetType expectedReturnType
    ) {
        JetFunctionLiteral functionLiteral = expression.getFunctionLiteral();
        JetBlockExpression bodyExpression = functionLiteral.getBodyExpression();
        assert bodyExpression != null;

        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(context.scope, functionDescriptor, context.trace);
        JetTypeReference returnTypeRef = functionLiteral.getReturnTypeRef();
        JetType declaredReturnType = null;
        if (returnTypeRef != null) {
            declaredReturnType = context.expressionTypingServices.getTypeResolver().resolveType(context.scope, returnTypeRef, context.trace, true);
            // This is needed for ControlStructureTypingVisitor#visitReturnExpression() to properly type-check returned expressions
            functionDescriptor.setReturnType(declaredReturnType);
            if (expectedReturnType != null) {
                if (!JetTypeChecker.INSTANCE.isSubtypeOf(declaredReturnType, expectedReturnType)) {
                    context.trace.report(EXPECTED_RETURN_TYPE_MISMATCH.on(returnTypeRef, expectedReturnType));
                }
            }
        }

        // Type-check the body
        ExpressionTypingContext newContext = context.replaceScope(functionInnerScope)
                .replaceExpectedType(declaredReturnType != null
                                     ? declaredReturnType
                                     : (expectedReturnType != null ? expectedReturnType : NO_EXPECTED_TYPE));

        JetType typeOfBodyExpression = context.expressionTypingServices.getBlockReturnedType(bodyExpression, COERCION_TO_UNIT, newContext).getType();

        List<JetType> returnedExpressionTypes = Lists.newArrayList(getTypesOfLocallyReturnedExpressions(
                functionLiteral, context.trace, collectReturns(bodyExpression)));
        ContainerUtil.addIfNotNull(returnedExpressionTypes, typeOfBodyExpression);

        if (declaredReturnType != null) return declaredReturnType;
        if (returnedExpressionTypes.isEmpty()) return null;
        return CommonSupertypes.commonSupertype(returnedExpressionTypes);
    }

    private static List<JetType> getTypesOfLocallyReturnedExpressions(
            final JetFunctionLiteral functionLiteral,
            final BindingTrace trace,
            Collection<JetReturnExpression> returnExpressions
    ) {
        return ContainerUtil.mapNotNull(returnExpressions, new Function<JetReturnExpression, JetType>() {
            @Override
            public JetType fun(JetReturnExpression returnExpression) {
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

                JetExpression returnedExpression = returnExpression.getReturnedExpression();
                if (returnedExpression == null) {
                    return KotlinBuiltIns.getInstance().getUnitType();
                }
                JetType returnedType = trace.get(EXPRESSION_TYPE, returnedExpression);
                assert returnedType != null : "No type for returned expression: " + returnedExpression + ",\n" +
                                              "the type should have been computed by getBlockReturnedType() above";
                return returnedType;
            }
        });
    }

    public static Collection<JetReturnExpression> collectReturns(@NotNull JetExpression expression) {
        Collection<JetReturnExpression> result = Lists.newArrayList();
        expression.accept(
                new JetTreeVisitor<Collection<JetReturnExpression>>() {
                    @Override
                    public Void visitReturnExpression(
                            JetReturnExpression expression, Collection<JetReturnExpression> data
                    ) {
                        data.add(expression);
                        return null;
                    }
                },
                result
        );
        return result;
    }
}
