package org.jetbrains.jet.lang.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;

import java.util.Collections;
import java.util.Set;

/**
 * @author svtk
 */
public class WhenChecker {
    public static boolean isWhenExhaust(@NotNull JetWhenExpression expression, @NotNull BindingTrace trace) {
        JetExpression subjectExpression = expression.getSubjectExpression();
        if (subjectExpression == null) return false;
        JetType type = trace.get(BindingContext.EXPRESSION_TYPE, subjectExpression);
        if (type == null) return false;
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        if (!(declarationDescriptor instanceof ClassDescriptor)) return false;
        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
        if (classDescriptor.getKind() != ClassKind.ENUM_CLASS || classDescriptor.getModality().isOverridable()) return false;
        ClassDescriptor classObjectDescriptor = classDescriptor.getClassObjectDescriptor();
        assert classObjectDescriptor != null;
        JetScope memberScope = classObjectDescriptor.getMemberScope(Collections.<TypeProjection>emptyList());
        Set<ClassDescriptor> objectDescriptors = memberScope.getObjectDescriptors();
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

    private static boolean containsEnumEntryCase(@NotNull JetWhenExpression whenExpression, @NotNull ClassDescriptor enumEntry, @NotNull BindingTrace trace) {
        assert enumEntry.getKind() == ClassKind.ENUM_ENTRY;
        for (JetWhenEntry whenEntry : whenExpression.getEntries()) {
            for (JetWhenCondition condition : whenEntry.getConditions()) {
                if (condition instanceof JetWhenConditionWithExpression) {
                    JetExpressionPattern pattern = ((JetWhenConditionWithExpression) condition).getPattern();
                    if (pattern == null) continue;
                    JetExpression patternExpression = pattern.getExpression();
                    JetType type = trace.get(BindingContext.EXPRESSION_TYPE, patternExpression);
                    if (type == null) continue;
                    if (type.getConstructor().getDeclarationDescriptor() == enumEntry) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
