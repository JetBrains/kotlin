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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.INVALID_TYPE_OF_ANNOTATION_MEMBER;
import static org.jetbrains.jet.lang.diagnostics.Errors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER;
import static org.jetbrains.jet.lang.resolve.BindingContext.VALUE_PARAMETER;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;

public class AnnotationUtils {

    public static void checkConstructorParametersType(@NotNull List<JetParameter> parameters, @NotNull BindingTrace trace) {
        for (JetParameter parameter : parameters) {
            VariableDescriptor parameterDescriptor = trace.getBindingContext().get(VALUE_PARAMETER, parameter);
            if (parameterDescriptor == null) continue;
            JetType parameterType = parameterDescriptor.getType();
            JetTypeReference typeReference = parameter.getTypeReference();
            if (typeReference != null) {
                if (parameterType.isNullable()) {
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
            builtIns.isPrimitiveArray(parameterType) ||
            builtIns.isPrimitiveType(parameterType) ||
            builtIns.getStringType().equals(parameterType)) {
                return true;
        }

        if (builtIns.isArray(parameterType)) {
            List<TypeProjection> arguments = parameterType.getArguments();
            if (arguments.size() == 1) {
                JetType arrayType = arguments.get(0).getType();
                if (arrayType.isNullable()) {
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

    public static boolean isArrayMethodCall(@NotNull ResolvedCall resolvedCall) {
        List<AnnotationDescriptor> annotations = resolvedCall.getResultingDescriptor().getOriginal().getAnnotations();
        if (annotations != null) {
            for (AnnotationDescriptor annotation : annotations) {
                //noinspection ConstantConditions
                if ("Intrinsic".equals(annotation.getType().getConstructor().getDeclarationDescriptor().getName().asString())) {
                    return "kotlin.arrays.array".equals(annotation.getAllValueArguments().values().iterator().next().getValue());
                }
            }
        }
        return false;
    }

    public static boolean isJavaClassMethodCall(@NotNull ResolvedCall resolvedCall) {
        List<AnnotationDescriptor> annotations = resolvedCall.getResultingDescriptor().getOriginal().getAnnotations();
        if (annotations != null) {
            for (AnnotationDescriptor annotation : annotations) {
                //noinspection ConstantConditions
                if ("Intrinsic".equals(annotation.getType().getConstructor().getDeclarationDescriptor().getName().asString())) {
                    return "kotlin.javaClass.function".equals(annotation.getAllValueArguments().values().iterator().next().getValue());
                }
            }
        }
        return false;
    }

    public static boolean isPropertyCompileTimeConstant(@NotNull PropertyDescriptor descriptor) {
        if (descriptor.isVar()) {
            return false;
        }
        if (isClassObject(descriptor.getContainingDeclaration()) || isTopLevelDeclaration(descriptor)) {
            JetType type = descriptor.getType();
            return KotlinBuiltIns.getInstance().isPrimitiveType(type) || KotlinBuiltIns.getInstance().getStringType().equals(type);
        }
        return false;
    }

    public static boolean isJavaLangClass(ClassDescriptor descriptor) {
        return "java.lang.Class".equals(DescriptorUtils.getFqName(descriptor).asString());
    }

    private AnnotationUtils() {
    }
}
