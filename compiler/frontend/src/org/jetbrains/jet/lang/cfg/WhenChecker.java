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

package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptorForObject;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getEnumEntriesScope;

public final class WhenChecker {
    private WhenChecker() {
    }

    public static boolean isWhenExhaustive(@NotNull JetWhenExpression expression, @NotNull BindingTrace trace) {
        JetExpression subjectExpression = expression.getSubjectExpression();
        if (subjectExpression == null) return false;
        JetType type = trace.get(BindingContext.EXPRESSION_TYPE, subjectExpression);
        if (type == null) return false;
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (!(declarationDescriptor instanceof ClassDescriptor)) return false;
        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
        if (classDescriptor.getKind() != ClassKind.ENUM_CLASS || classDescriptor.getModality().isOverridable()) return false;
        Collection<ClassDescriptor> objectDescriptors = getEnumEntriesScope(classDescriptor).getObjectDescriptors();
        boolean isExhaust = true;
        boolean notEmpty = false;
        for (ClassDescriptor descriptor : objectDescriptors) {
            if (descriptor.getKind() == ClassKind.ENUM_ENTRY) {
                notEmpty = true;
                if (!containsEnumEntryCase(expression, descriptor, trace)) {
                    isExhaust = false;
                }
            }
        }
        return isExhaust && notEmpty;
    }

    private static boolean containsEnumEntryCase(
            @NotNull JetWhenExpression whenExpression,
            @NotNull ClassDescriptor enumEntry,
            @NotNull BindingTrace trace
    ) {
        assert enumEntry.getKind() == ClassKind.ENUM_ENTRY;
        for (JetWhenEntry whenEntry : whenExpression.getEntries()) {
            for (JetWhenCondition condition : whenEntry.getConditions()) {
                if (!(condition instanceof JetWhenConditionWithExpression)) {
                    continue;
                }
                if (isCheckForEnumEntry((JetWhenConditionWithExpression) condition, enumEntry, trace)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isCheckForEnumEntry(
            @NotNull JetWhenConditionWithExpression whenExpression,
            @NotNull ClassDescriptor enumEntry,
            @NotNull BindingTrace trace
    ) {
        JetSimpleNameExpression reference = getReference(whenExpression.getExpression());
        if (reference == null) return false;

        DeclarationDescriptor target = trace.get(BindingContext.REFERENCE_TARGET, reference);
        if (!(target instanceof VariableDescriptorForObject)) {
            return false;
        }

        ClassDescriptor classDescriptor = ((VariableDescriptorForObject) target).getObjectClass();
        return classDescriptor == enumEntry;
    }

    @Nullable
    private static JetSimpleNameExpression getReference(@Nullable JetExpression expression) {
        if (expression == null) {
            return null;
        }
        if (expression instanceof JetSimpleNameExpression) {
            return (JetSimpleNameExpression) expression;
        }
        if (expression instanceof JetQualifiedExpression) {
            return getReference(((JetQualifiedExpression) expression).getSelectorExpression());
        }
        return null;
    }
}
