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
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContext.ANNOTATION_DESCRIPTOR_TO_PSI_ELEMENT;
import static org.jetbrains.jet.lang.resolve.BindingContext.COMPILE_TIME_INITIALIZER;
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

    private void resolveAnnotationArgument(
            @NotNull AnnotationDescriptorImpl annotationDescriptor,
            @NotNull ResolvedCall<? extends CallableDescriptor> call,
            @NotNull BindingTrace trace
    ) {
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> descriptorToArgument :
                call.getValueArguments().entrySet()) {
            ValueParameterDescriptor parameterDescriptor = descriptorToArgument.getKey();

            JetType varargElementType = parameterDescriptor.getVarargElementType();
            List<CompileTimeConstant<?>> constants = resolveValueArguments(descriptorToArgument.getValue(), parameterDescriptor.getType(), trace);
            if (varargElementType == null) {
                for (CompileTimeConstant<?> constant : constants) {
                    annotationDescriptor.setValueArgument(parameterDescriptor, constant);
                }
            }
            else {
                JetType arrayType = KotlinBuiltIns.getInstance().getPrimitiveArrayJetTypeByPrimitiveJetType(varargElementType);
                if (arrayType == null) {
                    arrayType = KotlinBuiltIns.getInstance().getArrayType(varargElementType);
                }
                annotationDescriptor.setValueArgument(parameterDescriptor, new ArrayValue(constants, arrayType));
            }
        }
    }

    @NotNull
    private List<CompileTimeConstant<?>> resolveValueArguments(
            @NotNull ResolvedValueArgument resolvedValueArgument,
            @NotNull JetType expectedType,
            @NotNull BindingTrace trace
    ) {
        List<CompileTimeConstant<?>> constants = Lists.newArrayList();
        for (ValueArgument argument : resolvedValueArgument.getArguments()) {
            JetExpression argumentExpression = argument.getArgumentExpression();
            if (argumentExpression != null) {
                CompileTimeConstant<?> constant = resolveExpressionToCompileTimeValue(argumentExpression, expectedType, trace);
                if (constant != null) {
                    constants.add(constant);
                }
            }
        }
        return constants;
    }

    @Nullable
    public CompileTimeConstant<?> resolveExpressionToCompileTimeValue(
            @NotNull JetExpression expression,
            @NotNull final JetType expectedType,
            @NotNull final BindingTrace trace
    ) {
        JetVisitor<CompileTimeConstant<?>, Void> visitor = new JetVisitor<CompileTimeConstant<?>, Void>() {
            @Override
            public CompileTimeConstant<?> visitConstantExpression(@NotNull JetConstantExpression expression, Void nothing) {
                JetType type = expressionTypingServices.getType(JetScope.EMPTY, expression, expectedType, DataFlowInfo.EMPTY, trace);
                if (type == null) {
                    // TODO:
                    //  trace.report(ANNOTATION_PARAMETER_SHOULD_BE_CONSTANT.on(expression));
                }
                return trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
            }


            // @Override
//            public CompileTimeConstant visitAnnotation(JetAnnotation annotation, Void nothing) {
//                super.visitAnnotation(annotation, null); // TODO
//            }
//
//            @Override
//            public CompileTimeConstant visitAnnotationEntry(JetAnnotationEntry annotationEntry, Void nothing) {
//                return super.visitAnnotationEntry(annotationEntry, null); // TODO
//            }

            @Override
            public CompileTimeConstant<?> visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, Void nothing) {
                JetExpression innerExpression = expression.getExpression();
                if (innerExpression == null) return null;
                return innerExpression.accept(this, null);
            }

            @Override
            public CompileTimeConstant<?> visitStringTemplateExpression(
                    @NotNull JetStringTemplateExpression expression,
                                                                        Void nothing) {
                return trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
            }

            @Override
            public CompileTimeConstant<?> visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, Void data) {
                ResolvedCall<? extends CallableDescriptor> resolvedCall =
                        trace.getBindingContext().get(BindingContext.RESOLVED_CALL, expression);
                if (resolvedCall != null) {
                    CallableDescriptor callableDescriptor = resolvedCall.getResultingDescriptor();
                    if (callableDescriptor instanceof PropertyDescriptor) {
                        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) callableDescriptor;
                        if (isEnumProperty(propertyDescriptor)) {
                            return new EnumValue(propertyDescriptor);
                        }
                        if (AnnotationUtils.isPropertyAcceptableAsAnnotationParameter(propertyDescriptor)) {
                            return trace.getBindingContext().get(COMPILE_TIME_INITIALIZER, propertyDescriptor);
                        }
                    }
                }
                return null;
            }

            @Override
            public CompileTimeConstant<?> visitQualifiedExpression(@NotNull JetQualifiedExpression expression, Void data) {
                JetExpression selectorExpression = expression.getSelectorExpression();
                if (selectorExpression != null) {
                    return selectorExpression.accept(this, null);
                }
                return super.visitQualifiedExpression(expression, data);
            }

            @Override
            public CompileTimeConstant<?> visitCallExpression(@NotNull JetCallExpression expression, Void data) {
                ResolvedCall<? extends CallableDescriptor> call =
                        trace.getBindingContext().get(BindingContext.RESOLVED_CALL, (expression).getCalleeExpression());
                if (call != null) {
                    CallableDescriptor resultingDescriptor = call.getResultingDescriptor();
                    if (AnnotationUtils.isArrayMethodCall(call)) {
                        JetType type = resultingDescriptor.getValueParameters().iterator().next().getVarargElementType();
                        List<CompileTimeConstant<?>> arguments = Lists.newArrayList();
                        for (ResolvedValueArgument descriptorToArgument : call.getValueArguments().values()) {
                            arguments.addAll(resolveValueArguments(descriptorToArgument, type, trace));
                        }
                        return new ArrayValue(arguments, resultingDescriptor.getReturnType());
                    }

                    if (resultingDescriptor instanceof ConstructorDescriptor) {
                        JetType constructorReturnType = resultingDescriptor.getReturnType();
                        assert constructorReturnType != null : "Constructor should have return type";
                        if (DescriptorUtils.isAnnotationClass(constructorReturnType.getConstructor().getDeclarationDescriptor())) {
                            AnnotationDescriptorImpl descriptor = new AnnotationDescriptorImpl();
                            descriptor.setAnnotationType(constructorReturnType);
                            resolveAnnotationArgument(descriptor, call, trace);
                            return new AnnotationValue(descriptor);
                        }
                    }

                    if (AnnotationUtils.isJavaClassMethodCall(call)) {
                        return new JavaClassValue(resultingDescriptor.getReturnType());
                    }
                }
                return null;
            }

            @Override
            public CompileTimeConstant<?> visitJetElement(@NotNull JetElement element, Void nothing) {
                // TODO:
                //trace.report(ANNOTATION_PARAMETER_SHOULD_BE_CONSTANT.on(element));
                return null;
            }
        };
        return expression.accept(visitor, null);
    }

    private static boolean isEnumProperty(@NotNull PropertyDescriptor descriptor) {
        ClassifierDescriptor classifier = descriptor.getType().getConstructor().getDeclarationDescriptor();
        return classifier != null &&
               DescriptorUtils.isEnumClass(classifier) &&
               DescriptorUtils.isEnumClassObject(descriptor.getContainingDeclaration());
    }

    @NotNull
    public List<AnnotationDescriptor> getResolvedAnnotations(@Nullable JetModifierList modifierList, BindingTrace trace) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        return getResolvedAnnotations(modifierList.getAnnotationEntries(), trace);
    }

    @SuppressWarnings("MethodMayBeStatic")
    @NotNull
    public List<AnnotationDescriptor> getResolvedAnnotations(List<JetAnnotationEntry> annotations, BindingTrace trace) {
        List<AnnotationDescriptor> result = Lists.newArrayList();
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
