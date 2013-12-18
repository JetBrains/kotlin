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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl;
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
import org.jetbrains.jet.lang.resolve.constants.IntegerValueTypeConstructor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.BindingContext.ANNOTATION_DESCRIPTOR_TO_PSI_ELEMENT;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;
import static org.jetbrains.jet.lang.types.TypeUtils.getPrimitiveNumberType;

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
    public List<AnnotationDescriptor> resolveAnnotationsWithoutArguments(
            @NotNull JetScope scope,
            @Nullable JetModifierList modifierList,
            @NotNull BindingTrace trace
    ) {
        return resolveAnnotations(scope, modifierList, trace, false);
    }

    @NotNull
    public List<AnnotationDescriptor> resolveAnnotationsWithArguments(
            @NotNull JetScope scope,
            @Nullable JetModifierList modifierList,
            @NotNull BindingTrace trace
    ) {
        return resolveAnnotations(scope, modifierList, trace, true);
    }

    @NotNull
    public List<AnnotationDescriptor> resolveAnnotationsWithArguments(
            @NotNull JetScope scope,
            @NotNull List<JetAnnotationEntry> annotationEntries,
            @NotNull BindingTrace trace
    ) {
        return resolveAnnotationEntries(scope, annotationEntries, trace, true);
    }

    private List<AnnotationDescriptor> resolveAnnotations(
            @NotNull JetScope scope,
            @Nullable JetModifierList modifierList,
            @NotNull BindingTrace trace,
            boolean shouldResolveArguments
    ) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        List<JetAnnotationEntry> annotationEntryElements = modifierList.getAnnotationEntries();

        return resolveAnnotationEntries(scope, annotationEntryElements, trace, shouldResolveArguments);
    }

    private List<AnnotationDescriptor> resolveAnnotationEntries(
            @NotNull JetScope scope,
            @NotNull List<JetAnnotationEntry> annotationEntryElements, @NotNull BindingTrace trace,
            boolean shouldResolveArguments
    ) {
        if (annotationEntryElements.isEmpty()) return Collections.emptyList();
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
        return result;
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
            @NotNull ResolvedCall<? extends CallableDescriptor> call,
            @NotNull BindingTrace trace
    ) {
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> descriptorToArgument :
                call.getValueArguments().entrySet()) {
            ValueParameterDescriptor parameterDescriptor = descriptorToArgument.getKey();

            JetType varargElementType = parameterDescriptor.getVarargElementType();
            List<CompileTimeConstant<?>> constants = resolveValueArguments(descriptorToArgument.getValue(), parameterDescriptor.getType(), trace);

            if (varargElementType != null && !hasSpread(descriptorToArgument.getValue())) {
                JetType arrayType = KotlinBuiltIns.getInstance().getPrimitiveArrayJetTypeByPrimitiveJetType(varargElementType);
                if (arrayType == null) {
                    arrayType = KotlinBuiltIns.getInstance().getArrayType(varargElementType);
                }
                annotationDescriptor.setValueArgument(parameterDescriptor, new ArrayValue(constants, arrayType));
            }
            else {
                for (CompileTimeConstant<?> constant : constants) {
                    annotationDescriptor.setValueArgument(parameterDescriptor, constant);
                }
            }
        }
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
                    IntegerValueTypeConstructor typeConstructor = ((IntegerValueTypeConstant) constant).getValue();
                    JetType defaultType = getPrimitiveNumberType(typeConstructor, expectedType);
                    ArgumentTypeResolver.updateNumberType(defaultType, argumentExpression, trace);
                }
                if (constant != null) {
                    constants.add(constant);
                }
                else {
                    JetType expressionType = trace.get(BindingContext.EXPRESSION_TYPE, argumentExpression);
                    if (expressionType != null && expressionType.equals(expectedType)) {
                        ClassifierDescriptor descriptor = expressionType.getConstructor().getDeclarationDescriptor();
                        if (descriptor != null && DescriptorUtils.isEnumClass(descriptor)) {
                            trace.report(Errors.ANNOTATION_PARAMETER_MUST_BE_ENUM_CONST.on(argumentExpression));
                        }
                        else if (descriptor instanceof ClassDescriptor && AnnotationUtils.isJavaLangClass((ClassDescriptor) descriptor)) {
                            trace.report(Errors.ANNOTATION_PARAMETER_MUST_BE_CLASS_LITERAL.on(argumentExpression));
                        }
                        else {
                            trace.report(Errors.ANNOTATION_PARAMETER_MUST_BE_CONST.on(argumentExpression));
                        }
                    }
                }
            }
        }
        return constants;
    }

    @SuppressWarnings("MethodMayBeStatic")
    @NotNull
    public List<AnnotationDescriptor> getResolvedAnnotations(@NotNull List<JetAnnotationEntry> annotations, @NotNull BindingTrace trace) {
        List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>(annotations.size());
        for (JetAnnotationEntry annotation : annotations) {
            AnnotationDescriptor annotationDescriptor = trace.get(BindingContext.ANNOTATION, annotation);
            if (annotationDescriptor == null) {
                throw new IllegalStateException("Annotation for annotation should have been resolved: " + annotation);
            }

            result.add(annotationDescriptor);
        }

        return result;
    }

    public static void reportUnsupportedAnnotationForTypeParameter(@NotNull JetModifierListOwner modifierListOwner, BindingTrace trace) {
        JetModifierList modifierList = modifierListOwner.getModifierList();
        if (modifierList == null) return;

        for (JetAnnotationEntry annotationEntry : modifierList.getAnnotationEntries()) {
            trace.report(Errors.UNSUPPORTED.on(annotationEntry, "Annotations for type parameters are not supported yet"));
        }
    }
}
