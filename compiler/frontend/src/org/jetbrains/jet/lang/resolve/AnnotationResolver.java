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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author abreslav
 */
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
    public List<AnnotationDescriptor> resolveAnnotations(@NotNull JetScope scope, @Nullable JetModifierList modifierList, BindingTrace trace) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        return resolveAnnotations(scope, modifierList.getAnnotationEntries(), trace);
    }

    @NotNull
    public List<AnnotationDescriptor> resolveAnnotations(@NotNull JetScope scope, @NotNull List<JetAnnotationEntry> annotationEntryElements, BindingTrace trace) {
        if (annotationEntryElements.isEmpty()) return Collections.emptyList();
        List<AnnotationDescriptor> result = Lists.newArrayList();
        for (JetAnnotationEntry entryElement : annotationEntryElements) {
            AnnotationDescriptor descriptor = new AnnotationDescriptor();
            resolveAnnotationStub(scope, entryElement, descriptor, trace);
            trace.record(BindingContext.ANNOTATION, entryElement, descriptor);
            result.add(descriptor);
        }
        return result;
    }

    public void resolveAnnotationStub(@NotNull JetScope scope, @NotNull JetAnnotationEntry entryElement,
            @NotNull AnnotationDescriptor descriptor, BindingTrace trace) {
        OverloadResolutionResults<FunctionDescriptor> results = resolveType(scope, entryElement, descriptor, trace);
        resolveArguments(results, descriptor, trace);
    }

    @NotNull
    private OverloadResolutionResults<FunctionDescriptor> resolveType(@NotNull JetScope scope,
            @NotNull JetAnnotationEntry entryElement,
            @NotNull AnnotationDescriptor descriptor, BindingTrace trace) {
        OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveFunctionCall(trace, scope, CallMaker.makeCall(ReceiverDescriptor.NO_RECEIVER, null, entryElement), NO_EXPECTED_TYPE, DataFlowInfo.EMPTY);
        JetType annotationType = results.getResultingDescriptor().getReturnType();
        if (results.isSuccess()) {
            descriptor.setAnnotationType(annotationType);
        } else {
            descriptor.setAnnotationType(ErrorUtils.createErrorType("Unresolved annotation type"));
        }
        return results;
    }

    private void resolveArguments(@NotNull OverloadResolutionResults<FunctionDescriptor> results,
            @NotNull AnnotationDescriptor descriptor, BindingTrace trace) {
        List<CompileTimeConstant<?>> arguments = Lists.newArrayList();
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> descriptorToArgument :
                results.getResultingCall().getValueArguments().entrySet()) {
            // TODO: are varargs supported here?
            List<ValueArgument> valueArguments = descriptorToArgument.getValue().getArguments();
            ValueParameterDescriptor parameterDescriptor = descriptorToArgument.getKey();
            for (ValueArgument argument : valueArguments) {
                arguments.add(resolveAnnotationArgument(argument.getArgumentExpression(), parameterDescriptor.getType(), trace));
            }
        }
        descriptor.setValueArguments(arguments);
    }

    @Nullable
    public CompileTimeConstant<?> resolveAnnotationArgument(@NotNull JetExpression expression, @NotNull final JetType expectedType, final BindingTrace trace) {
        JetVisitor<CompileTimeConstant<?>, Void> visitor = new JetVisitor<CompileTimeConstant<?>, Void>() {
            @Override
            public CompileTimeConstant<?> visitConstantExpression(JetConstantExpression expression, Void nothing) {
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
            public CompileTimeConstant<?> visitParenthesizedExpression(JetParenthesizedExpression expression, Void nothing) {
                JetExpression innerExpression = expression.getExpression();
                if (innerExpression == null) return null;
                return innerExpression.accept(this, null);
            }

            @Override
            public CompileTimeConstant<?> visitStringTemplateExpression(JetStringTemplateExpression expression,
                                                                        Void nothing) {
                return trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
            }

            @Override
            public CompileTimeConstant<?> visitJetElement(JetElement element, Void nothing) {
                // TODO:
                //trace.report(ANNOTATION_PARAMETER_SHOULD_BE_CONSTANT.on(element));
                return null;
            }
        };
        return expression.accept(visitor, null);
    }

    @NotNull
    public List<AnnotationDescriptor> createAnnotationStubs(@Nullable JetModifierList modifierList, BindingTrace trace) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        return createAnnotationStubs(modifierList.getAnnotationEntries(), trace);
    }

    @NotNull
    public List<AnnotationDescriptor> createAnnotationStubs(List<JetAnnotationEntry> annotations, BindingTrace trace) {
        List<AnnotationDescriptor> result = Lists.newArrayList();
        for (JetAnnotationEntry annotation : annotations) {
            AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor();
            result.add(annotationDescriptor);
            trace.record(BindingContext.ANNOTATION, annotation, annotationDescriptor);
        }
        return result;
    }
}
