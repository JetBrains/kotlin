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

package org.jetbrains.jet.lang.types.lang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.constants.ArrayValue;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.EnumValue;

import java.util.List;

public class InlineUtil {

    public static boolean hasNoinlineAnnotation(@NotNull CallableDescriptor valueParameterDescriptor) {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        return KotlinBuiltIns.containsAnnotation(valueParameterDescriptor, builtIns.getNoinlineClassAnnotation());
    }

    @NotNull
    public static InlineStrategy getInlineType(@Nullable Annotations annotations) {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        ClassDescriptor annotationClass = builtIns.getInlineClassAnnotation();
        AnnotationDescriptor annotation = getAnnotation(annotations, annotationClass);
        if (annotation != null) {
            ValueParameterDescriptor parameterDescriptor = annotationClass.getConstructors().iterator().next().getValueParameters().get(0);
            CompileTimeConstant<?> argument = annotation.getValueArgument(parameterDescriptor);
            if (argument == null) {
                //default parameter
                return InlineStrategy.AS_FUNCTION;
            }
            else {
                assert argument instanceof EnumValue : "Inline annotation parameter should be inline entry but was: " + argument + "!";
                String name = ((EnumValue) argument).getValue().getName().asString();
                return name.equals(InlineStrategy.IN_PLACE.name()) ? InlineStrategy.IN_PLACE : InlineStrategy.AS_FUNCTION;
            }
        }
        else {
            return InlineStrategy.NOT_INLINE;
        }
    }

    @Nullable
    private static AnnotationDescriptor getAnnotation(@Nullable Annotations annotations, @NotNull ClassDescriptor annotationClass) {
        if (annotations != null) {
            for (AnnotationDescriptor annotation : annotations) {
                if (annotationClass.equals(annotation.getType().getConstructor().getDeclarationDescriptor())) {
                    return annotation;
                }
            }
        }
        return null;
    }

    public static boolean hasOnlyLocalContinueAndBreak(@NotNull ValueParameterDescriptor descriptor) {
        return hasInlineOption(descriptor, InlineOption.LOCAL_CONTINUE_AND_BREAK);
    }

    public static boolean hasOnlyLocalReturn(@NotNull ValueParameterDescriptor descriptor) {
        return hasInlineOption(descriptor, InlineOption.ONLY_LOCAL_RETURN);
    }

    private static boolean hasInlineOption(@NotNull ValueParameterDescriptor descriptor, @NotNull InlineOption option) {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        ClassDescriptor annotationClass = builtIns.getInlineOptionsClassAnnotation();
        AnnotationDescriptor optionsAnnotation = getAnnotation(descriptor.getAnnotations(), annotationClass);

        if (optionsAnnotation != null) {
            ValueParameterDescriptor parameterDescriptor = annotationClass.getConstructors().iterator().next().getValueParameters().get(0);
            CompileTimeConstant<?> argument = optionsAnnotation.getValueArgument(parameterDescriptor);

            if (argument == null) {
                return false;
            } else {
                if (argument instanceof ArrayValue) {
                    List<CompileTimeConstant<?>> values = ((ArrayValue) argument).getValue();

                    for (CompileTimeConstant<?> value : values) {
                        if (value instanceof EnumValue) {
                            if (((EnumValue) value).getValue().getName().asString().equals(option.name())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}

