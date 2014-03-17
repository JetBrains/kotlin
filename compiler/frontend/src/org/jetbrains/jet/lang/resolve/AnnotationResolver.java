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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.constants.ArrayValue;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.IntegerValueTypeConstant;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.ANNOTATION_DESCRIPTOR_TO_PSI_ELEMENT;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

public class AnnotationResolver {

    private ExpressionTypingServices expressionTypingServices;
    private CallResolver callResolver;

    @Inject
    public void setExpressionTypingServices(ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setCallResolver(CallResolver callResolver) {
        this.callResolver = callResolver;
    }

    @NotNull
    public Annotations resolveAnnotationsWithoutArguments(
            @NotNull JetScope scope,
            @Nullable JetModifierList modifierList,
            @NotNull BindingTrace trace
    ) {
        return resolveAnnotations(scope, modifierList, trace, false);
    }

    @NotNull
    public Annotations resolveAnnotationsWithArguments(
            @NotNull JetScope scope,
            @Nullable JetModifierList modifierList,
            @NotNull BindingTrace trace
    ) {
        return resolveAnnotations(scope, modifierList, trace, true);
    }

    @NotNull
    public Annotations resolveAnnotationsWithArguments(
            @NotNull JetScope scope,
            @NotNull List<JetAnnotationEntry> annotationEntries,
            @NotNull BindingTrace trace
    ) {
        return resolveAnnotationEntries(scope, annotationEntries, trace, true);
    }

    private Annotations resolveAnnotations(
            @NotNull JetScope scope,
            @Nullable JetModifierList modifierList,
            @NotNull BindingTrace trace,
            boolean shouldResolveArguments
    ) {
        if (modifierList == null) {
            return Annotations.EMPTY;
        }
        List<JetAnnotationEntry> annotationEntryElements = modifierList.getAnnotationEntries();

        return resolveAnnotationEntries(scope, annotationEntryElements, trace, shouldResolveArguments);
    }

    private Annotations resolveAnnotationEntries(
            @NotNull JetScope scope,
            @NotNull List<JetAnnotationEntry> annotationEntryElements, @NotNull BindingTrace trace,
            boolean shouldResolveArguments
    ) {
        if (annotationEntryElements.isEmpty()) return Annotations.EMPTY;
        List<AnnotationDescriptor> result = Lists.newArrayList();
        for (JetAnnotationEntry entryElement : annotationEntryElements) {
            AnnotationDescriptorImpl descriptor = trace.get(BindingContext.ANNOTATION, entryElement);
            if (descriptor == null) {
                descriptor = new AnnotationDescriptorImpl();
                resolveAnnotationStub(scope, entryElement, descriptor, trace);
                trace.record(BindingContext.ANNOTATION, entryElement, descriptor);
            }

            if (shouldResolveArguments) {
                resolveAnnotationArguments(entryElement, scope, trace);
            }

            result.add(descriptor);
        }
        return new AnnotationsImpl(result);
    }

    public void resolveAnnotationStub(
            @NotNull JetScope scope,
            @NotNull JetAnnotationEntry entryElement,
            @NotNull AnnotationDescriptorImpl annotationDescriptor,
            @NotNull BindingTrace trace
    ) {
        TemporaryBindingTrace temporaryBindingTrace = new TemporaryBindingTrace(trace, "Trace for resolve annotation type");
        OverloadResolutionResults<FunctionDescriptor> results = resolveAnnotationCall(entryElement, scope, temporaryBindingTrace);
        if (results.isSingleResult()) {
            FunctionDescriptor descriptor = results.getResultingDescriptor();
            if (!ErrorUtils.isError(descriptor)) {
                if (descriptor instanceof ConstructorDescriptor) {
                    ConstructorDescriptor constructor = (ConstructorDescriptor)descriptor;
                    ClassDescriptor classDescriptor = constructor.getContainingDeclaration();
                    if (classDescriptor.getKind() != ClassKind.ANNOTATION_CLASS) {
                        trace.report(Errors.NOT_AN_ANNOTATION_CLASS.on(entryElement, classDescriptor.getName().asString()));
                    }
                }
                else {
                    trace.report(Errors.NOT_AN_ANNOTATION_CLASS.on(entryElement, descriptor.getName().asString()));
                }
            }
            JetType annotationType = results.getResultingDescriptor().getReturnType();
            annotationDescriptor.setAnnotationType(annotationType);
        }
        else {
            JetConstructorCalleeExpression calleeExpression = entryElement.getCalleeExpression();
            annotationDescriptor.setAnnotationType(ErrorUtils.createErrorType("Unresolved annotation type: " +
                                                                              (calleeExpression == null ? "null" : calleeExpression.getText())));
        }
    }

    private OverloadResolutionResults<FunctionDescriptor> resolveAnnotationCall(
            JetAnnotationEntry annotationEntry,
            JetScope scope,
            BindingTrace trace
    ) {
        return callResolver.resolveFunctionCall(
                trace, scope,
                CallMaker.makeCall(ReceiverValue.NO_RECEIVER, null, annotationEntry),
                NO_EXPECTED_TYPE,
                DataFlowInfo.EMPTY,
                true);
    }

    public void resolveAnnotationsArguments(@NotNull JetScope scope, @Nullable JetModifierList modifierList, @NotNull BindingTrace trace) {
        if (modifierList == null) {
            return;
        }

        for (JetAnnotationEntry annotationEntry : modifierList.getAnnotationEntries()) {
            resolveAnnotationArguments(annotationEntry, scope, trace);
        }
    }

    public void resolveAnnotationsArguments(@NotNull Annotated descriptor, @NotNull BindingTrace trace, @NotNull JetScope scope) {
        for (AnnotationDescriptor annotationDescriptor : descriptor.getAnnotations()) {
            JetAnnotationEntry annotationEntry = trace.getBindingContext().get(ANNOTATION_DESCRIPTOR_TO_PSI_ELEMENT, annotationDescriptor);
            assert annotationEntry != null : "Cannot find annotation entry: " + annotationDescriptor;
            resolveAnnotationArguments(annotationEntry, scope, trace);
        }
    }

    private void resolveAnnotationArguments(
            @NotNull JetAnnotationEntry annotationEntry,
            @NotNull JetScope scope,
            @NotNull BindingTrace trace
    ) {
        OverloadResolutionResults<FunctionDescriptor> results = resolveAnnotationCall(annotationEntry, scope, trace);
        if (results.isSingleResult()) {
            AnnotationDescriptorImpl annotationDescriptor = trace.getBindingContext().get(BindingContext.ANNOTATION, annotationEntry);
            assert annotationDescriptor != null : "Annotation descriptor should be created before resolving arguments for " + annotationEntry.getText();
            resolveAnnotationArgument(annotationDescriptor, results.getResultingCall(), trace);
        }
    }

    public static void resolveAnnotationArgument(
            @NotNull AnnotationDescriptorImpl annotationDescriptor,
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull BindingTrace trace
    ) {
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> descriptorToArgument : resolvedCall.getValueArguments().entrySet()) {
            ValueParameterDescriptor parameterDescriptor = descriptorToArgument.getKey();

            JetType varargElementType = parameterDescriptor.getVarargElementType();
            boolean argumentsAsVararg = varargElementType != null && !hasSpread(descriptorToArgument.getValue());
            List<CompileTimeConstant<?>> constants = resolveValueArguments(descriptorToArgument.getValue(),
                                                                           argumentsAsVararg ? varargElementType : parameterDescriptor.getType(),
                                                                           trace);

            if (argumentsAsVararg) {
                JetType arrayType = KotlinBuiltIns.getInstance().getPrimitiveArrayJetTypeByPrimitiveJetType(varargElementType);
                if (arrayType == null) {
                    arrayType = KotlinBuiltIns.getInstance().getArrayType(varargElementType);
                }
                annotationDescriptor.setValueArgument(parameterDescriptor, new ArrayValue(constants, arrayType, true));
            }
            else {
                for (CompileTimeConstant<?> constant : constants) {
                    annotationDescriptor.setValueArgument(parameterDescriptor, constant);
                }
            }
        }
    }

    private static void checkCompileTimeConstant(
            @NotNull JetExpression argumentExpression,
            @NotNull JetType expectedType,
            @NotNull BindingTrace trace
    ) {
        JetType expressionType = trace.get(BindingContext.EXPRESSION_TYPE, argumentExpression);

        if (expressionType == null || !expressionType.equals(expectedType)) {
            // TYPE_MISMATCH should be reported otherwise
            return;
        }

        // array(1, <!>null<!>, 3) - error should be reported on inner expression
        if (argumentExpression instanceof JetCallExpression) {
            Pair<List<JetExpression>, JetType> arrayArgument = getArgumentExpressionsForArrayCall((JetCallExpression) argumentExpression, trace);
            if (arrayArgument != null) {
                for (JetExpression expression : arrayArgument.getFirst()) {
                    checkCompileTimeConstant(expression, arrayArgument.getSecond(), trace);
                }
            }
        }

        CompileTimeConstant<?> constant = trace.get(BindingContext.COMPILE_TIME_VALUE, argumentExpression);
        if (constant != null && constant.canBeUsedInAnnotations()) {
            return;
        }

        ClassifierDescriptor descriptor = expressionType.getConstructor().getDeclarationDescriptor();
        if (descriptor != null && DescriptorUtils.isEnumClass(descriptor)) {
            trace.report(Errors.ANNOTATION_PARAMETER_MUST_BE_ENUM_CONST.on(argumentExpression));
        }
        else if (descriptor instanceof ClassDescriptor && CompileTimeConstantUtils.isJavaLangClass((ClassDescriptor) descriptor)) {
            trace.report(Errors.ANNOTATION_PARAMETER_MUST_BE_CLASS_LITERAL.on(argumentExpression));
        }
        else {
            trace.report(Errors.ANNOTATION_PARAMETER_MUST_BE_CONST.on(argumentExpression));
        }
    }

    @Nullable
    private static Pair<List<JetExpression>, JetType> getArgumentExpressionsForArrayCall(
            @NotNull JetCallExpression expression,
            @NotNull BindingTrace trace
    ) {
        ResolvedCall<?> resolvedCall = trace.get(BindingContext.RESOLVED_CALL, (expression).getCalleeExpression());
        if (resolvedCall == null || !CompileTimeConstantUtils.isArrayMethodCall(resolvedCall)) {
            return null;
        }

        assert resolvedCall.getValueArguments().size() == 1 : "Array function should have only one vararg parameter";
        Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> argumentEntry = resolvedCall.getValueArguments().entrySet().iterator().next();

        List<JetExpression> result = Lists.newArrayList();
        JetType elementType = argumentEntry.getKey().getVarargElementType();
        for (ValueArgument valueArgument : argumentEntry.getValue().getArguments()) {
            JetExpression valueArgumentExpression = valueArgument.getArgumentExpression();
            if (valueArgumentExpression != null) {
                if (elementType != null) {
                    result.add(valueArgumentExpression);
                }
            }
        }
        return new Pair<List<JetExpression>, JetType>(result, elementType);
    }

    private static boolean hasSpread(@NotNull ResolvedValueArgument argument) {
        List<ValueArgument> arguments = argument.getArguments();
        return arguments.size() == 1 && arguments.get(0).getSpreadElement() != null;
    }

    @NotNull
    private static List<CompileTimeConstant<?>> resolveValueArguments(
            @NotNull ResolvedValueArgument resolvedValueArgument,
            @NotNull JetType expectedType,
            @NotNull BindingTrace trace
    ) {
        List<CompileTimeConstant<?>> constants = Lists.newArrayList();
        for (ValueArgument argument : resolvedValueArgument.getArguments()) {
            JetExpression argumentExpression = argument.getArgumentExpression();
            if (argumentExpression != null) {
                CompileTimeConstant<?> constant = ConstantExpressionEvaluator.object$.evaluate(argumentExpression, trace, expectedType);
                if (constant instanceof IntegerValueTypeConstant) {
                    JetType defaultType = ((IntegerValueTypeConstant) constant).getType(expectedType);
                    ArgumentTypeResolver.updateNumberType(defaultType, argumentExpression, trace);
                }
                if (constant != null) {
                    constants.add(constant);
                }
                checkCompileTimeConstant(argumentExpression, expectedType, trace);
            }
        }
        return constants;
    }

    @SuppressWarnings("MethodMayBeStatic")
    @NotNull
    public Annotations getResolvedAnnotations(@NotNull List<JetAnnotationEntry> annotations, @NotNull BindingTrace trace) {
        List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>(annotations.size());
        for (JetAnnotationEntry annotation : annotations) {
            AnnotationDescriptor annotationDescriptor = trace.get(BindingContext.ANNOTATION, annotation);
            if (annotationDescriptor == null) {
                throw new IllegalStateException("Annotation for annotation should have been resolved: " + annotation);
            }

            result.add(annotationDescriptor);
        }

        return new AnnotationsImpl(result);
    }

    public static void reportUnsupportedAnnotationForTypeParameter(@NotNull JetModifierListOwner modifierListOwner, BindingTrace trace) {
        JetModifierList modifierList = modifierListOwner.getModifierList();
        if (modifierList == null) return;

        for (JetAnnotationEntry annotationEntry : modifierList.getAnnotationEntries()) {
            trace.report(Errors.UNSUPPORTED.on(annotationEntry, "Annotations for type parameters are not supported yet"));
        }
    }
}
