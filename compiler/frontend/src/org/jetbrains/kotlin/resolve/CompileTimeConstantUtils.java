/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import kotlin.collections.SetsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtPsiUtil;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.BooleanValue;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.KotlinTypeKt;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.INVALID_TYPE_OF_ANNOTATION_MEMBER;
import static org.jetbrains.kotlin.diagnostics.Errors.NULLABLE_TYPE_OF_ANNOTATION_MEMBER;
import static org.jetbrains.kotlin.resolve.BindingContext.VALUE_PARAMETER;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isAnnotationClass;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass;

public class CompileTimeConstantUtils {

    private final static Set<String> ARRAY_CALL_NAMES = SetsKt.hashSetOf(
            "kotlin.arrayOf",
            "kotlin.doubleArrayOf",
            "kotlin.floatArrayOf",
            "kotlin.longArrayOf",
            "kotlin.intArrayOf",
            "kotlin.charArrayOf",
            "kotlin.shortArrayOf",
            "kotlin.byteArrayOf",
            "kotlin.booleanArrayOf",
            "kotlin.emptyArray"
    );

    public static void checkConstructorParametersType(@NotNull List<KtParameter> parameters, @NotNull BindingTrace trace) {
        for (KtParameter parameter : parameters) {
            VariableDescriptor parameterDescriptor = trace.getBindingContext().get(VALUE_PARAMETER, parameter);
            if (parameterDescriptor == null) continue;
            KotlinType parameterType = parameterDescriptor.getType();
            KtTypeReference typeReference = parameter.getTypeReference();
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

    private static boolean isAcceptableTypeForAnnotationParameter(@NotNull KotlinType parameterType) {
        if (KotlinTypeKt.isError(parameterType)) return true;

        ClassDescriptor typeDescriptor = TypeUtils.getClassDescriptor(parameterType);
        if (typeDescriptor == null) return false;

        if (isEnumClass(typeDescriptor) ||
            isAnnotationClass(typeDescriptor) ||
            KotlinBuiltIns.isKClass(typeDescriptor) ||
            KotlinBuiltIns.isPrimitiveArray(parameterType) ||
            KotlinBuiltIns.isPrimitiveType(parameterType) ||
            KotlinBuiltIns.isString(parameterType)) {
            return true;
        }

        if (KotlinBuiltIns.isArray(parameterType)) {
            List<TypeProjection> arguments = parameterType.getArguments();
            if (arguments.size() == 1) {
                KotlinType arrayType = arguments.get(0).getType();
                if (arrayType.isMarkedNullable()) {
                    return false;
                }
                ClassDescriptor arrayTypeDescriptor = TypeUtils.getClassDescriptor(arrayType);
                if (arrayTypeDescriptor != null) {
                    return isEnumClass(arrayTypeDescriptor) ||
                           isAnnotationClass(arrayTypeDescriptor) ||
                           KotlinBuiltIns.isKClass(arrayTypeDescriptor) ||
                           KotlinBuiltIns.isString(arrayType);
                }
            }
        }

        return false;
    }

    public static boolean isArrayFunctionCall(@NotNull ResolvedCall<?> resolvedCall) {
        return ARRAY_CALL_NAMES.contains(DescriptorUtils.getFqName(resolvedCall.getCandidateDescriptor()).asString());
    }

    public static boolean canBeReducedToBooleanConstant(
            @Nullable KtExpression expression,
            @NotNull BindingContext context,
            @Nullable Boolean expectedValue
    ) {
        KtExpression effectiveExpression = KtPsiUtil.deparenthesize(expression);

        if (effectiveExpression == null) return false;

        CompileTimeConstant<?> compileTimeConstant = ConstantExpressionEvaluator.getConstant(effectiveExpression, context);
        if (!(compileTimeConstant instanceof TypedCompileTimeConstant) || compileTimeConstant.getUsesVariableAsConstant()) return false;

        ConstantValue constantValue = ((TypedCompileTimeConstant) compileTimeConstant).getConstantValue();

        if (!(constantValue instanceof BooleanValue)) return false;

        Boolean value = ((BooleanValue) constantValue).getValue();
        return expectedValue == null || expectedValue.equals(value);
    }

    private CompileTimeConstantUtils() {
    }
}
