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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetParameter;
import org.jetbrains.kotlin.psi.JetTypeReference;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.BooleanValue;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.INVALID_TYPE_OF_ANNOTATION_MEMBER;
import static org.jetbrains.kotlin.diagnostics.Errors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER;
import static org.jetbrains.kotlin.resolve.BindingContext.VALUE_PARAMETER;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isAnnotationClass;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass;

public class CompileTimeConstantUtils {

    public static void checkConstructorParametersType(@NotNull List<JetParameter> parameters, @NotNull BindingTrace trace) {
        for (JetParameter parameter : parameters) {
            VariableDescriptor parameterDescriptor = trace.getBindingContext().get(VALUE_PARAMETER, parameter);
            if (parameterDescriptor == null) continue;
            JetType parameterType = parameterDescriptor.getType();
            JetTypeReference typeReference = parameter.getTypeReference();
            if (typeReference != null) {
                if (parameterType.isMarkedNullable()) {
                    trace.report(NULLABLE_TYPE_OF_ANNOTATION_MEMBER.on(typeReference));
                }
                else if (!isAcceptableTypeForAnnotationParameter(parameterType)) {
                    trace.report(INVALID_TYPE_OF_ANNOTATION_MEMBER.on(typeReference));
                }
            }
        }
    }

    private static boolean isAcceptableTypeForAnnotationParameter(@NotNull JetType parameterType) {
        ClassDescriptor typeDescriptor = TypeUtils.getClassDescriptor(parameterType);
        if (typeDescriptor == null) {
            return false;
        }

        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        if (isEnumClass(typeDescriptor) ||
            isAnnotationClass(typeDescriptor) ||
            isJavaLangClass(typeDescriptor) ||
            KotlinBuiltIns.isPrimitiveArray(parameterType) ||
            KotlinBuiltIns.isPrimitiveType(parameterType) ||
            builtIns.getStringType().equals(parameterType)) {
                return true;
        }

        if (KotlinBuiltIns.isArray(parameterType)) {
            List<TypeProjection> arguments = parameterType.getArguments();
            if (arguments.size() == 1) {
                JetType arrayType = arguments.get(0).getType();
                if (arrayType.isMarkedNullable()) {
                    return false;
                }
                ClassDescriptor arrayTypeDescriptor = TypeUtils.getClassDescriptor(arrayType);
                if (arrayTypeDescriptor != null) {
                    return isEnumClass(arrayTypeDescriptor) ||
                           isAnnotationClass(arrayTypeDescriptor) ||
                           isJavaLangClass(arrayTypeDescriptor) ||
                           builtIns.getStringType().equals(arrayType);
                }
            }
        }
        return false;
    }

    @Nullable
    public static String getIntrinsicAnnotationArgument(@NotNull Annotated annotatedDescriptor) {
        AnnotationDescriptor intrinsicAnnotation =
                annotatedDescriptor.getAnnotations().findAnnotation(new FqName("kotlin.jvm.internal.Intrinsic"));
        if (intrinsicAnnotation == null) return null;

        Collection<CompileTimeConstant<?>> values = intrinsicAnnotation.getAllValueArguments().values();
        if (values.isEmpty()) return null;

        Object value = values.iterator().next().getValue();
        return value instanceof String ? (String) value : null;
    }

    public static boolean isArrayMethodCall(@NotNull ResolvedCall<?> resolvedCall) {
        return "kotlin.arrays.array".equals(getIntrinsicAnnotationArgument(resolvedCall.getResultingDescriptor().getOriginal()));
    }

    public static boolean isJavaClassMethodCall(@NotNull ResolvedCall<?> resolvedCall) {
        return "kotlin.javaClass.function".equals(getIntrinsicAnnotationArgument(resolvedCall.getResultingDescriptor().getOriginal()));
    }

    public static boolean isJavaLangClass(ClassDescriptor descriptor) {
        return "java.lang.Class".equals(DescriptorUtils.getFqName(descriptor).asString());
    }

    public static boolean canBeReducedToBooleanConstant(
            @Nullable JetExpression expression,
            @NotNull BindingTrace trace,
            @Nullable Boolean expectedValue
    ) {
        if (expression == null) return false;
        CompileTimeConstant<?> compileTimeConstant =
                ConstantExpressionEvaluator.OBJECT$.evaluate(expression, trace, KotlinBuiltIns.getInstance().getBooleanType());
        if (!(compileTimeConstant instanceof BooleanValue) || compileTimeConstant.usesVariableAsConstant()) return false;

        Boolean value = ((BooleanValue) compileTimeConstant).getValue();
        return expectedValue == null || expectedValue.equals(value);
    }

    private CompileTimeConstantUtils() {
    }
}
