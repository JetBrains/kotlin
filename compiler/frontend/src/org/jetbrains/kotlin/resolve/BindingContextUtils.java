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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.Severity;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.diagnostics.MutableDiagnosticsWithSuppression;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import org.jetbrains.kotlin.util.slicedMap.*;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

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
    public static VariableDescriptor extractVariableDescriptorIfAny(@NotNull BindingContext bindingContext, @Nullable KtElement element, boolean onlyReference) {
        DeclarationDescriptor descriptor = null;
        if (!onlyReference && (element instanceof KtVariableDeclaration || element instanceof KtParameter)) {
            descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        }
        else if (element instanceof KtSimpleNameExpression) {
            descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (KtSimpleNameExpression) element);
        }
        else if (element instanceof KtQualifiedExpression) {
            descriptor = extractVariableDescriptorIfAny(bindingContext, ((KtQualifiedExpression) element).getSelectorExpression(), onlyReference);
        }
        if (descriptor instanceof VariableDescriptor) {
            return (VariableDescriptor) descriptor;
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

    public static FunctionDescriptor getEnclosingFunctionDescriptor(@NotNull BindingContext context, @NotNull KtElement element) {
        KtFunction function = PsiTreeUtil.getParentOfType(element, KtFunction.class);
        return (FunctionDescriptor)context.get(DECLARATION_TO_DESCRIPTOR, function);
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
        if (!context.get(BindingContext.PROCESSED, expression)) return null;
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

    public static boolean isVarCapturedInClosure(BindingContext bindingContext, DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof VariableDescriptor) || descriptor instanceof PropertyDescriptor) return false;
        VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
        return bindingContext.get(CAPTURED_IN_CLOSURE, variableDescriptor) != null && variableDescriptor.isVar();
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

        return new Pair<FunctionDescriptor, PsiElement>(containingFunctionDescriptor, containingFunction);
    }

    @Nullable
    public static ResolvedCall<ConstructorDescriptor> getDelegationConstructorCall(
            @NotNull BindingContext bindingContext,
            @NotNull ConstructorDescriptor constructorDescriptor
    ) {
        return bindingContext.get(CONSTRUCTOR_RESOLVED_DELEGATION_CALL, constructorDescriptor);
    }

    static void addOwnDataTo(
            @NotNull final BindingTrace trace, @Nullable final TraceEntryFilter filter, boolean commitDiagnostics,
            @NotNull MutableSlicedMap map, MutableDiagnosticsWithSuppression diagnostics
    ) {
        map.forEach(new Function3<WritableSlice, Object, Object, Void>() {
            @Override
            public Void invoke(WritableSlice slice, Object key, Object value) {
                if (filter == null || filter.accept(slice, null, key)) {
                    trace.record(slice, key, value);
                }

                return null;
            }
        });

        if (!commitDiagnostics) return;

        for (Diagnostic diagnostic : diagnostics.getOwnDiagnostics()) {
            if (filter == null || filter.accept(null, diagnostic, diagnostic.getPsiElement())) {
                trace.report(diagnostic);
            }
        }

    }
}
