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

import com.intellij.codeInsight.AnnotationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils;
import org.jetbrains.kotlin.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass;

public final class WhenChecker {
    private WhenChecker() {
    }

    public static boolean mustHaveElse(@NotNull JetWhenExpression expression, @NotNull BindingTrace trace) {
        JetType expectedType = trace.get(BindingContext.EXPECTED_EXPRESSION_TYPE, expression);
        boolean isUnit = expectedType != null && KotlinBuiltIns.isUnit(expectedType);
        // Some "statements" are actually expressions returned from lambdas, their expected types are non-null
        boolean isStatement = BindingContextUtilPackage.isUsedAsStatement(expression, trace.getBindingContext()) && expectedType == null;

        return !isUnit && !isStatement && !isWhenExhaustive(expression, trace);
    }

    private static final FqName notNullAnnotationName = new FqName(AnnotationUtil.NOT_NULL);

    public static boolean isExhaustiveWhenOnPlatformNullableEnum(@NotNull JetWhenExpression expression, @NotNull BindingTrace trace) {
        JetType type = whenSubjectType(expression, trace.getBindingContext());
        if (type == null) return false;
        ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(type);
        return (isPlatformEnum(type, classDescriptor)
                && isWhenOnEnumExhaustive(expression, trace, classDescriptor)
                // nullable from Kotlin side
                && TypeUtils.isNullableType(type)
                // and from Java side too
                && type.getAnnotations().findAnnotation(notNullAnnotationName) == null
                && type.getAnnotations().findExternalAnnotation(notNullAnnotationName) == null
                // but no null case
                && !containsNullCase(expression, trace));
    }

    public static boolean isWhenByEnum(@NotNull JetWhenExpression expression, @NotNull BindingContext context) {
        return getClassDescriptorOfTypeIfEnum(whenSubjectType(expression, context)) != null;
    }

    @Nullable
    private static ClassDescriptor getClassDescriptorOfTypeIfEnum(@Nullable JetType type) {
        if (type == null) return null;
        ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(type);
        if (classDescriptor == null) return null;
        if (classDescriptor.getKind() != ClassKind.ENUM_CLASS || classDescriptor.getModality().isOverridable()) return null;

        return classDescriptor;
    }
        
    @Nullable
    private static JetType whenSubjectType(@NotNull JetWhenExpression expression, @NotNull BindingContext context) {
        JetExpression subjectExpression = expression.getSubjectExpression();
        return subjectExpression == null ? null : context.getType(subjectExpression);
    }

    private static boolean isWhenOnBooleanExhaustive(@NotNull JetWhenExpression expression, @NotNull BindingTrace trace) {
        // It's assumed (and not checked) that expression is of the boolean type
        boolean containsFalse = false;
        boolean containsTrue = false;
        for (JetWhenEntry whenEntry: expression.getEntries()) {
            for (JetWhenCondition whenCondition : whenEntry.getConditions()) {
                if (whenCondition instanceof JetWhenConditionWithExpression) {
                    JetExpression whenExpression = ((JetWhenConditionWithExpression) whenCondition).getExpression();
                    if (CompileTimeConstantUtils.canBeReducedToBooleanConstant(whenExpression, trace, true)) containsTrue = true;
                    if (CompileTimeConstantUtils.canBeReducedToBooleanConstant(whenExpression, trace, false)) containsFalse = true;
                }
            }
        }
        return containsFalse && containsTrue;
    }

    private static boolean isWhenOnEnumExhaustive(
            @NotNull JetWhenExpression expression, @NotNull BindingTrace trace, @NotNull ClassDescriptor enumClassDescriptor) {
        assert isEnumClass(enumClassDescriptor);
        boolean notEmpty = false;
        for (DeclarationDescriptor descriptor : enumClassDescriptor.getUnsubstitutedInnerClassesScope().getAllDescriptors()) {
            if (isEnumEntry(descriptor)) {
                notEmpty = true;
                if (!containsEnumEntryCase(expression, (ClassDescriptor) descriptor, trace)) {
                    return false;
                }
            }
        }
        return notEmpty;
    }

    private static boolean isWhenOnSealedClassExhaustive(
            @NotNull JetWhenExpression expression, @NotNull BindingTrace trace, @NotNull ClassDescriptor enumClassDescriptor) {
        // TODO: sealed classes, related problems: KT-6299, KT-7606
        // All possible subclasses must be defined inside base class
        // Inner classes / objects are OK, but what should we do with local classes used e.g. in properties / local variables?
        return false;
    }

    private static boolean isSealed(@NotNull ClassDescriptor descriptor, @NotNull JetClass klass) {
        // Class is assumed as sealed if and only if all its constructors are private AND the class itself is abstract
        for (ConstructorDescriptor constructorDescriptor: descriptor.getConstructors()) {
            if (constructorDescriptor.getVisibility() != Visibilities.PRIVATE) {
                return false;
            }
        }
        return PsiUtilPackage.isAbstract(klass);
    }

    private static boolean isPlatformEnum(@NotNull JetType type, @Nullable ClassDescriptor classDescriptor) {
        // instanceof JetClass are Kotlin types, as well as nullable types
        return classDescriptor != null && classDescriptor.getKind() == ClassKind.ENUM_CLASS
               && !(type instanceof JetClass) && !type.isMarkedNullable();
    }

    public static boolean isWhenExhaustive(@NotNull JetWhenExpression expression, @NotNull BindingTrace trace) {
        JetType type = whenSubjectType(expression, trace.getBindingContext());
        if (type == null) return false;
        ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(type);

        boolean exhaustive;
        if (classDescriptor != null) {
            if (classDescriptor.getKind() == ClassKind.ENUM_CLASS && !classDescriptor.getModality().isOverridable()) {
                // Enum
                exhaustive = isWhenOnEnumExhaustive(expression, trace, classDescriptor);
            }
            else if (KotlinBuiltIns.isBoolean(TypeUtils.makeNotNullable(type))) {
                // Boolean
                exhaustive = isWhenOnBooleanExhaustive(expression, trace);
            }
            else if (type instanceof JetClass && isSealed(classDescriptor, (JetClass) type)) {
                exhaustive = isWhenOnSealedClassExhaustive(expression, trace, classDescriptor);
            }
            else {
                exhaustive = false;

            }
        }
        else {
            exhaustive = false;
        }
        if (exhaustive && (!TypeUtils.isNullableType(type) || containsNullCase(expression, trace) || isPlatformEnum(type, classDescriptor))) {
            trace.record(BindingContext.EXHAUSTIVE_WHEN, expression);
            return true;
        }
        return false;
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

    private static boolean containsNullCase(@NotNull JetWhenExpression expression, @NotNull BindingTrace trace) {
        for (JetWhenEntry entry : expression.getEntries()) {
            for (JetWhenCondition condition : entry.getConditions()) {
                if (condition instanceof JetWhenConditionWithExpression) {
                    JetType type = trace.getBindingContext().getType(((JetWhenConditionWithExpression) condition).getExpression());
                    if (type != null && KotlinBuiltIns.isNothingOrNullableNothing(type)) {
                        return true;
                    }
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
