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
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isEnumEntry;

public final class WhenChecker {
    private WhenChecker() {
    }

    public static boolean mustHaveElse(@NotNull JetWhenExpression expression, @NotNull BindingTrace trace) {
        JetType expectedType = trace.get(BindingContext.EXPECTED_EXPRESSION_TYPE, expression);
        boolean isUnit = expectedType != null && KotlinBuiltIns.getInstance().isUnit(expectedType);
        // Some "statements" are actually expressions returned from lambdas, their expected types are non-null
        boolean isStatement = Boolean.TRUE.equals(trace.get(BindingContext.STATEMENT, expression)) && expectedType == null;

        return !isUnit && !isStatement && !isWhenExhaustive(expression, trace);
    }

    public static boolean isWhenByEnum(@NotNull JetWhenExpression expression, @NotNull BindingContext context) {
        return getSubjectClassDescriptorIfEnum(expression, context) != null;
    }

    private static ClassDescriptor getSubjectClassDescriptorIfEnum(@NotNull JetWhenExpression expression, @NotNull BindingContext context) {
        JetExpression subjectExpression = expression.getSubjectExpression();
        if (subjectExpression == null) return null;
        JetType type = context.get(BindingContext.EXPRESSION_TYPE, subjectExpression);
        if (type == null) return null;
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (!(declarationDescriptor instanceof ClassDescriptor)) return null;
        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
        if (classDescriptor.getKind() != ClassKind.ENUM_CLASS || classDescriptor.getModality().isOverridable()) return null;

        return classDescriptor;
    }

    private static boolean isWhenExhaustive(@NotNull JetWhenExpression expression, @NotNull BindingTrace trace) {
        ClassDescriptor classDescriptor = getSubjectClassDescriptorIfEnum(expression, trace.getBindingContext());

        if (classDescriptor == null) return false;

        boolean isExhaust = true;
        boolean notEmpty = false;
        for (DeclarationDescriptor descriptor : classDescriptor.getUnsubstitutedInnerClassesScope().getAllDescriptors()) {
            if (isEnumEntry(descriptor)) {
                notEmpty = true;
                if (!containsEnumEntryCase(expression, (ClassDescriptor) descriptor, trace)) {
                    isExhaust = false;
                }
            }
        }
        boolean exhaustive = isExhaust && notEmpty;
        if (exhaustive) {
            trace.record(BindingContext.EXHAUSTIVE_WHEN, expression);
        }
        return exhaustive;
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
        return target == enumEntry;
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
