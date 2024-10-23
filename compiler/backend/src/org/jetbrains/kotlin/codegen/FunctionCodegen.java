/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.annotations.ThrowUtilKt;
import org.jetbrains.kotlin.resolve.constants.ArrayValue;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.KClassValue;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION;

public class FunctionCodegen {
    @NotNull
    public static List<ClassDescriptor> getThrownExceptions(
            @NotNull FunctionDescriptor function,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        if (function.getKind() == DELEGATION &&
            languageVersionSettings.supportsFeature(LanguageFeature.DoNotGenerateThrowsForDelegatedKotlinMembers)) {
            return Collections.emptyList();
        }

        AnnotationDescriptor annotation = function.getAnnotations().findAnnotation(ThrowUtilKt.JVM_THROWS_ANNOTATION_FQ_NAME);
        if (annotation == null) return Collections.emptyList();

        Collection<ConstantValue<?>> values = annotation.getAllValueArguments().values();
        if (values.isEmpty()) return Collections.emptyList();

        Object value = values.iterator().next();
        if (!(value instanceof ArrayValue)) return Collections.emptyList();
        ArrayValue arrayValue = (ArrayValue) value;

        return CollectionsKt.mapNotNull(
                arrayValue.getValue(),
                (ConstantValue<?> constant) -> {
                    if (constant instanceof KClassValue) {
                        return DescriptorUtils.getClassDescriptorForType(
                                ((KClassValue) constant).getArgumentType(DescriptorUtilsKt.getModule(function))
                        );
                    }
                    return null;
                }
        );
    }
}
