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

package org.jetbrains.kotlin.builtins;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.constants.ArrayValue;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.EnumValue;
import org.jetbrains.kotlin.types.JetType;

import static kotlin.KotlinPackage.firstOrNull;

public class InlineUtil {

    public static boolean hasNoinlineAnnotation(@NotNull CallableDescriptor valueParameterOrReceiver) {
        return KotlinBuiltIns.containsAnnotation(valueParameterOrReceiver, KotlinBuiltIns.getNoinlineClassAnnotationFqName());
    }

    public static boolean isInlineLambdaParameter(@NotNull CallableDescriptor valueParameterOrReceiver) {
        JetType type = valueParameterOrReceiver.getOriginal().getReturnType();
        return !hasNoinlineAnnotation(valueParameterOrReceiver) &&
               type != null &&
               KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type);
    }

    public static boolean isInline(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof SimpleFunctionDescriptor && getInlineStrategy(descriptor).isInline();
    }

    @NotNull
    public static InlineStrategy getInlineStrategy(@NotNull DeclarationDescriptor descriptor) {
        ClassDescriptor inlineAnnotation = KotlinBuiltIns.getInstance().getInlineClassAnnotation();
        AnnotationDescriptor annotation = descriptor.getAnnotations().findAnnotation(DescriptorUtils.getFqNameSafe(inlineAnnotation));
        if (annotation == null) {
            return InlineStrategy.NOT_INLINE;
        }
        CompileTimeConstant<?> argument = firstOrNull(annotation.getAllValueArguments().values());
        if (argument == null) {
            return InlineStrategy.AS_FUNCTION;
        }
        assert argument instanceof EnumValue : "Inline annotation parameter should be enum entry but was: " + argument;
        return InlineStrategy.valueOf(((EnumValue) argument).getValue().getName().asString());
    }

    public static boolean hasOnlyLocalContinueAndBreak(@NotNull ValueParameterDescriptor descriptor) {
        return hasInlineOption(descriptor, InlineOption.LOCAL_CONTINUE_AND_BREAK);
    }

    public static boolean hasOnlyLocalReturn(@NotNull ValueParameterDescriptor descriptor) {
        return hasInlineOption(descriptor, InlineOption.ONLY_LOCAL_RETURN);
    }

    private static boolean hasInlineOption(@NotNull ValueParameterDescriptor descriptor, @NotNull InlineOption option) {
        AnnotationDescriptor annotation = descriptor.getAnnotations().findAnnotation(
                DescriptorUtils.getFqNameSafe(KotlinBuiltIns.getInstance().getInlineOptionsClassAnnotation())
        );
        if (annotation != null) {
            CompileTimeConstant<?> argument = firstOrNull(annotation.getAllValueArguments().values());
            if (argument instanceof ArrayValue) {
                for (CompileTimeConstant<?> value : ((ArrayValue) argument).getValue()) {
                    if (value instanceof EnumValue && ((EnumValue) value).getValue().getName().asString().equals(option.name())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
