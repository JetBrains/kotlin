/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.calls.tower.TowerLevelsKt;
import org.jetbrains.kotlin.resolve.diagnostics.MutableDiagnosticsWithSuppression;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.expressions.CaptureKind;
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import org.jetbrains.kotlin.util.slicedMap.MutableSlicedMap;
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Errors.AMBIGUOUS_LABEL;
import static org.jetbrains.kotlin.resolve.BindingContext.*;

public class BindingContextUtils {
    private BindingContextUtils() {
    }

    @Nullable
    public static VariableDescriptor extractVariableFromResolvedCall(
            @NotNull BindingContext bindingContext,
            @Nullable KtElement callElement
    ) {
        ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilKt.getResolvedCall(callElement, bindingContext);
        if (resolvedCall == null || !(resolvedCall.getResultingDescriptor() instanceof VariableDescriptor)) return null;
        return (VariableDescriptor) resolvedCall.getResultingDescriptor();
    }

    @Nullable
    public static VariableDescriptor variableDescriptorForDeclaration(@Nullable DeclarationDescriptor descriptor) {
        if (descriptor instanceof VariableDescriptor)
            return (VariableDescriptor) descriptor;
        if (descriptor instanceof ClassDescriptor) {
            return TowerLevelsKt.getFakeDescriptorForObject((ClassDescriptor) descriptor);
        }
        return null;
    }

    @Nullable
    public static VariableDescriptor extractVariableDescriptorFromReference(
            @NotNull BindingContext bindingContext,
            @Nullable KtElement element
    ) {
        if (element instanceof KtSimpleNameExpression) {
            return variableDescriptorForDeclaration(bindingContext.get(BindingContext.REFERENCE_TARGET, (KtSimpleNameExpression) element));
        }
        else if (element instanceof KtQualifiedExpression) {
            return extractVariableDescriptorFromReference(bindingContext, ((KtQualifiedExpression) element).getSelectorExpression());
        }
        return null;
    }

    public static void recordFunctionDeclarationToDescriptor(@NotNull BindingTrace trace,
            @NotNull PsiElement psiElement, @NotNull SimpleFunctionDescriptor function) {
        trace.record(BindingContext.FUNCTION, psiElement, function);
    }

    @NotNull
    public static <K, V> V getNotNull(
        @NotNull BindingContext bindingContext,
        @NotNull ReadOnlySlice<K, V> slice,
        @NotNull K key
    ) {
        return getNotNull(bindingContext, slice, key, "Value at " + slice + " must not be null for " + key);
    }

    @NotNull
    public static KotlinType getTypeNotNull(
            @NotNull BindingContext bindingContext,
            @NotNull KtExpression expression
    ) {
        KotlinType result = bindingContext.getType(expression);
        if (result == null) {
            throw new IllegalStateException("Type must be not null for " + expression);
        }
        return result;
    }

    @NotNull
    public static <K, V> V getNotNull(
            @NotNull BindingContext bindingContext,
            @NotNull ReadOnlySlice<K, V> slice,
            @NotNull K key,
            @NotNull String messageIfNull
    ) {
        V value = bindingContext.get(slice, key);
        if (value == null) {
            throw new IllegalStateException(messageIfNull);
        }
        return value;
    }

    @NotNull
    public static DeclarationDescriptor getEnclosingDescriptor(@NotNull BindingContext context, @NotNull KtElement element) {
        KtNamedDeclaration declaration = PsiTreeUtil.getParentOfType(element, KtNamedDeclaration.class);
        if (declaration instanceof KtFunctionLiteral) {
            return getEnclosingDescriptor(context, declaration);
        }
        DeclarationDescriptor descriptor = context.get(DECLARATION_TO_DESCRIPTOR, declaration);
        assert descriptor != null : "No descriptor for named declaration: " + declaration.getText() + "\n(of type " + declaration.getClass() + ")";
        return descriptor;
    }

    @Nullable
    public static FunctionDescriptor getEnclosingFunctionDescriptor(@NotNull BindingContext context, @NotNull KtElement element) {
        KtElement functionOrClass = PsiTreeUtil.getParentOfType(element, KtFunction.class, KtClassOrObject.class);
        DeclarationDescriptor descriptor = context.get(DECLARATION_TO_DESCRIPTOR, functionOrClass);
        if (functionOrClass instanceof KtFunction) {
            if (descriptor instanceof FunctionDescriptor) return (FunctionDescriptor) descriptor;
            return null;
        }
        else {
            if (descriptor instanceof ClassDescriptor) return ((ClassDescriptor) descriptor).getUnsubstitutedPrimaryConstructor();
            return null;
        }
    }

    public static void reportAmbiguousLabel(
            @NotNull BindingTrace trace,
            @NotNull KtSimpleNameExpression targetLabel,
            @NotNull Collection<DeclarationDescriptor> declarationsByLabel
    ) {
        Collection<PsiElement> targets = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : declarationsByLabel) {
            PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
            assert element != null : "Label can only point to something in the same lexical scope";
            targets.add(element);
        }
        if (!targets.isEmpty()) {
            trace.record(AMBIGUOUS_LABEL_TARGET, targetLabel, targets);
        }
        trace.report(AMBIGUOUS_LABEL.on(targetLabel));
    }

    @Nullable
    public static KotlinType updateRecordedType(
            @Nullable KotlinType type,
            @NotNull KtExpression expression,
            @NotNull BindingTrace trace,
            boolean shouldBeMadeNullable
    ) {
        if (type == null) return null;
        if (shouldBeMadeNullable) {
            type = TypeUtils.makeNullable(type);
        }
        trace.recordType(expression, type);
        return type;
    }

    @Nullable
    public static KotlinTypeInfo getRecordedTypeInfo(@NotNull KtExpression expression, @NotNull BindingContext context) {
        // noinspection ConstantConditions
        if (context.get(BindingContext.PROCESSED, expression) != Boolean.TRUE) return null;
        // NB: should never return null if expression is already processed
        KotlinTypeInfo result = context.get(BindingContext.EXPRESSION_TYPE_INFO, expression);
        return result != null ? result : TypeInfoFactoryKt.noTypeInfo(DataFlowInfoFactory.EMPTY);
    }

    public static boolean isExpressionWithValidReference(
            @NotNull KtExpression expression,
            @NotNull BindingContext context
    ) {
        if (expression instanceof KtCallExpression) {
            ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(expression, context);
            return resolvedCall instanceof VariableAsFunctionResolvedCall;
        }
        return expression instanceof KtReferenceExpression;
    }

    public static boolean isCapturedInClosure(BindingContext bindingContext, DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof VariableDescriptor) || descriptor instanceof PropertyDescriptor) return false;
        VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
        return bindingContext.get(CAPTURED_IN_CLOSURE, variableDescriptor) != null;
    }

    public static boolean isCapturedInClosureWithExactlyOnceEffect(BindingContext bindingContext, DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof VariableDescriptor) || descriptor instanceof PropertyDescriptor) return false;
        VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
        return bindingContext.get(CAPTURED_IN_CLOSURE, variableDescriptor) == CaptureKind.EXACTLY_ONCE_EFFECT;
    }

    public static boolean isBoxedLocalCapturedInClosure(BindingContext bindingContext, DeclarationDescriptor descriptor) {
        return (isCapturedInClosure(bindingContext, descriptor) && ((VariableDescriptor) descriptor).isVar()) ||
               isCapturedInClosureWithExactlyOnceEffect(bindingContext, descriptor);
    }

    @NotNull
    public static Pair<FunctionDescriptor, PsiElement> getContainingFunctionSkipFunctionLiterals(
            @Nullable DeclarationDescriptor startDescriptor,
            boolean strict
    ) {
        FunctionDescriptor containingFunctionDescriptor = DescriptorUtils.getParentOfType(startDescriptor, FunctionDescriptor.class, strict);
        PsiElement containingFunction =
                containingFunctionDescriptor != null ? DescriptorToSourceUtils.getSourceFromDescriptor(containingFunctionDescriptor) : null;
        while (containingFunction instanceof KtFunctionLiteral) {
            containingFunctionDescriptor = DescriptorUtils.getParentOfType(containingFunctionDescriptor, FunctionDescriptor.class);
            containingFunction = containingFunctionDescriptor != null ? DescriptorToSourceUtils
                    .getSourceFromDescriptor(containingFunctionDescriptor) : null;
        }

        return new Pair<>(containingFunctionDescriptor, containingFunction);
    }

    @Nullable
    public static ResolvedCall<ConstructorDescriptor> getDelegationConstructorCall(
            @NotNull BindingContext bindingContext,
            @NotNull ConstructorDescriptor constructorDescriptor
    ) {
        return bindingContext.get(CONSTRUCTOR_RESOLVED_DELEGATION_CALL, constructorDescriptor);
    }

    @SuppressWarnings("unchecked")
    static void addOwnDataTo(
            @NotNull BindingTrace trace, @Nullable TraceEntryFilter filter, boolean commitDiagnostics,
            @NotNull MutableSlicedMap map, MutableDiagnosticsWithSuppression diagnostics
    ) {
        map.forEach((slice, key, value) -> {
            if (filter == null || filter.accept(slice, key)) {
                trace.record(slice, key, value);
            }

            return null;
        });

        if (!commitDiagnostics) return;

        for (Diagnostic diagnostic : diagnostics.getOwnDiagnostics()) {
            if (filter == null || filter.accept(null, diagnostic.getPsiElement())) {
                trace.report(diagnostic);
            }
        }

    }
}
