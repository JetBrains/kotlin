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

package org.jetbrains.jet.lang.evaluate;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import javax.rmi.CORBA.ClassDesc;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContext.COMPILE_TIME_INITIALIZER;

public class ConstantExpressionEvaluator extends JetVisitor<CompileTimeConstant<?>, Void> {
    @NotNull private final BindingTrace trace;
    @NotNull private final JetType expectedType;

    public ConstantExpressionEvaluator(@NotNull BindingTrace trace, @NotNull JetType expectedType) {
        this.trace = trace;
        this.expectedType = expectedType;
    }

    @Override
    public CompileTimeConstant<?> visitConstantExpression(@NotNull JetConstantExpression expression, Void nothing) {
        return trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
    }

    @Override
    public CompileTimeConstant<?> visitParenthesizedExpression(@NotNull JetParenthesizedExpression expression, Void nothing) {
        JetExpression innerExpression = expression.getExpression();
        if (innerExpression == null) return null;
        return innerExpression.accept(this, null);
    }

    @Override
    public CompileTimeConstant<?> visitStringTemplateExpression(@NotNull JetStringTemplateExpression expression, Void nothing) {
        return trace.get(BindingContext.COMPILE_TIME_VALUE, expression);
    }

    @Override
    public CompileTimeConstant<?> visitSimpleNameExpression(@NotNull JetSimpleNameExpression expression, Void data) {
        DeclarationDescriptor descriptor = trace.getBindingContext().get(BindingContext.REFERENCE_TARGET, expression);
        if (descriptor != null && DescriptorUtils.isEnumEntry(descriptor)) {
            return new EnumValue((ClassDescriptor) descriptor);
        }

        ResolvedCall<? extends CallableDescriptor> resolvedCall =
                trace.getBindingContext().get(BindingContext.RESOLVED_CALL, expression);
        if (resolvedCall != null) {
            CallableDescriptor callableDescriptor = resolvedCall.getResultingDescriptor();
            if (callableDescriptor instanceof PropertyDescriptor) {
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) callableDescriptor;
                if (AnnotationUtils.isPropertyAcceptableAsAnnotationParameter(propertyDescriptor)) {
                    return trace.getBindingContext().get(COMPILE_TIME_INITIALIZER, propertyDescriptor);
                }
            }
        }

        return null;
    }

    @Override
    public CompileTimeConstant<?> visitQualifiedExpression(@NotNull JetQualifiedExpression expression, Void data) {
        JetExpression receiverExpression = expression.getReceiverExpression();
        JetExpression selectorExpression = expression.getSelectorExpression();
        if (receiverExpression instanceof JetConstantExpression && selectorExpression instanceof JetCallExpression) {
            CompileTimeConstant<?> constant = ConstantsPackage.propagateConstantValues(expression, trace, (JetCallExpression) selectorExpression);
            if (constant != null) {
                return constant;
            }
        }

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
                    arguments.addAll(resolveArguments(descriptorToArgument.getArguments(), type, trace));
                }
                return new ArrayValue(arguments, resultingDescriptor.getReturnType());
            }

            if (resultingDescriptor instanceof ConstructorDescriptor) {
                JetType constructorReturnType = resultingDescriptor.getReturnType();
                assert constructorReturnType != null : "Constructor should have return type";
                if (DescriptorUtils.isAnnotationClass(constructorReturnType.getConstructor().getDeclarationDescriptor())) {
                    AnnotationDescriptorImpl descriptor = new AnnotationDescriptorImpl();
                    descriptor.setAnnotationType(constructorReturnType);
                    AnnotationResolver.resolveAnnotationArgument(descriptor, call, trace);
                    return new AnnotationValue(descriptor);
                }
            }

            if (AnnotationUtils.isJavaClassMethodCall(call)) {
                return new JavaClassValue(resultingDescriptor.getReturnType());
            }
        }
        return null;
    }

    @NotNull
    private static List<CompileTimeConstant<?>> resolveArguments(@NotNull List<ValueArgument> valueArguments, @NotNull JetType expectedType, @NotNull BindingTrace trace) {
        List<CompileTimeConstant<?>> constants = Lists.newArrayList();
        for (ValueArgument argument : valueArguments) {
            JetExpression argumentExpression = argument.getArgumentExpression();
            if (argumentExpression != null) {
                CompileTimeConstant<?> constant = argumentExpression.accept(new ConstantExpressionEvaluator(trace, expectedType), null);
                if (constant != null) {
                    constants.add(constant);
                }
            }
        }
        return constants;
    }

    @Override
    public CompileTimeConstant<?> visitJetElement(@NotNull JetElement element, Void nothing) {
        return null;
    }
}
