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

package org.jetbrains.kotlin.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilsKt;
import org.jetbrains.kotlin.types.FlexibleTypesKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.HashSet;
import java.util.Set;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass;

public final class WhenChecker {
    private WhenChecker() {
    }

    public static boolean mustHaveElse(@NotNull KtWhenExpression expression, @NotNull BindingTrace trace) {
        KotlinType expectedType = trace.get(BindingContext.EXPECTED_EXPRESSION_TYPE, expression);
        boolean isUnit = expectedType != null && KotlinBuiltIns.isUnit(expectedType);
        // Some "statements" are actually expressions returned from lambdas, their expected types are non-null
        boolean isStatement = BindingContextUtilsKt.isUsedAsStatement(expression, trace.getBindingContext()) && expectedType == null;

        return !isUnit && !isStatement && !isWhenExhaustive(expression, trace);
    }

    public static boolean isWhenByEnum(@NotNull KtWhenExpression expression, @NotNull BindingContext context) {
        return getClassDescriptorOfTypeIfEnum(whenSubjectType(expression, context)) != null;
    }

    @Nullable
    public static ClassDescriptor getClassDescriptorOfTypeIfEnum(@Nullable KotlinType type) {
        if (type == null) return null;
        ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(type);
        if (classDescriptor == null) return null;
        if (classDescriptor.getKind() != ClassKind.ENUM_CLASS || classDescriptor.getModality().isOverridable()) return null;

        return classDescriptor;
    }

    @Nullable
    private static KotlinType whenSubjectType(@NotNull KtWhenExpression expression, @NotNull BindingContext context) {
        KtExpression subjectExpression = expression.getSubjectExpression();
        return subjectExpression == null ? null : context.getType(subjectExpression);
    }

    private static boolean isWhenOnBooleanExhaustive(@NotNull KtWhenExpression expression, @NotNull BindingTrace trace) {
        // It's assumed (and not checked) that expression is of the boolean type
        boolean containsFalse = false;
        boolean containsTrue = false;
        for (KtWhenEntry whenEntry: expression.getEntries()) {
            for (KtWhenCondition whenCondition : whenEntry.getConditions()) {
                if (whenCondition instanceof KtWhenConditionWithExpression) {
                    KtExpression whenExpression = ((KtWhenConditionWithExpression) whenCondition).getExpression();
                    if (CompileTimeConstantUtils.canBeReducedToBooleanConstant(whenExpression, trace, true)) containsTrue = true;
                    if (CompileTimeConstantUtils.canBeReducedToBooleanConstant(whenExpression, trace, false)) containsFalse = true;
                }
            }
        }
        return containsFalse && containsTrue;
    }

    public static boolean isWhenOnEnumExhaustive(
            @NotNull KtWhenExpression expression,
            @NotNull BindingTrace trace,
            @NotNull ClassDescriptor enumClassDescriptor
    ) {
        assert isEnumClass(enumClassDescriptor) :
                "isWhenOnEnumExhaustive should be called with an enum class descriptor";
        Set<ClassDescriptor> entryDescriptors = new HashSet<ClassDescriptor>();
        for (DeclarationDescriptor descriptor : enumClassDescriptor.getUnsubstitutedInnerClassesScope().getAllDescriptors()) {
            if (isEnumEntry(descriptor)) {
                entryDescriptors.add((ClassDescriptor) descriptor);
            }
        }
        return !entryDescriptors.isEmpty() && containsAllClassCases(expression, entryDescriptors, trace);
    }

    private static void collectNestedSubclasses(
            @NotNull ClassDescriptor baseDescriptor,
            @NotNull ClassDescriptor currentDescriptor,
            @NotNull Set<ClassDescriptor> subclasses
    ) {
        for (DeclarationDescriptor descriptor : currentDescriptor.getUnsubstitutedInnerClassesScope().getAllDescriptors()) {
            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor memberClassDescriptor = (ClassDescriptor) descriptor;
                if (DescriptorUtils.isDirectSubclass(memberClassDescriptor, baseDescriptor)) {
                    subclasses.add(memberClassDescriptor);
                }
                collectNestedSubclasses(baseDescriptor, memberClassDescriptor, subclasses);
            }
        }
    }

    private static boolean isWhenOnSealedClassExhaustive(
            @NotNull KtWhenExpression expression,
            @NotNull BindingTrace trace,
            @NotNull ClassDescriptor classDescriptor
    ) {
        assert classDescriptor.getModality() == Modality.SEALED :
                "isWhenOnSealedClassExhaustive should be called with a sealed class descriptor";
        Set<ClassDescriptor> memberClassDescriptors = new HashSet<ClassDescriptor>();
        collectNestedSubclasses(classDescriptor, classDescriptor, memberClassDescriptors);
        // When on a sealed class without derived members is considered non-exhaustive (see test WhenOnEmptySealed)
        return !memberClassDescriptors.isEmpty() && containsAllClassCases(expression, memberClassDescriptors, trace);
    }

    /**
     * It's assumed that function is called for a final type. In this case the only possible smart cast is to not nullable type.
     * @return true if type is nullable, and cannot be smart casted
     */
    private static boolean isNullableTypeWithoutPossibleSmartCast(
            @Nullable KtExpression expression,
            @NotNull KotlinType type,
            @NotNull BindingContext context
    ) {
        if (expression == null) return false; // Normally should not happen
        if (!TypeUtils.isNullableType(type)) return false;
        // We cannot read data flow information here due to lack of inputs (module descriptor is necessary)
        if (context.get(BindingContext.SMARTCAST, expression) != null) {
            // We have smart cast from enum or boolean to something
            // Not very nice but we *can* decide it was smart cast to not-null
            // because both enum and boolean are final
            return false;
        }
        return true;
    }

    public static boolean isWhenExhaustive(@NotNull KtWhenExpression expression, @NotNull BindingTrace trace) {
        KotlinType type = whenSubjectType(expression, trace.getBindingContext());
        if (type == null) return false;
        ClassDescriptor enumClassDescriptor = getClassDescriptorOfTypeIfEnum(type);

        boolean exhaustive;
        if (enumClassDescriptor == null) {
            if (KotlinBuiltIns.isBoolean(TypeUtils.makeNotNullable(type))) {
                exhaustive = isWhenOnBooleanExhaustive(expression, trace);
            }
            else {
                ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(type);
                exhaustive = (classDescriptor != null
                              && classDescriptor.getModality() == Modality.SEALED
                              && isWhenOnSealedClassExhaustive(expression, trace, classDescriptor));
            }
        }
        else {
            exhaustive = isWhenOnEnumExhaustive(expression, trace, enumClassDescriptor);
        }
        if (exhaustive) {
            if (// Flexible (nullable) enum types are also counted as exhaustive
                (enumClassDescriptor != null && FlexibleTypesKt.isFlexible(type))
                || containsNullCase(expression, trace)
                || !isNullableTypeWithoutPossibleSmartCast(expression.getSubjectExpression(), type, trace.getBindingContext())) {

                trace.record(BindingContext.EXHAUSTIVE_WHEN, expression);
                return true;
            }
        }
        return false;
    }

    private static boolean containsAllClassCases(
            @NotNull KtWhenExpression whenExpression,
            @NotNull Set<ClassDescriptor> memberDescriptors,
            @NotNull BindingTrace trace
    ) {
        Set<ClassDescriptor> checkedDescriptors = new HashSet<ClassDescriptor>();
        for (KtWhenEntry whenEntry : whenExpression.getEntries()) {
            for (KtWhenCondition condition : whenEntry.getConditions()) {
                boolean negated = false;
                ClassDescriptor checkedDescriptor = null;
                if (condition instanceof KtWhenConditionIsPattern) {
                    KtWhenConditionIsPattern conditionIsPattern = (KtWhenConditionIsPattern) condition;
                    KotlinType checkedType = trace.get(BindingContext.TYPE, conditionIsPattern.getTypeReference());
                    if (checkedType != null) {
                        checkedDescriptor = TypeUtils.getClassDescriptor(checkedType);
                    }
                    negated = conditionIsPattern.isNegated();
                }
                else if (condition instanceof KtWhenConditionWithExpression) {
                    KtWhenConditionWithExpression conditionWithExpression = (KtWhenConditionWithExpression) condition;
                    if (conditionWithExpression.getExpression() != null) {
                        KtSimpleNameExpression reference = getReference(conditionWithExpression.getExpression());
                        if (reference != null) {
                            DeclarationDescriptor target = trace.get(BindingContext.REFERENCE_TARGET, reference);
                            if (target instanceof ClassDescriptor) {
                                checkedDescriptor = (ClassDescriptor) target;
                            }
                        }
                    }
                }

                // Checks are important only for nested subclasses of the sealed class
                // In additional, check without "is" is important only for objects
                if (checkedDescriptor == null
                    || !memberDescriptors.contains(checkedDescriptor)
                    || (condition instanceof KtWhenConditionWithExpression
                        && !DescriptorUtils.isObject(checkedDescriptor)
                        && !DescriptorUtils.isEnumEntry(checkedDescriptor))) {
                    continue;
                }
                if (negated) {
                    if (checkedDescriptors.contains(checkedDescriptor)) return true; // all members are already there
                    checkedDescriptors.addAll(memberDescriptors);
                    checkedDescriptors.remove(checkedDescriptor);
                }
                else {
                    checkedDescriptors.add(checkedDescriptor);
                }
            }
        }
        return checkedDescriptors.containsAll(memberDescriptors);
    }

    public static boolean containsNullCase(@NotNull KtWhenExpression expression, @NotNull BindingTrace trace) {
        for (KtWhenEntry entry : expression.getEntries()) {
            for (KtWhenCondition condition : entry.getConditions()) {
                if (condition instanceof KtWhenConditionWithExpression) {
                    KtWhenConditionWithExpression conditionWithExpression = (KtWhenConditionWithExpression) condition;
                    if (conditionWithExpression.getExpression() != null) {
                        KotlinType type = trace.getBindingContext().getType(conditionWithExpression.getExpression());
                        if (type != null && KotlinBuiltIns.isNothingOrNullableNothing(type)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    private static KtSimpleNameExpression getReference(@Nullable KtExpression expression) {
        if (expression == null) {
            return null;
        }
        if (expression instanceof KtSimpleNameExpression) {
            return (KtSimpleNameExpression) expression;
        }
        if (expression instanceof KtQualifiedExpression) {
            return getReference(((KtQualifiedExpression) expression).getSelectorExpression());
        }
        return null;
    }
}
